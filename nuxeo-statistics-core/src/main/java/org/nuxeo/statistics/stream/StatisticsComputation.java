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
package org.nuxeo.statistics.stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.lib.stream.computation.AbstractComputation;
import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.statistics.StatisticsService;

import java.time.Duration;

/**
 * Statistics computation.
 *
 * @since 11.5
 */
public class StatisticsComputation extends AbstractComputation {

    private static final Logger log = LogManager.getLogger(StatisticsComputation.class);

    protected final String computer;

    protected final long intervalMs;

    public StatisticsComputation(String computer, Duration interval) {
        super("statistics/" + computer, 1, 0);
        this.computer = computer;
        this.intervalMs = interval.toMillis();
    }

    @Override
    public void init(ComputationContext context) {
        if (!context.isSpareComputation()) {
            log.warn("Instance elected to report statistics");
            context.setTimer("timer", System.currentTimeMillis() + intervalMs);
        }
    }

    @Override
    public void processTimer(ComputationContext context, String key, long timestamp) {
        getStatisticsService().computeStatistics(computer);
        context.setTimer("timer", System.currentTimeMillis() + intervalMs);
    }

    @Override
    public void processRecord(ComputationContext computationContext, String s, Record record) {
        //
    }

    protected StatisticsService getStatisticsService() {
        return Framework.getService(StatisticsService.class);
    }
}
