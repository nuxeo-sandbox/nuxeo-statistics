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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataGenerator {

	public static List<Map<String, Long>> genData(long t0, int deltaS, int nbRecords) {

		List<Map<String, Long>> result = new ArrayList<>();
		Long ts = t0;

		for (int i = 0; i < nbRecords; i++) {

			Map<String, Long> metrics = new HashMap<String, Long>();

			metrics.put("ts", t0 - (nbRecords - i) * deltaS);

			metrics.put("nuxeo.statistics.metricA", i * 10L);
			if (i % 2 == 0) {
				metrics.put("nuxeo.statistics.metricB", (i / 2) * 10L);
			}
			if (i % 3 == 0) {
				metrics.put("nuxeo.statistics.metricC", (i / 3) * 10L);
			}
			result.add(metrics);
		}

		return result;
	}

}
