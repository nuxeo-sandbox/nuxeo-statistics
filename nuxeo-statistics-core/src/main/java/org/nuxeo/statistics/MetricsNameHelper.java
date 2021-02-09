package org.nuxeo.statistics;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.dropwizard.metrics5.MetricName;

public class MetricsNameHelper {
	
	public static String getMetricKey(MetricName metricName) {
		String key = metricName.getKey();		
		return getMetricKey(key, metricName.getTags());
	}
	
	public static String getMetricKey(String key, Map<String, String> tags) {		
		Set<String> tagNames = tags.keySet();
		List<String> sortedTagNames = tagNames.stream().collect(Collectors.toList());
		Collections.sort(sortedTagNames);
		for (String tag : sortedTagNames) {
			key = key + "." + tags.get(tag);
		}
		return key;
	}

}
