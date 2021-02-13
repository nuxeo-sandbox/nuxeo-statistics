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

import java.util.List;
import java.util.Map;

/**
 * @since 11.5
 */
public interface StatisticsService {

	StatisticsComputer getComputer(String name);

	List<StatisticsComputer> getComputers();

	void computeStatistics(String computer);

	Long getStatistic(String key);

	void storeStatisticsTimeSerie(List<Map<String, Long>> tsMetrics);

	String getStatisticsTimeSerieAsJson();

	List<Map<String, Long>> getStatisticsTimeSerie();

}
