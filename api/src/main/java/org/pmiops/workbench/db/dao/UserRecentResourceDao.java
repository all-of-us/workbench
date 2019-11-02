package org.pmiops.workbench.db.dao;

import java.util.List;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbConceptSet;
import org.pmiops.workbench.db.model.DbUserRecentResource;
import org.springframework.data.repository.CrudRepository;

public interface UserRecentResourceDao extends CrudRepository<DbUserRecentResource, Long> {

  long countUserRecentResourceByUserId(long userId);

  DbUserRecentResource findTopByUserIdOrderByLastAccessDate(long userId);

  DbUserRecentResource findByUserIdAndWorkspaceIdAndCohort(
      long userId, long workspaceId, DbCohort cohort);

  DbUserRecentResource findByUserIdAndWorkspaceIdAndNotebookName(
      long userId, long workspaceId, String notebookPath);

  DbUserRecentResource findByUserIdAndWorkspaceIdAndConceptSet(
      long userId, long workspaceId, DbConceptSet conceptSet);

  List<DbUserRecentResource> findUserRecentResourcesByUserIdOrderByLastAccessDateDesc(long userId);
}
