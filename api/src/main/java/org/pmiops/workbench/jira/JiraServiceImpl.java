package org.pmiops.workbench.jira;

import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.jira.api.JiraApi;
import org.pmiops.workbench.jira.model.*;
import org.pmiops.workbench.model.BugReport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

import javax.inject.Provider;


@Service
public class JiraServiceImpl implements JiraService {

  private JiraApi api = new JiraApi();
  private final Provider<WorkbenchConfig> configProvider;

  @Autowired
  public JiraServiceImpl(Provider<WorkbenchConfig> configProvider) {
    this.configProvider = configProvider;
  }

  /**
   * Authenticate Jira API
   * @param username
   * @param password
   */
  public void authenticate(String username, String password){
    api.getApiClient().setUsername(username);
    api.getApiClient().setPassword(password);
  }

  /**
   * Creates Issue in JIRA
   * @param bugReport
   * @return Issue number created
   */
  public String createIssue(BugReport bugReport) {
    bugReport.setReproSteps(bugReport.getReproSteps() +"\\n Contact Email: "+bugReport.getContactEmail());

    IssueRequest issueDetails = new IssueRequest();
    IssueType issueType = new IssueType();
    FieldsDetails fieldsDetail = new FieldsDetails();
    ProjectDetails projectDetails = new ProjectDetails();

    projectDetails.setKey(configProvider.get().jira.projectKey);

    fieldsDetail.setDescription(bugReport.getReproSteps());
    fieldsDetail.setSummary(bugReport.getShortDescription());
    fieldsDetail.setProject(projectDetails);

    issueType.setName(configProvider.get().jira.issueType);
    fieldsDetail.setIssuetype(issueType);

    issueDetails.setFields(fieldsDetail);
    try{
      IssueResponse response = api.createIssue(issueDetails);
      return response.getKey();
    }
    catch(Exception ex){
      System.out.println(ex);
    }
      return "";
  }

  /**
   * Attach Log files to issue
   * @param issueKey : Issue Id number to which log files are to be attached
   * @param fileList: List of attachments
   */
  public void attachLogFiles(String issueKey, List<File> fileList) {
    try {
      for(File file: fileList) {
        api.addAttachments(issueKey, file,"nocheck");
      }
    }catch(Exception ex){
      System.out.println(ex);
    }
    return;
  }
}
