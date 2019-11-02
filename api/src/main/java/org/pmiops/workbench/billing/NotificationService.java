package org.pmiops.workbench.billing;

import org.pmiops.workbench.db.model.DbUser;

public interface NotificationService {
  void alertUser(DbUser user, String msg);
}
