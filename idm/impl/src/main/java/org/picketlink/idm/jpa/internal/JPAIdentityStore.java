package org.picketlink.idm.jpa.internal;

import static org.picketlink.idm.jpa.internal.JPAIdentityStoreConfiguration.PROPERTY_ATTRIBUTE_IDENTITY;
import static org.picketlink.idm.jpa.internal.JPAIdentityStoreConfiguration.PROPERTY_ATTRIBUTE_NAME;
import static org.picketlink.idm.jpa.internal.JPAIdentityStoreConfiguration.PROPERTY_ATTRIBUTE_VALUE;
import static org.picketlink.idm.jpa.internal.JPAIdentityStoreConfiguration.PROPERTY_CREDENTIAL_IDENTITY;
import static org.picketlink.idm.jpa.internal.JPAIdentityStoreConfiguration.PROPERTY_IDENTITY_DISCRIMINATOR;
import static org.picketlink.idm.jpa.internal.JPAIdentityStoreConfiguration.PROPERTY_IDENTITY_ID;
import static org.picketlink.idm.jpa.internal.JPAIdentityStoreConfiguration.PROPERTY_IDENTITY_KEY;
import static org.picketlink.idm.jpa.internal.JPAIdentityStoreConfiguration.PROPERTY_IDENTITY_NAME;
import static org.picketlink.idm.jpa.internal.JPAIdentityStoreConfiguration.PROPERTY_IDENTITY_PARTITION;
import static org.picketlink.idm.jpa.internal.JPAIdentityStoreConfiguration.PROPERTY_MEMBERSHIP_GROUP;
import static org.picketlink.idm.jpa.internal.JPAIdentityStoreConfiguration.PROPERTY_MEMBERSHIP_MEMBER;
import static org.picketlink.idm.jpa.internal.JPAIdentityStoreConfiguration.PROPERTY_MEMBERSHIP_ROLE;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.picketlink.idm.IdentityManagementException;
import org.picketlink.idm.event.AbstractBaseEvent;
import org.picketlink.idm.internal.AbstractIdentityStore;
import org.picketlink.idm.internal.util.properties.Property;
import org.picketlink.idm.jpa.annotations.IDMAttribute;
import org.picketlink.idm.jpa.internal.JPAIdentityStoreConfiguration.MappedAttribute;
import org.picketlink.idm.model.Agent;
import org.picketlink.idm.model.Attribute;
import org.picketlink.idm.model.Group;
import org.picketlink.idm.model.GroupRole;
import org.picketlink.idm.model.IdentityType;
import org.picketlink.idm.model.Partition;
import org.picketlink.idm.model.Realm;
import org.picketlink.idm.model.Role;
import org.picketlink.idm.model.SimpleGroupRole;
import org.picketlink.idm.model.User;
import org.picketlink.idm.query.IdentityQuery;
import org.picketlink.idm.query.internal.DefaultIdentityQuery;
import org.picketlink.idm.spi.IdentityStoreInvocationContext;

/**
 * Implementation of IdentityStore that stores its state in a relational database. This is a lightweight object
 * that is generally created once per request, and is provided references to a (heavyweight) configuration and
 * invocation context.
 * 
 * @author Shane Bryzak
 */
public class JPAIdentityStore extends AbstractIdentityStore<JPAIdentityStoreConfiguration> {

    // Invocation context parameters
    public static final String INVOCATION_CTX_ENTITY_MANAGER = "CTX_ENTITY_MANAGER";

    /**
     * The configuration for this instance
     */
    private JPAIdentityStoreConfiguration config;

    /**
     * The invocation context
     */
    private IdentityStoreInvocationContext context;

    private Map<String, IdentityTypeManager<? extends IdentityType>> identityTypeStores = new HashMap<String, IdentityTypeManager<? extends IdentityType>>();

    public void setup(JPAIdentityStoreConfiguration config, IdentityStoreInvocationContext context) {
        this.config = config;
        this.context = context;

        this.identityTypeStores.put(getIdentityDiscriminator(User.class), new UserTypeManager(this));
        this.identityTypeStores.put(getIdentityDiscriminator(Agent.class), new AgentTypeManager(this));
        this.identityTypeStores.put(getIdentityDiscriminator(Role.class), new RoleTypeManager(this));
        this.identityTypeStores.put(getIdentityDiscriminator(Group.class), new GroupTypeManager(this));
    }

    @Override
    public JPAIdentityStoreConfiguration getConfig() {
        return config;
    }

    @Override
    public IdentityStoreInvocationContext getContext() {
        return context;
    }

