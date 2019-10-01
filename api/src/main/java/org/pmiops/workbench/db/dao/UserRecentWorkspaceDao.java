package org.pmiops.workbench.db.dao;

import java.util.List;
import java.util.Optional;
import org.pmiops.workbench.db.model.UserRecentWorkspace;
import org.springframework.data.repository.CrudRepository;

public interface UserRecentWorkspaceDao extends CrudRepository<UserRecentWorkspace, Long> {
  List<UserRecentWorkspace> findByUserIdOrderByLastAccessDateDesc(long userId);

  Optional<UserRecentWorkspace> findFirstByWorkspaceIdAndUserId(long workspaceId, long userId);

  void deleteByWorkspaceIdIn(List<Long> workspaceIds);
}
