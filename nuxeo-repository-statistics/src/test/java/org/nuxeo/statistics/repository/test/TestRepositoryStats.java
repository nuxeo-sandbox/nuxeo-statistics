package org.nuxeo.statistics.repository.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.nuxeo.lib.stream.computation.log.ComputationRunner.NUXEO_METRICS_REGISTRY_NAME;

import java.io.Serializable;
import java.util.SortedMap;

import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.EventServiceAdmin;
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
import org.nuxeo.statistics.repository.impl.ESRepositoryStatisticsComputer;

import com.google.inject.Inject;

import io.dropwizard.metrics5.Gauge;
import io.dropwizard.metrics5.Metric;
import io.dropwizard.metrics5.MetricFilter;
import io.dropwizard.metrics5.MetricName;
import io.dropwizard.metrics5.MetricRegistry;
import io.dropwizard.metrics5.SharedMetricRegistries;

@RunWith(FeaturesRunner.class)
@Features({RuntimeStreamFeature.class, RepositoryElasticSearchFeature.class})
@Deploy("org.nuxeo.ecm.platform.audit.api")
@Deploy("org.nuxeo.ecm.platform.audit")
@Deploy("org.nuxeo.ecm.platform.uidgen.core")
@Deploy("org.nuxeo.elasticsearch.core")
@Deploy("org.nuxeo.elasticsearch.seqgen")
@Deploy("org.nuxeo.statistics.repository.test:elasticsearch-seqgen-index-test-contrib.xml")
@Deploy("org.nuxeo.statistics.repository.test:elasticsearch-audit-index-test-contrib.xml")
@Deploy("org.nuxeo.elasticsearch.audit")
@Deploy({"org.nuxeo.statistics.core","org.nuxeo.statistics.repository.test:test-metrics-contrib.xml"})
public class TestRepositoryStats {

	//@Test
	public void stupid() throws Exception{
		addSomeContent();
	
		SearchRequest req = new SearchRequest("audit");		
		SearchSourceBuilder sourceBuilder = new SearchSourceBuilder().size(20).
				query(QueryBuilders.matchAllQuery());
		req.source(sourceBuilder);
		SearchResponse response = esa.getClient().search(req);
		System.out.println(response.toString());
		System.out.println(" #events= " + response.getHits().getTotalHits().value);
	}
	
	@Inject
	protected CoreSession session;
	
	@Inject
	protected EventService eventService;
	
	@Inject
	protected ElasticSearchService ess;
	
	@Inject
	protected ElasticSearchAdmin esa;
	
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
	
	
	@Test
	public void checkStatsComputerDeployed() throws Exception {
		StatisticsService stats = Framework.getService(StatisticsService.class);
		assertNotNull(stats);
		
		StatisticsComputer computer = stats.getComputer("repository");
		assertNotNull(computer);
		StatisticsComputer auditComputer = stats.getComputer("audit");
		assertNotNull(auditComputer);
				
		addSomeContent();
		
		Thread.sleep(5000);
		
		MetricRegistry registry = SharedMetricRegistries.getOrCreate(NUXEO_METRICS_REGISTRY_NAME);

		MetricFilter filter=new MetricFilter() {
			@Override
			public boolean matches(MetricName name, Metric metric) {
				return name.toString().startsWith("nuxeo.statistics");
			}
		};
		
		SortedMap<MetricName, Gauge > gauges = registry.getGauges(filter);
		
		
		int foundMetrics=0;
		for (MetricName mn : gauges.keySet()) {
			
			if (mn.getKey().startsWith("nuxeo.statistics.repository.doc-type")) {
				if (mn.getTags().values().contains("File")) {
					assertEquals(2L,gauges.get(mn).getValue());
					foundMetrics++;
				} 
				else if (mn.getTags().values().contains("Folder")) {
					assertEquals(1L,gauges.get(mn).getValue());
					foundMetrics++;
				} 

			}
			if (mn.getKey().startsWith("nuxeo.statistics.repository.blobs.mainBlobs")) {
				assertEquals(15L,gauges.get(mn).getValue());
				foundMetrics++;		
			}
			
			if (mn.getKey().startsWith("nuxeo.statistics.audit.event")) {
				if (mn.getTags().values().contains("documentCreated")) {
					assertEquals(3L,gauges.get(mn).getValue());
					foundMetrics++;
				} 
				else if (mn.getTags().values().contains("documentModified")) {
					assertEquals(1L,gauges.get(mn).getValue());
					foundMetrics++;
				} 
			}
			
			System.out.println(mn.toString() + ":" + gauges.get(mn).getValue());
		}
		assertEquals(5, foundMetrics);
		
	}
	
}
