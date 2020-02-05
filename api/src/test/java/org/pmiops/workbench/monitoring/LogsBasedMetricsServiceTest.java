package org.pmiops.workbench.monitoring;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import com.google.cloud.MonitoredResource;
import com.google.cloud.logging.LogEntry;
import com.google.cloud.logging.Logging;
import com.google.cloud.logging.Payload.JsonPayload;
import com.google.cloud.logging.Payload.Type;
import com.google.cloud.logging.Severity;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.pmiops.workbench.model.DataAccessLevel;
import org.pmiops.workbench.model.WorkspaceActiveStatus;
import org.pmiops.workbench.monitoring.labels.MetricLabel;
import org.pmiops.workbench.monitoring.views.EventMetric;
import org.pmiops.workbench.monitoring.views.GaugeMetric;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class LogsBasedMetricsServiceTest {

  private static MonitoredResource MONITORED_RESOURCE =
      MonitoredResource.newBuilder("resource_type_woot")
          .addLabel("height", "3 apples tall")
          .addLabel("area_code", "90210")
          .build();
  @MockBean Logging mockLogging;
  @MockBean StackdriverStatsExporterService mockStackdriverStatsExporterService;

  @Captor ArgumentCaptor<Iterable<LogEntry>> logEntriesCaptor;
  @Autowired LogsBasedMetricService logsBasedMetricService;

  @TestConfiguration
  @Import({LogsBasedMetricServiceImpl.class})
  static class Configuration {}

  @Before
  public void settup() {
    doReturn(MONITORED_RESOURCE)
        .when(mockStackdriverStatsExporterService)
        .getLoggingMonitoredResource();
  }

  @Test
  public void testRecordMeasurementBundle_writes() {
    final MeasurementBundle gaugeMeasurement =
        MeasurementBundle.builder()
            .addMeasurement(GaugeMetric.WORKSPACE_COUNT, 3)
            .addTag(MetricLabel.DATA_ACCESS_LEVEL, DataAccessLevel.PROTECTED.toString())
            .addTag(MetricLabel.WORKSPACE_ACTIVE_STATUS, WorkspaceActiveStatus.ACTIVE.toString())
            .build();
    logsBasedMetricService.record(gaugeMeasurement);
    verify(mockLogging).write(logEntriesCaptor.capture());
    List<LogEntry> sentEntries =
        StreamSupport.stream(logEntriesCaptor.getValue().spliterator(), false)
            .collect(Collectors.toList());
    assertThat(sentEntries).hasSize(1);

    final LogEntry logEntry = sentEntries.get(0);
    assertThat(logEntry.getPayload().getType()).isEqualTo(Type.JSON);
    assertThat(logEntry.getResource()).isEqualTo(MONITORED_RESOURCE);
    assertThat(logEntry.getSeverity()).isEqualTo(Severity.INFO);

    final Map<String, Object> payloadMap = logEntry.<JsonPayload>getPayload().getDataAsMap();
    assertThat(payloadMap).hasSize(3);

    final String metricName =
        (String) payloadMap.getOrDefault(LogsBasedMetricService.METRIC_NAME_KEY, "");
    assertThat(metricName).isEqualTo(GaugeMetric.WORKSPACE_COUNT.getName());

    final Double metricValue =
        (Double) payloadMap.getOrDefault(LogsBasedMetricService.METRIC_VALUE_KEY, "");
    assertThat(metricValue).isEqualTo(3.0);

    @SuppressWarnings("unchecked")
    final Map<String, String> labelToValue =
        (Map<String, String>)
            payloadMap.getOrDefault(LogsBasedMetricService.METRIC_LABELS_KEY, ImmutableMap.of());
    assertThat(labelToValue).hasSize(2);
    assertThat(labelToValue.get(MetricLabel.DATA_ACCESS_LEVEL.getName()))
        .isEqualTo(DataAccessLevel.PROTECTED.toString());
  }

  @Test
  public void testRecord_handlesMultipleMeasurements() {
    final MeasurementBundle measurements =
        MeasurementBundle.builder()
            .addEvent(EventMetric.NOTEBOOK_CLONE)
            .addEvent(EventMetric.NOTEBOOK_DELETE)
            .build();
    logsBasedMetricService.record(measurements);
    verify(mockLogging).write(logEntriesCaptor.capture());
    List<LogEntry> sentEntries =
        StreamSupport.stream(logEntriesCaptor.getValue().spliterator(), false)
            .collect(Collectors.toList());
    assertThat(sentEntries).hasSize(2);

    final LogEntry logEntry = sentEntries.get(0);
    assertThat(logEntry.getPayload().getType()).isEqualTo(Type.JSON);
    assertThat(logEntry.getResource()).isEqualTo(MONITORED_RESOURCE);
    assertThat(logEntry.getSeverity()).isEqualTo(Severity.INFO);

    final ImmutableSet<String> metricNames = sentEntries.stream()
        .map(e -> (JsonPayload) e.getPayload())
        .map(JsonPayload::getDataAsMap)
        .map(m -> (String) m.get(LogsBasedMetricService.METRIC_NAME_KEY))
        .filter(Objects::nonNull)
        .collect(ImmutableSet.toImmutableSet());
    assertThat(metricNames).containsAllIn(ImmutableSet.of(EventMetric.NOTEBOOK_CLONE.getName(),
        EventMetric.NOTEBOOK_DELETE.getName()));
  }
}
