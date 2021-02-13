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

package org.nuxeo.statistics.repository.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.lib.stream.computation.log.ComputationRunner.NUXEO_METRICS_REGISTRY_NAME;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.UnboundEventContext;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.query.NxQueryBuilder;
import org.nuxeo.elasticsearch.test.RepositoryElasticSearchFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.stream.RuntimeStreamFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.nuxeo.statistics.StatisticsComputer;
import org.nuxeo.statistics.StatisticsService;
import org.nuxeo.statistics.api.FetchStatisticOperation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;

import io.dropwizard.metrics5.Gauge;
import io.dropwizard.metrics5.Metric;
import io.dropwizard.metrics5.MetricFilter;
import io.dropwizard.metrics5.MetricName;
import io.dropwizard.metrics5.MetricRegistry;
import io.dropwizard.metrics5.SharedMetricRegistries;

@RunWith(FeaturesRunner.class)
@Features({ RuntimeStreamFeature.class, RepositoryElasticSearchFeature.class, AutomationFeature.class })
@Deploy("org.nuxeo.ecm.platform.audit.api")
@Deploy("org.nuxeo.ecm.platform.audit")
@Deploy("org.nuxeo.ecm.platform.uidgen.core")
@Deploy("org.nuxeo.elasticsearch.core")
@Deploy("org.nuxeo.elasticsearch.seqgen")
@Deploy("org.nuxeo.statistics.repository.test:elasticsearch-seqgen-index-test-contrib.xml")
@Deploy("org.nuxeo.statistics.repository.test:elasticsearch-audit-index-test-contrib.xml")
@Deploy("org.nuxeo.statistics.repository.test:kv-store-contrib.xml")
@Deploy("org.nuxeo.elasticsearch.audit")
@Deploy({ "org.nuxeo.statistics.core", "org.nuxeo.statistics.repository.test:test-metrics-contrib.xml" })
public class TestRepositoryStats {

	protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@Inject
	protected CoreSession session;

	@Inject
	protected EventService eventService;

	@Inject
	protected ElasticSearchService ess;

	@Inject
	protected ElasticSearchAdmin esa;

	@Inject
	protected AutomationService as;

	protected void addSomeContent() throws Exception {

		DocumentModel folder = session.createDocumentModel("/", "root", "Folder");
		folder.setPropertyValue("dc:title", "Root");
		folder = session.createDocument(folder);

		DocumentModel file = session.createDocumentModel(folder.getPathAsString(), "file", "File");
		file.setPropertyValue("dc:title", "File");
		Blob blob = new StringBlob("0123456789", "text/plain", "UTF-8");
		blob.setFilename("SampleFile.txt");
		file.setPropertyValue("file:content", (Serializable) blob);

		file = session.createDocument(file);

		DocumentModel file2 = session.createDocumentModel(folder.getPathAsString(), "file2", "File");
		file2.setPropertyValue("dc:title", "File2");
		Blob blob2 = new StringBlob("01234", "text/plain", "UTF-8");
		blob2.setFilename("SampleFile2.txt");
		file2.setPropertyValue("file:content", (Serializable) blob2);

		file2 = session.createDocument(file2);

		file2.setPropertyValue("dc:description", "Modified");
		file2 = session.saveDocument(file2);

		fireUserEvents();

		session.save();
		TransactionHelper.commitOrRollbackTransaction();
		TransactionHelper.startTransaction();
		Thread.sleep(2000);
		eventService.waitForAsyncCompletion();

		// ensure indexed
		NxQueryBuilder queryBuilder = new NxQueryBuilder(session).nxql("Select * from Document");
		DocumentModelList allDocs = ess.query(queryBuilder);
		assertNotEquals(0, allDocs.size());
	}

	protected void fireUserEvents() {

		for (int i = 0; i < 5; i++) {

			NuxeoPrincipal principal = new UserPrincipal("JackyChan" + i, null, false, false);

			Map<String, Serializable> props = new HashMap<>();
			props.put("AuthenticationPlugin", "foobar");
			props.put("category", "NuxeoAuthentication");
			props.put("comment", "This is a login");
			EventContext ctx = new UnboundEventContext(principal, props);

			eventService.fireEvent(ctx.newEvent("loginSuccess"));
		}

		for (int i = 0; i < 3; i++) {
			NuxeoPrincipal principal = new UserPrincipal("JackyChan" + i, null, false, false);
			Map<String, Serializable> props = new HashMap<>();
			props.put("category", "Document");
			EventContext ctx = new UnboundEventContext(principal, props);
			eventService.fireEvent(ctx.newEvent("download"));
		}
	}

