package org.pmiops.workbench.actionaudit;

import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableResult;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.stream.StreamSupport;
import javax.inject.Provider;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.config.WorkbenchConfig.ActionAuditConfig;
import org.pmiops.workbench.model.AuditLogEntriesResponse;
import org.pmiops.workbench.model.AuditLogEntry;
import org.pmiops.workbench.utils.FieldValues;
import org.springframework.stereotype.Service;

@Service
public class ActionAuditQueryServiceImpl implements ActionAuditQueryService {

  private BigQueryService bigQueryService;
  private Provider<WorkbenchConfig> workbenchConfigProvider;
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
          + "WHERE jsonPayload.target_id = %d AND\n"
          + "  jsonPayload.target_type = 'WORKSPACE'\n"
          + "ORDER BY event_time, agent_id, action_id\n"
          + "LIMIT %d";

  public ActionAuditQueryServiceImpl(
      BigQueryService bigQueryService, Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.bigQueryService = bigQueryService;
    this.workbenchConfigProvider = workbenchConfigProvider;
  }

  @Override
  public AuditLogEntriesResponse queryEventsForWorkspace(long workspaceDatabaseId, long limit) {
    final ActionAuditConfig actionAuditConfig = workbenchConfigProvider.get().actionAudit;
    final String fullyQualifiedTableName =
        String.format(
            "`%s.%s.%s`",
            workbenchConfigProvider.get().server.projectId,
            actionAuditConfig.bigQueryDataset,
            actionAuditConfig.bigQueryTable);

    final String queryString =
        String.format(
            WORKSPACE_EVENTS_QUERY_STRING_FORMAT,
            fullyQualifiedTableName,
            workspaceDatabaseId,
            limit);
    final QueryJobConfiguration queryJobConfiguration =
        QueryJobConfiguration.newBuilder(queryString).setUseLegacySql(false).build();

    final TableResult tableResult = bigQueryService.executeQuery(queryJobConfiguration);

    // Transform all results on all pages.
    final List<AuditLogEntry> logEntries =
        StreamSupport.stream(tableResult.iterateAll().spliterator(), false)
            .map(this::fieldValueListToAditLogEntry)
            .collect(ImmutableList.toImmutableList());

    final ImmutableMap<String, String> metadata =
        ImmutableMap.of(
            "workspaceDatabaseId", Long.toString(workspaceDatabaseId), "query", queryString);

    return new AuditLogEntriesResponse().logEntries(logEntries).queryMetadata(metadata);
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
}
