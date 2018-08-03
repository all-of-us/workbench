package org.pmiops.workbench.db.dao;

import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.db.model.WorkspaceUserRole;
import org.springframework.data.repository.CrudRepository;

public interface WorkspaceUserRoleDao extends CrudRepository<WorkspaceUserRole, Long> {
   WorkspaceUserRole findWorkspaceUserRolesByWorkspaceAndUser(Workspace workspace, User user);

}
