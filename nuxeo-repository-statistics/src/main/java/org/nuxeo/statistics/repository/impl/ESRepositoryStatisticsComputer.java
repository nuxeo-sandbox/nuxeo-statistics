/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
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
 *
 * Contributors:
 *      Nelson Silva
 */
package org.nuxeo.statistics.repository.impl;

import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.client.core.CountResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.client.ESRestClient;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.statistics.repository.RepositoryStatisticsComputer;

public class ESRepositoryStatisticsComputer extends RepositoryStatisticsComputer {

    protected final String index;

    public ESRepositoryStatisticsComputer() {
        index = getESIndexName();
    }

    @Override
    public long getTotalDocuments() {
        return count(QueryBuilders.matchAllQuery());
    }

    @Override
    public long getDeletedDocuments() {
        return count(QueryBuilders.matchQuery("ecm:currentLifeCycleState", "deleted"));
    }

    protected String getESIndexName() {
        ElasticSearchAdmin esa = Framework.getService(ElasticSearchAdmin.class);
        return esa.getIndexNameForRepository("default");
    }

    protected ESRestClient getClient() {
        return (ESRestClient) Framework.getService(ElasticSearchAdmin.class).getClient();
    }

    protected long count(QueryBuilder query) {
        CountRequest request = new CountRequest()
                .indices(index).query(query);
        CountResponse response = getClient().count(request);
        return response.getCount();
    }

}
