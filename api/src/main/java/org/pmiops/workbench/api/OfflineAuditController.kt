package org.pmiops.workbench.api

import com.google.cloud.bigquery.FieldValue
import com.google.cloud.bigquery.QueryJobConfiguration
import com.google.cloud.bigquery.TableResult
import com.google.common.annotations.VisibleForTesting
import com.google.common.collect.ImmutableList
import com.google.common.collect.Sets
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit
import java.util.logging.Logger
import java.util.stream.Collectors
import java.util.stream.IntStream
import org.pmiops.workbench.db.dao.CdrVersionDao
import org.pmiops.workbench.db.dao.WorkspaceDao
import org.pmiops.workbench.model.AuditBigQueryResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

/**
 * The audit controller is meant for performing offline audit checks. Currently, audit violations
 * result in SEVERE log statements being written, which may then be alerted on in cloud console. The
 * audit policy implemented by this API reflects the design outlined here:
 * https://docs.google.com/document/d/14HT1GWXHPMaCc9rhCM0y5CglAIY-GgRBwebdZpqwEDs
 */
@RestController
class OfflineAuditController @Autowired
internal constructor(
        private val clock: Clock,
        private val bigQueryService: BigQueryService,
        private val cdrVersionDao: CdrVersionDao,
        private val workspaceDao: WorkspaceDao) : OfflineAuditApiDelegate {

    fun auditBigQuery(): ResponseEntity<AuditBigQueryResponse> {
        // We expect to only see queries run within Firecloud AoU projects, or for administrative
        // purposes within the CDR project itself.
        val cdrProjects = ImmutableList.copyOf<CdrVersion>(cdrVersionDao.findAll()).stream()
                .map<String> { v -> v.getBigqueryProject() }
                .collect<Set<String>, Any>(Collectors.toSet())
        val whitelist = Sets.union(workspaceDao.findAllWorkspaceNamespaces(), cdrProjects)

        val now = clock.instant()
        val suffixes = IntStream.range(0, AUDIT_DAY_RANGE)
                .mapToObj { i -> auditTableSuffix(now, i) }
                .collect<List<String>, Any>(Collectors.toList())

        var numBad = 0
        var numQueries = 0
        for (cdrProjectId in cdrProjects) {
            val result = bigQueryService.executeQuery(QueryJobConfiguration.of(auditSql(cdrProjectId, suffixes)))
            val rm = bigQueryService.getResultMapper(result)

            for (row in result.iterateAll()) {
                val project_id = bigQueryService.getString(row, rm["client_project_id"])
                val email = bigQueryService.getString(row, rm["user_email"])
                val total = bigQueryService.getLong(row, rm["total"])!!
                if (bigQueryService.isNull(row, rm["client_project_id"])) {
                    log.severe(
                            String.format(
                                    "AUDIT: (CDR project '%s') %d queries with missing project ID from user '%s'; "
                                            + "indicates an ACL misconfiguration, this user can access the CDR but is not a "
                                            + "project jobUser",
                                    cdrProjectId, total, email))
                    numBad += total.toInt()
                } else if (!whitelist.contains(project_id)) {
                    log.severe(
                            String.format(
                                    "AUDIT: (CDR project '%s') %d queries in unrecognized project '%s' from user '%s'",
                                    cdrProjectId, total, project_id, email))
                    numBad += total.toInt()
                }
                numQueries += total.toInt()
            }
        }
        log.info(
                String.format("AUDIT: found audit issues with %d/%d BigQuery queries", numBad, numQueries))
        return ResponseEntity.ok(AuditBigQueryResponse().numQueryIssues(numBad))
    }

    companion object {

        private val log = Logger.getLogger(OfflineAuditController::class.java.name)
        private val AUDIT_SINK_NAME = "cdr_audit_logs"
        // How many days into the past (including today) logs should be checked. This could become a
        // request parameter if the need arises.
        private val AUDIT_DAY_RANGE = 7
        // BigQuery log sink table names are have a suffix like "20170103", per
        // https://cloud.google.com/logging/docs/export/using_exported_logs#table_organization
        private val auditTableNameDateFormatter = DateTimeFormatterBuilder()
                .appendValue(ChronoField.YEAR, 4)
                .appendValue(ChronoField.MONTH_OF_YEAR, 2)
                .appendValue(ChronoField.DAY_OF_MONTH, 2)
                .toFormatter()

        @VisibleForTesting
        internal fun auditTableSuffix(now: Instant, daysAgo: Int): String {
            val target = now.minus(daysAgo.toLong(), ChronoUnit.DAYS)
            return auditTableNameDateFormatter.withZone(ZoneId.of("UTC")).format(target)
        }

        private fun auditSql(cdrProjectId: String, tableSuffixes: List<String>): String {
            // "jobInsertResponse" appears to always be included, despite whether or not job request
            // metadata was included (i.e. for jobs running in other projects).
            val tableWildcard = String.format(
                    "%s.%s.cloudaudit_googleapis_com_data_access_*", cdrProjectId, AUDIT_SINK_NAME)
            return String.format(
                    "SELECT\n"
                            + "  protopayload_auditlog.servicedata_v1_bigquery.jobInsertResponse.resource.jobName.projectId client_project_id,\n"
                            + "  protopayload_auditlog.authenticationInfo.principalEmail user_email,\n"
                            + "  SUM(1) total\n"
                            + "FROM `%s`\n"
                            + "WHERE protopayload_auditlog.methodName = 'jobservice.insert'\n"
                            +
                            // Filter out failed queries (0/unset means OK).
                            "  AND (protopayload_auditlog.status.code IS NULL\n"
                            + "       OR protopayload_auditlog.status.code = 0)\n"
                            + "  AND _TABLE_SUFFIX IN (%s)\n"
                            + "GROUP BY 1, 2",
                    tableWildcard,
                    tableSuffixes.stream().map { s -> "'$s'" }.collect<String, *>(Collectors.joining(",")))
        }
    }
}
