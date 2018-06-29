package org.pmiops.workbench.jira;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.jira.api.JiraApi;
import org.pmiops.workbench.jira.model.IssueResponse;
import org.pmiops.workbench.model.BugReport;
import org.pmiops.workbench.test.Providers;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

public class JiraServiceImplTest {

  private JiraServiceImpl service;

  @Mock
  private WorkbenchConfig workbenchConfig;
  @Mock
  private CloudStorageService cloudStorageService;
  @Mock
  private JiraApi jiraApi;
  @Mock
  private ApiClient apiClient;
  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Before
  public void setUp() throws Exception {
    JSONObject jiraCredentials = new JSONObject();
    jiraCredentials.putOnce("username", "username");
    jiraCredentials.putOnce("password", "password");
    when(cloudStorageService.getJiraCredentials()).thenReturn(jiraCredentials);

    workbenchConfig.jira = new WorkbenchConfig.JiraConfig();
    workbenchConfig.jira.projectKey = "TEST";

    when(jiraApi.getApiClient()).thenReturn(apiClient);
    when(jiraApi.createIssue(any())).thenReturn(new IssueResponse());

    service = new JiraServiceImpl(
        Providers.of(workbenchConfig),
        Providers.of(cloudStorageService)
    );
    service.setJiraApi(jiraApi);
  }


  // TODO: Flesh out the positive and negative cases of create issue
  @Test
  public void testCreateIssue() throws Exception {
    IssueResponse response = service.createIssue(new BugReport());
    Assert.assertNotNull(response);
  }

  // TODO: Flesh out the upload attachment tests.

}
