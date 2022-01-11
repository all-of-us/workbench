package org.pmiops.workbench.db.dao;

import java.util.Collection;
import org.pmiops.workbench.db.model.DbUserRecentlyModifiedResource;
import org.springframework.data.repository.CrudRepository;

public interface UserRecentlyModifiedResourceDao
    extends CrudRepository<DbUserRecentlyModifiedResource, Long> {

  long countDbUserRecentResourcesIdsByUserId(long userId);

  DbUserRecentlyModifiedResource findTopByUserIdOrderByLastAccessDate(long userId);

  void deleteByUserIdAndWorkspaceIdIn(long userId, Collection<Long> ids);

  DbUserRecentlyModifiedResource
      findDbUserRecentResourcesIdByUserIdAndWorkspaceIdAndResourceTypeAndResourceId(
          long userId,
          long workspaceId,
          DbUserRecentlyModifiedResource.DbUserRecentlyModifiedResourceType resourceType,
          String resourceId);

  default DbUserRecentlyModifiedResource findDbUserRecentResources(
      long userId,
      long workspaceId,
      DbUserRecentlyModifiedResource.DbUserRecentlyModifiedResourceType resourceType,
      String resourceId) {
    return this.findDbUserRecentResourcesIdByUserIdAndWorkspaceIdAndResourceTypeAndResourceId(
        userId, workspaceId, resourceType, resourceId);
  }
}
