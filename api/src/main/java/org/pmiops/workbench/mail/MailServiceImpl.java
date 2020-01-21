package org.pmiops.workbench.mail;

import com.google.api.services.directory.model.User;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
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
import org.pmiops.workbench.google.CloudStorageService;
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
  private String BETA_ACCESS_TEXT = "A new user has requested beta access: ";

  private final Provider<MandrillApi> mandrillApiProvider;
  private final Provider<CloudStorageService> cloudStorageServiceProvider;
  private Provider<WorkbenchConfig> workbenchConfigProvider;
  private static final Logger log = Logger.getLogger(MailServiceImpl.class.getName());
  private static final String WELCOME_RESOURCE = "emails/welcomeemail/content.html";
  private static final String BETA_ACCESS_RESOURCE = "emails/betaaccessemail/content.html";

  enum Status {
    REJECTED,
    API_ERROR,
    SUCCESSFUL
  }

  @Autowired
  public MailServiceImpl(
      Provider<MandrillApi> mandrillApiProvider,
      Provider<CloudStorageService> cloudStorageServiceProvider,
      Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.mandrillApiProvider = mandrillApiProvider;
    this.cloudStorageServiceProvider = cloudStorageServiceProvider;
    this.workbenchConfigProvider = workbenchConfigProvider;
  }

  @Override
  public void sendBetaAccessRequestEmail(final String userName) throws MessagingException {
    WorkbenchConfig workbenchConfig = workbenchConfigProvider.get();

    MandrillMessage msg = new MandrillMessage();
    RecipientAddress toAddress = new RecipientAddress();
    toAddress.setEmail(workbenchConfig.admin.adminIdVerification);
    msg.setTo(Collections.singletonList(toAddress));
    msg.setSubject("[Beta Access Request: " + workbenchConfig.server.shortName + "]: " + userName);
    msg.setHtml(BETA_ACCESS_TEXT + userName);
    msg.setFromEmail(workbenchConfig.mandrill.fromEmail);

    sendWithRetries(msg, "Beta Access submit notification");
  }

  @Override
  public void sendWelcomeEmail(final String contactEmail, final String password, final User user)
      throws MessagingException {

    final MandrillMessage msg =
        new MandrillMessage()
            .to(Collections.singletonList(validatedRecipient(contactEmail)))
            .html(buildHtml(WELCOME_RESOURCE, welcomeMessageSubstitutionMap(password, user)))
            .subject("Your new All of Us Account")
            .fromEmail(workbenchConfigProvider.get().mandrill.fromEmail);

    sendWithRetries(msg, String.format("Welcome for %s", user.getName()));
  }

  @Override
  public void sendBetaAccessCompleteEmail(final String contactEmail, final String username)
      throws MessagingException {

    final MandrillMessage msg =
        new MandrillMessage()
            .to(Collections.singletonList(validatedRecipient(contactEmail)))
            .html(buildHtml(BETA_ACCESS_RESOURCE, betaAccessSubstitutionMap(username)))
            .subject("All of Us ID Verification Complete")
            .fromEmail(workbenchConfigProvider.get().mandrill.fromEmail);

    sendWithRetries(msg, String.format("BetaAccess Complete for %s", contactEmail));
  }

  private Map<EmailSubstitutionField, String> welcomeMessageSubstitutionMap(
      final String password, final User user) {
    final CloudStorageService cloudStorageService = cloudStorageServiceProvider.get();
    return new ImmutableMap.Builder<EmailSubstitutionField, String>()
        .put(EmailSubstitutionField.USERNAME, user.getPrimaryEmail())
        .put(EmailSubstitutionField.PASSWORD, password)
        .put(EmailSubstitutionField.URL, workbenchConfigProvider.get().admin.loginUrl)
        .put(
            EmailSubstitutionField.HEADER_IMG,
            cloudStorageService.getImageUrl("all_of_us_logo.png"))
        .put(EmailSubstitutionField.BULLET_1, cloudStorageService.getImageUrl("bullet_1.png"))
        .put(EmailSubstitutionField.BULLET_2, cloudStorageService.getImageUrl("bullet_2.png"))
        .build();
  }

  private Map<EmailSubstitutionField, String> betaAccessSubstitutionMap(final String username) {
    final CloudStorageService cloudStorageService = cloudStorageServiceProvider.get();

    final String action =
        "login to the workbench via <a class=\"link\" href=\""
            + workbenchConfigProvider.get().admin.loginUrl
            + "\">"
            + workbenchConfigProvider.get().admin.loginUrl
            + "</a>";

    return new ImmutableMap.Builder<EmailSubstitutionField, String>()
        .put(EmailSubstitutionField.ACTION, action)
        .put(EmailSubstitutionField.BETA_ACCESS_REPORT, "approved for use")
        .put(
            EmailSubstitutionField.HEADER_IMG,
            cloudStorageService.getImageUrl("all_of_us_logo.png"))
        .put(EmailSubstitutionField.USERNAME, username)
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

  private void sendWithRetries(MandrillMessage msg, String description) throws MessagingException {
    String apiKey = cloudStorageServiceProvider.get().readMandrillApiKey();
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
}
