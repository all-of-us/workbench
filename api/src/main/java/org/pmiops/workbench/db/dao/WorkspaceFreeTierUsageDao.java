package org.pmiops.workbench.db.dao;

import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.db.model.WorkspaceFreeTierUsage;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkspaceFreeTierUsageDao extends CrudRepository<WorkspaceFreeTierUsage, Long> {

  WorkspaceFreeTierUsage findOneByWorkspaceId(long workspaceId);

  default void updateCost(Workspace workspace, double cost) {
    WorkspaceFreeTierUsage usage = findOneByWorkspaceId(workspace.getWorkspaceId());
    if (usage == null) {
      usage = new WorkspaceFreeTierUsage(workspace);
    }
    usage.setCost(cost);
    usage.setLastUpdateTime();
    save(usage);
  }
}
