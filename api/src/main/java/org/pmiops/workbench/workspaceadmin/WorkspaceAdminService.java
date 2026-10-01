package org.pmiops.workbench.workspaceadmin;

import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.AdminWorkspaceObjectsCounts;
import org.pmiops.workbench.model.CloudStorageTraffic;
import org.pmiops.workbench.model.WorkspaceAdminView;
import org.pmiops.workbench.model.WorkspaceAuditLogQueryResponse;
import org.pmiops.workbench.model.WorkspaceUserAdminView;
import org.pmiops.workbench.model.WorkspaceWaitingForRetrieval;

public interface WorkspaceAdminService {
  Optional<DbWorkspace> getFirstWorkspaceByNamespace(String workspaceNamespace);

  AdminWorkspaceObjectsCounts getAdminWorkspaceObjects(long workspaceId);

  CloudStorageTraffic getCloudStorageTraffic(String workspaceNamespace);

  WorkspaceAdminView getWorkspaceAdminView(String workspaceNamespace);

  WorkspaceAuditLogQueryResponse getWorkspaceAuditLogEntries(
      String workspaceNamespace,
      Integer limit,
      Long afterMillis,
      @Nullable Long beforeMillisNullable);

  List<WorkspaceWaitingForRetrieval> getWorkspacesWaitingForRetrieval();

  List<WorkspaceUserAdminView> getWorkspaceCollaborators(String namespace);
}
