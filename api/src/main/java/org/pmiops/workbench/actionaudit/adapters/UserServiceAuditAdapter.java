package org.pmiops.workbench.actionaudit.adapters;

import java.time.Instant;
import org.pmiops.workbench.actionaudit.targetproperties.BypassTimeTargetProperty;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.model.DataAccessLevel;

public interface UserServiceAuditAdapter {
  void fireUpdateDataAccessAction(
      DbUser targetUser,
      DataAccessLevel dataAccessLevel,
      DataAccessLevel previousDataAccessLevel);

  void fireAdministrativeBypassTime(
      long userId,
      BypassTimeTargetProperty bypassTimeTargetProperty,
      Instant bypassTime);
}
