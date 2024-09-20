package org.pmiops.workbench.api;

import jakarta.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.featuredworkspace.FeaturedWorkspaceService;
import org.pmiops.workbench.model.FeaturedWorkspaceCategory;
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
    return ResponseEntity.ok(
        new WorkspaceResponseListResponse().items(workspaceService.getFeaturedWorkspaces()));
  }

  @Override
  public ResponseEntity<WorkspaceResponseListResponse> getFeaturedWorkspacesByCategory(
      String category) {
    FeaturedWorkspaceCategory requestedCategory = FeaturedWorkspaceCategory.fromValue(category);

    if (requestedCategory == null) {
      throw new BadRequestException("Invalid featured workspace category: " + category);
    }

    return ResponseEntity.ok(
        new WorkspaceResponseListResponse()
            .items(
                featuredWorkspaceService.getWorkspaceResponseByFeaturedCategory(
                    requestedCategory)));
  }
}
