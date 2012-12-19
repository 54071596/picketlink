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

import static org.picketlink.idm.jpa.internal.JPAIdentityStoreConfiguration.PROPERTY_IDENTITY_DISCRIMINATOR;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.picketlink.idm.internal.util.properties.Property;
import org.picketlink.idm.query.IdentityQuery;
import org.picketlink.idm.query.QueryParameter;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 * 
 */
public class JPACriteriaQueryBuilder {

    private JPAIdentityStoreConfiguration config;
    private IdentityQuery<?> identityQuery;
    private EntityManager entityManager;
    private CriteriaBuilder builder;
    private Root<?> root;
    private CriteriaQuery<?> criteria;
    private List<Predicate> predicates = new ArrayList<Predicate>();
    private JPAIdentityStore identityStore;
    private IdentityTypeManager identityTypeManager;

    public JPACriteriaQueryBuilder(JPAIdentityStore identityStore, IdentityQuery<?> identityQuery) {
        this.identityStore = identityStore;
        this.identityQuery = identityQuery;
        this.config = identityStore.getConfig();
        this.identityTypeManager = this.identityStore.getIdentityTypeManager(identityQuery.getIdentityType());
        this.entityManager = identityStore.getEntityManager();
        this.builder = this.entityManager.getCriteriaBuilder();
        
        Class<?> identityClass = this.config.getIdentityClass();
        
        this.criteria = builder.createQuery(identityClass);
        this.root = criteria.from(identityClass);
    }

    public List<Predicate> getPredicates() {
        this.builder = this.entityManager.getCriteriaBuilder();

        String discriminator = this.config.getIdentityTypeDiscriminator(identityQuery.getIdentityType());

        this.predicates.add(builder.equal(root.get(this.config.getModelProperty(PROPERTY_IDENTITY_DISCRIMINATOR).getName()),
                discriminator));

        for (Entry<QueryParameter, Object[]> entry : this.identityQuery.getParameters().entrySet()) {
            QueryParameter queryParameter = entry.getKey();
            Object[] parameterValues = entry.getValue();
            
            this.predicates.addAll(this.identityTypeManager.getPredicate(queryParameter, parameterValues, this));
        }

        return this.predicates;
    }

    public CriteriaQuery<?> getCriteria() {
        return this.criteria;
    }
    
    public CriteriaBuilder getBuilder() {
        return builder;
    }
    
    public Root<?> getRoot() {
        return root;
    }

}
