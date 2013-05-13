package org.picketlink.idm.internal;

import static org.picketlink.idm.IDMLogger.LOGGER;
import static org.picketlink.idm.IDMMessages.MESSAGES;

import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.config.IdentityConfiguration;
import org.picketlink.idm.model.Partition;
import org.picketlink.idm.model.Realm;
import org.picketlink.idm.model.Tier;
import org.picketlink.idm.spi.SecurityContext;
import org.picketlink.idm.spi.SecurityContextFactory;
import org.picketlink.idm.spi.StoreFactory;

/**
 * Default implementation for IdentityManagerFactory
 *
 * @author Shane Bryzak
 *
 */
public class IdentityManagerFactory {

    private SecurityContextFactory contextFactory;
    private StoreFactory storeFactory;

    public IdentityManagerFactory(IdentityConfiguration identityConfig) {
        if (identityConfig == null) {
            throw MESSAGES.nullArgument("IdentityConfiguration");
        }

        if (contextFactory == null) {
            this.contextFactory = new DefaultSecurityContextFactory();
        } else {
            this.contextFactory = identityConfig.getSecurityContextFactory();
        }

        if (storeFactory == null) {
            this.storeFactory = new DefaultStoreFactory(identityConfig);
        } else {
            this.storeFactory = identityConfig.getStoreFactory();
        }

        LOGGER.identityManagerBootstrapping();
    }

    public void setIdentityStoreFactory(StoreFactory factory) {
        this.storeFactory = factory;
    }

    public void setContextFactory(SecurityContextFactory contextFactory) {
        this.contextFactory = contextFactory;
    }


    public IdentityManager createIdentityManager() {
        Realm defaultRealm = getRealm(Realm.DEFAULT_REALM);

        if (defaultRealm == null) {
            throw MESSAGES.configurationDefaultRealmNotDefined();
        }

        return createIdentityManager(defaultRealm);
    }


    public IdentityManager createIdentityManager(Partition partition) {
        if (partition == null) {
            throw MESSAGES.nullArgument("Partition");
        }

        SecurityContext context = contextFactory.createContext(partition);
        return new DefaultIdentityManager(context, storeFactory);
    }


    public Realm getRealm(String id) {
        return storeFactory.getRealm(id);
    }


    public Tier getTier(String id) {
        return storeFactory.getTier(id);
    }

}
