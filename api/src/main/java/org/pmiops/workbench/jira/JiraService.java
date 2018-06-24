package org.pmiops.workbench.jira;

import org.pmiops.workbench.model.BugReport;

import java.io.File;
import java.util.List;

public interface JiraService {
  void authenticate(String username, String password);
  String createIssue(BugReport bugReport);
  void attachLogFiles(String issueKey, List<File> fileList);
}