    @Override
    public void add(IdentityType identityType) {
        checkInvalidIdentityType(identityType);

        try {
            IdentityTypeManager<IdentityType> identityTypeManager = getIdentityTypeManager(identityType.getClass());

            Object identity = identityTypeManager.createIdentityInstance(getContext().getRealm(), identityType);

            updateAttributes(identityType, identity);

            EntityManager em = getEntityManager();

            em.persist(identity);
            em.flush();

            AbstractBaseEvent event = identityTypeManager.raiseCreatedEvent(identityType);

            event.getContext().setValue(EVENT_CONTEXT_USER_ENTITY, identity);
            getContext().getEventBridge().raiseEvent(event);
        } catch (Exception ex) {
            throw new IdentityManagementException("Exception while creating IdentityType [" + identityType + "].", ex);
        }
    }

    @Override
    public void update(IdentityType identityType) {
        checkInvalidIdentityType(identityType);

        IdentityTypeManager<IdentityType> identityTypeManager = getIdentityTypeManager(identityType.getClass());

        Object identity = getIdentityObject(identityType);

        identityTypeManager.populateIdentityInstance(getContext().getRealm(), identity, identityType);

        updateAttributes(identityType, identity);

        EntityManager em = getEntityManager();

        em.merge(identity);
        em.flush();

        AbstractBaseEvent event = identityTypeManager.raiseUpdatedEvent(identityType);

        event.getContext().setValue(EVENT_CONTEXT_USER_ENTITY, identity);
        getContext().getEventBridge().raiseEvent(event);
    }

    @Override
    public void remove(IdentityType identityType) {
        checkInvalidIdentityType(identityType);

        EntityManager em = getEntityManager();
        IdentityTypeManager<IdentityType> identityTypeManager = getIdentityTypeManager(identityType.getClass());

        Object identity = getIdentityObject(identityType);

        identityTypeManager.remove(identity, identityType);

        // Remove credentials
        removeCredentials(identity);

        // Remove attributes
        removeAttributes(identity);

        // Remove memberships - this takes a little more work because the identity may be
        // a member, a role or a group
        removeMemberships(identity);

        // Remove the identity object itself
        em.remove(identity);
        em.flush();

        AbstractBaseEvent event = identityTypeManager.raiseDeletedEvent(identityType);

        event.getContext().setValue(EVENT_CONTEXT_USER_ENTITY, identity);
        getContext().getEventBridge().raiseEvent(event);
    }

    @Override
    public User getUser(String id) {
        if (id == null) {
            return null;
        }

        // Check the cache first
        User user = getContext().getCache().lookupUser(context.getRealm(), id);

        // If the cache doesn't have a reference to the User, we have to look up it's identity object
        // and create a User instance based on it
        if (user == null) {
            DefaultIdentityQuery<User> defaultIdentityQuery = new DefaultIdentityQuery(User.class, this);

            defaultIdentityQuery.setParameter(User.ID, id);

            List<User> resultList = defaultIdentityQuery.getResultList();

            if (!resultList.isEmpty()) {
                user = resultList.get(0);
            }

            getContext().getCache().putUser(context.getRealm(), user);
        }

        return user;
    }

    private void configurePartition(Partition partition, Object identity, IdentityType identityType) {
        if (getConfig().isModelPropertySet(PROPERTY_IDENTITY_PARTITION)) {

            // TODO implement cache support for partitions
            Object partitionInstance = getModelProperty(Object.class, identity, PROPERTY_IDENTITY_PARTITION);
            identityType.setPartition(convertPartitionEntityToPartition(partitionInstance));

        } else {
            identityType.setPartition(partition);
        }
    }

    @Override
    public Group getGroup(String groupId) {
        if (groupId == null) {
            return null;
        }

        // Check the cache first
        Realm partition = context.getRealm();
        Group group = getContext().getCache().lookupGroup(partition, groupId);

        if (group == null) {
            DefaultIdentityQuery<Group> defaultIdentityQuery = new DefaultIdentityQuery(Group.class, this);

            defaultIdentityQuery.setParameter(Group.NAME, groupId);

            List<Group> resultList = defaultIdentityQuery.getResultList();

            if (!resultList.isEmpty()) {
                group = resultList.get(0);
            }

            getContext().getCache().putGroup(partition, group);
        }

        return group;
    }

    @Override
    public Group getGroup(String name, Group parent) {
        if (name == null || parent == null) {
            return null;
        }

        Group group = getGroup(name);

        if (group.getParentGroup() == null || !group.getParentGroup().getName().equals(parent.getName())) {
            group = null;
        }

        return group;
    }

