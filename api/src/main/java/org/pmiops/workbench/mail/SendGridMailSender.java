package org.pmiops.workbench.mail;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.google.CloudStorageClient;
import org.springframework.stereotype.Service;

@Service
public class SendGridMailSender implements MailSender {
  private final SendGrid sendGrid;
  private final int maxRetries;
  private static final Logger log = Logger.getLogger(SendGridMailSender.class.getName());


  public SendGridMailSender(CloudStorageClient cloudStorageClient, WorkbenchConfig workbenchConfig) {
    this.sendGrid = new SendGrid(cloudStorageClient.readSendgridApiKey());
    this.maxRetries = workbenchConfig.mandrill.sendRetries;
  }

  @Override
  public void send(String from, List<String> toRecipientEmails, List<String> ccRecipientEmails,
      List<String> bccRecipientEmails, String subject, String descriptionForLog, String htmlMessage)
      throws MessagingException {
    Mail mail = createMail(from, toRecipientEmails, ccRecipientEmails, bccRecipientEmails, subject, htmlMessage);
    Request request = new Request();
    request.setMethod(Method.POST);
    request.setEndpoint("mail/send");

    try {
      request.setBody(mail.build());
    } catch (IOException e) {
      log.log(Level.SEVERE, String.format("Error building email '%s': %s", descriptionForLog, e.getMessage()));
      throw new MessagingException("Building email failed", e);
    }

    int retries = maxRetries;
    do {
      retries--;
      try {
        sendGrid.api(request);

        log.log(Level.INFO, String.format("Email '%s' was sent.", descriptionForLog));
        return;
      } catch (IOException e) {
        log.log(
            Level.WARNING,
            String.format(
                "Messaging Exception: Email '%s' not sent: %s",
                descriptionForLog, e.getMessage()));
        if (retries == 0) {
          log.log(
              Level.SEVERE,
              String.format(
                  "Messaging Exception: On Last Attempt! Email '%s' not sent: %s",
                  descriptionForLog, e.getMessage()));
          throw new MessagingException("Sending email failed", e);
        }
      }
    } while (retries > 0);

  }

  Mail createMail(String from, List<String> toRecipientEmails,
      List<String> ccRecipientEmails, List<String> bccRecipientEmails, String subject, String htmlMessage) {
    Mail mail = new Mail();
    Personalization personalization = new Personalization();
    toRecipientEmails.stream().map(this::validatedRecipient).forEach(personalization::addTo);
    ccRecipientEmails.stream().map(this::validatedRecipient).forEach(personalization::addCc);
    bccRecipientEmails.stream().map(this::validatedRecipient).forEach(personalization::addBcc);


    mail.addPersonalization(personalization);
    mail.setFrom(new Email(from));
    mail.setSubject(subject);

    mail.addContent(new Content("text/html", htmlMessage));
    return mail;
  }

  private Email validatedRecipient(final String contactEmail) {
    try {
      final InternetAddress contactInternetAddress = new InternetAddress(contactEmail);
      contactInternetAddress.validate();
    } catch (AddressException e) {
      throw new ServerErrorException(String.format("Email: %s is invalid.", contactEmail));
    }

    return new Email(contactEmail);
  }
}
