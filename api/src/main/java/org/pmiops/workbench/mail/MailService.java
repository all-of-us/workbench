package org.pmiops.workbench.mail;

import java.time.Instant;
import java.util.List;
import javax.annotation.Nullable;
import javax.mail.MessagingException;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exfiltration.EgressRemediationAction;
import org.pmiops.workbench.leonardo.model.LeonardoListPersistentDiskResponse;
import org.pmiops.workbench.model.SendBillingSetupEmailRequest;

public interface MailService {
  @Deprecated
  void sendWelcomeEmail_deprecated(
      final String contactEmail, final String password, final String username)
      throws MessagingException;

  void sendWelcomeEmail(
      final String contactEmail,
      final String password,
      final String username,
      final String institutionName,
      final Boolean showEraStepInRt,
      final Boolean showEraStepInCt)
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

  /**
   * Notifies the specified users via BCC that there is an unused persistent disk. CC could also be
   * considered here, but may require a policy discussion, as we'd be introducing the sharing of
   * contact emails across users with this change. the workspace is on initial credits,
   * workspaceInitialCreditsRemaining should be provided.
   */
  void alertUsersUnusedDiskWarningThreshold(
      List<DbUser> users,
      DbWorkspace diskWorkspace,
      LeonardoListPersistentDiskResponse disk,
      int daysUnused,
      @Nullable Double workspaceInitialCreditsRemaining)
      throws MessagingException;

  void sendBillingSetupEmail(final DbUser user, SendBillingSetupEmailRequest emailRequest)
      throws MessagingException;

  void sendEgressRemediationEmail(
      final DbUser user, EgressRemediationAction egressRemediationAction) throws MessagingException;

  void sendWorkspaceAdminLockingEmail(
      final DbWorkspace workspace, final String lockingReason, List<DbUser> owners)
      throws MessagingException;
}
