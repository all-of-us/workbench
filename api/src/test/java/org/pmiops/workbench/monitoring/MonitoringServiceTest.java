package org.pmiops.workbench.monitoring;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import com.google.common.collect.ImmutableMap;
import io.opencensus.stats.Measure.MeasureDouble;
import io.opencensus.stats.Measure.MeasureLong;
import io.opencensus.stats.MeasureMap;
import io.opencensus.stats.StatsRecorder;
import io.opencensus.stats.View;
import io.opencensus.stats.ViewManager;
import io.opencensus.tags.TagContext;
import io.opencensus.tags.TagContextBuilder;
import io.opencensus.tags.Tagger;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.pmiops.workbench.monitoring.views.EventMetric;
import org.pmiops.workbench.monitoring.views.GaugeMetric;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class MonitoringServiceTest {

  // These objects are not injected by Spring.
  @Mock private MeasureMap mockMeasureMap;
  @Mock private TagContextBuilder mockTagContextBuilder;
  @Mock private TagContext mockTagsContext;

  // TODO(jaycarlton): figure out why IntelliJ isn't copacetic with this
  // annotation but things still work.
  @Autowired private StackdriverStatsExporterService mockInitService;
  @Autowired private ViewManager mockViewManager;
  @Autowired private StatsRecorder mockStatsRecorder;
  @Autowired private Tagger mockTagger;
  @Autowired private MonitoringService monitoringService;

  @TestConfiguration
  @Import({MonitoringServiceImpl.class})
  @MockBean({
    ViewManager.class,
    StatsRecorder.class,
    StackdriverStatsExporterService.class,
    Tagger.class
  })
  static class Configuration {}

  @Before
  public void setup() {
    initMockMeasureMap();

    doReturn(mockTagsContext).when(mockTagContextBuilder).build();

    doReturn(mockTagContextBuilder).when(mockTagger).currentBuilder();
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
    monitoringService.recordEvent(EventMetric.NOTEBOOK_SAVE);
    verify(mockInitService).createAndRegister();

    // TODO(jaycarlton): include other metric types as they are re-added
    final int metricCount = GaugeMetric.values().length;
    verify(mockViewManager, times(metricCount)).registerView(any(View.class));

    verify(mockStatsRecorder).newMeasureMap();
    verify(mockMeasureMap).put(EventMetric.NOTEBOOK_SAVE.getMeasureLong(), 1L);
    verify(mockMeasureMap).record(any(TagContext.class));
  }

  @Test
  public void testRecordValue() {
    long value = 16L;
    monitoringService.recordValue(GaugeMetric.BILLING_BUFFER_PROJECT_COUNT, value);

    verify(mockInitService).createAndRegister();
    verify(mockStatsRecorder).newMeasureMap();
    verify(mockMeasureMap).put(GaugeMetric.BILLING_BUFFER_PROJECT_COUNT.getMeasureLong(), value);
    verify(mockMeasureMap).record(any(TagContext.class));
  }

  @Test
  public void testRecordMap() {
    monitoringService.recordValues(
        ImmutableMap.of(
            GaugeMetric.BILLING_BUFFER_PROJECT_COUNT, 2L,
            GaugeMetric.USER_COUNT, 33L));
    verify(mockStatsRecorder).newMeasureMap();
    verify(mockMeasureMap, times(2)).put(any(MeasureLong.class), anyLong());
    verify(mockMeasureMap).record(any(TagContext.class));
  }

  @Test
  public void testRecordValue_noOpOnEmptyMap() {
    monitoringService.recordValues(Collections.emptyMap());
    verify(mockInitService).createAndRegister();
    verifyZeroInteractions(mockStatsRecorder);
    verifyZeroInteractions(mockMeasureMap);
  }
}
