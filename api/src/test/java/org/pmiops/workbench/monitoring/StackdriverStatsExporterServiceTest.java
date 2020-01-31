package org.pmiops.workbench.monitoring;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.google.api.MonitoredResource;
import com.google.appengine.api.modules.ModulesService;
import io.opencensus.exporter.stats.stackdriver.StackdriverStatsConfiguration;
import javax.inject.Provider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.config.CacheSpringConfiguration;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.config.WorkbenchConfig.ServerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class StackdriverStatsExporterServiceTest {

  private static final String PROJECT_ID = "fake-project";
  @Autowired private StackdriverStatsExporterService exporterService;

  @TestConfiguration
  @Import(StackdriverStatsExporterService.class)
  @MockBean({ModulesService.class})
  static class Configuration {
    @Bean(name = CacheSpringConfiguration.WORKBENCH_CONFIG_SINGLETON)
    public WorkbenchConfig workbenchConfig() {
      final WorkbenchConfig workbenchConfig = new WorkbenchConfig();
      // Nothing should show up in any Metrics backend for these tests.
      workbenchConfig.server = new ServerConfig();
      workbenchConfig.server.shortName = "unit-test";
      workbenchConfig.server.projectId = PROJECT_ID;
      workbenchConfig.server.appEngineLocationId = "moon-luna-1";
      return workbenchConfig;
    }

    // In order to mock a value class that's injected via a Provider, I believe
    // this is still the best way to do it (assuming I'm OK with Singleton scope, which
    // is what I want here).
    @Bean
    public MonitoredResource getMonitoredResource(
        @Qualifier(CacheSpringConfiguration.WORKBENCH_CONFIG_SINGLETON)
            Provider<WorkbenchConfig> workbenchConfigProvider,
        @Qualifier(MonitoringSpringConfiguration.APP_ENGINE_NODE_ID)
            Provider<String> appEngineNodeIdProvider) {
      final MonitoredResource mockMonitoredResource = mock(MonitoredResource.class);
      doReturn("generic_node").when(mockMonitoredResource).getType();
      return mockMonitoredResource;
    }
  }

  @Test
  public void testMakeConfiguration() {
    StackdriverStatsConfiguration statsConfiguration =
        exporterService.makeStackdriverStatsConfiguration();
    assertThat(statsConfiguration.getProjectId()).isEqualTo(PROJECT_ID);
    assertThat(statsConfiguration.getMetricNamePrefix()).isEqualTo("custom.googleapis.com/");

    final MonitoredResource monitoredResource = statsConfiguration.getMonitoredResource();
    assertThat(monitoredResource.getType()).isEqualTo("generic_node");
  }
}
