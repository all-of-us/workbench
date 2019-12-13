package org.pmiops.workbench.monitoring;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableMap;
import java.time.Clock;
import java.util.Collections;
import java.util.Map;
import javax.management.monitor.Monitor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.pmiops.workbench.billing.BillingProjectBufferService;
import org.pmiops.workbench.cohortreview.CohortReviewService;
import org.pmiops.workbench.cohorts.CohortCloningService;
import org.pmiops.workbench.monitoring.views.MonitoringViews;
import org.pmiops.workbench.monitoring.views.OpenCensusStatsViewInfo;
import org.pmiops.workbench.notebooks.NotebooksService;
import org.pmiops.workbench.notebooks.NotebooksServiceImpl;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.pmiops.workbench.workspaces.WorkspaceServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class GaugeRecorderServiceTest {

  private static final Map<OpenCensusStatsViewInfo, Number> BILLING_BUFFER_GAUGE_MAP =
      ImmutableMap.of(MonitoringViews.BILLING_BUFFER_ASSIGNING_PROJECT_COUNT, 2L,
          MonitoringViews.BILLING_BUFFER_AVAILABLE_PROJECT_COUNT, 1L,
          MonitoringViews.BILLING_BUFFER_CREATING_PROJECT_COUNT, 0L);
  private static final long WORKSPACES_COUNT = 101L;
  private static final Map<OpenCensusStatsViewInfo, Number> WORKSPACE_GAUGE_MAP =
      ImmutableMap.of(MonitoringViews.WORKSPACE_TOTAL_COUNT, WORKSPACES_COUNT);

  @Captor private ArgumentCaptor<Map<OpenCensusStatsViewInfo, Number>> gaugeDataCaptor;

  // In order to access the GaugeDataCollector method, we need to mock the
  // Implementation classes for the ones that implement it. We could get
  // around this using Qualifiers if we wanted, but we only really care in
  // this test class.
  @Autowired private BillingProjectBufferService mockBillingProjectBufferService;
  @Autowired private WorkspaceServiceImpl mockWorkspaceServiceImpl;
  @Autowired private MonitoringService mockMonitoringService;
  @Autowired private GaugeRecorderService gaugeRecorderService;

  @TestConfiguration
  @Import({
      GaugeRecorderService.class
  })
  @MockBean({
      BillingProjectBufferService.class,
      CohortReviewService.class,
      MonitoringService.class,
      WorkspaceServiceImpl.class
  })
  public static class Config {
    @Bean
    public Clock getClock() {
      return new FakeClock();
    }
  }

  @Before
  public void setup() {
    doReturn(WORKSPACE_GAUGE_MAP).when(mockWorkspaceServiceImpl).getGaugeData();
    doReturn(BILLING_BUFFER_GAUGE_MAP).when(mockBillingProjectBufferService).getGaugeData();
  }

  @Test
  public void testRecord() {
    final Map<OpenCensusStatsViewInfo, Number> result = gaugeRecorderService.record();

    verify(mockMonitoringService).recordValues(gaugeDataCaptor.capture());
    verify(mockWorkspaceServiceImpl).getGaugeData();
    verify(mockBillingProjectBufferService).getGaugeData();

    assertThat(result)
        .hasSize(WORKSPACE_GAUGE_MAP.size() + BILLING_BUFFER_GAUGE_MAP.size());
    assertThat(gaugeDataCaptor.getValue()).isEqualTo(result);
    assertThat(result.get(MonitoringViews.WORKSPACE_TOTAL_COUNT)).isEqualTo(WORKSPACES_COUNT);
  }

  @Test
  public void testRecord_emptyMap() {
    doReturn(Collections.emptyMap()).when(mockWorkspaceServiceImpl).getGaugeData();
    doReturn(Collections.emptyMap()).when(mockBillingProjectBufferService).getGaugeData();

    assertThat(gaugeRecorderService.record()).isEmpty();

    verify(mockMonitoringService).recordValues(gaugeDataCaptor.capture());
    verify(mockWorkspaceServiceImpl).getGaugeData();
    verify(mockBillingProjectBufferService).getGaugeData();

    assertThat(gaugeDataCaptor.getValue()).isEmpty();
  }
}
