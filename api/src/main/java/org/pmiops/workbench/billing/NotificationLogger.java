package org.pmiops.workbench.billing;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.pmiops.workbench.db.model.DbUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

// RW-3661: replace this file with an implementation which sends emails instead

@Service
public class NotificationLogger implements NotificationService {

  private static final Logger logger = LoggerFactory.getLogger(BillingProjectBufferService.class);

  @Override
  public void alertUserFreeTierDollarThreshold(
      final DbUser user, double threshold, double currentUsage, double remainingBalance) {
    final String logMsg =
        String.format(
            "User %s has passed the %.2f free tier dollar threshold.  Current total usage is $%.2f with remaining balance $%.2f",
            user.getUsername(), threshold, currentUsage, remainingBalance);
    logger.info(logMsg);
  }

  @Override
  public void alertUserFreeTierTimeThreshold(
      final DbUser user,
      long daysRemaining,
      final LocalDate expirationDate,
      double remainingDollarBalance) {

    // TODO choose desired date format
    final String dateStr =
        DateTimeFormatter.ofPattern("MM/dd/yyyy")
            .withZone(ZoneId.systemDefault())
            .format(expirationDate);

    final String logMsg =
        String.format(
            "User %s has %d days remaining until their expiration date of %s.  Current total usage is $%.2f",
            user.getUsername(), daysRemaining, dateStr, remainingDollarBalance);
    logger.info(logMsg);
  }

  @Override
  public void alertUserFreeTierExpiration(final DbUser user) {
    final String logMsg =
        String.format("Free credits have expired for User %s", user.getUsername());
    logger.info(logMsg);
  }
}
