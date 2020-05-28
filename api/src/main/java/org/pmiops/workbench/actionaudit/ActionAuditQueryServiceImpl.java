package org.pmiops.workbench.actionaudit;

import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.StandardSQLTypeName;
import com.google.cloud.bigquery.TableResult;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.StreamSupport;
import javax.inject.Provider;
import org.joda.time.DateTime;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.config.WorkbenchConfig.ActionAuditConfig;
import org.pmiops.workbench.model.AuditLogEntry;
import org.pmiops.workbench.model.WorkspaceAuditLogQueryResponse;
import org.pmiops.workbench.utils.FieldValues;
import org.springframework.stereotype.Service;

@Service
public class ActionAuditQueryServiceImpl implements ActionAuditQueryService {

  private static final int MICROSECONDS_IN_MILLISECOND = 1000;

  enum Parameters {
    LIMIT("limit"),
    WORKSPACE_DB_ID("workspace_db_id"),
    AFTER_INCLUSIVE("after_inclusive"),
    BEFORE_EXCLUSIVE("before_exclusive");

    private String name;

    Parameters(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }
  }

  private final BigQueryService bigQueryService;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  private static final long MAX_QUERY_LIMIT = 1000L;
  // The table name can't be in a QueryParameterValue, so we substitute it with String.format()
  private static final String WORKSPACE_EVENTS_QUERY_STRING_FORMAT =
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
          + "  @after_inclusive <= TIMESTAMP_MILLIS(CAST(jsonPayload.timestamp AS INT64)) AND\n"
          + "  TIMESTAMP_MILLIS(CAST(jsonPayload.timestamp AS INT64)) < @before_exclusive\n"
          + "ORDER BY event_time, agent_id, action_id\n"
          + "LIMIT @limit";

  public ActionAuditQueryServiceImpl(
      BigQueryService bigQueryService, Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.bigQueryService = bigQueryService;
    this.workbenchConfigProvider = workbenchConfigProvider;
  }

  @Override
  public WorkspaceAuditLogQueryResponse queryEventsForWorkspace(
      long workspaceDatabaseId, long limit, DateTime afterInclusive, DateTime beforeExclusive) {
    final ActionAuditConfig actionAuditConfig = workbenchConfigProvider.get().actionAudit;
    final String fullyQualifiedTableName =
        String.format(
            "`%s.%s.%s`",
            workbenchConfigProvider.get().server.projectId,
            actionAuditConfig.bigQueryDataset,
            actionAuditConfig.bigQueryTable);

    final String queryString =
        String.format(WORKSPACE_EVENTS_QUERY_STRING_FORMAT, fullyQualifiedTableName);

    final QueryJobConfiguration queryJobConfiguration =
        QueryJobConfiguration.newBuilder(queryString)
            .setNamedParameters(
                ImmutableMap.of(
                    Parameters.WORKSPACE_DB_ID.getName(),
                        QueryParameterValue.int64(workspaceDatabaseId),
                    Parameters.LIMIT.getName(),
                        QueryParameterValue.int64(Math.max(limit, MAX_QUERY_LIMIT)),
                    Parameters.AFTER_INCLUSIVE.getName(),
                        QueryParameterValue.timestamp(
                            afterInclusive.getMillis() * MICROSECONDS_IN_MILLISECOND),
                    Parameters.BEFORE_EXCLUSIVE.getName(),
                        QueryParameterValue.timestamp(
                            beforeExclusive.getMillis() * MICROSECONDS_IN_MILLISECOND)))
            .build();

    final TableResult tableResult = bigQueryService.executeQuery(queryJobConfiguration);

    final List<AuditLogEntry> logEntries =
        StreamSupport.stream(tableResult.iterateAll().spliterator(), false)
            .map(this::fieldValueListToAditLogEntry)
            .collect(ImmutableList.toImmutableList());

    return new WorkspaceAuditLogQueryResponse()
        .logEntries(logEntries)
        .query(getReplacedQueryText(queryJobConfiguration))
        .workspaceDatabaseId(workspaceDatabaseId);
  }

  private AuditLogEntry fieldValueListToAditLogEntry(FieldValueList row) {
    final AuditLogEntry entry = new AuditLogEntry();
    FieldValues.getString(row, "action_id").ifPresent(entry::setActionId);
    FieldValues.getString(row, "action_type").ifPresent(entry::setActionType);
    FieldValues.getLong(row, "agent_id").ifPresent(entry::setAgentId);
    FieldValues.getString(row, "agent_type").ifPresent(entry::setAgentType);
    FieldValues.getString(row, "agent_username").ifPresent(entry::setAgentUsername);
    FieldValues.getDateTime(row, "event_time").ifPresent(entry::setEventTime);
    FieldValues.getString(row, "new_value").ifPresent(entry::setNewValue);
    FieldValues.getString(row, "prev_value").ifPresent(entry::setPreviousValue);
    FieldValues.getLong(row, "target_id").ifPresent(entry::setTargetId);
    FieldValues.getString(row, "target_property").ifPresent(entry::setTargetProperty);
    FieldValues.getString(row, "target_type").ifPresent(entry::setTargetType);
    return entry;
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
