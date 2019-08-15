package org.pmiops.workbench.billing;

import org.pmiops.workbench.db.model.User;

public interface NotificationService {
  void alertUser(User user, String msg);
}
