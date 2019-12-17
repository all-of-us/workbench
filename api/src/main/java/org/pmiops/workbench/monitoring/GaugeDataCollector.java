package org.pmiops.workbench.monitoring;

import io.opencensus.metrics.data.AttachmentValue;
import java.util.Collections;
import java.util.Map;
import org.pmiops.workbench.monitoring.views.OpenCensusStatsViewInfo;

// Each implemnetation of this interface will correspond to a single measure. If that's going to be
// unnaturay for an implementation that's
public interface GaugeDataCollector {
  Map<OpenCensusStatsViewInfo, Number> getGaugeData();

  default Map<String, AttachmentValue> getMeasureMapAttachments() {
    return Collections.emptyMap();
  }
}
