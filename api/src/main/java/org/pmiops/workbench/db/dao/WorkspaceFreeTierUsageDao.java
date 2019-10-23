package org.pmiops.workbench.db.dao;

import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.db.model.WorkspaceFreeTierUsage;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkspaceFreeTierUsageDao extends CrudRepository<WorkspaceFreeTierUsage, Long> {

  WorkspaceFreeTierUsage findOneByWorkspace(Workspace workspace);

  default void updateCost(Workspace workspace, double cost) {
    WorkspaceFreeTierUsage usage = findOneByWorkspace(workspace);
    if (usage == null) {
      usage = new WorkspaceFreeTierUsage(workspace);
    }
    usage.setCost(cost);
    save(usage);
  }
}
