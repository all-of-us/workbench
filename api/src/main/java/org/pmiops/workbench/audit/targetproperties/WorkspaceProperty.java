package org.pmiops.workbench.audit.targetproperties;

public enum WorkspaceProperty {
  NAME("name"),
  INTENDED_STUDY("intended_study"),
  CREATOR("creator");

  private String propertyName;

  WorkspaceProperty(String propertyName) {
    this.propertyName = propertyName;
  }
}
