package org.pmiops.workbench.monitoring;

import java.util.Collection;
import java.util.Map;
import org.pmiops.workbench.monitoring.views.OpenCensusView;

// Each implemnetation of this interface will correspond to a single measure. If that's going to be
// unnaturay for an implementation that's
public interface GaugeDataCollector {
  Collection<MeasurementBundle> getGaugeData();
}
