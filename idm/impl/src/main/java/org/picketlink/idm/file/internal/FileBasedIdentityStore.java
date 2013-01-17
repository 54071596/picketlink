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

package org.picketlink.idm.file.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.picketlink.idm.IdentityManagementException;
import org.picketlink.idm.SecurityConfigurationException;
import org.picketlink.idm.credential.Credentials;
import org.picketlink.idm.credential.internal.DigestCredentialHandler;
import org.picketlink.idm.credential.internal.PasswordCredentialHandler;
import org.picketlink.idm.credential.internal.X509CertificateCredentialHandler;
import org.picketlink.idm.credential.spi.CredentialHandler;
import org.picketlink.idm.credential.spi.CredentialStorage;
import org.picketlink.idm.credential.spi.annotations.CredentialHandlers;
import org.picketlink.idm.credential.spi.annotations.Stored;
import org.picketlink.idm.event.AgentCreatedEvent;
import org.picketlink.idm.event.AgentDeletedEvent;
import org.picketlink.idm.event.AgentUpdatedEvent;
import org.picketlink.idm.event.GroupCreatedEvent;
import org.picketlink.idm.event.GroupDeletedEvent;
import org.picketlink.idm.event.GroupUpdatedEvent;
import org.picketlink.idm.event.RelationshipCreatedEvent;
import org.picketlink.idm.event.RelationshipDeletedEvent;
import org.picketlink.idm.event.RelationshipUpdatedEvent;
import org.picketlink.idm.event.RoleCreatedEvent;
import org.picketlink.idm.event.RoleDeletedEvent;
import org.picketlink.idm.event.RoleUpdatedEvent;
import org.picketlink.idm.event.UserCreatedEvent;
import org.picketlink.idm.event.UserDeletedEvent;
import org.picketlink.idm.event.UserUpdatedEvent;
import org.picketlink.idm.internal.util.IDMUtil;
import org.picketlink.idm.internal.util.properties.Property;
import org.picketlink.idm.internal.util.properties.query.AnnotatedPropertyCriteria;
import org.picketlink.idm.internal.util.properties.query.NamedPropertyCriteria;
import org.picketlink.idm.internal.util.properties.query.PropertyQueries;
import org.picketlink.idm.model.Agent;
import org.picketlink.idm.model.Attribute;
import org.picketlink.idm.model.AttributedType;
import org.picketlink.idm.model.Grant;
import org.picketlink.idm.model.Group;
import org.picketlink.idm.model.GroupMembership;
import org.picketlink.idm.model.GroupRole;
import org.picketlink.idm.model.IdentityType;
import org.picketlink.idm.model.Partition;
import org.picketlink.idm.model.Realm;
import org.picketlink.idm.model.Relationship;
import org.picketlink.idm.model.Role;
import org.picketlink.idm.model.SimpleAgent;
import org.picketlink.idm.model.SimpleGroup;
import org.picketlink.idm.model.SimpleRole;
import org.picketlink.idm.model.SimpleUser;
import org.picketlink.idm.model.Tier;
import org.picketlink.idm.model.User;
import org.picketlink.idm.model.annotation.RelationshipIdentity;
import org.picketlink.idm.query.IdentityQuery;
import org.picketlink.idm.query.IdentityTypeQueryParameter;
import org.picketlink.idm.query.QueryParameter;
import org.picketlink.idm.query.RelationshipQuery;
import org.picketlink.idm.query.internal.DefaultRelationshipQuery;
import org.picketlink.idm.spi.CredentialStore;
import org.picketlink.idm.spi.IdentityStore;
import org.picketlink.idm.spi.IdentityStoreInvocationContext;

/**
 * <p>
 * File based {@link IdentityStore} implementation. By default, each new instance recreate the data files. This behavior can be
 * changed by configuring the <code>alwaysCreateFiles</code> property to false.
 * </p>
 * 
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 * 
 */
@CredentialHandlers({ PasswordCredentialHandler.class, X509CertificateCredentialHandler.class, DigestCredentialHandler.class })
public class FileBasedIdentityStore implements IdentityStore<FileIdentityStoreConfiguration>, CredentialStore {

    private FileIdentityStoreConfiguration config;
    private IdentityStoreInvocationContext context;

    @Override
    public void setup(FileIdentityStoreConfiguration config, IdentityStoreInvocationContext context) {
        this.config = config;
        this.context = context;
    }

    @Override
    public FileIdentityStoreConfiguration getConfig() {
        return this.config;
    }

    @Override
    public IdentityStoreInvocationContext getContext() {
        return this.context;
    }

    @Override
    public void add(AttributedType attributedType) {
        attributedType.setId(generateUUID());

        Object eventToFire = null;

        if (IdentityType.class.isInstance(attributedType)) {
            Class<? extends IdentityType> identityTypeClass = (Class<? extends IdentityType>) attributedType.getClass();

            if (IDMUtil.isUserType(identityTypeClass)) {
                User storedUser = addUser((User) attributedType);

                eventToFire = new UserCreatedEvent(storedUser);
            } else if (IDMUtil.isAgentType(identityTypeClass)) {
                Agent storedAgent = addAgent((Agent) attributedType);

                eventToFire = new AgentCreatedEvent(storedAgent);
            } else if (IDMUtil.isGroupType(identityTypeClass)) {
                Group storedGroup = addGroup((Group) attributedType);

                eventToFire = new GroupCreatedEvent(storedGroup);
            } else if (IDMUtil.isRoleType(identityTypeClass)) {
                Role storedRole = addRole((Role) attributedType);

                eventToFire = new RoleCreatedEvent(storedRole);
            } else {
                throw createUnsupportedIdentityTypeException(identityTypeClass);
            }
        } else if (Relationship.class.isInstance(attributedType)) {
            Relationship relationship = (Relationship) attributedType;

            addRelationship(relationship);

            eventToFire = new RelationshipCreatedEvent(relationship);
        } else {
            throw new IdentityManagementException("Unsupported AttributedType [" + attributedType.getClass().getName() + "].");
        }

        getContext().getEventBridge().raiseEvent(eventToFire);
    }

