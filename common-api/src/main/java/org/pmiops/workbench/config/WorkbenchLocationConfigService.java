package org.pmiops.workbench.config;

import javax.inject.Provider;
import org.springframework.stereotype.Service;

@Service
public class WorkbenchLocationConfigService {
  private Provider<WorkbenchConfig> workbenchConfig;

  WorkbenchLocationConfigService(Provider<WorkbenchConfig> workbenchConfig) {
    this.workbenchConfig = workbenchConfig;
  }

  public String getWorknechAppEngineLocationId() {
    return workbenchConfig.get().server.appEngineLocationId;
  }

  public String getCloudTaskLocationId() {
    String appEngineLocationId = workbenchConfig.get().server.appEngineLocationId;
    if (appEngineLocationId == "us-central") return "us-central1";
    else if (appEngineLocationId == "europe-west") return "europe-west1";
    return appEngineLocationId;
  }
}
