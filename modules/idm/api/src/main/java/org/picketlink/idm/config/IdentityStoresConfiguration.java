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

import java.util.Collections;
import org.picketlink.idm.spi.IdentityStore;
import org.picketlink.idm.spi.StoreFactory;

import java.util.List;
import java.util.Map;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;

/**
 * <p>Holds all the configuration for identity stores.</p>
 *
 * @author Pedro Igor
 *
 */
public class IdentityStoresConfiguration {

    private final List<IdentityStoreConfiguration> configurations;
    private final StoreFactory storeFactory;
    private Map<Class<? extends IdentityStoreConfiguration>, Class<? extends IdentityStore>> identityStores;

    public IdentityStoresConfiguration(List<IdentityStoreConfiguration> configurations, StoreFactory storeFactory) {
        this.configurations = unmodifiableList(configurations);
        this.storeFactory = storeFactory;
    }

    public IdentityStoresConfiguration(List<IdentityStoreConfiguration> configurations, StoreFactory storeFactory,
            Map<Class<? extends IdentityStoreConfiguration>, Class<? extends IdentityStore>> identityStores) {
        this(configurations, storeFactory);
        this.identityStores = unmodifiableMap(identityStores);
    }

    public List<IdentityStoreConfiguration> getConfigurations() {
        return this.configurations;
    }

    public StoreFactory getStoreFactory() {
        return this.storeFactory;
    }

    public Map<Class<? extends IdentityStoreConfiguration>, Class<? extends IdentityStore>> getIdentityStores() {
        return this.identityStores;
    }
}