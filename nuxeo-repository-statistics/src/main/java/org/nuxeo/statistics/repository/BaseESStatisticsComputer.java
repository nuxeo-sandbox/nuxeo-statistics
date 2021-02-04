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
 *      Tiry
 */
package org.nuxeo.statistics.repository;

import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.client.ESRestClient;
import org.nuxeo.runtime.api.Framework;

public abstract class BaseESStatisticsComputer extends AbstractStatisticsComputer {
   
	
	protected String getESIndexName(String repositoryName) {
		ElasticSearchAdmin esa = Framework.getService(ElasticSearchAdmin.class);
		return esa.getIndexNameForRepository(repositoryName);
	}

	protected ESRestClient getClient() {
		return (ESRestClient) Framework.getService(ElasticSearchAdmin.class).getClient();
	}

}
