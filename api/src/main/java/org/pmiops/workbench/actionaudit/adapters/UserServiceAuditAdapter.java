package org.pmiops.workbench.actionaudit.adapters;

import org.pmiops.workbench.model.DataAccessLevel;

public interface UserServiceAuditAdapter {
  void fireUpdateDataAccessAction(
      DataAccessLevel dataAccessLevel, DataAccessLevel previousDataAccessLevel);
}
