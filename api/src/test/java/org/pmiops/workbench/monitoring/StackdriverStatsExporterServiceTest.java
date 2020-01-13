package org.pmiops.workbench.monitoring;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

import com.google.api.MonitoredResource;
import com.google.appengine.api.modules.ModulesException;
import com.google.appengine.api.modules.ModulesService;
import io.opencensus.exporter.stats.stackdriver.StackdriverStatsConfiguration;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.config.WorkbenchConfig.ServerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class StackdriverStatsExporterServiceTest {

  private static final String PROJECT_ID = "fake-project";
  private static final String FOUND_NODE_ID = "node-11001001";
  @Autowired private ModulesService mockModulesService;
  @Autowired private StackdriverStatsExporterService exporterService;

  @TestConfiguration
  @Import(StackdriverStatsExporterService.class)
  @MockBean(ModulesService.class)
  static class Configuration {
    @Bean
    public WorkbenchConfig workbenchConfig() {
      final WorkbenchConfig workbenchConfig = new WorkbenchConfig();
      // Nothing should show up in any Metrics backend for these tests.
      workbenchConfig.server = new ServerConfig();
      workbenchConfig.server.shortName = "unit-test";
      workbenchConfig.server.projectId = PROJECT_ID;
      workbenchConfig.server.appEngineLocationId = "moon-luna-1";
      return workbenchConfig;
    }
  }

  @Test
  public void testMakeConfiguration() {
    doReturn(FOUND_NODE_ID).when(mockModulesService).getCurrentInstanceId();

    StackdriverStatsConfiguration statsConfiguration =
        exporterService.makeStackdriverStatsConfiguration();
    assertThat(statsConfiguration.getProjectId()).isEqualTo(PROJECT_ID);
    assertThat(statsConfiguration.getMetricNamePrefix()).isEqualTo("custom.googleapis.com/");

    final MonitoredResource monitoredResource = statsConfiguration.getMonitoredResource();
    assertThat(monitoredResource.getType()).isEqualTo("generic_node");

    final Map<String, String> labelToValue = monitoredResource.getLabelsMap();
    assertThat(statsConfiguration.getMonitoredResource().getLabelsMap()).isNotEmpty();
    assertThat(labelToValue.get("node_id")).isEqualTo(FOUND_NODE_ID);
  }

  @Test
  public void testMakeMonitoredResource_noInstanceIdAvailable() {
    doThrow(ModulesException.class).when(mockModulesService).getCurrentInstanceId();
    final MonitoredResource monitoredResource =
        exporterService.makeStackdriverStatsConfiguration().getMonitoredResource();
    assertThat(monitoredResource.getLabelsMap().get("node_id")).isNotEmpty();
  }
}
