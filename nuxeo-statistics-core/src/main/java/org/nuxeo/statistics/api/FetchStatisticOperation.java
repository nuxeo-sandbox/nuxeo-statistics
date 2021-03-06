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
 *      Tiry
 */

package org.nuxeo.statistics.api;

import java.util.List;
import java.util.Map;

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

	@Param(name = "maxValues", required = false)
	protected Integer maxValues = null;

	@Context
	protected CoreSession session;

	protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@OperationMethod
	public String run() throws Exception {

		StatisticsService service = Framework.getService(StatisticsService.class);

		if (filter != null || start != null || duration != null || maxValues != null) {

			List<Map<String, Long>> ts = Framework.getService(StatisticsService.class).getStatisticsTimeSerie();

			if (ts == null || ts.size() == 0) {
				log.warn("No data available yet");
				return "[]";
			}

			MetricsFilter mf = new MetricsFilter(filter, start, duration, maxValues);
			List<Map<String, Long>> filtered = mf.process(ts);
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
