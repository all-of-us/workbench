package org.pmiops.workbench.api;

import org.pmiops.workbench.model.StatusResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatusController implements StatusApiDelegate {
  StatusController() {}

  @Override
  public ResponseEntity<StatusResponse> getStatus() {
    StatusResponse statusResponse = new StatusResponse();
    return ResponseEntity.ok(statusResponse);
  }
}
