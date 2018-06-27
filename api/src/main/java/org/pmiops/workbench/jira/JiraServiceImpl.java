package org.pmiops.workbench.jira;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Logger;

import javax.inject.Provider;

@Service
public class JiraServiceImpl implements JiraService {

  private static final Logger log = Logger.getLogger(JiraServiceImpl.class.getName());
  private JiraApi api = new JiraApi();
  private final Provider<WorkbenchConfig> configProvider;
  private CloudStorageService cloudStorageService;

  @Autowired
  public JiraServiceImpl(Provider<WorkbenchConfig> configProvider,
      Provider<CloudStorageService> cloudStorageServiceProvider) {
    this.configProvider = configProvider;
    this.cloudStorageService = cloudStorageServiceProvider.get();
  }

  /**
   * Sets username password for JIRA api
   */
  private void setJiraCredentails(){
    JSONObject jiraCredentails = cloudStorageService.getJiraCredentials();
    api.getApiClient().setUsername(jiraCredentails.getString("username"));
    api.getApiClient().setPassword(jiraCredentails.getString("password"));
  }


  @Override
  public String createIssue(BugReport bugReport) throws ApiException {
    setJiraCredentails();
    IssueRequest issueDetails = new IssueRequest();
    IssueType issueType = new IssueType();
    FieldsDetails fieldsDetail = new FieldsDetails();
    ProjectDetails projectDetails = new ProjectDetails();

    projectDetails.setKey(configProvider.get().jira.projectKey);

    fieldsDetail.setDescription(String.format("%s %nContact Email: %s",bugReport.getReproSteps(),
        bugReport.getContactEmail()));
    fieldsDetail.setSummary(bugReport.getShortDescription());
    fieldsDetail.setProject(projectDetails);

    issueType.setName(IssueTypeEnum.Bug.name());
    fieldsDetail.setIssuetype(issueType);

    issueDetails.setFields(fieldsDetail);
    api.getApiClient().setDebugging(true);
    IssueResponse response = api.createIssue(issueDetails);
    return response.getKey();
  }


  @Override
  public void uploadAttachment(String issueKey, String attachmentName, Object content) throws ApiException {
    try {
      if (content instanceof String) {
        File temporary = createTempFile(attachmentName, ((String) content).getBytes());
        api.addAttachments(issueKey, temporary, "nocheck");
        temporary.delete();
      }
    } catch (SecurityException ex) {
      log.warning(String.format("Exception while deleting temp file '%s'", attachmentName));
    }
  }

  /**
   * Creates temp File to be attached to jira issue
   * @param name
   * @param content
   * @return temp File
   */
  private File createTempFile(String name,byte[] content) {
    try{
      File tempFile = File.createTempFile(name, ".log");
      FileOutputStream writer = new FileOutputStream(tempFile);
      writer.write(content);
      return tempFile;
    } catch(IOException e){
      log.severe(String.format("Error while creating temporary log files %s", name));
    }
    return null;
  }
}
