package org.pmiops.workbench.google;

import static com.google.common.truth.Truth.assertThat;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.test.Providers;

public class DirectoryServiceImplIntegrationTest {
  private DirectoryServiceImpl service;
  private final GoogleCredential googleCredential = getGoogleCredential();
  private final WorkbenchConfig workbenchConfig = createConfig();

  @Before
  public void setup() {
    service = new DirectoryServiceImpl(
        Providers.of(googleCredential), Providers.of(workbenchConfig));
  }

  @Test
  public void testDummyUsernameIsNotTaken() throws IOException {
    assertThat(service.isUsernameTaken("username-that-should-not-exist")).isFalse();
  }

  @Test
  public void testDirectoryServiceUsernameIsTaken() throws IOException {
    assertThat(service.isUsernameTaken("directory-service")).isTrue();
  }

  @Test
  public void testCreateAndDeleteTestUser() throws IOException {
    service.createUser("Integration", "Test", "integration.test", "notasecret");
    assertThat(service.isUsernameTaken("integration.test")).isTrue();
    service.deleteUser("integration.test");
    assertThat(service.isUsernameTaken("integration.test")).isFalse();
  }

  private static GoogleCredential getGoogleCredential() {
    try {
      String saKeyPath = "src/main/webapp/WEB-INF/sa-key.json";
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
