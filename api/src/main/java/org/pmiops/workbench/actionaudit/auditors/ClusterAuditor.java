package org.pmiops.workbench.actionaudit.auditors;

public interface ClusterAuditor {
  void fireDeleteClustersInProject(String projectId);
}
