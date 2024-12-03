package org.pmiops.workbench.api;

import org.pmiops.workbench.model.VwbEgressEventRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VwbEgressAdminController implements VwbEgressAdminApiDelegate {
  @Override
  public ResponseEntity<Void> createVwbEgressEvent(VwbEgressEventRequest body) {
    return ResponseEntity.ok().build();
  }
}
