package org.pmiops.workbench.mail;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.pmiops.workbench.mail.MailServiceImpl.ATTACHED_DISK_STATUS;
import static org.pmiops.workbench.mail.MailServiceImpl.DETACHED_DISK_STATUS;

import com.google.common.collect.ImmutableList;
import jakarta.mail.MessagingException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.config.WorkbenchConfig.EgressAlertRemediationPolicy;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.exfiltration.EgressRemediationAction;
import org.pmiops.workbench.google.CloudStorageClient;
import org.pmiops.workbench.mandrill.ApiException;
import org.pmiops.workbench.mandrill.api.MandrillApi;
import org.pmiops.workbench.mandrill.model.MandrillApiKeyAndMessage;
import org.pmiops.workbench.mandrill.model.MandrillMessage;
import org.pmiops.workbench.mandrill.model.MandrillMessageStatus;
import org.pmiops.workbench.mandrill.model.MandrillMessageStatuses;
import org.pmiops.workbench.mandrill.model.RecipientAddress;
import org.pmiops.workbench.mandrill.model.RecipientType;
import org.pmiops.workbench.model.Disk;
import org.pmiops.workbench.model.DiskType;
import org.pmiops.workbench.model.SendBillingSetupEmailRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@Import(FakeClockConfiguration.class)
@SpringJUnitConfig
public class MailServiceTest {
  private static final String CONTACT_EMAIL = "bob@example.com";
  private static final String INVALID_EMAIL = "Nota valid email";
  private static final String PASSWORD = "secretpassword";
  private static final String INSTITUTION_NAME = "BROAD Institute";
  private static final String FULL_USER_NAME = "bob@researchallofus.org";
  private static final String API_KEY = "this-is-an-api-key";
  private static final String FROM_EMAIL = "test-donotreply@fake-research-aou.org";

  private static WorkbenchConfig workbenchConfig;

  @TestConfiguration
  @Import({FakeClockConfiguration.class, MailServiceImpl.class})
  @MockBean({CloudStorageClient.class, MandrillApi.class})
  static class Configuration {
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public WorkbenchConfig workbenchConfig() {
      return workbenchConfig;
    }
  }

  @Captor private ArgumentCaptor<MandrillApiKeyAndMessage> mandrillCaptor;

  @Autowired private CloudStorageClient mockCloudStorageClient;
  @Autowired private MandrillApi mockMandrillApi;

  @Autowired private MailService mailService;

  @BeforeEach
  public void setUp() throws ApiException {
    workbenchConfig = createWorkbenchConfig();

    MandrillMessageStatuses msgStatuses = new MandrillMessageStatuses();
    msgStatuses.add(new MandrillMessageStatus());
    when(mockMandrillApi.send(any())).thenReturn(msgStatuses);
    when(mockCloudStorageClient.readMandrillApiKey()).thenReturn(API_KEY);
    when(mockCloudStorageClient.getImageUrl(any())).thenReturn("test_img");
  }

  @Test
  public void testSendWelcomeEmail_throwsMessagingException() throws ApiException {
    MandrillMessageStatuses msgStatuses = new MandrillMessageStatuses();
    msgStatuses.add(new MandrillMessageStatus().rejectReason("this was rejected"));
    when(mockMandrillApi.send(any())).thenReturn(msgStatuses);
    assertThrows(
        MessagingException.class,
        () -> mailService.sendWelcomeEmail(createDbUser(), PASSWORD, INSTITUTION_NAME));
    verify(mockMandrillApi, times(1)).send(any());
  }

  @Test
  public void testSendWelcomeEmail_throwsApiException() throws ApiException {
    doThrow(ApiException.class).when(mockMandrillApi).send(any());
    assertThrows(
        MessagingException.class,
        () -> mailService.sendWelcomeEmail(createDbUser(), PASSWORD, INSTITUTION_NAME));
    verify(mockMandrillApi, times(3)).send(any());
  }

