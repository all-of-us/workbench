package org.pmiops.workbench.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import org.pmiops.workbench.actionaudit.auditors.SumoLogicAuditor;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.UnauthorizedException;
import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.model.EgressEvent;
import org.pmiops.workbench.model.EgressEventRequest;
import org.pmiops.workbench.model.EmptyResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SumoLogicController implements SumoLogicApiDelegate {

  public static final String SUMOLOGIC_KEY_FILENAME = "inbound-sumologic-keys.txt";

  private static final Logger log = Logger.getLogger(SumoLogicController.class.getName());
  private final SumoLogicAuditor sumoLogicAuditor;
  private final CloudStorageService cloudStorageService;

  @Autowired
  SumoLogicController(SumoLogicAuditor sumoLogicAuditor, CloudStorageService cloudStorageService) {
    this.sumoLogicAuditor = sumoLogicAuditor;
    this.cloudStorageService = cloudStorageService;
  }

  @Override
  public ResponseEntity<EmptyResponse> logEgressEvent(
      String X_API_KEY, EgressEventRequest request) {
    authorizeRequest(X_API_KEY, request);

    try {
      // The "eventsJsonArray" field is a JSON-formatted array of EgressEvent JSON objects. Parse
      // this
      // out so we can work with each event as a model object.
      ObjectMapper mapper = new ObjectMapper();
      EgressEvent[] events = mapper.readValue(request.getEventsJsonArray(), EgressEvent[].class);
      Arrays.stream(events).forEach(event -> handleEgressEvent(event));
      return ResponseEntity.ok(new EmptyResponse());
    } catch (IOException e) {
      log.severe(
          String.format(
              "Failed to parse SumoLogic egress event JSON: %s", request.getEventsJsonArray()));
      log.severe(e.getMessage());
      this.sumoLogicAuditor.fireFailedToParseEgressEvent(request);
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
  private List<String> getSumoLogicApiKeys() throws IOException {
    String apiKeys = cloudStorageService.getCredentialsBucketString(SUMOLOGIC_KEY_FILENAME);
    return Arrays.asList(apiKeys.split("[\\r\\n]+"));
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
      List<String> validApiKeys = getSumoLogicApiKeys();
      if (!validApiKeys.contains(apiKey)) {
        log.severe(
            String.format(
                "Received SumoLogic egress event with bad API key in header: %s",
                request.toString()));
        this.sumoLogicAuditor.fireBadApiKeyEgressEvent(apiKey, request);
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
    this.sumoLogicAuditor.fireEgressEvent(event);
  }
}
