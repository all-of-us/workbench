package org.pmiops.workbench;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.pmiops.workbench.test.FakeClock;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

public class SpringTest {

  protected static final Timestamp NOW = Timestamp.from(Instant.now());
  protected static final long NOW_TIME = NOW.getTime();
  protected static final FakeClock CLOCK = new FakeClock(NOW.toInstant(), ZoneId.systemDefault());

  @TestConfiguration
  static class Configuration {
    @Bean
    Clock clock() {
      return CLOCK;
    }
  }
}
