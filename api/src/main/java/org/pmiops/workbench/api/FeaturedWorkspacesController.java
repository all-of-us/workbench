package org.pmiops.workbench.api;

import javax.inject.Provider;
import org.pmiops.workbench.config.FeaturedWorkspacesConfig;
import org.pmiops.workbench.model.FeaturedWorkspacesConfigResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FeaturedWorkspacesController implements FeaturedWorkspacesConfigApiDelegate {
  private final Provider<FeaturedWorkspacesConfig> configProvider;

  @Autowired
  FeaturedWorkspacesController(Provider<FeaturedWorkspacesConfig> configProvider) {
    this.configProvider = configProvider;
  }

  @Override
  public ResponseEntity<FeaturedWorkspacesConfigResponse> getFeaturedWorkspacesConfig() {
    FeaturedWorkspacesConfig fwConfig = configProvider.get();
    return ResponseEntity.ok(
        new FeaturedWorkspacesConfigResponse().featuredWorkspacesList(fwConfig.featuredWorkspaces));
  }
}
