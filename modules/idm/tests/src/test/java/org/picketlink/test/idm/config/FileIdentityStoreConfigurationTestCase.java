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

import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.IdentityManagerFactory;
import org.picketlink.idm.config.IdentityConfiguration;
import org.picketlink.idm.config.JPAIdentityStoreConfiguration;
import org.picketlink.idm.config.builder.FileStoreConfigurationBuilder;
import org.picketlink.idm.config.builder.IdentityConfigurationBuilder;
import org.picketlink.idm.internal.DefaultIdentityManagerFactory;

/**
 * <p>
 * Test case for the {@link JPAIdentityStoreConfiguration}.
 * </p>
 * 
 * @author Pedro Silva
 * 
 */
public class FileIdentityStoreConfigurationTestCase extends
        AbstractFeaturesSetConfigurationTestCase<FileStoreConfigurationBuilder> {

    @Override
    protected FileStoreConfigurationBuilder createMinimalConfiguration(IdentityConfigurationBuilder builder) {
        FileStoreConfigurationBuilder fileConfig = builder.stores().file();
        
        fileConfig
<<<<<<< HEAD
            .preserveState(false)
=======
>>>>>>> 5268ffdd8cddc071aeac1f26787ca3f20366d717
            .supportAllFeatures();
        
        return fileConfig;
    }

    @Override
    protected IdentityManager createIdentityManager(IdentityConfiguration config) {
        IdentityManagerFactory factory = new DefaultIdentityManagerFactory(config);
        return factory.createIdentityManager();
    }

}
