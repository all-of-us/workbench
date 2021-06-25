package org.pmiops.workbench.mail;

import com.google.api.services.directory.model.User;
import com.google.common.collect.ImmutableMap;
import com.google.common.html.HtmlEscapers;
import com.google.common.io.Resources;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.inject.Provider;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.text.StringSubstitutor;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.google.CloudStorageClient;
import org.pmiops.workbench.mandrill.api.MandrillApi;
import org.pmiops.workbench.mandrill.model.MandrillApiKeyAndMessage;
import org.pmiops.workbench.mandrill.model.MandrillMessage;
import org.pmiops.workbench.mandrill.model.MandrillMessageStatus;
import org.pmiops.workbench.mandrill.model.MandrillMessageStatuses;
import org.pmiops.workbench.mandrill.model.RecipientAddress;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MailServiceImpl implements MailService {
  private final Provider<MandrillApi> mandrillApiProvider;
  private final Provider<CloudStorageClient> cloudStorageClientProvider;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  private static final Logger log = Logger.getLogger(MailServiceImpl.class.getName());

  private static final String WELCOME_RESOURCE = "emails/welcomeemail/content.html";
  private static final String INSTRUCTIONS_RESOURCE = "emails/instructionsemail/content.html";
  private static final String FREE_TIER_DOLLAR_THRESHOLD_RESOURCE =
      "emails/dollarthresholdemail/content.html";
  private static final String FREE_TIER_EXPIRATION_RESOURCE =
      "emails/freecreditsexpirationemail/content.html";
  private static final String REGISTERED_TIER_ACCESS_THRESHOLD_RESOURCE =
      "emails/rt_access_threshold_email/content.html";
  private static final String REGISTERED_TIER_ACCESS_EXPIRED_RESOURCE =
      "emails/rt_access_expired_email/content.html";

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
  public void sendWelcomeEmail(final String contactEmail, final String password, final User user)
      throws MessagingException {

    final String htmlMessage =
        buildHtml(WELCOME_RESOURCE, welcomeMessageSubstitutionMap(password, user));

    sendWithRetries(
        contactEmail,
        "Your new All of Us Researcher Workbench Account",
        String.format("Welcome for %s", user.getName()),
        htmlMessage);
  }

  @Override
  public void sendInstitutionUserInstructions(String contactEmail, String userInstructions)
      throws MessagingException {

    // TODO(RW-6482): Use a templating system rather than manual oneoff escaping.
    // These institutional instructions are stored unescaped. Though they are input by admins,
    // the strings should not be trusted as HTML.
    String escapedUserInstructions = HtmlEscapers.htmlEscaper().escape(userInstructions);
    final String htmlMessage =
        buildHtml(INSTRUCTIONS_RESOURCE, instructionsSubstitutionMap(escapedUserInstructions));

    sendWithRetries(
        contactEmail,
        "Instructions from your institution on using the Researcher Workbench",
        String.format("Institution user instructions for %s", contactEmail),
        htmlMessage);
  }

  @Override
  public void alertUserFreeTierDollarThreshold(
      final DbUser user, double threshold, double currentUsage, double remainingBalance)
      throws MessagingException {

    final String logMsg =
        String.format(
            "User %s has passed the %.2f free tier dollar threshold.  Current total usage is $%.2f with remaining balance $%.2f",
            user.getUsername(), threshold, currentUsage, remainingBalance);
    log.info(logMsg);

    final String htmlMessage =
        buildHtml(
            FREE_TIER_DOLLAR_THRESHOLD_RESOURCE,
            freeTierDollarThresholdSubstitutionMap(user, currentUsage, remainingBalance));

    sendWithRetries(
        user.getContactEmail(),
        String.format(
            "Reminder - %s Free credit usage in All of Us Researcher Workbench",
            formatPercentage(threshold)),
        String.format("User %s passed a free tier dollar threshold", user.getUsername()),
        htmlMessage);
  }

  @Override
  public void alertUserFreeTierExpiration(final DbUser user) throws MessagingException {

    final String expirationMsg =
        String.format("Free credits have expired for User %s", user.getUsername());
    log.info(expirationMsg);

    final String htmlMessage =
        buildHtml(FREE_TIER_EXPIRATION_RESOURCE, freeTierExpirationSubstitutionMap(user));

    sendWithRetries(
        user.getContactEmail(),
        "Alert - Free credit expiration in All of Us Researcher Workbench",
        expirationMsg,
        htmlMessage);
  }

  @Override
  public void alertUserRegisteredTierWarningThreshold(
      final DbUser user, long daysRemaining, Instant expirationTime) throws MessagingException {
    final WorkbenchConfig workbenchConfig = workbenchConfigProvider.get();

    final String logMsg =
        String.format(
            "Registered Tier access expiration will occur for user %s in %d days (on %s).",
            user.getUsername(), daysRemaining, formatCentralTime(expirationTime));
    log.info(logMsg);

    if (workbenchConfig.accessRenewal.sendEmails) {
      final String htmlMessage =
          buildHtml(
              REGISTERED_TIER_ACCESS_THRESHOLD_RESOURCE,
              registeredTierAccessSubstitutionMap(expirationTime));

      sendWithRetries(
          user.getContactEmail(),
          "Your access to All of Us Registered Tier Data will expire "
              + (daysRemaining == 1 ? "tomorrow" : String.format("in %d days", daysRemaining)),
          String.format(
              "User %s will lose registered tier access in %d days",
              user.getUsername(), daysRemaining),
          htmlMessage);
    } else {
      log.info(
          "Email NOT sent.  Enable `accessRenewal.sendEmails` to send emails in this environment.");
    }
  }

  @Override
  public void alertUserRegisteredTierExpiration(final DbUser user, Instant expirationTime)
      throws MessagingException {
    final WorkbenchConfig workbenchConfig = workbenchConfigProvider.get();

    final String logMsg =
        String.format(
            "Registered Tier access expired for user %s (on %s).",
            user.getUsername(), formatCentralTime(expirationTime));
    log.info(logMsg);

    if (workbenchConfig.accessRenewal.sendEmails) {
      final String htmlMessage =
          buildHtml(
              REGISTERED_TIER_ACCESS_EXPIRED_RESOURCE,
              registeredTierAccessSubstitutionMap(expirationTime));

      sendWithRetries(
          user.getContactEmail(),
          "Your access to All of Us Registered Tier Data has expired",
          String.format("Registered Tier access expired for user %s", user.getUsername()),
          htmlMessage);
    } else {
      log.info(
          "Email NOT sent.  Enable `accessRenewal.sendEmails` to send emails in this environment.");
    }
  }

  private Map<EmailSubstitutionField, String> welcomeMessageSubstitutionMap(
      final String password, final User user) {
    final CloudStorageClient cloudStorageClient = cloudStorageClientProvider.get();
    return new ImmutableMap.Builder<EmailSubstitutionField, String>()
        .put(EmailSubstitutionField.USERNAME, user.getPrimaryEmail())
        .put(EmailSubstitutionField.PASSWORD, password)
        .put(EmailSubstitutionField.URL, workbenchConfigProvider.get().admin.loginUrl)
        .put(EmailSubstitutionField.HEADER_IMG, getAllOfUsLogo())
        .put(EmailSubstitutionField.REGISTRATION_IMG, getRegistrationImage())
        .put(EmailSubstitutionField.BULLET_1, cloudStorageClient.getImageUrl("bullet_1.png"))
        .put(EmailSubstitutionField.BULLET_2, cloudStorageClient.getImageUrl("bullet_2.png"))
        .put(EmailSubstitutionField.BULLET_3, cloudStorageClient.getImageUrl("bullet_3.png"))
        .build();
  }

  private Map<EmailSubstitutionField, String> instructionsSubstitutionMap(
      final String instructions) {
    return new ImmutableMap.Builder<EmailSubstitutionField, String>()
        .put(EmailSubstitutionField.HEADER_IMG, getAllOfUsLogo())
        .put(EmailSubstitutionField.INSTRUCTIONS, instructions)
        .build();
  }

  private String getFreeCreditsResolutionText() {
    if (workbenchConfigProvider.get().featureFlags.enableBillingUpgrade) {
      return "you can request additional free credits by contacting support "
          + "or provide a new billing account in the Workbench to continue with your analyses. "
          + "Instructions for providing a new billing account are provided in the Workbench.";
    } else {
      return "you can request for an extension of free credits by contacting support.";
    }
  }

  private ImmutableMap<EmailSubstitutionField, String> freeTierDollarThresholdSubstitutionMap(
      final DbUser user, double currentUsage, double remainingBalance) {

    return new ImmutableMap.Builder<EmailSubstitutionField, String>()
        .put(EmailSubstitutionField.HEADER_IMG, getAllOfUsLogo())
        .put(EmailSubstitutionField.FIRST_NAME, user.getGivenName())
        .put(EmailSubstitutionField.LAST_NAME, user.getFamilyName())
        .put(EmailSubstitutionField.USED_CREDITS, formatCurrency(currentUsage))
        .put(EmailSubstitutionField.CREDIT_BALANCE, formatCurrency(remainingBalance))
        .put(EmailSubstitutionField.FREE_CREDITS_RESOLUTION, getFreeCreditsResolutionText())
        .build();
  }

  private ImmutableMap<EmailSubstitutionField, String> freeTierExpirationSubstitutionMap(
      DbUser user) {

    return new ImmutableMap.Builder<EmailSubstitutionField, String>()
        .put(EmailSubstitutionField.HEADER_IMG, getAllOfUsLogo())
        .put(EmailSubstitutionField.FIRST_NAME, user.getGivenName())
        .put(EmailSubstitutionField.LAST_NAME, user.getFamilyName())
        .put(EmailSubstitutionField.FREE_CREDITS_RESOLUTION, getFreeCreditsResolutionText())
        .build();
  }

  private ImmutableMap<EmailSubstitutionField, String> registeredTierAccessSubstitutionMap(
      Instant expirationTime) {

    return new ImmutableMap.Builder<EmailSubstitutionField, String>()
        .put(EmailSubstitutionField.HEADER_IMG, getAllOfUsLogo())
        .put(EmailSubstitutionField.EXPIRATION_DATE, formatCentralTime(expirationTime))
        .put(EmailSubstitutionField.URL, getURLAsHref())
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

  private RecipientAddress validatedRecipient(final String contactEmail) throws MessagingException {
    try {
      final InternetAddress contactInternetAddress = new InternetAddress(contactEmail);
      contactInternetAddress.validate();
    } catch (AddressException e) {
      throw new MessagingException("Email: " + contactEmail + " is invalid.");
    }

    final RecipientAddress toAddress = new RecipientAddress();
    toAddress.setEmail(contactEmail);
    return toAddress;
  }

  private void sendWithRetries(
      String contactEmail, String subject, String description, String htmlMessage)
      throws MessagingException {
    final MandrillMessage msg =
        new MandrillMessage()
            .to(Collections.singletonList(validatedRecipient(contactEmail)))
            .html(htmlMessage)
            .subject(subject)
            .fromEmail(workbenchConfigProvider.get().mandrill.fromEmail);

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
            throw new MessagingException("Sending email failed: " + attempt.getRight().toString());
          }
          break;

        case REJECTED:
          log.log(
              Level.SEVERE,
              String.format(
                  "Messaging Exception: Email '%s' not sent: %s",
                  description, attempt.getRight().toString()));
          throw new MessagingException("Sending email failed: " + attempt.getRight().toString());

        case SUCCESSFUL:
          log.log(Level.INFO, String.format("Email '%s' was sent.", description));
          return;

        default:
          if (retries == 0) {
            log.log(
                Level.SEVERE, String.format("Email '%s' was not sent. Default case.", description));
            throw new MessagingException("Sending email failed: " + attempt.getRight().toString());
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

  private String getRegistrationImage() {
    return cloudStorageClientProvider.get().getImageUrl("email_registration_example.png");
  }

  private String formatPercentage(double threshold) {
    return NumberFormat.getPercentInstance().format(threshold);
  }

  private String formatCurrency(double currentUsage) {
    return NumberFormat.getCurrencyInstance().format(currentUsage);
  }

  private String formatCentralTime(Instant date) {
    // e.g. April 5, 2021 at 1:23PM Central Time
    return DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' h:mm a 'Central Time'")
        .withLocale(Locale.US)
        .withZone(ZoneId.of("America/Chicago"))
        .format(date);
  }

  private String getURLAsHref() {
    final String url = workbenchConfigProvider.get().server.uiBaseUrl;
    return String.format("<a href=\"%s\">%s</a>", url, url);
  }
}
