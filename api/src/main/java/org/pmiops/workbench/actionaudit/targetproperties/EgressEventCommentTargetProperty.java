package org.pmiops.workbench.actionaudit.targetproperties;

import org.jetbrains.annotations.NotNull;

/**
 * A simple comment property relating to a high-egress event received by the Workbench. This
 * property is used to convey non-structured information about the inbound event, e.g. when the
 * event JSON failed to parse or when an associated workspace could not be found.
 */
public enum EgressEventCommentTargetProperty implements SimpleTargetProperty {
  COMMENT("comment");

  private final String propertyName;

  EgressEventCommentTargetProperty(String propertyName) {
    this.propertyName = propertyName;
  }

  @NotNull
  @Override
  public String getPropertyName() {
    return propertyName;
  }
}
