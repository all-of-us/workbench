package org.pmiops.workbench.utils;

import static com.google.common.truth.Truth.assertThat;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public class TimeAssertions {

  // We can't convert the whole range for OffsetDateTime to  Instant b/c of long overflow.
  public static final Instant MIN_SUPPORTED_INSTANT = Instant.parse("1000-01-30T01:20:00.00Z");
  public static final Instant MAX_SUPPORTED_INSTANT = Instant.parse("3000-01-30T01:20:00.00Z");

  public static final Duration DEFAULT_TOLERANCE = Duration.ofMillis(100);

  public static void assertTimeApprox(OffsetDateTime actual, OffsetDateTime expected) {
    assertTimeApprox(actual.toInstant(), expected.toInstant(), DEFAULT_TOLERANCE);
  }

  public static void assertTimeApprox(
      OffsetDateTime actual, OffsetDateTime expected, Duration tolerance) {
    assertTimeApprox(actual.toInstant(), expected.toInstant(), tolerance);
  }

  public static void assertTimeApprox(Timestamp actual, Timestamp expected) {
    assertTimeApprox(actual.toInstant(), expected.toInstant());
  }

  public static void assertTimeApprox(Timestamp actual, Timestamp expected, Duration tolerance) {
    assertTimeApprox(actual.toInstant(), expected.toInstant(), tolerance);
  }

  public static void assertTimeApprox(OffsetDateTime actual, Timestamp expected) {
    // TODO: move to TimeMappers class after merging that
    assertTimeApprox(actual, OffsetDateTime.ofInstant(expected.toInstant(), ZoneOffset.UTC));
  }

  public static void assertTimeApprox(
      OffsetDateTime actual, Timestamp expected, Duration duration) {
    assertTimeApprox(
        actual, OffsetDateTime.ofInstant(expected.toInstant(), ZoneOffset.UTC), duration);
  }

  public static void assertTimeApprox(Timestamp actual, OffsetDateTime expected) {
    assertTimeApprox(actual.toInstant(), expected.toInstant());
  }

  public static void assertTimeApprox(Instant actual, Instant expected) {
    assertTimeApprox(actual, expected, DEFAULT_TOLERANCE);
  }

  public static void assertTimeApprox(Instant actual, Instant expected, Duration tolerance) {
    assertTimeApprox(actual.toEpochMilli(), expected.toEpochMilli(), tolerance.toMillis());
  }

  public static void assertTimeApprox(long actualEpochMillis, long expectedEpochMillis) {
    assertTimeApprox(actualEpochMillis, expectedEpochMillis, DEFAULT_TOLERANCE.toMillis());
  }

  public static void assertTimeApprox(
      long actualEpochMillis, long expectedEpochMillis, double toleranceMillis) {
    assertThat((double) actualEpochMillis).isWithin(toleranceMillis).of(expectedEpochMillis);
  }
}
