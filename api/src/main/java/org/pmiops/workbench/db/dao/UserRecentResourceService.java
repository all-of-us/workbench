package org.pmiops.workbench.db.dao;

import org.springframework.stereotype.Service;

import java.sql.Timestamp;

@Service
public interface UserRecentResourceService {

  void updateNotebookEntry(long workspace_Id, long user_Id, String notebookName, Timestamp lastAccessDateTime);

  void updateCohortEntry(long workspace_Id, long user_Id, long cohortId, Timestamp lastAccessDateTime);

  void deleteNotebookEntry(long workspace_Id, long user_Id, String notebookName);
}
