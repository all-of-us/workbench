package org.pmiops.workbench.mail;

import com.google.api.services.directory.model.User;
import java.time.Instant;
import javax.mail.MessagingException;
import org.pmiops.workbench.db.model.DbUser;

public interface MailService {

  void sendBetaAccessRequestEmail(final String userName) throws MessagingException;

  void sendWelcomeEmail(final String contactEmail, final String password, final User user)
      throws MessagingException;

  void sendInstitutionUserInstructions(final String contactEmail, final String userInstructions)
      throws MessagingException;

  void alertUserFreeTierDollarThreshold(
      final DbUser user, double threshold, double currentUsage, double remainingBalance)
      throws MessagingException;

  void alertUserFreeTierExpiration(final DbUser user) throws MessagingException;

  void alertUserRegisteredTierWarningThreshold(
      final DbUser user, long daysRemaining, Instant expirationTime) throws MessagingException;

  void alertUserRegisteredTierExpiration(final DbUser user, Instant expirationTime)
      throws MessagingException;
}
