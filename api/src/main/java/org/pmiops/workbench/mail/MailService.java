package org.pmiops.workbench.mail;

import java.time.Instant;
import java.util.List;
import javax.mail.MessagingException;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exfiltration.EgressRemediationAction;
import org.pmiops.workbench.model.SendBillingSetupEmailRequest;

public interface MailService {
  void sendWelcomeEmail(final String contactEmail, final String password, final String username)
      throws MessagingException;

  void sendInstitutionUserInstructions(
      final String contactEmail, final String userInstructions, final String username)
      throws MessagingException;

  void alertUserFreeTierDollarThreshold(
      final DbUser user, double threshold, double currentUsage, double remainingBalance)
      throws MessagingException;

  void alertUserFreeTierExpiration(final DbUser user) throws MessagingException;

  void alertUserRegisteredTierWarningThreshold(
      final DbUser user, long daysRemaining, Instant expirationTime) throws MessagingException;

  void alertUserRegisteredTierExpiration(final DbUser user, Instant expirationTime)
      throws MessagingException;

  void sendBillingSetupEmail(final DbUser user, SendBillingSetupEmailRequest emailRequest)
      throws MessagingException;

  void sendEgressRemediationEmail(
      final DbUser user, EgressRemediationAction egressRemediationAction) throws MessagingException;

  void sendWorkspaceAdminLockingEmail(
      final DbWorkspace workspace, final String lockingReason, List<DbUser> owners)
      throws MessagingException;
}
