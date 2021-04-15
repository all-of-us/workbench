package org.pmiops.workbench.db.dao;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.pmiops.workbench.db.model.DbUserRecentWorkspace;
import org.springframework.data.repository.CrudRepository;

public interface UserRecentWorkspaceDao extends CrudRepository<DbUserRecentWorkspace, Long> {
  List<DbUserRecentWorkspace> findByUserIdOrderByLastAccessDateDesc(long userId);

  Optional<DbUserRecentWorkspace> findFirstByWorkspaceIdAndUserId(long workspaceId, long userId);

  void deleteByUserIdAndWorkspaceIdIn(long userId, Collection<Long> ids);
}
