package org.pmiops.workbench.db.dao;

import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.db.model.Workspace;

import java.sql.Date;
import java.sql.Timestamp;

public interface NotebookCohortCacheService {

  NotebookCohortCacheDao getDao();

  void updateNotebook(Workspace workspace, User user, String notebookName, Timestamp lastAccessDateTime);

  void updateCohort(Workspace workspace, User user, long cohortId, Timestamp lastAccessDateTime);

  void deleteNotebook(Workspace workspace, User user, String notebookName);
}
