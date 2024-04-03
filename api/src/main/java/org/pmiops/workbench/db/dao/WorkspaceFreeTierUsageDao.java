package org.pmiops.workbench.db.dao;

import java.util.Map;
import org.jetbrains.annotations.Nullable;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.db.model.DbWorkspaceFreeTierUsage;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkspaceFreeTierUsageDao extends CrudRepository<DbWorkspaceFreeTierUsage, Long> {

  @Nullable
  DbWorkspaceFreeTierUsage findOneByWorkspace(DbWorkspace workspace);

  Iterable<DbWorkspaceFreeTierUsage> findAllByWorkspaceIn(Iterable<DbWorkspace> workspaceList);

  default void updateCost(
      Map<Long, DbWorkspaceFreeTierUsage> cache, DbWorkspace workspace, double cost) {
    DbWorkspaceFreeTierUsage usage = cache.get(workspace.getWorkspaceId());
    if (usage == null) {
      usage = new DbWorkspaceFreeTierUsage(workspace);
    }
    usage.setCost(cost);
    save(usage);
  }

  @Query("SELECT SUM(cost) FROM DbWorkspaceFreeTierUsage u WHERE user = :user")
  Double totalCostByUser(@Param("user") DbUser user);
}
