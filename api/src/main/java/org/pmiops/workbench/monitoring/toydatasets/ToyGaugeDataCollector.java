package org.pmiops.workbench.monitoring.toydatasets;

import com.google.common.collect.ImmutableMap;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Map;
import org.pmiops.workbench.monitoring.GaugeDataCollector;
import org.pmiops.workbench.monitoring.views.MonitoringViews;
import org.pmiops.workbench.monitoring.views.OpenCensusStatsViewInfo;
import org.springframework.stereotype.Service;

@Service
public class ToyGaugeDataCollector implements GaugeDataCollector {

  private static final long TOY_CONSTANT_VALUE = 101L;
  private final SecureRandom random;
  private Clock clock;

  public ToyGaugeDataCollector(Clock clock) {
    this.clock = clock;
    this.random = new SecureRandom();
  }

  @Override
  public Map<OpenCensusStatsViewInfo, Number> getGaugeData() {
    ImmutableMap.Builder<OpenCensusStatsViewInfo, Number> signalToValueBuilder = ImmutableMap
        .builder();
    // Toy data series for developing & testing dashboards and alerts in low-traffic environments.
    signalToValueBuilder.put(MonitoringViews.DEBUG_CONSTANT_VALUE, TOY_CONSTANT_VALUE);
    signalToValueBuilder.put(MonitoringViews.DEBUG_MILLISECONDS_SINCE_EPOCH, clock.millis());
    signalToValueBuilder.put(MonitoringViews.DEBUG_RANDOM_DOUBLE, random.nextDouble());
    return signalToValueBuilder.build();
  }
}
