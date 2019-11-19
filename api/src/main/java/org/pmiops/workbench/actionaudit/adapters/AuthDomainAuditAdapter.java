package org.pmiops.workbench.actionaudit.adapters;

public interface AuthDomainAuditAdapter {
  void fireSetAccountEnabled(long userId, boolean newEnabledValue, boolean oldEnabledValue);
}
