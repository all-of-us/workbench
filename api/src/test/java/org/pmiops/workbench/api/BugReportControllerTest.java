package org.pmiops.workbench.api;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.exceptions.EmailException;
import org.pmiops.workbench.mail.MailService;
import org.pmiops.workbench.mail.MailServiceImpl;
import org.pmiops.workbench.model.BugReport;
import org.pmiops.workbench.test.Providers;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import javax.mail.MessagingException;

import java.io.UnsupportedEncodingException;

import static com.google.common.truth.Truth.assertThat;

@RunWith(SpringRunner.class)
public class BugReportControllerTest {

    private BugReportController controller;
    private MailService mailService;

    @Before
    public void setup() throws MessagingException {
        mailService = Mockito.mock(MailServiceImpl.class);
        Mockito.doNothing().when(mailService).send(Mockito.any());
        controller = new BugReportController(
            Providers.of(createConfig()),
            Providers.of(mailService));
    }

    @Test
    public void testSendBugReport() {
        BugReport report = makeBugReport();
        ResponseEntity<BugReport> entity = controller.sendBugReport(report);
        assertThat(entity).isNotNull();
        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(entity.getBody()).isNotNull();
        assertThat(entity.getBody()).isEqualTo(report);
    }

    @Test(expected = EmailException.class)
    public void testSendBugReportMessagingFailure() throws MessagingException {
        Mockito.doThrow(new MessagingException()).when(mailService).send(Mockito.any());
        BugReport report = makeBugReport();
        controller.sendBugReport(report);
    }

    private WorkbenchConfig createConfig() {
        WorkbenchConfig config = new WorkbenchConfig();
        config.admin = new WorkbenchConfig.AdminConfig();
        config.admin.supportGroup = "test@fake-research-aou.org";
        config.admin.verifiedSendingAddress = "test@fake-research-aou.org";
        return config;
    }

    private BugReport makeBugReport() {
        BugReport report = new BugReport();
        report.setContactEmail("contact@fake-research-aou.org");
        report.setReproSteps("Repro Steps");
        report.setShortDescription("Description");
        return report;
    }
}
