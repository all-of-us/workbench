package org.pmiops.workbench.jira;

import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.jira.api.JiraApi;
import org.pmiops.workbench.jira.model.*;
import org.pmiops.workbench.model.BugReport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.logging.Logger;

import javax.inject.Provider;


@Service
public class JiraServiceImpl implements JiraService {

  private static final Logger log = Logger.getLogger(JiraServiceImpl.class.getName());

  private JiraApi api = new JiraApi();
  private final Provider<WorkbenchConfig> configProvider;
  public enum IssueTypeEnum {
    Bug
  }

  @Autowired
  public JiraServiceImpl(Provider<WorkbenchConfig> configProvider) {
    this.configProvider = configProvider;
  }

  /**
   * Authenticate Jira API
   * @param username
   * @param password
   */
  public void setJiraCredentials(String username, String password){
    api.getApiClient().setUsername(username);
    api.getApiClient().setPassword(password);
  }

  /**
   * Creates Issue in JIRA
   * @param bugReport
   * @return Issue number created
   */
  public String createIssue(BugReport bugReport) throws ApiException {
    bugReport.setReproSteps(bugReport.getReproSteps() + System.getProperty("line.separator")+
        "Contact Email: "+bugReport.getContactEmail());

    IssueRequest issueDetails = new IssueRequest();
    IssueType issueType = new IssueType();
    FieldsDetails fieldsDetail = new FieldsDetails();
    ProjectDetails projectDetails = new ProjectDetails();

    projectDetails.setKey(configProvider.get().jira.projectKey);

    fieldsDetail.setDescription(bugReport.getReproSteps());
    fieldsDetail.setSummary(bugReport.getShortDescription());
    fieldsDetail.setProject(projectDetails);

    issueType.setName(IssueTypeEnum.Bug.name());
    fieldsDetail.setIssuetype(issueType);

    issueDetails.setFields(fieldsDetail);
    IssueResponse response = api.createIssue(issueDetails);
    return response.getKey();
  }

  /**
   * Attach Log files to issue
   * @param issueKey : Issue Id number to which log files are to be attached
   * @param file: Attachment
   */
  public void attachLogFiles(String issueKey, File file) throws ApiException {
    api.addAttachments(issueKey, file,"nocheck");
    try {
      file.delete();
    } catch(SecurityException ex){
      log.warning("Exception while deleting temp log files");
    }
    return;
  }
}
