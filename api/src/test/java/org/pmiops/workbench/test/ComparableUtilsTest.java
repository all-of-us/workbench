package org.pmiops.workbench.test;

import static com.google.common.truth.Truth.assertThat;

import java.time.Duration;
import org.junit.Test;
import org.pmiops.workbench.utils.ComparableUtils;

public class ComparableUtilsTest {

  private static final Duration SMALL_DURATION = Duration.ofMinutes(3);
  private static final Duration LARGE_DURATION = Duration.ofMinutes(10);

  @Test
  public void testComparisons() {
    assertThat(ComparableUtils.isLessThan(SMALL_DURATION, LARGE_DURATION)).isTrue();
    assertThat(ComparableUtils.isLessThan(LARGE_DURATION, SMALL_DURATION)).isFalse();
    assertThat(ComparableUtils.isGreaterThan(SMALL_DURATION, LARGE_DURATION)).isFalse();
    assertThat(ComparableUtils.isEqualValueTo(SMALL_DURATION, LARGE_DURATION)).isFalse();
    assertThat(ComparableUtils.isEqualValueTo(SMALL_DURATION, SMALL_DURATION)).isTrue();
  }
}
