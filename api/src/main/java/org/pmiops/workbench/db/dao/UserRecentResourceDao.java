package org.pmiops.workbench.db.dao;

import org.pmiops.workbench.db.model.UserRecentResource;
import org.springframework.data.repository.CrudRepository;

public interface UserRecentResourceDao extends CrudRepository<UserRecentResource, Long> {

  long countUserRecentResourceByUserId(long userId);

  UserRecentResource findTopByUserIdOrderByLastAccessTime(long userId);

  UserRecentResource findByUserIdAndWorkspaceIdAndCohortId(long userId, long workspaceId, long cohortId);

  UserRecentResource findByUserIdAndWorkspaceIdAndNotebookName(long userId, long workspaceId, String notebookName);

  void deleteUserRecentResourceByUserIdAndWorkspaceIdAndNotebookName(long userId, long workspaceId, String notebookName);
}
