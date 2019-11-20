package org.pmiops.workbench.actionaudit.adapters;

import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.model.DataAccessLevel;

public interface UserServiceAuditAdapter {
  void fireUpdateDataAccessAction(
      DbUser targetUser, DataAccessLevel dataAccessLevel, DataAccessLevel previousDataAccessLevel);
}
