package org.pmiops.workbench.db.dao;

import org.pmiops.workbench.db.model.Cohort;
import org.pmiops.workbench.db.model.UserRecentResource;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface UserRecentResourceDao extends CrudRepository<UserRecentResource, Long> {

  long countUserRecentResourceByUserId(long userId);

  UserRecentResource findTopByUserIdOrderByLastAccessDate(long userId);

  UserRecentResource findByUserIdAndWorkspaceIdAndCohort(long userId, long workspaceId, Cohort cohort);

  UserRecentResource findByUserIdAndWorkspaceIdAndNotebookName(long userId, long workspaceId, String notebookName);
  
  List<UserRecentResource> findUserRecentResourcesByUserIdOrderByLastAccessDateDesc(long userId);

}
