package org.pmiops.workbench.utils;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class LogFormatters {
  public static String duration(String description, Duration duration) {
    return String.format("%s: %s", description, formatDurationPretty(duration));
  }

  public static String rate(String description, Duration duration, double count, String unit) {
    final double rate = 1000.0 * count / duration.toMillis();
    return String.format(
        "%s: %d in %s (%f %s/sec)",
        description, (int) count, formatDurationPretty(duration), rate, unit);
  }

  // see https://stackoverflow.com/a/51026056. Better options are available in Java 9+
  public static String formatDurationPretty(Duration duration) {
    String result = "";

    if (duration.toDays() > 0) {
      result += String.format("%d days ", duration.toDays());
    }

    if (result.length() > 0 || duration.toHours() > 0) {
      result +=
          String.format("%dh ", duration.toHours() - TimeUnit.DAYS.toHours(duration.toDays()));
    }

    if (result.length() > 0 || duration.toMinutes() > 0) {
      result +=
          String.format(
              "%dm ", duration.toMinutes() - TimeUnit.HOURS.toMinutes(duration.toHours()));
    }
    if (result.length() > 0 || duration.getSeconds() > 0) {
      result +=
          String.format(
              "%ds ", duration.getSeconds() - TimeUnit.MINUTES.toSeconds(duration.toMinutes()));
    }
    // always include millis
    result += String.format("%dms", duration.toMillis() % TimeUnit.SECONDS.toMillis(1));

    return result;
  }
}
