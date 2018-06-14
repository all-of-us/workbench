package org.pmiops.workbench.mail;

import com.google.api.services.admin.directory.model.User;
import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.mandrill.api.MandrillApi;
import org.pmiops.workbench.mandrill.ApiException;
import org.pmiops.workbench.mandrill.model.MandrillApiKeyAndMessage;
import org.pmiops.workbench.mandrill.model.MandrillMessage;
import org.pmiops.workbench.mandrill.model.MandrillMessageStatuses;
import org.pmiops.workbench.mandrill.model.RecipientAddress;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import javax.inject.Provider;
import java.util.Arrays;
import org.springframework.stereotype.Service;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Transport;

@Service
public class MailServiceImpl implements MailService {

    private final Provider<MandrillApi> mandrillApiProvider;
    private final Provider<CloudStorageService> cloudStorageServiceProvider;

    @Autowired
    public MailServiceImpl(Provider<MandrillApi> mandrillApiProvider,
                               Provider<CloudStorageService> cloudStorageServiceProvider) {
        this.mandrillApiProvider = mandrillApiProvider;
        this.cloudStorageServiceProvider = cloudStorageServiceProvider;
    }

    public void send(Message msg) throws MessagingException {
        Transport.send(msg);
    }

    public MandrillMessageStatuses sendEmail(String contactEmail, String password, User user) throws MessagingException {
        MandrillApi mandrillApi = mandrillApiProvider.get();
        String apiKey = cloudStorageServiceProvider.get().readMandrillApiKey();
        MandrillApiKeyAndMessage keyAndMessage = new MandrillApiKeyAndMessage();
        keyAndMessage.setKey(apiKey);
        MandrillMessage msg = buildMandrillMessage(contactEmail, password, user);
        keyAndMessage.setMessage(msg);
        try {
            MandrillMessageStatuses msgStatuses = mandrillApi.send(keyAndMessage);
            return msgStatuses;
        } catch (ApiException e){
            throw new MessagingException("Sending email failed.");
        }
    }

    private MandrillMessage buildMandrillMessage(String contactEmail, String password, User user) {
        MandrillMessage msg = new MandrillMessage();
        RecipientAddress toAddress = new RecipientAddress();
        toAddress.setEmail(contactEmail);
        msg.setTo(Arrays.asList(toAddress));
        String msgBody = "Your new account is: " + user.getPrimaryEmail() +
          "\nThe password for your new account is: " + password;
        msg.setHtml(msgBody);
        msg.setSubject("Your new All of Us Account");
        msg.setFromEmail(cloudStorageServiceProvider.get().readMandrillFromEmail());
        return msg;
    }

}
