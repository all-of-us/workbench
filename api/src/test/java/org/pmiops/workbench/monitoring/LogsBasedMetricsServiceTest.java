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
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.pmiops.workbench.model.DataAccessLevel;
import org.pmiops.workbench.model.WorkspaceActiveStatus;
import org.pmiops.workbench.monitoring.LogsBasedMetricServiceImpl.PayloadKey;
import org.pmiops.workbench.monitoring.labels.MetricLabel;
import org.pmiops.workbench.monitoring.views.DistributionMetric;
import org.pmiops.workbench.monitoring.views.EventMetric;
import org.pmiops.workbench.monitoring.views.GaugeMetric;
import org.pmiops.workbench.monitoring.views.UnitOfMeasure;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class LogsBasedMetricsServiceTest {

  private static final Duration OPERATION_DURATION = Duration.ofMillis(15);
  private static MonitoredResource MONITORED_RESOURCE =
      MonitoredResource.newBuilder("resource_type_woot")
          .addLabel("height", "3 apples tall")
          .addLabel("area_code", "90210")
          .build();
  @MockBean Logging mockLogging;
  @MockBean StackdriverStatsExporterService mockStackdriverStatsExporterService;
  @MockBean Stopwatch mockStopwatch;

  @Captor ArgumentCaptor<Iterable<LogEntry>> logEntriesCaptor;
  @Autowired LogsBasedMetricService logsBasedMetricService;

  @TestConfiguration
  @Import({LogsBasedMetricServiceImpl.class})
  static class Configuration {}

  @Before
  public void setup() {
    doReturn(MONITORED_RESOURCE)
        .when(mockStackdriverStatsExporterService)
        .getLoggingMonitoredResource();
    doReturn(OPERATION_DURATION).when(mockStopwatch).elapsed();
    doReturn(mockStopwatch).when(mockStopwatch).start();
    doReturn(mockStopwatch).when(mockStopwatch).stop();
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
    List<LogEntry> sentEntries = getWrittenLogEntries();
    assertThat(sentEntries).hasSize(1);

    final LogEntry logEntry = sentEntries.get(0);
    assertThat(logEntry.getPayload().getType()).isEqualTo(Type.JSON);
    assertThat(logEntry.getResource()).isEqualTo(MONITORED_RESOURCE);
    assertThat(logEntry.getSeverity()).isEqualTo(Severity.INFO);

    final Map<String, Object> payloadMap = logEntry.<JsonPayload>getPayload().getDataAsMap();
    assertThat(payloadMap).hasSize(PayloadKey.values().length);

    final String metricName = (String) payloadMap.getOrDefault(PayloadKey.NAME.getKeyName(), "");
    assertThat(metricName).isEqualTo(GaugeMetric.WORKSPACE_COUNT.getName());

    final Double metricValue = (Double) payloadMap.getOrDefault(PayloadKey.VALUE.getKeyName(), "");
    assertThat(metricValue).isEqualTo(3.0);

    @SuppressWarnings("unchecked")
    final Map<String, String> labelToValue =
        (Map<String, String>)
            payloadMap.getOrDefault(PayloadKey.LABELS.getKeyName(), ImmutableMap.of());
    assertThat(labelToValue).hasSize(2);
    assertThat(labelToValue.get(MetricLabel.DATA_ACCESS_LEVEL.getName()))
        .isEqualTo(DataAccessLevel.PROTECTED.toString());
  }

  @NotNull
  private List<LogEntry> getWrittenLogEntries() {
    verify(mockLogging).write(logEntriesCaptor.capture());
    return StreamSupport.stream(logEntriesCaptor.getValue().spliterator(), false)
        .collect(Collectors.toList());
  }

  @Test
  public void testRecord_handlesMultipleMeasurements() {
    final MeasurementBundle measurements =
        MeasurementBundle.builder()
            .addEvent(EventMetric.NOTEBOOK_CLONE)
            .addEvent(EventMetric.NOTEBOOK_DELETE)
            .build();
    logsBasedMetricService.record(measurements);
    List<LogEntry> sentEntries = getWrittenLogEntries();
    assertThat(sentEntries).hasSize(2);

    final LogEntry logEntry = sentEntries.get(0);
    assertThat(logEntry.getPayload().getType()).isEqualTo(Type.JSON);
    assertThat(logEntry.getResource()).isEqualTo(MONITORED_RESOURCE);
    assertThat(logEntry.getSeverity()).isEqualTo(Severity.INFO);

    final ImmutableSet<String> metricNames =
        sentEntries.stream()
            .map(e -> (JsonPayload) e.getPayload())
            .map(JsonPayload::getDataAsMap)
            .map(m -> (String) m.get(PayloadKey.NAME.getKeyName()))
            .filter(Objects::nonNull)
            .collect(ImmutableSet.toImmutableSet());
    assertThat(metricNames)
        .containsAllIn(
            ImmutableSet.of(
                EventMetric.NOTEBOOK_CLONE.getName(), EventMetric.NOTEBOOK_DELETE.getName()));
  }

  @Test
  public void testTimeAndRecordWithRunnable() {
    Set<Integer> sideEffectSet = new HashSet<>();
    logsBasedMetricService.recordElapsedTime(
        MeasurementBundle.builder().addTag(MetricLabel.OPERATION_NAME, "test1"),
        DistributionMetric.WORKSPACE_OPERATION_TIME,
        () -> {
          int innerInt = 2;
          sideEffectSet.add(3);
          innerInt -= 3;
          assertThat(innerInt).isEqualTo(-1);
        });
    assertThat(sideEffectSet).contains(3);
    verify(mockLogging).write(logEntriesCaptor.capture());
    final Map<String, Object> entryData =
        StreamSupport.stream(logEntriesCaptor.getValue().spliterator(), false)
            .map(LogEntry::getPayload)
            .map(p -> (JsonPayload) p)
            .map(JsonPayload::getDataAsMap)
            .findFirst()
            .orElse(Collections.emptyMap());
    assertThat(entryData).hasSize(4);
    assertThat(entryData.get(PayloadKey.NAME.getKeyName()))
        .isEqualTo(DistributionMetric.WORKSPACE_OPERATION_TIME.getName());
    assertThat((double) entryData.get(PayloadKey.VALUE.getKeyName()))
        .isEqualTo((double) OPERATION_DURATION.toMillis());
  }

  public void testTimeAndRecordWithSupplier() {
    Set<Integer> aSet = new HashSet<>();
    final int result =
        logsBasedMetricService.recordElapsedTime(
            MeasurementBundle.builder().addTag(MetricLabel.OPERATION_NAME, "test1"),
            DistributionMetric.WORKSPACE_OPERATION_TIME,
            () -> {
              return 99;
            });
    assertThat(aSet).contains(3);
    assertThat(result).isEqualTo(99);

    verify(mockLogging).write(logEntriesCaptor.capture());
    final Map<String, Object> entryData =
        StreamSupport.stream(logEntriesCaptor.getValue().spliterator(), false)
            .map(LogEntry::getPayload)
            .map(p -> (JsonPayload) p)
            .map(JsonPayload::getDataAsMap)
            .findFirst()
            .orElse(Collections.emptyMap());
    assertThat(entryData).hasSize(4);
    assertThat(entryData.get(PayloadKey.NAME.getKeyName()))
        .isEqualTo(DistributionMetric.WORKSPACE_OPERATION_TIME.getName());
    assertThat(entryData.get(PayloadKey.UNIT.getKeyName()))
        .isEqualTo(UnitOfMeasure.MILLISECOND.getUcmSymbol());

    assertThat((Double) entryData.get(PayloadKey.VALUE.getKeyName()))
        .isEqualTo(OPERATION_DURATION.toMillis());
  }

  @Test(expected = IllegalAccessError.class)
  public void testRecordElapsedTime_throws() {
    logsBasedMetricService.recordElapsedTime(
        MeasurementBundle.builder(),
        DistributionMetric.COHORT_OPERATION_TIME,
        () -> {
          throw new IllegalAccessError("Boo!");
        });
  }

  @Test
  public void testRecordElapsedTime_nestedWorks() {
    final Set<Integer> someSet = new HashSet<>();
    final Duration innerTime = Duration.ofMillis(30);
    final Duration outerTime = Duration.ofMillis(100);

    logsBasedMetricService.recordElapsedTime(
        MeasurementBundle.builder(),
        DistributionMetric.UNIFORM_RANDOM_SAMPLE,
        () -> {
          doReturn(innerTime).when(mockStopwatch).elapsed();
          someSet.add(1);
          someSet.add(3);
          Boolean b =
              logsBasedMetricService.recordElapsedTime(
                  MeasurementBundle.builder(),
                  DistributionMetric.WORKSPACE_OPERATION_TIME,
                  () -> {
                    someSet.add(2);
                    someSet.remove(3);
                    return true;
                  });
          final List<LogEntry> logEntries = getWrittenLogEntries();
          assertThat(logEntries).hasSize(1);
          assertThat(b).isTrue();

          final Map<String, Object> payloadMap =
              ((JsonPayload) logEntries.get(0).getPayload()).getDataAsMap();
          assertThat(payloadMap).hasSize(PayloadKey.values().length);
          assertThat(payloadMap.get(PayloadKey.VALUE.getKeyName()))
              .isEqualTo((double) innerTime.toMillis());

          // So even though the stopwatch is injected as a prototype,  this test
          // is using the same object for both invocations (in the same call stack).  Thus,
          // we need to reset  its state here.
          Mockito.reset(mockLogging);
          doReturn(outerTime).when(mockStopwatch).elapsed();
          doReturn(mockStopwatch).when(mockStopwatch).start();
          doReturn(mockStopwatch).when(mockStopwatch).stop();
        });
    assertThat(someSet).containsAllIn(ImmutableSet.of(1, 2));
    final List<LogEntry> logEntries = getWrittenLogEntries();
    assertThat(logEntries).hasSize(1);

    final Map<String, Object> payloadMap =
        ((JsonPayload) logEntries.get(0).getPayload()).getDataAsMap();
    assertThat(payloadMap).hasSize(PayloadKey.values().length);
    assertThat(payloadMap.get(PayloadKey.VALUE.getKeyName()))
        .isEqualTo((double) outerTime.toMillis());
    assertThat(payloadMap.get(PayloadKey.NAME.getKeyName()))
        .isEqualTo(DistributionMetric.UNIFORM_RANDOM_SAMPLE.getName());
  }
}
