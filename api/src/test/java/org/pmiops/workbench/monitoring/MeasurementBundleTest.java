package org.pmiops.workbench.monitoring;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.pmiops.workbench.db.model.DbBillingProjectBufferEntry.BufferEntryStatus;
import org.pmiops.workbench.monitoring.attachments.Attachment;
import org.pmiops.workbench.monitoring.views.GaugeMetric;
import org.pmiops.workbench.monitoring.views.Metric;

public class MeasurementBundleTest {

  private static final long USER_COUNT = 999L;

  @Test
  public void testBuild_singleMeasurementBundle() {
    final MeasurementBundle bundle =
        MeasurementBundle.builder().addMeasurement(GaugeMetric.USER_COUNT, USER_COUNT).build();

    assertThat(bundle.getMeasurements()).hasSize(1);
    assertThat(bundle.getMeasurements().get(GaugeMetric.USER_COUNT)).isEqualTo(USER_COUNT);
    assertThat(bundle.getAttachments()).isEmpty();
  }

  @Test
  public void testBuild_multipleMeasurementsNoAttachments() {
    final ImmutableMap<Metric, Number> measurementMap =
        ImmutableMap.of(
            GaugeMetric.BILLING_BUFFER_PROJECT_COUNT, 202L,
            GaugeMetric.COHORT_COUNT, 300L,
            GaugeMetric.USER_COUNT, USER_COUNT);
    final MeasurementBundle bundle =
        MeasurementBundle.builder().addAllMeasurements(measurementMap).build();
    assertThat(bundle.getAttachments()).hasSize(measurementMap.size());
    assertThat(bundle.getMeasurements().get(GaugeMetric.USER_COUNT)).isEqualTo(USER_COUNT);
  }

  @Test(expected = IllegalStateException.class)
  public void testBuild_missingMeasurementsThrows() {
    MeasurementBundle.builder()
        .addAttachment(Attachment.USER_DISABLED, Boolean.valueOf(true).toString())
        .build();
  }

  @Test(expected = IllegalStateException.class)
  public void testBuild_incompatibleMetricAndAttachmentThrows() {
    MeasurementBundle.builder()
        .addMeasurement(GaugeMetric.COHORT_COUNT, 101L)
        .addMeasurement(GaugeMetric.BILLING_BUFFER_PROJECT_COUNT, 202L)
        .addAttachment(Attachment.BUFFER_ENTRY_STATUS, BufferEntryStatus.AVAILABLE.toString())
        .build();
  }

  @Test(expected = IllegalStateException.class)
  public void testBuild_unsupportedAttachmentValueThrows() {
    MeasurementBundle.builder()
        .addAttachment(Attachment.BUFFER_ENTRY_STATUS, "lost and gone forever")
        .build();
  }
}
