package org.pmiops.workbench.google;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.Test;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.test.Providers;

public class DirectoryServiceImplIntegrationTest {
  private DirectoryServiceImpl service;
  private final WorkbenchConfig workbenchConfig = createConfig();

  @Before
  public void setup() {
    service = new DirectoryServiceImpl(Providers.of(workbenchConfig));
  }

  @Test
  public void testDummyUsernameIsNotTaken() {
    assertThat(service.isUsernameTaken("username-that-should-not-exist")).isFalse();
  }

  @Test
  public void testDirectoryServiceUsernameIsTaken() {
    assertThat(service.isUsernameTaken("directory-service")).isTrue();
  }

  private static WorkbenchConfig createConfig() {
    WorkbenchConfig config = new WorkbenchConfig();
    config.googleDirectoryService = new WorkbenchConfig.GoogleDirectoryServiceConfig();
    config.googleDirectoryService.gSuiteDomain = "fake-research-aou.org";
    return config;
  }
}
