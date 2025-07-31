package org.pmiops.workbench.db.dao;

import java.util.Set;
import org.pmiops.workbench.db.model.DbWorkspaceUserCache;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

/**
 * Cache of workspace access control lists. This cache is NOT to be used for any authorization. This
 * cache exists solely to reduce the load AoU places on Terra for a few nightly jobs that need to
 * access the ACLs of all workspaces such as the WRD upload and unshared resource cleanup.
 */
public interface WorkspaceUserCacheDao extends CrudRepository<DbWorkspaceUserCache, Long> {
  @Modifying
  void deleteAllByWorkspaceIdIn(Set<Long> workspaceIds);

  @Modifying
  @Query(
      value =
          "delete from workspace_user_cache wuc where exists (select * from workspace w where w.workspace_id = wuc.workspace_id and w.active_status = 1)",
      nativeQuery = true)
  void deleteAllInactiveWorkspaces();
}
