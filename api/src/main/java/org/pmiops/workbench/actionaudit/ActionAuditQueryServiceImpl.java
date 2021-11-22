package org.pmiops.workbench.actionaudit;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.TableResult;
import com.google.common.collect.ImmutableMap;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import javax.inject.Provider;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.cohortbuilder.util.QueryParameterValues;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.config.WorkbenchConfig.ActionAuditConfig;
import org.pmiops.workbench.model.AuditLogEntry;
import org.pmiops.workbench.model.UserAuditLogQueryResponse;
import org.pmiops.workbench.model.WorkspaceAuditLogQueryResponse;
import org.pmiops.workbench.utils.mappers.AuditLogEntryMapper;
import org.springframework.stereotype.Service;

@Service
public class ActionAuditQueryServiceImpl implements ActionAuditQueryService {

  private static final long MAX_QUERY_LIMIT = 1000L;
  private static final String QUERY_FORMAT =
      "SELECT\n"
          + "  TIMESTAMP_MILLIS(CAST(jsonPayload.timestamp AS INT64)) as event_time,\n"
          + "  jsonPayload.agent_type AS agent_type,\n"
          + "  CAST(jsonPayload.agent_id AS INT64) AS agent_id,\n"
          + "  jsonPayload.agent_email AS agent_username,\n"
          + "  jsonPayload.action_id AS action_id,\n"
          + "  jsonPayload.action_type AS action_type,\n"
          + "  jsonPayload.target_type AS target_type,\n"
          + "  CAST(jsonPayload.target_id AS INT64) AS target_id,\n"
          + "  jsonPayload.target_property AS target_property,\n"
          + "  jsonPayload.prev_value AS prev_value,\n"
          + "  jsonPayload.new_value AS new_value\n"
          + "FROM %s\n"
          + "WHERE %s\n"
          + "  AND @after <= TIMESTAMP_MILLIS(CAST(jsonPayload.timestamp AS INT64))\n"
          + "  AND TIMESTAMP_MILLIS(CAST(jsonPayload.timestamp AS INT64)) < @before\n"
          + "  AND @after_partition_time <= %s AND %<s < @before_partition_time\n"
          + "ORDER BY event_time DESC, agent_id, action_id\n"
          + "LIMIT @limit;";

  private final AuditLogEntryMapper auditLogEntryMapper;
  private final BigQueryService bigQueryService;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private static final Duration PARTITION_BUFFER = Duration.ofDays(1);

  public ActionAuditQueryServiceImpl(
      AuditLogEntryMapper auditLogEntryMapper,
      BigQueryService bigQueryService,
      Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.auditLogEntryMapper = auditLogEntryMapper;
    this.bigQueryService = bigQueryService;
    this.workbenchConfigProvider = workbenchConfigProvider;
  }

  @Override
  public WorkspaceAuditLogQueryResponse queryEventsForWorkspace(
      long workspaceDatabaseId, long limit, Instant after, Instant before) {
    final String whereClausePrefix =
        "jsonPayload.target_id = @workspace_db_id AND\n"
            + "  jsonPayload.target_type = 'WORKSPACE'\n";
    final String queryString =
        String.format(
            QUERY_FORMAT,
            getTableName(),
            whereClausePrefix,
            workbenchConfigProvider.get().actionAudit.partitionColumn);

    final QueryJobConfiguration queryJobConfiguration =
        QueryJobConfiguration.newBuilder(queryString)
            .setNamedParameters(
                getNamedParameterMapBuilder(limit, after, before)
                    .put("workspace_db_id", QueryParameterValue.int64(workspaceDatabaseId))
                    .build())
            .build();

    final TableResult tableResult = bigQueryService.executeQuery(queryJobConfiguration);
    final List<AuditLogEntry> logEntries = auditLogEntryMapper.tableResultToLogEntries(tableResult);

    return new WorkspaceAuditLogQueryResponse()
        .logEntries(logEntries)
        .query(QueryParameterValues.replaceNamedParameters(queryJobConfiguration))
        .workspaceDatabaseId(workspaceDatabaseId)
        .actions(auditLogEntryMapper.logEntriesToActions(logEntries));
  }

  private String getTableName() {
    final ActionAuditConfig actionAuditConfig = workbenchConfigProvider.get().actionAudit;
    return String.format(
        "`%s.%s.%s`",
        workbenchConfigProvider.get().server.projectId,
        actionAuditConfig.bigQueryDataset,
        actionAuditConfig.bigQueryTable);
  }

  @Override
  public UserAuditLogQueryResponse queryEventsForUser(
      long userDatabaseId, long limit, Instant after, Instant before) {

    // Workaround RW-5289 by omitting all LOGIN events from the result set. Otherwise
    // they crowd out all the real events.
    final String whereClausePrefix =
        "((jsonPayload.target_id = @user_db_id AND jsonPayload.target_type = 'USER') OR\n"
            + "  (jsonPayload.agent_id = @user_db_id AND jsonPayload.agent_type = 'USER')) AND\n"
            + "  jsonPayload.action_type != 'LOGIN'";
    final String queryString =
        String.format(
            QUERY_FORMAT,
            getTableName(),
            whereClausePrefix,
            workbenchConfigProvider.get().actionAudit.partitionColumn);

    final QueryJobConfiguration queryJobConfiguration =
        QueryJobConfiguration.newBuilder(queryString)
            .setNamedParameters(
                getNamedParameterMapBuilder(limit, after, before)
                    .put("user_db_id", QueryParameterValue.int64(userDatabaseId))
                    .build())
            .build();

    final TableResult tableResult = bigQueryService.executeQuery(queryJobConfiguration);

    final List<AuditLogEntry> logEntries = auditLogEntryMapper.tableResultToLogEntries(tableResult);
    final String formattedQuery =
        QueryParameterValues.formatQuery(
            QueryParameterValues.replaceNamedParameters(queryJobConfiguration));

    return new UserAuditLogQueryResponse()
        .actions(auditLogEntryMapper.logEntriesToActions(logEntries))
        .logEntries(logEntries)
        .query(formattedQuery)
        .userDatabaseId(userDatabaseId);
  }

  private ImmutableMap.Builder<String, QueryParameterValue> getNamedParameterMapBuilder(
      long limit, Instant after, Instant before) {
    final Instant afterPartitionTime = after.minus(PARTITION_BUFFER);
    final Instant beforePartitionTime = before.plus(PARTITION_BUFFER);

    return ImmutableMap.<String, QueryParameterValue>builder()
        .put("limit", QueryParameterValue.int64(Math.min(limit, MAX_QUERY_LIMIT)))
        .put("after", QueryParameterValues.instantToQPValue(after))
        .put("before", QueryParameterValues.instantToQPValue(before))
        .put("after_partition_time", QueryParameterValues.instantToQPValue(afterPartitionTime))
        .put("before_partition_time", QueryParameterValues.instantToQPValue(beforePartitionTime));
  }
}
