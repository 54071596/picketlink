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

package org.picketlink.idm.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.picketlink.idm.credential.spi.CredentialHandler;
import org.picketlink.idm.model.AttributedType;
import org.picketlink.idm.model.sample.Agent;
import org.picketlink.idm.model.sample.Grant;
import org.picketlink.idm.model.sample.Group;
import org.picketlink.idm.model.sample.GroupMembership;
import org.picketlink.idm.model.sample.GroupRole;
import org.picketlink.idm.model.sample.Realm;
import org.picketlink.idm.model.sample.Role;
import org.picketlink.idm.model.sample.Tier;
import org.picketlink.idm.model.sample.User;
import org.picketlink.idm.spi.ContextInitializer;
import static org.picketlink.idm.IDMMessages.MESSAGES;
import static org.picketlink.idm.config.IdentityStoreConfiguration.TypeOperation;

/**
 * @author Pedro Igor
 */
public abstract class AbstractIdentityStoreConfigurationBuilder<T extends IdentityStoreConfiguration, S extends IdentityStoreConfigurationBuilder<T, S>>
        extends AbstractIdentityConfigurationChildBuilder implements IdentityStoreConfigurationBuilder<T, S> {

    private Map<Class<? extends AttributedType>, Set<TypeOperation>> supportedTypes = new HashMap<Class<? extends AttributedType>, Set<TypeOperation>>();
    private Map<Class<? extends AttributedType>, Set<TypeOperation>> unsupportedTypes = new HashMap<Class<? extends AttributedType>, Set<TypeOperation>>();

    private List<Class<? extends CredentialHandler>> credentialHandlers = new ArrayList<Class<? extends CredentialHandler>>();
    private Map<String, Object> credentialHandlerProperties = new HashMap<String, Object>();

    private List<ContextInitializer> contextInitializers = new ArrayList<ContextInitializer>();

    private IdentityStoresConfigurationBuilder identityStoresConfigurationBuilder;

    protected AbstractIdentityStoreConfigurationBuilder(IdentityStoresConfigurationBuilder builder) {
        super(builder);
        this.identityStoresConfigurationBuilder = builder;
    }

    public FileStoreConfigurationBuilder file() {
        return this.identityStoresConfigurationBuilder.file();
    }

    public JPAStoreConfigurationBuilder jpa() {
        return this.identityStoresConfigurationBuilder.jpa();
    }

    public LDAPStoreConfigurationBuilder ldap() {
        return this.identityStoresConfigurationBuilder.ldap();
    }

    @Override
    public S supportType(Class<? extends AttributedType>... attributedTypes) {
        if (attributedTypes == null) {
            throw MESSAGES.nullArgument("Attributed Types");
        }

        for (Class<? extends AttributedType> attributedType: attributedTypes) {
            if (!this.supportedTypes.containsKey(attributedType)) {
                List<TypeOperation> defaultTypeOperations = Arrays.asList(TypeOperation.values());
                HashSet<TypeOperation> supportedOperations =
                        new HashSet<TypeOperation>(defaultTypeOperations);
                this.supportedTypes.put(attributedType, supportedOperations);
            }
        }

        return (S) this;
    }

    @Override
    public S unsupportType(Class<? extends AttributedType> type) {
        if (!this.unsupportedTypes.containsKey(type)) {
            this.unsupportedTypes.put(type, new HashSet<TypeOperation>());
        }

        for (TypeOperation op : TypeOperation.values()) {
            this.unsupportedTypes.get(type).add(op);
        }

        return (S) this;
    }

    @Override
    public S supportAllFeatures() {
        return (S) this;
    }

    @Override
    public S addContextInitializer(ContextInitializer contextInitializer) {
        this.contextInitializers.add(contextInitializer);
        return (S) this;
    }

    @Override
    public S setCredentialHandlerProperty(String propertyName, Object value) {
        this.credentialHandlerProperties.put(propertyName, value);
        return (S) this;
    }

    @Override
    public S addCredentialHandler(Class<? extends CredentialHandler> credentialHandler) {
        this.credentialHandlers.add(credentialHandler);
        return (S) this;
    }

    @Override
    public void validate() {
    }

    @Override
    public Builder<?> readFrom(T configuration) {
        return this;
    }

    protected List<ContextInitializer> getContextInitializers() {
        return this.contextInitializers;
    }

    protected Map<String, Object> getCredentialHandlerProperties() {
        return this.credentialHandlerProperties;
    }

    protected List<Class<? extends CredentialHandler>> getCredentialHandlers() {
        return this.credentialHandlers;
    }

    public Map<Class<? extends AttributedType>, Set<TypeOperation>> getSupportedTypes() {
        return this.supportedTypes;
    }

    public Map<Class<? extends AttributedType>, Set<TypeOperation>> getUnsupportedTypes() {
        return this.unsupportedTypes;
    }

    private static Class<? extends AttributedType>[] getDefaultIdentityModelClasses() {
        List<Class<? extends AttributedType>> classes = new ArrayList<Class<? extends AttributedType>>();

        // identity types
        classes.add(Agent.class);
        classes.add(User.class);
        classes.add(Role.class);
        classes.add(Group.class);

        // relationship types
        classes.add(Grant.class);
        classes.add(GroupMembership.class);
        classes.add(GroupRole.class);

        // partition types
        classes.add(Realm.class);
        classes.add(Tier.class);

        return (Class<? extends AttributedType>[]) classes.toArray(new Class<?>[classes.size()]);
    }

}