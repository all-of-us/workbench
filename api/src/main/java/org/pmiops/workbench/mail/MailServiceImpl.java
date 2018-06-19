package org.pmiops.workbench.mail;

import com.google.api.services.admin.directory.model.User;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.mandrill.api.MandrillApi;
import org.pmiops.workbench.mandrill.ApiException;
import org.pmiops.workbench.mandrill.model.MandrillApiKeyAndMessage;
import org.pmiops.workbench.mandrill.model.MandrillMessage;
import org.pmiops.workbench.mandrill.model.MandrillMessageStatuses;
import org.pmiops.workbench.mandrill.model.MandrillMessageStatus;
import org.pmiops.workbench.mandrill.model.RecipientAddress;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import javax.inject.Provider;
import java.util.Collections;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Transport;

@Service
public class MailServiceImpl implements MailService {

    private final Provider<MandrillApi> mandrillApiProvider;
    private final Provider<CloudStorageService> cloudStorageServiceProvider;
    private Provider<WorkbenchConfig> workbenchConfigProvider;

    @Autowired
    public MailServiceImpl(Provider<MandrillApi> mandrillApiProvider,
                           Provider<CloudStorageService> cloudStorageServiceProvider,
                           Provider<WorkbenchConfig> workbenchConfigProvider) {
        this.mandrillApiProvider = mandrillApiProvider;
        this.cloudStorageServiceProvider = cloudStorageServiceProvider;
        this.workbenchConfigProvider = workbenchConfigProvider;
    }

    public void send(Message msg) throws MessagingException {
        Transport.send(msg);
    }

    public void sendWelcomeEmail(String contactEmail, String password, User user) throws MessagingException {
        String apiKey = cloudStorageServiceProvider.get().readMandrillApiKey();
        MandrillApiKeyAndMessage keyAndMessage = new MandrillApiKeyAndMessage();
        keyAndMessage.setKey(apiKey);
        MandrillMessage msg = buildWelcomeMessage(contactEmail, password, user);
        keyAndMessage.setMessage(msg);
        try {
            MandrillMessageStatuses msgStatuses = mandrillApiProvider.get().send(keyAndMessage);
            for (MandrillMessageStatus msgStatus : msgStatuses) {
                if (msgStatus.getRejectReason() != null) {
                  throw new MessagingException(msgStatus.getRejectReason());
                }
            }
        } catch (ApiException e) {
            throw new MessagingException("Sending email failed.");
        } catch (MessagingException e) {
            throw new MessagingException("Sending email failed with message: " + e.getMessage());
        }
    }

    private MandrillMessage buildWelcomeMessage(String contactEmail, String password, User user) {
        MandrillMessage msg = new MandrillMessage();
        RecipientAddress toAddress = new RecipientAddress();
        toAddress.setEmail(contactEmail);
        msg.setTo(Collections.singletonList(toAddress));
        String msgBody = "Your new account is: " + user.getPrimaryEmail() +
          "\nThe password for your new account is: " + password;
        msg.setHtml(msgBody);
        msg.setSubject("Your new All of Us Account");
        msg.setFromEmail(workbenchConfigProvider.get().mandrill.fromEmail);
        return msg;
    }

}
