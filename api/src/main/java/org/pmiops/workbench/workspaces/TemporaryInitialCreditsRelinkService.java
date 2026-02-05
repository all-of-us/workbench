package org.pmiops.workbench.workspaces;

import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.Workspace;

public interface TemporaryInitialCreditsRelinkService {
  void initiateTemporaryRelinking(DbWorkspace fromWorkspace, Workspace toWorkspace);

  void cleanupTemporarilyRelinkedWorkspaces();
}
