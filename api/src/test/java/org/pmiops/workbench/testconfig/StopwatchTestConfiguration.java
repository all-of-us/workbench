package org.pmiops.workbench.testconfig;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.google.common.base.Stopwatch;
import com.google.common.base.Ticker;
import java.time.Duration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class StopwatchTestConfiguration {

  // Fixed value of elapsed time, for assertions.
  public static Duration ELAPSED = Duration.ofMillis(100);

  @Bean
  public Stopwatch getStopwatch() {
    final Ticker mockTicker = mock(Ticker.class);
    doReturn(ELAPSED.toNanos()).when(mockTicker).read();
    return Stopwatch.createUnstarted(mockTicker);
  }
}
