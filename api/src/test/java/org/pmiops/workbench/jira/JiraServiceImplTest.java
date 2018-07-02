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

import java.io.File;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
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

  private File mockFile = new File("dummyPath");

  @Before
  public void setUp() throws Exception {
    JSONObject jiraCredentials = new JSONObject();
    jiraCredentials.putOnce("username", "username");
    jiraCredentials.putOnce("password", "password");
    when(cloudStorageService.getJiraCredentials()).thenReturn(jiraCredentials);
    workbenchConfig.jira = new WorkbenchConfig.JiraConfig();
    workbenchConfig.jira.projectKey = "RW";
    service = new JiraServiceImpl(
        Providers.of(workbenchConfig),
        Providers.of(cloudStorageService)
    );
    doThrow(new  ApiException("Missing the required parameter 'issueKey' when calling addAttachments(Async)"))
        .when(jiraApi).addAttachments(null, mockFile, "nocheck");

    doThrow(new  ApiException("Missing the required parameter 'file' when calling addAttachments(Async)"))
        .when(jiraApi).addAttachments("IssueKey", null, "nocheck");

    service.setJiraApi(jiraApi);
    when(jiraApi.getApiClient()).thenReturn(apiClient);
    when(jiraApi.createIssue( any() )).thenReturn(new IssueResponse());
  }

  @Test
  public void testCreateIssue() throws Exception {
    IssueResponse response = service.createIssue(new BugReport());
    Assert.assertNotNull(response);
  }

  @Test
  public void testAddAttachmentNullIssueKey() {
    try {
      service.uploadAttachment(null, mockFile);
      Assert.assertTrue(false);
    } catch (ApiException e) {
      Assert.assertEquals(e.getMessage(),
          "Missing the required parameter 'issueKey' when calling addAttachments(Async)");
    }
  }

  @Test
  public void testAddAttachmentNoFile() {
    try {
      service.uploadAttachment("IssueKey", null);
      Assert.assertTrue(false);
    } catch (ApiException e) {
      Assert.assertEquals(e.getMessage(),
          "Missing the required parameter 'file' when calling addAttachments(Async)");
    }
  }

  @Test
  public void testAddAttachment() {
    try {
      service.uploadAttachment("IssueKey", mockFile);
      Assert.assertTrue(true);
    } catch (ApiException e) {
      Assert.assertFalse(true);
    }
  }
}