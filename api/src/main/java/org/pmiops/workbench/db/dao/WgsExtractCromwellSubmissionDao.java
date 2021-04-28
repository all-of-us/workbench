package org.pmiops.workbench.db.dao;

import java.util.List;
import java.util.Optional;
import org.pmiops.workbench.db.model.DbWgsExtractCromwellSubmission;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface WgsExtractCromwellSubmissionDao
    extends CrudRepository<DbWgsExtractCromwellSubmission, Long> {
  List<DbWgsExtractCromwellSubmission> findAllByWorkspace(DbWorkspace dbWorkspace);

  @Query(value = "SELECT * FROM wgs_extract_cromwell_submission WHERE wgs_extract_cromwell_submission_id = :workspaceId and workspace_id = :jobId", nativeQuery = true)
  Optional<DbWgsExtractCromwellSubmission> findByWorkspaceIdAndJobId(@Param("workspaceId") Long workspaceId, @Param("jobId") Long jobId);
}
