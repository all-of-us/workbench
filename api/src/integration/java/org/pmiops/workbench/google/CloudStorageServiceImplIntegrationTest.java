package org.pmiops.workbench.google;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.test.Providers;

public class CloudStorageServiceImplIntegrationTest {
  private CloudStorageServiceImpl service;
  private final WorkbenchConfig workbenchConfig = createConfig();

  @Before
  public void setUp() {
    service = new CloudStorageServiceImpl(Providers.of(workbenchConfig));
  }

  @Test
  public void testCanReadFile() {
    assertThat(service.readInvitationKey().length() > 4).isTrue();
  }

  private static WorkbenchConfig createConfig() {
    WorkbenchConfig config = new WorkbenchConfig();
    config.googleCloudStorageService = new WorkbenchConfig.GoogleCloudStorageServiceConfig();
    config.googleCloudStorageService.credentialsBucketName = "all-of-us-workbench-test-credentials";
    return config;
  }
}
