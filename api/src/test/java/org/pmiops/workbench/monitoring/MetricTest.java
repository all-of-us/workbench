package org.pmiops.workbench.monitoring;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.pmiops.workbench.monitoring.attachments.MetricLabel;
import org.pmiops.workbench.monitoring.views.EventMetric;
import org.pmiops.workbench.monitoring.views.GaugeMetric;

public class MetricTest {

  @Test
  public void testMatchingAttachmentIsSupported() {
    assertThat(GaugeMetric.USER_COUNT.getLabels())
        .contains(MetricLabel.USER_BYPASSED_BETA);
    assertThat(GaugeMetric.USER_COUNT.supportsLabel(MetricLabel.USER_BYPASSED_BETA)).isTrue();
    assertThat(EventMetric.NOTEBOOK_CLONE.supportsLabel(MetricLabel.USER_BYPASSED_BETA))
        .isFalse();
  }

  @Test
  public void testEmptyAttachmentListAllowsNoAttachments() {
    assertThat(GaugeMetric.COHORT_COUNT.getLabels()).isEmpty();
    assertThat(GaugeMetric.COHORT_COUNT.supportsLabel(MetricLabel.DATA_ACCESS_LEVEL))
        .isFalse();
  }
}
