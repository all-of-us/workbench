package org.pmiops.workbench.billing;

import java.time.LocalDate;
import org.pmiops.workbench.db.model.DbUser;

public interface NotificationService {
  void alertUserFreeTierDollarThreshold(
      final DbUser user, double threshold, double currentUsage, double remainingBalance);

  void alertUserFreeTierTimeThreshold(
      final DbUser user,
      long daysRemaining,
      final LocalDate expirationDate,
      double remainingDollarBalance);

  void alertUserFreeTierExpiration(final DbUser user);
}
