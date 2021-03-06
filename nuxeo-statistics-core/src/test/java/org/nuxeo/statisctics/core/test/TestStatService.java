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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.lib.stream.computation.log.ComputationRunner.NUXEO_METRICS_REGISTRY_NAME;

import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.stream.RuntimeStreamFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.statistics.StatisticsComputer;
import org.nuxeo.statistics.StatisticsService;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.dropwizard.metrics5.Gauge;
import io.dropwizard.metrics5.Metric;
import io.dropwizard.metrics5.MetricFilter;
import io.dropwizard.metrics5.MetricName;
import io.dropwizard.metrics5.MetricRegistry;
import io.dropwizard.metrics5.SharedMetricRegistries;

@RunWith(FeaturesRunner.class)
@Features({ RuntimeStreamFeature.class, CoreFeature.class })
@Deploy({ "org.nuxeo.statistics.core", "org.nuxeo.statistics.core.test:test-metrics-contrib.xml",
		"org.nuxeo.statistics.core.test:kv-store-contrib.xml" })
public class TestStatService {

	protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@Test
	public void checkServiceAndContrinbutionDeployed() throws Exception {
		StatisticsService stats = Framework.getService(StatisticsService.class);
		assertNotNull(stats);

		StatisticsComputer dummy = stats.getComputer("dummy");
		assertNotNull(dummy);
	}

	@Test
	public void checkMetricsExposed() throws Exception {

		// run for 10 seconds to be sure we can get aggregates
		Thread.sleep(10000);

		MetricRegistry registry = SharedMetricRegistries.getOrCreate(NUXEO_METRICS_REGISTRY_NAME);

		MetricFilter filter = new MetricFilter() {
			@Override
			public boolean matches(MetricName name, Metric metric) {
				return name.toString().startsWith("nuxeo.statistics.dummy");
			}
		};

		SortedMap<MetricName, Gauge> gauges = registry.getGauges(filter);
		assertEquals(2, gauges.size());
		int checks = 0;

		for (MetricName mn : gauges.keySet()) {
			if (mn.getKey().equals("nuxeo.statistics.dummy.counter")) {
				assertTrue(mn.getTags().values().contains("bar"));
				checks++;
			} else if (mn.getKey().equals("nuxeo.statistics.dummy.key")) {
				assertEquals(DummyStatisticsComputer.key, (Long) gauges.get(mn).getValue());
				checks++;
			} else {
				System.out.println("unknonw key " + mn.getKey());
			}
		}
		assertEquals(2, checks);

		List<Map<String, Long>> ts = Framework.getService(StatisticsService.class).getStatisticsTimeSerie();
		System.out.print(ts);
		assertTrue(ts.size() >= 1);
		ts.get(0).containsKey("nuxeo.statistics.dummy.counter");

	}

}
