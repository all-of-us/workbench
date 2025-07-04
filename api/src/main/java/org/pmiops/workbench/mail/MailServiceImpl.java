package org.pmiops.workbench.mail;

import static org.pmiops.workbench.access.AccessTierService.CONTROLLED_TIER_SHORT_NAME;
import static org.pmiops.workbench.access.AccessTierService.REGISTERED_TIER_SHORT_NAME;
import static org.pmiops.workbench.leonardo.LeonardoAppUtils.appServiceNameToAppType;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.html.HtmlEscapers;
import com.google.common.io.Resources;
import jakarta.annotation.Nullable;
import jakarta.inject.Provider;
import jakarta.mail.MessagingException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.config.WorkbenchConfig.EgressAlertRemediationPolicy;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exfiltration.EgressRemediationAction;
import org.pmiops.workbench.google.CloudStorageClient;
import org.pmiops.workbench.leonardo.LeonardoAppUtils;
import org.pmiops.workbench.leonardo.PersistentDiskUtils;
import org.pmiops.workbench.model.Disk;
import org.pmiops.workbench.model.SendBillingSetupEmailRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MailServiceImpl implements MailService {
  private static final String AOU_ITALICS = "<i>All of Us</i>";
  private static final String EGRESS_FORM_LINK =
      "<a href=\"https://redcap.pmi-ops.org/surveys/?s=K4PA9H8E979AHJEP\">Researcher Account Activity Confirmation</a>";
  private static final String DOWNLOAD_FORM_LINK =
      "<a href=\"https://redcap.pmi-ops.org/surveys/?s=YRXMJFJ97J3WMWLE\">Large Data Download Request</a>";

  public static final Map<EgressRemediationAction, String> EGRESS_REMEDIATION_ACTION_MAP =
      Map.of(
          EgressRemediationAction.SUSPEND_COMPUTE,
          String.format(
              """
              <div>
                Your access to analyze the All of Us dataset in the Researcher Workbench has been
                temporarily suspended and will automatically restore after a brief duration.
              </div>
              <div><b>
                To confirm your activity, please complete the %s form. Your Researcher Workbench account
                may be suspended if the form is not submitted within 24 hours.
              </b></div>
              <div>
                If you are working with large sized data files due to the nature of your research
                (i.e.: genomics) or large sized notebooks, you may request a temporary increase in
                your egress threshold limit by filling out and submitting the %s form.
              </div>
              """,
              EGRESS_FORM_LINK, DOWNLOAD_FORM_LINK),
          EgressRemediationAction.DISABLE_USER,
          String.format(
              """
              <div><b>
                Your Researcher Workbench account has been suspended and will remain disabled until
                you complete the %s form and following manual review by the %s Researcher Workbench
                security team.
              </b></div>
              """,
              EGRESS_FORM_LINK, AOU_ITALICS));

  private static final Logger log = Logger.getLogger(MailServiceImpl.class.getName());
  private static final String EGRESS_SOURCE = " when using the <b>%s</b> application";
  private static final String EGRESS_REMEDIATION_RESOURCE =
      "emails/egress_remediation/content.html";
  private static final String INITIAL_CREDITS_DOLLAR_THRESHOLD_RESOURCE =
      "emails/initial_credits_dollar_threshold/content.html";
  private static final String INITIAL_CREDITS_EXHAUSTION_RESOURCE =
      "emails/initial_credits_exhaustion/content.html";
  private static final String INITIAL_CREDITS_EXPIRING_RESOURCE =
      "emails/initial_credits_expiring/content.html";
  private static final String INSTRUCTIONS_RESOURCE = "emails/instructions/content.html";
  private static final String NEW_USER_SATISFACTION_SURVEY_RESOURCE =
      "emails/new_user_satisfaction_survey/content.html";
  private static final String TIER_ACCESS_EXPIRED_RESOURCE =
      "emails/tier_access_expired/content.html";
  private static final String TIER_ACCESS_THRESHOLD_RESOURCE =
      "emails/tier_access_threshold/content.html";
  private static final String SETUP_BILLING_ACCOUNT_RESOURCE =
      "emails/setup_gcp_billing_account/content.html";
  private static final String UNUSED_DISK_RESOURCE = "emails/unused_disk/content.html";
  private static final String WELCOME_RESOURCE = "emails/welcome/content.html";
  private static final String WORKSPACE_ADMIN_LOCKING_RESOURCE =
      "emails/workspace_admin_locking/content.html";

  private static final String PUBLISH_COMMUNITY_WORKSPACE_RESOURCE =
      "emails/publish_community_workspace/content.html";

  private static final String UNPUBLISH_WORKSPACE_RESOURCE =
      "emails/unpublish_workspace/content.html";

  private static final String RAB_SUPPORT_EMAIL = "aouresourceaccess@od.nih.gov";

  private static final String UNUSED_DISK_DELETE_HELP =
      "https://support.researchallofus.org/hc/en-us/articles/5140493753620#h_01H0NEWRR4DRJ8JJAE7HW5NRR3";

  private static final String OPEN_LI_TAG = "<li>";
  private static final String CLOSE_LI_TAG = "</li>";

  @VisibleForTesting static final String ATTACHED_DISK_STATUS = "attached to";

  @VisibleForTesting static final String DETACHED_DISK_STATUS = "detached from";

  // RT Steps
  private static final String TWO_STEP_VERIFICATION = "Turn on Google 2-Step Verification";
  private static final String CT_INSTITUTION_CHECK = "Check that %s allows Controlled Tier access";
  private static final String RT_TRAINING =
      "Complete All of Us Responsible Conduct of Research Training";
  private static final String LOGIN_GOV = "Verify your identity with Login.gov";

  // CT Steps
  private static final String CT_TRAINING = "Complete All of Us Controlled Tier Training";

  private final Provider<MailSender> mailSenderProvider;
  private final Provider<CloudStorageClient> cloudStorageClientProvider;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  @Autowired
  public MailServiceImpl(
      Provider<MailSender> mailSenderProvider,
      Provider<CloudStorageClient> cloudStorageClientProvider,
      Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.mailSenderProvider = mailSenderProvider;
    this.cloudStorageClientProvider = cloudStorageClientProvider;
    this.workbenchConfigProvider = workbenchConfigProvider;
  }

  @Override
  public void sendWelcomeEmail(
      final DbUser user, final String password, final String institutionName)
      throws MessagingException {
    final String htmlMessage =
        buildHtml(
            WELCOME_RESOURCE,
            welcomeMessageSubstitutionMap(password, user.getUsername(), institutionName));

    sendWithRetries(
        Collections.singletonList(user.getContactEmail()),
        Collections.emptyList(),
        "Your new All of Us Researcher Workbench Account",
        String.format("Welcome for %s", userForLogging(user)),
        htmlMessage);
  }

  @Override
  public void sendInstitutionUserInstructions(
      String contactEmail, String userInstructions, final String username)
      throws MessagingException {

    // TODO(RW-6482): Use a templating system rather than manual oneoff escaping.
    // These institutional instructions are stored unescaped. Though they are input by admins,
    // the strings should not be trusted as HTML.
    String escapedUserInstructions = HtmlEscapers.htmlEscaper().escape(userInstructions);
    final String htmlMessage =
        buildHtml(
            INSTRUCTIONS_RESOURCE, instructionsSubstitutionMap(escapedUserInstructions, username));

    sendWithRetries(
        Collections.singletonList(contactEmail),
        Collections.emptyList(),
        "Instructions from your institution on using the Researcher Workbench",
        String.format(
            "Institution user instructions for %s", userForLogging(username, contactEmail)),
        htmlMessage);
  }

  @Override
  public void alertUserInitialCreditsDollarThreshold(
      final DbUser user, double threshold, double currentUsage, double remainingBalance)
      throws MessagingException {

    final String logMsg =
        String.format(
            "User %s has passed the %.2f initial credits dollar threshold.  Current total usage is $%.2f with remaining balance $%.2f",
            userForLogging(user), threshold, currentUsage, remainingBalance);
    log.info(logMsg);

    final String htmlMessage =
        buildHtml(
            INITIAL_CREDITS_DOLLAR_THRESHOLD_RESOURCE,
            initialCreditsDollarThresholdSubstitutionMap(user, currentUsage, remainingBalance));

    sendWithRetries(
        Collections.singletonList(user.getContactEmail()),
        Collections.emptyList(),
        String.format(
            "Reminder - %s Initial credit usage in All of Us Researcher Workbench",
            formatPercentage(threshold)),
        String.format("User %s passed an initial credits dollar threshold", userForLogging(user)),
        htmlMessage);
  }

  @Override
  public void alertUserInitialCreditsExhausted(final DbUser user) throws MessagingException {
    final String logMsg =
        String.format(
            "Sending email because initial credits have been exhausted for User %s",
            userForLogging(user));
    log.info(logMsg);

    final String htmlMessage =
        buildHtml(
            INITIAL_CREDITS_EXHAUSTION_RESOURCE, initialCreditsExhaustionSubstitutionMap(user));

    sendWithRetries(
        Collections.singletonList(user.getContactEmail()),
        Collections.emptyList(),
        "Alert - Initial credit exhaustion in All of Us Researcher Workbench",
        logMsg,
        htmlMessage);
  }

  @Override
  public void alertUserInitialCreditsExpiring(DbUser user) throws MessagingException {
    final String logMsg =
        String.format(
            "Sending email because initial credits are expiring soon for User %s",
            userForLogging(user));
    log.info(logMsg);

    final String htmlMessage =
        buildHtml(INITIAL_CREDITS_EXPIRING_RESOURCE, initialCreditsExpiringSubstitutionMap(user));

    sendWithRetries(
        Collections.singletonList(user.getContactEmail()),
        Collections.emptyList(),
        "Alert - Initial credit expiration in All of Us Researcher Workbench",
        logMsg,
        htmlMessage);
  }

  @Override
  public void alertUserAccessTierWarningThreshold(
      final DbUser user, long daysRemaining, Instant expirationTime, String tierShortName)
      throws MessagingException {

    String capitalizedAccessTierShortName = StringUtils.capitalize(tierShortName);

    final String logMsg =
        String.format(
            "%s Tier access expiration will occur for user %s in %d days (on %s).",
            capitalizedAccessTierShortName,
            userForLogging(user),
            daysRemaining,
            formatCentralTime(expirationTime));
    log.info(logMsg);

    final String htmlMessage =
        buildHtml(
            TIER_ACCESS_THRESHOLD_RESOURCE,
            accessTierSubstitutionMap(expirationTime, user, tierShortName));

    sendWithRetries(
        Collections.singletonList(user.getContactEmail()),
        Collections.emptyList(),
        "Your access to All of Us "
            + capitalizedAccessTierShortName
            + " Tier Data will expire "
            + (daysRemaining == 1 ? "tomorrow" : String.format("in %d days", daysRemaining)),
        String.format(
            "User %s will lose %s tier access in %d days",
            userForLogging(user), tierShortName, daysRemaining),
        htmlMessage);
  }

  @Override
  public void alertUserAccessTierExpiration(
      final DbUser user, Instant expirationTime, String tierShortName) throws MessagingException {
    String capitalizedAccessTierShortName = StringUtils.capitalize(tierShortName);

    final String logMsg =
        String.format(
            "%s Tier access expired for user %s on %s.",
            capitalizedAccessTierShortName,
            userForLogging(user),
            formatCentralTime(expirationTime));
    log.info(logMsg);

    final String htmlMessage =
        buildHtml(
            TIER_ACCESS_EXPIRED_RESOURCE,
            accessTierSubstitutionMap(expirationTime, user, tierShortName));

    sendWithRetries(
        Collections.singletonList(user.getContactEmail()),
        Collections.emptyList(),
        String.format(
            "Your access to All of Us %s Tier Data has expired", capitalizedAccessTierShortName),
        logMsg,
        htmlMessage);
  }

  @Override
  public void alertUsersUnusedDiskWarningThreshold(
      List<DbUser> users,
      DbWorkspace diskWorkspace,
      Disk disk,
      boolean isDiskAttached,
      int daysUnused,
      @Nullable Double workspaceInitialCreditsRemaining)
      throws MessagingException {
    final String htmlMessage =
        buildHtml(
            UNUSED_DISK_RESOURCE,
            ImmutableMap.<EmailSubstitutionField, String>builder()
                .put(EmailSubstitutionField.HEADER_IMG, getAllOfUsLogo())
                .put(
                    EmailSubstitutionField.DISK_SIZE,
                    NumberFormat.getNumberInstance(Locale.US).format(disk.getSize()) + " GB")
                .put(EmailSubstitutionField.WORKSPACE_NAME, diskWorkspace.getName())
                .put(EmailSubstitutionField.DISK_UNUSED_DAYS, Integer.toString(daysUnused))
                .put(
                    EmailSubstitutionField.DISK_COST_PER_MONTH,
                    String.format("$%.2f", PersistentDiskUtils.costPerMonth(disk)))
                .put(
                    EmailSubstitutionField.DISK_CREATION_DATE,
                    formatDateCentralTime(Instant.parse(disk.getCreatedDate())))
                .put(EmailSubstitutionField.DISK_CREATOR_USERNAME, disk.getCreator())
                .put(
                    EmailSubstitutionField.DISK_STATUS,
                    isDiskAttached ? ATTACHED_DISK_STATUS : DETACHED_DISK_STATUS)
                .put(
                    EmailSubstitutionField.ENVIRONMENT_TYPE,
                    disk.isGceRuntime()
                        ? "Jupyter"
                        : LeonardoAppUtils.appDisplayName(disk.getAppType()))
                .put(
                    EmailSubstitutionField.BILLING_ACCOUNT_DETAILS,
                    buildBillingAccountDescription(diskWorkspace, workspaceInitialCreditsRemaining))
                .put(EmailSubstitutionField.WORKSPACE_URL, buildWorkspaceUrl(diskWorkspace))
                .put(EmailSubstitutionField.DISK_DELETE_INSTRUCTION, UNUSED_DISK_DELETE_HELP)
                .build());
    sendWithRetries(
        workbenchConfigProvider.get().mail.fromEmail,
        Collections.emptyList(),
        Collections.emptyList(),
        users.stream().map(DbUser::getContactEmail).toList(),
        "Reminder - Unused Disk in your Workspace",
        "Unused disk notification",
        htmlMessage);
  }

  @Override
  public void sendBillingSetupEmail(DbUser dbUser, SendBillingSetupEmailRequest emailRequest)
      throws MessagingException {
    final WorkbenchConfig workbenchConfig = workbenchConfigProvider.get();

    final String htmlMessage =
        buildHtml(
            SETUP_BILLING_ACCOUNT_RESOURCE, setupBillingAccountEmailMap(dbUser, emailRequest));

    List<String> receiptEmails = new ArrayList<>();
    receiptEmails.add(dbUser.getContactEmail());
    if (!Strings.isNullOrEmpty(workbenchConfig.billing.carahsoftEmail)) {
      receiptEmails.add(workbenchConfig.billing.carahsoftEmail);
    }
    sendWithRetries(
        receiptEmails,
        Collections.singletonList(workbenchConfigProvider.get().mail.fromEmail),
        "Request to set up Google Cloud Billing Account for All of Us Workbench",
        String.format(" User %s requests billing setup from Carasoft.", userForLogging(dbUser)),
        htmlMessage);
  }

  @Override
  public void sendEgressRemediationEmail(
      DbUser dbUser, EgressRemediationAction action, @Nullable String gkeServiceName)
      throws MessagingException {
    sendEgressRemediationEmailCommon(dbUser, action, gkeServiceName, false);
  }

  @Override
  public void sendEgressRemediationEmailForVwb(DbUser dbUser, EgressRemediationAction action)
      throws MessagingException {
    sendEgressRemediationEmailCommon(dbUser, action, null, true);
  }

  private void sendEgressRemediationEmailCommon(
      DbUser dbUser,
      EgressRemediationAction action,
      @Nullable String gkeServiceName,
      boolean isVwbEgress)
      throws MessagingException {
    String remediation = EGRESS_REMEDIATION_ACTION_MAP.get(action);
    String givenName = Optional.ofNullable(dbUser.getGivenName()).orElse("Researcher");

    String egressSource = "";

    if (!isVwbEgress) {
      String environmentType =
          appServiceNameToAppType(Strings.nullToEmpty(gkeServiceName))
              .map(LeonardoAppUtils::appDisplayName)
              .orElse("Jupyter");
      egressSource = String.format(EGRESS_SOURCE, environmentType);
    }

    var substitutionMap =
        Map.of(
            EmailSubstitutionField.FIRST_NAME, givenName,
            EmailSubstitutionField.HEADER_IMG, getAllOfUsLogo(),
            EmailSubstitutionField.ALL_OF_US, AOU_ITALICS,
            EmailSubstitutionField.USERNAME, dbUser.getUsername(),
            EmailSubstitutionField.EGRESS_REMEDIATION_DESCRIPTION, remediation,
            EmailSubstitutionField.EGRESS_SOURCE, egressSource);

    String htmlMessage = buildHtml(EGRESS_REMEDIATION_RESOURCE, substitutionMap);

    EgressAlertRemediationPolicy egressPolicy =
        workbenchConfigProvider.get().egressAlertRemediationPolicy;
    sendWithRetries(
        egressPolicy.notifyFromEmail,
        List.of(dbUser.getContactEmail()),
        Optional.ofNullable(egressPolicy.notifyCcEmails).orElse(Collections.emptyList()),
        Collections.emptyList(),
        "[Response Required] AoU Researcher Workbench High Data Egress Alert",
        String.format("Egress remediation email for %s", userForLogging(dbUser)),
        htmlMessage);
  }

  @Override
  public void sendPublishCommunityWorkspaceEmails(DbWorkspace workspace, List<DbUser> owners)
      throws MessagingException {
    sendPublishUnpublishWorkspaceEmails(
        workspace, owners, PUBLISH_COMMUNITY_WORKSPACE_RESOURCE, "publish");
  }

  @Override
  public void sendAdminUnpublishWorkspaceEmails(DbWorkspace workspace, List<DbUser> owners)
      throws MessagingException {
    sendPublishUnpublishWorkspaceEmails(
        workspace, owners, UNPUBLISH_WORKSPACE_RESOURCE, "unpublish");
  }

  private void sendPublishUnpublishWorkspaceEmails(
      DbWorkspace workspace, List<DbUser> owners, String emailResource, String actionPresentTense)
      throws MessagingException {
    final String supportEmail = workbenchConfigProvider.get().mail.fromEmail;
    final String presentTenseCapitalized = StringUtils.capitalize(actionPresentTense);
    final String pastTense = actionPresentTense.toLowerCase() + "ed";

    for (DbUser owner : owners) {
      sendWithRetries(
          Collections.singletonList(owner.getContactEmail()),
          Collections.singletonList(supportEmail),
          String.format("Your AoU Researcher Workbench workspace has been %s", pastTense),
          String.format(
              "%s workspace email for workspace '%s' (%s) sent to owner %s",
              presentTenseCapitalized,
              workspace.getName(),
              workspace.getWorkspaceNamespace(),
              owner.getContactEmail()),
          buildHtml(
              emailResource,
              publishUnpublishWorkspaceSubstitutionMap(workspace, owner, supportEmail)));
    }
  }

  @Override
  public void sendNewUserSatisfactionSurveyEmail(DbUser dbUser, String surveyLink)
      throws MessagingException {
    String htmlMessage =
        buildHtml(
            NEW_USER_SATISFACTION_SURVEY_RESOURCE,
            ImmutableMap.<EmailSubstitutionField, String>builder()
                .put(EmailSubstitutionField.HEADER_IMG, getAllOfUsLogo())
                .put(EmailSubstitutionField.ALL_OF_US, AOU_ITALICS)
                .put(EmailSubstitutionField.SURVEY_LINK, surveyLink)
                .build());

    sendWithRetries(
        List.of(dbUser.getContactEmail()),
        Collections.emptyList(),
        "Researcher satisfaction survey for the All of Us Researcher Workbench",
        String.format("New user satisfaction survey email for %s", userForLogging(dbUser)),
        htmlMessage);
  }

  @Override
  public void sendWorkspaceAdminLockingEmail(
      DbWorkspace workspace, String lockingReason, List<DbUser> owners) throws MessagingException {

    WorkbenchConfig config = workbenchConfigProvider.get();
    List<String> ccSupportMaybe =
        config.featureFlags.ccSupportWhenAdminLocking
            ? List.of(config.mail.fromEmail)
            : Collections.emptyList();

    sendWithRetries(
        ownersEmailList(owners),
        ccSupportMaybe,
        "[Response Required] AoU Researcher Workbench Workspace Admin Locked",
        String.format(
            "Admin locking email for workspace '%s' (%s) sent to owners %s",
            workspace.getName(), workspace.getWorkspaceNamespace(), ownersForLogging(owners)),
        buildHtml(
            WORKSPACE_ADMIN_LOCKING_RESOURCE,
            workspaceAdminLockedSubstitutionMap(workspace, lockingReason)));
  }

  private Map<EmailSubstitutionField, String> welcomeMessageSubstitutionMap(
      final String password, final String username, final String institutionName) {
    final CloudStorageClient cloudStorageClient = cloudStorageClientProvider.get();
    return new ImmutableMap.Builder<EmailSubstitutionField, String>()
        .put(EmailSubstitutionField.USERNAME, username)
        .put(EmailSubstitutionField.PASSWORD, password)
        .put(EmailSubstitutionField.URL, workbenchConfigProvider.get().admin.loginUrl)
        .put(EmailSubstitutionField.HEADER_IMG, getAllOfUsLogo())
        .put(EmailSubstitutionField.ALL_OF_US, AOU_ITALICS)
        .put(EmailSubstitutionField.REGISTRATION_IMG, getRegistrationImage())
        .put(EmailSubstitutionField.BULLET_1, cloudStorageClient.getImageUrl("bullet_1.png"))
        .put(EmailSubstitutionField.BULLET_2, cloudStorageClient.getImageUrl("bullet_2.png"))
        .put(EmailSubstitutionField.BULLET_3, cloudStorageClient.getImageUrl("bullet_3.png"))
        .put(EmailSubstitutionField.RT_STEPS, getRTSteps())
        .put(EmailSubstitutionField.CT_STEPS, getCTSteps(institutionName))
        .build();
  }

  private String getRTSteps() {
    StringBuilder rtSteps = new StringBuilder();
    encloseInLiTag(rtSteps, TWO_STEP_VERIFICATION);
    encloseInLiTag(rtSteps, LOGIN_GOV);
    encloseInLiTag(rtSteps, RT_TRAINING);
    return rtSteps.toString();
  }

  private String getCTSteps(String institutionName) {
    StringBuilder ctSteps = new StringBuilder();
    encloseInLiTag(ctSteps, String.format(CT_INSTITUTION_CHECK, institutionName));
    encloseInLiTag(ctSteps, CT_TRAINING);
    return ctSteps.toString();
  }

  private StringBuilder encloseInLiTag(StringBuilder steps, String step) {
    return steps.append(OPEN_LI_TAG).append(step).append(CLOSE_LI_TAG);
  }

  private Map<EmailSubstitutionField, String> instructionsSubstitutionMap(
      final String instructions, final String username) {
    return new ImmutableMap.Builder<EmailSubstitutionField, String>()
        .put(EmailSubstitutionField.HEADER_IMG, getAllOfUsLogo())
        .put(EmailSubstitutionField.ALL_OF_US, AOU_ITALICS)
        .put(EmailSubstitutionField.INSTRUCTIONS, instructions)
        .put(EmailSubstitutionField.USED_CREDITS, username)
        .build();
  }

  private String getInitialCreditsResolutionText() {
    return String.format(
        "can provide a new billing account in the Workbench to continue with your analyses. "
            + "Instructions for providing a new billing account are provided in the %s.",
        getSupportHubUrlAsHref());
  }

  private ImmutableMap<EmailSubstitutionField, String> initialCreditsDollarThresholdSubstitutionMap(
      final DbUser user, double currentUsage, double remainingBalance) {

    return new ImmutableMap.Builder<EmailSubstitutionField, String>()
        .put(EmailSubstitutionField.HEADER_IMG, getAllOfUsLogo())
        .put(EmailSubstitutionField.FIRST_NAME, user.getGivenName())
        .put(EmailSubstitutionField.LAST_NAME, user.getFamilyName())
        .put(EmailSubstitutionField.USERNAME, user.getUsername())
        .put(EmailSubstitutionField.USED_CREDITS, formatCurrency(currentUsage))
        .put(EmailSubstitutionField.CREDIT_BALANCE, formatCurrency(remainingBalance))
        .put(EmailSubstitutionField.INITIAL_CREDITS_RESOLUTION, getInitialCreditsResolutionText())
        .build();
  }

  private ImmutableMap<EmailSubstitutionField, String> initialCreditsExhaustionSubstitutionMap(
      DbUser user) {

    return new ImmutableMap.Builder<EmailSubstitutionField, String>()
        .put(EmailSubstitutionField.HEADER_IMG, getAllOfUsLogo())
        .put(EmailSubstitutionField.FIRST_NAME, user.getGivenName())
        .put(EmailSubstitutionField.LAST_NAME, user.getFamilyName())
        .put(EmailSubstitutionField.USERNAME, user.getUsername())
        .put(EmailSubstitutionField.INITIAL_CREDITS_RESOLUTION, getInitialCreditsResolutionText())
        .build();
  }

  private ImmutableMap<EmailSubstitutionField, String> initialCreditsExpiringSubstitutionMap(
      DbUser user) {

    return new ImmutableMap.Builder<EmailSubstitutionField, String>()
        .put(EmailSubstitutionField.HEADER_IMG, getAllOfUsLogo())
        .put(EmailSubstitutionField.ALL_OF_US, AOU_ITALICS)
        .put(EmailSubstitutionField.FIRST_NAME, user.getGivenName())
        .put(EmailSubstitutionField.USERNAME, user.getUsername())
        .put(
            EmailSubstitutionField.INITIAL_CREDITS_EXPIRATION,
            formatCondensedDateCentralTime(
                user.getUserInitialCreditsExpiration().getExpirationTime().toInstant()))
        .build();
  }

  private ImmutableMap<EmailSubstitutionField, String> accessTierSubstitutionMap(
      Instant expirationTime, DbUser user, String tierShortName) {

    return new ImmutableMap.Builder<EmailSubstitutionField, String>()
        .put(EmailSubstitutionField.HEADER_IMG, getAllOfUsLogo())
        .put(EmailSubstitutionField.ALL_OF_US, AOU_ITALICS)
        .put(EmailSubstitutionField.EXPIRATION_DATE, formatCentralTime(expirationTime))
        .put(EmailSubstitutionField.USERNAME, user.getUsername())
        .put(EmailSubstitutionField.URL, getUiUrlAsHref())
        .put(EmailSubstitutionField.TIER, StringUtils.capitalize(tierShortName))
        .put(
            EmailSubstitutionField.FIRST_NAME,
            HtmlEscapers.htmlEscaper().escape(user.getGivenName()))
        .put(EmailSubstitutionField.BADGE_URL, getBadgeImage(tierShortName))
        .build();
  }

  private ImmutableMap<EmailSubstitutionField, String> setupBillingAccountEmailMap(
      DbUser user, SendBillingSetupEmailRequest request) {

    return new ImmutableMap.Builder<EmailSubstitutionField, String>()
        .put(EmailSubstitutionField.HEADER_IMG, getAllOfUsLogo())
        .put(
            EmailSubstitutionField.FIRST_NAME,
            HtmlEscapers.htmlEscaper().escape(user.getGivenName()))
        .put(
            EmailSubstitutionField.LAST_NAME,
            HtmlEscapers.htmlEscaper().escape(user.getFamilyName()))
        .put(EmailSubstitutionField.ALL_OF_US, AOU_ITALICS)
        .put(
            EmailSubstitutionField.INSTITUTION_NAME,
            HtmlEscapers.htmlEscaper().escape(request.getInstitution()))
        .put(
            EmailSubstitutionField.USER_PHONE,
            HtmlEscapers.htmlEscaper().escape(request.getPhone()))
        .put(EmailSubstitutionField.FROM_EMAIL, workbenchConfigProvider.get().mail.fromEmail)
        .put(EmailSubstitutionField.USERNAME, user.getUsername())
        .put(EmailSubstitutionField.USER_CONTACT_EMAIL, user.getContactEmail())
        .put(
            EmailSubstitutionField.NIH_FUNDED,
            Boolean.TRUE.equals(request.isNihFunded()) ? "Yes" : "No")
        .build();
  }

  private Map<EmailSubstitutionField, String> workspaceAdminLockedSubstitutionMap(
      DbWorkspace workspace, String lockingReason) {
    return ImmutableMap.<EmailSubstitutionField, String>builder()
        .put(EmailSubstitutionField.HEADER_IMG, getAllOfUsLogo())
        .put(EmailSubstitutionField.ALL_OF_US, AOU_ITALICS)
        .put(EmailSubstitutionField.WORKSPACE_NAME, workspace.getName())
        .put(EmailSubstitutionField.WORKSPACE_NAMESPACE, workspace.getWorkspaceNamespace())
        .put(EmailSubstitutionField.LOCKING_REASON, lockingReason)
        .put(EmailSubstitutionField.RAB_SUPPORT_EMAIL, RAB_SUPPORT_EMAIL)
        .build();
  }

  private Map<EmailSubstitutionField, String> publishUnpublishWorkspaceSubstitutionMap(
      DbWorkspace workspace, DbUser user, String supportEmail) {
    return new ImmutableMap.Builder<EmailSubstitutionField, String>()
        .put(EmailSubstitutionField.HEADER_IMG, getAllOfUsLogo())
        .put(EmailSubstitutionField.ALL_OF_US, AOU_ITALICS)
        .put(EmailSubstitutionField.FIRST_NAME, user.getGivenName())
        .put(EmailSubstitutionField.LAST_NAME, user.getFamilyName())
        .put(EmailSubstitutionField.WORKSPACE_NAME, workspace.getName())
        .put(EmailSubstitutionField.WORKSPACE_NAMESPACE, workspace.getWorkspaceNamespace())
        .put(EmailSubstitutionField.SUPPORT_EMAIL, supportEmail)
        .build();
  }

  private String buildHtml(
      final String resource, final Map<EmailSubstitutionField, String> replacementMap)
      throws MessagingException {

    final String emailContent;
    try {
      emailContent =
          String.join(
              "\n", Resources.readLines(Resources.getResource(resource), StandardCharsets.UTF_8));
    } catch (IOException e) {
      throw new MessagingException("Error reading in email");
    }

    final Map<String, String> stringMap =
        replacementMap.entrySet().stream()
            .collect(Collectors.toMap(e -> e.getKey().toString(), Map.Entry::getValue));

    return new StringSubstitutor(stringMap).replace(emailContent);
  }

  private void sendWithRetries(
      List<String> toRecipientEmails,
      List<String> ccRecipientEmails,
      String subject,
      String descriptionForLog,
      String htmlMessage)
      throws MessagingException {
    sendWithRetries(
        workbenchConfigProvider.get().mail.fromEmail,
        toRecipientEmails,
        ccRecipientEmails,
        Collections.emptyList(),
        subject,
        descriptionForLog,
        htmlMessage);
  }

  private void sendWithRetries(
      String from,
      List<String> toRecipientEmails,
      List<String> ccRecipientEmails,
      List<String> bccRecipientEmails,
      String subject,
      String descriptionForLog,
      String htmlMessage)
      throws MessagingException {
    mailSenderProvider
        .get()
        .sendWithRetries(
            from,
            toRecipientEmails,
            ccRecipientEmails,
            bccRecipientEmails,
            subject,
            descriptionForLog,
            htmlMessage);
  }

  private String getAllOfUsLogo() {
    return cloudStorageClientProvider.get().getImageUrl("all_of_us_logo.png");
  }

  private String getRegistrationImage() {
    return cloudStorageClientProvider.get().getImageUrl("email_registration_example.png");
  }

  private String getBadgeImage(String tierShortName) {
    String imageURL =
        switch (tierShortName) {
          case REGISTERED_TIER_SHORT_NAME -> "registered-tier-badge.png";
          case CONTROLLED_TIER_SHORT_NAME -> "controlled-tier-badge.png";
          default -> null;
        };

    return cloudStorageClientProvider.get().getImageUrl(imageURL);
  }

  private String formatPercentage(double threshold) {
    return NumberFormat.getPercentInstance().format(threshold);
  }

  private String formatCurrency(double currentUsage) {
    return NumberFormat.getCurrencyInstance().format(currentUsage);
  }

  private String formatCondensedDateCentralTime(Instant date) {
    // e.g. 04/05/2021
    return DateTimeFormatter.ofPattern("MM/dd/yyyy")
        .withLocale(Locale.US)
        .withZone(ZoneId.of("America/Chicago"))
        .format(date);
  }

  private String formatDateCentralTime(Instant date) {
    // e.g. April 5, 2021
    return DateTimeFormatter.ofPattern("MMMM d, yyyy")
        .withLocale(Locale.US)
        .withZone(ZoneId.of("America/Chicago"))
        .format(date);
  }

  private String formatCentralTime(Instant date) {
    // e.g. April 5, 2021 at 1:23PM CT
    return DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' h:mm a 'CT'")
        .withLocale(Locale.US)
        .withZone(ZoneId.of("America/Chicago"))
        .format(date);
  }

  private String buildBillingAccountDescription(
      DbWorkspace workspace, @Nullable Double initialCreditsRemaining) {
    if (initialCreditsRemaining == null) {
      return String.format(
          "user-provided billing account \"%s\"", workspace.getBillingAccountName());
    } else {
      return String.format(
          "%s's initial credits ($%.2f remaining)",
          workspace.getCreator().getUsername(), initialCreditsRemaining);
    }
  }

  private String buildWorkspaceUrl(DbWorkspace workspace) {
    return String.format(
        "%s/workspaces/%s/%s/about",
        workbenchConfigProvider.get().server.uiBaseUrl,
        workspace.getWorkspaceNamespace(),
        workspace.getFirecloudName());
  }

  private String getUiUrlAsHref() {
    final String url = workbenchConfigProvider.get().server.uiBaseUrl;
    return href(url, url);
  }

  private String getSupportHubUrlAsHref() {
    final String host = workbenchConfigProvider.get().zendesk.host;
    final String path =
        "/hc/en-us/articles/360039539411-Initial-credits-and-how-to-create-a-billing-account";
    return href(host + path, "User Support Hub");
  }

  private String href(String url, String text) {
    return String.format("<a href=\"%s\">%s</a>", url, text);
  }

  private String userForLogging(String username, String contactEmail) {
    return String.format("%s (%s)", username, contactEmail);
  }

  private String userForLogging(DbUser user) {
    return userForLogging(user.getUsername(), user.getContactEmail());
  }

  private String ownersForLogging(Collection<DbUser> owners) {
    return owners.stream().map(this::userForLogging).collect(Collectors.joining(", "));
  }

  private List<String> ownersEmailList(Collection<DbUser> owners) {
    return owners.stream().map(DbUser::getContactEmail).toList();
  }
}
