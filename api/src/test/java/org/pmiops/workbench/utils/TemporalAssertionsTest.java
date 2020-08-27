package org.pmiops.workbench.utils;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.pmiops.workbench.utils.TemporalAssertions.assertTimeWithinTolerance;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.Test;

public class TemporalAssertionsTest {

  private static final Instant EXPECTED_TIME = Instant.parse("2010-06-30T01:20:00.00Z");

  @Test
  public void testZeroTime() {
    assertTimeWithinTolerance(0, 0);
    assertTimeWithinTolerance(Instant.ofEpochMilli(0), Instant.ofEpochMilli(0));
  }

  @Test
  public void testSmallDifference() {
    assertTimeWithinTolerance(EXPECTED_TIME.plusMillis(50), EXPECTED_TIME);
  }

  @Test
  public void testLong() {
    assertTimeWithinTolerance(1_598_550_909_000L, 1_598_550_909_000L);
  }

  @Test(expected = AssertionError.class)
  public void testOffByOneSecond() {
    assertTimeWithinTolerance(EXPECTED_TIME.plusSeconds(1), EXPECTED_TIME);
  }

  @Test
  public void testOffsetDateTime() {
    final OffsetDateTime expected = OffsetDateTime.ofInstant(EXPECTED_TIME, ZoneOffset.UTC);
    final AssertionError exception =
        assertThrows(
            AssertionError.class,
            () -> assertTimeWithinTolerance(expected.plusSeconds(2), expected));
    assertThat(exception.getMessage()).contains("but was          : 1.277860802E12");
    assertThat(exception.getMessage()).contains("outside tolerance: 100.0");
  }
}
