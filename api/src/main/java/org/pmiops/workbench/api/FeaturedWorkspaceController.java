package org.pmiops.workbench.api;

import org.pmiops.workbench.model.FeaturedWorkspacesConfigResponse;
import org.pmiops.workbench.utils.FeaturedWorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FeaturedWorkspaceController implements FeaturedWorkspaceApiDelegate {

  private final FeaturedWorkspaceService featuredWorkspaceService;

  @Autowired
  public FeaturedWorkspaceController(FeaturedWorkspaceService featuredWorkspaceService) {
    this.featuredWorkspaceService = featuredWorkspaceService;
  }

  @Override
  public ResponseEntity<FeaturedWorkspacesConfigResponse> getFeaturedWorkspacesFromTable() {
    FeaturedWorkspacesConfigResponse response = new FeaturedWorkspacesConfigResponse();
    response.featuredWorkspacesList(featuredWorkspaceService.getAllFeaturedWorkspaces());
    return ResponseEntity.ok(response);
  }
}
