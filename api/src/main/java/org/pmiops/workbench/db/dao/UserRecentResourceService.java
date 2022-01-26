package org.pmiops.workbench.db.dao;

import java.util.List;
import org.pmiops.workbench.db.model.DbUserRecentResource;
import org.pmiops.workbench.db.model.DbUserRecentlyModifiedResource;
import org.springframework.stereotype.Service;

@Service
public interface UserRecentResourceService {

  int USER_ENTRY_COUNT = 10;

  DbUserRecentResource updateNotebookEntry(
      long workspaceId, long userId, String notebookNameWithPath);

  void updateCohortEntry(long workspaceId, long userId, long cohortId);

  void updateConceptSetEntry(long workspaceId, long userId, long conceptSetId);

  void updateDataSetEntry(long workspaceId, long userId, long dataSetId);

  void updateCohortReviewEntry(long workspaceId, long userId, long cohortReviewId);

  void deleteNotebookEntry(long workspaceId, long userId, String notebookName);

  void deleteCohortEntry(long workspaceId, long userId, long cohortId);

  void deleteConceptSetEntry(long workspaceId, long userId, long conceptSetId);

  void deleteDataSetEntry(long workspaceId, long userId, long dataSetId);

  void deleteCohortReviewEntry(long workspaceId, long userId, long cohortReviewId);

  List<DbUserRecentResource> findAllResourcesByUser(long userId);

  List<DbUserRecentlyModifiedResource> findAllRecentlyModifiedResourcesByUser(long userId);
}
