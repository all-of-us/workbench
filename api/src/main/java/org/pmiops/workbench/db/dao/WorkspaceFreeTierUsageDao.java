package org.pmiops.workbench.db.dao;

import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.db.model.WorkspaceFreeTierUsage;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkspaceFreeTierUsageDao extends CrudRepository<WorkspaceFreeTierUsage, Long> {

  WorkspaceFreeTierUsage findOneByWorkspace(DbWorkspace workspace);

  default void updateCost(DbWorkspace workspace, double cost) {
    WorkspaceFreeTierUsage usage = findOneByWorkspace(workspace);
    if (usage == null) {
      usage = new WorkspaceFreeTierUsage(workspace);
    }
    usage.setCost(cost);
    save(usage);
  }

  @Query("SELECT SUM(cost) FROM WorkspaceFreeTierUsage u WHERE user = :user")
  Double totalCostByUser(@Param("user") User user);
}
