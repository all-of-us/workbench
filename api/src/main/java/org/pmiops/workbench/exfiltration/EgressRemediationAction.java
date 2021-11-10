package org.pmiops.workbench.exfiltration;

/** An action taken in response to an egress event. */
public enum EgressRemediationAction {
  SUSPEND_COMPUTE,
  DISABLE_USER,
  NONE
}
