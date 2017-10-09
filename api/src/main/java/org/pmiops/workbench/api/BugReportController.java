package org.pmiops.workbench.api;

import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.AddressException;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Properties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import org.pmiops.workbench.exceptions.EmailException;
import org.pmiops.workbench.model.BugReport;


@RestController
public class BugReportController implements BugReportApiDelegate {

  private static final Logger log = Logger.getLogger(BugReportController.class.getName());

  @Override
  public ResponseEntity<BugReport> sendBugReport(BugReport bugReport) {
    Properties props = new Properties();
    Session session = Session.getDefaultInstance(props, null);
    try {
      Message msg = new MimeMessage(session);
      msg.setFrom(new InternetAddress("all-of-us-workbench-eng@googlegroups.com"));
      InternetAddress[] replyTo = new InternetAddress[1];
      replyTo[0] = new InternetAddress(bugReport.getContactEmail());
      msg.setReplyTo(replyTo);
      // To test the bug reporting functionality, change the recipient email to your email rather
      // than the group.
      // https://precisionmedicineinitiative.atlassian.net/browse/RW-40
      msg.addRecipient(Message.RecipientType.TO, new InternetAddress(
          "all-of-us-workbench-eng@googlegroups.com", "AofU Workbench Engineers"));
      msg.setSubject("[AofU Bug Report]: " + bugReport.getShortDescription());
      msg.setText(bugReport.getReproSteps());
      Transport.send(msg);
    } catch (MessagingException e) {
      throw new EmailException("Error sending bug report", e);
    } catch (UnsupportedEncodingException e) {
      throw new EmailException("Error sending bug report", e);
    }

    return ResponseEntity.ok(bugReport);
  }
}
