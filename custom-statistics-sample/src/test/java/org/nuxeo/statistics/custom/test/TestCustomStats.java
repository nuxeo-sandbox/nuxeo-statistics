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

package org.nuxeo.statistics.custom.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.nuxeo.lib.stream.computation.log.ComputationRunner.NUXEO_METRICS_REGISTRY_NAME;

import java.util.SortedMap;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.stream.RuntimeStreamFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.statistics.StatisticsComputer;
import org.nuxeo.statistics.StatisticsService;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;

import io.dropwizard.metrics5.Gauge;
import io.dropwizard.metrics5.Metric;
import io.dropwizard.metrics5.MetricFilter;
import io.dropwizard.metrics5.MetricName;
import io.dropwizard.metrics5.MetricRegistry;
import io.dropwizard.metrics5.SharedMetricRegistries;

@RunWith(FeaturesRunner.class)
@Features({ RuntimeStreamFeature.class, CoreFeature.class })
@Deploy({ "org.nuxeo.statistics.core", "org.nuxeo.statistics.custom.sample",
		"org.nuxeo.statistics.custom.sample.test:kv-store-contrib.xml" })
public class TestCustomStats {

	protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@Inject
	protected StatisticsService stats;

	@Test
	public void checkCustomStatsDeployedAndWorking() throws Exception {

		// check that the new computed is deployed
		StatisticsComputer customComputer = stats.getComputer("custom");
		assertNotNull(customComputer);

		// read the value until computed
		Long value = stats.getStatistic("nuxeo.statistics.custom.foo");
		int nbTries = 10;
		while (value == null && nbTries > 0) {
			value = stats.getStatistic("nuxeo.statistics.custom.foo");
			nbTries--;
			Thread.sleep(1000);
		}
		assertNotNull(value);

		// access with tags
		value = stats.getStatistic("nuxeo.statistics.custom.bar.default");
		assertNotNull(value);

		// check metrics registered
		MetricRegistry registry = SharedMetricRegistries.getOrCreate(NUXEO_METRICS_REGISTRY_NAME);
		MetricFilter filter = new MetricFilter() {
			@Override
			public boolean matches(MetricName name, Metric metric) {
				return name.toString().startsWith("nuxeo.statistics.custom");
			}
		};

		SortedMap<MetricName, Gauge> gauges = registry.getGauges(filter);
		assertEquals(2, gauges.size());
	}

}
