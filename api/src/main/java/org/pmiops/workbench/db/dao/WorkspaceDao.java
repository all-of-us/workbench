package org.pmiops.workbench.db.dao;

import java.util.List;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.db.model.Workspace;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.jpa.repository.Query;


/**
 * Declaration of automatic query methods for Workspaces. The methods declared here are
 * automatically interpreted by Spring Data (see README).
 */
public interface WorkspaceDao extends CrudRepository<Workspace, Long> {
  @Query("SELECT w FROM Workspace w WHERE w.workspaceNamespace = ?1 AND w.firecloudName = ?2")
  Workspace get(String workspaceNamespace, String firecloudName);

  List<Workspace> findByWorkspaceNamespace(String workspaceNamespace);

  List<Workspace> findByCreatorOrderByNameAsc(User creator);

  @Query("SELECT w FROM Workspace w WHERE w.approved IS NULL AND w.reviewRequested = true");
  List<Workspace> findForReview();
}
