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

  public static final Timestamp NOW = Timestamp.from(Instant.now());
  public static final long NOW_TIME = NOW.getTime();
  public static final FakeClock CLOCK = new FakeClock(NOW.toInstant(), ZoneId.systemDefault());

  @Bean
  FakeClock fakeClock() {
    return new FakeClock(NOW.toInstant(), ZoneId.systemDefault());
  }

  @Bean
  Clock clock(FakeClock c) {
    return c;
  }
}
