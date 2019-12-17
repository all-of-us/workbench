package org.pmiops.workbench.monitoring;

import java.util.Collection;

/** Each class wishing to supply gauge data to the gauge cron job can simply
 *  implement this interface. By returning a well-formed MeasurementBundle, including
 *  any appropriate attachments, in getGaugeData(), the class is ensured that these
 *  values are polled as appropriate.
 *
 *  Using this arrangement minimizes the added dependencies for instrumented classes.
 *  In particular, they do not depend on the MonitoringService.
 */
public interface GaugeDataCollector {

  /**
   * Return a collection of @link {MeasurementBundle} objects. Each bundle can have one
   * or more View/Value pairs and zero or more metadata AttachmentKey/String pairs. There a 1:1
   * correspondents between MeasurementBundles returned and instantiation of MeasureMaps (and calls
   * to record).
   *
   * If a class has no attachments to provide, then it's fine to include all samples in a single
   * MeasurementBundle, which will save calls to the metrics backend.
   * @return collection of MeasurementBundles to be recorded.
   */
  Collection<MeasurementBundle> getGaugeData();
}
