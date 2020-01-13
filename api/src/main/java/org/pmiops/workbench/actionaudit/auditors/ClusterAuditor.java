package org.pmiops.workbench.actionaudit.auditors;

import java.util.List;

public interface ClusterAuditor {
  void fireDeleteClustersInProject(String projectId, List<String> clusterNames);
}
