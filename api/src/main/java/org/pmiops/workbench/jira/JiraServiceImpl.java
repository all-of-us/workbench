package org.pmiops.workbench.jira;

import com.google.common.annotations.VisibleForTesting;

import org.json.JSONObject;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.jira.api.JiraApi;
import org.pmiops.workbench.jira.model.FieldsDetails;
import org.pmiops.workbench.jira.model.IssueRequest;
import org.pmiops.workbench.jira.model.IssueResponse;
import org.pmiops.workbench.jira.model.IssueType;
import org.pmiops.workbench.jira.model.ProjectDetails;
import org.pmiops.workbench.model.BugReport;
import org.pmiops.workbench.model.BugReportType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;

import javax.inject.Provider;

@Service
public class JiraServiceImpl implements JiraService {
  private static final String JIRA_CDR_PROJECT = "CDR";
  private static final String JIRA_WORKBENCH_PROJECT = "RW";

  private JiraApi api = new JiraApi();
  private CloudStorageService cloudStorageService;

  @Autowired
  public JiraServiceImpl(Provider<CloudStorageService> cloudStorageServiceProvider) {
    this.cloudStorageService = cloudStorageServiceProvider.get();
  }

  @VisibleForTesting
  public void setJiraApi(JiraApi api) {
    this.api = api;
  }

  //Sets username password for JIRA api
  private void setJiraCredentials() {
    JSONObject jiraCredentials = cloudStorageService.getJiraCredentials();
    api.getApiClient().setUsername(jiraCredentials.getString("username"));
    api.getApiClient().setPassword(jiraCredentials.getString("password"));
  }


  @Override
  public IssueResponse createIssue(BugReport bugReport) throws ApiException {
    setJiraCredentials();
    IssueRequest issueDetails = new IssueRequest();
    IssueType issueType = new IssueType().name(IssueTypeEnum.Bug.name());
    String projectKey = JIRA_WORKBENCH_PROJECT;
    if (BugReportType.DATA.equals(bugReport.getBugType())) {
      projectKey = JIRA_CDR_PROJECT;
    }
    FieldsDetails fieldsDetail = new FieldsDetails()
        .description(String.format("%s %nContact Email: %s", bugReport.getReproSteps(), bugReport.getContactEmail()))
        .summary("User Reported: " + bugReport.getShortDescription())
        .project(new ProjectDetails().key(projectKey))
        .issuetype(issueType);
    issueDetails.setFields(fieldsDetail);
    return api.createIssue(issueDetails);
  }


  @Override
  public void uploadAttachment(String issueKey, File attachment) throws ApiException {
    api.addAttachments(issueKey, attachment, "nocheck");
  }
}
