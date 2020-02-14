package org.pmiops.workbench.monitoring;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableList;
import java.time.Clock;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.pmiops.workbench.billing.BillingProjectBufferService;
import org.pmiops.workbench.cohortreview.CohortReviewService;
import org.pmiops.workbench.db.model.DbBillingProjectBufferEntry.BufferEntryStatus;
import org.pmiops.workbench.monitoring.labels.MetricLabel;
import org.pmiops.workbench.monitoring.views.GaugeMetric;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.workspaces.WorkspaceServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class GaugeRecorderServiceTest {

  private static final List<MeasurementBundle> BILLING_BUFFER_GAUGE_BUNDLES =
      ImmutableList.of(
          MeasurementBundle.builder()
              .addMeasurement(GaugeMetric.BILLING_BUFFER_PROJECT_COUNT, 22L)
              .addTag(MetricLabel.BUFFER_ENTRY_STATUS, BufferEntryStatus.AVAILABLE.toString())
              .build(),
          MeasurementBundle.builder()
              .addMeasurement(GaugeMetric.BILLING_BUFFER_PROJECT_COUNT, 3L)
              .addTag(MetricLabel.BUFFER_ENTRY_STATUS, BufferEntryStatus.CREATING.toString())
              .build());

  private static final long WORKSPACES_COUNT = 101L;
  private static final MeasurementBundle WORKSPACE_MEASUREMENT_BUNDLE =
      MeasurementBundle.builder()
          .addMeasurement(GaugeMetric.WORKSPACE_COUNT, WORKSPACES_COUNT)
          .build();
  private static final String TEST_GAUGE_DATA_COLLECTOR = "test gauge data collector";

  @Captor private ArgumentCaptor<MeasurementBundle> measurementBundleCaptor;
  @Captor private ArgumentCaptor<Collection<MeasurementBundle>> measurementBundlesListCaptor;

  // In order to access the GaugeDataCollector method, we need to mock the
  // Implementation classes for the ones that implement it. We could get
  // around this using Qualifiers if we wanted, but we only really care in
  // this test class.
  @Autowired private BillingProjectBufferService mockBillingProjectBufferService;
  @Autowired private WorkspaceServiceImpl mockWorkspaceServiceImpl;
  @Autowired private MonitoringService mockMonitoringService;

  @Autowired
  @Qualifier(TEST_GAUGE_DATA_COLLECTOR)
  private GaugeDataCollector standAloneGaugeDataCollector;

  @Autowired private GaugeRecorderService gaugeRecorderService;

  @TestConfiguration
  @Import({GaugeRecorderService.class,
    LogsBasedMetricServiceFakeImpl.class})
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

    /**
     * This is "yet another" GaugeDataCollector implementation, meant to showcase how we just grab
     * all of the implementers nad inject them into the GaugeRecorderService, and that the two
     * services I called out above aren't a special or all-inclusive list.
     *
     * @return
     */
    @Bean(name = TEST_GAUGE_DATA_COLLECTOR)
    public GaugeDataCollector getGaugeDataCollector() {
      return () ->
          Collections.singleton(
              MeasurementBundle.builder().addMeasurement(GaugeMetric.DATASET_COUNT, 999L).build());
    }
  }

  @Before
  public void setup() {
    doReturn(Collections.singleton(WORKSPACE_MEASUREMENT_BUNDLE))
        .when(mockWorkspaceServiceImpl)
        .getGaugeData();
    doReturn(BILLING_BUFFER_GAUGE_BUNDLES).when(mockBillingProjectBufferService).getGaugeData();
  }

  @Test
  public void testStandAloneBeanInitialized() {
    assertThat(standAloneGaugeDataCollector).isNotNull();
    final Collection<MeasurementBundle> bundles = standAloneGaugeDataCollector.getGaugeData();
    assertThat(bundles).isNotEmpty();
  }

  @Test
  public void testRecord() {
    gaugeRecorderService.record();

    verify(mockMonitoringService, atLeast(1)).recordBundles(measurementBundlesListCaptor.capture());
    verify(mockWorkspaceServiceImpl).getGaugeData();
    verify(mockBillingProjectBufferService).getGaugeData();

    final List<Collection<MeasurementBundle>> allRecordedBundles =
        measurementBundlesListCaptor.getAllValues();
    final int expectedSize =
        mockWorkspaceServiceImpl.getGaugeData().size()
            + mockBillingProjectBufferService.getGaugeData().size()
            + standAloneGaugeDataCollector.getGaugeData().size();
    final int flatSize =
        allRecordedBundles.stream().map(Collection::size).mapToInt(Integer::valueOf).sum();
    assertThat(flatSize).isEqualTo(expectedSize);

    final Optional<MeasurementBundle> workspacesBundle =
        allRecordedBundles.stream()
            .flatMap(Collection::stream)
            .filter(b -> b.getMeasurements().containsKey(GaugeMetric.WORKSPACE_COUNT))
            .findFirst();

    assertThat(
            workspacesBundle.map(MeasurementBundle::getMeasurements).orElse(Collections.emptyMap()))
        .hasSize(1);
    assertThat(
            workspacesBundle
                .map(wb -> wb.getMeasurements().get(GaugeMetric.WORKSPACE_COUNT))
                .orElse(0))
        .isEqualTo(WORKSPACES_COUNT);
  }
}
