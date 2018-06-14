package org.pmiops.workbench.mail;

import javax.mail.Message;
import javax.mail.MessagingException;

public interface MailService {

    void send(Message msg) throws MessagingException;

}
