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
import static org.junit.Assert.assertFalse;

import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.nuxeo.common.utils.DurationUtils;
import org.nuxeo.statistics.api.MetricsFilter;

public class TestMetricFilter {

	@Test
	public void canAlignMetrics() {

		int nbM = 60;

		List<Map<String, Long>> metrics = DataGenerator.genData(nbM, 1, nbM);

		assertEquals(nbM, metrics.size());
		System.out.println(metrics);
		// sanity checks
		assertEquals((Long) 0L, metrics.get(0).get("ts"));
		assertEquals((Long) 0L, metrics.get(0).get("nuxeo.statistics.metricA"));
		assertEquals((Long) 0L, metrics.get(0).get("nuxeo.statistics.metricB"));
		assertEquals((Long) 0L, metrics.get(0).get("nuxeo.statistics.metricC"));

		assertEquals((Long) 1L, metrics.get(1).get("ts"));
		assertEquals((Long) 10L, metrics.get(1).get("nuxeo.statistics.metricA"));
		assertEquals((Long) null, metrics.get(1).get("nuxeo.statistics.metricB"));
		assertEquals((Long) null, metrics.get(1).get("nuxeo.statistics.metricC"));

		assertEquals((Long) 2L, metrics.get(2).get("ts"));
		assertEquals((Long) 20L, metrics.get(2).get("nuxeo.statistics.metricA"));
		assertEquals((Long) 10L, metrics.get(2).get("nuxeo.statistics.metricB"));
		assertEquals((Long) null, metrics.get(2).get("nuxeo.statistics.metricC"));

		assertEquals((Long) 3L, metrics.get(3).get("ts"));
		assertEquals((Long) 30L, metrics.get(3).get("nuxeo.statistics.metricA"));
		assertEquals((Long) null, metrics.get(3).get("nuxeo.statistics.metricB"));
		assertEquals((Long) 10L, metrics.get(3).get("nuxeo.statistics.metricC"));

		MetricsFilter mf = new MetricsFilter(30);

		List<Map<String, Long>> filtred = mf.process(metrics);
		System.out.println(filtred);
		assertEquals(30, filtred.size());
		assertEquals((Long) 0L, filtred.get(0).get("ts"));
		assertEquals((Long) 5L, filtred.get(0).get("nuxeo.statistics.metricA"));
		assertEquals((Long) 0L, filtred.get(0).get("nuxeo.statistics.metricB"));
		assertEquals((Long) 0L, filtred.get(0).get("nuxeo.statistics.metricC"));

		assertEquals((Long) 2L, filtred.get(1).get("ts"));
		assertEquals((Long) 25L, filtred.get(1).get("nuxeo.statistics.metricA"));
		assertEquals((Long) 10L, filtred.get(1).get("nuxeo.statistics.metricB"));
		assertEquals((Long) 10L, filtred.get(1).get("nuxeo.statistics.metricC"));

		assertEquals((Long) 4L, filtred.get(2).get("ts"));
		assertEquals((Long) 45L, filtred.get(2).get("nuxeo.statistics.metricA"));
		assertEquals((Long) 20L, filtred.get(2).get("nuxeo.statistics.metricB"));
		assertEquals((Long) null, filtred.get(2).get("nuxeo.statistics.metricC"));
		assertFalse(filtred.get(2).containsKey("nuxeo.statistics.metricC"));

		assertEquals((Long) 6L, filtred.get(3).get("ts"));
		assertEquals((Long) 65L, filtred.get(3).get("nuxeo.statistics.metricA"));
		assertEquals((Long) 30L, filtred.get(3).get("nuxeo.statistics.metricB"));
		assertEquals((Long) 20L, filtred.get(3).get("nuxeo.statistics.metricC"));

		assertEquals((Long) 8L, filtred.get(4).get("ts"));
		assertEquals((Long) 85L, filtred.get(4).get("nuxeo.statistics.metricA"));
		assertEquals((Long) 40L, filtred.get(4).get("nuxeo.statistics.metricB"));
		assertEquals((Long) 30L, filtred.get(4).get("nuxeo.statistics.metricC"));

		mf = new MetricsFilter(20);

		filtred = mf.process(metrics);
		System.out.println(filtred);
		assertEquals(20, filtred.size());
		assertEquals((Long) 1L, filtred.get(0).get("ts"));
		assertEquals((Long) 10L, filtred.get(0).get("nuxeo.statistics.metricA"));
		assertEquals((Long) 5L, filtred.get(0).get("nuxeo.statistics.metricB"));
		assertEquals((Long) 0L, filtred.get(0).get("nuxeo.statistics.metricC"));

		assertEquals((Long) 4L, filtred.get(1).get("ts"));
		assertEquals((Long) 40L, filtred.get(1).get("nuxeo.statistics.metricA"));
		assertEquals((Long) 20L, filtred.get(1).get("nuxeo.statistics.metricB"));
		assertEquals((Long) 10L, filtred.get(1).get("nuxeo.statistics.metricC"));

		mf = new MetricsFilter(9);

		filtred = mf.process(metrics);
		System.out.println(filtred);
		assertEquals(9, filtred.size());
	}

