package org.pmiops.workbench.monitoring;

import io.opencensus.stats.StatsRecorder;
import io.opencensus.stats.ViewManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.config.WorkbenchConfig.ServerConfig;
import org.pmiops.workbench.monitoring.views.MonitoringViews;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class MonigoringServiceTest {

  @Autowired private MonitoringService monitoringService;

  @TestConfiguration
  @Import({MonitoringServiceStackdriverImpl.class})
  @MockBean({
    ViewManager.class,
    StatsRecorder.class,
    StackdriverStatsExporterInitializationService.class
  })
  static class Configuration {

    @Bean
    public WorkbenchConfig workbenchConfig() {
      final WorkbenchConfig workbenchConfig = new WorkbenchConfig();
      workbenchConfig.server = new ServerConfig();
      workbenchConfig.server.projectId = "aou-gcp-project";
      workbenchConfig.server.shortName = "unit-test";
      return workbenchConfig;
    }
  }

  @Test
  public void testRecordIncrement() {
    monitoringService.recordIncrement(MonitoringViews.NOTEBOOK_SAVE);
  }
}
