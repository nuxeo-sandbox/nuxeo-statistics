package org.nuxeo.statistics.aggregate;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.computation.AbstractComputation;
import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.log.LogRecord;
import org.nuxeo.lib.stream.log.LogTailer;
import org.nuxeo.lib.stream.log.Name;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.codec.CodecService;
import org.nuxeo.runtime.stream.StreamService;
import org.nuxeo.statistics.MetricsNameHelper;
import org.nuxeo.statistics.StatisticsService;
import org.nuxeo.statistics.history.StreamMetricsHistoryCollector;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class StatisticTSAggregateComputation extends AbstractComputation {

	private static final Logger log = LogManager.getLogger(StatisticTSAggregateComputation.class);

	protected final long intervalMs;

	protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	public StatisticTSAggregateComputation(Duration interval) {
		super("statistics/AggregateComputation", 1, 0);
		this.intervalMs = interval.toMillis();
	}

	@Override
	public void init(ComputationContext context) {
		if (!context.isSpareComputation()) {
			log.warn("Instance elected to aggregate statistics with interval " + intervalMs);
			setTimer(context);
		}
	}

	protected void setTimer(ComputationContext context) {
		context.setTimer("statsAggregate", System.currentTimeMillis() + intervalMs);
	}

	@Override
	public void processRecord(ComputationContext computationContext, String s, Record record) {
		System.out.println("process Record");
	}

	@Override
	public void processTimer(ComputationContext context, String key, long timestamp) {
		StreamService service = Framework.getService(StreamService.class);
		if (service != null) {
			Codec<Record> codec = Framework.getService(CodecService.class).getCodec("avro", Record.class);
			LogTailer<Record> tailer = null;
			try {
				tailer = service.getLogManager().createTailer(Name.ofUrn("StatisticAggregator"),
						Name.ofUrn(StreamMetricsHistoryCollector.STATS_HISTORY_STREAM), codec);
			} catch (Exception e) {
				System.out.println("Unale to open source stream:");
				e.printStackTrace();
				setTimer(context);
				return;
			}

			List<Map<String, Long>> result = new ArrayList<Map<String, Long>>();
			LogRecord<Record> entry = null;
			try {

				do {
					entry = tailer.read(Duration.ofSeconds(1));
					if (entry != null) {
						result.add(aggregate(entry.message()));
					}
				} while (entry != null);

				// close without committing position
				// because we want to read from the beginning each time!
				tailer.close();
			} catch (InterruptedException e) {
				log.error("Error while reading metrics from stream", e);
			}
			Framework.getService(StatisticsService.class).storeStatisticsTimeSerie(result);
		}
		setTimer(context);
	}

	protected Map<String, Long> aggregate(Record metricsRecord) {
		Map<String, Long> data = new HashMap<>();
		try {

			JsonNode node = OBJECT_MAPPER.reader().readTree(metricsRecord.getData());
			data.put("ts", node.get("timestamp").asLong());
			Iterator<JsonNode> metrics = node.get("metrics").elements();

			while (metrics.hasNext()) {
				JsonNode metric = metrics.next();

				String name = metric.get("k").textValue();
				Long value = metric.get("v").asLong();

				String key = buildMetricKey(metric, name);

				data.put(key, value);
			}
		} catch (IOException e) {
			log.error("Unable to process metric data from stream", e);
		}
		return data;
	}

	protected String buildMetricKey(JsonNode metricData, String name) {
		Iterator<String> fields = metricData.fieldNames();
		Map<String, String> tags = new HashMap<>();
		while(fields.hasNext()) {
			String tagName = fields.next();
			if (!tagName.equals("k") && !tagName.equals("v")) {
				tags.put(tagName, metricData.get(tagName).textValue());
			}			
		}	
		return MetricsNameHelper.getMetricKey(name, tags);
	}


	protected String buildMetricKey_Old(JsonNode metricData, String name) {

		Iterator<String> fields = metricData.fieldNames();
		Map<String, String> tags = new HashMap<>();
		while(fields.hasNext()) {
			String tagName = fields.next();
			if (!tagName.equals("k") && !tagName.equals("v")) {
				tags.put(tagName, metricData.get(tagName).textValue());
			}
			
		}
		if (name.endsWith(".documents")) {
			String repo = metricData.get("repository").textValue();
			String docType = metricData.get("doctype").textValue();
			return String.join(".", "nuxeo.statistics.repository", repo, "documents", docType);
		} else if (name.endsWith(".audit.events")) {
			String event = metricData.get("event").textValue();
			return String.join(".", name, event);
		} else if (name.endsWith(".mainBlobs")) {
			String repo = metricData.get("repository").textValue();
			return String.join(".", "nuxeo.statistics.repository", repo, "blobs.mainBlobs");
		}
		return name;
	}

}
