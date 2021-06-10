package org.pmiops.workbench.utils;

import static com.google.common.truth.Truth.assertThat;
import static org.pmiops.workbench.utils.LogFormatters.formatDurationPretty;
import static org.pmiops.workbench.utils.LogFormatters.rate;

import java.time.Duration;
import org.junit.jupiter.api.Test;

public class LogFormattersTest {

  public static final String DESCRIPTION = "round-trip";
  public static final Duration DURATION =
      Duration.ofMinutes(2).plus(Duration.ofSeconds(27)).plus(Duration.ofMillis(456));

  @Test
  public void testDuration() {
    final String logString = LogFormatters.duration(DESCRIPTION, DURATION);
    assertThat(logString).startsWith(DESCRIPTION);
    assertThat(logString).contains("2m 27s 456ms");
  }

  @Test
  public void testPrettyPrintDuration() {
    final String prettyDuration = formatDurationPretty(DURATION);
    assertThat(prettyDuration).isEqualTo("2m 27s 456ms");
    assertThat(formatDurationPretty(Duration.ofMillis(1100))).isEqualTo("1s 100ms");
    assertThat(formatDurationPretty(Duration.ofMillis(3))).isEqualTo("3ms");
    assertThat(formatDurationPretty(Duration.ofMillis(0))).isEqualTo("0ms");
    assertThat(formatDurationPretty(Duration.ofDays(0))).isEqualTo("0ms");
    assertThat(formatDurationPretty(Duration.ofHours(3))).isEqualTo("3h 0m 0s 0ms");
    assertThat(formatDurationPretty(Duration.ofMinutes(5))).isEqualTo("5m 0s 0ms");
  }

  @Test
  public void testRate() {
    final String logString = rate("Eat", Duration.ofMinutes(5), 600, "hot dogs");
    assertThat(logString).isEqualTo("Eat: 600 in 5m 0s 0ms (2.000000 hot dogs/sec)");
  }
}
