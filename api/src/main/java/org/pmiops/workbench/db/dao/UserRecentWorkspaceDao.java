package org.pmiops.workbench.db.dao;

import org.pmiops.workbench.db.model.UserRecentWorkspace;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface UserRecentWorkspaceDao extends CrudRepository<UserRecentWorkspace, Long> {
    List<UserRecentWorkspace> findByUserIdOrderByLastAccessDate(long userId);

    Optional<UserRecentWorkspace> findFirstByWorkspaceIdAndUserId(long workspaceId, long userId);
}
