package org.pmiops.workbench.api

import com.google.cloud.bigquery.BigQuery
import org.springframework.beans.factory.annotation.Autowired

/**
 * This sole purpose of this class is to override the setting of projectId on the BigQuery service
 * instance so test cases are runnable inside IntelliJ.
 */
class BigQueryTestService : BigQueryService() {

    @Autowired
    protected override val bigQueryService: BigQuery? = null
}
