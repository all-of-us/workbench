package org.pmiops.workbench.audit;

// do not rename these, as they are serialized as text
public enum AuditColumn {
  TIMESTAMP,
  ACTION_ID,
  ACTION_TYPE,
  AGENT_TYPE,
  AGENT_ID,
  AGENT_EMAIL,
  TARGET_TYPE,
  TARGET_ID,
  TARGET_PROPERTY,
  PREV_VALUE,
  NEW_VALUE
}
