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

import org.nuxeo.common.utils.DurationUtils;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.Descriptor;

import io.dropwizard.metrics5.MetricName;

import java.time.Duration;
import java.util.Map;
import java.util.function.Supplier;

@XObject(value = "computer")
public class StatisticsComputer implements Supplier<Map<MetricName, Long>>, Descriptor {

	public static final String DEFAULT_INTERVAL_PROP_NAME = "nuxeo.statistics.computer.default.interval";
	
    public static final Duration DEFAULT_INTERVAL = DurationUtils.parse(Framework.getProperty(DEFAULT_INTERVAL_PROP_NAME, "1h"));

    @XNode("@name")
    public String name;

    @XNode("@class")
    public Class<? extends Supplier<Map<MetricName, Long>>> klass;

    @XNode("@interval")
    public Duration interval = DEFAULT_INTERVAL;

    @XNode("@store")
    public String store;

    @Override
    public Map<MetricName, Long> get() {
        return supplier().get();
    }

    public String getId() {
    	return name;
    }
    
    protected Supplier<Map<MetricName, Long>> supplier() {
        try {
            return klass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to create statistics computer " + name, e);
        }
    }
}