  @Test
  public void testSendWelcomeEmail_invalidEmail_RtAndCt() {
    DbUser user = createDbUser().setContactEmail(INVALID_EMAIL);
    ServerErrorException exception =
        assertThrows(
            ServerErrorException.class,
            () -> mailService.sendWelcomeEmail(user, PASSWORD, INSTITUTION_NAME));
    assertThat(exception.getMessage()).isEqualTo("Email: Nota valid email is invalid.");
  }

  @Test
  public void testSendWelcomeEmail_invalidEmail_OnlyRt() throws MessagingException {
    DbUser user = createDbUser().setContactEmail(INVALID_EMAIL);
    assertThrows(
        ServerErrorException.class,
        () -> mailService.sendWelcomeEmail(user, PASSWORD, INSTITUTION_NAME));
  }

  @Test
  public void testSendWelcomeEmail_invalidEmail_NoRtOrCt() throws MessagingException {
    DbUser user = createDbUser().setContactEmail(INVALID_EMAIL);
    assertThrows(
        ServerErrorException.class,
        () -> mailService.sendWelcomeEmail(user, PASSWORD, INSTITUTION_NAME));
  }

  @Test
  public void testSendWelcomeEmailRTAndCT() throws MessagingException, ApiException {
    mailService.sendWelcomeEmail(createDbUser(), PASSWORD, INSTITUTION_NAME);
    verify(mockMandrillApi, times(1)).send(any(MandrillApiKeyAndMessage.class));
  }

  @Test
  public void testSendWelcomeEmailOnlyRT() throws MessagingException, ApiException {
    mailService.sendWelcomeEmail(createDbUser(), PASSWORD, INSTITUTION_NAME);
    verify(mockMandrillApi, times(1)).send(any(MandrillApiKeyAndMessage.class));
  }

  @Test
  public void testSendWelcomeEmailNoRtAndCt() throws MessagingException, ApiException {
    mailService.sendWelcomeEmail(createDbUser(), PASSWORD, INSTITUTION_NAME);
    verify(mockMandrillApi, times(1)).send(any(MandrillApiKeyAndMessage.class));
  }

  @Test
  public void testSendInstructions() throws Exception {
    mailService.sendInstitutionUserInstructions(
        "asdf@gmail.com",
        "Ask for help at help@myinstitute.org <script>window.alert()</script>>",
        "asdf@fake-research");
    verify(mockMandrillApi, times(1)).send(mandrillCaptor.capture());

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
    mailService.sendBillingSetupEmail(user, request);

    verify(mockMandrillApi, times(1)).send(mandrillCaptor.capture());

    MandrillMessage gotMessage = (MandrillMessage) mandrillCaptor.getValue().getMessage();

    assertThat(gotMessage.getTo())
        .containsExactly(
            new RecipientAddress().email(user.getContactEmail()).type(RecipientType.TO),
            new RecipientAddress().email(FROM_EMAIL).type(RecipientType.CC));

    String gotHtml = gotMessage.getHtml();
    assertThat(gotHtml).contains(FULL_USER_NAME);
    assertThat(gotHtml).contains("given name family name");
    assertThat(gotHtml).contains(CONTACT_EMAIL);
    assertThat(gotHtml)
        .contains("Is this work NIH-funded and eligible for the STRIDES Program?: No");
  }

  @Test
  public void testSendBillingSetupEmail_withCarasoft() throws Exception {
    workbenchConfig.billing.carahsoftEmail = "test@carasoft.com";
    DbUser user = createDbUser();
    SendBillingSetupEmailRequest request =
        new SendBillingSetupEmailRequest().institution("inst").nihFunded(true).phone("123456");
    mailService.sendBillingSetupEmail(user, request);

    verify(mockMandrillApi, times(1)).send(mandrillCaptor.capture());

    MandrillMessage gotMessage = (MandrillMessage) mandrillCaptor.getValue().getMessage();

    assertThat(gotMessage.getTo())
        .containsExactly(
            new RecipientAddress().email(user.getContactEmail()).type(RecipientType.TO),
            new RecipientAddress().email("test@carasoft.com").type(RecipientType.TO),
            new RecipientAddress().email(FROM_EMAIL).type(RecipientType.CC));

    String gotHtml = gotMessage.getHtml();
    assertThat(gotHtml).contains(FULL_USER_NAME);
    assertThat(gotHtml).contains("given name family name");
    assertThat(gotHtml).contains(CONTACT_EMAIL);
    assertThat(gotHtml)
        .contains("Is this work NIH-funded and eligible for the STRIDES Program?: Yes");
  }

