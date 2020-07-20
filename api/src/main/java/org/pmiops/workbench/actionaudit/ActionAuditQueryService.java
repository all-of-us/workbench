package org.pmiops.workbench.actionaudit;

import java.time.Instant;
import org.pmiops.workbench.model.UserAuditLogQueryResponse;
import org.pmiops.workbench.model.WorkspaceAuditLogQueryResponse;

public interface ActionAuditQueryService {
  WorkspaceAuditLogQueryResponse queryEventsForWorkspace(
      long workspaceDatabaseId, long limit, Instant after, Instant before);

  UserAuditLogQueryResponse queryEventsForUser(
      long userDatabaseId, long limit, Instant after, Instant before);
}
