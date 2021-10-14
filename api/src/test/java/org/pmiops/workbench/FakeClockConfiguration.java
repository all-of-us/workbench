package org.pmiops.workbench;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.pmiops.workbench.test.FakeClock;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class FakeClockConfiguration {

  public static final Timestamp NOW = Timestamp.from(Instant.parse("2000-01-01T00:00:00.00Z"));
  public static final long NOW_TIME = NOW.getTime();
  public static final FakeClock CLOCK = new FakeClock(NOW.toInstant(), ZoneId.systemDefault());

  @Bean
  public FakeClock fakeClock() {
    return new FakeClock(NOW.toInstant(), ZoneId.systemDefault());
  }

  @Bean
  public Clock clock(FakeClock c) {
    return c;
  }
}
