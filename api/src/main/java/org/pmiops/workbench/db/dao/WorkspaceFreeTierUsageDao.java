package org.pmiops.workbench.db.dao;

import org.pmiops.workbench.db.model.WorkspaceFreeTierUsage;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkspaceFreeTierUsageDao extends CrudRepository<WorkspaceFreeTierUsage, Long> {

  default void upsert(WorkspaceFreeTierUsage usageEntry) {
    if (existsByUserIdAndWorkspaceId(usageEntry.getUserId(), usageEntry.getWorkspaceId())) {
      update(usageEntry);
    } else {
      save(usageEntry);
    }
  }

  default int update(WorkspaceFreeTierUsage usageEntry) {
    return update(usageEntry.getUserId(), usageEntry.getWorkspaceId(), usageEntry.getCost());
  }

  @Modifying
  @Query(
      "UPDATE WorkspaceFreeTierUsage "
          + "SET cost = :cost "
          + "WHERE userId = :userId "
          + "AND workspaceId = :workspaceId")
  int update(
      @Param("userId") long userId,
      @Param("workspaceId") long workspaceId,
      @Param("cost") double cost);

  Boolean existsByUserIdAndWorkspaceId(long userId, long workspaceId);

  WorkspaceFreeTierUsage findOneByWorkspaceId(long workspaceId);
}
