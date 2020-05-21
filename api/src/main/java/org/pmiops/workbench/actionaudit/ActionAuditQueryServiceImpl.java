package org.pmiops.workbench.actionaudit;

import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableResult;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import javax.inject.Provider;
import org.joda.time.Instant;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.config.WorkbenchConfig.ActionAuditConfig;
import org.pmiops.workbench.model.AuditLogEntriesResponse;
import org.pmiops.workbench.model.AuditLogEntry;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.stereotype.Service;

@Service
public class ActionAuditQueryServiceImpl implements ActionAuditQueryService {

  private BigQueryService bigQueryService;
  private Provider<WorkbenchConfig> workbenchConfigProvider;
  private WorkspaceService workspaceService;
  public static final String WORKSPACE_EVENTS_QUERY_STRING_FORMAT =
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
      BigQueryService bigQueryService,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      WorkspaceService workspaceService) {
    this.bigQueryService = bigQueryService;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.workspaceService = workspaceService;
  }

  @Override
  public AuditLogEntriesResponse queryEventsForWorkspace(long workspaceDatabaseId, long limit) {
    // TODO(jaycarlton) - use config
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

    final List<AuditLogEntry> logEntries =
        StreamSupport.stream(tableResult.iterateAll().spliterator(), false)
            .map(this::fieldValueListToAditLogEntry)
            .collect(ImmutableList.toImmutableList());

    final ImmutableMap<String, String> metadata =
        ImmutableMap.of(
            "workspaceDatabaseId", Long.toString(workspaceDatabaseId), "query", queryString);

    return new AuditLogEntriesResponse().logEntries(logEntries).queryMetadata(metadata);
  }

  AuditLogEntry fieldValueListToAditLogEntry(FieldValueList row) {
    // Define helper methods to fetch the value if non-null for each needed type
    @Nullable
    final Function<String, String> getString = (fieldName) -> getPopulatedFieldValue(row, fieldName)
        .map(FieldValue::getStringValue)
        .orElse(null);

    @Nullable
    final Function<String, Long> getTimestampMillis = (fieldName) -> getPopulatedFieldValue(row, fieldName)
        .map(FieldValue::getTimestampValue)
        .orElse(null);

    @Nullable
    final Function<String, Long> getLong = (fieldName) -> getPopulatedFieldValue(row, fieldName)
        .map(FieldValue::getLongValue)
        .orElse(null);

    return new AuditLogEntry()
        .actionId(Optional.ofNullable(getString.apply("action_id")).map(UUID::fromString).orElse(null))
        .actionType(getString.apply("action_type"))
        .agentId(getLong.apply("agent_id"))
        .agentType(getString.apply("agent_type"))
        .agentUsername(getString.apply("agent_username"))
        .eventTime(new Instant(getTimestampMillis.apply("event_time")).toDateTime())
        .newValue(getString.apply("new_value"))
        .previousValue(getString.apply("prev_value"))
        .targetId(getLong.apply("target_id"))
        .targetProperty(getString.apply("target_property"))
        .targetType(getString.apply("target_type"));
  }

  /**
   * Return an Optional containing a FieldValue if isNull() is false,
   * empty otherwise.
   */
  Optional<FieldValue> getPopulatedFieldValue(FieldValueList row, String fieldName) {
    final FieldValue value = row.get(fieldName);
    if (value.isNull()) {
      return Optional.empty();
    } else {
      return Optional.of(value);
    }
  }

//  @Nullable
//  String getString(FieldValueList row, String fieldName) {
//    return getPopulatedFieldValue(row, fieldName)
//        .map(FieldValue::getStringValue)
//        .orElse(null);
//  }

//  @Nullable
//  Long getLong(FieldValueList row, String fieldName) {
//    return getPopulatedFieldValue(row, fieldName)
//        .map(FieldValue::getLongValue)
//        .orElse(null);
//  }

  @Nullable
  Long getTimestamp(FieldValueList row, String fieldName) {
    return getPopulatedFieldValue(row, fieldName)
        .map(FieldValue::getTimestampValue)
        .orElse(null);

  }
}
