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
package org.nuxeo.statistics.custom;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.nuxeo.statistics.AbstractStatisticsComputer;

import io.dropwizard.metrics5.MetricName;

public class CustomStatisticsComputerSample extends AbstractStatisticsComputer {
	
	@Override
	public Map<MetricName, Long> get() {
		
		Map<MetricName, Long> metrics = new HashMap<>();
		
		metrics.putAll(computeFoo());
		metrics.putAll(computeBar());		
		
		return metrics;
	}

	protected Map<MetricName, Long> computeFoo() {
		
		Map<MetricName, Long> metrics= new HashMap<>();
		
		// build name and add tags
		MetricName name = mkMetricName("custom","foo").tagged("app","XXX");
			
		// fake metric computation
		metrics.put(name, new Random().nextLong());
		
		return metrics;		
	}

    protected Map<MetricName, Long> computeBar() {
		
		Map<MetricName, Long> metrics= new HashMap<>();
		
		// build name and add tags
		MetricName name = mkMetricName("custom","bar").tagged("app","XXX").tagged("domain","YYY");
			
		// fake metric computation
		metrics.put(name, new Random().nextLong());
		
		return metrics;		
	}

}
