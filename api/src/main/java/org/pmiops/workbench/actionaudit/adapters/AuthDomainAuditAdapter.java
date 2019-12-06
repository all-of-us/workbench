package org.pmiops.workbench.actionaudit.adapters;

public interface AuthDomainAuditAdapter {
  void fireSetAccountDisabledStatus(
      long userId, boolean newDisabledValue, boolean oldDisabledValue);
}
