package org.pmiops.workbench.mail;

import javax.mail.Message;
import javax.mail.MessagingException;

import com.google.api.services.admin.directory.model.User;

public interface MailService {

  void send(Message msg) throws MessagingException;

  void sendWelcomeEmail(String contactEmail, String password, User user) throws MessagingException;
}
