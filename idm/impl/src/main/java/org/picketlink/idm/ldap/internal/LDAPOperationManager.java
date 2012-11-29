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

package org.picketlink.idm.ldap.internal;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

/**
 * <p>This class provides a set of operations to manage LDAP trees.</p>
 * 
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 *
 */
public class LDAPOperationManager {

    private DirContext ctx;

    public LDAPOperationManager(DirContext context) {
        this.ctx = context;
    }
    
    /**
     * <p>
     * Binds a {@link Object} to the LDAP tree.
     * </p>
     * 
     * @param ldapUser
     */
    public void bind(String dn, Object object) {
        try {
            ctx.bind(dn, object);
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * <p>
     * Modifies the given {@link Attribute} instance using the given DN. This method performs a REPLACE_ATTRIBUTE operation.
     * </p>
     * 
     * @param dn
     * @param attribute
     */
    public void modifyAttribute(String dn, Attribute attribute) {
        try {
            ModificationItem[] mods = new ModificationItem[] { new ModificationItem(DirContext.REPLACE_ATTRIBUTE, attribute) };
            ctx.modifyAttributes(dn, mods);
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * <p>
     * Removes the given {@link Attribute} instance using the given DN. This method performs a REMOVE_ATTRIBUTE operation.
     * </p>
     * 
     * @param dn
     * @param attribute
     */
    public void removeAttribute(String dn, Attribute attribute) {
        try {
            ModificationItem[] mods = new ModificationItem[] { new ModificationItem(DirContext.REMOVE_ATTRIBUTE, attribute) };
            ctx.modifyAttributes(dn, mods);
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * <p>
     * Adds the given {@link Attribute} instance using the given DN. This method performs a ADD_ATTRIBUTE operation.
     * </p>
     * 
     * @param dn
     * @param attribute
     */
    public void addAttribute(String dn, Attribute attribute) {
        try {
            ModificationItem[] mods = new ModificationItem[] { new ModificationItem(DirContext.ADD_ATTRIBUTE, attribute) };
            ctx.modifyAttributes(dn, mods);
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * <p>
     * Re-binds a {@link Object} to the LDAP tree.
     * </p>
     * 
     * @param dn
     * @param object
     */
    public void rebind(String dn, Object object) {
        try {
            ctx.rebind(dn, object);
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * <p>
     * Looks up a entry on the LDAP tree with the given DN.
     * </p>
     * 
     * @param dn
     * @return
     * @throws NamingException
     */
    @SuppressWarnings("unchecked")
    public <T> T lookup(String dn) {
        try {
            return (T) ctx.lookup(dn);
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * <p>Searches the LDAP tree.</p>
     * 
     * @param baseDN
     * @param attributesToSearch
     * @return
     */
    public NamingEnumeration<SearchResult> search(String baseDN, Attributes attributesToSearch) {
        try {
            return ctx.search(baseDN, attributesToSearch);
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * <p>Searches the LDAP tree.</p>
     * 
     * @param baseDN
     * @param attributesToSearch
     * @return
     */
    public NamingEnumeration<SearchResult> search(String baseDN, Attributes attributesToSearch, String[] attributesToReturn) {
        try {
            return ctx.search(baseDN, attributesToSearch, attributesToReturn);
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * <p>Searches the LDAP tree.</p>
     * 
     * @param baseDN
     * @param filter
     * @param attributesToReturn
     * @param searchControls
     * @return
     */
    public NamingEnumeration<SearchResult> search(String baseDN, String filter, String[] attributesToReturn, SearchControls searchControls) {
        try {
            return this.ctx.search(baseDN, filter, attributesToReturn, searchControls);
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * <p>
     * Destroys a subcontext with the given DN from the LDAP tree.
     * </p>
     * 
     * @param dn
     */
    public void destroySubcontext(String dn) {
        try {
            ctx.destroySubcontext(dn);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * <p>Ask the ldap server for the schema for the attribute.</p>
     * 
     * @param attributeName
     * @return
     */
    public boolean checkAttributePresence(String attributeName) {
        try {
            DirContext schema = ctx.getSchema("");

            DirContext cnSchema = (DirContext) schema.lookup("AttributeDefinition/" + attributeName);
            if (cnSchema != null) {
                return true;
            }
        } catch (Exception e) {
            return false; // Probably an unmanaged attribute
        }

        return false;
    }

    public boolean authenticate(String dn, String password) {
        try {
            ctx.addToEnvironment(Context.SECURITY_PRINCIPAL, dn);
            ctx.addToEnvironment(Context.SECURITY_CREDENTIALS, password);
            lookup(dn);
        } catch (Exception e) {
            return false;
        }
        
        return true;
    }

}
