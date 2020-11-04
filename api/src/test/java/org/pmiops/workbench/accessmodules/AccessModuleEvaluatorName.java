package org.pmiops.workbench.accessmodules;

public enum AccessModuleEvaluatorName {
  MOODLE("moodle"),
  DOCUSIGN("docusign"),
  RAS("ras");

  private final String name;

  AccessModuleEvaluatorName(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
