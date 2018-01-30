package org.pmiops.workbench.api;

import java.util.logging.Logger;
import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.model.ConfigResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConfigController implements ConfigApiDelegate {

  private static final Logger log = Logger.getLogger(ConfigController.class.getName());
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
        .stackdriverApiKey(configProvider.get().server.stackdriverApiKey));
  }
}
