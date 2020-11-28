package org.pmiops.workbench.compliance;

public enum MoodleBadge {
  DATA_USE_AGREEMENT("data_use_agreement"),
  RESEARCH_ETHICS_TRAINING("research_ethics_training");

  private final String badgeName;

  MoodleBadge(String badgeName) {
    this.badgeName = badgeName;
  }

  public String getBadgeName() {
    return badgeName;
  }
}
