package org.pmiops.workbench.monitoring.views;

/**
 * Unit strings must conform to the
 * @see <a href=https://unitsofmeasure.org/ucum.html>Unified Code for Units of Measure</a>
 * @return canonical string for unit
 */
public enum UnitOfMeasure {
  COUNT("1"),
  MILLISECOND("ms");

  private String ucmValue;

  UnitOfMeasure(String ucmValue) {
    this.ucmValue = ucmValue;
  }

  public String getUcmSymbol() {
    return ucmValue;
  }
}
