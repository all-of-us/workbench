package org.pmiops.workbench.db.dao;

import org.pmiops.workbench.db.model.UserRecentWorkspace;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface UserRecentWorkspaceDao extends CrudRepository<UserRecentWorkspace, Long> {
    List<UserRecentWorkspace> findTopByUserIdOrderByLastAccessDate(long userId);
}
