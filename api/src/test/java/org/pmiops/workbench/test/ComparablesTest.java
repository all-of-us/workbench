package org.pmiops.workbench.test;

import static com.google.common.truth.Truth.assertThat;

import java.time.Duration;
import org.junit.Test;
import org.pmiops.workbench.utils.Comparables;

public class ComparablesTest {

  private static final Duration SMALL_DURATION = Duration.ofMinutes(3);
  private static final Duration LARGE_DURATION = Duration.ofMinutes(10);

  @Test
  public void testIsLessThan() {
    assertThat(Comparables.isLessThan(SMALL_DURATION, SMALL_DURATION)).isFalse();
    assertThat(Comparables.isLessThan(SMALL_DURATION, LARGE_DURATION)).isTrue();
    assertThat(Comparables.isLessThan(LARGE_DURATION, SMALL_DURATION)).isFalse();
  }

  @Test
  public void testIsLessThanOrEqualTo() {
    assertThat(Comparables.isLessThanOrEqualTo(SMALL_DURATION, SMALL_DURATION)).isTrue();
    assertThat(Comparables.isLessThanOrEqualTo(SMALL_DURATION, LARGE_DURATION)).isTrue();
    assertThat(Comparables.isLessThanOrEqualTo(LARGE_DURATION, SMALL_DURATION)).isFalse();
  }

  @Test
  public void testIsEqualValueTo() {
    assertThat(Comparables.isEqualValueTo(SMALL_DURATION, SMALL_DURATION)).isTrue();
    assertThat(Comparables.isEqualValueTo(SMALL_DURATION, LARGE_DURATION)).isFalse();
  }

  @Test
  public void testIsGreaterThan() {
    assertThat(Comparables.isGreaterThan(SMALL_DURATION, SMALL_DURATION)).isFalse();
    assertThat(Comparables.isGreaterThan(SMALL_DURATION, LARGE_DURATION)).isFalse();
    assertThat(Comparables.isGreaterThan(LARGE_DURATION, SMALL_DURATION)).isTrue();
  }

  @Test
  public void testIsGreaterThanOrEqualTo() {
    assertThat(Comparables.isGreaterThanOrEqualTo(SMALL_DURATION, SMALL_DURATION)).isTrue();
    assertThat(Comparables.isGreaterThanOrEqualTo(SMALL_DURATION, LARGE_DURATION)).isFalse();
    assertThat(Comparables.isGreaterThanOrEqualTo(LARGE_DURATION, SMALL_DURATION)).isTrue();
  }
}
