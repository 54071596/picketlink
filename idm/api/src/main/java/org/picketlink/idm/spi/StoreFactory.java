package org.picketlink.idm.spi;

import org.picketlink.idm.config.IdentityStoreConfiguration;
import org.picketlink.idm.config.PartitionStoreConfiguration;

/**
 * Creates IdentityStore instances based on a provided configuration
 * 
 * @author Shane Bryzak
 *
 */
public interface StoreFactory {
    /**
     * Creates an instance of an IdentityStore using the provided configuration
     * 
     * @param config
     * @return
     */
    IdentityStore createIdentityStore(IdentityStoreConfiguration config, IdentityStoreInvocationContext context);

    PartitionStore createPartitionStore(PartitionStoreConfiguration config);

    /**
     * Maps specific implementations of StoreConfiguration to a corresponding
     * IdentityStore implementation.
     * 
     * @param configClass
     * @param storeClass
     */
    void mapIdentityConfiguration(Class<? extends IdentityStoreConfiguration> configClass, 
            Class<? extends IdentityStore> storeClass);
    
    void mapPartitionConfiguration(Class<? extends PartitionStoreConfiguration> configClass,
            Class<? extends PartitionStore> storeClass);
}
