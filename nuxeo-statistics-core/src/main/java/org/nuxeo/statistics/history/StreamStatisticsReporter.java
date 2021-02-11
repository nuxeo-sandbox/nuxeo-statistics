package org.nuxeo.statistics.history;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.utils.DurationUtils;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.metrics.AbstractMetricsReporter;
import org.nuxeo.statistics.aggregate.StatisticTSAggregateComputation;

import io.dropwizard.metrics5.MetricAttribute;
import io.dropwizard.metrics5.MetricFilter;
import io.dropwizard.metrics5.MetricRegistry;
import io.dropwizard.metrics5.ScheduledReporter;

public class StreamStatisticsReporter extends AbstractMetricsReporter {

    private static final Logger log = LogManager.getLogger(StreamStatisticsReporter.class);

    protected ScheduledReporter reporter;

    protected static final MetricFilter STREAM_STATS_FILTER = MetricFilter.startsWith("nuxeo.statistics");

    public static final String DEFAULT_INTERVAL_PROP_NAME = "nuxeo.statistics.snapshot.default.interval";

    @Override
    public void start(MetricRegistry registry, MetricFilter filter, Set<MetricAttribute> deniedExpansions) {
        log.warn("Reporting Stream Statistics to: {}", StreamMetricsHistoryCollector.STATS_HISTORY_STREAM);
        reporter = new StreamMetricsHistoryCollector(registry, STREAM_STATS_FILTER);
        reporter.start(getPollInterval(), TimeUnit.SECONDS);
    }

    protected long getPollInterval() {
    	if (Framework.isTestModeSet()) {
    		return 10L;
    	}
    	Duration d = DurationUtils.parse(Framework.getProperty(DEFAULT_INTERVAL_PROP_NAME, "5m"));     
    	return d.toSeconds();
    }
    
    @Override
    public void stop() {
        log.debug("Stop reporting");
        if (reporter != null) {
            reporter.stop();
        }
    }

}
