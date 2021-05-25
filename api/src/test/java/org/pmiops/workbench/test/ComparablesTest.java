package org.pmiops.workbench.test;

import static com.google.common.truth.Truth.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.utils.Comparables;

class ComparablesTest {

  private static final Duration SMALL_DURATION = Duration.ofMinutes(3);
  private static final Duration LARGE_DURATION = Duration.ofMinutes(10);

  @Test
void testIsLessThan() {
    assertThat(Comparables.isLessThan(SMALL_DURATION, SMALL_DURATION)).isFalse();
    assertThat(Comparables.isLessThan(SMALL_DURATION, LARGE_DURATION)).isTrue();
    assertThat(Comparables.isLessThan(LARGE_DURATION, SMALL_DURATION)).isFalse();
  }

  @Test
void testIsLessThanOrEqualTo() {
    assertThat(Comparables.isLessThanOrEqualTo(SMALL_DURATION, SMALL_DURATION)).isTrue();
    assertThat(Comparables.isLessThanOrEqualTo(SMALL_DURATION, LARGE_DURATION)).isTrue();
    assertThat(Comparables.isLessThanOrEqualTo(LARGE_DURATION, SMALL_DURATION)).isFalse();
  }

  @Test
void testIsEqualValueTo() {
    assertThat(Comparables.isEqualValueTo(SMALL_DURATION, SMALL_DURATION)).isTrue();
    assertThat(Comparables.isEqualValueTo(SMALL_DURATION, LARGE_DURATION)).isFalse();
    assertThat(Comparables.isEqualValueTo(LARGE_DURATION, SMALL_DURATION)).isFalse();
  }

  @Test
void testIsGreaterThan() {
    assertThat(Comparables.isGreaterThan(SMALL_DURATION, SMALL_DURATION)).isFalse();
    assertThat(Comparables.isGreaterThan(SMALL_DURATION, LARGE_DURATION)).isFalse();
    assertThat(Comparables.isGreaterThan(LARGE_DURATION, SMALL_DURATION)).isTrue();
  }

  @Test
void testIsGreaterThanOrEqualTo() {
    assertThat(Comparables.isGreaterThanOrEqualTo(SMALL_DURATION, SMALL_DURATION)).isTrue();
    assertThat(Comparables.isGreaterThanOrEqualTo(SMALL_DURATION, LARGE_DURATION)).isFalse();
    assertThat(Comparables.isGreaterThanOrEqualTo(LARGE_DURATION, SMALL_DURATION)).isTrue();
  }
}
