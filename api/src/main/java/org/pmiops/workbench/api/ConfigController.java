package org.pmiops.workbench.api;

import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.model.ConfigResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConfigController implements ConfigApiDelegate {

  private final Provider<WorkbenchConfig> configProvider;

  @Autowired
  ConfigController(Provider<WorkbenchConfig> configProvider) {
    this.configProvider = configProvider;
  }

  @Override
  public ResponseEntity<ConfigResponse> getConfig() {
    return ResponseEntity.ok(
      new ConfigResponse()
        .gsuiteDomain(configProvider.get().googleDirectoryService.gSuiteDomain)
        .projectId(configProvider.get().server.projectId)
        .publicApiKeyForErrorReports(configProvider.get().server.publicApiKeyForErrorReports));
  }
}
