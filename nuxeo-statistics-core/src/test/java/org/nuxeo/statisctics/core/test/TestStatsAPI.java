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
package org.nuxeo.statisctics.core.test;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.stream.RuntimeStreamFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.statistics.StatisticsService;
import org.nuxeo.statistics.api.FetchStatisticOperation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features({ RuntimeStreamFeature.class, AutomationFeature.class })
@Deploy({ "org.nuxeo.statistics.core", "org.nuxeo.statistics.core.test:test-metrics-contrib.xml",
		"org.nuxeo.statistics.core.test:kv-store-contrib.xml" })
public class TestStatsAPI {

	protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@Inject
	protected CoreSession session;

	@Inject
	protected AutomationService as;

	protected Long t0;

	protected void initFakeMetrics() {

		t0 = System.currentTimeMillis() / 1000;

		List<Map<String, Long>> metrics = DataGenerator.genData(t0, 1, 30);

		// store a timeserie
		Framework.getService(StatisticsService.class).storeStatisticsTimeSerie(metrics);

	}

	@Test
	public void testAutomationAPI() throws Exception {

		initFakeMetrics();

		OperationContext ctx = new OperationContext(session);
		Map<String, Object> params = new HashMap<>();

		// call without any filter
		String json = (String) as.run(ctx, FetchStatisticOperation.ID, params);
		List<Map<String, Long>> ts = (List<Map<String, Long>>) OBJECT_MAPPER.readValue(json,
				new TypeReference<List<Map<String, Long>>>() {
				});
		assertEquals(30, ts.size());
		assertEquals(4, ts.get(0).size());

		// call with regexp
		ctx = new OperationContext(session);
		params.put("filter", "nuxeo.statistics.metricA");
		json = (String) as.run(ctx, FetchStatisticOperation.ID, params);
		ts = (List<Map<String, Long>>) OBJECT_MAPPER.readValue(json, new TypeReference<List<Map<String, Long>>>() {
		});
		assertEquals(30, ts.size());
		assertEquals(2, ts.get(0).size());

		// call with regexp and duration
		ctx = new OperationContext(session);
		params.put("filter", "nuxeo.statistics.metricA");
		params.put("duration", "10s");
		json = (String) as.run(ctx, FetchStatisticOperation.ID, params);
		ts = (List<Map<String, Long>>) OBJECT_MAPPER.readValue(json, new TypeReference<List<Map<String, Long>>>() {
		});
		assertEquals(10, ts.size());
		assertEquals(2, ts.get(0).size());

		// call with regexp, start and duration
		ctx = new OperationContext(session);
		params.put("filter", "nuxeo.statistics.metricA");
		params.put("start", t0 - 5);
		params.put("duration", "10s");

		json = (String) as.run(ctx, FetchStatisticOperation.ID, params);
		ts = (List<Map<String, Long>>) OBJECT_MAPPER.readValue(json, new TypeReference<List<Map<String, Long>>>() {
		});

		assertEquals(11, ts.size());
		assertEquals(2, ts.get(0).size());

		// call with regexp, start, duration and alignment
		ctx = new OperationContext(session);
		params.put("filter", "nuxeo.statistics.metricA");
		params.put("start", t0 - 5);
		params.put("duration", "14s");
		params.put("maxValues", 5);

		json = (String) as.run(ctx, FetchStatisticOperation.ID, params);
		ts = (List<Map<String, Long>>) OBJECT_MAPPER.readValue(json, new TypeReference<List<Map<String, Long>>>() {
		});
		assertEquals(5, ts.size());
		assertEquals(2, ts.get(0).size());

	}

}
