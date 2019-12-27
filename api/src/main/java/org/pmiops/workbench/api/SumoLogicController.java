package org.pmiops.workbench.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import org.pmiops.workbench.actionaudit.auditors.SumoLogicAuditor;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.EgressEvent;
import org.pmiops.workbench.model.EgressEventRequest;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SumoLogicController implements SumoLogicApiDelegate {

  private static final Logger log = Logger.getLogger(SumoLogicController.class.getName());
  private final SumoLogicAuditor sumoLogicAuditor;

  @Autowired
  SumoLogicController(SumoLogicAuditor sumoLogicAuditor) {
    this.sumoLogicAuditor = sumoLogicAuditor;
  }

  @Override
  public ResponseEntity<EmptyResponse> logEgressEvent(EgressEventRequest request) {
    try {
      // The "eventsJsonArray" field is a JSON-formatted array of EgressEvent JSON objects. Parse this
      // out so we can work with each event as a model object.
      ObjectMapper mapper = new ObjectMapper();
      EgressEvent[] events = mapper.readValue(request.getEventsJsonArray(), EgressEvent[].class);
      Arrays.stream(events).forEach(event -> handleEgressEvent(event));
      return ResponseEntity.ok(new EmptyResponse());
    } catch (IOException e) {
      log.severe(String.format("Failed to parse SumoLogic egress event JSON: %s",
          request.getEventsJsonArray()));
      this.sumoLogicAuditor.fireFailedToParseEgressEvent(request);
      throw new BadRequestException("Error parsing high-egress event details");
    }
  }

  private void handleEgressEvent(EgressEvent event) {
    log.warning(
        String.format(
            "Received an egressz event from project %s (%.2fMib, VM %s)",
            event.getProjectName(), event.getEgressMib(), event.getVmName()));
    this.sumoLogicAuditor.fireEgressEvent(event);
  }
}
