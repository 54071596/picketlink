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

package org.picketlink.test.idm.config;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.PartitionManager;
import org.picketlink.idm.config.AbstractIdentityStoreConfiguration;
import org.picketlink.idm.config.AbstractIdentityStoreConfigurationBuilder;
import org.picketlink.idm.config.IdentityConfiguration;
import org.picketlink.idm.config.IdentityConfigurationBuilder;
import org.picketlink.idm.config.IdentityStoresConfigurationBuilder;
import org.picketlink.idm.credential.Credentials;
import org.picketlink.idm.credential.spi.CredentialHandler;
import org.picketlink.idm.credential.spi.CredentialStorage;
import org.picketlink.idm.model.Account;
import org.picketlink.idm.model.Attribute;
import org.picketlink.idm.model.AttributedType;
import org.picketlink.idm.model.IdentityType;
import org.picketlink.idm.model.Relationship;
import org.picketlink.idm.model.sample.Agent;
import org.picketlink.idm.model.sample.Group;
import org.picketlink.idm.model.sample.Role;
import org.picketlink.idm.model.sample.User;
import org.picketlink.idm.query.IdentityQuery;
import org.picketlink.idm.query.RelationshipQuery;
import org.picketlink.idm.spi.ContextInitializer;
import org.picketlink.idm.spi.CredentialStore;
import org.picketlink.idm.spi.IdentityContext;
import org.picketlink.idm.spi.IdentityStore;
import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 *
 * @author Pedro Igor
 * 
 */
public class CustomIdentityStoreTestCase {

    @Test
    public void testConfiguration() throws Exception {
        IdentityConfigurationBuilder builder = new IdentityConfigurationBuilder();

        // let's use this instance to test the custom store configuration and check for the methods invocation
        MethodInvocationContext methodInvocationContext = new MethodInvocationContext();

        builder
            .named("default")
                .stores()
                    .add(MyIdentityStoreConfiguration.class,
                        MyIdentityStoreConfigurationBuilder.class)
                    .methodInvocationContext(methodInvocationContext)
                    .supportAllFeatures();

        IdentityConfiguration configuration = builder.build();
        PartitionManager partitionManager = null;
        fail("Create PartitionManager");
        
        IdentityManager identityManager = partitionManager.createIdentityManager();
        
        identityManager.add(new User("john"));

        assertEquals("addAttributedType", methodInvocationContext.getMethodName());

        identityManager.getUser("john");

        assertEquals("getUser", methodInvocationContext.getMethodName());
    }

    public static class MyIdentityStoreConfigurationBuilder extends
            AbstractIdentityStoreConfigurationBuilder<MyIdentityStoreConfiguration, MyIdentityStoreConfigurationBuilder> {

        private MethodInvocationContext methodInvocationContext;

        public MyIdentityStoreConfigurationBuilder(IdentityStoresConfigurationBuilder builder) {
            super(builder);
        }

        @Override
        public MyIdentityStoreConfiguration create() {
            MyIdentityStoreConfiguration config = new MyIdentityStoreConfiguration(getSupportedTypes(),
                    getUnsupportedTypes(),
                    getContextInitializers(),
                    getCredentialHandlerProperties(),
                    getCredentialHandlers());

            config.setMethodInvocationContext(this.methodInvocationContext);

            return config;
        }

        public MyIdentityStoreConfigurationBuilder methodInvocationContext(MethodInvocationContext methodInvocationContext) {
            this.methodInvocationContext = methodInvocationContext;
            return this;
        }
    }

    public static class MyIdentityStoreConfiguration extends AbstractIdentityStoreConfiguration {

        private MethodInvocationContext methodInvocationContext;

        protected MyIdentityStoreConfiguration(Map<Class<? extends AttributedType>, Set<IdentityOperation>> supportedTypes, Map<Class<? extends AttributedType>, Set<IdentityOperation>> unsupportedTypes, List<ContextInitializer> contextInitializers, Map<String, Object> credentialHandlerProperties, List<Class<? extends CredentialHandler>> credentialHandlers) {
            super(supportedTypes, unsupportedTypes, contextInitializers, credentialHandlerProperties, credentialHandlers);
        }

        @Override
        protected void initConfig() {

        }

        @Override
        public Class<? extends IdentityStore> getIdentityStoreType() {
            return MyIdentityStore.class;
        }

        public void setMethodInvocationContext(MethodInvocationContext assertion) {
            this.methodInvocationContext = assertion;
        }

        public MethodInvocationContext getMethodInvocationContext() {
            return this.methodInvocationContext;
        }
    }

    public static class MyIdentityStore implements CredentialStore<MyIdentityStoreConfiguration> {

        private MyIdentityStoreConfiguration config;

        @Override
        public void setup(MyIdentityStoreConfiguration config) {
            this.config = config;
        }

        @Override
        public MyIdentityStoreConfiguration getConfig() {
            return this.config;
        }

        @Override
        public <I extends IdentityType> I getIdentity(Class<I> identityType, String id) {
            return null;
        }

        @Override
        public void add(IdentityContext context, AttributedType value) {
            value.setId(context.getIdGenerator().generate());
            getConfig().getMethodInvocationContext().setMethodName("addAttributedType");
        }

        @Override
        public void update(IdentityContext context, AttributedType value) {

        }

        @Override
        public void remove(IdentityContext context, AttributedType value) {

        }

        @Override
        public Agent getAgent(IdentityContext context, String loginName) {
            return null;
        }

        @Override
        public User getUser(IdentityContext context, String loginName) {
            getConfig().getMethodInvocationContext().setMethodName("getUser");
            return null;
        }

        @Override
        public Group getGroup(IdentityContext context, String groupPath) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Group getGroup(IdentityContext context, String name, Group parent) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Role getRole(IdentityContext context, String name) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public <V extends IdentityType> List<V> fetchQueryResults(IdentityContext context, IdentityQuery<V> identityQuery) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public <V extends IdentityType> int countQueryResults(IdentityContext context, IdentityQuery<V> identityQuery) {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public <V extends Relationship> List<V> fetchQueryResults(IdentityContext context, RelationshipQuery<V> query) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public <V extends Relationship> int countQueryResults(IdentityContext context, RelationshipQuery<V> query) {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public void setAttribute(IdentityContext context, AttributedType attributedType, Attribute<? extends Serializable> attribute) {
            // TODO Auto-generated method stub

        }

        @Override
        public <V extends Serializable> Attribute<V> getAttribute(IdentityContext context, AttributedType attributedType,
                String attributeName) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void removeAttribute(IdentityContext context, AttributedType attributedType, String attributeName) {
            // TODO Auto-generated method stub

        }

        @Override
        public void validateCredentials(IdentityContext context, Credentials credentials) {
            // TODO Auto-generated method stub

        }

        @Override
        public void updateCredential(IdentityContext context, Account agent, Object credential, Date effectiveDate,
                Date expiryDate) {
            // TODO Auto-generated method stub

        }

        @Override
        public void storeCredential(IdentityContext context, Account agent, CredentialStorage storage) {
            // TODO Auto-generated method stub

        }

        @Override
        public <T extends CredentialStorage> T retrieveCurrentCredential(IdentityContext context, Account account,
                Class<T> storageClass) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public <T extends CredentialStorage> List<T> retrieveCredentials(IdentityContext context, Account agent,
                Class<T> storageClass) {
            // TODO Auto-generated method stub
            return null;
        }

    }

    public static class MethodInvocationContext {

        private String methodName;

        public void setMethodName(String methodName) {
            this.methodName = methodName;
        }

        public String getMethodName() {
            return this.methodName;
        }
    }
}
