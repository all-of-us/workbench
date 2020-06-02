package org.pmiops.workbench.actionaudit;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.StandardSQLTypeName;
import com.google.cloud.bigquery.TableResult;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Provider;
import org.joda.time.DateTime;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.config.WorkbenchConfig.ActionAuditConfig;
import org.pmiops.workbench.model.AuditLogEntry;
import org.pmiops.workbench.model.UserAuditLogQueryResponse;
import org.pmiops.workbench.model.WorkspaceAuditLogQueryResponse;
import org.pmiops.workbench.utils.mappers.AuditLogEntryMapper;
import org.springframework.stereotype.Service;

@Service
public class ActionAuditQueryServiceImpl implements ActionAuditQueryService {

  private static final int MICROSECONDS_IN_MILLISECOND = 1000;
  private static final long MAX_QUERY_LIMIT = 1000L;

  private final AuditLogEntryMapper auditLogEntryMapper;
  private final BigQueryService bigQueryService;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;

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
      long workspaceDatabaseId, long limit, DateTime after, DateTime before) {

    final String queryString =
        String.format(
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
                + "WHERE jsonPayload.target_id = @workspace_db_id AND\n"
                + "  jsonPayload.target_type = 'WORKSPACE' AND\n"
                + "  @after <= TIMESTAMP_MILLIS(CAST(jsonPayload.timestamp AS INT64)) AND\n"
                + "  TIMESTAMP_MILLIS(CAST(jsonPayload.timestamp AS INT64)) < @before\n"
                + "ORDER BY event_time, agent_id, action_id\n"
                + "LIMIT @limit",
            getTableName());

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
        .query(getReplacedQueryText(queryJobConfiguration))
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
      long userDatabaseId, long limit, DateTime after, DateTime before) {

    final String queryString =
        String.format(
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
                + "WHERE ((jsonPayload.target_id = @user_db_id AND jsonPayload.target_type = 'USER') OR\n"
                + "  (jsonPayload.agent_id = @user_db_id AND jsonPayload.agent_type = 'USER')) AND\n"
                + "  @after <= TIMESTAMP_MILLIS(CAST(jsonPayload.timestamp AS INT64)) AND\n"
                + "  TIMESTAMP_MILLIS(CAST(jsonPayload.timestamp AS INT64)) < @before\n"
                + "ORDER BY event_time, agent_id, action_id\n"
                + "LIMIT @limit",
            getTableName());

    final QueryJobConfiguration queryJobConfiguration =
        QueryJobConfiguration.newBuilder(queryString)
            .setNamedParameters(
                getNamedParameterMapBuilder(limit, after, before)
                    .put("user_db_id", QueryParameterValue.int64(userDatabaseId))
                    .build())
            .build();

    final TableResult tableResult = bigQueryService.executeQuery(queryJobConfiguration);

    final List<AuditLogEntry> logEntries = auditLogEntryMapper.tableResultToLogEntries(tableResult);

    return new UserAuditLogQueryResponse()
        .actions(auditLogEntryMapper.logEntriesToActions(logEntries))
        .logEntries(logEntries)
        .query(getReplacedQueryText(queryJobConfiguration))
        .userDatabaseId(userDatabaseId);
  }

  private ImmutableMap.Builder<String, QueryParameterValue> getNamedParameterMapBuilder(
      long limit, DateTime after, DateTime before) {
    return ImmutableMap.<String, QueryParameterValue>builder()
        .put("limit", QueryParameterValue.int64(Math.max(limit, MAX_QUERY_LIMIT)))
        .put(
            "after", QueryParameterValue.timestamp(after.getMillis() * MICROSECONDS_IN_MILLISECOND))
        .put(
            "before",
            QueryParameterValue.timestamp(before.getMillis() * MICROSECONDS_IN_MILLISECOND));
  }

  private String getReplacedQueryText(QueryJobConfiguration queryJobConfiguration) {
    String result = queryJobConfiguration.getQuery();

    for (Map.Entry<String, QueryParameterValue> entry :
        queryJobConfiguration.getNamedParameters().entrySet()) {
      String parameterName = "@" + entry.getKey();
      final QueryParameterValue parameterValue = entry.getValue();
      final String rawStringValue = Optional.ofNullable(parameterValue.getValue()).orElse("NULL");
      final String replacement;
      if (parameterValue.getType() == StandardSQLTypeName.TIMESTAMP) {
        replacement = String.format("TIMESTAMP '%s'", rawStringValue);
      } else {
        replacement = rawStringValue;
      }
      result = result.replace(parameterName, replacement);
    }
    result = result.replace("\n", " ");
    return result;
  }
}
