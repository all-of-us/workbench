package org.pmiops.workbench.workspaceadmin;

import java.util.Optional;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.AdminWorkspaceObjectsCounts;

public interface WorkspaceAdminService {
  Optional<DbWorkspace> getFirstWorkspaceByNamespace(String workspaceNamespace);

  AdminWorkspaceObjectsCounts getAdminWorkspaceObjects(long workspaceId);
}
