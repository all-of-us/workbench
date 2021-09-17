package org.pmiops.workbench.mail;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import javax.mail.MessagingException;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.pmiops.workbench.SpringTest;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.google.CloudStorageClientImpl;
import org.pmiops.workbench.mandrill.ApiException;
import org.pmiops.workbench.mandrill.api.MandrillApi;
import org.pmiops.workbench.mandrill.model.MandrillApiKeyAndMessage;
import org.pmiops.workbench.mandrill.model.MandrillMessage;
import org.pmiops.workbench.mandrill.model.MandrillMessageStatus;
import org.pmiops.workbench.mandrill.model.MandrillMessageStatuses;
import org.pmiops.workbench.mandrill.model.RecipientAddress;
import org.pmiops.workbench.mandrill.model.RecipientType;
import org.pmiops.workbench.model.SendBillingSetupEmailRequest;
import org.pmiops.workbench.test.Providers;

public class MailServiceImplTest extends SpringTest {

  private MailServiceImpl service;
  private static final String CONTACT_EMAIL = "bob@example.com";
  private static final String PASSWORD = "secretpassword";
  private static final String FULL_USER_NAME = "bob@researchallofus.org";
  private static final String API_KEY = "this-is-an-api-key";
  private static final String FROM_EMAIL = "test-donotreply@fake-research-aou.org";

  private WorkbenchConfig workbenchConfig = createWorkbenchConfig();

  @Mock private CloudStorageClientImpl cloudStorageClient;
  @Mock private MandrillApi mandrillApi;
  @Mock private MandrillMessageStatus msgStatus;
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @BeforeEach
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
            Providers.of(workbenchConfig));
  }

  @Test
  public void testSendWelcomeEmail_throwsMessagingException()
      throws MessagingException, ApiException {
    when(msgStatus.getRejectReason()).thenReturn("this was rejected");
    assertThrows(
        MessagingException.class,
        () -> service.sendWelcomeEmail(CONTACT_EMAIL, PASSWORD, FULL_USER_NAME));
    verify(mandrillApi, times(1)).send(any());
  }

  @Test
  public void testSendWelcomeEmail_throwsApiException() throws MessagingException, ApiException {
    doThrow(ApiException.class).when(mandrillApi).send(any());
    assertThrows(
        MessagingException.class,
        () -> service.sendWelcomeEmail(CONTACT_EMAIL, PASSWORD, FULL_USER_NAME));
    verify(mandrillApi, times(3)).send(any());
  }

  @Test
  public void testSendWelcomeEmail_invalidEmail() throws MessagingException {
    assertThrows(
        ServerErrorException.class,
        () -> service.sendWelcomeEmail("Nota valid email", PASSWORD, FULL_USER_NAME));
  }

  @Test
  public void testSendWelcomeEmail() throws MessagingException, ApiException {
    service.sendWelcomeEmail(CONTACT_EMAIL, PASSWORD, FULL_USER_NAME);
    verify(mandrillApi, times(1)).send(any(MandrillApiKeyAndMessage.class));
  }

  @Test
  public void testSendInstructions() throws Exception {
    service.sendInstitutionUserInstructions(
        "asdf@gmail.com",
        "Ask for help at help@myinstitute.org <script>window.alert()</script>>",
        "asdf@fake-research");
    verify(mandrillApi, times(1))
        .send(
            argThat(
                got -> {
                  assertThat((((MandrillMessage) got.getMessage()).getTo()))
                      .containsExactly(
                          new RecipientAddress().email("asdf@gmail.com").type(RecipientType.TO));

                  String gotHtml = ((MandrillMessage) got.getMessage()).getHtml();
                  // tags should be escaped, email addresses shouldn't.
                  return gotHtml.contains("help@myinstitute.org")
                      && gotHtml.contains("&lt;script&gt;window.alert()&lt;/script&gt;&gt;");
                }));
  }

  @Test
  public void testSendBillingSetupEmail() throws Exception {
    DbUser user = createDbUser();
    SendBillingSetupEmailRequest request =
        new SendBillingSetupEmailRequest().institution("inst").phone("123456");
    service.sendBillingSetupEmail(user, request);
    verify(mandrillApi, times(1))
        .send(
            argThat(
                got -> {
                  List<RecipientAddress> receipts = (((MandrillMessage) got.getMessage()).getTo());
                  assertThat(receipts)
                      .containsExactly(
                          new RecipientAddress()
                              .email(user.getContactEmail())
                              .type(RecipientType.TO),
                          new RecipientAddress().email(FROM_EMAIL).type(RecipientType.CC));

                  String gotHtml = ((MandrillMessage) got.getMessage()).getHtml();
                  // tags should be escaped, email addresses shouldn't.
                  return gotHtml.contains("username@research.org")
                      && gotHtml.contains("given name family name")
                      && gotHtml.contains("user@contact.com")
                      && gotHtml.contains(
                          "Is this work NIH-funded and eligible for the STRIDES Program?: No");
                }));
  }

  @Test
  public void testSendBillingSetupEmail_withCarasoft() throws Exception {
    workbenchConfig.billing.carahsoftEmail = "test@carasoft.com";
    DbUser user = createDbUser();
    SendBillingSetupEmailRequest request =
        new SendBillingSetupEmailRequest().institution("inst").isNihFunded(true).phone("123456");
    service.sendBillingSetupEmail(user, request);
    verify(mandrillApi, times(1))
        .send(
            argThat(
                got -> {
                  List<RecipientAddress> receipts = (((MandrillMessage) got.getMessage()).getTo());
                  assertThat(receipts)
                      .containsExactly(
                          new RecipientAddress()
                              .email(user.getContactEmail())
                              .type(RecipientType.TO),
                          new RecipientAddress().email("test@carasoft.com").type(RecipientType.TO),
                          new RecipientAddress().email(FROM_EMAIL).type(RecipientType.CC));

                  String gotHtml = ((MandrillMessage) got.getMessage()).getHtml();
                  // tags should be escaped, email addresses shouldn't.
                  return gotHtml.contains("username@research.org")
                      && gotHtml.contains("given name family name")
                      && gotHtml.contains("user@contact.com")
                      && gotHtml.contains(
                          "Is this work NIH-funded and eligible for the STRIDES Program?: Yes");
                }));
  }

  @Test
  public void testSendBillingSetupEmailNotSent_featureDisabled() throws Exception {
    workbenchConfig.featureFlags.enableBillingUpgrade = false;
    DbUser user = createDbUser();
    SendBillingSetupEmailRequest request =
        new SendBillingSetupEmailRequest().institution("inst").isNihFunded(true).phone("123456");
    service.sendBillingSetupEmail(user, request);
    verifyZeroInteractions(mandrillApi);
  }

  private DbUser createDbUser() {
    DbUser user = new DbUser();
    user.setFamilyName("family name");
    user.setGivenName("given name");
    user.setContactEmail("user@contact.com");
    user.setUsername("username@research.org");
    return user;
  }

  private WorkbenchConfig createWorkbenchConfig() {
    WorkbenchConfig workbenchConfig = WorkbenchConfig.createEmptyConfig();
    workbenchConfig.mandrill.fromEmail = FROM_EMAIL;
    workbenchConfig.mandrill.sendRetries = 3;
    workbenchConfig.googleCloudStorageService.credentialsBucketName = "test-bucket";
    workbenchConfig.googleDirectoryService.gSuiteDomain = "research.org";
    workbenchConfig.admin.loginUrl = "http://localhost:4200/";
    workbenchConfig.featureFlags.enableBillingUpgrade = true;
    return workbenchConfig;
  }
}
