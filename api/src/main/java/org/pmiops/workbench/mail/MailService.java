package org.pmiops.workbench.mail;

import com.google.api.services.directory.model.User;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import javax.mail.MessagingException;
import org.pmiops.workbench.db.model.DbUser;

public interface MailService {

  void sendBetaAccessRequestEmail(final String userName) throws MessagingException;

  void sendWelcomeEmail(final String contactEmail, final String password, final User user)
      throws MessagingException;

  void sendBetaAccessCompleteEmail(final String contactEmail, final String username)
      throws MessagingException;

  void alertUserFreeTierDollarThreshold(
      final DbUser user,
      double threshold,
      double currentUsage,
      double remainingBalance,
      final Optional<Instant> expirationTimeIfKnown)
      throws MessagingException;

  void alertUserFreeTierTimeThreshold(
      final DbUser user,
      long daysRemaining,
      final LocalDate expirationDate,
      double remainingDollarBalance)
      throws MessagingException;

  void alertUserFreeTierExpiration(final DbUser user) throws MessagingException;
}
