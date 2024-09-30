package org.pmiops.workbench.mail;

import jakarta.annotation.Nullable;
import jakarta.mail.MessagingException;
import java.time.Instant;
import java.util.List;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exfiltration.EgressRemediationAction;
import org.pmiops.workbench.leonardo.model.LeonardoListPersistentDiskResponse;
import org.pmiops.workbench.model.SendBillingSetupEmailRequest;

public interface MailService {

  void sendWelcomeEmail(final DbUser user, final String password, final String institutionName)
      throws MessagingException;

  void sendInstitutionUserInstructions(
      final String contactEmail, final String userInstructions, final String username)
      throws MessagingException;

  void alertUserInitialCreditsDollarThreshold(
      final DbUser user, double threshold, double currentUsage, double remainingBalance)
      throws MessagingException;

  void alertUserInitialCreditsExhausted(final DbUser user) throws MessagingException;

  void alertUserInitialCreditsExpired(final DbUser user) throws MessagingException;

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
      final DbUser user, EgressRemediationAction egressRemediationAction, String environmentType)
      throws MessagingException;

  void sendNewUserSatisfactionSurveyEmail(DbUser dbUser, String surveyLink)
      throws MessagingException;

  void sendWorkspaceAdminLockingEmail(
      final DbWorkspace workspace, final String lockingReason, List<DbUser> owners)
      throws MessagingException;

  void sendFileLengthsEgressRemediationEmail(DbUser dbUser, EgressRemediationAction action)
      throws MessagingException;

  /**
   * Sends emails to workspace owners notifying them that a workspace has been published as a
   * Community Workspace. Note that it sends one email per owner, in order to address them in text
   * individually.
   *
   * @param workspace the workspace being published as a Community Workspace
   * @param owners the workspace's owners
   * @throws MessagingException
   */
  void sendPublishCommunityWorkspaceEmails(DbWorkspace workspace, List<DbUser> owners)
      throws MessagingException;

  /**
   * Sends emails to workspace owners notifying them that a workspace has been unpublished. Note
   * that it sends one email per owner, in order to address them in text individually.
   *
   * @param workspace the workspace being unpublished
   * @param owners the workspace's owners
   * @throws MessagingException
   */
  void sendAdminUnpublishWorkspaceEmails(DbWorkspace workspace, List<DbUser> owners)
      throws MessagingException;
}
