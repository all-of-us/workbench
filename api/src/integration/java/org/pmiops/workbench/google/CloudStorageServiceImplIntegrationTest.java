package org.pmiops.workbench.google;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.IntegrationTestConfig;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.test.Providers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {IntegrationTestConfig.class})
public class CloudStorageServiceImplIntegrationTest {
  @Autowired
  private CloudStorageService service;

  @Test
  public void testCanReadFile() {
    assertThat(service.readInvitationKey().length() > 4).isTrue();
  }

}
