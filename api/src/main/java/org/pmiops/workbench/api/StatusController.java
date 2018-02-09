package org.pmiops.workbench.api;

import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.StatusResponse;
import org.pmiops.workbench.notebooks.NotebooksService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatusController implements StatusApiDelegate {

  private final FireCloudService fireCloudService;
  private final NotebooksService notebooksService;

  @Autowired
  StatusController(FireCloudService fireCloudService,
      NotebooksService notebooksService) {
    this.fireCloudService = fireCloudService;
    this.notebooksService = notebooksService;
  }

  @Override
  public ResponseEntity<StatusResponse> getStatus() {
    StatusResponse statusResponse = new StatusResponse();
    // TODO: Check mailchimp and blockscore
    statusResponse.setFirecloudStatus(fireCloudService.getFirecloudStatus());
    statusResponse.setNotebooksStatus(notebooksService.getNotebooksStatus());
    return ResponseEntity.ok(statusResponse);
  }
}
