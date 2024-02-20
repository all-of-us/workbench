package org.pmiops.workbench.actionaudit.auditors;

/** Auditor service which handles collecting audit logs for billing project deletion actions. */
public interface BillingProjectAuditor {
  /** Fires an audit log event for a billing project deletion action. */
  void fireDeleteAction(String billingProjectName);
}
