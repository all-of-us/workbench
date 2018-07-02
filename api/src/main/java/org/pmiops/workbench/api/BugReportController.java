package org.pmiops.workbench.api;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;

import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.jira.JiraService;
import org.pmiops.workbench.model.BillingProjectStatus;
import org.pmiops.workbench.model.BugReport;
import org.pmiops.workbench.notebooks.ApiException;
import org.pmiops.workbench.notebooks.NotebooksService;
import org.pmiops.workbench.notebooks.api.JupyterApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Provider;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

@RestController
public class BugReportController implements BugReportApiDelegate {
  private static final Logger log = Logger.getLogger(BugReportController.class.getName());
  private static final List<String> notebookLogFiles =
      ImmutableList.of("delocalization.log", "jupyter.log", "localization.log");

  private final Provider<JiraService> jiraServiceProvider;
  private final Provider<JupyterApi> jupyterApiProvider;
  private Provider<User> userProvider;

  @Autowired
  BugReportController(
      Provider<User> userProvider,
      Provider<JupyterApi> jupyterApiProvider,
      Provider<JiraService> jiraService) {
    this.userProvider = userProvider;
    this.jupyterApiProvider = jupyterApiProvider;
    this.jiraServiceProvider = jiraService;
  }

  @VisibleForTesting
  void setUserProvider(Provider<User> userProvider) {
    this.userProvider = userProvider;
  }


  @Override
  public ResponseEntity<BugReport> sendBugReport(BugReport bugReport) {
    User user = userProvider.get();
    JupyterApi jupyterApi = jupyterApiProvider.get();
    JiraService jiraService = jiraServiceProvider.get();
    try {
      String issueKey = jiraService.createIssue(bugReport).getKey();
      // If requested, try to pull logs from the notebook cluster using the researcher's creds. Some
      // or all of these log files might be missing, or the cluster may not even exist, so ignore
      // failures here.
      if (Optional.ofNullable(bugReport.getIncludeNotebookLogs()).orElse(false) &&
          BillingProjectStatus.READY.equals(user.getFreeTierBillingProjectStatus())) {
        for (String fileName : BugReportController.notebookLogFiles) {
          try {
            String logContent = jupyterApi.getRootContents(
                user.getFreeTierBillingProjectName(), NotebooksService.DEFAULT_CLUSTER_NAME,
                fileName, "file", "text", /* content */ 1).getContent();
            if (logContent == null) {
              log.info(
                  String.format("Jupyter returned null content for '%s', continuing", fileName));
              continue;
            }
            File tempLogFile = createTempFile(fileName, logContent);
            try {
              jiraService.uploadAttachment(issueKey, tempLogFile);
            } catch (org.pmiops.workbench.jira.ApiException e) {
              log.severe(String.format("failed to upload attachment '%s', continuing", fileName));
            } finally {
              if ( tempLogFile != null) {
                try {
                  tempLogFile.delete();
                } catch (SecurityException ex){
                  log.severe(String.format("Error while deleting temporary log file %s", fileName));
                }
              }
            }
          } catch (ApiException e) {
            log.info(String.format("failed to retrieve notebook log '%s', continuing", fileName));
          }
        }
     }
    } catch (org.pmiops.workbench.jira.ApiException e) {
      log.severe(String.format("Error while connecting to JIRA server %s", e.getMessage() ));
      if (e.getCode() == HttpStatus.BAD_REQUEST.value())
        throw new BadRequestException("Bad Request please check the summary or description");
      throw new ServerErrorException("Error while connecting to JIRA server ");
    }
    return ResponseEntity.ok(bugReport);
  }

  /**
   * Creates temp File to be attached to jira issue
   * @param name
   * @param content
   * @return temp File
   */
  private File createTempFile(String name,String content) {
    try{
      File tempFile = File.createTempFile(name, ".log");
      FileOutputStream writer = new FileOutputStream(tempFile);
      writer.write(content.getBytes());
      return tempFile;
    } catch(IOException e){
      log.severe(String.format("Error while creating temporary log files %s", name));
    }
    return null;
  }
}