    @Override
    public void update(AttributedType attributedType) {
        Object eventToFire = null;

        if (IdentityType.class.isInstance(attributedType)) {
            Class<? extends IdentityType> identityTypeClass = (Class<? extends IdentityType>) attributedType.getClass();

            if (IDMUtil.isUserType(identityTypeClass)) {
                User updatedUser = (User) attributedType;

                User storedUser = getStoredUser(updatedUser);

                updateUser(updatedUser, storedUser);

                eventToFire = new UserUpdatedEvent(storedUser);
            } else if (IDMUtil.isAgentType(identityTypeClass)) {
                Agent updatedAgent = (Agent) attributedType;

                Agent storedAgent = getStoredAgent(updatedAgent);

                updateAgent(updatedAgent, storedAgent);

                eventToFire = new AgentUpdatedEvent(storedAgent);
            } else if (IDMUtil.isGroupType(identityTypeClass)) {
                Group updatedGroup = (Group) attributedType;

                Group storedGroup = getStoredGroup(updatedGroup);

                updateGroup(updatedGroup, storedGroup);

                eventToFire = new GroupUpdatedEvent(storedGroup);
            } else if (IDMUtil.isRoleType(identityTypeClass)) {
                Role updatedRole = (Role) attributedType;

                Role storedRole = getStoredRole(updatedRole);

                updateRole(updatedRole, storedRole);

                eventToFire = new RoleUpdatedEvent(storedRole);
            } else {
                throw createUnsupportedIdentityTypeException(identityTypeClass);
            }
        } else if (Relationship.class.isInstance(attributedType)) {
            Relationship relationship = (Relationship) attributedType;

            List<FileRelationshipStorage> relationships = getConfig().getRelationships(getContext()).get(
                    attributedType.getClass().getName());

            for (FileRelationshipStorage storedRelationship : new ArrayList<FileRelationshipStorage>(relationships)) {
                if (storedRelationship.getId().equals(relationship.getId())) {
                    updateAttributedType(relationship, convertToRelationship(storedRelationship));
                    updateRelationshipAttributes(relationship, storedRelationship);
                }
            }

            getConfig().flushRelationships(getContext());

            eventToFire = new RelationshipUpdatedEvent(relationship);
        } else {
            throw new IdentityManagementException("Unsupported AttributedType [" + attributedType.getClass().getName() + "].");
        }

        getContext().getEventBridge().raiseEvent(eventToFire);
    }

    @Override
    public void remove(AttributedType attributedType) {
        Class<? extends IdentityType> attributedTypeClass = (Class<? extends IdentityType>) attributedType.getClass();

        Object eventToFire = null;

        if (IdentityType.class.isInstance(attributedType)) {
            if (IDMUtil.isUserType(attributedTypeClass)) {
                User user = (User) attributedType;

                User storedUser = getStoredUser(user);

                removeUser(storedUser);

                eventToFire = new UserDeletedEvent(storedUser);
            } else if (IDMUtil.isAgentType(attributedTypeClass)) {
                Agent agent = (Agent) attributedType;

                Agent storedAgent = getStoredAgent(agent);

                removeAgent(storedAgent);

                eventToFire = new AgentDeletedEvent(storedAgent);
            } else if (IDMUtil.isGroupType(attributedTypeClass)) {
                Group group = (Group) attributedType;

                Group storedGroup = getStoredGroup(group);

                removeGroup(storedGroup);

                eventToFire = new GroupDeletedEvent(storedGroup);
            } else if (IDMUtil.isRoleType(attributedTypeClass)) {
                Role role = (Role) attributedType;

                Role storedRole = getStoredRole(role);

                removeRole(storedRole);

                eventToFire = new RoleDeletedEvent(storedRole);
            }
        } else if (Relationship.class.isInstance(attributedType)) {
            Relationship relationship = (Relationship) attributedType;

            List<FileRelationshipStorage> relationships = getConfig().getRelationships(getContext()).get(
                    attributedTypeClass.getName());

            for (FileRelationshipStorage storedRelationship : new ArrayList<FileRelationshipStorage>(relationships)) {
                if (storedRelationship.getId().equals(relationship.getId())) {
                    relationships.remove(storedRelationship);
                }
            }

            getConfig().flushRelationships(getContext());

            eventToFire = new RelationshipDeletedEvent(relationship);
        } else {
            throw new IdentityManagementException("Unsupported AttributedType [" + attributedType.getClass().getName() + "].");
        }

        getContext().getEventBridge().raiseEvent(eventToFire);
    }

    @Override
    public Agent getAgent(String loginName) {
        return getConfig().getAgents(getContext()).get(loginName);
    }

    @Override
    public User getUser(String loginName) {
        Agent agent = getAgent(loginName);

        if (!User.class.isInstance(agent)) {
            return null;
        }

        return (User) agent;
    }

    @Override
    public Role getRole(String roleName) {
        return lookupRole(roleName, getCurrentRealm(), getContext().getTier());
    }
    
    private Role lookupRole(String roleName, Realm realm, Tier tier) {
        Role role = getConfig().getRoles(realm.getId()).get(roleName);
        
        if (role == null && tier != null) {
            role = getConfig().getRoles(tier.getId()).get(roleName);
            
            if (role == null && tier.getParent() != null) {
                role = lookupRole(roleName, realm, tier.getParent());
            }
        }
        
        return role;
    }
    
    private Group lookupGroup(String groupName, Realm realm, Tier tier) {
        Group group = getConfig().getGroups(realm.getId()).get(groupName);
        
        if (group == null && tier != null) {
            group = getConfig().getGroups(tier.getId()).get(groupName);
            
            if (group == null && tier.getParent() != null) {
                group = lookupGroup(groupName, realm, tier.getParent());
            }
        }
        
        return group;
    }
    
