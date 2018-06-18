package org.pmiops.workbench.mail;

import com.google.api.services.admin.directory.model.UserEmail;
import com.google.api.services.admin.directory.model.UserName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.google.CloudStorageServiceImpl;
import com.google.api.services.admin.directory.model.User;
import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.google.DirectoryServiceImpl;
import org.pmiops.workbench.mandrill.api.MandrillApi;
import org.pmiops.workbench.mandrill.ApiException;
import org.pmiops.workbench.mandrill.model.MandrillApiKeyAndMessage;
import org.pmiops.workbench.mandrill.model.MandrillMessage;
import org.pmiops.workbench.mandrill.model.MandrillMessageStatuses;
import org.pmiops.workbench.mandrill.model.RecipientAddress;
import org.pmiops.workbench.model.CreateAccountRequest;
import org.pmiops.workbench.model.InvitationVerificationRequest;
import org.pmiops.workbench.model.Profile;
import org.pmiops.workbench.test.Providers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import javax.inject.Provider;
import javax.mail.MessagingException;
import javax.validation.constraints.NotNull;

import java.util.Collections;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
public class MailServiceImplTest {

  private MailServiceImpl service;
  private MandrillMessage mandrillMessage;
  private MandrillApiKeyAndMessage apiKeyAndMessage;
  private RecipientAddress recipientAddress;
  private static final String GIVEN_NAME = "Bob";
  private static final String FAMILY_NAME = "Bobberson";
  private static final String CONTACT_EMAIL = "bob@example.com";
  private static final String PASSWORD = "secretpassword";
  private static final String PRIMARY_EMAIL = "bob@researchallofus.org";
  private static final String API_KEY = "this-is-an-api-key";
  private static final String HTML = "This is a test email";
  private static final String SUBJECT = "Subject: Test email";
  private static final String FROM_EMAIL = "donotreply@fake-research-aou.org";

  @Mock
  private WorkbenchConfig workbenchConfig;

  @Mock
  private CloudStorageServiceImpl cloudStorageService;

  @Mock
  private Provider<CloudStorageService> cloudStorageServiceProvider;

  @Mock
  private MandrillApi mandrillApi;

  @Mock
  private Provider<MandrillApi> mandrillApiProvider;

  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Before
  public void setup() {
    workbenchConfig = new WorkbenchConfig();
    workbenchConfig.mandrill = new WorkbenchConfig.MandrillConfig();
    workbenchConfig.mandrill.fromEmail = "test-donotreply@fake-research-aou.org";
    workbenchConfig.googleCloudStorageService = new WorkbenchConfig.GoogleCloudStorageServiceConfig();
    workbenchConfig.googleCloudStorageService.credentialsBucketName = "test-bucket";
    when(mandrillApiProvider.get()).thenReturn(mandrillApi);
    when(cloudStorageServiceProvider.get()).thenReturn(cloudStorageService);
    service = new MailServiceImpl(Providers.of(mandrillApi),
      Providers.of(cloudStorageService), Providers.of(workbenchConfig));
    apiKeyAndMessage = new MandrillApiKeyAndMessage();
    recipientAddress = new RecipientAddress();
    mandrillMessage = new MandrillMessage();
    recipientAddress.setEmail(CONTACT_EMAIL);
    mandrillMessage.setTo(Collections.singletonList(recipientAddress));
    mandrillMessage.setHtml(HTML);
    mandrillMessage.setSubject(SUBJECT);
    mandrillMessage.setFromEmail(FROM_EMAIL);
    apiKeyAndMessage.setKey(API_KEY);
    apiKeyAndMessage.setMessage(mandrillMessage);
  }

  @NotNull
  private User createUser() {
    return new User()
      .setPrimaryEmail(PRIMARY_EMAIL)
      .setPassword(PASSWORD)
      .setName(new UserName().setGivenName(GIVEN_NAME).setFamilyName(FAMILY_NAME))
      .setEmails(new UserEmail().setType("custom").setAddress(CONTACT_EMAIL).setCustomType("contact"))
      .setChangePasswordAtNextLogin(true);
  }

//  @Test(expected = MessagingException.class)
//  public void testSendWelcomeEmail_throwsAPIException() throws Exception {
//    when(Providers.of(cloudStorageService).get().readMandrillApiKey()).thenReturn(API_KEY);
//    when(Providers.of(workbenchConfig).get().mandrill.fromEmail).thenReturn(FROM_EMAIL);
//    when(mandrillApi.send(apiKeyAndMessage)).thenThrow(new ApiException());
//    User user = createUser();
//    service.sendWelcomeEmail(CONTACT_EMAIL, PASSWORD, user);
//  }

  @Test(expected = MessagingException.class)
  public void testSendWelcomeEmail_throwsMessagingException() throws MessagingException, ApiException {
    when(Providers.of(cloudStorageService).get().readMandrillApiKey()).thenReturn(API_KEY);
    when(Providers.of(mandrillApi).get().send(apiKeyAndMessage)).thenThrow(new ApiException());
    User user = createUser();
    service.sendWelcomeEmail(CONTACT_EMAIL, PASSWORD, user);
  }

//  @Test
//  public void testSendWelcomEmail() throws Exception {
//    when(Providers.of(cloudStorageService).get().readMandrillApiKey()).thenReturn(API_KEY);
//    when(Providers.of(workbenchConfig).get().mandrill.fromEmail).thenReturn(FROM_EMAIL);
//    when(mandrillApi.send(apiKeyAndMessage)).thenReturn(new MandrillMessageStatuses());
//    User user = createUser();
//    service.sendWelcomeEmail(CONTACT_EMAIL, PASSWORD, user);
//    verify(mandrillApi).send(apiKeyAndMessage);
//  }
}
