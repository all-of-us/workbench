package org.pmiops.workbench.api;

import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.leonardo.LeonardoApiClient;
import org.pmiops.workbench.model.StatusResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatusController implements StatusApiDelegate {

  private final FireCloudService fireCloudService;
  private final LeonardoApiClient leonardoNotebooksClient;

  @Autowired
  StatusController(FireCloudService fireCloudService, LeonardoApiClient leonardoNotebooksClient) {
    this.fireCloudService = fireCloudService;
    this.leonardoNotebooksClient = leonardoNotebooksClient;
  }

  @Override
  public ResponseEntity<StatusResponse> getStatus() {
    StatusResponse statusResponse = new StatusResponse();
    statusResponse.setFirecloudStatus(fireCloudService.getFirecloudStatus());
    statusResponse.setNotebooksStatus(leonardoNotebooksClient.getLeonardoStatus());
    return ResponseEntity.ok(statusResponse);
  }
}
