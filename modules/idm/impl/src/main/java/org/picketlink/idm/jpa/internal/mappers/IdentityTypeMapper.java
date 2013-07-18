/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.picketlink.idm.jpa.internal.mappers;

import org.picketlink.common.properties.Property;
import org.picketlink.common.properties.query.AnnotatedPropertyCriteria;
import org.picketlink.common.properties.query.PropertyQueries;
import org.picketlink.idm.config.SecurityConfigurationException;
import org.picketlink.idm.jpa.annotations.CreationDate;
import org.picketlink.idm.jpa.annotations.Enabled;
import org.picketlink.idm.jpa.annotations.ExpiryDate;
import org.picketlink.idm.jpa.annotations.IdentityClass;
import org.picketlink.idm.jpa.annotations.entity.IdentityManaged;

/**
 * @author pedroigor
 */
public class IdentityTypeMapper extends AbstractAttributedTypeMapper {

    @Override
    public boolean supports(Class<?> entityType) {
        if (entityType.isAnnotationPresent(IdentityManaged.class)) {
            Property<Object> result = PropertyQueries.createQuery(entityType)
                    .addCriteria(new AnnotatedPropertyCriteria(IdentityClass.class)).getFirstResult();

            if (result != null) {
                if (!result.getJavaClass().equals(String.class)) {
                    throw new SecurityConfigurationException("IdentityType entities should be mapped with String valued @IdentityClass property.");
                }
                return true;
            }
        }

        return false;
    }

    @Override
    public EntityMapping doCreateMapping(Class<?> managedType, Class<?> entityType) {
        EntityMapping entityMapping = super.doCreateMapping(managedType, entityType);

        entityMapping.addTypeProperty(getAnnotatedProperty(IdentityClass.class, entityType));
        entityMapping.addProperty(getNamedProperty("enabled", managedType), getAnnotatedProperty(Enabled.class, entityType));
        entityMapping.addProperty(getNamedProperty("createdDate", managedType), getAnnotatedProperty(CreationDate.class, entityType));
        entityMapping.addProperty(getNamedProperty("expirationDate", managedType), getAnnotatedProperty(ExpiryDate.class, entityType));

        return entityMapping;
    }

}