package org.pmiops.workbench.mail;

import com.google.api.services.directory.model.User;
import javax.mail.MessagingException;

public interface MailService {

  void sendBetaAccessRequestEmail(final String userName) throws MessagingException;

  void sendWelcomeEmail(final String contactEmail, final String password, final User user)
      throws MessagingException;

  void sendBetaAccessCompleteEmail(final String contactEmail, final String username)
      throws MessagingException;
}
