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

  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final WorkspaceService workspaceService;
  private final FeaturedWorkspaceService featuredWorkspaceService;

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

  /**
   * Temporary method to backfill the Featured Workspace DB table with information from the Featured
   * Workspaces Config JSON. This method will be deleted after the backfill process has been
   * completed in all environments using curl.
   *
   * @return List of all Featured workspaces saved in database table featured_workspace
   */
  @Override
  public ResponseEntity<WorkspaceResponseListResponse> backFillFeaturedWorkspaces() {
    featuredWorkspaceService.backFillFeaturedWorkspaces();
    // To confirm the entity stored in Database
    return getFeaturedWorkspaces();
  }
}
