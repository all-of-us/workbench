package org.pmiops.workbench.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Provider;
import org.pmiops.workbench.actionaudit.auditors.EgressEventAuditor;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.UnauthorizedException;
import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.model.EgressEvent;
import org.pmiops.workbench.model.EgressEventRequest;
import org.pmiops.workbench.opsgenie.EgressEventService;
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
  private static final Logger log = Logger.getLogger(SumoLogicController.class.getName());

  private final CloudStorageService cloudStorageService;
  private final EgressEventAuditor egressEventAuditor;
  private final EgressEventService egressEventService;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  @Autowired
  SumoLogicController(
      CloudStorageService cloudStorageService,
      EgressEventAuditor egressEventAuditor,
      EgressEventService egressEventService,
      Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.cloudStorageService = cloudStorageService;
    this.egressEventAuditor = egressEventAuditor;
    this.egressEventService = egressEventService;
    this.workbenchConfigProvider = workbenchConfigProvider;
  }

  @Override
  public ResponseEntity<Void> logEgressEvent(String X_API_KEY, EgressEventRequest request) {
    authorizeRequest(X_API_KEY, request);
    ObjectMapper mapper = new ObjectMapper();
    EgressEvent[] events;
    try {
      // Try to deserialize input with default ObjectMapper configuration. If failed to deserialize,
      // log it then
      // disable unknown properties restriction and try again.
      // The "eventsJsonArray" field is a JSON-formatted array of EgressEvent JSON objects. Parse
      // this out so we can work with each event as a model object.
      events = mapper.readValue(request.getEventsJsonArray(), EgressEvent[].class);
    } catch (JsonProcessingException e) {
      log.log(
          Level.WARNING,
          String.format(
              "Failed to parse SumoLogic egress event JSON: %s. Disabling unknown properties restriction and trying again...",
              request.getEventsJsonArray()),
          e);
      mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
      try {
        events = mapper.readValue(request.getEventsJsonArray(), EgressEvent[].class);
      } catch (IOException ioException) {
        log.log(
            Level.SEVERE,
            String.format(
                "Failed to parse SumoLogic egress event JSON: %s", request.getEventsJsonArray()),
            e);
        this.egressEventAuditor.fireFailedToParseEgressEventRequest(request);
        throw new BadRequestException("Error parsing event details");
      }
    }
    Arrays.stream(events).forEach(egressEventService::handleEvent);
    return ResponseEntity.noContent().build();
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
}
