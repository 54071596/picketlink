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
package org.picketlink.config;

import org.picketlink.idm.config.Builder;
import org.picketlink.idm.config.SecurityConfigurationException;

/**
 * <p>A configuration builder with covenience methods to configure the behavior of the {@link org.picketlink.Identity} bean.</p>
 *
 * @author Pedro Igor
 */
public class IdentityBeanConfigurationBuilder extends AbstractSecurityConfigurationBuilder<IdentityBeanConfiguration> {

    private boolean stateless;

    public IdentityBeanConfigurationBuilder(SecurityConfigurationBuilder builder) {
        super(builder);
    }

    /**
     * <p>Enables the stateless mode of the {@link org.picketlink.Identity} bean.</p>
     *
     * <p>Default is false.</p>
     *
     * @return
     */
    public IdentityBeanConfigurationBuilder stateless() {
        this.stateless = true;
        return this;
    }

    @Override
    protected IdentityBeanConfiguration create() throws SecurityConfigurationException {
        return new IdentityBeanConfiguration(this.stateless);
    }

    @Override
    protected void validate() throws SecurityConfigurationException {

    }

    @Override
    protected Builder<IdentityBeanConfiguration> readFrom(IdentityBeanConfiguration fromConfiguration) throws SecurityConfigurationException {
        if (fromConfiguration.isStateless()) {
            this.stateless();
        }

        return this;
    }
}
