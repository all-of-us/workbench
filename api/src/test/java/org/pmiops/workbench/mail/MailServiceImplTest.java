package org.pmiops.workbench.mail;

import com.google.api.services.admin.directory.model.User;
import com.google.api.services.admin.directory.model.UserEmail;
import com.google.api.services.admin.directory.model.UserName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.Mock;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.google.CloudStorageServiceImpl;
import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.google.DirectoryServiceImpl;
import org.pmiops.workbench.mandrill.api.MandrillApi;
import org.pmiops.workbench.mandrill.ApiException;
import org.pmiops.workbench.mandrill.model.MandrillApiKeyAndMessage;
import org.pmiops.workbench.mandrill.model.MandrillMessage;
import org.pmiops.workbench.mandrill.model.MandrillMessageStatuses;
import org.pmiops.workbench.mandrill.model.MandrillMessageStatus;
import org.pmiops.workbench.mandrill.model.RecipientAddress;
import org.pmiops.workbench.test.Providers;

import javax.inject.Provider;
import javax.mail.MessagingException;
import javax.validation.constraints.NotNull;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MailServiceImplTest {

  private MailServiceImpl service;
  private static final String GIVEN_NAME = "Bob";
  private static final String FAMILY_NAME = "Bobberson";
  private static final String CONTACT_EMAIL = "bob@example.com";
  private static final String PASSWORD = "secretpassword";
  private static final String PRIMARY_EMAIL = "bob@researchallofus.org";
  private static final String API_KEY = "this-is-an-api-key";

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

  @Mock
  private MandrillMessageStatuses msgStatuses;

  @Mock
  private MandrillMessageStatus msgStatus;

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
    msgStatuses = new MandrillMessageStatuses();
    msgStatuses.add(msgStatus);
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

  @Test(expected = MessagingException.class)
  public void testSendWelcomeEmail_throwsAPIException() throws MessagingException, ApiException {
    when(Providers.of(cloudStorageService).get().readMandrillApiKey()).thenReturn(API_KEY);
    when(Providers.of(mandrillApi).get().send(any())).thenReturn(msgStatuses);
    when(msgStatus.getRejectReason()).thenReturn("this was rejected");
    User user = createUser();
    service.sendWelcomeEmail(CONTACT_EMAIL, PASSWORD, user);
  }

  @Test(expected = MessagingException.class)
  public void testSendWelcomeEmail_throwsMessagingException() throws MessagingException, ApiException {
    when(Providers.of(cloudStorageService).get().readMandrillApiKey()).thenReturn(API_KEY);
    when(Providers.of(mandrillApi).get().send(any())).thenThrow(new ApiException());
    User user = createUser();
    service.sendWelcomeEmail(CONTACT_EMAIL, PASSWORD, user);
  }

  @Test
  public void testSendWelcomEmail() throws MessagingException, ApiException {
    when(Providers.of(cloudStorageService).get().readMandrillApiKey()).thenReturn(API_KEY);
    when(Providers.of(mandrillApi).get().send(any())).thenReturn(msgStatuses);
    User user = createUser();
    service.sendWelcomeEmail(CONTACT_EMAIL, PASSWORD, user);
    //did not think we would want to test here for exact contents
    verify(mandrillApi).send(any(MandrillApiKeyAndMessage.class));
  }
}
