package org.picketlink.idm.internal;

import org.picketlink.idm.event.EventBridge;
import org.picketlink.idm.model.Realm;
import org.picketlink.idm.model.Tier;
import org.picketlink.idm.spi.IdentityStoreInvocationContext;
import org.picketlink.idm.spi.IdentityStoreSession;

/**
 * 
 * @author Shane Bryzak
 */
public class DefaultIdentityStoreInvocationContext implements IdentityStoreInvocationContext {

    private IdentityStoreSession session;
    private EventBridge eventBridge;

    public DefaultIdentityStoreInvocationContext(IdentityStoreSession session, EventBridge eventBridge) {
        this.session = session;
        this.eventBridge = eventBridge;
    }

    @Override
    public IdentityStoreSession getIdentityStoreSession() {
        return session;
    }

    @Override
    public EventBridge getEventBridge() {
        return eventBridge;
    }

    @Override
    public Realm getRealm() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Tier getTier() {
        // TODO Auto-generated method stub
        return null;
    }
}