    /**
     * <p>
     * Returns the identifier for the given {@link Realm}. If it is null, the default {@link Realm} identifier will be returned.
     * </p>
     * 
     * @param realm
     * @return
     */
    private Partition getRealmId(IdentityStoreInvocationContext context) {
        Partition partition = context.getRealm();
        
        if (partition == null) {
            partition = getCurrentRealm();
        }
        
        if (context.getTier() != null) {
            partition = context.getTier();
        }

        return partition;
    }

    @Override
    public Group getGroup(String groupName) {
        return lookupGroup(groupName, getCurrentRealm(), getContext().getTier());
    }

    @Override
    public Group getGroup(String name, Group parent) {
        Group group = getGroup(name);
        Group parentGroup = group.getParentGroup();

        if (parentGroup == null || !parentGroup.getName().equals(parent.getName())) {
            group = null;
        }

        return group;
    }

    @Override
    public void validateCredentials(Credentials credentials) {
        CredentialHandler handler = getContext().getCredentialValidator(credentials.getClass(), this);
        if (handler == null) {
            throw new SecurityConfigurationException(
                    "No suitable CredentialHandler available for validating Credentials of type [" + credentials.getClass()
                            + "] for IdentityStore [" + this.getClass() + "]");
        }
        handler.validate(credentials, this);
    }

    @Override
    public void updateCredential(Agent agent, Object credential, Date effectiveDate, Date expiryDate) {
        CredentialHandler handler = getContext().getCredentialUpdater(credential.getClass(), this);
        if (handler == null) {
            throw new SecurityConfigurationException(
                    "No suitable CredentialHandler available for updating Credentials of type [" + credential.getClass()
                            + "] for IdentityStore [" + this.getClass() + "]");
        }
        handler.update(agent, credential, this, effectiveDate, expiryDate);
    }

    @Override
    public void storeCredential(Agent agent, CredentialStorage storage) {
        Map<String, List<FileCredentialStorage>> agentCredentials = getConfig().getCredentials(getContext()).get(
                agent.getLoginName());

        if (agentCredentials == null) {
            agentCredentials = new HashMap<String, List<FileCredentialStorage>>();
        }

        List<FileCredentialStorage> credentials = agentCredentials.get(storage.getClass().getName());

        if (credentials == null) {
            credentials = new ArrayList<FileCredentialStorage>();
        }

        for (FileCredentialStorage fileCredentialStorage : credentials) {
            if (isCurrentCredential(fileCredentialStorage)) {
                fileCredentialStorage.setExpiryDate(new Date());
            }
        }

        List<Property<Object>> annotatedTypes = PropertyQueries.createQuery(storage.getClass())
                .addCriteria(new AnnotatedPropertyCriteria(Stored.class)).getResultList();

        FileCredentialStorage credential = new FileCredentialStorage();

        for (Property<Object> property : annotatedTypes) {
            credential.getStoredFields().put(property.getName(), (Serializable) property.getValue(storage));
        }

        if (credential.getEffectiveDate() == null) {
            credential.setEffectiveDate(new Date());
        }

        credentials.add(credential);
        agentCredentials.put(storage.getClass().getName(), credentials);
        getConfig().getCredentials(getContext()).put(agent.getLoginName(), agentCredentials);

        getConfig().flushCredentials(getContext());
    }

    @Override
    public <T extends CredentialStorage> T retrieveCurrentCredential(Agent agent, Class<T> storageClass) {
        Map<String, List<FileCredentialStorage>> agentCredentials = getConfig().getCredentials(getContext()).get(
                agent.getLoginName());

        if (agentCredentials == null) {
            agentCredentials = new HashMap<String, List<FileCredentialStorage>>();
        }

        List<FileCredentialStorage> credentials = agentCredentials.get(storageClass.getName());

        if (credentials != null) {
            for (FileCredentialStorage fileCredentialStorage : credentials) {
                if (isCurrentCredential(fileCredentialStorage)) {
                    return convertToCredentialStorage(storageClass, fileCredentialStorage);
                }
            }
        }

        return null;
    }

    @Override
    public <T extends CredentialStorage> List<T> retrieveCredentials(Agent agent, Class<T> storageClass) {
        ArrayList<T> storedCredentials = new ArrayList<T>();

        Map<String, List<FileCredentialStorage>> agentCredentials = getConfig().getCredentials(getContext()).get(
                agent.getLoginName());

        if (agentCredentials == null) {
            agentCredentials = new HashMap<String, List<FileCredentialStorage>>();
        }

        List<FileCredentialStorage> credentials = agentCredentials.get(storageClass.getName());

        if (credentials != null) {
            for (FileCredentialStorage fileCredentialStorage : credentials) {
                storedCredentials.add(convertToCredentialStorage(storageClass, fileCredentialStorage));
            }
        }

        return storedCredentials;
    }

    @Override
    public <T extends IdentityType> int countQueryResults(IdentityQuery<T> identityQuery) {
        throw createNotImplementedYetException();
    }

    @Override
    public <T extends Serializable> Attribute<T> getAttribute(IdentityType identityType, String attributeName) {
        throw createNotImplementedYetException();
    }

    @Override
    public void setAttribute(IdentityType identityType, Attribute<? extends Serializable> attribute) {
        throw createNotImplementedYetException();
    }

    @Override
    public void removeAttribute(IdentityType identityType, String attributeName) {
        throw createNotImplementedYetException();
    }

