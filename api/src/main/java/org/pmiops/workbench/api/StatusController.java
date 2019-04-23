package org.pmiops.workbench.api;

import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.StatusResponse;
import org.pmiops.workbench.notebooks.LeonoardoNotebooksClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatusController implements StatusApiDelegate {

  private final FireCloudService fireCloudService;
  private final LeonoardoNotebooksClient leonoardoNotebooksClient;

  @Autowired
  StatusController(FireCloudService fireCloudService,
      LeonoardoNotebooksClient leonoardoNotebooksClient) {
    this.fireCloudService = fireCloudService;
    this.leonoardoNotebooksClient = leonoardoNotebooksClient;
  }

  @Override
  public ResponseEntity<StatusResponse> getStatus() {
    StatusResponse statusResponse = new StatusResponse();
    statusResponse.setFirecloudStatus(fireCloudService.getFirecloudStatus());
    statusResponse.setNotebooksStatus(leonoardoNotebooksClient.getNotebooksStatus());
    return ResponseEntity.ok(statusResponse);
  }
}
