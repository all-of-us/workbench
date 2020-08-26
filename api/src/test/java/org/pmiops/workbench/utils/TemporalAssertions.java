package org.pmiops.workbench.utils;

import static com.google.common.truth.Truth.assertThat;

import java.time.Instant;
import java.time.OffsetDateTime;

public class TemporalAssertions {

  private static final double TIME_TOLERANCE_MILLIS = 100.0;

  public static void assertTimeWithinTolerance(OffsetDateTime actual, OffsetDateTime expected) {
    assertTimeWithinTolerance(actual.toInstant(), expected.toInstant(), TIME_TOLERANCE_MILLIS);
  }

  public static void assertTimeWithinTolerance(Instant actual, Instant expected) {
    assertTimeWithinTolerance(actual, expected, TIME_TOLERANCE_MILLIS);
  }

  public static void assertTimeWithinTolerance(Instant actual, Instant expected, double tolerance) {
    assertTimeWithinTolerance(actual.toEpochMilli(), expected.toEpochMilli(), tolerance);
  }

  public static void assertTimeWithinTolerance(long actualEpochMillis, long expectedEpochMillis) {
    assertTimeWithinTolerance(actualEpochMillis, expectedEpochMillis, TIME_TOLERANCE_MILLIS);
  }

  public static void assertTimeWithinTolerance(
      long actualEpochMillis, long expectedEpochMillis, double tolerance) {
    assertThat((double) actualEpochMillis).isWithin(tolerance).of(expectedEpochMillis);
  }
}
