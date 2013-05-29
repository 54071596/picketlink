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
package org.picketlink.idm.model;


import org.picketlink.idm.model.annotation.AttributeProperty;
import org.picketlink.idm.query.QueryParameter;

/**
 * <p>Default {@link IdentityType} implementation  to represent roles.</p>
 *
 * @author Shane Bryzak
 */
public class Role extends AbstractIdentityType {

    private static final long serialVersionUID = 5641696145573437982L;

    /**
     * A query parameter used to set the name value.
     */
    public static final QueryParameter NAME = new QueryParameter() {};

    private String name;

    public Role() {
    }

    public Role(String name) {
        this.name = name;
    }

    @AttributeProperty
    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
