package org.nuxeo.statistics.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.ws.rs.core.Context;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.statistics.StatisticsService;

import com.fasterxml.jackson.databind.ObjectMapper;

@Operation(id = FetchStatisticOperation.ID, category = Constants.CAT_DOCUMENT)
public class FetchStatisticOperation {

	public static final String ID = "Statistics.Fetch";

	@Param(name = "filter", required = false)
	protected String filter = null;

	//@Context
	//protected StatisticsService service;

	protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@OperationMethod
	public String run() throws Exception {

		StatisticsService service = Framework.getService(StatisticsService.class);
		
		if (filter != null) {

			List<Map<String, Long>> ts = Framework.getService(StatisticsService.class).getStatisticsTimeSerie();
			List<Map<String, Long>> filtered = new ArrayList<>();
			Pattern regexp = Pattern.compile(filter);

			for (Map<String, Long> metrics : ts) {

				Map<String, Long> filterdMetrics = new HashMap<>();

				for (String key : metrics.keySet()) {
					if ("ts".equals(key) || regexp.matcher(key).matches()) {
						filterdMetrics.put(key, metrics.get(key));
					}
				}
				filtered.add(filterdMetrics);
			}

			return OBJECT_MAPPER.writer().writeValueAsString(filtered);
		} else {
			return service.getStatisticsTimeSerieAsJson();
		}
	}

}
