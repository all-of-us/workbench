package org.pmiops.workbench;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserRecentResourceService;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.FireCloudServiceImpl;
import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.google.DirectoryServiceImpl;
import org.pmiops.workbench.monitoring.MonitoringService;
import org.pmiops.workbench.notebooks.LeonardoNotebooksClient;
import org.pmiops.workbench.notebooks.LeonardoNotebooksClientImpl;
import org.pmiops.workbench.notebooks.NotebooksRetryHandler;
import org.pmiops.workbench.notebooks.api.ClusterApi;
import org.pmiops.workbench.notebooks.api.NotebooksApi;
import org.pmiops.workbench.test.Providers;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.pmiops.workbench.workspaces.WorkspaceServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.retry.backoff.NoBackOffPolicy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
// The notebooks client depends on WorkspaceService, which in turn relies on many of our DAOs.
// By marking this as a DataJpaTest, the entire tree of beans required by the Leo client can be
// injected. (Alternatively, we could have explicitly mocked out the workspace service with a
// @MockBean({WorkspaceService.class}) annotation and omitted the DataJpaTest annotation.)
@DataJpaTest
public class NotebooksIntegrationTest {
  @Autowired
  private LeonardoNotebooksClient leonardoNotebooksClient;

  @TestConfiguration
  @ComponentScan("org.pmiops.workbench.notebooks")
  @Import({LeonardoNotebooksClientImpl.class, IntegrationTestConfig.class})
  static class Configuration {}

  @Test
  public void testStatus() {
    assertThat(leonardoNotebooksClient.getNotebooksStatus()).isTrue();
  }
}
