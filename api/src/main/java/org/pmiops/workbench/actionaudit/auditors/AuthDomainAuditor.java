package org.pmiops.workbench.actionaudit.auditors;

public interface AuthDomainAuditor {
  void fireSetAccountDisabledStatus(
      long userId, boolean newDisabledValue, boolean oldDisabledValue);
}
