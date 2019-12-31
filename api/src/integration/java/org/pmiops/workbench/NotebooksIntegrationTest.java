package org.pmiops.workbench;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserRecentResourceService;
import org.pmiops.workbench.firecloud.FireCloudService;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.retry.backoff.NoBackOffPolicy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class NotebooksIntegrationTest {
  @Autowired
  private LeonardoNotebooksClient leonardoNotebooksClient;

  @TestConfiguration
  @Import({LeonardoNotebooksClientImpl.class, IntegrationTestConfig.class})
  @MockBean({
      // Mock out the workspace service
      WorkspaceService.class
  })
  static class Configuration {}

  @Test
  public void testStatus() {
    assertThat(leonardoNotebooksClient.getNotebooksStatus()).isTrue();
  }
}
