package org.pmiops.workbench.monitoring;

import static com.google.common.truth.Truth.assertThat;

import java.util.Arrays;
import org.junit.Test;
import org.pmiops.workbench.db.model.DbBillingProjectBufferEntry.BufferEntryStatus;
import org.pmiops.workbench.monitoring.attachments.Attachment;
import org.pmiops.workbench.monitoring.views.GaugeMetric;

public class AttachmentTest {

  @Test
  public void testBufferEntryStatus() {
    assertThat(Attachment.BUFFER_ENTRY_STATUS.getAllowedMetrics())
        .contains(GaugeMetric.BILLING_BUFFER_PROJECT_COUNT);
    assertThat(Attachment.BUFFER_ENTRY_STATUS.supportsMetric(GaugeMetric.COHORT_COUNT)).isFalse();
    assertThat(
            Attachment.BUFFER_ENTRY_STATUS.supportsDiscreteValue(
                BufferEntryStatus.AVAILABLE.toString()))
        .isTrue();
    assertThat(Attachment.BUFFER_ENTRY_STATUS.supportsDiscreteValue("coolio")).isFalse();
  }

  @Test
  public void testUnrestricted() {
    assertThat(Attachment.DEBUG_CONTINUOUS_VALUE.supportsDiscreteValue("foo"));

    assertThat(
        Arrays.stream(GaugeMetric.values())
            .allMatch(Attachment.DEBUG_CONTINUOUS_VALUE::supportsMetric));
  }
}
