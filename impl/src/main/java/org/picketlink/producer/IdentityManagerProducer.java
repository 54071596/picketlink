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

package org.picketlink.producer;

import java.util.Arrays;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.event.Event;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import org.picketlink.IdentityConfigurationEvent;
import org.picketlink.annotations.PicketLink;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.PartitionManager;
import org.picketlink.idm.RelationshipManager;
import org.picketlink.idm.config.IdentityConfiguration;
import org.picketlink.idm.config.IdentityConfigurationBuilder;
import org.picketlink.idm.config.SecurityConfigurationException;
import org.picketlink.idm.internal.DefaultPartitionManager;
import org.picketlink.idm.model.Partition;
import org.picketlink.idm.model.Relationship;
import org.picketlink.idm.model.sample.Realm;
import org.picketlink.internal.EEJPAContextInitializer;
import org.picketlink.internal.EESecurityContextFactory;
import org.picketlink.internal.IdentityStoreAutoConfiguration;
import org.picketlink.internal.SecuredIdentityManager;

/**
 * 
 * @author Shane Bryzak
 */
@ApplicationScoped
public class IdentityManagerProducer {

    private static final String DEFAULT_CONFIGURATION_NAME = "default";

    @Inject
    Instance<IdentityConfiguration> identityConfigInstance;

    @Inject
    Event<IdentityConfigurationEvent> identityConfigEvent;

    @Inject
    EESecurityContextFactory icf;

    @Inject
    EEJPAContextInitializer jpaContextInitializer;

    @Inject
    IdentityStoreAutoConfiguration autoConfig;

    @Inject @PicketLink Instance<Partition> defaultPartition;

    private PartitionManager partitionManager;

    @Inject
    public void init() {
        IdentityConfigurationBuilder builder;

        if (!identityConfigInstance.isUnsatisfied()) {
            IdentityConfiguration identityConfiguration = identityConfigInstance.get();
            builder = new IdentityConfigurationBuilder(Arrays.asList(identityConfiguration));
        } else if (identityConfigInstance.isAmbiguous()) {
            throw new SecurityConfigurationException("Multiple IdentityConfiguration beans found, can not "
                    + "configure IdentityManagerFactory");
        } else {
            builder = new IdentityConfigurationBuilder();
        }

        this.identityConfigEvent.fire(new IdentityConfigurationEvent(builder));

        if (!builder.isConfigured()) {
            loadAutoConfig(builder);

            this.partitionManager = new DefaultPartitionManager(builder.build());
        } else {
            this.partitionManager = new DefaultPartitionManager(builder.build());
        }

        this.partitionManager.add(new Realm(Realm.DEFAULT_REALM), DEFAULT_CONFIGURATION_NAME);
    }

    private void loadAutoConfig(IdentityConfigurationBuilder builder) {
        if (this.autoConfig.isConfigured()) {
            Class<?>[] entities = new Class[this.autoConfig.getEntities().size()];
            this.autoConfig.getEntities().toArray(entities);
            builder.named(DEFAULT_CONFIGURATION_NAME)
                .stores()
                    .jpa()
                        .mappedEntity(entities)
                        .addContextInitializer(this.jpaContextInitializer)
                        .supportGlobalRelationship(Relationship.class)
                        .supportAllFeatures();
        } else {
            builder.named(DEFAULT_CONFIGURATION_NAME)
                .stores()
                    .file()
                        .supportGlobalRelationship(Relationship.class)
                        .supportAllFeatures();
        }
    }

    @Produces
    public PartitionManager createIdentityManagerFactory() {
        return partitionManager;
    }

    @Produces
    @Dependent
    public IdentityManager createIdentityManager() {
        if (defaultPartition.isUnsatisfied() || defaultPartition.get() == null) {
            return new SecuredIdentityManager(this.partitionManager.createIdentityManager());
        } else {
            return new SecuredIdentityManager(this.partitionManager.createIdentityManager(defaultPartition.get()));
        }
    }

    @Produces
    @Dependent
    public RelationshipManager createRelationshipManager() {
        return this.partitionManager.createRelationshipManager();
    }

}
