package org.pmiops.workbench.google;

import static com.google.common.truth.Truth.assertThat;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.apache.ApacheHttpTransport;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.Clock;
import javax.mail.MessagingException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.mail.MailService;
import org.pmiops.workbench.mail.MailServiceImpl;
import org.pmiops.workbench.test.Providers;
import org.springframework.retry.backoff.NoBackOffPolicy;

public class DirectoryServiceImplIntegrationTest {
  private DirectoryServiceImpl service;
  private final GoogleCredential googleCredential = getGoogleCredential();
  private final WorkbenchConfig workbenchConfig = createConfig();
  private final ApacheHttpTransport httpTransport = new ApacheHttpTransport();

  @Before
  public void setUp() throws MessagingException {
    MailService mailService = Mockito.mock(MailServiceImpl.class);
    service =
        new DirectoryServiceImpl(
            Providers.of(googleCredential),
            Providers.of(workbenchConfig),
            httpTransport,
            new GoogleRetryHandler(new NoBackOffPolicy()));
  }

  @Test
  public void testDummyUsernameIsNotTaken() {
    assertThat(service.isUsernameTaken("username-that-should-not-exist")).isFalse();
  }

  @Test
  public void testDirectoryServiceUsernameIsTaken() {
    assertThat(service.isUsernameTaken("directory-service")).isTrue();
  }

  @Test
  public void testCreateAndDeleteTestUser() {
    String userName = String.format("integration.test.%d", Clock.systemUTC().millis());
    service.createUser("Integration", "Test", userName, "notasecret@gmail.com");
    assertThat(service.isUsernameTaken(userName)).isTrue();
    // Ensure our two custom schema fields are correctly set & re-fetched from GSuite.
    assertThat(service.getContactEmailAddress(userName).equals("notasecret@gmail.com"));
    assertThat(
        service
            .getUserByUsername(userName)
            .getCustomSchemas()
            .get("All_of_Us_Workbench")
            .get("Institution")
            .equals("All of Us Research Workbench"));
    service.deleteUser(userName);
    assertThat(service.isUsernameTaken(userName)).isFalse();
  }

  private static GoogleCredential getGoogleCredential() {
    try {
      String saKeyPath = "src/main/webapp/WEB-INF/gsuite-admin-sa.json";
      return GoogleCredential.fromStream(new FileInputStream(new File(saKeyPath)));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static WorkbenchConfig createConfig() {
    WorkbenchConfig config = new WorkbenchConfig();
    config.googleDirectoryService = new WorkbenchConfig.GoogleDirectoryServiceConfig();
    config.googleDirectoryService.gSuiteDomain = "fake-research-aou.org";
    return config;
  }
}
