package org.pmiops.workbench.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.ifountain.opsgenie.client.swagger.ApiException;
import com.ifountain.opsgenie.client.swagger.model.CreateAlertRequest;
import com.ifountain.opsgenie.client.swagger.model.SuccessResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.logging.Logger;
import javax.inject.Provider;
import org.pmiops.workbench.actionaudit.auditors.EgressEventAuditor;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.UnauthorizedException;
import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.model.EgressEvent;
import org.pmiops.workbench.model.EgressEventRequest;
import org.pmiops.workbench.opsgenie.OpsGenieService;
import org.pmiops.workbench.workspaceadmin.WorkspaceAdminController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Implements an inbound API that is called by the Broad Institute's SumoLogic installation to
 * report high-egress events detected by SumoLogic. See http://broad.io/vpc-egress-alerting for more
 * details on the end-to-end data flow.
 */
@RestController
public class SumoLogicController implements SumoLogicApiDelegate {

  public static final String SUMOLOGIC_KEY_FILENAME = "inbound-sumologic-keys.txt";
  private static final Logger logger = Logger.getLogger(SumoLogicController.class.getName());

  private final CloudStorageService cloudStorageService;
  private final EgressEventAuditor egressEventAuditor;
  private final OpsGenieService opsGenieService;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private WorkspaceAdminController workspaceAdminController;

  @Autowired
  SumoLogicController(
      EgressEventAuditor egressEventAuditor,
      CloudStorageService cloudStorageService,
      OpsGenieService opsGenieService,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      WorkspaceAdminController workspaceAdminController) {
    this.egressEventAuditor = egressEventAuditor;
    this.cloudStorageService = cloudStorageService;
    this.opsGenieService = opsGenieService;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.workspaceAdminController = workspaceAdminController;
  }

  @Override
  public ResponseEntity<Void> logEgressEvent(String X_API_KEY, EgressEventRequest request) {
    if (!workbenchConfigProvider.get().featureFlags.enableSumoLogicEventHandling) {
      throw new BadRequestException("SumoLogic event handling is disabled in this environment.");
    }

    authorizeRequest(X_API_KEY, request);

    try {
      // The "eventsJsonArray" field is a JSON-formatted array of EgressEvent JSON objects. Parse
      // this out so we can work with each event as a model object.
      ObjectMapper mapper = new ObjectMapper();
      EgressEvent[] events = mapper.readValue(request.getEventsJsonArray(), EgressEvent[].class);
      Arrays.stream(events).forEach(this::handleEgressEvent);
      return ResponseEntity.noContent().build();
    } catch (IOException e) {
      logger.severe(
          String.format(
              "Failed to parse SumoLogic egress event JSON: %s", request.getEventsJsonArray()));
      logger.severe(e.getMessage());
      this.egressEventAuditor.fireFailedToParseEgressEventRequest(request);
      throw new BadRequestException("Error parsing event details");
    }
  }

  /**
   * Reads the SumoLogic keys file to get the list of valid API key strings for inbound SumoLogic
   * requests.
   *
   * @return
   * @throws IOException
   */
  private Set<String> getSumoLogicApiKeys() throws IOException {
    String apiKeys = cloudStorageService.getCredentialsBucketString(SUMOLOGIC_KEY_FILENAME);
    return Sets.newHashSet(apiKeys.split("[\\r\\n]+"));
  }

  /**
   * Authorizes an inbound EgressEventRequest with a given API key. If the API key is invalid, a
   * generic audit event is logged.
   *
   * @param apiKey
   * @param request
   */
  private void authorizeRequest(String apiKey, EgressEventRequest request) {
    try {
      Set<String> validApiKeys = getSumoLogicApiKeys();
      if (!validApiKeys.contains(apiKey)) {
        logger.severe(
            String.format(
                "Received SumoLogic egress event with bad API key in header: %s",
                request.toString()));
        this.egressEventAuditor.fireBadApiKey(apiKey, request);
        throw new UnauthorizedException("Invalid API key");
      }
    } catch (IOException e) {
      logger.severe(
          "Failed to load API keys for SumoLogic request authorization. "
              + "Allowing request to be processed.");
      logger.severe(e.getMessage());
    }
  }

  private void handleEgressEvent(EgressEvent event) {
    logger.warning(
        String.format(
            "Received an egress event from project %s (%.2fMib, VM %s)",
            event.getProjectName(), event.getEgressMib(), event.getVmName()));
    this.egressEventAuditor.fireEgressEvent(event);
    this.createEgressEventAlert(event);
  }

  private void createEgressEventAlert(EgressEvent egressEvent) {
    final CreateAlertRequest createAlertRequest = egressEventToOpsGenieAlert(egressEvent);
    try {
      final SuccessResponse response = opsGenieService.createAlert(createAlertRequest);
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
    request.setDescription(
        String.format("Workspace project: %s\n", egressEvent.getProjectName())
            + String.format("Workspace Name (Jupyter VM name): %s\n", egressEvent.getVmName())
            + String.format(
                "Egress detected: %.2f Mib in %d secs\n\n",
                egressEvent.getEgressMib(), egressEvent.getTimeWindowDuration())
            + String.format(
                "Workspace Admin Console (Prod Admin User): %s/admin/workspaces/%s/\n",
                workbenchConfigProvider.get().server.uiBaseUrl, egressEvent.getProjectName())
            + "Playbook Entry: https://broad.io/aou-high-egress-event");

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
}
