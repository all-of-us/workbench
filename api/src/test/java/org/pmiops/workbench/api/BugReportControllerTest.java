package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.inject.Provider;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;

import org.json.JSONObject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.jira.JiraService;
import org.pmiops.workbench.mail.MailService;
import org.pmiops.workbench.model.BillingProjectStatus;
import org.pmiops.workbench.model.BugReport;
import org.pmiops.workbench.notebooks.ApiException;
import org.pmiops.workbench.notebooks.api.JupyterApi;
import org.pmiops.workbench.notebooks.model.JupyterContents;
import org.pmiops.workbench.test.Providers;
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

import java.util.ArrayList;
import java.util.List;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class BugReportControllerTest {
  private static final String FC_PROJECT_ID = "fc-project";
  private static final String USER_EMAIL = "falco@lombardi.com";

  private static final JupyterContents TEST_CONTENTS =
      new JupyterContents().content("log contents");
  private List<Message> sentMessages = new ArrayList<>();

  @TestConfiguration
  @Import({BugReportController.class})
  @MockBean({CloudStorageService.class, JiraService.class, JupyterApi.class})
  static class Configuration {
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
  @Mock
  MailService mailService;
  @Autowired
  JiraService jiraService;
  @Autowired
  CloudStorageService cloudStorageService;
  @Autowired
  JupyterApi jupyterApi;
  @Autowired
  BugReportController bugReportController;

  @Before
  public void setUp() throws MessagingException {
    User user = new User();
    user.setEmail(USER_EMAIL);
    user.setUserId(123L);
    user.setFreeTierBillingProjectName(FC_PROJECT_ID);
    user.setFreeTierBillingProjectStatus(BillingProjectStatus.READY);
    user.setDisabled(false);
    when(userProvider.get()).thenReturn(user);
    bugReportController.setUserProvider(userProvider);
    JSONObject credentails = new JSONObject();
   
   try {
     credentails.put("username", "mockUsername");
     credentails.put("password", "mockPassword");
     when(cloudStorageService.getJiraCredentials()).thenReturn(credentails);
     Mockito.doNothing().when(jiraService).authenticate("mockUsername", "mockPassword");
     Mockito.doNothing().when(jiraService).createIssue(any());
     Mockito.doNothing().when(jiraService).attachLogFiles(any(), any());
   }
  catch(Exception ex){}
  }

  @Test
  public void testSendBugReport() throws Exception {
    BugReport input = new BugReport()
        .contactEmail(USER_EMAIL)
        .includeNotebookLogs(false)
        .reproSteps("press button")
        .shortDescription("bug");
    bugReportController.sendBugReport(input);
    verify(jiraService, times(1)).createIssue(input);
    // The message content should have 1 part, the main body part and no attachments
    verify(jupyterApi, never()).getRootContents(any(), any(), any(), any(), any(), any());
    verify(jiraService,never()).attachLogFiles(any(),any());

  }

  @Test
  public void testSendBugReportWithNotebooks() throws Exception {
    when(jupyterApi.getRootContents(any(), any(), any(), any(), any(), any()))
        .thenReturn(TEST_CONTENTS);
    BugReport input = new BugReport()
        .contactEmail(USER_EMAIL)
        .includeNotebookLogs(true)
        .reproSteps("press button")
        .shortDescription("bug");
    bugReportController.sendBugReport(input
       );

    verify(jiraService, times(1)).createIssue(input);
    // The message content should have 4 parts, the main body part and three attachments
    verify(jupyterApi).getRootContents(
        eq(FC_PROJECT_ID), any(), eq("delocalization.log"), any(), any(), any());
    verify(jupyterApi).getRootContents(
        eq(FC_PROJECT_ID), any(), eq("localization.log"), any(), any(), any());
    verify(jupyterApi).getRootContents(
        eq(FC_PROJECT_ID), any(), eq("jupyter.log"), any(), any(), any());
    verify(jiraService,times(3)).attachLogFiles(any(),any());
  }

  @Test
  public void testSendBugReportWithNotebookErrors() throws Exception {
    when(jupyterApi.getRootContents(any(), any(), any(), any(), any(), any()))
        .thenReturn(TEST_CONTENTS);
    when(jupyterApi.getRootContents(any(), any(), eq("jupyter.log"), any(), any(), any()))
        .thenThrow(new ApiException(404, "not found"));
    BugReport input = new BugReport()
        .contactEmail(USER_EMAIL)
        .includeNotebookLogs(true)
        .reproSteps("press button")
        .shortDescription("bug");
    bugReportController.sendBugReport(input);

    verify(jiraService, times(1)).createIssue(input);
    // The message content should have 3 parts, the main body part and two attachments
    verify(jiraService,times(2)).attachLogFiles(any(),any());

  }



}
