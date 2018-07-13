package org.pmiops.workbench.api;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.jira.JiraService;
import org.pmiops.workbench.jira.model.IssueResponse;
import org.pmiops.workbench.model.BillingProjectStatus;
import org.pmiops.workbench.model.BugReport;
import org.pmiops.workbench.notebooks.ApiException;
import org.pmiops.workbench.notebooks.api.JupyterApi;
import org.pmiops.workbench.notebooks.model.JupyterContents;
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
  private static final String FC_PROJECT_ID = "fc-project";
  private static final String USER_EMAIL = "falco@lombardi.com";

  private static final JupyterContents TEST_CONTENTS =
      new JupyterContents().content("log contents");

  @TestConfiguration
  @Import({BugReportController.class})
  @MockBean({JiraService.class, JupyterApi.class})
  static class Configuration {
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
  JiraService jiraService;
  @Autowired
  JupyterApi jupyterApi;
  @Autowired
  BugReportController bugReportController;

  @Before
  public void setUp() throws Exception {
    User user = new User();
    user.setEmail(USER_EMAIL);
    user.setUserId(123L);
    user.setFreeTierBillingProjectName(FC_PROJECT_ID);
    user.setFreeTierBillingProjectStatus(BillingProjectStatus.READY);
    user.setDisabled(false);
    when(userProvider.get()).thenReturn(user);
    bugReportController.setUserProvider(userProvider);
    doReturn(new IssueResponse()).when(jiraService).createIssue(any());
    doNothing().when(jiraService).uploadAttachment(any(), any());
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
    // No calls should be made for uploads
    verify(jupyterApi, never()).getRootContents(any(), any(), any(), any(), any(), any());
    verify(jiraService, never()).uploadAttachment(any(), any());
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
    bugReportController.sendBugReport(input);
    verify(jiraService, times(1)).createIssue(input);
    verify(jupyterApi).getRootContents(
        eq(FC_PROJECT_ID), any(), eq("delocalization.log"), any(), any(), any());
    verify(jupyterApi).getRootContents(
        eq(FC_PROJECT_ID), any(), eq("localization.log"), any(), any(), any());
    verify(jupyterApi).getRootContents(
        eq(FC_PROJECT_ID), any(), eq("jupyter.log"), any(), any(), any());
    verify(jiraService, times(3)).uploadAttachment(any(), any());
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
    // There should be just 2 calls for uploads
    verify(jiraService, times(2)).uploadAttachment(any(), any());
  }

}
