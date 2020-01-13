package org.pmiops.workbench.monitoring;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.pmiops.workbench.monitoring.attachments.Attachment;
import org.pmiops.workbench.monitoring.views.EventMetric;
import org.pmiops.workbench.monitoring.views.GaugeMetric;

public class MetricTest {

  @Test
  public void testMatchingAttachmentIsSupported() {
    assertThat(GaugeMetric.USER_COUNT.getSupportedAttachments())
        .contains(Attachment.USER_BYPASSED_BETA);
    assertThat(GaugeMetric.USER_COUNT.supportsAttachment(Attachment.USER_BYPASSED_BETA)).isTrue();
    assertThat(EventMetric.NOTEBOOK_CLONE.supportsAttachment(Attachment.USER_BYPASSED_BETA))
        .isFalse();
  }

  @Test
  public void testEmptyAttachmentListAllowsNoAttachments() {
    assertThat(GaugeMetric.COHORT_COUNT.getSupportedAttachments()).isEmpty();
    assertThat(GaugeMetric.COHORT_COUNT.supportsAttachment(Attachment.DATA_ACCESS_LEVEL)).isFalse();
  }
}
