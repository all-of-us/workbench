package org.pmiops.workbench.db.dao;

import org.pmiops.workbench.db.model.DbUserRecentResourcesId;
import org.springframework.data.repository.CrudRepository;

public interface UserRecentResourcesIdDao extends CrudRepository<DbUserRecentResourcesId, Long> {

  long countDbUserRecentResourcesIdsByUserId(long userId);

  DbUserRecentResourcesId findTopByUserIdOrderByLastAccessDate(long userId);

  DbUserRecentResourcesId
      findDbUserRecentResourcesIdByUserIdAndWorkspaceIdAndResourceTypeAndResourceId(
          long userId, long workspaceId, short resourceType, String resourceId);
}
