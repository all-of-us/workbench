package org.pmiops.workbench.billing;

import java.time.LocalDate;
import org.pmiops.workbench.db.model.DbUser;

public interface NotificationService {
  void alertUserFreeTierDollarThreshold(
      DbUser user, double threshold, double currentUsage, double remainingBalance);

  void alertUserFreeTierTimeThreshold(
      DbUser user, long daysRemaining, LocalDate expirationDate, double remainingDollarBalance);

  void alertUserFreeTierExpiration(DbUser user);
}
