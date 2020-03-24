package org.pmiops.workbench.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SumoLogicController implements SumoLogicApiDelegate {

  public static final String SUMOLOGIC_KEY_FILENAME = "inbound-sumologic-keys.txt";

  private static final Logger log = Logger.getLogger(SumoLogicController.class.getName());
  private final EgressEventAuditor egressEventAuditor;
  private final CloudStorageService cloudStorageService;
  private final OpsGenieService opsGenieService;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  @Autowired
  SumoLogicController(
      EgressEventAuditor egressEventAuditor,
      CloudStorageService cloudStorageService,
      OpsGenieService opsGenieService,
      Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.egressEventAuditor = egressEventAuditor;
    this.cloudStorageService = cloudStorageService;
    this.opsGenieService = opsGenieService;
    this.workbenchConfigProvider = workbenchConfigProvider;
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
      log.severe(
          String.format(
              "Failed to parse SumoLogic egress event JSON: %s", request.getEventsJsonArray()));
      log.severe(e.getMessage());
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
        log.severe(
            String.format(
                "Received SumoLogic egress event with bad API key in header: %s",
                request.toString()));
        this.egressEventAuditor.fireBadApiKey(apiKey, request);
        throw new UnauthorizedException("Invalid API key");
      }
    } catch (IOException e) {
      log.severe(
          "Failed to load API keys for SumoLogic request authorization. "
              + "Allowing request to be processed.");
      log.severe(e.getMessage());
    }
  }

  private void handleEgressEvent(EgressEvent event) {
    log.warning(
        String.format(
            "Received an egress event from project %s (%.2fMib, VM %s)",
            event.getProjectName(), event.getEgressMib(), event.getVmName()));
    this.egressEventAuditor.fireEgressEvent(event);
    this.opsGenieService.createEgressEventAlert(event);
  }
}
