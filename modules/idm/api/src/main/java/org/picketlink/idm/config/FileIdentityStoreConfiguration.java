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

package org.picketlink.idm.config;

import org.picketlink.idm.credential.spi.CredentialHandler;
import org.picketlink.idm.model.AttributedType;
import org.picketlink.idm.spi.ContextInitializer;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 *
 */
public class FileIdentityStoreConfiguration extends AbstractIdentityStoreConfiguration {

    private int asyncThreadPool = 5;
    private boolean asyncWrite = false;
    private boolean alwaysCreateFiles = true;
    private String workingDir;

    FileIdentityStoreConfiguration(
            String workingDir,
            boolean preserveState,
            boolean asyncWrite,
            int asyncWriteThreadPool,
            Map<Class<? extends AttributedType>, Set<TypeOperation>> supportedTypes,
            Map<Class<? extends AttributedType>, Set<TypeOperation>> unsupportedTypes,
            List<ContextInitializer> contextInitializers,
            Map<String, Object> credentialHandlerProperties,
            List<Class<? extends CredentialHandler>> credentialHandlers) {
        super(supportedTypes, unsupportedTypes, contextInitializers, credentialHandlerProperties, credentialHandlers);
        this.workingDir = workingDir;
        this.alwaysCreateFiles = !preserveState;
        this.asyncWrite = asyncWrite;
        this.asyncThreadPool = asyncWriteThreadPool;
    }

    @Override
    protected void initConfig() throws SecurityConfigurationException {
    }

    public String getWorkingDir() {
        return this.workingDir;
    }

    public boolean isAlwaysCreateFiles() {
        return this.alwaysCreateFiles;
    }

    public boolean isAsyncWrite() {
        return this.asyncWrite;
    }

    public int getAsyncThreadPool() {
        return this.asyncThreadPool;
    }

}