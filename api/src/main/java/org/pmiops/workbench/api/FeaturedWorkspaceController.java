package org.pmiops.workbench.api;

import jakarta.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.exceptions.NotImplementedException;
import org.pmiops.workbench.featuredworkspace.FeaturedWorkspaceService;
import org.pmiops.workbench.model.WorkspaceResponseListResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FeaturedWorkspaceController implements FeaturedWorkspaceApiDelegate {

  private final FeaturedWorkspaceService featuredWorkspaceService;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  @Autowired
  FeaturedWorkspaceController(
      FeaturedWorkspaceService featuredWorkspaceService,
      Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.featuredWorkspaceService = featuredWorkspaceService;
    this.workbenchConfigProvider = workbenchConfigProvider;
  }

  /**
   * Get all Featured workspaces, using the Featured Workspace DB table as the source.
   *
   * @return List of all Featured workspaces
   */
  @Override
  public ResponseEntity<WorkspaceResponseListResponse> getFeaturedWorkspaces() {
    if (workbenchConfigProvider.get().featureFlags.enablePublishedWorkspacesViaDb) {
      return ResponseEntity.ok(
          new WorkspaceResponseListResponse()
              .items(featuredWorkspaceService.getFeaturedWorkspaces()));
    } else {
      throw new NotImplementedException(
          "Not implemented in this environment: combine the results of getFeaturedWorkspacesConfig() and getPublishedWorkspaces() to generate the list of featured workspaces.");
    }
  }
}
