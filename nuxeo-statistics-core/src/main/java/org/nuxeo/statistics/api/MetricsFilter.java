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

package org.nuxeo.statistics.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.nuxeo.common.utils.DurationUtils;

public class MetricsFilter {

	protected final Integer maxValues;

	protected String filter = null;

	protected Long start = null;

	protected Long end = null;

	protected String duration = null;

	protected Pattern regexp = null;

	public static final String DEFAULT_DURATION = "365d";

	public MetricsFilter(Integer maxValues) {
		this.maxValues = maxValues;
	}

	public MetricsFilter(String filter, Long start, String duration, Integer maxValues) {
		this.maxValues = maxValues;

		// santity checks
		if (start == null && duration != null) {
			start = System.currentTimeMillis() / 1000;
		} else if (start != null && duration == null) {
			duration = DEFAULT_DURATION;
		}
		if (start != null && duration != null) {
			end = start - DurationUtils.parse(duration).getSeconds();
		}

		this.filter = filter;
		this.duration = duration;
		this.start = start;

		if (filter != null) {
			regexp = Pattern.compile(filter);
		}

	}

	public List<Map<String, Long>> process(List<Map<String, Long>> ts) {
		if (ts == null || ts.size() == 0) {
			return Collections.emptyList();
		}

		List<Map<String, Long>> filtred = null;
		// filter if needed
		if (filter != null || start != null || duration != null) {
			filtred = filter(ts);
		} else {
			filtred = ts;
		}
		if (maxValues != null) {
			return align(filtred);
		} else {
			return filtred;
		}
	}

	protected List<Map<String, Long>> filter(List<Map<String, Long>> ts) {

		List<Map<String, Long>> filtered = new ArrayList<>();

		for (Map<String, Long> metrics : ts) {

			Map<String, Long> filterdMetrics = new HashMap<>();

			if (regexp != null) {
				for (String key : metrics.keySet()) {
					if ("ts".equals(key) || regexp.matcher(key).matches()) {
						filterdMetrics.put(key, metrics.get(key));
					}
				}
			} else {
				filterdMetrics = metrics;
			}

			if (start != null && end != null) {
				long t = filterdMetrics.get("ts");

				if (t <= start && t >= end && filterdMetrics.size() > 1) {
					filtered.add(filterdMetrics);
				}
			} else {
				if (filterdMetrics.size() > 1) {
					filtered.add(filterdMetrics);
				}
			}
		}
		return filtered;
	}

	protected List<Map<String, Long>> align(List<Map<String, Long>> ts) {

		if (ts.size() <= maxValues) {
			return ts;
		}

		// divide + round up

		long groupSize = (ts.size() + maxValues - 1) / maxValues;

		List<Map<String, Long>> aligned = new ArrayList<>();

		Map<String, List<Long>> agg = null;

		for (int i = 0; i < ts.size(); i++) {

			if (i % groupSize == 0) {
				if (agg != null) {
					aligned.add(average(agg));
					agg = new HashMap<>();
				}
			}
			if (agg == null) {
				agg = new HashMap<>();
			}
			for (String key : ts.get(i).keySet()) {
				accumulate(key, ts.get(i).get(key), agg);
			}
		}
		aligned.add(average(agg));
		return aligned;
	}

	protected void accumulate(String key, Long value, Map<String, List<Long>> agg) {
		if (!agg.containsKey(key)) {
			agg.put(key, new ArrayList<Long>());
		}
		if (value != null) {
			agg.get(key).add(value);
		}
	}

	protected Map<String, Long> average(Map<String, List<Long>> source) {

		Map<String, Long> result = new HashMap<>();

		for (String key : source.keySet()) {
			Long avg = source.get(key).stream().reduce(0L, Long::sum) / source.get(key).size();
			result.put(key, avg);
		}
		return result;
	}
}
