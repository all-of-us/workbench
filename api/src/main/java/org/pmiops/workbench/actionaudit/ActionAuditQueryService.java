package org.pmiops.workbench.actionaudit;

import org.pmiops.workbench.model.AuditLogEntriesResponse;

public interface ActionAuditQueryService {
  AuditLogEntriesResponse queryEventsForWorkspace(long workspaceDatabaseId, long limit);
}
