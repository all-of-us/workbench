package org.pmiops.workbench.api;

import java.io.UnsupportedEncodingException;
import java.util.Properties;
import javax.inject.Provider;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.exceptions.EmailException;
import org.pmiops.workbench.mail.MailService;
import org.pmiops.workbench.model.BugReport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class BugReportController implements BugReportApiDelegate {
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final Provider<MailService> mailServiceProvider;

  @Autowired
  BugReportController(Provider<WorkbenchConfig> workbenchConfigProvider, Provider<MailService> mailServiceProvider) {
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.mailServiceProvider = mailServiceProvider;
  }

  @Override
  public ResponseEntity<BugReport> sendBugReport(BugReport bugReport) {
    WorkbenchConfig workbenchConfig = workbenchConfigProvider.get();
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
      msg.setText(bugReport.getReproSteps());
      mailServiceProvider.get().send(msg);
    } catch (MessagingException e) {
      throw new EmailException("Error sending bug report", e);
    } catch (UnsupportedEncodingException e) {
      throw new EmailException("Error sending bug report", e);
    }

    return ResponseEntity.ok(bugReport);
  }
}
