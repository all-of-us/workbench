package org.pmiops.workbench.db.dao;

import org.pmiops.workbench.db.model.NotebookCohortCache;
import org.pmiops.workbench.db.model.WorkspaceUserRole;
import org.springframework.data.repository.CrudRepository;

public interface NotebookCohortCacheDao extends CrudRepository<NotebookCohortCache, Long> {

  long countNotebookCohortCachesByUserWorkspaceId(WorkspaceUserRole userWorkspaceRole);

  NotebookCohortCache findTopByUserWorkspaceIdOrderByLastAccessTime(WorkspaceUserRole userWorkspaceRole);

  NotebookCohortCache findByUserWorkspaceIdAndCohortId(WorkspaceUserRole userWorkspaceRole, long cohortId);

  NotebookCohortCache findByUserWorkspaceIdAndNotebookName(WorkspaceUserRole userWorkspaceRole, String notebookName);

  void deleteNotebookCohortCacheByUserWorkspaceIdAndNotebookName(WorkspaceUserRole workspaceUserRole, String notebookName);

}
