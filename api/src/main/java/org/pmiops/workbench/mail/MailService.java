package org.pmiops.workbench.mail;

import org.springframework.stereotype.Service;

import javax.mail.Message;
import javax.mail.MessagingException;

@Service
public interface MailService {

    void send(Message msg) throws MessagingException;

}
