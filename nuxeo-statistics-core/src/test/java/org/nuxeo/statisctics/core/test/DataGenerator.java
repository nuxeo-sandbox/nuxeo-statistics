package org.nuxeo.statisctics.core.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataGenerator {

	
	public static List<Map<String, Long>> genData(long t0, int deltaS, int nbRecords) {
	
		List<Map<String, Long>> result = new ArrayList<>();
		Long ts = t0;
		
		for (int i = 0; i < nbRecords; i++) {
					
			Map<String, Long> metrics = new HashMap<String, Long>();		
		
			metrics.put("ts", t0-(nbRecords-i)*deltaS);
			
			metrics.put("nuxeo.statistics.metricA", i*10L);
			if (i%2==0) {
				metrics.put("nuxeo.statistics.metricB", (i/2)*10L);							
			}
			if (i%3==0) {
				metrics.put("nuxeo.statistics.metricC", (i/3)*10L);							
			}
			result.add(metrics);
		}
		
		return result;
	}
	
}