  @Test
  public void testSendEgressRemediationEmail_suspendCompute() throws Exception {
    workbenchConfig.egressAlertRemediationPolicy.notifyFromEmail = "egress@aou.com";
    DbUser user = createDbUser();
    mailService.sendEgressRemediationEmail(
        user, EgressRemediationAction.SUSPEND_COMPUTE, "Jupyter");

    verify(mockMandrillApi, times(1)).send(mandrillCaptor.capture());
    MandrillApiKeyAndMessage got = mandrillCaptor.getValue();

    assertThat(((MandrillMessage) got.getMessage()).getFromEmail()).isEqualTo("egress@aou.com");
    List<RecipientAddress> receipts = ((MandrillMessage) got.getMessage()).getTo();
    assertThat(receipts)
        .containsExactly(
            new RecipientAddress().email(user.getContactEmail()).type(RecipientType.TO));

    String gotHtml = ((MandrillMessage) got.getMessage()).getHtml();
    assertThat(gotHtml).contains("temporarily suspended");
    assertThat(gotHtml).contains("when using the <b>Jupyter</b> application");
    assertThat(gotHtml).doesNotContain("${");
  }

  @Test
  public void testSendEgressRemediationEmail_disableUser() throws Exception {
    workbenchConfig.egressAlertRemediationPolicy.notifyFromEmail = "egress@aou.com";
    workbenchConfig.egressAlertRemediationPolicy.notifyCcEmails =
        ImmutableList.of("egress-cc@aou.com");
    DbUser user = createDbUser();
    mailService.sendEgressRemediationEmail(user, EgressRemediationAction.DISABLE_USER, "Jupyter");

    verify(mockMandrillApi, times(1)).send(mandrillCaptor.capture());
    MandrillApiKeyAndMessage got = mandrillCaptor.getValue();

    List<RecipientAddress> receipts = ((MandrillMessage) got.getMessage()).getTo();
    assertThat(receipts)
        .containsExactly(
            new RecipientAddress().email(user.getContactEmail()).type(RecipientType.TO),
            new RecipientAddress().email("egress-cc@aou.com").type(RecipientType.CC));

    String gotHtml = ((MandrillMessage) got.getMessage()).getHtml();
    assertThat(gotHtml).contains("will remain disabled");
    assertThat(gotHtml).contains("when using the <b>Jupyter</b> application");
    assertThat(gotHtml).doesNotContain("${");
  }

  @Test
  public void testSendEgressRemediationEmail_vwbEgress() throws Exception {
    workbenchConfig.egressAlertRemediationPolicy.notifyFromEmail = "egress@aou.com";
    DbUser user = createDbUser();
    mailService.sendEgressRemediationEmailForVwb(user, EgressRemediationAction.SUSPEND_COMPUTE);

    verify(mockMandrillApi, times(1)).send(mandrillCaptor.capture());
    MandrillApiKeyAndMessage got = mandrillCaptor.getValue();

    assertThat(((MandrillMessage) got.getMessage()).getFromEmail()).isEqualTo("egress@aou.com");
    List<RecipientAddress> receipts = ((MandrillMessage) got.getMessage()).getTo();
    assertThat(receipts)
        .containsExactly(
            new RecipientAddress().email(user.getContactEmail()).type(RecipientType.TO));

    String gotHtml = ((MandrillMessage) got.getMessage()).getHtml();
    assertThat(gotHtml).contains("temporarily suspended");
    assertThat(gotHtml).doesNotContain("when using the <b>Jupyter</b> application");
    assertThat(gotHtml).doesNotContain("${");
  }

