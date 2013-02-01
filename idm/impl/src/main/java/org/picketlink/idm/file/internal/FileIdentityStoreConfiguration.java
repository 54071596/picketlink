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

package org.picketlink.idm.file.internal;

import org.picketlink.idm.SecurityConfigurationException;
import org.picketlink.idm.config.IdentityStoreConfiguration;
import org.picketlink.idm.spi.IdentityStore;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 * 
 */
public class FileIdentityStoreConfiguration extends IdentityStoreConfiguration {

    /**
     * <p>
     * Defines the feature set for this {@link IdentityStore}.
     * </p>
     */
    private FeatureSet featureSet = new FeatureSet();
    
    private FileDataSource dataSource = new FileDataSource();

    @Override
    public void init() throws SecurityConfigurationException {
        configureFeatureSet();
        this.dataSource.init();
    }
    
    /**
     * <p>
     * Configures the {@link Feature} set supported by this store.
     * </p>
     */
    private void configureFeatureSet() {
        this.featureSet.addSupportedFeature(Feature.all);
    }

    @Override
    public FeatureSet getFeatureSet() {
        return this.featureSet;
    }

    public void setDataSource(FileDataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    public FileDataSource getDataSource() {
        return this.dataSource;
    }

}
