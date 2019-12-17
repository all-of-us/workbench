package org.pmiops.workbench.monitoring.toydatasets;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Collection;
import java.util.Map;
import org.pmiops.workbench.monitoring.GaugeDataCollector;
import org.pmiops.workbench.monitoring.MeasurementBundle;
import org.pmiops.workbench.monitoring.views.OpenCensusView;
import org.pmiops.workbench.monitoring.views.ViewProperties;
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
  public Collection<MeasurementBundle> getGaugeData() {
//    ImmutableMap.Builder<OpenCensusView, Number> signalToValueBuilder = ImmutableMap.builder();
//    // Toy data series for developing & testing dashboards and alerts in low-traffic environments.
//    signalToValueBuilder.put(ViewProperties.DEBUG_CONSTANT_VALUE, TOY_CONSTANT_VALUE);
//    signalToValueBuilder.put(ViewProperties.DEBUG_MILLISECONDS_SINCE_EPOCH, clock.millis());
//    signalToValueBuilder.put(ViewProperties.DEBUG_RANDOM_DOUBLE, random.nextDouble());
//    return signalToValueBuilder.build();
    ImmutableSet.builder()
        .build();
  }
}
