package org.pmiops.workbench.monitoring;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.pmiops.workbench.monitoring.attachments.MetricLabel;
import org.pmiops.workbench.monitoring.views.EventMetric;
import org.pmiops.workbench.monitoring.views.GaugeMetric;

public class MetricTest {

  @Test
  public void testMatchingAttachmentIsSupported() {
    assertThat(GaugeMetric.USER_COUNT.getSupportedAttachments())
        .contains(MetricLabel.USER_BYPASSED_BETA);
    assertThat(GaugeMetric.USER_COUNT.supportsAttachment(MetricLabel.USER_BYPASSED_BETA)).isTrue();
    assertThat(EventMetric.NOTEBOOK_CLONE.supportsAttachment(MetricLabel.USER_BYPASSED_BETA))
        .isFalse();
  }

  @Test
  public void testEmptyAttachmentListAllowsNoAttachments() {
    assertThat(GaugeMetric.COHORT_COUNT.getSupportedAttachments()).isEmpty();
    assertThat(GaugeMetric.COHORT_COUNT.supportsAttachment(MetricLabel.DATA_ACCESS_LEVEL))
        .isFalse();
  }
}
