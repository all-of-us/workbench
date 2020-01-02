package org.pmiops.workbench.monitoring;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.time.Clock;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.pmiops.workbench.billing.BillingProjectBufferService;
import org.pmiops.workbench.cohortreview.CohortReviewService;
import org.pmiops.workbench.db.model.DbBillingProjectBufferEntry.BufferEntryStatus;
import org.pmiops.workbench.monitoring.attachments.AttachmentKey;
import org.pmiops.workbench.monitoring.views.Metric;
import org.pmiops.workbench.monitoring.views.OpenCensusView;
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

  private static final Map<OpenCensusView, Number> BILLING_BUFFER_INDIVIDUAL_GAUGE_MAP =
      ImmutableMap.of(
          Metric.BILLING_BUFFER_ASSIGNING_PROJECT_COUNT,
          2L,
          Metric.BILLING_BUFFER_AVAILABLE_PROJECT_COUNT,
          1L,
          Metric.BILLING_BUFFER_CREATING_PROJECT_COUNT,
          0L);

  private static final List<MeasurementBundle> BILLING_BUFFER_GAUGE_BUNDLES =
      ImmutableList.of(
          MeasurementBundle.builder().addAll(BILLING_BUFFER_INDIVIDUAL_GAUGE_MAP).build(),
          MeasurementBundle.builder()
              .addDelta(Metric.BILLING_BUFFER_COUNT_BY_STATUS, 22L)
              .attach(AttachmentKey.BUFFER_ENTRY_STATUS, BufferEntryStatus.AVAILABLE.toString())
              .build(),
          MeasurementBundle.builder()
              .addDelta(Metric.BILLING_BUFFER_COUNT_BY_STATUS, 3L)
              .attach(AttachmentKey.BUFFER_ENTRY_STATUS, BufferEntryStatus.CREATING.toString())
              .build());

  private static final long WORKSPACES_COUNT = 101L;
  private static final MeasurementBundle WORKSPACE_MEASUREMENT_BUNDLE =
      MeasurementBundle.builder().addDelta(Metric.WORKSPACE_TOTAL_COUNT, WORKSPACES_COUNT).build();
  public static final String TEST_GAUGE_DATA_COLLECTOR = "test gauge data collector";

  @Captor private ArgumentCaptor<MeasurementBundle> measurementBundleCaptor;
  @Captor private ArgumentCaptor<Collection<MeasurementBundle>> gaugeDataCaptor;

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
  @Import({GaugeRecorderService.class})
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
              MeasurementBundle.builder()
                  .addDelta(Metric.DATASET_COUNT, 999L)
                  .addDelta(Metric.DEBUG_CONSTANT_VALUE, 100L)
                  .build());
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

    verify(mockMonitoringService, atLeast(1)).recordBundle(measurementBundleCaptor.capture());
    verify(mockWorkspaceServiceImpl).getGaugeData();
    verify(mockBillingProjectBufferService).getGaugeData();

    final List<MeasurementBundle> allRecordedBundles = measurementBundleCaptor.getAllValues();
    final int expectedSize =
        mockWorkspaceServiceImpl.getGaugeData().size()
            + mockBillingProjectBufferService.getGaugeData().size()
            + standAloneGaugeDataCollector.getGaugeData().size();
    assertThat(allRecordedBundles).hasSize(expectedSize);

    final Optional<MeasurementBundle> workspacesBundle =
        allRecordedBundles.stream()
            .filter(b -> b.getMeasurements().containsKey(Metric.WORKSPACE_TOTAL_COUNT))
            .findFirst();

    assertThat(
            workspacesBundle.map(MeasurementBundle::getMeasurements).orElse(Collections.emptyMap()))
        .hasSize(1);
    assertThat(workspacesBundle.get().getMeasurements().get(Metric.WORKSPACE_TOTAL_COUNT));
  }
}
