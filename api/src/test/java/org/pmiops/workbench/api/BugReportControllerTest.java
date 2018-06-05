package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.appengine.api.mail.MailServicePb;
import com.google.appengine.api.mail.dev.LocalMailService;
import com.google.appengine.tools.development.ApiProxyLocal;
import com.google.appengine.tools.development.testing.LocalMailServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.apphosting.api.ApiProxy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import javax.inject.Provider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.model.BillingProjectStatus;
import org.pmiops.workbench.model.BugReport;
import org.pmiops.workbench.notebooks.ApiException;
import org.pmiops.workbench.notebooks.api.JupyterApi;
import org.pmiops.workbench.notebooks.model.JupyterContents;
import org.pmiops.workbench.test.FakeClock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class BugReportControllerTest {
  private static final Instant NOW = Instant.now();
  private static final FakeClock CLOCK = new FakeClock(NOW, ZoneId.systemDefault());
  private static final String FC_PROJECT_ID = "fc-project";
  private static final String USER_EMAIL = "falco@lombardi.com";

  private static final JupyterContents TEST_CONTENTS =
      new JupyterContents().content("log contents");

  @TestConfiguration
  @Import({BugReportController.class})
  @MockBean({JupyterApi.class})
  static class Configuration {
    @Bean
    Clock clock() {
      return CLOCK;
    }

    @Bean
    WorkbenchConfig workbenchConfig() {
      WorkbenchConfig config = new WorkbenchConfig();
      config.admin = new WorkbenchConfig.AdminConfig();
      config.admin.supportGroup = "support@asdf.com";
      config.admin.verifiedSendingAddress = "sender@asdf.com";
      return config;
    }

    @Bean
    User user() {
      // Allows for wiring of the initial Provider<User>; actual mocking of the
      // user is achieved via setUserProvider().
      return null;
    }
  }

  @Mock
  Provider<User> userProvider;
  @Autowired
  JupyterApi jupyterApi;
  @Autowired
  BugReportController bugReportController;

  private LocalServiceTestHelper gaeHelper =
      new LocalServiceTestHelper(new LocalMailServiceTestConfig());
  private LocalMailService mailService;

  @Before
  public void setUp() {
    gaeHelper.setUp();
    User user = new User();
    user.setEmail(USER_EMAIL);
    user.setUserId(123L);
    user.setFreeTierBillingProjectName(FC_PROJECT_ID);
    user.setFreeTierBillingProjectStatus(BillingProjectStatus.READY);
    user.setDisabled(false);
    when(userProvider.get()).thenReturn(user);
    bugReportController.setUserProvider(userProvider);

    ApiProxyLocal proxy = (ApiProxyLocal) ApiProxy.getDelegate();
    mailService = (LocalMailService) proxy.getService(LocalMailService.PACKAGE);

    CLOCK.setInstant(NOW);
  }

  @After
  public void tearDown() {
    gaeHelper.tearDown();
  }

  @Test
  public void testSendBugReport() throws Exception {
    bugReportController.sendBugReport(
        new BugReport()
          .contactEmail(USER_EMAIL)
          .includeNotebookLogs(false)
          .reproSteps("press button")
          .shortDescription("bug"));
    assertThat(mailService.getSentMessages().size()).isEqualTo(1);
    verify(jupyterApi, never()).getRootContents(any(), any(), any(), any(), any(), any());
  }

  @Test
  public void testSendBugReportWithNotebooks() throws Exception {
    when(jupyterApi.getRootContents(any(), any(), any(), any(), any(), any()))
        .thenReturn(TEST_CONTENTS);
    bugReportController.sendBugReport(
        new BugReport()
          .contactEmail(USER_EMAIL)
          .includeNotebookLogs(true)
          .reproSteps("press button")
          .shortDescription("bug"));

    assertThat(mailService.getSentMessages().size()).isEqualTo(1);
    MailServicePb.MailMessage msg = mailService.getSentMessages().get(0);
    assertThat(msg.attachments().size()).isEqualTo(3);
    verify(jupyterApi).getRootContents(
        eq(FC_PROJECT_ID), any(), eq("delocalization.log"), any(), any(), any());
    verify(jupyterApi).getRootContents(
        eq(FC_PROJECT_ID), any(), eq("localization.log"), any(), any(), any());
    verify(jupyterApi).getRootContents(
        eq(FC_PROJECT_ID), any(), eq("jupyter.log"), any(), any(), any());
  }

  @Test
  public void testSendBugReportWithNotebookErrors() throws Exception {
    when(jupyterApi.getRootContents(any(), any(), any(), any(), any(), any()))
        .thenReturn(TEST_CONTENTS);
    when(jupyterApi.getRootContents(any(), any(), eq("jupyter.log"), any(), any(), any()))
        .thenThrow(new ApiException(404, "not found"));
    bugReportController.sendBugReport(
        new BugReport()
          .contactEmail(USER_EMAIL)
          .includeNotebookLogs(true)
          .reproSteps("press button")
          .shortDescription("bug"));

    assertThat(mailService.getSentMessages().size()).isEqualTo(1);
    MailServicePb.MailMessage msg = mailService.getSentMessages().get(0);
    assertThat(msg.attachments().size()).isEqualTo(2);
  }
}
