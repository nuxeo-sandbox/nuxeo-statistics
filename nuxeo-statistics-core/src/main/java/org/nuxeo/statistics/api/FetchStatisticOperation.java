package org.nuxeo.statistics.api;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.ws.rs.core.Context;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.statistics.StatisticsService;

import com.fasterxml.jackson.databind.ObjectMapper;

@Operation(id = FetchStatisticOperation.ID, category = Constants.CAT_DOCUMENT)
public class FetchStatisticOperation {

	public static final String ID = "Statistics.Fetch";

	private static final Logger log = LogManager.getLogger(FetchStatisticOperation.class);

	@Param(name = "filter", required = false)
	protected String filter = null;

	@Param(name = "start", required = false)
	protected Long start = null;

	@Param(name = "duration", required = false)
	protected String duration = null;

	@Context
	protected CoreSession session;

	protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@OperationMethod
	public String run() throws Exception {

		StatisticsService service = Framework.getService(StatisticsService.class);

		if (filter != null || start != null || duration != null) {

			Long end = null;
			if (start == null && duration != null) {
				start = System.currentTimeMillis() / 1000;
			} else if (start != null && duration == null) {
				duration = "1y";
			}
			if (start != null && duration != null) {
				duration = duration.toUpperCase();
				if (!duration.startsWith("P")) {
					if (duration.contains("D")) {
						duration = "P" + duration;
					} else {
						duration = "PT" + duration;
					}
				}
				end = start - Duration.parse(duration).getSeconds();
			}

			List<Map<String, Long>> ts = Framework.getService(StatisticsService.class).getStatisticsTimeSerie();

			if (ts==null || ts.size()==0) {
				log.warn("No data available yet");
				return "[]";
			}
			List<Map<String, Long>> filtered = new ArrayList<>();

			Pattern regexp = null;

			if (filter != null) {
				regexp = Pattern.compile(filter);
			}

			for (Map<String, Long> metrics : ts) {

				Map<String, Long> filterdMetrics = new HashMap<>();

				for (String key : metrics.keySet()) {
					if ("ts".equals(key) || regexp.matcher(key).matches()) {
						filterdMetrics.put(key, metrics.get(key));
					}
				}

				if (start != null && end != null) {
					long t = filterdMetrics.get("ts");

					if (t <= start && t >= end) {
						filtered.add(filterdMetrics);
					}
				} else {
					filtered.add(filterdMetrics);
				}
			}
			return OBJECT_MAPPER.writer().writeValueAsString(filtered);
		} else {
			return service.getStatisticsTimeSerieAsJson();
		}
	}

	@OperationMethod
	public Long run(String statisticName) throws Exception {

		StatisticsService service = Framework.getService(StatisticsService.class);

		return service.getStatistic(statisticName);
	}

}
