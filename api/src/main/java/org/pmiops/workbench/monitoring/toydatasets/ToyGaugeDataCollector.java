package org.pmiops.workbench.monitoring.toydatasets;

import com.google.common.collect.ImmutableSet;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Collection;
import org.pmiops.workbench.monitoring.GaugeDataCollector;
import org.pmiops.workbench.monitoring.MeasurementBundle;
import org.pmiops.workbench.monitoring.attachments.AttachmentKey;
import org.pmiops.workbench.monitoring.views.Metric;
import org.springframework.stereotype.Service;

@Service
public class ToyGaugeDataCollector implements GaugeDataCollector {

  private static final long TOY_CONSTANT_VALUE = 101L;
  public static final String COLOR_LABEL = "Blue";
  private final SecureRandom random;
  private Clock clock;

  public ToyGaugeDataCollector(Clock clock) {
    this.clock = clock;
    this.random = new SecureRandom();
  }

  @Override
  public Collection<MeasurementBundle> getGaugeData() {
    return ImmutableSet.<MeasurementBundle>builder()
        .add(
            MeasurementBundle.builder()
                .addDelta(Metric.DEBUG_CONSTANT_VALUE, TOY_CONSTANT_VALUE)
                .addDelta(Metric.DEBUG_MILLISECONDS_SINCE_EPOCH, clock.millis())
                .addDelta(Metric.DEBUG_RANDOM_DOUBLE, random.nextDouble())
                .attach(AttachmentKey.DEBUG_COLOR, COLOR_LABEL)
                .build())
        .build();
  }
}
