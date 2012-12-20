/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.picketlink.idm.jpa.internal;

import static org.picketlink.idm.jpa.internal.JPAIdentityStoreConfiguration.PROPERTY_IDENTITY_NAME;

import java.util.List;

import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import org.picketlink.idm.event.AbstractBaseEvent;
import org.picketlink.idm.event.RoleCreatedEvent;
import org.picketlink.idm.event.RoleDeletedEvent;
import org.picketlink.idm.event.RoleUpdatedEvent;
import org.picketlink.idm.internal.util.properties.Property;
import org.picketlink.idm.model.IdentityType;
import org.picketlink.idm.model.Role;
import org.picketlink.idm.model.SimpleGroup;
import org.picketlink.idm.model.SimpleRole;
import org.picketlink.idm.query.QueryParameter;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 *
 */
public class RoleTypeManager extends IdentityTypeManager<Role>{

    public RoleTypeManager(JPAIdentityStore store) {
        super(store);
    }

    @Override
    protected void fromIdentityType(Object toIdentity, Role fromRole) {
        getStore().setModelProperty(toIdentity, PROPERTY_IDENTITY_NAME, fromRole.getName(), true);
    }

    @Override
    protected AbstractBaseEvent raiseCreatedEvent(Role fromIdentityType) {
        return new RoleCreatedEvent(fromIdentityType);
    }

    @Override
    protected AbstractBaseEvent raiseUpdatedEvent(Role fromIdentityType) {
        return new RoleUpdatedEvent(fromIdentityType);
    }

    @Override
    protected AbstractBaseEvent raiseDeletedEvent(Role fromIdentityType) {
        return new RoleDeletedEvent(fromIdentityType);
    }

    @Override
    protected Role createIdentityType(Object identity) {
        String name = getStore().getModelProperty(String.class, identity, PROPERTY_IDENTITY_NAME);

        SimpleRole role = new SimpleRole(name);
        
        return role;
    }
    
    @Override
    protected List<Predicate> getPredicate(QueryParameter queryParameter, Object[] parameterValues,
            JPACriteriaQueryBuilder criteria) {
        List<Predicate> predicates = super.getPredicate(queryParameter, parameterValues, criteria);
        
        if (queryParameter.equals(Role.NAME)) {
            predicates.add(criteria.getBuilder().equal(
                    criteria.getRoot().get(getConfig().getModelProperty(PROPERTY_IDENTITY_NAME).getName()),
                    parameterValues[0]));
        }
        
        if (queryParameter.equals(IdentityType.ROLE_OF)) {
            for (Object object : parameterValues) {
                Property<Object> memberModelProperty = getConfig().getModelProperty(JPAIdentityStoreConfiguration.PROPERTY_MEMBERSHIP_MEMBER);
                Property<Object> roleModelProperty = getConfig().getModelProperty(JPAIdentityStoreConfiguration.PROPERTY_MEMBERSHIP_ROLE);


                Subquery<?> subquery = criteria.getCriteria().subquery(getConfig().getMembershipClass());
                Root fromProject = subquery.from(getConfig().getMembershipClass());
                Subquery<?> select = subquery.select(fromProject.get(roleModelProperty.getName()));

                Predicate conjunction = criteria.getBuilder().conjunction();

                conjunction.getExpressions().add(
                        criteria.getBuilder().equal(fromProject.get(roleModelProperty.getName()), criteria.getRoot()));
                conjunction.getExpressions().add(
                        criteria.getBuilder().equal(fromProject.get(memberModelProperty.getName()), getStore().lookupIdentityObjectById((IdentityType) object)));

                subquery.where(conjunction);
                
                predicates.add(criteria.getBuilder().in(criteria.getRoot()).value(subquery));
            }
        }
        
        return predicates;
    }
    
}
