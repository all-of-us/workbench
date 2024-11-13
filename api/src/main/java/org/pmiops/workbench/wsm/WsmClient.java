package org.pmiops.workbench.wsm;

import jakarta.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.wsmanager.api.WorkspaceApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class WsmClient {

  private final Provider<WorkspaceApi> workspaceApi;
  private final Provider<WorkspaceApi> workspaceServiceApi;

  private final WsmRetryHandler wsmRetryHandler;

  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  public WsmClient(
      @Qualifier(WsmConfig.WSM_WORKSPACE_API) Provider<WorkspaceApi> workspaceApi,
      @Qualifier(WsmConfig.SERVICE_ACCOUNT_WORKSPACE_API)
          Provider<WorkspaceApi> workspaceServiceApi,
      WsmRetryHandler wsmRetryHandler,
      Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.workspaceApi = workspaceApi;
    this.workspaceServiceApi = workspaceServiceApi;
    this.wsmRetryHandler = wsmRetryHandler;
    this.workbenchConfigProvider = workbenchConfigProvider;
  }
}
