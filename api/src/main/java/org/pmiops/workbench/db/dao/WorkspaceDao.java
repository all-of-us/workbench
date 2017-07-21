package org.pmiops.workbench.db.dao;

import org.pmiops.workbench.db.model.Workspace;
import org.springframework.data.repository.CrudRepository;

public interface WorkspaceDao extends CrudRepository<Workspace, Long> {

}
