package org.pmiops.workbench.mail;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.services.directory.model.User;
import com.google.api.services.directory.model.UserEmail;
import com.google.api.services.directory.model.UserName;
import javax.mail.MessagingException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.google.CloudStorageClientImpl;
import org.pmiops.workbench.mandrill.ApiException;
import org.pmiops.workbench.mandrill.api.MandrillApi;
import org.pmiops.workbench.mandrill.model.MandrillApiKeyAndMessage;
import org.pmiops.workbench.mandrill.model.MandrillMessage;
import org.pmiops.workbench.mandrill.model.MandrillMessageStatus;
import org.pmiops.workbench.mandrill.model.MandrillMessageStatuses;
import org.pmiops.workbench.test.Providers;

public class MailServiceImplTest {

  private MailServiceImpl service;
  private static final String GIVEN_NAME = "Bob";
  private static final String FAMILY_NAME = "Bobberson";
  private static final String CONTACT_EMAIL = "bob@example.com";
  private static final String PASSWORD = "secretpassword";
  private static final String PRIMARY_EMAIL = "bob@researchallofus.org";
  private static final String API_KEY = "this-is-an-api-key";

  @Mock private CloudStorageClientImpl cloudStorageClient;
  @Mock private MandrillApi mandrillApi;
  @Mock private MandrillMessageStatus msgStatus;
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Before
  public void setUp() throws ApiException {
    MandrillMessageStatuses msgStatuses = new MandrillMessageStatuses();
    msgStatuses.add(msgStatus);
    when(mandrillApi.send(any())).thenReturn(msgStatuses);
    when(cloudStorageClient.readMandrillApiKey()).thenReturn(API_KEY);
    when(cloudStorageClient.getImageUrl(any())).thenReturn("test_img");

    service =
        new MailServiceImpl(
            Providers.of(mandrillApi),
            Providers.of(cloudStorageClient),
            Providers.of(createWorkbenchConfig()));
  }

  @Test(expected = MessagingException.class)
  public void testSendWelcomeEmail_throwsMessagingException()
      throws MessagingException, ApiException {
    when(msgStatus.getRejectReason()).thenReturn("this was rejected");
    User user = createUser();
    service.sendWelcomeEmail(CONTACT_EMAIL, PASSWORD, user);
    verify(mandrillApi, times(1)).send(any());
  }

  @Test(expected = MessagingException.class)
  public void testSendWelcomeEmail_throwsApiException() throws MessagingException, ApiException {
    doThrow(ApiException.class).when(mandrillApi).send(any());
    User user = createUser();
    service.sendWelcomeEmail(CONTACT_EMAIL, PASSWORD, user);
    verify(mandrillApi, times(3)).send(any());
  }

  @Test(expected = MessagingException.class)
  public void testSendWelcomeEmail_invalidEmail() throws MessagingException {
    User user = createUser();
    service.sendWelcomeEmail("Nota valid email", PASSWORD, user);
  }

  @Test
  public void testSendWelcomeEmail() throws MessagingException, ApiException {
    User user = createUser();
    service.sendWelcomeEmail(CONTACT_EMAIL, PASSWORD, user);
    verify(mandrillApi, times(1)).send(any(MandrillApiKeyAndMessage.class));
  }

  @Test
  public void testSendInstructions() throws Exception {
    service.sendInstitutionUserInstructions(
        "asdf@gmail.com", "Ask for help at help@myinstitute.org <script>window.alert()</script>>");
    verify(mandrillApi, times(1))
        .send(
            argThat(
                got -> {
                  String gotHtml = ((MandrillMessage) got.getMessage()).getHtml();
                  // tags should be escaped, email addresses shouldn't.
                  return gotHtml.contains("help@myinstitute.org")
                      && gotHtml.contains("&lt;script&gt;window.alert()&lt;/script&gt;&gt;");
                }));
  }

  private User createUser() {
    return new User()
        .setPrimaryEmail(PRIMARY_EMAIL)
        .setPassword(PASSWORD)
        .setName(new UserName().setGivenName(GIVEN_NAME).setFamilyName(FAMILY_NAME))
        .setEmails(
            new UserEmail().setType("custom").setAddress(CONTACT_EMAIL).setCustomType("contact"))
        .setChangePasswordAtNextLogin(true);
  }

  private WorkbenchConfig createWorkbenchConfig() {
    WorkbenchConfig workbenchConfig = WorkbenchConfig.createEmptyConfig();
    workbenchConfig.mandrill.fromEmail = "test-donotreply@fake-research-aou.org";
    workbenchConfig.mandrill.sendRetries = 3;
    workbenchConfig.googleCloudStorageService.credentialsBucketName = "test-bucket";
    workbenchConfig.admin.loginUrl = "http://localhost:4200/";
    return workbenchConfig;
  }
}
