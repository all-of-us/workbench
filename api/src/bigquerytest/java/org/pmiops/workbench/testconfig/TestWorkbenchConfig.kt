package org.pmiops.workbench.testconfig

class TestWorkbenchConfig {

    var bigquery: BigQueryConfig? = null

    class BigQueryConfig {
        var dataSetId: String? = null
        var projectId: String? = null
    }
}
