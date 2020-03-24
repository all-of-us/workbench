package org.pmiops.workbench.opsgenie;

import com.ifountain.opsgenie.client.swagger.ApiException;
import com.ifountain.opsgenie.client.swagger.api.AlertApi;
import com.ifountain.opsgenie.client.swagger.model.CreateAlertRequest;
import java.util.logging.Logger;
import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.model.EgressEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OpsGenieServiceImpl implements OpsGenieService {
  private static final Logger logger = Logger.getLogger(OpsGenieServiceImpl.class.getName());

  private Provider<AlertApi> alertApiProvider;
  private Provider<WorkbenchConfig> workbenchConfigProvider;

  @Autowired
  public OpsGenieServiceImpl(
      Provider<AlertApi> alertApiProvider, Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.alertApiProvider = alertApiProvider;
    this.workbenchConfigProvider = workbenchConfigProvider;
  }

  private CreateAlertRequest egressEventToOpsGenieAlert(
      EgressEvent egressEvent, WorkbenchConfig workbenchConfig) {
    CreateAlertRequest request = new CreateAlertRequest();
    request.setMessage(String.format("High-egress event (%s)", egressEvent.getProjectName()));
    request.setDescription(
        new StringBuilder()
            .append(String.format("Workspace project: %s\n", egressEvent.getProjectName()))
            .append(String.format("VM name: %s\n", egressEvent.getVmName()))
            .append(String.format("Egress amount: %.2f Mib\n\n", egressEvent.getEgressMib()))
            .append(
                String.format(
                    "Admin link (PMI-Ops): %s/admin/workspaces/%s/\n",
                    workbenchConfig.server.clientBaseUrl, egressEvent.getProjectName()))
            .append("Playbook: https://broad.io/aou-high-egress-event")
            .toString());
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
    // Set the alias, which is Opsgenie's string key for alert de-duplication. See
    // https://docs.opsgenie.com/docs/alert-deduplication
    request.setAlias(egressEvent.getProjectName() + " - " + egressEvent.getVmName());
    return request;
  }

  @Override
  public void createEgressEventAlert(EgressEvent egressEvent) {
    try {
      this.alertApiProvider
          .get()
          .createAlert(egressEventToOpsGenieAlert(egressEvent, workbenchConfigProvider.get()));
    } catch (ApiException e) {
      logger.severe(
          String.format(
              "Error creating OpsGenie alert for egress event on project %s: %s",
              egressEvent.getProjectName(), e.getMessage()));
      e.printStackTrace();
    }
  }
}