  @Test
  public void testAlertUsersUnusedDiskWarning_attached() throws Exception {
    DbUser user = createDbUser();
    mailService.alertUsersUnusedDiskWarningThreshold(
        Collections.singletonList(user),
        new DbWorkspace().setName("my workspace").setCreator(user),
        new Disk()
            .diskType(DiskType.SSD)
            .gceRuntime(true)
            .size(123)
            .createdDate(
                FakeClockConfiguration.NOW.toInstant().minus(Duration.ofDays(20)).toString())
            .creator(user.getUsername()),
        true,
        14,
        20.0);

    verify(mockMandrillApi, times(1)).send(mandrillCaptor.capture());
    MandrillApiKeyAndMessage got = mandrillCaptor.getValue();

    List<RecipientAddress> bcc = ((MandrillMessage) got.getMessage()).getTo();
    assertThat(bcc)
        .containsExactly(
            new RecipientAddress().email(user.getContactEmail()).type(RecipientType.BCC));

    String gotHtml = ((MandrillMessage) got.getMessage()).getHtml();
    assertThat(gotHtml).contains("123 GB");
    assertThat(gotHtml).contains("$20.91 per month");
    assertThat(gotHtml)
        .contains(String.format("%s's initial credits ($20.00 remaining)", FULL_USER_NAME));
    assertThat(gotHtml).contains("Jupyter");
    assertThat(gotHtml).contains(ATTACHED_DISK_STATUS);
    assertThat(gotHtml).doesNotContain("${");
  }

  @Test
  public void testAlertUsersUnusedDiskWarning_detached() throws Exception {
    DbUser user = createDbUser();
    mailService.alertUsersUnusedDiskWarningThreshold(
        Collections.singletonList(user),
        new DbWorkspace().setName("my workspace").setCreator(user),
        new Disk()
            .diskType(DiskType.SSD)
            .gceRuntime(true)
            .size(123)
            .createdDate(
                FakeClockConfiguration.NOW.toInstant().minus(Duration.ofDays(20)).toString())
            .creator(user.getUsername()),
        false,
        14,
        20.0);

    verify(mockMandrillApi, times(1)).send(mandrillCaptor.capture());
    MandrillApiKeyAndMessage got = mandrillCaptor.getValue();

    List<RecipientAddress> bcc = ((MandrillMessage) got.getMessage()).getTo();
    assertThat(bcc)
        .containsExactly(
            new RecipientAddress().email(user.getContactEmail()).type(RecipientType.BCC));

    String gotHtml = ((MandrillMessage) got.getMessage()).getHtml();
    assertThat(gotHtml).contains("123 GB");
    assertThat(gotHtml).contains("$20.91 per month");
    assertThat(gotHtml)
        .contains(String.format("%s's initial credits ($20.00 remaining)", FULL_USER_NAME));
    assertThat(gotHtml).contains("Jupyter");
    assertThat(gotHtml).contains(DETACHED_DISK_STATUS);
    assertThat(gotHtml).doesNotContain("${");
  }

  @Test
  public void testSendNewUserSatisfactionSurveyEmail() throws Exception {
    DbUser user = createDbUser();
    String surveyLink = "example.com?survey_code=123";
    mailService.sendNewUserSatisfactionSurveyEmail(user, surveyLink);

    verify(mockMandrillApi, times(1)).send(mandrillCaptor.capture());

    MandrillMessage gotMessage = (MandrillMessage) mandrillCaptor.getValue().getMessage();

    assertThat(gotMessage.getTo())
        .containsExactly(
            new RecipientAddress().email(user.getContactEmail()).type(RecipientType.TO));

    String gotHtml = gotMessage.getHtml();
    assertThat(gotHtml).contains(surveyLink);
    assertThat(gotHtml).contains("Please take two minutes to rate your satisfaction");
  }

  private DbUser createDbUser() {
    DbUser user = new DbUser();
    user.setFamilyName("family name");
    user.setGivenName("given name");
    user.setContactEmail(CONTACT_EMAIL);
    user.setUsername(FULL_USER_NAME);
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
