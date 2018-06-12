package org.pmiops.workbench.mail;

import org.springframework.stereotype.Service;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Transport;

@Service
public class MailServiceImpl implements MailService {

    public void send(Message msg) throws MessagingException {
        Transport.send(msg);
    }

}