    @Override
    public <T extends Relationship> List<T> fetchQueryResults(RelationshipQuery<T> query) {
        List<T> result = new ArrayList<T>();

        Class<T> relationshipType = query.getRelationshipType();
        List<FileRelationshipStorage> relationships = getConfig().getRelationships(getContext())
                .get(relationshipType.getName());

        if (relationships == null) {
            return result;
        }

        for (FileRelationshipStorage storedRelationship : relationships) {
            boolean match = false;

            if (query.getRelationshipType().getName().equals(storedRelationship.getType())) {
                Set<Entry<QueryParameter, Object[]>> parameters = query.getParameters().entrySet();

                for (Entry<QueryParameter, Object[]> entry : parameters) {
                    QueryParameter queryParameter = entry.getKey();
                    Object[] values = entry.getValue();

                    if (entry.getKey() instanceof IdentityTypeQueryParameter) {
                        IdentityTypeQueryParameter identityTypeParameter = (IdentityTypeQueryParameter) entry.getKey();
                        int valuesMathCount = values.length;

                        IdentityType identityTypeRel = storedRelationship.getIdentityTypes().get(
                                identityTypeParameter.getName());

                        for (Object object : values) {
                            IdentityType identityType = (IdentityType) object;

                            if (identityTypeRel.getClass().isInstance(identityType)
                                    && identityTypeRel.getId().equals(identityType.getId())) {
                                valuesMathCount--;
                            }
                        }

                        match = valuesMathCount <= 0;
                    }

                    if (AttributedType.AttributeParameter.class.isInstance(queryParameter) && values != null) {
                        AttributedType.AttributeParameter customParameter = (AttributedType.AttributeParameter) queryParameter;
                        Serializable userAttributeValue = storedRelationship.getAttributes().get(customParameter.getName());

                        if (userAttributeValue != null) {
                            int count = values.length;

                            for (Object value : values) {
                                if (userAttributeValue.getClass().isArray()) {
                                    Object[] userValues = (Object[]) userAttributeValue;

                                    for (Object object : userValues) {
                                        if (object.equals(value)) {
                                            count--;
                                        }
                                    }
                                } else {
                                    if (value.equals(userAttributeValue)) {
                                        count--;
                                    }
                                }
                            }

                            match = count <= 0;
                        }
                    }

                    if (!match) {
                        break;
                    }
                }
            }

            if (match) {
                result.add((T) convertToRelationship(storedRelationship));
            }
        }

        return result;
    }

