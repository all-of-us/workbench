package org.pmiops.workbench.opsgenie;

import com.google.common.collect.ImmutableList;
import com.ifountain.opsgenie.client.swagger.ApiException;
import com.ifountain.opsgenie.client.swagger.api.AlertApi;
import com.ifountain.opsgenie.client.swagger.model.CreateAlertRequest;
import com.ifountain.opsgenie.client.swagger.model.SuccessResponse;
import java.util.logging.Logger;
import javax.inject.Provider;
import org.jetbrains.annotations.NotNull;
import org.pmiops.workbench.actionaudit.auditors.EgressEventAuditor;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.model.EgressEvent;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.model.WorkspaceAdminView;
import org.pmiops.workbench.workspaceadmin.WorkspaceAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EgressEventServiceImpl implements EgressEventService {
  private static final Logger logger = Logger.getLogger(EgressEventServiceImpl.class.getName());

  private EgressEventAuditor egressEventAuditor;
  private Provider<AlertApi> alertApiProvider;
  private Provider<WorkbenchConfig> workbenchConfigProvider;
  private WorkspaceAdminService workspaceAdminService;

  @Autowired
  public EgressEventServiceImpl(
      EgressEventAuditor egressEventAuditor,
      Provider<AlertApi> alertApiProvider,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      WorkspaceAdminService workspaceAdminService) {
    this.egressEventAuditor = egressEventAuditor;
    this.alertApiProvider = alertApiProvider;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.workspaceAdminService = workspaceAdminService;
  }

  @Override
  public void handleEvent(EgressEvent event) {
    logger.warning(
        String.format(
            "Received an egress event from project %s (%.2fMib, VM %s)",
            event.getProjectName(), event.getEgressMib(), event.getVmName()));
    this.egressEventAuditor.fireEgressEvent(event);
    this.createEgressEventAlert(event);
  }

  // Create (or potentially update) an OpsGenie alert for an egress event.
  private SuccessResponse createAlert(CreateAlertRequest createAlertRequest) throws ApiException {
    return this.alertApiProvider.get().createAlert(createAlertRequest);
  }

  private void createEgressEventAlert(EgressEvent egressEvent) {
    final CreateAlertRequest createAlertRequest = egressEventToOpsGenieAlert(egressEvent);
    try {
      final SuccessResponse response = createAlert(createAlertRequest);
      logger.info(
          String.format(
              "Successfully created or updated Opsgenie alert for high-egress event on project %s (Opsgenie request ID %s)",
              egressEvent.getProjectName(), response.getRequestId()));
    } catch (ApiException e) {
      logger.severe(
          String.format(
              "Error creating Opsgenie alert for egress event on project %s: %s",
              egressEvent.getProjectName(), e.getMessage()));
      e.printStackTrace();
    }
  }

  private CreateAlertRequest egressEventToOpsGenieAlert(EgressEvent egressEvent) {
    final CreateAlertRequest request = new CreateAlertRequest();
    request.setMessage(String.format("High-egress event (%s)", egressEvent.getProjectName()));
    //    final DbWorkspace dbWorkspace =
    // workspaceDao.findAllByWorkspaceNamespace(egressEvent.getVmName());
    request.setDescription(getDescription(egressEvent));

    // Add a note with some more specific details about the alerting criteria and threshold. Notes
    // are appended to an existing Opsgenie ticket if this request is de-duplicated against an
    // existing ticket, so they're a helpful way to summarize temporal updates to the status of
    // an incident.
    request.setNote(
        String.format(
            "Time window: %d secs, threshold: %.2f Mib, observed: %.2f Mib",
            egressEvent.getTimeWindowDuration(),
            egressEvent.getEgressMibThreshold(),
            egressEvent.getEgressMib()));
    request.setTags(ImmutableList.of("high-egress-event"));

    // Set the alias, which is Opsgenie's string key for alert de-duplication. See
    // https://docs.opsgenie.com/docs/alert-deduplication
    request.setAlias(egressEvent.getProjectName() + " | " + egressEvent.getVmName());
    return request;
  }

  @NotNull
  private String getDescription(EgressEvent egressEvent) {
    final WorkspaceAdminView adminWorkspace =
        workspaceAdminService.getWorkspaceAdminView(egressEvent.getProjectName());
    final Workspace workspace = adminWorkspace.getWorkspace();

    return String.format("Workspace \"%s\"", workspace.getName())
        + String.format(
            "GCP Billing Project/Firecloud Namespace: %s\n", egressEvent.getProjectName())
        + String.format("Notebook Jupyter VM name: %s\n", egressEvent.getVmName())
        + String.format("MySQL workspace_id: %d", adminWorkspace.getWorkspaceDatabaseId())
        + String.format(
            "Egress detected: %.2f Mib in %d secs\n\n",
            egressEvent.getEgressMib(), egressEvent.getTimeWindowDuration())
        + String.format(
            "Workspace Admin Console (Prod Admin User): %s/admin/workspaces/%s/\n",
            workbenchConfigProvider.get().server.uiBaseUrl, egressEvent.getProjectName())
        + "Playbook Entry: https://broad.io/aou-high-egress-event";
  }
}
