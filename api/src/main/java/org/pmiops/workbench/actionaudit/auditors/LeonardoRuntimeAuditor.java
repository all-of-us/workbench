package org.pmiops.workbench.actionaudit.auditors;

public interface LeonardoRuntimeAuditor {
  void fireDeleteRuntime(String projectId, String runtimeName);
}
