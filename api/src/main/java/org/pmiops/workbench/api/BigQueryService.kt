package org.pmiops.workbench.api

import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.BigQueryException
import com.google.cloud.bigquery.BigQueryOptions
import com.google.cloud.bigquery.Field
import com.google.cloud.bigquery.FieldList
import com.google.cloud.bigquery.FieldValue
import com.google.cloud.bigquery.JobInfo
import com.google.cloud.bigquery.QueryJobConfiguration
import com.google.cloud.bigquery.TableId
import com.google.cloud.bigquery.TableResult
import com.google.common.annotations.VisibleForTesting
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Level
import java.util.logging.Logger
import java.util.stream.Collectors
import javax.inject.Provider
import javax.servlet.http.HttpServletResponse
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import org.pmiops.workbench.cdr.CdrVersionContext
import org.pmiops.workbench.config.WorkbenchConfig
import org.pmiops.workbench.db.model.CdrVersion
import org.pmiops.workbench.exceptions.BadRequestException
import org.pmiops.workbench.exceptions.ForbiddenException
import org.pmiops.workbench.exceptions.ServerErrorException
import org.pmiops.workbench.exceptions.ServerUnavailableException
import org.pmiops.workbench.model.Domain
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
open class BigQueryService {

    @Autowired
    private val workbenchConfigProvider: Provider<WorkbenchConfig>? = null
    @Autowired
    private val defaultBigQuery: BigQuery? = null

    protected open// If a query is being executed in the context of a CDR, it must be run within that project as
    // well. By default, the query would run in the Workbench App Engine project, which would
    // violate VPC-SC restrictions.
    val bigQueryService: BigQuery?
        @VisibleForTesting
        get() {
            val cdrVersion = CdrVersionContext.getCdrVersion() ?: return defaultBigQuery
            return BigQueryOptions.newBuilder()
                    .setProjectId(cdrVersion.bigqueryProject)
                    .build()
                    .service
        }

    /** Execute the provided query using bigquery.  */
    @JvmOverloads
    fun executeQuery(query: QueryJobConfiguration, waitTime: Long = 60000L): TableResult {
        if (workbenchConfigProvider!!.get().cdr.debugQueries) {
            logger.log(
                    Level.INFO,
                    "Executing query ({0}) with parameters ({1})",
                    arrayOf(query.query, query.namedParameters))
        }
        try {
            return bigQueryService!!
                    .create(JobInfo.of(query))
                    .getQueryResults(BigQuery.QueryResultsOption.maxWaitTime(waitTime))
        } catch (e: InterruptedException) {
            throw BigQueryException(500, "Something went wrong with BigQuery: " + e.message)
        } catch (e: BigQueryException) {
            if (e.code == HttpServletResponse.SC_SERVICE_UNAVAILABLE) {
                throw ServerUnavailableException(
                        "BigQuery was temporarily unavailable, try again later", e)
            } else if (e.code == HttpServletResponse.SC_FORBIDDEN) {
                throw ForbiddenException("BigQuery access denied", e)
            } else {
                throw ServerErrorException(
                        String.format(
                                "An unexpected error occurred querying against BigQuery with " + "query = (%s), params = (%s)",
                                query.query, query.namedParameters),
                        e)
            }
        }

    }

    fun filterBigQueryConfig(queryJobConfiguration: QueryJobConfiguration): QueryJobConfiguration {
        val cdrVersion = CdrVersionContext.getCdrVersion()
                ?: throw ServerErrorException("No CDR version specified")
        var returnSql = queryJobConfiguration.query.replace("\${projectId}", cdrVersion.bigqueryProject)
        returnSql = returnSql.replace("\${dataSetId}", cdrVersion.bigqueryDataset)
        return queryJobConfiguration.toBuilder().setQuery(returnSql).build()
    }

    fun getResultMapper(result: TableResult): Map<String, Int> {
        if (result.totalRows == 0L) {
            return emptyMap()
        }
        val index = AtomicInteger()
        return result.schema.fields.stream()
                .collect<Map<String, Int>, Any>(Collectors.toMap(Function<Field, String> { it.getName() }, { s -> index.getAndIncrement() }))
    }

    fun getLong(row: List<FieldValue>, index: Int): Long? {
        if (row[index].isNull) {
            throw BigQueryException(500, "FieldValue is null at position: $index")
        }
        return row[index].longValue
    }

    fun isNull(row: List<FieldValue>, index: Int): Boolean {
        return row[index].isNull
    }

    fun getString(row: List<FieldValue>, index: Int): String? {
        return if (row[index].isNull) null else row[index].stringValue
    }

    fun getBoolean(row: List<FieldValue>, index: Int): Boolean? {
        return row[index].booleanValue
    }

    fun getDateTime(row: List<FieldValue>, index: Int): String {
        if (row[index].isNull) {
            throw BigQueryException(500, "FieldValue is null at position: $index")
        }
        val df = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss zzz").withZoneUTC()
        return df.print(row[index].timestampValue / 1000L)
    }

    fun getDate(row: List<FieldValue>, index: Int): String {
        if (row[index].isNull) {
            throw BigQueryException(500, "FieldValue is null at position: $index")
        }
        return row[index].stringValue
    }

    fun getTableFieldsFromDomain(d: Domain): FieldList {
        val cdrVersion = CdrVersionContext.getCdrVersion()
        val tableName: String
        if (Domain.CONDITION.equals(d)) {
            tableName = "ds_condition_occurrence"
        } else if (Domain.PROCEDURE.equals(d)) {
            tableName = "ds_procedure_occurrence"
        } else if (Domain.DRUG.equals(d)) {
            tableName = "ds_drug_exposure"
        } else if (Domain.MEASUREMENT.equals(d)) {
            tableName = "ds_measurement"
        } else if (Domain.SURVEY.equals(d)) {
            tableName = "ds_survey"
        } else if (Domain.PERSON.equals(d)) {
            tableName = "ds_person"
        } else {
            throw BadRequestException("Invalid domain, unable to fetch fields from table")
        }
        val tableId = TableId.of(cdrVersion.bigqueryProject, cdrVersion.bigqueryDataset, tableName)

        return bigQueryService!!.getTable(tableId).getDefinition<TableDefinition>().getSchema()!!.getFields()
    }

    companion object {

        private val logger = Logger.getLogger(BigQueryService::class.java.name)
    }
}
/** Execute the provided query using bigquery.  */
