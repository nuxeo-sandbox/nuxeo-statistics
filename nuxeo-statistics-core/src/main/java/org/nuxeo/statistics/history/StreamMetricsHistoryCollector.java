package org.nuxeo.statistics.history;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.Externalizable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

import org.nuxeo.lib.stream.StreamRuntimeException;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.log.LogAppender;
import org.nuxeo.lib.stream.log.Name;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.cluster.ClusterService;
import org.nuxeo.runtime.stream.StreamService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.dropwizard.metrics5.Counter;
import io.dropwizard.metrics5.Gauge;
import io.dropwizard.metrics5.Histogram;
import io.dropwizard.metrics5.Meter;
import io.dropwizard.metrics5.MetricFilter;
import io.dropwizard.metrics5.MetricName;
import io.dropwizard.metrics5.MetricRegistry;
import io.dropwizard.metrics5.ScheduledReporter;
import io.dropwizard.metrics5.Snapshot;
import io.dropwizard.metrics5.Timer;

/**
 * 
 * 
 * NB: This code is taken from the StreamMetricsReporter available in 11.5
 * 
 * @author tiry
 *
 */
public class StreamMetricsHistoryCollector extends ScheduledReporter {

	protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	protected String hostIp;

	protected String hostname;

	protected String nodeId;

	public static final String STATS_HISTORY_STREAM = "statistics/history";

	public StreamMetricsHistoryCollector(MetricRegistry registry, MetricFilter filter) {
		super(registry, "stream-stats-reporter", filter, TimeUnit.SECONDS, TimeUnit.SECONDS);
		try {
			InetAddress host = InetAddress.getLocalHost();
			hostIp = host.getHostAddress();
			hostname = host.getHostName();
		} catch (UnknownHostException e) {
			hostIp = "unknown";
			hostname = "unknown";
		}
	}

	protected String getNodeId() {
		if (nodeId == null) {
			ClusterService clusterService = Framework.getService(ClusterService.class);
			if (clusterService.isEnabled()) {
				nodeId = clusterService.getNodeId();
			}
		}
		return nodeId;
	}

	@Override
	public void report(SortedMap<MetricName, Gauge> gauges, SortedMap<MetricName, Counter> counters,
			SortedMap<MetricName, Histogram> histograms, SortedMap<MetricName, Meter> meters,
			SortedMap<MetricName, Timer> timers) {

		
		StreamService service = Framework.getService(StreamService.class);
		if (service == null) {
			// stream service is not yet ready
			return;
		}
		// like for other reporters, there is no need for millisecond granularity
		long timestamp = System.currentTimeMillis() / 1000;
		ArrayNode metrics = OBJECT_MAPPER.createArrayNode();
		for (Map.Entry<MetricName, Gauge> entry : gauges.entrySet()) {
			reportGauge(metrics, entry.getKey(), entry.getValue());
		}
		for (Map.Entry<MetricName, Timer> entry : timers.entrySet()) {
			reportTimer(metrics, entry.getKey(), entry.getValue());
		}
		for (Map.Entry<MetricName, Counter> entry : counters.entrySet()) {
			reportCounter(metrics, entry.getKey(), entry.getValue());
		}
		ObjectNode ret = OBJECT_MAPPER.createObjectNode();
		ret.put("timestamp", timestamp);
		ret.put("hostname", hostname);
		ret.put("ip", hostIp);
		ret.put("nodeId", getNodeId());
		ret.set("metrics", metrics);

		try {			
			
			Name streamName = Name.ofUrn(STATS_HISTORY_STREAM);
			Record rec = Record.of(hostIp, OBJECT_MAPPER.writer().writeValueAsString(ret).getBytes(UTF_8));
			
			LogAppender<Externalizable> appender = service.getLogManager().getAppender(streamName);			
			appender.append(rec.getKey(), rec);					
		
		} catch (JsonProcessingException e) {
			throw new StreamRuntimeException("Cannot convert to json", e);
		} catch (Exception e ) {
			e.printStackTrace();
		}		
		//System.out.println("History stored");
		//System.out.println(ret.toString());	
	}

	protected void reportTimer(ArrayNode metrics, MetricName key, Timer value) {
		ObjectNode metric = OBJECT_MAPPER.createObjectNode();
		metric.put("k", key.getKey());
		key.getTags().forEach(metric::put);
		if (value.getCount() == 0) {
			// don't report empty timer
			metric.put("count", 0);
		} else {
			metric.put("count", value.getCount());
			metric.put("rate1m", value.getOneMinuteRate());
			metric.put("rate5m", value.getFiveMinuteRate());
			metric.put("sum", value.getSum());
			Snapshot snapshot = value.getSnapshot();
			metric.put("max", convertDuration(snapshot.getMax()));
			metric.put("mean", convertDuration(snapshot.getMean()));
			metric.put("min", convertDuration(snapshot.getMin()));
			metric.put("stddev", convertDuration(snapshot.getStdDev()));
			metric.put("p50", convertDuration(snapshot.getMedian()));
			metric.put("p95", convertDuration(snapshot.get95thPercentile()));
			metric.put("p99", convertDuration(snapshot.get99thPercentile()));
		}
		metrics.add(metric);
	}

	protected void reportCounter(ArrayNode metrics, MetricName key, Counter value) {
		ObjectNode metric = OBJECT_MAPPER.createObjectNode();
		metric.put("k", key.getKey());
		key.getTags().forEach(metric::put);
		metric.put("v", value.getCount());
		metrics.add(metric);
	}

	protected void reportGauge(ArrayNode metrics, MetricName key, Gauge<?> value) {
		ObjectNode metric = OBJECT_MAPPER.createObjectNode();
		metric.put("k", key.getKey());
		key.getTags().forEach(metric::put);
		putGaugeMetric(metric, value.getValue());
		metrics.add(metric);
	}

	protected void putGaugeMetric(ObjectNode metric, Object o) {
		if (o instanceof Float) {
			metric.put("v", (Float) o);
		} else if (o instanceof Double) {
			metric.put("v", (Double) o);
		} else if (o instanceof Byte) {
			metric.put("v", ((Byte) o).intValue());
		} else if (o instanceof Short) {
			metric.put("v", ((Short) o));
		} else if (o instanceof Integer) {
			metric.put("v", ((Integer) o));
		} else if (o instanceof Long) {
			metric.put("v", ((Long) o));
		} else if (o instanceof BigInteger) {
			metric.put("v", ((BigInteger) o));
		} else if (o instanceof BigDecimal) {
			metric.put("v", ((BigDecimal) o));
		} else if (o instanceof Boolean) {
			metric.put("v", (Boolean) o ? 1 : 0);
		}
	}

}
