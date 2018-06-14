package org.pmiops.workbench.mail;

import javax.mail.Message;
import javax.mail.MessagingException;

import com.google.api.services.admin.directory.model.User;
import org.pmiops.workbench.mandrill.model.MandrillMessageStatuses;

public interface MailService {

    void send(Message msg) throws MessagingException;

    MandrillMessageStatuses sendEmail(String contactEmail, String password, User user) throws MessagingException;
}
