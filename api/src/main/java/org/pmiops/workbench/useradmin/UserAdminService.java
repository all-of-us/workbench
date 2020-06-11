package org.pmiops.workbench.useradmin;

import java.util.List;
import org.pmiops.workbench.model.AccessBypassRequest;
import org.pmiops.workbench.model.Profile;
import org.pmiops.workbench.model.UserAuditLogQueryResponse;

public interface UserAdminService {
  UserAuditLogQueryResponse queryEventsForUser(
      String usernameWithoutGsuiteDomain,
      Integer limit,
      Long afterMillis,
      Long beforeMillisNullable);

  List<Profile> listAllProfiles();

  void updateBypassTime(long userDatabaseId, AccessBypassRequest accessBypassRequest);
}
