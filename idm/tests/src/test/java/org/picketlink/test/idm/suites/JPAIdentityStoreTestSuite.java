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

package org.picketlink.test.idm.suites;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.IdentityManagerFactory;
import org.picketlink.idm.config.FeatureSet;
import org.picketlink.idm.config.FileIdentityStoreConfiguration;
import org.picketlink.idm.config.IdentityConfiguration;
import org.picketlink.idm.config.IdentityStoreConfiguration;
import org.picketlink.idm.config.JPAIdentityStoreConfiguration;
import org.picketlink.idm.internal.DefaultIdentityManagerFactory;
import org.picketlink.idm.jpa.internal.JPAContextInitializer;
import org.picketlink.idm.jpa.internal.JPAIdentityStore;
import org.picketlink.idm.jpa.schema.CredentialObject;
import org.picketlink.idm.jpa.schema.CredentialObjectAttribute;
import org.picketlink.idm.jpa.schema.IdentityObject;
import org.picketlink.idm.jpa.schema.IdentityObjectAttribute;
import org.picketlink.idm.jpa.schema.PartitionObject;
import org.picketlink.idm.jpa.schema.RelationshipIdentityObject;
import org.picketlink.idm.jpa.schema.RelationshipObject;
import org.picketlink.idm.jpa.schema.RelationshipObjectAttribute;
import org.picketlink.idm.model.Authorization;
import org.picketlink.idm.model.Realm;
import org.picketlink.test.idm.IdentityManagerRunner;
import org.picketlink.test.idm.TestLifecycle;
import org.picketlink.test.idm.basic.UserManagementTestCase;
import org.picketlink.test.idm.relationship.CustomRelationship;

/**
 * <p>
 * Test suite for the {@link IdentityManager} using a {@link JPAIdentityStore}. For each test is created a fresh
 * {@link IdentityManager} instance. Data is not preserved between tests.
 * </p>
 * 
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 * 
 */
@RunWith(IdentityManagerRunner.class)
@SuiteClasses({ UserManagementTestCase.class })
public class JPAIdentityStoreTestSuite implements TestLifecycle {

    private EntityManagerFactory emf;
    private EntityManager entityManager;

    public static TestLifecycle init() throws Exception {
        return new JPAIdentityStoreTestSuite();
    }

    @Override
    public void onInit() {
        this.emf = Persistence.createEntityManagerFactory("jpa-identity-store-tests-pu");
        this.entityManager = emf.createEntityManager();
        this.entityManager.getTransaction().begin();
    }

    @Override
    public void onDestroy() {
        this.entityManager.getTransaction().commit();
        this.entityManager.close();
        this.emf.close();
    }

    @Override
    public IdentityManagerFactory createIdentityManagerFactory() {
        IdentityConfiguration config = new IdentityConfiguration();
        
        IdentityStoreConfiguration jpaConfig = getDefaultConfiguration();
        
        jpaConfig.addContextInitializer(new JPAContextInitializer(emf) {
            @Override
            public EntityManager getEntityManager() {
                return entityManager;
            }
        });
        
        config.addConfig(jpaConfig);

        return new DefaultIdentityManagerFactory(config);
    }

    private IdentityManager createIdentityManager(IdentityConfiguration config) {
        return new DefaultIdentityManagerFactory(config).createIdentityManager();
    }

    /**
     * <p>
     * Returns a specific {@link FileIdentityStoreConfiguration} for the Realm.DEFAULT_REALM.
     * </p>
     * 
     * @return
     */
    private IdentityStoreConfiguration getDefaultConfiguration() {
        JPAIdentityStoreConfiguration configuration = new JPAIdentityStoreConfiguration();

        configuration.addRealm(Realm.DEFAULT_REALM);
        configuration.addRealm("Testing");

        configuration.setIdentityClass(IdentityObject.class);
        configuration.setAttributeClass(IdentityObjectAttribute.class);
        configuration.setRelationshipClass(RelationshipObject.class);
        configuration.setRelationshipIdentityClass(RelationshipIdentityObject.class);
        configuration.setRelationshipAttributeClass(RelationshipObjectAttribute.class);
        configuration.setCredentialClass(CredentialObject.class);
        configuration.setCredentialAttributeClass(CredentialObjectAttribute.class);
        configuration.setPartitionClass(PartitionObject.class);

        FeatureSet.addFeatureSupport(configuration.getFeatureSet());
        FeatureSet.addRelationshipSupport(configuration.getFeatureSet());
        FeatureSet.addRelationshipSupport(configuration.getFeatureSet(), CustomRelationship.class);
        FeatureSet.addRelationshipSupport(configuration.getFeatureSet(), Authorization.class);
        configuration.getFeatureSet().setSupportsCustomRelationships(true);
        configuration.getFeatureSet().setSupportsMultiRealm(true);

        return configuration;
    }

}
