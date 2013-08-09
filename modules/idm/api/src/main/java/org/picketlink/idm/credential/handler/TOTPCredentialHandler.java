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

package org.picketlink.idm.credential.handler;

import org.picketlink.idm.credential.TOTPCredential;
import org.picketlink.idm.credential.TOTPCredentials;
import org.picketlink.idm.credential.handler.annotations.SupportsCredentials;
import org.picketlink.idm.credential.storage.OTPCredentialStorage;
import org.picketlink.idm.credential.util.CredentialUtils;
import org.picketlink.idm.credential.util.TimeBasedOTP;
import org.picketlink.idm.model.Account;
import org.picketlink.idm.spi.CredentialStore;
import org.picketlink.idm.spi.IdentityContext;

import java.util.Date;
import java.util.List;

import static org.picketlink.common.util.StringUtil.*;
import static org.picketlink.idm.credential.Credentials.*;
import static org.picketlink.idm.credential.util.TimeBasedOTP.*;

/**
 * <p>
 * This particular implementation supports the validation of {@link TOTPCredentials}, and updating {@link TOTPCredential}
 * credentials.
 * </p>
 *
 * @author Shane Bryzak
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 */
@SupportsCredentials({TOTPCredentials.class, TOTPCredential.class})
public class TOTPCredentialHandler extends PasswordCredentialHandler<CredentialStore<?>, TOTPCredentials, TOTPCredential> {

    public static final String ALGORITHM = "ALGORITHM";
    public static final String INTERVAL_SECONDS = "INTERVAL_SECONDS";
    public static final String NUMBER_DIGITS = "NUMBER_DIGITS";
    public static final String DELAY_WINDOW = "DELAY_WINDOW";
    public static final String DEFAULT_DEVICE = "DEFAULT_DEVICE";

    private TimeBasedOTP totp;

    @Override
    public void setup(CredentialStore<?> store) {
        super.setup(store);

        String algorithm = getConfigurationProperty(store, ALGORITHM, DEFAULT_ALGORITHM);
        String intervalSeconds = getConfigurationProperty(store, INTERVAL_SECONDS, "" + DEFAULT_INTERVAL_SECONDS);
        String numberDigits = getConfigurationProperty(store, NUMBER_DIGITS, "" + DEFAULT_NUMBER_DIGITS);
        String delayWindow = getConfigurationProperty(store, DELAY_WINDOW, "" + DEFAULT_DELAY_WINDOW);

        this.totp = new TimeBasedOTP(algorithm, Integer.parseInt(numberDigits), Integer.valueOf(intervalSeconds), Integer.valueOf(delayWindow));
    }

    @Override
    public void validate(final IdentityContext context, final TOTPCredentials credentials, final CredentialStore<?> store) {
        super.validate(context, credentials, store);

        if (Status.VALID.equals(credentials.getStatus())
                || Status.EXPIRED.equals(credentials.getStatus())) {
            String device = getDevice(credentials.getDevice());

            OTPCredentialStorage storage = getOTPCredentialStorage(context, credentials, store, device);

            if (storage != null) {
                String secretKey = storage.getSecretKey();
                String token = credentials.getToken();

                if (!this.totp.validate(token, secretKey.getBytes())) {
                    credentials.setStatus(Status.INVALID);
                    credentials.setValidatedAccount(null);
                }
            }
        }
    }

    private OTPCredentialStorage getOTPCredentialStorage(final IdentityContext context, final TOTPCredentials credentials, final CredentialStore<?> store, final String device) {
        OTPCredentialStorage storage = null;
        List<OTPCredentialStorage> storedCredentials =
                store.retrieveCredentials(context, getAccount(context, credentials.getUsername()), OTPCredentialStorage.class);

        for (OTPCredentialStorage storedCredential : storedCredentials) {
            if (storedCredential.getDevice().equals(device)
                    && CredentialUtils.isCurrentCredential(storedCredential)) {
                if (storage == null || storage.getEffectiveDate().compareTo(storedCredential.getEffectiveDate()) <= 0) {
                    storage = storedCredential;
                }
            }
        }

        return storage;
    }

    @Override
    public void update(IdentityContext context, Account account, TOTPCredential credential, CredentialStore<?> store, Date effectiveDate, Date expiryDate) {
        // if a credential was not provided, updates only the secret.
        if (credential.getValue() != null && credential.getValue().length > 0) {
            super.update(context, account, credential, store, effectiveDate, expiryDate);
        }

        OTPCredentialStorage storage = new OTPCredentialStorage();

        if (effectiveDate != null) {
            storage.setEffectiveDate(effectiveDate);
        }

        storage.setExpiryDate(expiryDate);

        storage.setSecretKey(credential.getSecret());
        storage.setDevice(getDevice(credential.getDevice()));

        store.storeCredential(context, account, storage);
    }

    private String getDevice(String device) {
        if (isNullOrEmpty(device)) {
            device = DEFAULT_DEVICE;
        }

        return device;
    }

    private String getConfigurationProperty(CredentialStore<?> store, String key, String defaultValue) {
        Object value = store.getConfig().getCredentialHandlerProperties().get(key);

        if (value != null) {
            return String.valueOf(value);
        }

        return defaultValue;
    }

}