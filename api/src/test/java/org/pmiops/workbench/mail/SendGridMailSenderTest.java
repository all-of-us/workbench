package org.pmiops.workbench.mail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Personalization;
import jakarta.mail.MessagingException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.google.CloudStorageClient;

public class SendGridMailSenderTest {
  private static final String API_KEY = "test-api-key";
  private static final String FROM_EMAIL = "from@example.com";
  private static final String TO_EMAIL = "to@example.com";
  private static final String CC_EMAIL = "cc@example.com";
  private static final String BCC_EMAIL = "bcc@example.com";
  private static final String INVALID_EMAIL = "not-a-valid-email";
  private static final String SUBJECT = "Test Subject";
  private static final String HTML_CONTENT = "<p>Test content</p>";
  private static final String LOG_DESCRIPTION = "Test email";

  @Mock private CloudStorageClient mockCloudStorageClient;

  @Mock private SendGrid mockSendGrid;

  private SendGridMailSender sendGridMailSender;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);

    // Configure mock behavior
    when(mockCloudStorageClient.readSendgridApiKey()).thenReturn(API_KEY);

    // Configure retry settings
    WorkbenchConfig workbenchConfig = new WorkbenchConfig();
    workbenchConfig.mandrill = new WorkbenchConfig.MandrillConfig();
    workbenchConfig.mandrill.sendRetries = 3;

    // Create instance of SendGridMailSender with mocked dependencies
    sendGridMailSender =
        new SendGridMailSender(mockCloudStorageClient, workbenchConfig) {
          @Override
          protected SendGrid createSendGrid(String apiKey) {
            // Override to return our mock instead of creating a real SendGrid client
            return mockSendGrid;
          }
        };
  }

  @Test
  public void testSendEmail_success() throws IOException, MessagingException {
    Response mockResponse = mock(Response.class);
    when(mockSendGrid.api(any(Request.class))).thenReturn(mockResponse);

    sendGridMailSender.send(
        FROM_EMAIL,
        Collections.singletonList(TO_EMAIL),
        Collections.singletonList(CC_EMAIL),
        Collections.singletonList(BCC_EMAIL),
        SUBJECT,
        LOG_DESCRIPTION,
        HTML_CONTENT);

    ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
    verify(mockSendGrid, times(1)).api(requestCaptor.capture());

    String expectedBody =
        sendGridMailSender
            .createMail(
                FROM_EMAIL,
                Collections.singletonList(TO_EMAIL),
                Collections.singletonList(CC_EMAIL),
                Collections.singletonList(BCC_EMAIL),
                SUBJECT,
                HTML_CONTENT)
            .build();

    Request capturedRequest = requestCaptor.getValue();
    assertEquals("POST", capturedRequest.getMethod().name());
    assertEquals("mail/send", capturedRequest.getEndpoint());
    assertEquals(expectedBody, capturedRequest.getBody());
  }

  @Test
  public void testSendEmail_retryFailures() throws IOException, MessagingException {
    when(mockSendGrid.api(any(Request.class)))
        .thenThrow(new IOException("SendGrid API error"))
        .thenReturn(mock(Response.class));

    sendGridMailSender.send(
        FROM_EMAIL,
        Collections.singletonList(TO_EMAIL),
        Collections.singletonList(CC_EMAIL),
        Collections.singletonList(BCC_EMAIL),
        SUBJECT,
        LOG_DESCRIPTION,
        HTML_CONTENT);

    ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
    verify(mockSendGrid, times(2)).api(requestCaptor.capture());

    String expectedBody =
        sendGridMailSender
            .createMail(
                FROM_EMAIL,
                Collections.singletonList(TO_EMAIL),
                Collections.singletonList(CC_EMAIL),
                Collections.singletonList(BCC_EMAIL),
                SUBJECT,
                HTML_CONTENT)
            .build();

    Request capturedRequest = requestCaptor.getValue();
    assertEquals("POST", capturedRequest.getMethod().name());
    assertEquals("mail/send", capturedRequest.getEndpoint());
    assertEquals(expectedBody, capturedRequest.getBody());
  }

  @Test
  public void testSendEmail_limitRetryFailures() throws IOException {
    when(mockSendGrid.api(any(Request.class))).thenThrow(new IOException("SendGrid API error"));

    assertThrows(
        MessagingException.class,
        () -> {
          sendGridMailSender.send(
              FROM_EMAIL,
              Collections.singletonList(TO_EMAIL),
              Collections.singletonList(CC_EMAIL),
              Collections.singletonList(BCC_EMAIL),
              SUBJECT,
              LOG_DESCRIPTION,
              HTML_CONTENT);
        });

    verify(mockSendGrid, times(3)).api(any(Request.class));
  }

  @Test
  public void testCreateMail_success() {
    Mail mail =
        sendGridMailSender.createMail(
            FROM_EMAIL,
            Collections.singletonList(TO_EMAIL),
            Collections.singletonList(CC_EMAIL),
            Collections.singletonList(BCC_EMAIL),
            SUBJECT,
            HTML_CONTENT);

    assertEquals(FROM_EMAIL, mail.getFrom().getEmail());
    assertEquals(SUBJECT, mail.getSubject());

    Content expectedContent = new Content("text/html", HTML_CONTENT);
    assertEquals(expectedContent, mail.getContent().get(0));

    List<Personalization> personalizations = mail.getPersonalization();
    assertEquals(1, personalizations.size());

    Personalization personalization = personalizations.get(0);
    assertEquals(1, personalization.getTos().size());
    assertEquals(TO_EMAIL, personalization.getTos().get(0).getEmail());
    assertEquals(1, personalization.getCcs().size());
    assertEquals(CC_EMAIL, personalization.getCcs().get(0).getEmail());
    assertEquals(1, personalization.getBccs().size());
    assertEquals(BCC_EMAIL, personalization.getBccs().get(0).getEmail());
  }

  @Test
  public void testCreateMail_invalidToEmail() {
    List<String> toEmails = Arrays.asList(TO_EMAIL, INVALID_EMAIL);

    assertThrows(
        ServerErrorException.class,
        () -> {
          sendGridMailSender.createMail(
              FROM_EMAIL,
              toEmails,
              Collections.singletonList(CC_EMAIL),
              Collections.singletonList(BCC_EMAIL),
              SUBJECT,
              HTML_CONTENT);
        });
  }

  @Test
  public void testCreateMail_invalidCcEmail() {
    List<String> ccEmails = Arrays.asList(CC_EMAIL, INVALID_EMAIL);

    assertThrows(
        ServerErrorException.class,
        () -> {
          sendGridMailSender.createMail(
              FROM_EMAIL,
              Collections.singletonList(TO_EMAIL),
              ccEmails,
              Collections.singletonList(BCC_EMAIL),
              SUBJECT,
              HTML_CONTENT);
        });
  }

  @Test
  public void testCreateMail_invalidBccEmail() {
    List<String> bccEmails = Arrays.asList(BCC_EMAIL, INVALID_EMAIL);

    assertThrows(
        ServerErrorException.class,
        () -> {
          sendGridMailSender.createMail(
              FROM_EMAIL,
              Collections.singletonList(TO_EMAIL),
              Collections.singletonList(CC_EMAIL),
              bccEmails,
              SUBJECT,
              HTML_CONTENT);
        });
  }
}
