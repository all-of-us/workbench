package org.pmiops.workbench.monitoring;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.verify;

import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.pmiops.workbench.cohortreview.CohortReviewService;
import org.pmiops.workbench.monitoring.views.OpenCensusStatsViewInfo;
import org.pmiops.workbench.notebooks.NotebooksService;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.pmiops.workbench.workspaces.WorkspaceServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class GaugeRecorderServiceTest {

  @Captor
  private ArgumentCaptor<Map<OpenCensusStatsViewInfo, Number>> gaugeDataCaptor;

  @Autowired private MonitoringService mockMonitoringService;
  @Autowired private WorkspaceServiceImpl mockWorkspaceServiceImpl;
  @Autowired private GaugeRecorderService gaugeRecorderService;

  @TestConfiguration
  @MockBean({
      CohortReviewService.class,
      NotebooksService.class,
      MonitoringService.class,
      WorkspaceService.class
  })
  public static class Config { }

  @Before
  public void setup() {
  }

  @Test
  public void testRecord() {
    gaugeRecorderService.record();
    verify(mockWorkspaceServiceImpl).getGaugeData();
    verify(mockMonitoringService).recordValues(gaugeDataCaptor.capture());

    assertThat(gaugeDataCaptor.getValue()).hasSize(5);
  }
}
