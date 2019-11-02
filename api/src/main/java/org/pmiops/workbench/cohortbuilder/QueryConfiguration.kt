package org.pmiops.workbench.cohortbuilder

import com.google.cloud.bigquery.QueryJobConfiguration
import com.google.common.collect.ImmutableList
import org.pmiops.workbench.config.CdrBigQuerySchemaConfig.ColumnConfig

class QueryConfiguration(
        val selectColumns: ImmutableList<ColumnInfo>, val queryJobConfiguration: QueryJobConfiguration) {

    class ColumnInfo internal constructor(val columnName: String, val columnConfig: ColumnConfig)
}
