package org.pmiops.workbench.utils;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.pmiops.workbench.utils.TimeAssertions.DEFAULT_TOLERANCE;
import static org.pmiops.workbench.utils.TimeAssertions.MAX_SUPPORTED_INSTANT;
import static org.pmiops.workbench.utils.TimeAssertions.MIN_SUPPORTED_INSTANT;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

public class TimeAssertionsTest {

  private static final Instant EXPECTED_TIME = Instant.parse("2010-06-30T01:20:00.00Z");
  private static final Duration DELTA_WITHIN_TOLERANCE = DEFAULT_TOLERANCE.dividedBy(2);
  private static final Duration DELTA_OUTSIDE_TOLERANCE = DEFAULT_TOLERANCE.multipliedBy(2);

  @Test
  public void testZeroTime() {
    TimeAssertions.assertTimeApprox(0, 0);
    TimeAssertions.assertTimeApprox(Instant.ofEpochMilli(0), Instant.ofEpochMilli(0));
  }

  @Test
  public void testInstant_smallDifference() {
    TimeAssertions.assertTimeApprox(EXPECTED_TIME.plus(DELTA_WITHIN_TOLERANCE), EXPECTED_TIME);
    TimeAssertions.assertTimeApprox(
        EXPECTED_TIME.plus(DELTA_WITHIN_TOLERANCE),
        EXPECTED_TIME,
        DEFAULT_TOLERANCE.multipliedBy(10));
    assertThrows(
        AssertionError.class,
        () ->
            TimeAssertions.assertTimeApprox(
                EXPECTED_TIME.plus(DELTA_OUTSIDE_TOLERANCE), EXPECTED_TIME));
  }

  @Test
  public void testLong() {
    TimeAssertions.assertTimeApprox(1_598_550_909_000L, 1_598_550_909_000L);
  }

  @Test
  public void testOffsetDateTime() {
    final OffsetDateTime expected = OffsetDateTime.ofInstant(EXPECTED_TIME, ZoneOffset.UTC);
    final AssertionError exception =
        assertThrows(
            AssertionError.class,
            () ->
                TimeAssertions.assertTimeApprox(expected.plus(DELTA_OUTSIDE_TOLERANCE), expected));
    assertThat(exception.getMessage()).contains("outside tolerance");

    TimeAssertions.assertTimeApprox(expected.plus(DELTA_WITHIN_TOLERANCE), expected);
    TimeAssertions.assertTimeApprox(expected.plusSeconds(3), expected, Duration.ofSeconds(4));
  }

  @Test
  public void testMaximumDateTime() {
    TimeAssertions.assertTimeApprox(MIN_SUPPORTED_INSTANT, MIN_SUPPORTED_INSTANT);
    TimeAssertions.assertTimeApprox(
        MAX_SUPPORTED_INSTANT.plus(DELTA_WITHIN_TOLERANCE), MAX_SUPPORTED_INSTANT);
    assertThrows(
        AssertionError.class,
        () ->
            TimeAssertions.assertTimeApprox(
                MIN_SUPPORTED_INSTANT.plus(DELTA_OUTSIDE_TOLERANCE), MIN_SUPPORTED_INSTANT));

    TimeAssertions.assertTimeApprox(MAX_SUPPORTED_INSTANT, MAX_SUPPORTED_INSTANT);
    TimeAssertions.assertTimeApprox(
        MAX_SUPPORTED_INSTANT.minus(DELTA_WITHIN_TOLERANCE), MAX_SUPPORTED_INSTANT);
    assertThrows(
        AssertionError.class,
        () ->
            TimeAssertions.assertTimeApprox(
                MAX_SUPPORTED_INSTANT.minus(DELTA_OUTSIDE_TOLERANCE), MAX_SUPPORTED_INSTANT));
  }

  @Test
  public void testInstant() {
    TimeAssertions.assertTimeApprox(EXPECTED_TIME.plus(DELTA_WITHIN_TOLERANCE), EXPECTED_TIME);
    TimeAssertions.assertTimeApprox(
        EXPECTED_TIME.plus(DELTA_OUTSIDE_TOLERANCE),
        EXPECTED_TIME,
        DELTA_OUTSIDE_TOLERANCE.multipliedBy(10));

    final AssertionError exception =
        assertThrows(
            AssertionError.class,
            () ->
                TimeAssertions.assertTimeApprox(
                    EXPECTED_TIME.plus(DELTA_OUTSIDE_TOLERANCE), EXPECTED_TIME));
    assertThat(exception.getMessage()).contains("outside tolerance");
  }
}
