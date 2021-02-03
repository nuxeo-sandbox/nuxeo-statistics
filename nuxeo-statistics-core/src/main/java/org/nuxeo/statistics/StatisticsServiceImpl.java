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

import io.dropwizard.metrics5.Gauge;
import io.dropwizard.metrics5.MetricRegistry;
import io.dropwizard.metrics5.SharedMetricRegistries;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.kv.KeyValueService;
import org.nuxeo.runtime.kv.KeyValueStore;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Descriptor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.nuxeo.lib.stream.computation.log.ComputationRunner.NUXEO_METRICS_REGISTRY_NAME;

public class StatisticsServiceImpl extends DefaultComponent implements StatisticsService {

    protected static final String XP_COMPUTERS = "computers";

    protected static final String STATISTICS_STORE_NAME = "statistics";

    protected static final String METRICS_PREFIX = "nuxeo.statistics";

    protected MetricRegistry registry = SharedMetricRegistries.getOrCreate(NUXEO_METRICS_REGISTRY_NAME);

    protected Map<String, Boolean> registeredMetrics = new HashMap<>();

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
    	StatisticsComputer optComputer= getRegistry().getDescriptor(name, XP_COMPUTERS, contribName);  	
    	return Optional.ofNullable(optComputer);
    }
    
    @Override
    public void computeStatistics(String computerName) {
    	
        Optional<StatisticsComputer> optComputer = getRegistryContribution(XP_COMPUTERS, computerName);
        if (optComputer.isPresent()) {
            StatisticsComputer computer = optComputer.get();
            computer.get().forEach((k, v) -> {
                var key = getEntryKey(computerName, k);
                getStore().put(key, v);
                if (!Boolean.TRUE.equals(registeredMetrics.get(computerName))) {
                    registerMetric(key);
                }
            });
            // XXX: does not allow changes to metrics
            registeredMetrics.putIfAbsent(computerName, true);
        }
    }

    @Override
    public Long getStatistic(String computerName, String key) {
        return getStore().getLong(getEntryKey(computerName, key));
    }

    protected void registerMetric(String key) {
        registry.register(METRICS_PREFIX + "." + key, (Gauge<Long>) () -> (Long) getStore().getLong(key));
    }

    protected String getEntryKey(String computerName, String key) {
        return computerName + "." + key;
    }

    protected KeyValueStore getStore() {
        KeyValueStore store = Framework.getService(KeyValueService.class).getKeyValueStore(STATISTICS_STORE_NAME);
        if (store == null) {
            throw new NuxeoException("Unknown key/value store: " + STATISTICS_STORE_NAME);
        }
        return store;
    }

}
