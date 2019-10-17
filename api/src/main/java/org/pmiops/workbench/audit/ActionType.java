package org.pmiops.workbench.audit;

// do not rename these, as they are serialized as text
public enum ActionType {
  LOGIN,
  LOGOUT,
  BYPASS,
  EDIT,
  CREATE,
  DUPLICATE,
  COLLABORATE,
  DELETE
}
