package org.pmiops.workbench.mail;

import javax.mail.Message;
import javax.mail.MessagingException;
import org.pmiops.workbench.mandrill.model.MandrillMessage;
import org.pmiops.workbench.mandrill.model.MandrillMessageStatuses;

public interface MailService {

    void send(Message msg) throws MessagingException;

    MandrillMessageStatuses sendEmail(final MandrillMessage msg) throws MessagingException;
}
