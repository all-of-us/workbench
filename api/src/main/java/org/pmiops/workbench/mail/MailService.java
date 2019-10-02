package org.pmiops.workbench.mail;

import com.google.api.services.directory.model.User;
import javax.mail.MessagingException;

public interface MailService {

  void sendBetaAccessRequestEmail(String userName) throws MessagingException;

  void sendWelcomeEmail(String contactEmail, String password, User user) throws MessagingException;

  void sendBetaAccessCompleteEmail(String contactEmail, String username) throws MessagingException;
}
