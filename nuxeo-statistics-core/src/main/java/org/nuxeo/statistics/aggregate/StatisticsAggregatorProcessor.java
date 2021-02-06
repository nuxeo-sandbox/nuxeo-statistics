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
 *      Nelson Silva
 */
package org.nuxeo.statistics.aggregate;

import static org.nuxeo.lib.stream.computation.AbstractComputation.INPUT_1;
import static org.nuxeo.lib.stream.computation.AbstractComputation.INPUT_NULL;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;

import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.stream.StreamProcessorTopology;

/**
 * A processor aggregating metrics in history stream.
 * 
 */
public class StatisticsAggregatorProcessor implements StreamProcessorTopology {

	@Override
	public Topology getTopology(Map<String, String> options) {
		Topology.Builder builder = Topology.builder();

		builder.addComputation(() -> new StatisticTSAggregateComputation(getPollInterval()),
				Collections.singletonList(INPUT_1 + ":" + INPUT_NULL));
		return builder.build();
	}

	protected Duration getPollInterval() {
		if (Framework.isTestModeSet()) {
			return Duration.ofSeconds(5);
		}
		return Duration.ofMinutes(5);
	}

}
