package org.pmiops.workbench.db.dao;

import java.util.List;
import org.pmiops.workbench.db.model.UserRecentResource;
import org.springframework.stereotype.Service;

@Service
public interface UserRecentResourceService {

  int USER_ENTRY_COUNT = 10;

  UserRecentResource updateNotebookEntry(
      long workspaceId, long userId, String notebookNameWithPath);

  void updateCohortEntry(
      long workspaceId, long userId, long cohortId);

  void updateConceptSetEntry(
      long workspaceId, long userId, long conceptSetId);

  void deleteNotebookEntry(long workspaceId, long userId, String notebookName);

  List<UserRecentResource> findAllResourcesByUser(long userId);
}
