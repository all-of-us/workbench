package org.pmiops.workbench.jira;

import org.pmiops.workbench.jira.model.IssueResponse;
import org.pmiops.workbench.model.BugReport;
import java.io.File;

public interface JiraService {

  /**
   * Issuetypenum denotes the type of Issue that will be created in JIRA
   * BUG : creates an issue of type Bug in JIRA
   */
  enum IssueTypeEnum {
    Bug
  };

  /**
   * Creates Issue in JIRA
   * @param bugReport
   * @return Issue key created
   */
  IssueResponse createIssue(BugReport bugReport) throws ApiException;

  /**
   * Uploads attachment to Jira Issue number issueKey
   * @param issueKey : Issue number
   * @param attachment: File to be uploaded
   * @throws ApiException
   */
  void uploadAttachment(String issueKey, File attachment) throws ApiException;
}
