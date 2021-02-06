package org.nuxeo.statistics.aggregate;

import java.time.Duration;

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
import org.nuxeo.statistics.history.StreamMetricsHistoryCollector;

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
			}  catch (Exception e ) {
				System.out.println("Unale to open source stream:");
				e.printStackTrace();
				setTimer(context);
				return;
			}

			LogRecord<Record> entry = null;
			try {

				do {
					entry = tailer.read(Duration.ofSeconds(1));
					if (entry != null) {
						aggregate(entry.message());
					}
				} while (entry != null);

				// close without committing position
				tailer.close();

			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		setTimer(context);
	}
	
	protected void aggregate(Record metricsRecord) {
		System.out.println("Aggregate record");
		System.out.println(metricsRecord.toString());
	}

}
