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
package org.nuxeo.statistics;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.dropwizard.metrics5.MetricName;

public class MetricsNameHelper {

	public static String getMetricKey(MetricName metricName) {
		String key = metricName.getKey();
		return getMetricKey(key, metricName.getTags());
	}

	public static String getMetricKey(String key, Map<String, String> tags) {
		Set<String> tagNames = tags.keySet();
		List<String> sortedTagNames = tagNames.stream().collect(Collectors.toList());
		Collections.sort(sortedTagNames);
		for (String tag : sortedTagNames) {
			key = key + "." + tags.get(tag);
		}
		return key;
	}

}
