package org.pmiops.workbench.api;

import org.pmiops.workbench.exfiltration.EgressRemediationService;
import org.pmiops.workbench.model.ProcessEgressEventRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CloudTaskEgressController implements CloudTaskEgressApiDelegate {

  @Autowired private EgressRemediationService egressRemediationService;

  @Override
  public ResponseEntity<Void> processEgressEvent(ProcessEgressEventRequest request) {
    egressRemediationService.remediateEgressEvent(request.getEventId());
    return ResponseEntity.ok().build();
  }
}
