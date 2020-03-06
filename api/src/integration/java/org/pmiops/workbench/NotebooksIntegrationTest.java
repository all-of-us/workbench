package org.pmiops.workbench;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.pmiops.workbench.db.dao.UserRecentResourceService;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.monitoring.LogsBasedMetricService;
import org.pmiops.workbench.monitoring.MonitoringService;
import org.pmiops.workbench.notebooks.LeonardoNotebooksClient;
import org.pmiops.workbench.notebooks.LeonardoNotebooksClientImpl;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

public class NotebooksIntegrationTest extends BaseIntegrationTest {
  @Autowired private LeonardoNotebooksClient leonardoNotebooksClient;

  // Provide a mock bean for WorkspaceService, which LeonardoNotebooksClientImpl depends on.
  @MockBean WorkspaceService mockWorkspaceService;
  @MockBean LogsBasedMetricService mockLogsBasedMetricService;
  // Provide mock beans for dependencies of NotebooksServiceImpl (which is loaded as a bean within
  // this test due to the @ComponentScan on the o.p.w.notebooks package.
  @MockBean FireCloudService mockFireCloudService;
  @MockBean UserRecentResourceService mockUserRecentResourceService;
  @MockBean MonitoringService mockMonitoringService;

  @TestConfiguration
  // N.B. in the other integration test classes we add a @ComponentScan which scans the package
  // where the class under test is defined. Adding that annotation
  @ComponentScan(basePackageClasses = LeonardoNotebooksClientImpl.class)
  @Import({LeonardoNotebooksClientImpl.class})
  static class Configuration {}

  @Test
  public void testStatus() {
    assertThat(leonardoNotebooksClient.getNotebooksStatus()).isTrue();
  }
}
