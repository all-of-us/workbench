package org.pmiops.workbench.actionaudit;

// do not rename these, as they are serialized as text
public enum ActionType {
  LOGIN,
  LOGOUT,
  BYPASS,
  EDIT,
  CREATE,
  DUPLICATE_FROM,
  DUPLICATE_TO,
  COLLABORATE,
  DELETE,
  DETECT_HIGH_EGRESS_EVENT,
  REMEDIATE_HIGH_EGRESS_EVENT,
  VIEW
}
