package org.pmiops.workbench.mail;

import com.google.api.services.admin.directory.model.User;
import com.google.common.io.Resources;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.mandrill.api.MandrillApi;
import org.pmiops.workbench.mandrill.model.MandrillApiKeyAndMessage;
import org.pmiops.workbench.mandrill.model.MandrillMessage;
import org.pmiops.workbench.mandrill.model.MandrillMessageStatuses;
import org.pmiops.workbench.mandrill.model.MandrillMessageStatus;
import org.pmiops.workbench.mandrill.model.RecipientAddress;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import javax.inject.Provider;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

@Service
public class MailServiceImpl implements MailService {

  private final Provider<MandrillApi> mandrillApiProvider;
  private final Provider<CloudStorageService> cloudStorageServiceProvider;
  private Provider<WorkbenchConfig> workbenchConfigProvider;
  private static final Logger log = Logger.getLogger(MailServiceImpl.class.getName());

  enum Status {REJECTED, API_ERROR, SUCCESSFUL}

  @Autowired
  public MailServiceImpl(Provider<MandrillApi> mandrillApiProvider,
                         Provider<CloudStorageService> cloudStorageServiceProvider,
                         Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.mandrillApiProvider = mandrillApiProvider;
    this.cloudStorageServiceProvider = cloudStorageServiceProvider;
    this.workbenchConfigProvider = workbenchConfigProvider;
  }

  public void send(Message msg) throws MessagingException {
    Transport.send(msg);
  }

  public void sendWelcomeEmail(String contactEmail, String password, User user) throws MessagingException {
    try {
      InternetAddress email = new InternetAddress(contactEmail);
      email.validate();
    } catch (AddressException e) {
      throw new MessagingException("Email: " + contactEmail + " is invalid.");
    }
    String apiKey = cloudStorageServiceProvider.get().readMandrillApiKey();
    int retries = workbenchConfigProvider.get().mandrill.sendRetries;
    MandrillApiKeyAndMessage keyAndMessage = new MandrillApiKeyAndMessage();
    keyAndMessage.setKey(apiKey);
    MandrillMessage msg = buildWelcomeMessage(contactEmail, password, user);
    keyAndMessage.setMessage(msg);
    do {
      retries--;
      ImmutablePair attempt = trySend(keyAndMessage);
      Status status = Status.valueOf(attempt.getLeft().toString());
      switch (status) {
        case API_ERROR:
          log.log(Level.WARNING, String.format(
            "ApiException: Welcome Email to '%s' for user '%s' not sent: %s", contactEmail, user.getName(), attempt.getRight().toString()));
          if (retries == 0) {
            log.log(Level.SEVERE, String.format(
              "ApiException: On Last Attempt! Welcome Email to '%s' for user '%s' not sent: %s", contactEmail, user.getName(), attempt.getRight().toString()));
            throw new MessagingException("Sending email failed: " + attempt.getRight().toString());
          }
          break;

        case REJECTED:
          log.log(Level.SEVERE, String.format(
            "Messaging Exception: Welcome Email to '%s' for user '%s' not sent: %s", contactEmail, user.getName(), attempt.getRight().toString()));
          throw new MessagingException("Sending email failed: " + attempt.getRight().toString());

        case SUCCESSFUL:
          log.log(Level.INFO, String.format(
            "Welcome Email to '%s' for user '%s' was sent.", contactEmail, user.getName()));
          return;

        default:
          if (retries == 0) {
            log.log(Level.SEVERE, String.format(
              "Welcome Email to '%s' for user '%s' was not sent. Default case.", contactEmail, user.getName()));
            throw new MessagingException("Sending email failed: " + attempt.getRight().toString());
          }
      }
    } while (retries > 0);
  }

  private MandrillMessage buildWelcomeMessage(String contactEmail, String password, User user) throws MessagingException{
    MandrillMessage msg = new MandrillMessage();
    RecipientAddress toAddress = new RecipientAddress();
    toAddress.setEmail(contactEmail);
    msg.setTo(Collections.singletonList(toAddress));
    String msgHtml = buildEmailHtml(password, user);
    msg.setHtml(msgHtml);
    msg.setSubject("Your new All of Us Account");
    msg.setFromEmail(workbenchConfigProvider.get().mandrill.fromEmail);
    return msg;
  }

  private String buildEmailHtml(String password, User user) throws MessagingException {
    CloudStorageService cloudStorageService = cloudStorageServiceProvider.get();
    StringBuilder contentBuilder = new StringBuilder();
    String path = Resources.getResource("emails/welcomeemail/content.html").getPath();
    try (Stream<String> stream = Files.lines( Paths.get(path), StandardCharsets.UTF_8)) {
      stream.forEach(s -> contentBuilder.append(s).append("\n"));
    } catch (IOException e) {
      throw new MessagingException("Error reading in email");
    }
    String string = contentBuilder.toString();
    Map<String, String> replaceMap = new HashMap<>();
    replaceMap.put("USERNAME", user.getPrimaryEmail());
    replaceMap.put("PASSWORD", password);
    replaceMap.put("URL", workbenchConfigProvider.get().admin.loginPage);
    replaceMap.put("HEADER_IMG", cloudStorageService.getImageUrl("all_of_us_logo.png"));
    replaceMap.put("BULLET_1", cloudStorageService.getImageUrl("bullet_1.png"));
    replaceMap.put("BULLET_2", cloudStorageService.getImageUrl("bullet_2.png"));
    StrSubstitutor email = new StrSubstitutor(replaceMap);
    return email.replace(string);
  }

  private ImmutablePair<Status, String> trySend(MandrillApiKeyAndMessage keyAndMessage) {
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
