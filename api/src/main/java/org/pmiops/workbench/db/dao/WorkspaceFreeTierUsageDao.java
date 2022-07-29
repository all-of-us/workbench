package org.pmiops.workbench.db.dao;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.db.model.DbWorkspaceFreeTierUsage;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkspaceFreeTierUsageDao extends CrudRepository<DbWorkspaceFreeTierUsage, Long> {

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

  interface UserCostPair {
    DbUser getUser();

    Double getCost();
  }

  @Query(
      "SELECT u.user AS user, SUM(u.cost) AS cost "
          + "FROM DbWorkspaceFreeTierUsage u "
          + "WHERE u.cost IS NOT NULL "
          + "GROUP BY u.user")
  Collection<UserCostPair> totalCostByUser();

  default Map<DbUser, Double> getUserCostMap() {
    return totalCostByUser().stream()
        .collect(Collectors.toMap(UserCostPair::getUser, UserCostPair::getCost));
  }
}
