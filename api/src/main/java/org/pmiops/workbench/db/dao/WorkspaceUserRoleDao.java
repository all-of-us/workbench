package org.pmiops.workbench.db.dao;

import java.util.List;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.db.model.WorkspaceUserRole;
import org.pmiops.workbench.db.model.Workspace;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.jpa.repository.Query;


/**
 * Declaration of automatic query methods for Workspaces. The methods declared here are
 * automatically interpreted by Spring Data (see README).
 *
 * Use of @Query is discouraged; if desired, define aliases in WorkspaceService.
 */
public interface WorkspaceUserRoleDao extends CrudRepository<WorkspaceUserRole, Long> {
  List<WorkspaceUserRole> findByUser(User user);
  List<WorkspaceUserRole> findByWorkspace(Workspace workspace);
}
