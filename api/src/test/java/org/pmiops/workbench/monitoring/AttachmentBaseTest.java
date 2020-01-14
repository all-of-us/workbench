package org.pmiops.workbench.monitoring;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import java.util.Set;
import org.junit.Test;
import org.pmiops.workbench.monitoring.attachments.MetricLabelBase;

public class AttachmentBaseTest {

  // use anonymous inner class implementing AttachmentBase objects so we don't depend on
  // enums directly. That is, we're just checking the interface here.
  private static final MetricLabelBase DISCRETE_VALUE_ATTACHMENT =
      new MetricLabelBase() {
        @Override
        public String getName() {
          return "dummy_discrete_value_attachment";
        }

        @Override
        public Set<String> getAllowedDiscreteValues() {
          return ImmutableSet.of("a", "b", "c");
        }
      };

  private static final MetricLabelBase CONTINUOUS_VALUE_ATTACHMENT =
      new MetricLabelBase() {
        @Override
        public String getName() {
          return "dummy_continuous_value_attachment";
        }

        @Override
        public Set<String> getAllowedDiscreteValues() {
          return Collections.emptySet();
        }
      };

  @Test
  public void testBufferEntryStatus() {
    assertThat(DISCRETE_VALUE_ATTACHMENT.supportsDiscreteValue("a")).isTrue();
    assertThat(DISCRETE_VALUE_ATTACHMENT.supportsDiscreteValue("coolio")).isFalse();
  }

  @Test
  public void testUnrestricted() {
    assertThat(CONTINUOUS_VALUE_ATTACHMENT.supportsDiscreteValue("101")).isTrue();
    assertThat(CONTINUOUS_VALUE_ATTACHMENT.supportsDiscreteValue("102"));
  }
}
