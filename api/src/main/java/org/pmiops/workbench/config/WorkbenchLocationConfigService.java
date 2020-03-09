package org.pmiops.workbench.config;

import javax.inject.Provider;
import org.springframework.stereotype.Service;

@Service
public class WorkbenchLocationConfigService {
  private Provider<WorkbenchConfig> workbenchConfig;

  WorkbenchLocationConfigService(Provider<WorkbenchConfig> workbenchConfig) {
    this.workbenchConfig = workbenchConfig;
  }

  public String getWorkbenchAppEngineLocationId() {
    return workbenchConfig.get().server.appEngineLocationId;
  }

  /**
   * In case of location Id for cloud task queue, replace us-central and europe-west to us-central1
   * and europe-west1 Refer to https://cloud.google.com/tasks/docs/dual-overview
   *
   * @return Location Id of the appEngine that hosts cloud task queue
   */
  public String getCloudTaskLocationId() {
    String appEngineLocationId = workbenchConfig.get().server.appEngineLocationId;
    if (appEngineLocationId.equals("us-central")) return "us-central1";
    else if (appEngineLocationId.equals("europe-west")) return "europe-west1";
    return appEngineLocationId;
  }
}
