package org.pmiops.workbench.actionaudit.auditors;

public interface LeonardoRuntimeAuditor {
  void fireDeleteRuntime(String googleProjectId, String runtimeName);
}
