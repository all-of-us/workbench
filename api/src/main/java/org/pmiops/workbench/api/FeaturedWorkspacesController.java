package org.pmiops.workbench.api;

import jakarta.inject.Provider;
import org.pmiops.workbench.config.FeaturedWorkspacesConfig;
import org.pmiops.workbench.model.FeaturedWorkspacesConfigResponse;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@Deprecated(since = "July 2024", forRemoval = true)
@RestController
public class FeaturedWorkspacesController implements FeaturedWorkspacesConfigApiDelegate {
  private final Provider<FeaturedWorkspacesConfig> configProvider;

  @Autowired
  FeaturedWorkspacesController(Provider<FeaturedWorkspacesConfig> configProvider) {
    this.configProvider = configProvider;
  }

  /**
   * @deprecated Get all Featured workspaces, using the Featured Workspaces Config JSON as the
   *     source. Use this in conjunction with {@link WorkspaceService#getPublishedWorkspaces()} to
   *     construct the list of (pre-July-2024) Featured workspaces.
   *     <p>Use {@link WorkspaceService#getFeaturedWorkspaces()} to retrieve the Featured/Published
   *     workspaces from July 2024 onward.
   * @return List of all Featured workspaces
   */
  @Deprecated(since = "July 2024", forRemoval = true)
  @Override
  public ResponseEntity<FeaturedWorkspacesConfigResponse> getFeaturedWorkspacesConfig() {
    FeaturedWorkspacesConfig fwConfig = configProvider.get();
    return ResponseEntity.ok(
        new FeaturedWorkspacesConfigResponse().featuredWorkspacesList(fwConfig.featuredWorkspaces));
  }
}
