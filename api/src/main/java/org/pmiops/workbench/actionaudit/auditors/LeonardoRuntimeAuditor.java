package org.pmiops.workbench.actionaudit.auditors;

import java.util.List;

public interface LeonardoRuntimeAuditor {
  void fireDeleteRuntimesInProject(String projectId, List<String> runtimeNames);
}
