package org.pmiops.workbench.workspaceadmin;

import java.util.Optional;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.AdminFederatedWorkspaceDetailsResponse;
import org.pmiops.workbench.model.AdminWorkspaceCloudStorageCounts;
import org.pmiops.workbench.model.AdminWorkspaceObjectsCounts;
import org.pmiops.workbench.model.CloudStorageTraffic;

public interface WorkspaceAdminService {
  Optional<DbWorkspace> getFirstWorkspaceByNamespace(String workspaceNamespace);

  AdminWorkspaceObjectsCounts getAdminWorkspaceObjects(long workspaceId);

  AdminWorkspaceCloudStorageCounts getAdminWorkspaceCloudStorageCounts(
      String workspaceNamespace, String workspaceName);

  CloudStorageTraffic getCloudStorageTraffic(String workspaceNamespace);

  AdminFederatedWorkspaceDetailsResponse getWorkspaceAdminView(String workspaceNamespace);
}
