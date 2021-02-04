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
package org.nuxeo.statistics.repository.impl;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.nuxeo.statistics.repository.BaseESStatisticsComputer;

import io.dropwizard.metrics5.MetricName;

public class ESAuditStatisticsComputer extends BaseESStatisticsComputer {

	private static final Log log = LogFactory.getLog(ESAuditStatisticsComputer.class);

	protected static final String AUDIT_INDEX = "audit";

	public ESAuditStatisticsComputer() {
	}

	@Override
	public Map<MetricName, Long> get() {
		return getCountsPerEventTypes();
	}
	
	protected SearchRequest searchRequest() {
		return new SearchRequest(AUDIT_INDEX).searchType(SearchType.DFS_QUERY_THEN_FETCH);
	}

	protected Map<MetricName, Long> getCountsPerEventTypes() {

		Map<MetricName, Long> ret = new LinkedHashMap<>();
		SearchRequest searchRequest = searchRequest();

		SearchSourceBuilder sourceBuilder = new SearchSourceBuilder().size(20)
				.query(QueryBuilders.rangeQuery("eventDate").gte("now-1h"))
				.aggregation(AggregationBuilders.terms("eventId").field("eventId"));
		;
		searchRequest.source(sourceBuilder);
		try {
			SearchResponse response = getClient().search(searchRequest);

			Terms terms = response.getAggregations().get("eventId");
			for (Terms.Bucket term : terms.getBuckets()) {

				MetricName mn = mkMetricName("audit", "event").tagged("event", term.getKeyAsString());
				ret.put(mn, term.getDocCount());
			}
		} catch (Exception e) {
			log.error("Failed to get Type Cardinality", e);
		}
		return ret;
	}

}
