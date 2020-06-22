package org.pmiops.workbench.useradmin;

import org.pmiops.workbench.model.UserAuditLogQueryResponse;

public interface UserAdminService {
  UserAuditLogQueryResponse queryEventsForUser(
      String usernameWithoutGsuiteDomain,
      Integer limit,
      Long afterMillis,
      Long beforeMillisNullable);
}
