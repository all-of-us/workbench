package org.pmiops.workbench.api;

import javax.mail;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.pmiops.workbench.firecloud.ApiException;
import org.pmiops.workbench.model.BugReport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BugReportController implements BugsApiDelegate {

  private static final Logger log = Logger.getLogger(BugReportController.class.getName());



  @Override
  public ResponseEntity<BugReport> sendBug(BugReport bugReport) {
    Properties props = new Properties();
    Session session = Session.getDefaultInstance(props, null);

    try {
      Message msg = new MimeMessage(session);
      // TODO: sender address msg.setFrom(new InternetAddress(""))
      msg.addRecipient(Message.RecipientType.TO, new InternetAddress("brubenst@broadinstitute.org", "My Email"));
      msg.setSubject("[AofU Bug Report]" + bugReport.shortDescription);
      msg.setText(bugReport.reproSteps);
      Transport.send(msg);
    } catch (AddressException e) {
      // ...
    } catch (MessagingException e) {

    } catch (UnsupportedEncodingException e) {

    }
  }
}
