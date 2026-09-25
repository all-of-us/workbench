package org.pmiops.workbench.actionaudit;

import java.time.Instant;
import java.util.List;
import org.pmiops.workbench.db.jdbc.ReportingQueryService.WorkspaceIdWithRole;
import org.pmiops.workbench.model.UserAuditLogQueryResponse;
import org.pmiops.workbench.model.WorkspaceAuditLogQueryResponse;

public interface ActionAuditQueryService {
  WorkspaceAuditLogQueryResponse queryEventsForWorkspace(
      long workspaceDatabaseId, long limit, Instant after, Instant before);

  UserAuditLogQueryResponse queryEventsForUser(
      long userDatabaseId, long limit, Instant after, Instant before);

  List<WorkspaceIdWithRoleImpl> getWorkspaceIdsAndRolesByCollaboratorId(long userId);

  List<ActionAuditQueryService.UserIdWithRoleImpl> getWorkspaceUsersById(long workspaceId);

  interface WorkspaceIdWithRole {
    Long workspaceId();

    String role();
  }

  record WorkspaceIdWithRoleImpl(Long workspaceId, String role) implements WorkspaceIdWithRole {}

  interface UserIdWithRole {
    Long userId();

    String role();
  }

  record UserIdWithRoleImpl(Long userId, String role) implements UserIdWithRole {}
}
