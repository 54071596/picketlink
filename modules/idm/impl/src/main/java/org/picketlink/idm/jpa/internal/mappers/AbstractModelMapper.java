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

import java.lang.annotation.Annotation;
import java.util.List;
import org.picketlink.common.properties.Property;
import org.picketlink.common.properties.query.AnnotatedPropertyCriteria;
import org.picketlink.common.properties.query.NamedPropertyCriteria;
import org.picketlink.common.properties.query.PropertyQueries;

/**
 * @author pedroigor
 */
public abstract class AbstractModelMapper implements ModelMapper {

    protected Property getAnnotatedProperty(Class<? extends Annotation> annotationType, Class<?> type) {
        return PropertyQueries.<String>createQuery(type)
                .addCriteria(new AnnotatedPropertyCriteria(annotationType))
                .getSingleResult();
    }

    protected List<Property<String>> getAnnotatedProperties(Class<? extends Annotation> annotationType, Class<?> type) {
        return PropertyQueries.<String>createQuery(type)
                .addCriteria(new AnnotatedPropertyCriteria(annotationType))
                .getResultList();
    }

    protected Property getNamedProperty(String propertyName, Class<?> type) {
        return PropertyQueries.<String>createQuery(type)
                .addCriteria(new NamedPropertyCriteria(propertyName))
                .getSingleResult();
    }

}
