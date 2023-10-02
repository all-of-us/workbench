package org.pmiops.workbench.workspaces;

import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceDetails;

public interface AwsWorkspaceService extends WorkspaceService {

  RawlsWorkspaceDetails createWorkspace(Workspace workspace);
}
