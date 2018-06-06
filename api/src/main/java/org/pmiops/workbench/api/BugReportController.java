package org.pmiops.workbench.api;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Logger;

import javax.activation.DataHandler;
import javax.inject.Provider;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.exceptions.EmailException;
import org.pmiops.workbench.notebooks.ApiException;
import org.pmiops.workbench.model.BillingProjectStatus;
import org.pmiops.workbench.model.BugReport;
import org.pmiops.workbench.notebooks.NotebooksService;
import org.pmiops.workbench.notebooks.api.JupyterApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class BugReportController implements BugReportApiDelegate {
  private static final Logger log = Logger.getLogger(BugReportController.class.getName());
  private static final List<String> notebookLogFiles =
      ImmutableList.of("delocalization.log", "jupyter.log", "localization.log");

  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final Provider<JupyterApi> jupyterApiProvider;
  private Provider<User> userProvider;

  @Autowired
  BugReportController(
      Provider<WorkbenchConfig> workbenchConfigProvider,
      Provider<User> userProvider,
      Provider<JupyterApi> jupyterApiProvider) {
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.userProvider = userProvider;
    this.jupyterApiProvider = jupyterApiProvider;
  }

  @VisibleForTesting
  void setUserProvider(Provider<User> userProvider) {
    this.userProvider = userProvider;
  }

  @Override
  public ResponseEntity<BugReport> sendBugReport(BugReport bugReport) {
    WorkbenchConfig workbenchConfig = workbenchConfigProvider.get();
    User user = userProvider.get();
    JupyterApi jupyterApi = jupyterApiProvider.get();
    Properties props = new Properties();
    Session session = Session.getDefaultInstance(props, null);
    try {
      Message msg = new MimeMessage(session);
      msg.setFrom(new InternetAddress(workbenchConfig.admin.verifiedSendingAddress));
      InternetAddress[] replyTo = new InternetAddress[1];
      replyTo[0] = new InternetAddress(bugReport.getContactEmail());
      msg.setReplyTo(replyTo);
      // To test the bug reporting functionality, change the recipient email to your email rather
      // than the group.
      // https://precisionmedicineinitiative.atlassian.net/browse/RW-40
      msg.addRecipient(Message.RecipientType.TO, new InternetAddress(
          workbenchConfig.admin.supportGroup, "AofU Workbench Engineers"));
      msg.setSubject("[AofU Bug Report]: " + bugReport.getShortDescription());

      Multipart multipart = new MimeMultipart();
      MimeBodyPart textPart = new MimeBodyPart();
      textPart.setText(bugReport.getReproSteps());
      multipart.addBodyPart(textPart);

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
            MimeBodyPart attachPart = new MimeBodyPart();
            attachPart.setDataHandler(new DataHandler(logContent, "text/plain"));
            attachPart.setFileName(fileName);
            multipart.addBodyPart(attachPart);
          } catch (ApiException e) {
            log.info(String.format("failed to retrieve notebook log '%s', continuing", fileName));
          }
        }
      }
      msg.setContent(multipart);
      Transport.send(msg);
    } catch (MessagingException e) {
      throw new EmailException("Error sending bug report", e);
    } catch (UnsupportedEncodingException e) {
      throw new EmailException("Error sending bug report", e);
    }

    return ResponseEntity.ok(bugReport);
  }
}
