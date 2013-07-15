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
package org.picketlink.idm.internal;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.picketlink.idm.config.IdentityStoreConfiguration;
import org.picketlink.idm.credential.Credentials;
import org.picketlink.idm.credential.spi.CredentialHandler;
import org.picketlink.idm.credential.spi.annotations.SupportsCredentials;
import org.picketlink.idm.model.Account;
import org.picketlink.idm.spi.IdentityContext;
import org.picketlink.idm.spi.IdentityStore;
import static org.picketlink.idm.IDMMessages.MESSAGES;

/**
 * @author pedroigor
 */
public abstract class AbstractIdentityStore<C extends IdentityStoreConfiguration> implements IdentityStore<C> {

    private C configuration;
    private Map<Class<? extends CredentialHandler>, CredentialHandler> credentialHandlers = new HashMap<Class<? extends CredentialHandler>, CredentialHandler>();

    @Override
    public void setup(C config) {
        this.configuration = config;
        initializeCredentialHandlers();
    }

    @Override
    public C getConfig() {
        return this.configuration;
    }

    @Override
    public void validateCredentials(IdentityContext context, Credentials credentials) {
        Class<? extends CredentialHandler> credentialHandler = getCredentialHandler(credentials);
        this.credentialHandlers.get(credentialHandler).validate(context, credentials, this);
    }

    @Override
    public void updateCredential(IdentityContext context, Account account, Object credential, Date effectiveDate, Date expiryDate) {
        Class<? extends CredentialHandler> credentialHandler = getCredentialHandler(credential);
        this.credentialHandlers.get(credentialHandler).update(context, account, credential, this, effectiveDate, expiryDate);
    }

    private Class<? extends CredentialHandler> getCredentialHandler(Object credentials) {
        Class<? extends CredentialHandler> credentialHandler = null;

        if (credentialHandler == null) {
            for (Class<? extends CredentialHandler> handlerClass : this.credentialHandlers.keySet()) {
                if (handlerClass.isAnnotationPresent(SupportsCredentials.class)) {
                    for (Class<?> cls : handlerClass.getAnnotation(SupportsCredentials.class).value()) {
                        if (cls.isAssignableFrom(credentials.getClass())) {
                            credentialHandler = handlerClass;

                            // if we found a specific handler for the credential, immediately return.
                            if (cls.equals(credentials.getClass())) {
                                return handlerClass;
                            }
                        }
                    }
                }
            }
        }

        if (credentialHandler == null) {
            throw MESSAGES.credentialHandlerNotFoundForCredentialType(credentials.getClass());
        }

        return credentialHandler;
    }

    private void initializeCredentialHandlers() {
        for (Class<? extends CredentialHandler> handlerType : configuration.getCredentialHandlers()) {
            CredentialHandler credentialHandler = null;

            try {
                credentialHandler = handlerType.newInstance();
                credentialHandler.setup(this);
            } catch (Exception e) {
                throw MESSAGES.credentialCredentialHandlerInstantiationError(handlerType, e);
            }

            this.credentialHandlers.put(handlerType, credentialHandler);
        }
    }

}