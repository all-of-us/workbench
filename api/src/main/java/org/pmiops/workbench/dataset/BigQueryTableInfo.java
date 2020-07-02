package org.pmiops.workbench.dataset;

import org.pmiops.workbench.model.Domain;

public enum BigQueryTableInfo {
  CONDITION(Domain.CONDITION, "condition_occurrence"),
  PROCEDURE(Domain.PROCEDURE, "procedure_occurrence"),
  DRUG(Domain.DRUG, "drug_exposure"),
  DEATH(Domain.DEATH, "death"),
  DEVICE(Domain.DEVICE, "device_exposure"),
  MEASUREMENT(Domain.MEASUREMENT, "measurement"),
  SURVEY(Domain.SURVEY, "observation"),
  PERSON(Domain.PERSON, "person"),
  OBSERVATION(Domain.OBSERVATION, "observation"),
  VISIT(Domain.VISIT, "visit_occurrence"),
  PHYSICALMEASUREMENT(Domain.PHYSICALMEASUREMENT, "measurement");

  private final Domain domain;
  private final String tableName;

  BigQueryTableInfo(Domain domain, String tableName) {
    this.domain = domain;
    this.tableName = tableName;
  }

  public static String getTableName(Domain domain) {
    for (BigQueryTableInfo info : values()) {
      if (info.domain.equals(domain)) {
        return info.tableName;
      }
    }
    return null;
  }
}
