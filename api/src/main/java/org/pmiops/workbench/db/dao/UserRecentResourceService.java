package org.pmiops.workbench.db.dao;

import java.sql.Timestamp;
import java.util.List;
import org.pmiops.workbench.db.model.DbUserRecentResource;
import org.springframework.stereotype.Service;

@Service
public interface UserRecentResourceService {

  DbUserRecentResource updateNotebookEntry(
      long workspaceId, long userId, String notebookNameWithPath, Timestamp lastAccessDateTime);

  void updateCohortEntry(
      long workspaceId, long userId, long cohortId, Timestamp lastAccessDateTime);

  void updateConceptSetEntry(
      long workspaceId, long userId, long conceptSetId, Timestamp lastAccessDateTime);

  void deleteNotebookEntry(long workspaceId, long userId, String notebookName);

  List<DbUserRecentResource> findAllResourcesByUser(long userId);
}
