package org.pmiops.workbench.jira;

import org.pmiops.workbench.model.BugReport;

import java.io.File;
import java.util.List;

public interface JiraService {
  void authenticate(String username, String password) throws ApiException;
  String createIssue(BugReport bugReport) throws ApiException;
  void attachLogFiles(String issueKey, File fileList) throws ApiException;
}
