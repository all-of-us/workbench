package org.pmiops.workbench.api;

import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryResult;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.model.AuditBigQueryResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;


/**
 * The audit controller is meant for performing offline audit checks. Currently, audit violations
 * result in SEVERE log statements being written, which may then be alerted on in cloud console.
 * The audit policy implemented by this API reflects the design outlined here:
 *   https://docs.google.com/document/d/14HT1GWXHPMaCc9rhCM0y5CglAIY-GgRBwebdZpqwEDs
 */
@RestController
public class AuditController implements AuditApiDelegate {

  private static final Logger log = Logger.getLogger(AuditController.class.getName());
  private static final String AUDIT_SINK_NAME = "cdr_audit_logs";
  // How many days into the past (including today) logs should be checked. This could become a
  // request parameter if the need arises.
  private static final int AUDIT_DAY_RANGE = 7;
  // BigQuery log sink table names are have a suffix like "20170103", per
  // https://cloud.google.com/logging/docs/export/using_exported_logs#table_organization
  private static final DateTimeFormatter auditTableNameDateFormatter =
      new DateTimeFormatterBuilder()
        .appendValue(ChronoField.YEAR, 4)
        .appendValue(ChronoField.MONTH_OF_YEAR, 2)
        .appendValue(ChronoField.DAY_OF_MONTH, 2)
        .toFormatter();

  private final Clock clock;
  private final BigQueryService bigQueryService;
  private final UserDao userDao;
  private final CdrVersionDao cdrVersionDao;

  @Autowired
  AuditController(
      Clock clock,
      BigQueryService bigQueryService,
      UserDao userDao,
      CdrVersionDao cdrVersionDao) {
    this.clock = clock;
    this.bigQueryService = bigQueryService;
    this.userDao = userDao;
    this.cdrVersionDao = cdrVersionDao;
  }

  @VisibleForTesting
  static String auditTableSuffix(Instant now, int daysAgo) {
    Instant target = now.minus(daysAgo, ChronoUnit.DAYS);
    return auditTableNameDateFormatter.withZone(ZoneId.of("UTC")).format(target);
  }

  private static String auditSql(String cdrProjectId, List<String> tableSuffixes) {
    // "jobInsertResponse" appears to always be included, despite whether or not job request
    // metadata was included (i.e. for jobs running in other projects).
    String tableWildcard = String.format(
        "%s.%s.cloudaudit_googleapis_com_data_access_*", cdrProjectId, AUDIT_SINK_NAME);
    return String.format(
        "SELECT\n" +
        "  protopayload_auditlog.servicedata_v1_bigquery.jobInsertResponse.resource.jobName.projectId client_project_id,\n" +
        "  protopayload_auditlog.authenticationInfo.principalEmail user_email,\n" +
        "  SUM(1) total\n" +
        "FROM `%s`\n" +
        "WHERE protopayload_auditlog.methodName = 'jobservice.insert'\n" +
        // Filter out failed queries (0/unset means OK).
        "  AND (protopayload_auditlog.status.code IS NULL\n" +
        "       OR protopayload_auditlog.status.code = 0)\n" +
        "  AND _TABLE_SUFFIX IN (%s)\n" +
        "GROUP BY 1, 2",
        tableWildcard,
        tableSuffixes.stream().map(s -> "'" + s + "'").collect(Collectors.joining(",")));
  }

  @Override
  public ResponseEntity<AuditBigQueryResponse> auditBigQuery() {
    // We expect to only see queries run within Firecloud AoU projects, or for administrative
    // purposes within the CDR project itself.
    Set<String> cdrProjects = ImmutableList.copyOf(cdrVersionDao.findAll())
        .stream()
        .map(v -> v.getBigqueryProject())
        .collect(Collectors.toSet());
    Set<String> whitelist = Sets.union(userDao.getAllUserProjects(), cdrProjects);

    Instant now = clock.instant();
    List<String> suffixes = IntStream.range(0, AUDIT_DAY_RANGE)
        .mapToObj(i -> auditTableSuffix(now, i))
        .collect(Collectors.toList());

    int numBad = 0;
    int numQueries = 0;
    for (String cdrProjectId : cdrProjects) {
      QueryResult result = bigQueryService.executeQuery(
          QueryJobConfiguration.of(auditSql(cdrProjectId, suffixes)));
      Map<String, Integer> rm = bigQueryService.getResultMapper(result);

      for (List<FieldValue> row : result.iterateAll()) {
        String project_id = bigQueryService.getString(row, rm.get("client_project_id"));
        String email = bigQueryService.getString(row, rm.get("user_email"));
        long total = bigQueryService.getLong(row, rm.get("total"));
        if (bigQueryService.isNull(row, rm.get("client_project_id"))) {
          log.severe(String.format(
              "AUDIT: (CDR project '%s') %d queries with missing project ID from user '%s'; " +
                  "indicates an ACL misconfiguration, this user can access the CDR but is not a " +
                  "project jobUser",
                  cdrProjectId, total, email));
          numBad += total;
        } else if (!whitelist.contains(project_id)) {
          log.severe(String.format(
              "AUDIT: (CDR project '%s') %d queries in unrecognized project '%s' from user '%s'",
              cdrProjectId, total, project_id, email));
          numBad += total;
        }
        numQueries += total;
      }
    }
    log.info(String.format(
        "AUDIT: found audit issues with %d/%d BigQuery queries", numBad, numQueries));
    return ResponseEntity.ok(new AuditBigQueryResponse().numQueryIssues(numBad));
  }
}
