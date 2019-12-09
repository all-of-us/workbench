package org.pmiops.workbench.monitoring;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.opencensus.exporter.stats.stackdriver.StackdriverStatsConfiguration;
import io.opencensus.stats.Measure.MeasureDouble;
import io.opencensus.stats.Measure.MeasureLong;
import io.opencensus.stats.MeasureMap;
import io.opencensus.stats.StatsRecorder;
import io.opencensus.stats.View;
import io.opencensus.stats.ViewManager;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.config.WorkbenchConfig.ServerConfig;
import org.pmiops.workbench.monitoring.views.MonitoringViews;
import org.pmiops.workbench.monitoring.views.StatsViewProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class MonitoringServiceTest {

  @Mock private MeasureMap mockMeasureMap;
  // TODO(jaycarlton): figure out why IntelliJ isn't copacetic with this
  // annotation but things still work.
  @Autowired private StackdriverStatsExporterInitializationService
      mockStackdriverStatsExporterInitializationService;
  @Autowired private ViewManager mockViewManager;
  @Autowired private StatsRecorder mockStatsRecorder;
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

  @Before
  public void setup() {
    doReturn(mockMeasureMap).when(mockStatsRecorder).newMeasureMap();
    doReturn(mockMeasureMap)
        .when(mockMeasureMap)
        .put(any(MeasureLong.class), anyLong());
    doReturn(mockMeasureMap)
        .when(mockMeasureMap)
        .put(any(MeasureDouble.class), anyDouble());
  }
  @Test
  public void testRecordIncrement() {
    monitoringService.recordIncrement(MonitoringViews.NOTEBOOK_SAVE);
    verify(mockStatsRecorder).newMeasureMap();
    verify(mockStackdriverStatsExporterInitializationService)
        .createAndRegister(any(StackdriverStatsConfiguration.class));
    final int registerCount = MonitoringViews.values().length;
    verify(mockViewManager, times(registerCount))
        .registerView(any(View.class));
    verify(mockMeasureMap).record();
//    final Map<Class, Integer> measureClassToCount =
//        Arrays.stream(MonitoringViews.values())
//          .collect(Collectors.groupingBy(
//              StatsViewProperties::getMeasureClass,
//              Collectors.summingInt(v -> 1)));
//    final int longCount = measureClassToCount.get(MeasureLong.class);
    verify(mockMeasureMap).put(any(MeasureLong.class), anyLong());
  }
}
