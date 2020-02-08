package org.pmiops.workbench.monitoring;

public enum MetricJsonKey {
  VALUE("data_point_value"),
  NAME("metric_name"),
  LABELS("labels"),
  UNIT("unit");

  private String keyName;

  MetricJsonKey(String keyName) {
    this.keyName = keyName;
  }

  public String getKeyName() {
    return keyName;
  }
}
