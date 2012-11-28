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

package org.picketlink.test.idm;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.model.Attribute;
import org.picketlink.idm.model.SimpleUser;
import org.picketlink.idm.model.User;

/**
 * <p>
 * Test case for {@link User} basic management operations.
 * </p>
 * 
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 * 
 */
public class UserManagementTestCase {

    private IdentityManager identityManager;

    /**
     * <p>
     * Creates a new {@link User} instance using the API. This method also checks if the user was properly created by retrieving
     * his information from the store.
     * </p>
     * 
     * @throws Exception
     */
    @Test
    public void testCreate() throws Exception {
        IdentityManager identityManager = getIdentityManager();

        User newUserInstance = new SimpleUser("jduke");

        newUserInstance.setEmail("jduke@jboss.org");
        newUserInstance.setFirstName("Java");
        newUserInstance.setLastName("Duke");

        // let's create the new user
        identityManager.createUser(newUserInstance);

        // let's retrieve the user information and see if they are properly stored
        User storedUserInstance = identityManager.getUser(newUserInstance.getId());

        assertNotNull(storedUserInstance);

        assertEquals(newUserInstance.getId(), storedUserInstance.getId());
        assertEquals(newUserInstance.getFirstName(), storedUserInstance.getFirstName());
        assertEquals(newUserInstance.getLastName(), storedUserInstance.getLastName());
        assertEquals(newUserInstance.getEmail(), storedUserInstance.getEmail());
    }

    /**
     * <p>
     * Loads from the LDAP tree an already stored user.
     * </p>
     * 
     * @throws Exception
     */
    @Test
    public void testGet() throws Exception {
        IdentityManager identityManager = getIdentityManager();

        User storedUserInstance = identityManager.getUser("admin");

        assertNotNull(storedUserInstance);

        assertEquals("admin", storedUserInstance.getId());
        assertEquals("The", storedUserInstance.getFirstName());
        assertEquals("Administrator", storedUserInstance.getLastName());
        assertEquals("admin@jboss.org", storedUserInstance.getEmail());
    }

    /**
     * <p>
     * Updates the stored user information.
     * </p>
     * 
     * @throws Exception
     */
    @Test
    public void testUpdate() throws Exception {
        IdentityManager identityManager = getIdentityManager();

        User storedUserInstance = identityManager.getUser("admin");

        assertNotNull(storedUserInstance);

        assertEquals("admin", storedUserInstance.getId());
        assertEquals("The", storedUserInstance.getFirstName());
        assertEquals("Administrator", storedUserInstance.getLastName());
        assertEquals("admin@jboss.org", storedUserInstance.getEmail());

        // let's update some user information
        storedUserInstance.setFirstName("Updated " + storedUserInstance.getFirstName());
        storedUserInstance.setLastName("Updated " + storedUserInstance.getLastName());
        storedUserInstance.setEmail("Updated " + storedUserInstance.getEmail());

        identityManager.updateUser(storedUserInstance);

        // let's load again the user from the store and check for the updated information
        User updatedUser = identityManager.getUser(storedUserInstance.getId());

        assertEquals("Updated The", updatedUser.getFirstName());
        assertEquals("Updated Administrator", updatedUser.getLastName());
        assertEquals("Updated admin@jboss.org", updatedUser.getEmail());

    }

    /**
     * <p>
     * Remove from the LDAP tree an already stored user.
     * </p>
     * 
     * @throws Exception
     */
    @Test
    public void testRemove() throws Exception {
        IdentityManager identityManager = getIdentityManager();

        User storedUserInstance = identityManager.getUser("admin");

        assertNotNull(storedUserInstance);

        identityManager.removeUser(storedUserInstance);

        User removedUserInstance = identityManager.getUser("admin");

        assertNull(removedUserInstance);
    }

