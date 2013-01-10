package org.picketlink.idm.model;

import org.picketlink.idm.model.annotation.RelationshipIdentity;
import org.picketlink.idm.query.QueryParameter;

/**
 * A Relationship that represents an identity's membership in a Group
 * 
 * @author Shane Bryzak
 */
public class GroupMembership extends AbstractAttributedType implements Relationship {
    QueryParameter MEMBER = new QueryParameter() {};

    QueryParameter GROUP = new QueryParameter() {};

    private static final long serialVersionUID = 6851576454138812116L;

    private IdentityType member;
    private Group group;

    public GroupMembership(IdentityType member, Group group) {
        this.member = member;
        this.group = group;
    }

    @RelationshipIdentity
    public IdentityType getMember() {
        return member;
    }

    @RelationshipIdentity
    public Group getGroup() {
        return group;
    }
}
