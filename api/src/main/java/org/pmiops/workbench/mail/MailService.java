package org.pmiops.workbench.mail;

import javax.mail.MessagingException;

import com.google.api.services.admin.directory.model.User;

public interface MailService {

  void sendBetaAccessRequestEmail(String userName) throws MessagingException;

  void sendWelcomeEmail(String contactEmail, String password, User user) throws MessagingException;

  void sendBetaAccessCompleteEmail(String contactEmail, String username) throws MessagingException;
}
