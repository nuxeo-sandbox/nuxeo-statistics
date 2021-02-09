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
package org.nuxeo.statistics;

import static org.nuxeo.lib.stream.computation.log.ComputationRunner.NUXEO_METRICS_REGISTRY_NAME;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.kv.KeyValueService;
import org.nuxeo.runtime.kv.KeyValueStore;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Descriptor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.dropwizard.metrics5.Gauge;
import io.dropwizard.metrics5.MetricName;
import io.dropwizard.metrics5.MetricRegistry;
import io.dropwizard.metrics5.SharedMetricRegistries;

public class StatisticsServiceImpl extends DefaultComponent implements StatisticsService {

	protected static final String XP_COMPUTERS = "computers";

	protected static final String STATISTICS_STORE_NAME = "statistics";

	protected static final String METRICS_PREFIX = "nuxeo.statistics";

	protected MetricRegistry registry = SharedMetricRegistries.getOrCreate(NUXEO_METRICS_REGISTRY_NAME);

	protected Map<String, Boolean> registeredMetrics = new HashMap<>();

	protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private static final Logger log = LogManager.getLogger(StatisticsServiceImpl.class);

	@Override
	public List<StatisticsComputer> getComputers() {
		return (List<StatisticsComputer>) (Object) getRegistry().getDescriptors(name, XP_COMPUTERS);
	}

	@Override
	public StatisticsComputer getComputer(String computerName) {

		Optional<StatisticsComputer> optComputer = getRegistryContribution(XP_COMPUTERS, computerName);
		if (optComputer.isPresent()) {
			return optComputer.get();
		}
		return null;
	}

	@Override
	protected boolean register(String xp, Descriptor descriptor) {
		return getRegistry().register(name, xp, descriptor);
	}

	@Override
	public void registerContribution(Object contribution, String xp, ComponentInstance component) {
		if (contribution instanceof Descriptor) {
			register(xp, (Descriptor) contribution);
		}
	}

	protected Optional<StatisticsComputer> getRegistryContribution(String xp, String contribName) {
		StatisticsComputer optComputer = getRegistry().getDescriptor(name, XP_COMPUTERS, contribName);
		return Optional.ofNullable(optComputer);
	}

	@Override
	public void computeStatistics(String computerName) {

		Optional<StatisticsComputer> optComputer = getRegistryContribution(XP_COMPUTERS, computerName);
		if (optComputer.isPresent()) {
			StatisticsComputer computer = optComputer.get();
			computer.get().forEach((name, v) -> {
				var key = MetricsNameHelper.getMetricKey(name);
				getStore().put(key, v);
				if (!Boolean.TRUE.equals(registeredMetrics.get(computerName))) {
					registerMetric(name, key);
				}
			});
			// XXX: does not allow changes to metrics
			registeredMetrics.putIfAbsent(computerName, true);
		}
	}

	@Override
	public Long getStatistic(String key) {
		return getStore().getLong(key);
	}

	protected void registerMetric(MetricName name) {
		registerMetric(name, MetricsNameHelper.getMetricKey(name));
	}

	protected void registerMetric(MetricName name, String key) {
		registry.register(name, (Gauge<Long>) () -> (Long) getStore().getLong(key));
	}


	protected KeyValueStore getStore() {
		KeyValueStore store = Framework.getService(KeyValueService.class).getKeyValueStore(STATISTICS_STORE_NAME);
		if (store == null) {
			throw new NuxeoException("Unknown key/value store: " + STATISTICS_STORE_NAME);
		}
		return store;
	}

	protected String getTSKey() {
		return METRICS_PREFIX + ".timeseries";
	}

	public void storeStatisticsTimeSerie(List<Map<String, Long>> tsMetrics) {
		String json;
		try {
			json = OBJECT_MAPPER.writer().writeValueAsString(tsMetrics);		
			getStore().put(getTSKey(), json);
		} catch (JsonProcessingException e) {
			log.error("Unable to convert to save metric aggregate", e);
		}
	}
	
	public String getStatisticsTimeSerieAsJson() {	
		return new String(getStore().get(getTSKey()));
	}

	public List<Map<String, Long>> getStatisticsTimeSerie() {	
		
		String json = getStatisticsTimeSerieAsJson();
		try {
			return (List<Map<String, Long>> ) OBJECT_MAPPER.readValue(json, new TypeReference<List<Map<String, Long>>>(){});		
		} catch (JsonProcessingException e) {
			log.error("Unable to convert to save metric aggregate", e);
		}
		return null;
	}


}
