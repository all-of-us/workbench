package org.pmiops.workbench.mail;

import static org.pmiops.workbench.access.AccessTierService.CONTROLLED_TIER_SHORT_NAME;
import static org.pmiops.workbench.access.AccessTierService.REGISTERED_TIER_SHORT_NAME;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Streams;
import com.google.common.html.HtmlEscapers;
import com.google.common.io.Resources;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.inject.Provider;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.text.CaseUtils;
import org.apache.commons.text.StringSubstitutor;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.config.WorkbenchConfig.EgressAlertRemediationPolicy;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.exfiltration.EgressRemediationAction;
import org.pmiops.workbench.google.CloudStorageClient;
import org.pmiops.workbench.leonardo.LeonardoLabelHelper;
import org.pmiops.workbench.leonardo.PersistentDiskUtils;
import org.pmiops.workbench.leonardo.model.LeonardoListPersistentDiskResponse;
import org.pmiops.workbench.mandrill.api.MandrillApi;
import org.pmiops.workbench.mandrill.model.MandrillApiKeyAndMessage;
import org.pmiops.workbench.mandrill.model.MandrillMessage;
import org.pmiops.workbench.mandrill.model.MandrillMessageStatus;
import org.pmiops.workbench.mandrill.model.MandrillMessageStatuses;
import org.pmiops.workbench.mandrill.model.RecipientAddress;
import org.pmiops.workbench.mandrill.model.RecipientType;
import org.pmiops.workbench.model.SendBillingSetupEmailRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MailServiceImpl implements MailService {

  public static final ImmutableMap<EgressRemediationAction, String> EGRESS_REMEDIATION_ACTION_MAP =
      ImmutableMap.of(
          EgressRemediationAction.DISABLE_USER,
          "Your account has been disabled pending manual review by the <i>All of Us</i> "
              + "security team.",
          EgressRemediationAction.SUSPEND_COMPUTE,
          "Your Workbench compute access has been temporarily suspended, and will be "
              + "automatically restored after a brief duration.");
  private final Provider<MandrillApi> mandrillApiProvider;
  private final Provider<CloudStorageClient> cloudStorageClientProvider;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  private static final Logger log = Logger.getLogger(MailServiceImpl.class.getName());

  private static final String EGRESS_REMEDIATION_RESOURCE =
      "emails/egress_remediation/content.html";
  private static final String FILE_LENGTHS_EGRESS_REMEDIATION_EMAIL =
      "emails/file_lengths_egress_remediation_email/content.html";
  private static final String INITIAL_CREDITS_DOLLAR_THRESHOLD_RESOURCE =
      "emails/initial_credits_dollar_threshold/content.html";
  private static final String INITIAL_CREDITS_EXPIRATION_RESOURCE =
      "emails/initial_credits_expiration/content.html";
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
  private static final String ERA_COMMON = "Connect your eRA Commons account";
  private static final String RT_TRAINING = "Complete All of Us Registered Tier Training";
  private static final String LOGIN_GOV = "Verify your identity with Login.gov";

  // CT Steps
  private static final String CT_TRAINING = "Complete All of Us Controlled Tier Training";

  private enum Status {
    REJECTED,
    API_ERROR,
    SUCCESSFUL
  }

  @Autowired
  public MailServiceImpl(
      Provider<MandrillApi> mandrillApiProvider,
      Provider<CloudStorageClient> cloudStorageClientProvider,
      Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.mandrillApiProvider = mandrillApiProvider;
    this.cloudStorageClientProvider = cloudStorageClientProvider;
    this.workbenchConfigProvider = workbenchConfigProvider;
  }

  @Override
  public void sendWelcomeEmail(
      final String contactEmail,
      final String password,
      final String username,
      final String institutionName,
      final Boolean showEraStepInRt,
      final Boolean showEraStepInCt)
      throws MessagingException {
    final String htmlMessage =
        buildHtml(
            WELCOME_RESOURCE,
            welcomeMessageSubstitutionMap(
                password, username, institutionName, showEraStepInRt, showEraStepInCt));

    sendWithRetries(
        Collections.singletonList(contactEmail),
        Collections.emptyList(),
        "Your new All of Us Researcher Workbench Account",
        String.format("Welcome for %s", username),
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
        String.format("Institution user instructions for %s", contactEmail),
        htmlMessage);
  }

  @Override
  public void alertUserInitialCreditsDollarThreshold(
      final DbUser user, double threshold, double currentUsage, double remainingBalance)
      throws MessagingException {

    final String logMsg =
        String.format(
            "User %s has passed the %.2f initial credits dollar threshold.  Current total usage is $%.2f with remaining balance $%.2f",
            user.getUsername(), threshold, currentUsage, remainingBalance);
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
        String.format("User %s passed an initial credits dollar threshold", user.getUsername()),
        htmlMessage);
  }

  @Override
  public void alertUserInitialCreditsExpiration(final DbUser user) throws MessagingException {

    final String expirationMsg =
        String.format("Initial credits have expired for User %s", user.getUsername());
    log.info(expirationMsg);

    final String htmlMessage =
        buildHtml(
            INITIAL_CREDITS_EXPIRATION_RESOURCE, initialCreditsExpirationSubstitutionMap(user));

    sendWithRetries(
        Collections.singletonList(user.getContactEmail()),
        Collections.emptyList(),
        "Alert - Initial credit expiration in All of Us Researcher Workbench",
        expirationMsg,
        htmlMessage);
  }

  @Override
  public void alertUserAccessTierWarningThreshold(
      final DbUser user, long daysRemaining, Instant expirationTime, String tierShortName)
      throws MessagingException {

    String capitalizedAccessTierShortName = StringUtils.capitalize(tierShortName);

    final String logMsg =
        String.format(
            "%s Tier access expiration will occur for user %s (%s) in %d days (on %s).",
            capitalizedAccessTierShortName,
            user.getUsername(),
            user.getContactEmail(),
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
            "User %s (%s) will lose " + tierShortName + " tier access in %d days",
            user.getUsername(),
            user.getContactEmail(),
            daysRemaining),
        htmlMessage);
  }

  @Override
  public void alertUserAccessTierExpiration(
      final DbUser user, Instant expirationTime, String tierShortName) throws MessagingException {
    String capitalizedAccessTierShortName = StringUtils.capitalize(tierShortName);

    final String logMsg =
        String.format(
            capitalizedAccessTierShortName + " Tier access expired for user %s (%s) on %s.",
            user.getUsername(),
            user.getContactEmail(),
            formatCentralTime(expirationTime));
    log.info(logMsg);

    final String htmlMessage =
        buildHtml(
            TIER_ACCESS_EXPIRED_RESOURCE,
            accessTierSubstitutionMap(expirationTime, user, tierShortName));

    sendWithRetries(
        Collections.singletonList(user.getContactEmail()),
        Collections.emptyList(),
        "Your access to All of Us " + capitalizedAccessTierShortName + " Tier Data has expired",
        String.format(
            capitalizedAccessTierShortName + " Tier access expired for user %s (%s)",
            user.getUsername(),
            user.getContactEmail()),
        htmlMessage);
  }

  private String getEnvironmentType(Object labels) {
    return LeonardoLabelHelper.maybeMapLeonardoLabelsToGkeApp(labels)
        .map(appType -> CaseUtils.toCamelCase(appType.toString(), true))
        .orElse("Jupyter");
  }

  @Override
  public void alertUsersUnusedDiskWarningThreshold(
      List<DbUser> users,
      DbWorkspace diskWorkspace,
      LeonardoListPersistentDiskResponse disk,
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
                    String.format(
                        "$%.2f",
                        PersistentDiskUtils.costPerMonth(disk, diskWorkspace.getGoogleProject())))
                .put(
                    EmailSubstitutionField.DISK_CREATION_DATE,
                    formatDateCentralTime(Instant.parse(disk.getAuditInfo().getCreatedDate())))
                .put(EmailSubstitutionField.DISK_CREATOR_USERNAME, disk.getAuditInfo().getCreator())
                .put(
                    EmailSubstitutionField.DISK_STATUS,
                    isDiskAttached ? ATTACHED_DISK_STATUS : DETACHED_DISK_STATUS)
                .put(EmailSubstitutionField.ENVIRONMENT_TYPE, getEnvironmentType(disk.getLabels()))
                .put(
                    EmailSubstitutionField.BILLING_ACCOUNT_DETAILS,
                    buildBillingAccountDescription(diskWorkspace, workspaceInitialCreditsRemaining))
                .put(EmailSubstitutionField.WORKSPACE_URL, buildWorkspaceUrl(diskWorkspace))
                .put(EmailSubstitutionField.DISK_DELETE_INSTRUCTION, UNUSED_DISK_DELETE_HELP)
                .build());
    sendWithRetries(
        workbenchConfigProvider.get().mandrill.fromEmail,
        ImmutableList.of(),
        ImmutableList.of(),
        users.stream().map(DbUser::getContactEmail).collect(Collectors.toList()),
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
        Collections.singletonList(workbenchConfigProvider.get().mandrill.fromEmail),
        "Request to set up Google Cloud Billing Account for All of Us Workbench",
        String.format(
            " User %s (%s) requests billing setup from Carasoft.",
            dbUser.getUsername(), dbUser.getContactEmail()),
        htmlMessage);
  }

  @Override
  public void sendEgressRemediationEmail(DbUser dbUser, EgressRemediationAction action)
      throws MessagingException {
    sendEgressRemediationEmailWithContent(dbUser, action, EGRESS_REMEDIATION_RESOURCE);
  }

  @Override
  public void sendFileLengthsEgressRemediationEmail(DbUser dbUser, EgressRemediationAction action)
      throws MessagingException {
    sendEgressRemediationEmailWithContent(dbUser, action, FILE_LENGTHS_EGRESS_REMEDIATION_EMAIL);
  }

  @Override
  public void sendNewUserSatisfactionSurveyEmail(DbUser dbUser, String surveyLink)
      throws MessagingException {
    String htmlMessage =
        buildHtml(
            NEW_USER_SATISFACTION_SURVEY_RESOURCE,
            ImmutableMap.<EmailSubstitutionField, String>builder()
                .put(EmailSubstitutionField.HEADER_IMG, getAllOfUsLogo())
                .put(EmailSubstitutionField.ALL_OF_US, getAllOfUsItalicsText())
                .put(EmailSubstitutionField.SURVEY_LINK, surveyLink)
                .build());

    sendWithRetries(
        ImmutableList.of(dbUser.getContactEmail()),
        ImmutableList.of(),
        "Researcher satisfaction survey for the All of Us Researcher Workbench",
        String.format("New user satisfaction survey email for %s", dbUser.getUsername()),
        htmlMessage);
  }

  @Override
  public void sendWorkspaceAdminLockingEmail(
      DbWorkspace workspace, String lockingReason, List<DbUser> owners) throws MessagingException {

    WorkbenchConfig config = workbenchConfigProvider.get();
    List<String> ccSupportMaybe =
        config.featureFlags.ccSupportWhenAdminLocking
            ? ImmutableList.of(config.mandrill.fromEmail)
            : Collections.emptyList();

    final String ownersInfoStr =
        owners.stream()
            .map(o -> String.format("%s (%s)", o.getUsername(), o.getContactEmail()))
            .collect(Collectors.joining(", "));

    sendWithRetries(
        owners.stream().map(DbUser::getContactEmail).collect(Collectors.toList()),
        ccSupportMaybe,
        "[Response Required] AoU Researcher Workbench Workspace Admin Locked",
        String.format(
            "Admin locking email for workspace '%s' (%s) sent to owners %s",
            workspace.getName(), workspace.getWorkspaceNamespace(), ownersInfoStr),
        buildHtml(
            WORKSPACE_ADMIN_LOCKING_RESOURCE,
            workspaceAdminLockedSubstitutionMap(workspace, lockingReason)));
  }

  private void sendEgressRemediationEmailWithContent(
      DbUser dbUser, EgressRemediationAction action, String remediationEmail)
      throws MessagingException {
    String remediationDescription = EGRESS_REMEDIATION_ACTION_MAP.get(action);
    String htmlMessage =
        buildHtml(
            remediationEmail,
            ImmutableMap.<EmailSubstitutionField, String>builder()
                .put(EmailSubstitutionField.HEADER_IMG, getAllOfUsLogo())
                .put(EmailSubstitutionField.ALL_OF_US, getAllOfUsItalicsText())
                .put(EmailSubstitutionField.USERNAME, dbUser.getUsername())
                .put(EmailSubstitutionField.EGRESS_REMEDIATION_DESCRIPTION, remediationDescription)
                .build());

    EgressAlertRemediationPolicy egressPolicy =
        workbenchConfigProvider.get().egressAlertRemediationPolicy;
    sendWithRetries(
        egressPolicy.notifyFromEmail,
        ImmutableList.of(dbUser.getContactEmail()),
        Optional.ofNullable(egressPolicy.notifyCcEmails).orElse(ImmutableList.of()),
        ImmutableList.of(),
        "[Response Required] AoU Researcher Workbench High Data Egress Alert",
        String.format("Egress remediation email for %s", dbUser.getUsername()),
        htmlMessage);
  }

  private Map<EmailSubstitutionField, String> welcomeMessageSubstitutionMap(
      final String password,
      final String username,
      final String institutionName,
      final Boolean showEraStepInRT,
      final Boolean showEraStepInCT) {
    final CloudStorageClient cloudStorageClient = cloudStorageClientProvider.get();
    return new ImmutableMap.Builder<EmailSubstitutionField, String>()
        .put(EmailSubstitutionField.USERNAME, username)
        .put(EmailSubstitutionField.PASSWORD, password)
        .put(EmailSubstitutionField.URL, workbenchConfigProvider.get().admin.loginUrl)
        .put(EmailSubstitutionField.HEADER_IMG, getAllOfUsLogo())
        .put(EmailSubstitutionField.ALL_OF_US, getAllOfUsItalicsText())
        .put(EmailSubstitutionField.REGISTRATION_IMG, getRegistrationImage())
        .put(EmailSubstitutionField.BULLET_1, cloudStorageClient.getImageUrl("bullet_1.png"))
        .put(EmailSubstitutionField.BULLET_2, cloudStorageClient.getImageUrl("bullet_2.png"))
        .put(EmailSubstitutionField.BULLET_3, cloudStorageClient.getImageUrl("bullet_3.png"))
        .put(EmailSubstitutionField.RT_STEPS, getRTSteps(showEraStepInRT))
        .put(EmailSubstitutionField.CT_STEPS, getCTSteps(showEraStepInCT, institutionName))
        .build();
  }

  private String getRTSteps(Boolean showEraStepInRT) {
    StringBuffer rtSteps = new StringBuffer();
    encloseInLiTag(rtSteps, TWO_STEP_VERIFICATION);
    encloseInLiTag(rtSteps, LOGIN_GOV);
    if (showEraStepInRT) {
      encloseInLiTag(rtSteps, ERA_COMMON);
    }
    encloseInLiTag(rtSteps, RT_TRAINING);
    return rtSteps.toString();
  }

  private String getCTSteps(Boolean showEraStepInCT, String institutionName) {
    StringBuffer ctSteps = new StringBuffer();
    encloseInLiTag(ctSteps, String.format(CT_INSTITUTION_CHECK, institutionName));
    if (showEraStepInCT) {
      encloseInLiTag(ctSteps, ERA_COMMON);
    }
    encloseInLiTag(ctSteps, CT_TRAINING);
    return ctSteps.toString();
  }

  private StringBuffer encloseInLiTag(StringBuffer steps, String step) {
    return steps.append(OPEN_LI_TAG).append(step).append(CLOSE_LI_TAG);
  }

  private Map<EmailSubstitutionField, String> instructionsSubstitutionMap(
      final String instructions, final String username) {
    return new ImmutableMap.Builder<EmailSubstitutionField, String>()
        .put(EmailSubstitutionField.HEADER_IMG, getAllOfUsLogo())
        .put(EmailSubstitutionField.ALL_OF_US, getAllOfUsItalicsText())
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

  private ImmutableMap<EmailSubstitutionField, String> initialCreditsExpirationSubstitutionMap(
      DbUser user) {

    return new ImmutableMap.Builder<EmailSubstitutionField, String>()
        .put(EmailSubstitutionField.HEADER_IMG, getAllOfUsLogo())
        .put(EmailSubstitutionField.FIRST_NAME, user.getGivenName())
        .put(EmailSubstitutionField.LAST_NAME, user.getFamilyName())
        .put(EmailSubstitutionField.USERNAME, user.getUsername())
        .put(EmailSubstitutionField.INITIAL_CREDITS_RESOLUTION, getInitialCreditsResolutionText())
        .build();
  }

  private ImmutableMap<EmailSubstitutionField, String> accessTierSubstitutionMap(
      Instant expirationTime, DbUser user, String tierShortName) {

    return new ImmutableMap.Builder<EmailSubstitutionField, String>()
        .put(EmailSubstitutionField.HEADER_IMG, getAllOfUsLogo())
        .put(EmailSubstitutionField.ALL_OF_US, getAllOfUsItalicsText())
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
        .put(EmailSubstitutionField.ALL_OF_US, getAllOfUsItalicsText())
        .put(
            EmailSubstitutionField.INSTITUTION_NAME,
            HtmlEscapers.htmlEscaper().escape(request.getInstitution()))
        .put(
            EmailSubstitutionField.USER_PHONE,
            HtmlEscapers.htmlEscaper().escape(request.getPhone()))
        .put(EmailSubstitutionField.FROM_EMAIL, workbenchConfigProvider.get().mandrill.fromEmail)
        .put(EmailSubstitutionField.USERNAME, user.getUsername())
        .put(EmailSubstitutionField.USER_CONTACT_EMAIL, user.getContactEmail())
        .put(
            EmailSubstitutionField.NIH_FUNDED,
            Boolean.TRUE.equals(request.getIsNihFunded()) ? "Yes" : "No")
        .build();
  }

  private Map<EmailSubstitutionField, String> workspaceAdminLockedSubstitutionMap(
      DbWorkspace workspace, String lockingReason) {
    return ImmutableMap.<EmailSubstitutionField, String>builder()
        .put(EmailSubstitutionField.HEADER_IMG, getAllOfUsLogo())
        .put(EmailSubstitutionField.ALL_OF_US, getAllOfUsItalicsText())
        .put(EmailSubstitutionField.WORKSPACE_NAME, workspace.getName())
        .put(EmailSubstitutionField.WORKSPACE_NAMESPACE, workspace.getWorkspaceNamespace())
        .put(EmailSubstitutionField.LOCKING_REASON, lockingReason)
        .put(EmailSubstitutionField.RAB_SUPPORT_EMAIL, RAB_SUPPORT_EMAIL)
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

  private RecipientAddress validatedRecipient(
      final String contactEmail, final RecipientType recipientType) {
    try {
      final InternetAddress contactInternetAddress = new InternetAddress(contactEmail);
      contactInternetAddress.validate();
    } catch (AddressException e) {
      throw new ServerErrorException("Email: " + contactEmail + " is invalid.");
    }

    final RecipientAddress toAddress = new RecipientAddress();
    toAddress.email(contactEmail).type(recipientType);
    return toAddress;
  }

  private void sendWithRetries(
      List<String> toRecipientEmails,
      List<String> ccRecipientEmails,
      String subject,
      String description,
      String htmlMessage)
      throws MessagingException {
    sendWithRetries(
        workbenchConfigProvider.get().mandrill.fromEmail,
        toRecipientEmails,
        ccRecipientEmails,
        ImmutableList.of(),
        subject,
        description,
        htmlMessage);
  }

  private void sendWithRetries(
      String from,
      List<String> toRecipientEmails,
      List<String> ccRecipientEmails,
      List<String> bccRecipientEmails,
      String subject,
      String description,
      String htmlMessage)
      throws MessagingException {
    List<RecipientAddress> toAddresses =
        Streams.concat(
                toRecipientEmails.stream().map(e -> validatedRecipient(e, RecipientType.TO)),
                ccRecipientEmails.stream().map(e -> validatedRecipient(e, RecipientType.CC)),
                bccRecipientEmails.stream().map(e -> validatedRecipient(e, RecipientType.BCC)))
            .collect(Collectors.toList());
    final MandrillMessage msg =
        new MandrillMessage()
            .to(toAddresses)
            .html(htmlMessage)
            .subject(subject)
            .preserveRecipients(true)
            .fromEmail(from);

    String apiKey = cloudStorageClientProvider.get().readMandrillApiKey();
    int retries = workbenchConfigProvider.get().mandrill.sendRetries;
    MandrillApiKeyAndMessage keyAndMessage = new MandrillApiKeyAndMessage();
    keyAndMessage.setKey(apiKey);
    keyAndMessage.setMessage(msg);
    do {
      retries--;
      Pair<Status, String> attempt = trySend(keyAndMessage);
      Status status = Status.valueOf(attempt.getLeft().toString());
      switch (status) {
        case API_ERROR:
          log.log(
              Level.WARNING,
              String.format(
                  "ApiException: Email '%s' not sent: %s",
                  description, attempt.getRight().toString()));
          if (retries == 0) {
            log.log(
                Level.SEVERE,
                String.format(
                    "ApiException: On Last Attempt! Email '%s' not sent: %s",
                    description, attempt.getRight().toString()));
            throw new MessagingException("Sending email failed: " + attempt.getRight());
          }
          break;

        case REJECTED:
          log.log(
              Level.SEVERE,
              String.format(
                  "Messaging Exception: Email '%s' not sent: %s",
                  description, attempt.getRight().toString()));
          throw new MessagingException("Sending email failed: " + attempt.getRight());

        case SUCCESSFUL:
          log.log(Level.INFO, String.format("Email '%s' was sent.", description));
          return;

        default:
          if (retries == 0) {
            log.log(
                Level.SEVERE, String.format("Email '%s' was not sent. Default case.", description));
            throw new MessagingException("Sending email failed: " + attempt.getRight());
          }
      }
    } while (retries > 0);
  }

  private Pair<Status, String> trySend(MandrillApiKeyAndMessage keyAndMessage) {
    try {
      MandrillMessageStatuses msgStatuses = mandrillApiProvider.get().send(keyAndMessage);
      for (MandrillMessageStatus msgStatus : msgStatuses) {
        if (msgStatus.getRejectReason() != null) {
          return new ImmutablePair<>(Status.REJECTED, msgStatus.getRejectReason());
        }
      }
    } catch (Exception e) {
      return new ImmutablePair<>(Status.API_ERROR, e.toString());
    }
    return new ImmutablePair<>(Status.SUCCESSFUL, "");
  }

  private String getAllOfUsLogo() {
    return cloudStorageClientProvider.get().getImageUrl("all_of_us_logo.png");
  }

  private String getAllOfUsItalicsText() {
    return "<i>All of Us</i>";
  }

  private String getRegistrationImage() {
    return cloudStorageClientProvider.get().getImageUrl("email_registration_example.png");
  }

  private String getBadgeImage(String tierShortName) {
    String imageURL;
    switch (tierShortName) {
      case REGISTERED_TIER_SHORT_NAME:
        imageURL = "registered-tier-badge.png";
        break;
      case CONTROLLED_TIER_SHORT_NAME:
        imageURL = "controlled-tier-badge.png";
        break;
      default:
        imageURL = null;
    }

    return cloudStorageClientProvider.get().getImageUrl(imageURL);
  }

  private String formatPercentage(double threshold) {
    return NumberFormat.getPercentInstance().format(threshold);
  }

  private String formatCurrency(double currentUsage) {
    return NumberFormat.getCurrencyInstance().format(currentUsage);
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
}