	@Test
	public void canFilderMetricsOnRegExp() {

		int nbM = 60;

		List<Map<String, Long>> metrics = DataGenerator.genData(nbM, 1, nbM);

		// matchAll filter
		MetricsFilter mf = new MetricsFilter("nuxeo.statistics.*", null, null, null);
		List<Map<String, Long>> filtred = mf.process(metrics);
		System.out.println(filtred);
		assertEquals(60, filtred.size());

		assertEquals((Long) 0L, filtred.get(0).get("ts"));
		assertEquals((Long) 0L, filtred.get(0).get("nuxeo.statistics.metricA"));
		assertEquals((Long) 0L, filtred.get(0).get("nuxeo.statistics.metricB"));
		assertEquals((Long) 0L, filtred.get(0).get("nuxeo.statistics.metricC"));

		// match one filter
		mf = new MetricsFilter("nuxeo.statistics.metricB", null, null, null);
		filtred = mf.process(metrics);
		System.out.println(filtred);
		assertEquals(30, filtred.size());

		assertEquals((Long) 0L, filtred.get(0).get("ts"));
		assertEquals(null, filtred.get(0).get("nuxeo.statistics.metricA"));
		assertEquals((Long) 0L, filtred.get(0).get("nuxeo.statistics.metricB"));
		assertEquals(null, filtred.get(0).get("nuxeo.statistics.metricC"));

		// match one filter
		mf = new MetricsFilter("nuxeo.statistics.metricC", null, null, null);
		filtred = mf.process(metrics);
		System.out.println(filtred);
		assertEquals(20, filtred.size());

		assertEquals((Long) 0L, filtred.get(0).get("ts"));
		assertEquals(null, filtred.get(0).get("nuxeo.statistics.metricA"));
		assertEquals(null, filtred.get(0).get("nuxeo.statistics.metricB"));
		assertEquals((Long) 0L, filtred.get(0).get("nuxeo.statistics.metricC"));

		// match many filter
		mf = new MetricsFilter("nuxeo.statistics.metric[BC]", null, null, null);
		filtred = mf.process(metrics);
		System.out.println(filtred);
		assertEquals(40, filtred.size());

		assertEquals((Long) 0L, filtred.get(0).get("ts"));
		assertEquals(null, filtred.get(0).get("nuxeo.statistics.metricA"));
		assertEquals((Long) 0L, filtred.get(0).get("nuxeo.statistics.metricB"));
		assertEquals((Long) 0L, filtred.get(0).get("nuxeo.statistics.metricC"));

		assertEquals((Long) 2L, filtred.get(1).get("ts"));
		assertEquals(null, filtred.get(1).get("nuxeo.statistics.metricA"));
		assertEquals((Long) 10L, filtred.get(1).get("nuxeo.statistics.metricB"));
		assertEquals(null, filtred.get(1).get("nuxeo.statistics.metricC"));
	}

	@Test
	public void canFilderMetricsOnTime() {

		int nbM = 60;

		DurationUtils.parse("365d");

		Long t0 = System.currentTimeMillis() / 1000;
		System.out.print("T0=" + t0);

		List<Map<String, Long>> metrics = DataGenerator.genData(t0, 1, nbM);
		System.out.println(metrics);

		// matchAll filter
		MetricsFilter mf = new MetricsFilter(null, t0, null, null);
		List<Map<String, Long>> filtred = mf.process(metrics);
		System.out.println(filtred);
		assertEquals(60, filtred.size());

		// only 30s
		mf = new MetricsFilter(null, t0, "30s", null);
		filtred = mf.process(metrics);
		System.out.println(filtred);
		assertEquals(30, filtred.size());

		mf = new MetricsFilter(null, null, "30s", null);
		filtred = mf.process(metrics);
		System.out.println(filtred);
		assertEquals(30, filtred.size());

		// only 30s
		mf = new MetricsFilter(null, t0 - 31, "30s", null);
		filtred = mf.process(metrics);
		System.out.println(filtred);
		assertEquals(30, filtred.size());

		// no data
		mf = new MetricsFilter(null, t0 - 61, null, null);
		filtred = mf.process(metrics);
		System.out.println(filtred);
		assertEquals(0, filtred.size());

		// only 1s
		mf = new MetricsFilter(null, t0, "1s", null);
		filtred = mf.process(metrics);
		System.out.println(filtred);
		assertEquals(1, filtred.size());

	}

}
