package org.pmiops.workbench.utils;

import java.time.Duration;

public class LogFormatters {
  public static String duration(String description, Duration duration) {
    return String.format("%s: %s", description, durationToSecondsWithMillis(duration));
  }

  public static String rate(String description, Duration duration, double count, String unit) {
    final double rate = 1000.0 * count / duration.toMillis();
    return String.format(
        "%s: %s (%f %s/sec)", description, durationToSecondsWithMillis(duration), rate, unit);
  }

  public static String durationToSecondsWithMillis(Duration duration) {
    return String.format("%d.%3ds", duration.toMillis() / 1000, duration.toMillis() % 1000);
  }
}
