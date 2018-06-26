package org.pmiops.workbench.jira;

import org.pmiops.workbench.model.BugReport;

import java.io.File;

public interface JiraService {
  void setJiraCredentials(String username, String password) throws ApiException;
  String createIssue(BugReport bugReport) throws ApiException;
  void attachLogFiles(String issueKey, File fileList) throws ApiException;
}
