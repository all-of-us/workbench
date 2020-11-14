package org.pmiops.workbench.accessmodules;

public enum AccessModuleEvaluatorKey {
  MOODLE("moodle"),
  DOCUSIGN("docusign"),
  RAS("ras");

  private final String name;

  AccessModuleEvaluatorKey(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
