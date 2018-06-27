package org.pmiops.workbench.jira;

import org.pmiops.workbench.model.BugReport;

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
  String createIssue(BugReport bugReport) throws ApiException;

  /**
   * Uploads attachment to jira ticket issueKey
   * @param issueKey
   * @param attachmentName
   * @param content
   * @throws ApiException
   */
  void uploadAttachment(String issueKey, String attachmentName, Object content) throws ApiException;
}
