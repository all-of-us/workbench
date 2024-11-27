package org.pmiops.workbench.workspaces;

import jakarta.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.model.Workspace;
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

    public WorkspaceService getWorkspaceService(Workspace.LabEnum lab) {
        if (!workbenchConfigProvider.get().featureFlags.enableVWBWorkspaceCreation || lab == null) {
            return terraWorkspaceService;
        }

        switch (lab) {
            case AOU:
                return terraWorkspaceService;
            case VWB:
                return vwbWorkspaceService;
            default:
                throw new IllegalArgumentException("Invalid workspace lab: " + lab);
        }
    }

}
