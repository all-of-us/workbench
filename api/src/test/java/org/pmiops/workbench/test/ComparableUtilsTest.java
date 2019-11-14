package org.pmiops.workbench.test;

import static com.google.common.truth.Truth.assertThat;

import java.time.Duration;
import org.junit.Test;
import org.pmiops.workbench.utils.ComparableUtils;

public class ComparableUtilsTest {

  @Test
  public void testComparisons() {
    final Duration first = Duration.ofMinutes(3);
    final Duration second = Duration.ofMinutes(10);
    assertThat(ComparableUtils.isLessThan(first, second)).isTrue();
    assertThat(ComparableUtils.isLessThan(second, first)).isFalse();
    assertThat(ComparableUtils.isGreaterThan(first, second)).isFalse();
    assertThat(ComparableUtils.isEqualValueTo(first, second)).isFalse();
  }
}
