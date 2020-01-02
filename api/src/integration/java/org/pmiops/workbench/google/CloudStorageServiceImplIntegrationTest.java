package org.pmiops.workbench.google;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.pmiops.workbench.BaseIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

public class CloudStorageServiceImplIntegrationTest extends BaseIntegrationTest {
  @Autowired private CloudStorageService service;

  @TestConfiguration
  @Import(CloudStorageServiceImpl.class)
  static class Configuration {}

  @Test
  public void testCanReadFile() {
    assertThat(service.readInvitationKey().length() > 4).isTrue();
  }
}
