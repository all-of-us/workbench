package org.pmiops.workbench.api;

import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.legacy_leonardo_client.LeonardoApiClient;
import org.pmiops.workbench.model.StatusResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatusController implements StatusApiDelegate {

  private final FireCloudService fireCloudService;
  private final LeonardoApiClient leonardoApiClient;

  @Autowired
  StatusController(FireCloudService fireCloudService, LeonardoApiClient leonardoApiClient) {
    this.fireCloudService = fireCloudService;
    this.leonardoApiClient = leonardoApiClient;
  }

  @Override
  public ResponseEntity<StatusResponse> getStatus() {
    StatusResponse statusResponse = new StatusResponse();
    statusResponse.setFirecloudStatus(fireCloudService.getFirecloudStatus());
    statusResponse.setNotebooksStatus(leonardoApiClient.getLeonardoStatus());
    return ResponseEntity.ok(statusResponse);
  }
}
