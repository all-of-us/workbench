package org.pmiops.workbench.mail;

import jakarta.mail.MessagingException;
import java.time.Instant;
import java.util.List;
import javax.annotation.Nullable;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exfiltration.EgressRemediationAction;
import org.pmiops.workbench.leonardo.model.LeonardoListPersistentDiskResponse;
import org.pmiops.workbench.model.SendBillingSetupEmailRequest;

public interface MailService {

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

  void alertUserInitialCreditsDollarThreshold(
      final DbUser user, double threshold, double currentUsage, double remainingBalance)
      throws MessagingException;

  void alertUserInitialCreditsExpiration(final DbUser user) throws MessagingException;

  void alertUserAccessTierWarningThreshold(
      final DbUser user, long daysRemaining, Instant expirationTime, String tierShortName)
      throws MessagingException;

  void alertUserAccessTierExpiration(
      final DbUser user, Instant expirationTime, String tierShortName) throws MessagingException;

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
      boolean isDiskAttached,
      int daysUnused,
      @Nullable Double workspaceInitialCreditsRemaining)
      throws MessagingException;

  void sendBillingSetupEmail(final DbUser user, SendBillingSetupEmailRequest emailRequest)
      throws MessagingException;

  void sendEgressRemediationEmail(
      final DbUser user, EgressRemediationAction egressRemediationAction) throws MessagingException;

  void sendNewUserSatisfactionSurveyEmail(DbUser dbUser, String surveyLink)
      throws MessagingException;

  void sendWorkspaceAdminLockingEmail(
      final DbWorkspace workspace, final String lockingReason, List<DbUser> owners)
      throws MessagingException;

  void sendFileLengthsEgressRemediationEmail(DbUser dbUser, EgressRemediationAction action)
      throws MessagingException;
}
