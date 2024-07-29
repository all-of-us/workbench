package org.pmiops.workbench.api;

import jakarta.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.exceptions.NotImplementedException;
import org.pmiops.workbench.featuredworkspace.FeaturedWorkspaceService;
import org.pmiops.workbench.model.WorkspaceResponseListResponse;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FeaturedWorkspaceController implements FeaturedWorkspaceApiDelegate {

  private final FeaturedWorkspaceService featuredWorkspaceService;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final WorkspaceService workspaceService;

  @Autowired
  FeaturedWorkspaceController(
      FeaturedWorkspaceService featuredWorkspaceService,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      WorkspaceService workspaceService) {
    this.featuredWorkspaceService = featuredWorkspaceService;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.workspaceService = workspaceService;
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
          new WorkspaceResponseListResponse().items(workspaceService.getFeaturedWorkspaces()));
    } else {
      throw new NotImplementedException(
          "Not implemented in this environment: combine the results of getFeaturedWorkspacesConfig() and getPublishedWorkspaces() to generate the list of featured workspaces.");
    }
  }

  @Override
  public ResponseEntity<WorkspaceResponseListResponse> getFeaturedWorkspacesByCategory(
      String category) {
    return ResponseEntity.ok(
        new WorkspaceResponseListResponse()
            .items(featuredWorkspaceService.getByFeaturedCategory(category)));
  }
}
