package org.pmiops.workbench.monitoring;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import com.google.common.collect.ImmutableMap;
import io.opencensus.exporter.stats.stackdriver.StackdriverStatsConfiguration;
import io.opencensus.stats.Measure.MeasureDouble;
import io.opencensus.stats.Measure.MeasureLong;
import io.opencensus.stats.MeasureMap;
import io.opencensus.stats.StatsRecorder;
import io.opencensus.stats.View;
import io.opencensus.stats.ViewManager;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.config.WorkbenchConfig.ServerConfig;
import org.pmiops.workbench.monitoring.views.MonitoringViews;
import org.pmiops.workbench.monitoring.views.OpenCensusStatsViewInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class MonitoringServiceTest {

  @Mock private MeasureMap mockMeasureMap; // not injected
  // TODO(jaycarlton): figure out why IntelliJ isn't copacetic with this
  // annotation but things still work.
  @Autowired private StackdriverStatsExporterInitializationService mockInitService;
  @Autowired private ViewManager mockViewManager;
  @Autowired private StatsRecorder mockStatsRecorder;
  @Autowired private MonitoringService monitoringService;

  @TestConfiguration
  @Import({MonitoringServiceOpenCensusImpl.class})
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
      workbenchConfig.server.shortName = "unit-test"; // should never show up in GCP for these tests
      return workbenchConfig;
    }
  }

  @Before
  public void setup() {
    initMockMeasureMap();
  }

  // We require that the measure map exists when created, and returns itself after any entry
  // is added, but we won't be using the result directly.
  private void initMockMeasureMap() {
    doReturn(mockMeasureMap).when(mockStatsRecorder).newMeasureMap();
    doReturn(mockMeasureMap).when(mockMeasureMap).put(any(MeasureLong.class), anyLong());
    doReturn(mockMeasureMap).when(mockMeasureMap).put(any(MeasureDouble.class), anyDouble());
  }

  @Test
  public void testRecordIncrement() {
    monitoringService.recordIncrement(MonitoringViews.NOTEBOOK_SAVE);

    verify(mockInitService).createAndRegister(any(StackdriverStatsConfiguration.class));
    verify(mockViewManager, times(MonitoringViews.values().length)).registerView(any(View.class));
    verify(mockStatsRecorder).newMeasureMap();
    verify(mockMeasureMap).put(MonitoringViews.NOTEBOOK_SAVE.getMeasureLong(), 1L);
    verify(mockMeasureMap).record();
  }

  @Test
  public void testRecordMap() {
    ImmutableMap.Builder<OpenCensusStatsViewInfo, Number> signalToValueBuilder =
        ImmutableMap.builder();
    signalToValueBuilder.put(MonitoringViews.BILLING_BUFFER_SIZE, 99L);
    signalToValueBuilder.put(MonitoringViews.BILLING_BUFFER_CREATING_PROJECT_COUNT, 2L);
    signalToValueBuilder.put(MonitoringViews.DEBUG_RANDOM_DOUBLE, 3.14);

    monitoringService.recordValue(signalToValueBuilder.build());
    verify(mockInitService).createAndRegister(any(StackdriverStatsConfiguration.class));
    verify(mockStatsRecorder).newMeasureMap();
    verify(mockMeasureMap, times(2)).put(any(MeasureLong.class), anyLong());
    verify(mockMeasureMap, times(1))
        .put(MonitoringViews.DEBUG_RANDOM_DOUBLE.getMeasureDouble(), 3.14);
    verify(mockMeasureMap).record();
  }

  @Test
  public void testRecordValue_noOpOnEmptyMap() {
    monitoringService.recordValue(Collections.emptyMap());
    verify(mockInitService).createAndRegister(any(StackdriverStatsConfiguration.class));
    verifyZeroInteractions(mockStatsRecorder);
    verifyZeroInteractions(mockMeasureMap);
  }
}
