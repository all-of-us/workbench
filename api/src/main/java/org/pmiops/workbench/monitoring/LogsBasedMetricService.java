package org.pmiops.workbench.monitoring;

public interface LogsBasedMetricService {

  String METRIC_VALUE_KEY = "data_point_value";
  String METRIC_NAME_KEY = "metric_name";
  String METRIC_LABELS_KEY = "labels";

  /**
   * This method of metric recording is essentially one LogEntry per Timeseries per sample; Reusing
   * the MeasurementBundle class from the Monitoring implementation is useful because of
   * familiarity, simplicity, and replaceability. If we get unstuck on the OpenCensus side, spots
   * that record gauge and cumulative metrics via this interface can quickly be re-wired to the
   * OpenCensus version.
   *
   * @param measurementBundle - Bundle of one or more measurements corresonding to the same labels
   *     map
   */
  void recordMeasurementBundle(MeasurementBundle measurementBundle);
}
