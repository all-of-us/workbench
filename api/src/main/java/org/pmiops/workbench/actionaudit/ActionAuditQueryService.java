package org.pmiops.workbench.actionaudit;

import org.joda.time.DateTime;
import org.pmiops.workbench.model.UserAuditLogQueryResponse;
import org.pmiops.workbench.model.WorkspaceAuditLogQueryResponse;

public interface ActionAuditQueryService {
  WorkspaceAuditLogQueryResponse queryEventsForWorkspace(
      long workspaceDatabaseId, long limit, DateTime after, DateTime before);

  UserAuditLogQueryResponse queryEventsForUser(
      long userDatabaseId, long limit, DateTime after, DateTime before);
}