    @Override
    public Role getRole(String name) {
        if (name == null) {
            return null;
        }

        // Check the cache first
        Realm partition = context.getRealm();
        Role role = getContext().getCache().lookupRole(partition, name);

        // If the cache doesn't have a reference to the Role, we have to look up it's identity object
        // and create a Role instance based on it
        if (role == null) {
            DefaultIdentityQuery<Role> defaultIdentityQuery = new DefaultIdentityQuery(Role.class, this);

            defaultIdentityQuery.setParameter(Role.NAME, name);

            List<Role> resultList = defaultIdentityQuery.getResultList();

            if (!resultList.isEmpty()) {
                role = resultList.get(0);
            }

            getContext().getCache().putRole(partition, role);
        }

        return role;

    }

    @Override
    public Agent getAgent(String id) {
        if (id == null) {
            return null;
        }

        // Check the cache first
        Realm partition = context.getRealm();
        Agent agent = getContext().getCache().lookupAgent(partition, id);

        // If the cache doesn't have a reference to the User, we have to look up it's identity object
        // and create a User instance based on it
        if (agent == null) {
            DefaultIdentityQuery<Agent> defaultIdentityQuery = new DefaultIdentityQuery(Agent.class, this);

            defaultIdentityQuery.setParameter(Agent.ID, id);

            List<Agent> resultList = defaultIdentityQuery.getResultList();

            if (!resultList.isEmpty()) {
                agent = resultList.get(0);
            } else {
                agent = getUser(id);
            }

            getContext().getCache().putAgent(partition, agent);
        }

        return agent;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends IdentityType> List<T> fetchQueryResults(IdentityQuery<T> identityQuery) {
        List<T> result = new ArrayList<T>();

        try {
            EntityManager em = getEntityManager();

            JPACriteriaQueryBuilder criteriaBuilder = new JPACriteriaQueryBuilder(this, identityQuery);

            List<Predicate> predicates = criteriaBuilder.getPredicates();

            CriteriaQuery<?> criteria = criteriaBuilder.getCriteria();

            criteria.where(predicates.toArray(new Predicate[predicates.size()]));

            List<?> queryResult = em.createQuery(criteria).getResultList();

            for (Object identity : queryResult) {
                String discriminator = getConfig().getModelProperty(PROPERTY_IDENTITY_DISCRIMINATOR).getValue(identity)
                        .toString();
                IdentityTypeManager<? extends IdentityType> identityTypeManager = this.identityTypeStores.get(discriminator);

                T identityType = (T) identityTypeManager.fromIdentityInstance(getContext().getRealm(), identity);

                configurePartition(getContext().getRealm(), identity, identityType);
                populateAttributes(identityType, identity);

                result.add(identityType);
            }
        } catch (Exception e) {
            throw new IdentityManagementException("Error executing query.", e);
        }

        return result;
    }

    @Override
    public GroupRole createMembership(IdentityType member, Group group, Role role) {
        Property<Object> memberModelProperty = getConfig().getModelProperty(
                JPAIdentityStoreConfiguration.PROPERTY_MEMBERSHIP_MEMBER);
        Property<Object> roleModelProperty = getConfig().getModelProperty(
                JPAIdentityStoreConfiguration.PROPERTY_MEMBERSHIP_ROLE);
        Property<Object> groupModelProperty = getConfig().getModelProperty(
                JPAIdentityStoreConfiguration.PROPERTY_MEMBERSHIP_GROUP);
        SimpleGroupRole groupRole = null;

        if (member instanceof User) {
            Role storedRole = null;
            Object identityRole = null;

            if (role != null) {
                storedRole = getRole(role.getName());
                identityRole = lookupIdentityObjectById(storedRole);
            }

            User storedUser = null;
            Object identityUser = null;

            if (member != null) {
                storedUser = getUser(((User) member).getId());
                identityUser = lookupIdentityObjectById(storedUser);
            }

            Group storedGroup = null;
            Object identityGroup = null;

            if (group != null) {
                storedGroup = getGroup(group.getName());
                identityGroup = lookupIdentityObjectById(storedGroup);
            }

            Object membership = null;

            try {
                membership = getConfig().getMembershipClass().newInstance();
            } catch (Exception e) {
                throw new IdentityManagementException("Could not create membership type instance.", e);
            }

            if (storedRole != null && storedGroup != null) {
                try {
                    memberModelProperty.setValue(membership, identityUser);
                    roleModelProperty.setValue(membership, identityRole);
                    groupModelProperty.setValue(membership, identityGroup);
                } catch (Exception e) {
                }
            } else {
                if (storedRole != null) {
                    memberModelProperty.setValue(membership, identityUser);
                    roleModelProperty.setValue(membership, identityRole);
                } else {
                    memberModelProperty.setValue(membership, identityUser);
                    groupModelProperty.setValue(membership, identityGroup);
                }
            }

            getEntityManager().persist(membership);
            getEntityManager().flush();

            groupRole = new SimpleGroupRole(storedUser, storedRole, storedGroup);
        } else if (member instanceof Group) {
            throw createNotImplementedYetException();
        } else {
            throw new IllegalArgumentException("The member parameter must be an instance of User or Group");
        }

        return groupRole;
    }

    @Override
    public void removeMembership(IdentityType member, Group group, Role role) {
        Property<Object> memberModelProperty = getConfig().getModelProperty(
                JPAIdentityStoreConfiguration.PROPERTY_MEMBERSHIP_MEMBER);
        Property<Object> roleModelProperty = getConfig().getModelProperty(
                JPAIdentityStoreConfiguration.PROPERTY_MEMBERSHIP_ROLE);
        Property<Object> groupModelProperty = getConfig().getModelProperty(
                JPAIdentityStoreConfiguration.PROPERTY_MEMBERSHIP_GROUP);

        EntityManager em = getEntityManager();

        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<?> criteria = builder.createQuery(getConfig().getMembershipClass());
        Root<?> root = criteria.from(getConfig().getMembershipClass());
        List<Predicate> predicates = new ArrayList<Predicate>();

        Object identityUser = lookupIdentityObjectById(member);

        predicates.add(builder.equal(root.get(memberModelProperty.getName()), identityUser));

        if (group != null && role != null) {
            Object identityRole = lookupIdentityObjectById(role);
            Object identityGroup = lookupIdentityObjectById(group);

            predicates.add(builder.equal(root.get(roleModelProperty.getName()), identityRole));
            predicates.add(builder.equal(root.get(groupModelProperty.getName()), identityGroup));
        } else {
            if (role != null) {
                Object identityRole = lookupIdentityObjectById(role);

                predicates.add(builder.equal(root.get(roleModelProperty.getName()), identityRole));
            }

            if (group != null) {
                Object identityGroup = lookupIdentityObjectById(group);

                predicates.add(builder.equal(root.get(groupModelProperty.getName()), identityGroup));
            }
        }

        criteria.where(predicates.toArray(new Predicate[predicates.size()]));

        List<?> resultList = em.createQuery(criteria).getResultList();

        for (Object object : resultList) {
            em.remove(object);
        }

        em.flush();
    }

    @Override
    public GroupRole getMembership(IdentityType member, Group group, Role role) {
        GroupRole groupRole = null;

        List<?> resultList = Collections.emptyList();

        DefaultIdentityQuery<IdentityType> defaultIdentityQuery = new DefaultIdentityQuery(member.getClass(), this);

        defaultIdentityQuery.setParameter(IdentityType.HAS_GROUP_ROLE, new SimpleGroupRole(member, role, group));

        resultList = defaultIdentityQuery.getResultList();

        if (!resultList.isEmpty()) {
            User storedUser = getUser(((User) member).getId());
            Role storedRole = null;
            Group storedGroup = null;

            if (role != null) {
                storedRole = getRole(role.getName());
            }

            if (group != null) {
                storedGroup = getGroup(group.getName());
            }

            groupRole = new SimpleGroupRole(storedUser, storedRole, storedGroup);
        }

        return groupRole;
    }

    @Override
    public <T extends IdentityType> int countQueryResults(IdentityQuery<T> identityQuery) {
        throw createNotImplementedYetException();
    }
    
    @Override
    public void setAttribute(IdentityType identity, Attribute<? extends Serializable> providedAttrib) {
        throw createNotImplementedYetException();
    }

    @Override
    public void removeAttribute(IdentityType identity, String name) {
        throw createNotImplementedYetException();
    }

    @Override
    public <T extends Serializable> Attribute<T> getAttribute(IdentityType identityType, String attributeName) {
        throw createNotImplementedYetException();
    }

    protected EntityManager getEntityManager() {
        if (!getContext().isParameterSet(INVOCATION_CTX_ENTITY_MANAGER)) {
            throw new IllegalStateException("Error while trying to determine EntityManager - context parameter not set.");
        }

        return (EntityManager) getContext().getParameter(INVOCATION_CTX_ENTITY_MANAGER);
    }

    /**
     * <p>
     * Stores the specified {@link Attribute}.
     * </p>
     * 
     * @param identity
     * @param userAttribute
     */
    private void storeAttribute(Object identity, Attribute<? extends Serializable> userAttribute) {
        Object value = userAttribute.getValue();
        Object[] values = null;

        if (value.getClass().isArray()) {
            values = (Object[]) value;
        } else {
            values = new Object[] { value };
        }

        Property<Object> attributeNameProperty = getAttributeNameProperty();
        Property<Object> attributeIdentityProperty = getAttributeIdentityProperty();
        Property<Object> attributeValueProperty = getAttributeValueProperty();

        try {
            for (Object attribValue : values) {
                Object newInstance = getConfig().getAttributeClass().newInstance();

                attributeNameProperty.setValue(newInstance, userAttribute.getName());
                attributeValueProperty.setValue(newInstance, attribValue);
                attributeIdentityProperty.setValue(newInstance, identity);

                getEntityManager().persist(newInstance);
            }
        } catch (Exception e) {
            throw new IdentityManagementException("Error creating attributes.", e);
        }
    }

    /**
     * <p>
     * Removes all attributes for the given <code>identity</code> except to those whose names exists on provided {@link List}.
     * </p>
     * 
     * @param identity
     * @param attributesToRetain
     */
    private void removeAttributes(Object identity, List<String> attributesToRetain) {
        StringBuffer attributeNames = new StringBuffer();

        for (String string : attributesToRetain) {
            if (attributeNames.length() != 0) {
                attributeNames.append(",");
            }

            attributeNames.append("'").append(string).append("'");
        }

        List<?> storedAttributes = findAttributes(identity);

        for (Object attribute : storedAttributes) {
            String attributeName = getAttributeNameProperty().getValue(attribute).toString();

            if (!attributesToRetain.contains(attributeName)) {
                getEntityManager().remove(attribute);
            }
        }
    }

    /**
     * <p>
     * Removes all attributes for the given <code>identity</code>.
     * </p>
     * 
     * @param identity
     */
    private void removeAllAttributes(Object identity) {
        removeAttributes(identity, Collections.<String> emptyList());
    }

    private List<?> findAttributes(IdentityType identityType, String idValue, Attribute<? extends Serializable> userAttribute) {
        Property<Object> attributeIdentityProperty = getAttributeIdentityProperty();

        EntityManager em = getEntityManager();

        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<?> criteria = builder.createQuery(getConfig().getAttributeClass());
        Root<?> root = criteria.from(getConfig().getAttributeClass());
        List<Predicate> predicates = new ArrayList<Predicate>();

        Join<?, ?> join = root.join(attributeIdentityProperty.getName());

        if (isUserType(identityType.getClass())) {
            predicates.add(builder.equal(join.get(getIdentityIdProperty().getName()), idValue));
        } else {
            predicates.add(builder.equal(join.get(getConfig().getModelProperty(PROPERTY_IDENTITY_NAME).getName()), idValue));
        }

        predicates.add(builder.equal(root.get(getAttributeNameProperty().getName()), userAttribute.getName()));

        criteria.where(predicates.toArray(new Predicate[predicates.size()]));

        return em.createQuery(criteria).getResultList();
    }

    private List<?> findAttributes(Object object) {
        EntityManager em = getEntityManager();

        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<?> criteria = builder.createQuery(getConfig().getAttributeClass());
        Root<?> root = criteria.from(getConfig().getAttributeClass());
        List<Predicate> predicates = new ArrayList<Predicate>();
        predicates.add(builder.equal(root.get(getAttributeIdentityProperty().getName()), object));
        criteria.where(predicates.toArray(new Predicate[predicates.size()]));

        return em.createQuery(criteria).getResultList();
    }

    private Property<Object> getIdentityIdProperty() {
        return getConfig().getModelProperty(PROPERTY_IDENTITY_ID);
    }

    private Property<Object> getAttributeIdentityProperty() {
        return getConfig().getModelProperty(PROPERTY_ATTRIBUTE_IDENTITY);
    }

    private Property<Object> getAttributeNameProperty() {
        return getConfig().getModelProperty(PROPERTY_ATTRIBUTE_NAME);
    }

    private Property<Object> getAttributeValueProperty() {
        return getConfig().getModelProperty(PROPERTY_ATTRIBUTE_VALUE);
    }

    protected Object lookupIdentityObjectById(IdentityType identityType) {
        String id = getIdentifierValue(identityType);

        if (id == null) {
            return null;
        }

        EntityManager em = getEntityManager();

        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<?> criteria = builder.createQuery(getConfig().getIdentityClass());
        Root<?> root = criteria.from(getConfig().getIdentityClass());
        List<Predicate> predicates = new ArrayList<Predicate>();

        predicates.add(builder.equal(root.get(getConfig().getModelProperty(PROPERTY_IDENTITY_DISCRIMINATOR).getName()),
                getIdentityDiscriminator(identityType.getClass())));

        if (isUserType(identityType.getClass()) || isAgentType(identityType.getClass())) {
            predicates.add(builder.equal(root.get(getIdentityIdProperty().getName()), id));
        } else if (isGroupType(identityType.getClass()) || isRoleType(identityType.getClass())) {
            predicates.add(builder.equal(root.get(getConfig().getModelProperty(PROPERTY_IDENTITY_NAME).getName()), id));
        } else {
            throw new SecurityException("Could not lookup identity by id - unsupported IdentityType ["
                    + identityType.getClass().getName() + "]");
        }

        criteria.where(predicates.toArray(new Predicate[predicates.size()]));

        List<?> results = em.createQuery(criteria).getResultList();

        if (results.isEmpty()) {
            return null;
        }

        if (results.size() == 1) {
            return results.get(0);
        } else {
            throw new SecurityException("Error looking up identity by id - ambiguous identities found for id: [" + id + "]");
        }

    }

    private Object lookupIdentityObjectByKey(String key) {
        if (key == null) {
            return null;
        }

        EntityManager em = getEntityManager();

        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<?> criteria = builder.createQuery(getConfig().getIdentityClass());
        Root<?> root = criteria.from(getConfig().getIdentityClass());
        List<Predicate> predicates = new ArrayList<Predicate>();
        predicates.add(builder.equal(root.get(getConfig().getModelProperty(PROPERTY_IDENTITY_KEY).getName()), key));

        if (getConfig().isModelPropertySet(PROPERTY_IDENTITY_PARTITION)) {
            // We need to determine what type of key we're dealing with.. if it's a User key, then
            // we need to set the Realm value
            if (key.startsWith(User.KEY_PREFIX)) {
                if (getContext().getRealm() == null) {
                    throw new SecurityException("Cannot look up User key without a provided realm.");
                }

                predicates.add(builder.equal(root.get(getConfig().getModelProperty(PROPERTY_IDENTITY_PARTITION).getName()),
                        lookupPartitionObject(getContext().getRealm())));

                // Otherwise if it's a group or role key, we need to set either the realm or the tier
            } else if (key.startsWith(Group.KEY_PREFIX) || key.startsWith(Role.KEY_PREFIX)) {
                if (getContext().getRealm() != null && getContext().getTier() != null) {
                    throw new SecurityException("Ambiguous lookup for key [" + key
                            + "] - both Realm and Tier have been specified in context.");
                }

                if (getContext().getRealm() != null) {
                    predicates.add(builder.equal(root.get(getConfig().getModelProperty(PROPERTY_IDENTITY_PARTITION).getName()),
                            lookupPartitionObject(getContext().getRealm())));
                } else if (getContext().getTier() != null) {
                    predicates.add(builder.equal(root.get(getConfig().getModelProperty(PROPERTY_IDENTITY_PARTITION).getName()),
                            lookupPartitionObject(getContext().getTier())));
                } else {
                    throw new SecurityException("Cannot look up key [" + key + "] without a provided realm or tier.");
                }
            }
        }

        criteria.where(predicates.toArray(new Predicate[predicates.size()]));

        List<?> results = em.createQuery(criteria).getResultList();

        if (results.isEmpty()) {
            return null;
        }

        if (results.size() == 1) {
            return results.get(0);
        } else {
            throw new SecurityException("Error looking up identity by key - ambiguous identities found for key: [" + key + "]");
        }
    }

    private void removeMemberships(Object object) {
        EntityManager em = getEntityManager();

        if (getConfig().getMembershipClass() != null) {
            CriteriaBuilder builder = em.getCriteriaBuilder();
            CriteriaQuery<?> criteria = builder.createQuery(getConfig().getMembershipClass());
            Root<?> root = criteria.from(getConfig().getMembershipClass());
            List<Predicate> predicates = new ArrayList<Predicate>();
            predicates.add(builder.equal(root.get(getConfig().getModelProperty(PROPERTY_MEMBERSHIP_MEMBER).getName()), object));
            criteria.where(predicates.toArray(new Predicate[predicates.size()]));

            List<?> results = em.createQuery(criteria).getResultList();
            for (Object result : results) {
                em.remove(result);
            }

            criteria = builder.createQuery(getConfig().getMembershipClass());
            root = criteria.from(getConfig().getMembershipClass());
            predicates.clear();
            predicates.add(builder.equal(root.get(getConfig().getModelProperty(PROPERTY_MEMBERSHIP_GROUP).getName()), object));
            criteria.where(predicates.toArray(new Predicate[predicates.size()]));

            results = em.createQuery(criteria).getResultList();
            for (Object result : results) {
                em.remove(result);
            }

            criteria = builder.createQuery(getConfig().getMembershipClass());
            root = criteria.from(getConfig().getMembershipClass());
            predicates.clear();
            predicates.add(builder.equal(root.get(getConfig().getModelProperty(PROPERTY_MEMBERSHIP_ROLE).getName()), object));
            criteria.where(predicates.toArray(new Predicate[predicates.size()]));

            results = em.createQuery(criteria).getResultList();
            for (Object result : results) {
                em.remove(result);
            }

        }
    }

    private void removeAttributes(Object object) {
        EntityManager em = getEntityManager();

        if (getConfig().getAttributeClass() != null) {
            List<?> results = findAttributes(object);
            for (Object result : results) {
                em.remove(result);
            }
        }
    }

    private void removeCredentials(Object object) {
        EntityManager em = getEntityManager();

        if (getConfig().getCredentialClass() != null) {
            CriteriaBuilder builder = em.getCriteriaBuilder();
            CriteriaQuery<?> criteria = builder.createQuery(getConfig().getCredentialClass());
            Root<?> root = criteria.from(getConfig().getCredentialClass());
            List<Predicate> predicates = new ArrayList<Predicate>();
            predicates
                    .add(builder.equal(root.get(getConfig().getModelProperty(PROPERTY_CREDENTIAL_IDENTITY).getName()), object));
            criteria.where(predicates.toArray(new Predicate[predicates.size()]));

            List<?> results = em.createQuery(criteria).getResultList();
            for (Object result : results) {
                em.remove(result);
            }
        }
    }

    <P> P getModelProperty(Class<P> propertyType, Object instance, String propertyName) {
        @SuppressWarnings("unchecked")
        Property<P> property = (Property<P>) getConfig().getModelProperty(propertyName);
        return property == null ? null : property.getValue(instance);
    }

    void setModelProperty(Object instance, String propertyName, Object value) {
        setModelProperty(instance, propertyName, value, false);
    }

    void setModelProperty(Object instance, String propertyName, Object value, boolean required) {
        if (getConfig().isModelPropertySet(propertyName)) {
            getConfig().getModelProperty(propertyName).setValue(instance, value);
        } else if (required) {
            throw new IdentityManagementException("Model property [" + propertyName + "] has not been configured.");
        }
    }

    String getIdentityDiscriminator(Class<? extends IdentityType> identityType) {
        return getConfig().getIdentityTypeDiscriminator(identityType);
    }

    private void updateAttributes(IdentityType identityType, Object identity) {
        EntityManager em = getEntityManager();

        if (identityType.getAttributes() != null && !identityType.getAttributes().isEmpty()) {
            List<String> attributesToRetain = new ArrayList<String>();

            for (Attribute<? extends Serializable> userAttribute : identityType.getAttributes()) {
                attributesToRetain.add(userAttribute.getName());

                try {
                    MappedAttribute mappedAttribute = getConfig().getAttributeProperties().get(userAttribute.getName());

                    // if the attribute was mapped as a property of the identity class
                    if (mappedAttribute != null) {
                        for (String attribName : getConfig().getAttributeProperties().keySet()) {
                            MappedAttribute attrib = getConfig().getAttributeProperties().get(attribName);

                            if (userAttribute.getName().equals(attribName)) {
                                attrib.getAttributeProperty().setValue(identity, userAttribute.getValue());
                            }
                        }
                    } else {
                        // remove the attributes to persist them again. Only the current attribute, not all.
                        List<?> results = findAttributes(identityType, getIdentifierValue(identityType), userAttribute);

                        for (Object object : results) {
                            em.remove(object);
                        }

                        storeAttribute(identity, userAttribute);
                    }
                } catch (Exception e) {
                    throw new IdentityManagementException("Error setting attribute [" + userAttribute + "] for [" + identity
                            + "]", e);
                }
            }

            // remove all attributes not present in the retain list.
            if (attributesToRetain.isEmpty()) {
                removeAllAttributes(identity);
            } else {
                removeAttributes(identity, attributesToRetain);
            }
        }
    }

    /**
     * <p>
     * Resolves the value of the identifier for the given {@link IdentityType}.
     * </p>
     * 
     * @param identityType
     * @return
     */
    private String getIdentifierValue(IdentityType identityType) {
        String value = null;

        if (isUserType(identityType.getClass())) {
            value = ((User) identityType).getId();
        } else if (isAgentType(identityType.getClass())) {
            value = ((Agent) identityType).getId();
        } else if (isRoleType(identityType.getClass())) {
            value = ((Role) identityType).getName();
        } else if (isGroupType(identityType.getClass())) {
            value = ((Group) identityType).getName();
        }

        return value;
    }

    /**
     * <p>
     * Populates the given {@link IdentityType} instance with the attributes associated with the given <code>identity</code>
     * argument.
     * </p>
     * 
     * @param identityType
     * @param identity
     */
    private void populateAttributes(IdentityType identityType, Object identity) {
        try {
            for (MappedAttribute attrib : getConfig().getAttributeProperties().values()) {
                if (attrib.getIdentityProperty() != null && attrib.getIdentityProperty().getValue(identity) == null) {
                    // TODO: need to deal with AttributeType
                } else {
                    Member member = attrib.getAttributeProperty().getMember();
                    String mappedName = null;
                    Object value = null;

                    if (member instanceof Field) {
                        Field field = (Field) member;
                        IDMAttribute annotation = field.getAnnotation(IDMAttribute.class);

                        field.setAccessible(true);

                        mappedName = annotation.name();
                        value = field.get(identity);
                    }

                    identityType.setAttribute(new Attribute<Serializable>(mappedName, (Serializable) value));
                }
            }

            if (getConfig().getAttributeClass() != null) {
                EntityManager em = getEntityManager();

                CriteriaBuilder builder = em.getCriteriaBuilder();
                CriteriaQuery<?> criteria = builder.createQuery(getConfig().getAttributeClass());
                Root<?> attributeClassRoot = criteria.from(getConfig().getAttributeClass());
                List<Predicate> predicates = new ArrayList<Predicate>();

                Join identityPropertyJoin = attributeClassRoot.join(getAttributeIdentityProperty().getName());
                String propertyNameToJoin = getIdentityIdProperty().getName();

                if (isRoleType(identityType.getClass()) || isGroupType(identityType.getClass())) {
                    propertyNameToJoin = getConfig().getModelProperty(PROPERTY_IDENTITY_NAME).getName();
                }

                predicates.add(builder.equal(identityPropertyJoin.get(propertyNameToJoin), getIdentifierValue(identityType)));

                criteria.where(predicates.toArray(new Predicate[predicates.size()]));

                List<?> results = em.createQuery(criteria).getResultList();

                if (!results.isEmpty()) {
                    for (Object object : results) {
                        Property<Object> attributeNameProperty = getAttributeNameProperty();
                        Property<Object> attributeValueProperty = getAttributeValueProperty();

                        String attribName = (String) attributeNameProperty.getValue(object);
                        Serializable attribValue = (Serializable) attributeValueProperty.getValue(object);

                        Attribute<Serializable> identityTypeAttribute = identityType.getAttribute(attribName);

                        if (identityTypeAttribute == null) {
                            identityTypeAttribute = new Attribute<Serializable>(attribName, attribValue);
                            identityType.setAttribute(identityTypeAttribute);
                        } else {
                            // if it is a multi-valued attribute
                            if (identityTypeAttribute.getValue() != null) {
                                String[] values = null;

                                if (identityTypeAttribute.getValue().getClass().isArray()) {
                                    values = (String[]) identityTypeAttribute.getValue();
                                } else {
                                    values = new String[1];
                                    values[0] = identityTypeAttribute.getValue().toString();
                                }

                                String[] newValues = Arrays.copyOf(values, values.length + 1);

                                newValues[newValues.length - 1] = attribValue.toString();

                                identityTypeAttribute.setValue(newValues);

                                identityType.setAttribute(identityTypeAttribute);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new IdentityManagementException("Error setting attribute.", e);
        }
    }

    private Partition convertPartitionEntityToPartition(Object instance) {
        return null;
    }

    Object lookupPartitionObject(Partition partition) {
        // TODO implement realm lookup
        return null;
    }

    /**
     * <p>
     * Checks if the provided {@link IdentityType} instance is valid or not null. An exception will be thrown when the
     * validation fails.
     * </p>
     * 
     * @param identityType
     * @throws IdentityManagementException
     */
    private void checkInvalidIdentityType(IdentityType identityType) throws IdentityManagementException {
        if (identityType == null) {
            throw new IdentityManagementException("The provided IdentityType instance is invalid or was null.");
        }
    }

    /**
     * <p>
     * Returns a {@link Object} for the Identity Class used to store {@link IdentityType} instances. If no instance was found an
     * exception will be thrown.
     * </p>
     * 
     * @param identityType
     * @return
     * @throws IdentityManagementException
     */
    private Object getIdentityObject(IdentityType identityType) throws IdentityManagementException {
        Object identity = lookupIdentityObjectById(identityType);

        if (identity == null) {
            throw new IdentityManagementException("The provided IdentityType instance does not exists.");
        }

        return identity;
    }

    IdentityTypeManager<IdentityType> getIdentityTypeManager(Class<? extends IdentityType> identityTypeClass) {
        IdentityTypeManager<IdentityType> identityTypeManager = (IdentityTypeManager<IdentityType>) this.identityTypeStores
                .get(getIdentityDiscriminator(identityTypeClass));
        return identityTypeManager;
    }

}