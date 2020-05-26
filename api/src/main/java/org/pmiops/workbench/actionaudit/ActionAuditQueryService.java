package org.pmiops.workbench.actionaudit;

import org.joda.time.DateTime;
import org.pmiops.workbench.model.AuditLogEntriesResponse;

public interface ActionAuditQueryService {
  AuditLogEntriesResponse queryEventsForWorkspace(
      long workspaceDatabaseId, long limit, DateTime afterInclusive, DateTime beforeExclusive);
}
