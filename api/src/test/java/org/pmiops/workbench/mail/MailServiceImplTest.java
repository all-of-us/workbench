package org.pmiops.workbench.mail;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import javax.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.config.WorkbenchConfig.EgressAlertRemediationPolicy;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.exfiltration.EgressRemediationAction;
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
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@Import(FakeClockConfiguration.class)
@SpringJUnitConfig
public class MailServiceImplTest {

  private MailServiceImpl service;
  private static final String CONTACT_EMAIL = "bob@example.com";
  private static final String PASSWORD = "secretpassword";
  private static final String FULL_USER_NAME = "bob@researchallofus.org";
  private static final String API_KEY = "this-is-an-api-key";
  private static final String FROM_EMAIL = "test-donotreply@fake-research-aou.org";

  private WorkbenchConfig workbenchConfig = createWorkbenchConfig();

  @Captor private ArgumentCaptor<MandrillApiKeyAndMessage> mandrillCaptor;

  @Mock private CloudStorageClientImpl cloudStorageClient;
  @Mock private MandrillApi mandrillApi;
  @Mock private MandrillMessageStatus msgStatus;

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
  public void testSendWelcomeEmail_throwsMessagingException() throws ApiException {
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
    verify(mandrillApi, times(1)).send(mandrillCaptor.capture());

    MandrillMessage gotMessage = (MandrillMessage) mandrillCaptor.getValue().getMessage();
    assertThat(gotMessage.getTo())
        .containsExactly(new RecipientAddress().email("asdf@gmail.com").type(RecipientType.TO));

    String gotHtml = gotMessage.getHtml();
    // tags should be escaped, email addresses shouldn't.
    assertThat(gotHtml).contains("help@myinstitute.org");
    assertThat(gotHtml).contains("&lt;script&gt;window.alert()&lt;/script&gt;&gt;");
  }

  @Test
  public void testSendBillingSetupEmail() throws Exception {
    DbUser user = createDbUser();
    SendBillingSetupEmailRequest request =
        new SendBillingSetupEmailRequest().institution("inst").phone("123456");
    service.sendBillingSetupEmail(user, request);

    verify(mandrillApi, times(1)).send(mandrillCaptor.capture());

    MandrillMessage gotMessage = (MandrillMessage) mandrillCaptor.getValue().getMessage();

    assertThat(gotMessage.getTo())
        .containsExactly(
            new RecipientAddress().email(user.getContactEmail()).type(RecipientType.TO),
            new RecipientAddress().email(FROM_EMAIL).type(RecipientType.CC));

    String gotHtml = gotMessage.getHtml();
    assertThat(gotHtml).contains("username@research.org");
    assertThat(gotHtml).contains("given name family name");
    assertThat(gotHtml).contains("user@contact.com");
    assertThat(gotHtml)
        .contains("Is this work NIH-funded and eligible for the STRIDES Program?: No");
  }

  @Test
  public void testSendBillingSetupEmail_withCarasoft() throws Exception {
    workbenchConfig.billing.carahsoftEmail = "test@carasoft.com";
    DbUser user = createDbUser();
    SendBillingSetupEmailRequest request =
        new SendBillingSetupEmailRequest().institution("inst").isNihFunded(true).phone("123456");
    service.sendBillingSetupEmail(user, request);

    verify(mandrillApi, times(1)).send(mandrillCaptor.capture());

    MandrillMessage gotMessage = (MandrillMessage) mandrillCaptor.getValue().getMessage();

    assertThat(gotMessage.getTo())
        .containsExactly(
            new RecipientAddress().email(user.getContactEmail()).type(RecipientType.TO),
            new RecipientAddress().email("test@carasoft.com").type(RecipientType.TO),
            new RecipientAddress().email(FROM_EMAIL).type(RecipientType.CC));

    String gotHtml = gotMessage.getHtml();
    assertThat(gotHtml).contains("username@research.org");
    assertThat(gotHtml).contains("given name family name");
    assertThat(gotHtml).contains("user@contact.com");
    assertThat(gotHtml)
        .contains("Is this work NIH-funded and eligible for the STRIDES Program?: Yes");
  }

  @Test
  public void testSendEgressRemediationEmail_suspendCompute() throws Exception {
    workbenchConfig.egressAlertRemediationPolicy.notifyFromEmail = "egress@aou.com";
    DbUser user = createDbUser();
    service.sendEgressRemediationEmail(user, EgressRemediationAction.SUSPEND_COMPUTE);

    verify(mandrillApi, times(1)).send(mandrillCaptor.capture());
    MandrillApiKeyAndMessage got = mandrillCaptor.getValue();
    List<RecipientAddress> receipts = (((MandrillMessage) got.getMessage()).getTo());
    assertThat(receipts)
        .containsExactly(
            new RecipientAddress().email(user.getContactEmail()).type(RecipientType.TO),
            new RecipientAddress().email("egress@aou.com").type(RecipientType.CC));

    String gotHtml = ((MandrillMessage) got.getMessage()).getHtml();
    assertThat(gotHtml).contains("compute access has been temporarily suspended");
    assertThat(gotHtml).doesNotContain("${");
  }

  @Test
  public void testSendEgressRemediationEmail_disableUser() throws Exception {
    workbenchConfig.egressAlertRemediationPolicy.notifyFromEmail = "egress@aou.com";
    DbUser user = createDbUser();
    service.sendEgressRemediationEmail(user, EgressRemediationAction.DISABLE_USER);

    verify(mandrillApi, times(1)).send(mandrillCaptor.capture());
    MandrillApiKeyAndMessage got = mandrillCaptor.getValue();

    String gotHtml = ((MandrillMessage) got.getMessage()).getHtml();
    assertThat(gotHtml).contains("account has been disabled");
    assertThat(gotHtml).doesNotContain("${");
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
    workbenchConfig.egressAlertRemediationPolicy = new EgressAlertRemediationPolicy();
    return workbenchConfig;
  }
}
