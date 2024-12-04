package org.pmiops.workbench.workspaces;

import jakarta.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class WorkspaceServiceFactory {

  private final WorkspaceService terraWorkspaceService;
  private final WorkspaceService vwbWorkspaceService;

  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  public WorkspaceServiceFactory(
      WorkspaceService terraWorkspaceService,
      @Qualifier(VwbWorkspaceServiceImpl.VWB_WORKSPACE_SERVICE)
          WorkspaceService vwbWorkspaceService,
      Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.terraWorkspaceService = terraWorkspaceService;
    this.vwbWorkspaceService = vwbWorkspaceService;
    this.workbenchConfigProvider = workbenchConfigProvider;
  }

  public WorkspaceService getWorkspaceService(Boolean isVwbWorkspace) {
    if (!workbenchConfigProvider.get().featureFlags.enableVWBWorkspaceCreation
        || isVwbWorkspace == null) {
      return terraWorkspaceService;
    }
    return isVwbWorkspace ? vwbWorkspaceService : terraWorkspaceService;
  }
}
