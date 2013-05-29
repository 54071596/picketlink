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

package org.picketlink.test.idm.relationship;

import org.picketlink.idm.model.Group;
import org.picketlink.idm.model.Partition;

/**
 * <p>
 * Test case for the relationship between {@link User} and {@link Role} types.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 *
 */
public class GroupGrantRelationshipTestCase extends AbstractGrantRelationshipTestCase<Group> {

    @Override
    protected Group createIdentityType(String name) {
        return createIdentityType(name, null);
    }
    
    @Override
    protected Group createIdentityType(String name, Partition partition) {
        if (name == null) {
            name = "someGroup";
        }
        
        if (partition != null) {
            return createGroup(name, null, partition);
        } else {
            return createGroup(name, null);
        }
    }

    @Override
    protected Group getIdentityType() {
        return getIdentityManager().getGroup("someGroup");
    }
}
