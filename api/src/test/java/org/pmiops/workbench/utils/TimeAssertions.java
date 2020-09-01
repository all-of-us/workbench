package org.pmiops.workbench.utils;

import static com.google.common.truth.Truth.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;

public class TimeAssertions {

  // We can't convert the whole range for OffsetDateTime to  Instant b/c of long overflow.
  public static final Instant MIN_SUPPORTED_INSTANT = Instant.parse("1000-01-30T01:20:00.00Z");
  public static final Instant MAX_SUPPORTED_INSTANT = Instant.parse("3000-01-30T01:20:00.00Z");

  public static final Duration DEFAULT_TOLERANCE = Duration.ofMillis(100);

  public static void assertTimeWithinTolerance(OffsetDateTime actual, OffsetDateTime expected) {
    assertTimeWithinTolerance(actual.toInstant(), expected.toInstant(), DEFAULT_TOLERANCE);
  }

  public static void assertTimeWithinTolerance(
      OffsetDateTime actual, OffsetDateTime expected, Duration tolerance) {
    assertTimeWithinTolerance(actual.toInstant(), expected.toInstant(), tolerance);
  }

  public static void assertTimeWithinTolerance(Instant actual, Instant expected) {
    assertTimeWithinTolerance(actual, expected, DEFAULT_TOLERANCE);
  }

  public static void assertTimeWithinTolerance(
      Instant actual, Instant expected, Duration tolerance) {
    assertTimeWithinTolerance(actual.toEpochMilli(), expected.toEpochMilli(), tolerance.toMillis());
  }

  public static void assertTimeWithinTolerance(long actualEpochMillis, long expectedEpochMillis) {
    assertTimeWithinTolerance(actualEpochMillis, expectedEpochMillis, DEFAULT_TOLERANCE.toMillis());
  }

  public static void assertTimeWithinTolerance(
      long actualEpochMillis, long expectedEpochMillis, double toleranceMillis) {
    assertThat((double) actualEpochMillis).isWithin(toleranceMillis).of(expectedEpochMillis);
  }
}
