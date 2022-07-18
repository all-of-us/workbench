package org.pmiops.workbench.db.dao;

import java.util.List;
import org.pmiops.workbench.db.model.DbUserRecentlyModifiedResource;
import org.springframework.data.repository.CrudRepository;

public interface UserRecentlyModifiedResourceDao
    extends CrudRepository<DbUserRecentlyModifiedResource, Long> {

  long countByUserId(long userId);

  DbUserRecentlyModifiedResource findTopByUserIdOrderByLastAccessDate(long userId);

  // Use getResource() as a shorter equivalent to this magic-JPA-named method
  DbUserRecentlyModifiedResource
      findDbUserRecentResourcesIdByUserIdAndWorkspaceIdAndResourceTypeAndResourceId(
          long userId,
          long workspaceId,
          DbUserRecentlyModifiedResource.DbUserRecentlyModifiedResourceType resourceType,
          String resourceId);

  // convenience method with a shorter name, for
  // findDbUserRecentResourcesIdByUserIdAndWorkspaceIdAndResourceTypeAndResourceId()
  default DbUserRecentlyModifiedResource getResource(
      long userId,
      long workspaceId,
      DbUserRecentlyModifiedResource.DbUserRecentlyModifiedResourceType resourceType,
      String resourceId) {
    return this.findDbUserRecentResourcesIdByUserIdAndWorkspaceIdAndResourceTypeAndResourceId(
        userId, workspaceId, resourceType, resourceId);
  }

  // Use getAllForUser() as a shorter equivalent to this magic-JPA-named method
  List<DbUserRecentlyModifiedResource>
      findDbUserRecentlyModifiedResourcesByUserIdOrderByLastAccessDateDesc(long userId);

  // convenience method with a shorter name, for
  // findDbUserRecentlyModifiedResourcesByUserIdOrderByLastAccessDateDesc()
  default List<DbUserRecentlyModifiedResource> getAllForUser(long userId) {
    return this.findDbUserRecentlyModifiedResourcesByUserIdOrderByLastAccessDateDesc(userId);
  }
}