	@Test
	public void checkStatsComputerDeployed() throws Exception {
		StatisticsService stats = Framework.getService(StatisticsService.class);
		assertNotNull(stats);

		StatisticsComputer computer = stats.getComputer("repository");
		assertNotNull(computer);
		StatisticsComputer auditComputer = stats.getComputer("audit");
		assertNotNull(auditComputer);

		addSomeContent();

		// run for 10 seconds to be sure we can get aggregates
		System.out.println("Wait for 10s to let the different stream processors do their work");
		Thread.sleep(10000);

		MetricRegistry registry = SharedMetricRegistries.getOrCreate(NUXEO_METRICS_REGISTRY_NAME);

		MetricFilter filter = new MetricFilter() {
			@Override
			public boolean matches(MetricName name, Metric metric) {
				return name.toString().startsWith("nuxeo.statistics");
			}
		};

		SortedMap<MetricName, Gauge> gauges = registry.getGauges(filter);

		System.out.println("##############################");
		System.out.println("Checking Computed metrics");
		int foundMetrics = 0;
		for (MetricName mn : gauges.keySet()) {

			if (mn.getKey().startsWith("nuxeo.statistics.repository.documents")) {
				if (mn.getTags().values().contains("File")) {
					assertEquals(2L, gauges.get(mn).getValue());
					foundMetrics++;
				} else if (mn.getTags().values().contains("Folder")) {
					assertEquals(1L, gauges.get(mn).getValue());
					foundMetrics++;
				}

			}
			if (mn.getKey().startsWith("nuxeo.statistics.repository.blobs.mainBlobs")) {
				assertEquals(15L, gauges.get(mn).getValue());
				foundMetrics++;
			}

			if (mn.getKey().startsWith("nuxeo.statistics.active.users")) {
				// 5 Jackies + Administrator + System
				assertEquals(7L, gauges.get(mn).getValue());
				foundMetrics++;
			}
			if (mn.getKey().startsWith("nuxeo.statistics.audit.events")) {
				if (mn.getTags().values().contains("documentCreated")) {
					// 3 created + 4 from Content Template because of Automation feature
					assertEquals(7L, gauges.get(mn).getValue());
					foundMetrics++;
				} else if (mn.getTags().values().contains("documentModified")) {
					assertEquals(1L, gauges.get(mn).getValue());
					foundMetrics++;
				} else if (mn.getTags().values().contains("loginSuccess")) {
					assertEquals(5L, gauges.get(mn).getValue());
					foundMetrics++;
				} else if (mn.getTags().values().contains("download")) {
					assertEquals(3L, gauges.get(mn).getValue());
					foundMetrics++;
				}
			}

			System.out.println(mn.toString() + ":" + gauges.get(mn).getValue());
		}
		assertEquals(8, foundMetrics);

		// check that TS were computed
		System.out.println("##############################");
		System.out.println("Checking TimeSeries");
		System.out.println("Service level");
		String json = Framework.getService(StatisticsService.class).getStatisticsTimeSerieAsJson();
		System.out.println(json);
		List<Map<String, Long>> ts = Framework.getService(StatisticsService.class).getStatisticsTimeSerie();
		assertTrue(ts.size() >= 2);

		// test Automation Operation
		System.out.println("Checking via Automation API");
		System.out.println("result without filter:");

		OperationContext ctx = new OperationContext(session);
		Map<String, Object> params = new HashMap<>();

		// commit current TX to avoid TX reentrency when calling Automation
		TransactionHelper.commitOrRollbackTransaction();

		String json2 = (String) as.run(ctx, FetchStatisticOperation.ID, params);
		ts = (List<Map<String, Long>>) OBJECT_MAPPER.readValue(json2, new TypeReference<List<Map<String, Long>>>() {
		});
		assertTrue(ts.size() >= 2);
		assertTrue(ts.get(0).containsKey("nuxeo.statistics.repository.documents.File.test"));
		assertTrue(ts.get(0).containsKey("nuxeo.statistics.audit.events.documentModified"));
		System.out.println(json2);

		// call unitary metric
		ctx = new OperationContext(session);
		ctx.setInput("nuxeo.statistics.repository.blobs.mainBlobs.test");
		params.clear();
		Long bytes = (Long) as.run(ctx, FetchStatisticOperation.ID, params);
		System.out.println(bytes);
		assertEquals((Long) 15L, bytes);

		ctx = new OperationContext(session);
		ctx.setInput("nuxeo.statistics.repository.documents.File.test");
		params.clear();
		Long nbdocs = (Long) as.run(ctx, FetchStatisticOperation.ID, params);
		System.out.println(nbdocs);
		assertEquals((Long) 2L, nbdocs);

	}

}
