package org.pmiops.workbench.monitoring;

import java.util.function.Supplier;
import org.pmiops.workbench.monitoring.MeasurementBundle.Builder;
import org.pmiops.workbench.monitoring.views.DistributionMetric;
import org.pmiops.workbench.monitoring.views.EventMetric;
import org.springframework.stereotype.Service;

/**
 * While it's possible to mock this service correctly, it's much less intrusive to simply provide a
 * no-op implementation. If you're curious on what the stubbing looks like, see
 * https://stackoverflow.com/questions/60138415/which-breaks-first-mockito-or-java-generics for
 * details on how to mock the generic supplier matcher.
 */
@Service("LOGS_BASED_METRIC_SERVICE_FAKE")
public class LogsBasedMetricServiceFakeImpl implements LogsBasedMetricService {

  @Override
  public void record(MeasurementBundle measurementBundle) {}

  @Override
  public void recordEvent(EventMetric eventMetric) {}

  // Simply run the operation
  @Override
  public void recordElapsedTime(
      Builder measurementBundleBuilder, DistributionMetric distributionMetric, Runnable operation) {
    operation.run();
  }

  // Return the output of the supplier
  @Override
  public <T> T recordElapsedTime(
      Builder measurementBundleBuilder,
      DistributionMetric distributionMetric,
      Supplier<T> operation) {
    return operation.get();
  }
}
