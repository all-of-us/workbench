package org.pmiops.workbench.workspaceadmin;

import java.util.Optional;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.AdminWorkspaceResources;
import org.pmiops.workbench.model.AdminWorkspaceResourcesWorkspaceObjects;

public interface WorkspaceAdminService {
  Optional<DbWorkspace> getFirstWorkspaceByNamespace(String workspaceNamespace);

  AdminWorkspaceResourcesWorkspaceObjects getAdminWorkspaceObjects(long workspaceId);
}
