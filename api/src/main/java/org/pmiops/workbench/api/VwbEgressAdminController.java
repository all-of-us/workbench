package org.pmiops.workbench.api;

import org.pmiops.workbench.exfiltration.EgressEventService;
import org.pmiops.workbench.model.VwbEgressEventRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VwbEgressAdminController implements VwbEgressAdminApiDelegate {
  private final EgressEventService egressEventService;

  @Autowired
  public VwbEgressAdminController(EgressEventService egressEventService) {
    this.egressEventService = egressEventService;
  }

  @Override
  public ResponseEntity<Void> createVwbEgressEvent(VwbEgressEventRequest body) {
    egressEventService.handleVwbEvent(body);
    return ResponseEntity.ok().build();
  }
}
