package org.pmiops.workbench.db.dao;

import org.pmiops.workbench.db.model.DbWorkspaceOperation;
import org.springframework.data.repository.CrudRepository;

public interface WorkspaceOperationDao extends CrudRepository<DbWorkspaceOperation, Long> {}
