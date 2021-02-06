package org.nuxeo.statistics.api;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;

@Operation(id = FetchStatisticOperation.ID, category = Constants.CAT_DOCUMENT)
public class FetchStatisticOperation {

	public static final String ID = "Statistics.Fetch";

	@Param(name = "filter", required = false)
	protected String filter = null;

	@OperationMethod
	public void run() {

		
		
		
	}

}