    @Override
    public <T extends Relationship> int countQueryResults(RelationshipQuery<T> query) {
        return 0;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends IdentityType> List<T> fetchQueryResults(IdentityQuery<T> identityQuery) {
        Class<T> identityTypeClass = identityQuery.getIdentityType();

        Set<?> entries = null;

        if (IdentityType.class.equals(identityTypeClass)) {
            Map<String, IdentityType> allIdentityTypes = new HashMap<String, IdentityType>();

            allIdentityTypes.putAll(getConfig().getAgents(getContext()));
            allIdentityTypes.putAll(getConfig().getRoles(getRealmId(getContext()).getId()));
            allIdentityTypes.putAll(getConfig().getGroups(getRealmId(getContext()).getId()));

            entries = allIdentityTypes.entrySet();
        } else if (IDMUtil.isAgentType(identityTypeClass)) {
            entries = getConfig().getAgents(getContext()).entrySet();
        } else if (IDMUtil.isRoleType(identityTypeClass)) {
            entries = getConfig().getRoles(getRealmId(getContext()).getId()).entrySet();
        } else if (IDMUtil.isGroupType(identityTypeClass)) {
            entries = getConfig().getGroups(getRealmId(getContext()).getId()).entrySet();
        } else {
            throw createUnsupportedIdentityTypeException(identityTypeClass);
        }

        List<T> result = new ArrayList<T>();

        for (Iterator<?> iterator = entries.iterator(); iterator.hasNext();) {
            Entry<String, IdentityType> entry = (Entry<String, IdentityType>) iterator.next();

            IdentityType storedEntry = entry.getValue();

            if (!identityTypeClass.isAssignableFrom(storedEntry.getClass())) {
                continue;
            }

            if (!isQueryParameterEquals(identityQuery, IdentityType.ID, storedEntry.getId())) {
                continue;
            }

            if (IDMUtil.isAgentType(identityTypeClass)) {
                Agent agent = (Agent) storedEntry;

                if (!isQueryParameterEquals(identityQuery, Agent.LOGIN_NAME, agent.getLoginName())) {
                    continue;
                }

                if (IDMUtil.isUserType(identityTypeClass)) {
                    User user = (User) storedEntry;

                    if (!isQueryParameterEquals(identityQuery, User.EMAIL, user.getEmail())) {
                        continue;
                    }

                    if (!isQueryParameterEquals(identityQuery, User.FIRST_NAME, user.getFirstName())) {
                        continue;
                    }

                    if (!isQueryParameterEquals(identityQuery, User.LAST_NAME, user.getLastName())) {
                        continue;
                    }
                }
            }

            if (IDMUtil.isRoleType(identityTypeClass)) {
                Role role = (Role) storedEntry;

                if (!isQueryParameterEquals(identityQuery, Role.NAME, role.getName())) {
                    continue;
                }
            }

            if (IDMUtil.isGroupType(identityTypeClass)) {
                Group group = (Group) storedEntry;

                if (!isQueryParameterEquals(identityQuery, Group.NAME, group.getName())) {
                    continue;
                }

                String parentGroupName = null;

                if (group.getParentGroup() != null) {
                    parentGroupName = group.getParentGroup().getName();
                }

                if (!isQueryParameterEquals(identityQuery, Group.PARENT, parentGroupName)) {
                    continue;
                }
            }

            if (!isQueryParameterEquals(identityQuery, IdentityType.ENABLED, storedEntry.isEnabled())) {
                continue;
            }

            if (identityQuery.getParameter(IdentityType.CREATED_DATE) != null
                    || identityQuery.getParameter(IdentityType.CREATED_BEFORE) != null
                    || identityQuery.getParameter(IdentityType.CREATED_AFTER) != null) {
                Date createdDate = storedEntry.getCreatedDate();

                if (createdDate != null) {
                    if (!isQueryParameterEquals(identityQuery, IdentityType.CREATED_DATE, createdDate)) {
                        continue;
                    }

                    long timeInMillis = createdDate.getTime();

                    if (!isQueryParameterLessThan(identityQuery, IdentityType.CREATED_BEFORE, timeInMillis)) {
                        continue;
                    }

                    if (!isQueryParameterGreaterThan(identityQuery, IdentityType.CREATED_AFTER, timeInMillis)) {
                        continue;
                    }
                }
            }

            if (identityQuery.getParameter(IdentityType.EXPIRY_DATE) != null
                    || identityQuery.getParameter(IdentityType.EXPIRY_BEFORE) != null
                    || identityQuery.getParameter(IdentityType.EXPIRY_AFTER) != null) {
                Date expiryDate = storedEntry.getExpirationDate();

                if (!isQueryParameterEquals(identityQuery, IdentityType.EXPIRY_DATE, expiryDate)) {
                    continue;
                }

                Long expiryDateInMillis = null;

                if (expiryDate != null) {
                    expiryDateInMillis = expiryDate.getTime();
                }

                if (!isQueryParameterLessThan(identityQuery, IdentityType.EXPIRY_BEFORE, expiryDateInMillis)) {
                    continue;
                }

                if (!isQueryParameterGreaterThan(identityQuery, IdentityType.EXPIRY_AFTER, expiryDateInMillis)) {
                    continue;
                }
            }

            Map<QueryParameter, Object[]> attributeParameters = identityQuery
                    .getParameters(AttributedType.AttributeParameter.class);

            if (!attributeParameters.isEmpty()) {
                boolean match = false;

                for (Entry<QueryParameter, Object[]> attributeParameterEntry : attributeParameters.entrySet()) {
                    QueryParameter queryParameter = attributeParameterEntry.getKey();
                    Object[] queryParameterValues = attributeParameterEntry.getValue();

                    AttributedType.AttributeParameter customParameter = (AttributedType.AttributeParameter) queryParameter;
                    Attribute<Serializable> userAttribute = storedEntry.getAttribute(customParameter.getName());

                    if (userAttribute != null && userAttribute.getValue() != null) {
                        int count = queryParameterValues.length;

                        for (Object value : queryParameterValues) {
                            if (userAttribute.getValue().getClass().isArray()) {
                                Object[] userValues = (Object[]) userAttribute.getValue();

                                for (Object object : userValues) {
                                    if (object.equals(value)) {
                                        count--;
                                    }
                                }
                            } else {
                                if (value.equals(userAttribute.getValue())) {
                                    count--;
                                }
                            }
                        }

                        match = count <= 0;

                        if (!match) {
                            break;
                        }
                    }
                }

                if (!match) {
                    continue;
                }
            }

            Object[] values = identityQuery.getParameter(IdentityType.HAS_ROLE);

            if (values != null) {
                int valuesMatchCount = values.length;

                for (Object roleName : values) {
                    Role role = getRole(roleName.toString());

                    RelationshipQuery<Grant> query = new DefaultRelationshipQuery<Grant>(Grant.class, this);

                    query.setParameter(Grant.ASSIGNEE, storedEntry);
                    query.setParameter(Grant.ROLE, role);

                    List<Grant> relationships = query.getResultList();

                    if (!relationships.isEmpty()) {
                        valuesMatchCount--;
                    }
                }

                if (valuesMatchCount > 0) {
                    continue;
                }
            }

            values = identityQuery.getParameter(IdentityType.MEMBER_OF);

            if (values != null) {
                int valuesMatchCount = values.length;

                for (Object groupName : values) {
                    Group group = getGroup(groupName.toString());

                    RelationshipQuery<GroupMembership> query = new DefaultRelationshipQuery<GroupMembership>(
                            GroupMembership.class, this);

                    query.setParameter(GroupMembership.MEMBER, storedEntry);
                    query.setParameter(GroupMembership.GROUP, group);

                    List<GroupMembership> relationships = query.getResultList();

                    if (!relationships.isEmpty()) {
                        valuesMatchCount--;
                    }
                }

                if (valuesMatchCount > 0) {
                    continue;
                }
            }

            values = identityQuery.getParameter(IdentityType.HAS_GROUP_ROLE);

            if (values != null) {
                int valuesMatchCount = values.length;

                for (Object object : values) {
                    GroupRole groupRole = (GroupRole) object;

                    RelationshipQuery<GroupRole> query = new DefaultRelationshipQuery<GroupRole>(GroupRole.class, this);

                    query.setParameter(GroupRole.MEMBER, storedEntry);
                    query.setParameter(GroupRole.GROUP, groupRole.getGroup());
                    query.setParameter(GroupRole.ROLE, groupRole.getRole());

                    List<GroupRole> relationships = query.getResultList();

                    if (!relationships.isEmpty()) {
                        valuesMatchCount--;
                    }
                }

                if (valuesMatchCount > 0) {
                    continue;
                }
            }

            values = identityQuery.getParameter(IdentityType.ROLE_OF);

            if (values != null) {
                Role currentRole = (Role) storedEntry;

                List<FileRelationshipStorage> relationships = getConfig().getRelationships(getContext()).get(
                        Grant.class.getName());

                if (relationships == null) {
                    continue;
                } else {
                    int valuesMatchCount = values.length;

                    for (Object object : values) {
                        Agent agent = (Agent) object;

                        for (FileRelationshipStorage storedRelationship : new ArrayList<FileRelationshipStorage>(relationships)) {
                            Grant grant = convertToRelationship(storedRelationship);

                            if (!grant.getRole().getId().equals(currentRole.getId())) {
                                continue;
                            }

                            if (grant.getAssignee().getId().equals(agent.getId())) {
                                valuesMatchCount--;
                            }
                        }
                    }

                    if (valuesMatchCount > 0) {
                        continue;
                    }
                }
            }

            values = identityQuery.getParameter(IdentityType.HAS_MEMBER);

            if (values != null) {
                Group currentGroup = (Group) storedEntry;

                List<FileRelationshipStorage> relationships = getConfig().getRelationships(getContext()).get(
                        GroupMembership.class.getName());

                if (relationships == null) {
                    continue;
                } else {
                    int valuesMatchCount = values.length;

                    for (Object object : values) {
                        Agent agent = (Agent) object;

                        for (FileRelationshipStorage storedRelationship : new ArrayList<FileRelationshipStorage>(relationships)) {
                            GroupMembership grant = convertToRelationship(storedRelationship);

                            if (!grant.getGroup().getId().equals(currentGroup.getId())) {
                                continue;
                            }

                            if (grant.getMember().getId().equals(agent.getId())) {
                                valuesMatchCount--;
                            }
                        }
                    }

                    if (valuesMatchCount > 0) {
                        continue;
                    }
                }
            }

            result.add((T) storedEntry);
        }

        return result;
    }

    /**
     * <p>
     * Updated the common properties for a specific {@link IdentityType} instance from another instance.
     * </p>
     * 
     * @param fromIdentityType
     * @param toIdentityType
     */
    private void updateIdentityType(IdentityType fromIdentityType, IdentityType toIdentityType) {
        toIdentityType.setEnabled(fromIdentityType.isEnabled());
        toIdentityType.setCreatedDate(fromIdentityType.getCreatedDate());
        toIdentityType.setExpirationDate(fromIdentityType.getExpirationDate());

        updateAttributedType(fromIdentityType, toIdentityType);
    }

    private void updateAttributedType(AttributedType fromIdentityType, AttributedType toIdentityType) {
        toIdentityType.setId(fromIdentityType.getId());

        for (Object object : toIdentityType.getAttributes().toArray()) {
            Attribute<? extends Serializable> attribute = (Attribute<? extends Serializable>) object;
            toIdentityType.removeAttribute(attribute.getName());
        }

        for (Attribute<? extends Serializable> attrib : fromIdentityType.getAttributes()) {
            toIdentityType.setAttribute(attrib);
        }
    }

    private boolean isQueryParameterEquals(IdentityQuery<?> identityQuery, QueryParameter queryParameter,
            Serializable valueToCompare) {
        Object[] values = identityQuery.getParameter(queryParameter);

        if (values == null) {
            return true;
        }

        Object value = values[0];

        if (Date.class.isInstance(valueToCompare)) {
            Date parameterDate = (Date) value;
            value = parameterDate.getTime();

            Date toCompareDate = (Date) valueToCompare;
            valueToCompare = toCompareDate.getTime();
        }

        if (values.length > 0 && valueToCompare != null && valueToCompare.equals(value)) {
            return true;
        }

        return false;
    }

    private boolean isQueryParameterEquals(IdentityQuery<?> identityQuery, QueryParameter queryParameter, Date valueToCompare) {
        Object[] values = identityQuery.getParameter(queryParameter);

        if (values == null) {
            return true;
        }
        if (values.length > 0 && valueToCompare != null && valueToCompare.equals(values[0])) {
            return true;
        }

        return false;
    }

    private boolean isQueryParameterGreaterThan(IdentityQuery<?> identityQuery, QueryParameter queryParameter,
            Long valueToCompare) {
        return isQueryParameterGreaterOrLessThan(identityQuery, queryParameter, valueToCompare, true);
    }

    private boolean isQueryParameterLessThan(IdentityQuery<?> identityQuery, QueryParameter queryParameter, Long valueToCompare) {
        return isQueryParameterGreaterOrLessThan(identityQuery, queryParameter, valueToCompare, false);
    }

    private boolean isQueryParameterGreaterOrLessThan(IdentityQuery<?> identityQuery, QueryParameter queryParameter,
            Long valueToCompare, boolean greaterThan) {
        Object[] values = identityQuery.getParameter(queryParameter);

        if (values == null) {
            return true;
        }

        long value = 0;

        if (Date.class.isInstance(values[0])) {
            Date parameterDate = (Date) values[0];
            value = parameterDate.getTime();
        } else {
            value = Long.valueOf(values[0].toString());
        }

        if (values.length > 0 && valueToCompare != null) {
            if (greaterThan && valueToCompare >= value) {
                return true;
            }

            if (!greaterThan && valueToCompare <= value) {
                return true;
            }
        }

        return false;
    }

    private boolean isCurrentCredential(FileCredentialStorage fileCredentialStorage) {
        boolean isCurrent = true;

        Date actualDate = new Date();

        if (fileCredentialStorage.getEffectiveDate() != null) {
            if (fileCredentialStorage.getEffectiveDate().compareTo(actualDate) > 0) {
                isCurrent = false;
            }
        }

        if (isCurrent) {
            if (fileCredentialStorage.getExpiryDate() != null) {
                if (fileCredentialStorage.getExpiryDate().compareTo(actualDate) <= 0) {
                    isCurrent = false;
                }
            }
        }

        return isCurrent;
    }

    private <T> T convertToCredentialStorage(Class<T> storageClass, FileCredentialStorage fileCredentialStorage) {
        T storage = null;

        try {
            storage = storageClass.newInstance();
        } catch (Exception e) {
            throw new IdentityManagementException("Could not create CredentialStorage instance for class ["
                    + storageClass.getName() + "].", e);
        }

        Set<Entry<String, Serializable>> storedFieldsEntrySet = fileCredentialStorage.getStoredFields().entrySet();

        for (Entry<String, Serializable> storedFieldEntry : storedFieldsEntrySet) {
            List<Property<Object>> annotatedTypes = PropertyQueries.createQuery(storageClass)
                    .addCriteria(new NamedPropertyCriteria(storedFieldEntry.getKey())).getResultList();

            if (annotatedTypes.isEmpty()) {
                throw new IdentityManagementException("Could not find property [" + storedFieldEntry.getKey()
                        + "] on CredentialStorage [" + storageClass.getName() + "].");
            } else if (annotatedTypes.size() > 1) {
                throw new IdentityManagementException("Ambiguos property [" + storedFieldEntry.getKey()
                        + "] on CredentialStorage [" + storageClass.getName() + "].");
            }

            Property<Object> property = annotatedTypes.get(0);

            property.setValue(storage, storedFieldEntry.getValue());
        }
        return storage;
    }

    private IdentityManagementException createNotImplementedYetException() {
        return new IdentityManagementException("Not implemented yet.");
    }

    private <T extends Relationship> T convertToRelationship(FileRelationshipStorage storedRelationship) {
        T relationship = null;
        Class<T> relationshipType = null;

        try {
            relationshipType = (Class<T>) Class.forName(storedRelationship.getType());
        } catch (ClassNotFoundException e1) {
            throw new IdentityManagementException("Could not get Relationship type [" + storedRelationship.getType() + "]");
        }

        try {
            relationship = relationshipType.newInstance();

            relationship.setId(storedRelationship.getId());

            Set<Entry<String, IdentityType>> identityTypes = storedRelationship.getIdentityTypes().entrySet();

            for (Entry<String, IdentityType> entry : identityTypes) {
                List<Property<IdentityType>> annotatedTypes = PropertyQueries.<IdentityType> createQuery(relationshipType)
                        .addCriteria(new NamedPropertyCriteria(entry.getKey())).getResultList();

                Property<IdentityType> property = annotatedTypes.get(0);

                property.setValue(relationship, entry.getValue());
            }

            Set<Entry<String, Serializable>> attributes = storedRelationship.getAttributes().entrySet();

            for (Entry<String, Serializable> entry : attributes) {
                relationship.setAttribute(new Attribute<Serializable>(entry.getKey(), entry.getValue()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return relationship;
    }

    private String generateUUID() {
        return getContext().getIdGenerator().generate();
    }

    private void addRelationship(Relationship relationship) {
        FileRelationshipStorage fileRelationship = new FileRelationshipStorage();

        fileRelationship.setId(relationship.getId());
        fileRelationship.setType(relationship.getClass().getName());

        updateRelationshipIdentity(relationship, fileRelationship);
        updateRelationshipAttributes(relationship, fileRelationship);

        Map<String, List<FileRelationshipStorage>> relationshipsMap = getConfig().getRelationships(getContext());
        List<FileRelationshipStorage> relationships = relationshipsMap.get(relationship.getClass().getName());

        if (!relationshipsMap.containsKey(relationship.getClass().getName())) {
            relationships = new ArrayList<FileRelationshipStorage>();
            relationshipsMap.put(relationship.getClass().getName(), relationships);
        }

        relationships.add(fileRelationship);
        getConfig().flushRelationships(getContext());
    }

    private void updateRelationshipIdentity(Relationship relationship, FileRelationshipStorage fileRelationship) {
        List<Property<IdentityType>> relationshipIdentityTypes = PropertyQueries
                .<IdentityType> createQuery(relationship.getClass())
                .addCriteria(new AnnotatedPropertyCriteria(RelationshipIdentity.class)).getResultList();

        for (Property<IdentityType> property : relationshipIdentityTypes) {
            IdentityType value = property.getValue(relationship);

            if (Agent.class.isInstance(value)) {
                Agent agent = (Agent) value;
                getStoredAgent(agent);
            }

            if (Role.class.isInstance(value)) {
                Role role = (Role) value;
                getStoredRole(role);
            }

            if (Group.class.isInstance(value)) {
                Group group = (Group) value;
                getStoredGroup(group);
            }

            fileRelationship.getIdentityTypes().put(property.getName(), value);
        }
    }

    private void updateRelationshipAttributes(Relationship relationship, FileRelationshipStorage fileRelationship) {
        fileRelationship.getAttributes().clear();

        Collection<Attribute<? extends Serializable>> attributes = relationship.getAttributes();

        for (Attribute<? extends Serializable> attribute : attributes) {
            fileRelationship.getAttributes().put(attribute.getName(), attribute.getValue());
        }
    }

    private Role getStoredRole(Role role) {
        if (role.getName() == null) {
            throw new IdentityManagementException("No identifier was provided.");
        }

        Role storedRole = getRole(role.getName());

        if (storedRole == null) {
            throw new RuntimeException("No Role found with the given name [" + role.getName() + "] for the current Partition ["
                    + getCurrentPartition().getName() + "].");
        }
        return storedRole;
    }

    private Group getStoredGroup(Group group) {
        if (group.getName() == null) {
            throw new IdentityManagementException("No identifier was provided.");
        }

        Group storedGroup = getGroup(group.getName());

        if (storedGroup == null) {
            throw new RuntimeException("No Group found with the given name [" + group.getName()
                    + "] for the current Partition [" + getCurrentPartition().getName() + "].");
        }
        return storedGroup;
    }

    private Agent getStoredAgent(Agent agent) {
        if (agent.getLoginName() == null) {
            throw new IdentityManagementException("No identifier was provided.");
        }

        Agent storedAgent = getAgent(agent.getLoginName());

        if (storedAgent == null) {
            throw new RuntimeException("No Agent found with the given loginName [" + agent.getLoginName()
                    + "] for the current Partition [" + getCurrentRealm().getName() + "]");
        }
        return storedAgent;
    }

    private User getStoredUser(User user) {
        if (user.getLoginName() == null) {
            throw new IdentityManagementException("No identifier was provided.");
        }

        User storedUser = getUser(user.getLoginName());

        if (storedUser == null) {
            throw new RuntimeException("No User found with the given loginName [" + user.getLoginName()
                    + "] for the current Partition [" + getCurrentRealm().getName() + "]");
        }

        return storedUser;
    }

    private Role addRole(Role role) {
        SimpleRole fileRole = new SimpleRole(role.getName());

        fileRole.setPartition(getCurrentPartition());

        updateIdentityType(role, fileRole);

        getConfig().getRoles(getRealmId(getContext()).getId()).put(fileRole.getName(), fileRole);
        getConfig().flushRoles(getContext());

        return fileRole;
    }

    private Group addGroup(Group group) {
        SimpleGroup fileGroup = null;

        if (group.getParentGroup() != null) {
            fileGroup = new SimpleGroup(group.getName(), getGroup(group.getParentGroup().getName()));
        } else {
            fileGroup = new SimpleGroup(group.getName());
        }

        fileGroup.setPartition(getCurrentPartition());

        updateIdentityType(group, fileGroup);

        getConfig().getGroups(getRealmId(getContext()).getId()).put(fileGroup.getName(), fileGroup);
        getConfig().flushGroups(getContext());

        return fileGroup;
    }

    private User addUser(User user) {
        User storedUser = new SimpleUser(user.getLoginName());

        storedUser.setFirstName(user.getFirstName());
        storedUser.setLastName(user.getLastName());
        storedUser.setEmail(user.getEmail());
        storedUser.setPartition(getCurrentRealm());

        updateIdentityType(user, storedUser);

        getConfig().getAgents(getContext()).put(storedUser.getLoginName(), storedUser);
        getConfig().flushAgents(getContext());

        return storedUser;
    }

    private Realm getCurrentRealm() {
        Realm realm = getContext().getRealm();

        if (realm == null) {
            realm = new Realm(Realm.DEFAULT_REALM);
            realm.setId(Realm.DEFAULT_REALM);
        }

        return realm;
    }

    private Partition getCurrentPartition() {
        Partition partition = getContext().getTier();

        if (partition == null) {
            partition = getCurrentRealm();
        }

        return partition;
    }

    private Agent addAgent(Agent user) {
        Agent storedAgent = new SimpleAgent(user.getLoginName());

        updateIdentityType(user, storedAgent);

        storedAgent.setPartition(getCurrentRealm());

        getConfig().getAgents(getContext()).put(storedAgent.getLoginName(), storedAgent);
        getConfig().flushAgents(getContext());

        return storedAgent;
    }

    private Role updateRole(Role updatedRole, Role storedRole) {
        if (storedRole != updatedRole) {
            updateIdentityType(updatedRole, storedRole);
        }

        getConfig().getRoles(getRealmId(getContext()).getId()).put(storedRole.getName(), storedRole);
        getConfig().flushRoles(getContext());

        return storedRole;
    }

    private Group updateGroup(Group updatedGroup, Group storedGroup) {
        if (storedGroup != updatedGroup) {
            updateIdentityType(updatedGroup, storedGroup);
        }

        getConfig().getGroups(getRealmId(getContext()).getId()).put(storedGroup.getName(), storedGroup);
        getConfig().flushGroups(getContext());

        return storedGroup;
    }

    private User updateUser(User updatedUser, User storedUser) {
        if (storedUser != updatedUser) {
            storedUser.setFirstName(updatedUser.getFirstName());
            storedUser.setLastName(updatedUser.getLastName());
            storedUser.setEmail(updatedUser.getEmail());

            updateIdentityType(updatedUser, storedUser);
        }

        getConfig().getAgents(getContext()).put(storedUser.getLoginName(), storedUser);
        getConfig().flushAgents(getContext());

        return updatedUser;
    }

    private Agent updateAgent(Agent updatedAgent, Agent storedAgent) {
        if (storedAgent != updatedAgent) {
            updateIdentityType(updatedAgent, storedAgent);
        }

        getConfig().getAgents(getContext()).put(storedAgent.getLoginName(), storedAgent);
        getConfig().flushAgents(getContext());

        return updatedAgent;
    }

    private Role removeRole(Role role) {
        getConfig().getRoles(getRealmId(getContext()).getId()).remove(role.getName());

        removeRelationships(role);

        getConfig().flushRoles(getContext());
        getConfig().flushRelationships(getContext());

        return role;
    }

    private void removeRelationships(AttributedType role) {
        Set<Entry<String, List<FileRelationshipStorage>>> entrySet = getConfig().getRelationships(getContext()).entrySet();

        for (Entry<String, List<FileRelationshipStorage>> entry : entrySet) {
            List<FileRelationshipStorage> relationships = entry.getValue();

            for (FileRelationshipStorage fileRelationshipStorage : new ArrayList<FileRelationshipStorage>(relationships)) {
                Collection<IdentityType> identityTypes = fileRelationshipStorage.getIdentityTypes().values();

                for (IdentityType identityType : identityTypes) {
                    if (role.getClass().isInstance(identityType)) {
                        if (role.getId().equals(identityType.getId())) {
                            remove(convertToRelationship(fileRelationshipStorage));
                        }
                    }
                }
            }
        }
    }

    private Group removeGroup(Group group) {
        getConfig().getGroups(getRealmId(getContext()).getId()).remove(group.getName());

        removeRelationships(group);

        getConfig().flushGroups(getContext());
        getConfig().flushRelationships(getContext());

        return group;
    }

    private User removeUser(User user) {
        getConfig().getAgents(getContext()).remove(user.getLoginName());

        removeRelationships(user);

        getConfig().flushAgents(getContext());
        getConfig().flushRelationships(getContext());

        return user;
    }

    private Agent removeAgent(Agent agent) {
        getConfig().getAgents(getContext()).remove(agent.getLoginName());

        removeRelationships(agent);

        getConfig().flushAgents(getContext());
        getConfig().flushRelationships(getContext());

        return agent;
    }

    private IdentityManagementException createUnsupportedIdentityTypeException(Class<? extends IdentityType> identityTypeClass) {
        return new IdentityManagementException("Unsupported IdentityType [" + identityTypeClass.getName() + "].");
    }
}
