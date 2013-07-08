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

import org.junit.runner.RunWith;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.PartitionManager;
import org.picketlink.idm.config.IdentityConfigurationBuilder;
import org.picketlink.idm.internal.DefaultPartitionManager;
import org.picketlink.idm.model.sample.Realm;
import org.picketlink.idm.model.sample.Tier;
import org.picketlink.test.idm.IdentityManagerRunner;
import org.picketlink.test.idm.TestLifecycle;
import org.picketlink.test.idm.basic.AgentManagementTestCase;
import org.picketlink.test.idm.basic.GroupManagementTestCase;
import org.picketlink.test.idm.basic.RoleManagementTestCase;
import org.picketlink.test.idm.basic.UserManagementTestCase;
import org.picketlink.test.idm.credential.CertificateCredentialTestCase;
import org.picketlink.test.idm.credential.DigestCredentialTestCase;
import org.picketlink.test.idm.credential.PasswordCredentialTestCase;
import org.picketlink.test.idm.credential.TOTPCredentialTestCase;
import org.picketlink.test.idm.partition.CustomPartitionTestCase;
import org.picketlink.test.idm.partition.RealmManagementTestCase;
import org.picketlink.test.idm.partition.TierManagementTestCase;
import org.picketlink.test.idm.query.AgentQueryTestCase;
import org.picketlink.test.idm.query.GroupQueryTestCase;
import org.picketlink.test.idm.query.IdentityTypeQueryTestCase;
import org.picketlink.test.idm.query.RelationshipQueryTestCase;
import org.picketlink.test.idm.query.RoleQueryTestCase;
import org.picketlink.test.idm.query.UserQueryTestCase;
import org.picketlink.test.idm.relationship.AgentGrantRelationshipTestCase;
import org.picketlink.test.idm.relationship.AgentGroupRoleRelationshipTestCase;
import org.picketlink.test.idm.relationship.AgentGroupsRelationshipTestCase;
import org.picketlink.test.idm.relationship.CustomRelationshipTestCase;
import org.picketlink.test.idm.relationship.GroupGrantRelationshipTestCase;
import org.picketlink.test.idm.relationship.GroupMembershipTestCase;
import org.picketlink.test.idm.relationship.UserGrantRelationshipTestCase;
import org.picketlink.test.idm.relationship.UserGroupRoleRelationshipTestCase;
import org.picketlink.test.idm.usecases.ApplicationUserRelationshipTestCase;
import static org.junit.runners.Suite.SuiteClasses;

/**
 * <p>
 * Test suite for the {@link IdentityManager} using a {@link org.picketlink.idm.file.internal.FileIdentityStore}.
 * </p>
 * 
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 * 
 */
@RunWith(IdentityManagerRunner.class)
@SuiteClasses ({
        RealmManagementTestCase.class, TierManagementTestCase.class, CustomPartitionTestCase.class,
        AgentManagementTestCase.class, UserManagementTestCase.class, RoleManagementTestCase.class, GroupManagementTestCase.class,
        UserGrantRelationshipTestCase.class, AgentGrantRelationshipTestCase.class, GroupGrantRelationshipTestCase.class,
        GroupMembershipTestCase.class, AgentGroupsRelationshipTestCase.class,
        AgentGroupRoleRelationshipTestCase.class, UserGroupRoleRelationshipTestCase.class, ApplicationUserRelationshipTestCase.class,
        CustomRelationshipTestCase.class,
        AgentQueryTestCase.class, GroupQueryTestCase.class, IdentityTypeQueryTestCase.class, RelationshipQueryTestCase.class,
        RoleQueryTestCase.class, UserQueryTestCase.class,
        PasswordCredentialTestCase.class, DigestCredentialTestCase.class, CertificateCredentialTestCase.class, TOTPCredentialTestCase.class

})
public class FileIdentityStoreTestSuite implements TestLifecycle {

    private static FileIdentityStoreTestSuite instance;

    public static TestLifecycle init() throws Exception {
        if (instance == null) {
            instance = new FileIdentityStoreTestSuite();
        }

        return instance;
    }

    @SuppressWarnings("unchecked")
    @Override
    public PartitionManager createPartitionManager() {
        IdentityConfigurationBuilder builder = new IdentityConfigurationBuilder();
        
        builder
            .named("default")
                .stores()
                    .file()
                        .preserveState(false)
                        .supportAllFeatures();

        PartitionManager partitionManager = new DefaultPartitionManager(builder.build());

        Realm defaultRealm = new Realm(Realm.DEFAULT_REALM);

        if (partitionManager.getPartition(defaultRealm.getClass(), defaultRealm.getName()) != null) {
            partitionManager.remove(defaultRealm);
        }

        partitionManager.add(defaultRealm);

        Realm testingRealm = new Realm("Testing");

        if (partitionManager.getPartition(testingRealm.getClass(), testingRealm.getName()) != null) {
            partitionManager.remove(testingRealm);
        }

        partitionManager.add(testingRealm);

        Tier tier = new Tier("Application A");

        if (partitionManager.getPartition(tier.getClass(), tier.getName()) != null) {
            partitionManager.remove(tier);
        }

        partitionManager.add(tier);

        tier = new Tier("Application B");

        if (partitionManager.getPartition(tier.getClass(), tier.getName()) != null) {
            partitionManager.remove(tier);
        }

        partitionManager.add(tier);

        return partitionManager;
    }

    @Override
    public void onInit() {

    }

    @Override
    public void onDestroy() {
    }

}
