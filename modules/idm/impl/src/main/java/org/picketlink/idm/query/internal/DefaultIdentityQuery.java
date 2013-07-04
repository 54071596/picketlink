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

package org.picketlink.idm.query.internal;

import org.picketlink.idm.IDMMessages;
import org.picketlink.idm.model.IdentityType;
import org.picketlink.idm.query.IdentityQuery;
import org.picketlink.idm.query.QueryParameter;
import org.picketlink.idm.spi.IdentityStore;
import org.picketlink.idm.spi.IdentityContext;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Default IdentityQuery implementation.
 *
 * @author Shane Bryzak
 *
 * @param <T>
 */
public class DefaultIdentityQuery<T extends IdentityType> implements IdentityQuery<T> {

    private final Map<QueryParameter, Object[]> parameters = new LinkedHashMap<QueryParameter, Object[]>();
    private final IdentityContext context;
    private final IdentityStore<?> identityStore;
    private final Class<T> identityType;
    private int offset;
    private int limit;
    private QueryParameter[] sortParameters;
    private boolean sortAscending = true;

    public DefaultIdentityQuery(IdentityContext context, Class<T> identityType, IdentityStore<?> identityStore) {
        this.context = context;
        this.identityStore = identityStore;
        this.identityType = identityType;
    }

    @Override
    public IdentityQuery<T> setParameter(QueryParameter param, Object... value) {
        parameters.put(param, value);
        return this;
    }

    @Override
    public Class<T> getIdentityType() {
        return identityType;
    }

    @Override
    public Map<QueryParameter, Object[]> getParameters() {
        return parameters;
    }

    @Override
    public Object[] getParameter(QueryParameter queryParameter) {
        return this.parameters.get(queryParameter);
    }

    @Override
    public Map<QueryParameter, Object[]> getParameters(Class<?> type) {
        Map<QueryParameter, Object[]> typedParameters = new HashMap<QueryParameter, Object[]>();

        Set<Entry<QueryParameter, Object[]>> entrySet = this.parameters.entrySet();

        for (Entry<QueryParameter, Object[]> entry : entrySet) {
            if (type.isInstance(entry.getKey())) {
                typedParameters.put(entry.getKey(), entry.getValue());
            }
        }

        return typedParameters;
    }

    @Override
    public int getLimit() {
        return limit;
    }

    @Override
    public int getOffset() {
        return offset;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public QueryParameter[] getSortParameters() {
        return sortParameters;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSortAscending() {
        return sortAscending;
    }

    /*@Override
    public IdentityQuery<T> setParameter(QueryParameter param, Operator operator, Object value) {
        // TODO Auto-generated method stub
        return null;
    }*/

    @Override
    public List<T> getResultList() {
        List<T> result = null;

        try {
            result = this.identityStore.fetchQueryResults(context, this);
        } catch (Exception e) {
            throw IDMMessages.MESSAGES.identityTypeQueryFailed(this, e);
        }

        return result;
    }

    @Override
    public int getResultCount() {
        return this.identityStore.countQueryResults(context, this);
    }

    @Override
    public IdentityQuery<T> setOffset(int offset) {
        this.offset = offset;
        return this;
    }

    @Override
    public IdentityQuery<T> setLimit(int limit) {
        this.limit = limit;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IdentityQuery<T> setSortParameters(QueryParameter... sortParameters) {
        this.sortParameters = sortParameters;
        return this;
    }

    @Override
    public IdentityQuery<T> setSortAscending(boolean sortAscending) {
        this.sortAscending = sortAscending;
        return this;
    }
}