    /**
     * <p>
     * Sets an one-valued attribute.
     * </p>
     * 
     * @throws Exception
     */
    @Test
    public void testSetOneValuedAttribute() throws Exception {
        IdentityManager identityManager = getIdentityManager();

        User storedUserInstance = identityManager.getUser("admin");

        storedUserInstance.setAttribute(new Attribute<String>("one-valued", "1"));

        identityManager.updateUser(storedUserInstance);

        User updatedUserInstance = identityManager.getUser(storedUserInstance.getId());

        Attribute<String> oneValuedAttribute = updatedUserInstance.getAttribute("one-valued");

        assertNotNull(oneValuedAttribute);
        assertEquals("1", oneValuedAttribute.getValue());
    }

    /**
     * <p>
     * Sets a multi-valued attribute.
     * </p>
     * 
     * @throws Exception
     */
    @Test
    public void testSetMultiValuedAttribute() throws Exception {
        IdentityManager identityManager = getIdentityManager();

        User storedUserInstance = identityManager.getUser("admin");

        storedUserInstance.setAttribute(new Attribute<String[]>("multi-valued", new String[] { "1", "2", "3" }));

        identityManager.updateUser(storedUserInstance);

        User updatedUserInstance = identityManager.getUser(storedUserInstance.getId());

        Attribute<String[]> multiValuedAttribute = updatedUserInstance.getAttribute("multi-valued");

        assertNotNull(multiValuedAttribute);
        assertEquals("1", multiValuedAttribute.getValue()[0]);
        assertEquals("2", multiValuedAttribute.getValue()[1]);
        assertEquals("3", multiValuedAttribute.getValue()[2]);
    }

    /**
     * <p>
     * Updates an attribute.
     * </p>
     * 
     * @throws Exception
     */
    @Test
    public void testUpdateAttribute() throws Exception {
        IdentityManager identityManager = getIdentityManager();

        User storedUserInstance = identityManager.getUser("admin");

        storedUserInstance.setAttribute(new Attribute<String[]>("multi-valued", new String[] { "1", "2", "3" }));

        identityManager.updateUser(storedUserInstance);

        User updatedUserInstance = identityManager.getUser(storedUserInstance.getId());

        Attribute<String[]> multiValuedAttribute = updatedUserInstance.getAttribute("multi-valued");

        assertNotNull(multiValuedAttribute);

        multiValuedAttribute.setValue(new String[] { "3", "4", "5" });

        updatedUserInstance.setAttribute(multiValuedAttribute);

        identityManager.updateUser(updatedUserInstance);

        updatedUserInstance = identityManager.getUser("admin");

        multiValuedAttribute = updatedUserInstance.getAttribute("multi-valued");

        assertNotNull(multiValuedAttribute);
        assertEquals("3", multiValuedAttribute.getValue()[0]);
        assertEquals("4", multiValuedAttribute.getValue()[1]);
        assertEquals("5", multiValuedAttribute.getValue()[2]);
    }

    /**
     * <p>
     * Removes an attribute.
     * </p>
     * 
     * @throws Exception
     */
    @Test
    public void testRemoveAttribute() throws Exception {
        IdentityManager identityManager = getIdentityManager();

        User storedUserInstance = identityManager.getUser("admin");

        storedUserInstance.setAttribute(new Attribute<String[]>("multi-valued", new String[] { "1", "2", "3" }));

        identityManager.updateUser(storedUserInstance);

        User updatedUserInstance = identityManager.getUser(storedUserInstance.getId());

        Attribute<String[]> multiValuedAttribute = updatedUserInstance.getAttribute("multi-valued");

        assertNotNull(multiValuedAttribute);

        updatedUserInstance.removeAttribute("multi-valued");

        identityManager.updateUser(updatedUserInstance);

        updatedUserInstance = identityManager.getUser("admin");

        multiValuedAttribute = updatedUserInstance.getAttribute("multi-valued");

        assertNull(multiValuedAttribute);
    }

    public IdentityManager getIdentityManager() {
        return this.identityManager;
    }
    
    public void setIdentityManager(IdentityManager identityManager) {
        this.identityManager = identityManager;
    }
}
