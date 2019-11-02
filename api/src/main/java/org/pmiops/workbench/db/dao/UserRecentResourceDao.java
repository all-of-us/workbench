package org.pmiops.workbench.db.dao;

import java.util.List;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbConceptSet;
import org.pmiops.workbench.db.model.UserRecentResource;
import org.springframework.data.repository.CrudRepository;

public interface UserRecentResourceDao extends CrudRepository<UserRecentResource, Long> {

  long countUserRecentResourceByUserId(long userId);

  UserRecentResource findTopByUserIdOrderByLastAccessDate(long userId);

  UserRecentResource findByUserIdAndWorkspaceIdAndCohort(
      long userId, long workspaceId, DbCohort cohort);

  UserRecentResource findByUserIdAndWorkspaceIdAndNotebookName(
      long userId, long workspaceId, String notebookPath);

  UserRecentResource findByUserIdAndWorkspaceIdAndConceptSet(
      long userId, long workspaceId, DbConceptSet conceptSet);

  List<UserRecentResource> findUserRecentResourcesByUserIdOrderByLastAccessDateDesc(long userId);
}
