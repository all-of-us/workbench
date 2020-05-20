package org.pmiops.workbench.actionaudit;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableResult;
import java.util.Collections;
import javax.inject.Provider;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.model.AuditLogEntriesResponse;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.stereotype.Service;

@Service
public class ActionAuditQueryServiceImpl implements ActionAuditQueryService {

  private BigQueryService bigQueryService;
  private Provider<WorkbenchConfig> workbenchConfigProvider;
  private WorkspaceService workspaceService;

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
    final String table =
        "`all-of-us-workbench-test.workbench_action_audit_test.workbench_action_audit_test`";

    final String queryStringFormat =
        "SELECT\n"
            + "  TIMESTAMP_MILLIS(CAST(jsonPayload.timestamp AS INT64)) as event_time,\n"
            + "  jsonPayload.agent_type AS agent_type,\n"
            + "  CAST(jsonPayload.agent_id AS INT64) AS agent_id,\n"
            + "  jsonPayload.agent_email AS agent_email,\n"
            + "  jsonPayload.action_id AS action_id,\n"
            + "  jsonPayload.action_type AS action_type,\n"
            + "  jsonPayload.target_type AS target_type,\n"
            + "  CAST(jsonPayload.target_id AS INT64) AS target_id,\n"
            + "  jsonPayload.target_property AS target_property,\n"
            + "  jsonPayload.prev_value AS prev_value,\n"
            + "  jsonPayload.new_value AS new_value\n"
            + "FROM %s\n"
            + "WHERE jsonPayload.agent_type = 'USER' AND\n"
            + "  jsonPayload.target_id = %d AND\n"
            + "  jsonPayload.target_type = 'WORKSPACE'\n"
            + "ORDER BY event_time, agent_id, action_id\n"
            + "LIMIT %d";

    final String queryString = String.format(queryStringFormat, table, workspaceDatabaseId, limit);
    final QueryJobConfiguration queryJobConfiguration =
        QueryJobConfiguration.newBuilder(queryString).setUseLegacySql(false).build();
    final TableResult tableResult = bigQueryService.executeQuery(queryJobConfiguration);
    return new AuditLogEntriesResponse()
        .logEntries(Collections.emptyList())
        .queryMetadata(Collections.emptyMap());
  }
}
