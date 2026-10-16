package org.pmiops.workbench.db.dao;

import java.util.List;
import org.pmiops.workbench.db.model.DbWorkspaceRecoveryErrorLog;
import org.springframework.data.repository.CrudRepository;

public interface WorkspaceRecoveryErrorLogDao
    extends CrudRepository<DbWorkspaceRecoveryErrorLog, Long> {

  List<DbWorkspaceRecoveryErrorLog> findByWorkspaceIdOrderByCreatedTimeDesc(long workspaceId);

  List<DbWorkspaceRecoveryErrorLog> findTop50ByWorkspaceIdOrderByCreatedTimeDesc(long workspaceId);
}
