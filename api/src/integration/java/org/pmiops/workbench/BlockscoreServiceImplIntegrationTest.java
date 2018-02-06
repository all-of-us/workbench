package org.pmiops.workbench;

import static com.google.common.truth.Truth.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.blockscore.BlockscoreServiceImpl;
import org.pmiops.workbench.google.CloudStorageServiceImpl;
import org.pmiops.workbench.test.Providers;

public class BlockscoreServiceImplIntegrationTest {
  private final BlockscoreServiceImpl service = new BlockscoreServiceImpl(
    Providers.of(new CloudStorageServiceImpl(Providers.of(createConfig())))
  );

  // TODO: Change names to be more distinct (there's a cloudStorageService test with the same name)
  @Test
  public void testCanReadFile() {
    assertThat(service.listPeople().getTotalCount()).isAtLeast(0);
  }

  private static WorkbenchConfig createConfig() {
    WorkbenchConfig config = new WorkbenchConfig();
    config.googleCloudStorageService = new WorkbenchConfig.GoogleCloudStorageServiceConfig();
    config.googleCloudStorageService.credentialsBucketName = "all-of-us-workbench-test-credentials";
    return config;
  }
}
