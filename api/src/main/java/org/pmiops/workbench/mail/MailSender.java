package org.pmiops.workbench.mail;

import jakarta.mail.MessagingException;
import java.util.List;

public interface MailSender {
  void sendWithRetries(
      String from,
      List<String> toRecipientEmails,
      List<String> ccRecipientEmails,
      List<String> bccRecipientEmails,
      String subject,
      String descriptionForLog,
      String htmlMessage)
      throws MessagingException;
}
