package org.pmiops.workbench.db.dao;

import java.util.List;
import org.pmiops.workbench.db.model.DbWorkspaceRecoveryErrorLog;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface WorkspaceRecoveryErrorLogDao
    extends CrudRepository<DbWorkspaceRecoveryErrorLog, Long> {

  List<DbWorkspaceRecoveryErrorLog> findByWorkspaceIdOrderByCreatedTimeDesc(long workspaceId);

  @Query(
      value =
          "SELECT * FROM workspace_recovery_error_log WHERE workspace_id = :workspaceId ORDER BY created_time DESC LIMIT :limit",
      nativeQuery = true)
  List<DbWorkspaceRecoveryErrorLog> findLatestErrorsByWorkspaceId(
      @Param("workspaceId") long workspaceId, @Param("limit") int limit);
}

