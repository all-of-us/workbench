package org.pmiops.workbench.api;

import org.pmiops.workbench.model.VwbEgressEventRequest;
import org.springframework.http.ResponseEntity;

public class VwbEgressAdminController implements VwbEgressAdminApiDelegate {
  @Override
  public ResponseEntity<Void> createVwbEgressEvent(VwbEgressEventRequest body) {
    return ResponseEntity.ok().build();
  }
}
