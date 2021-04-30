package org.pmiops.workbench.db.dao;

import java.util.List;
import java.util.Optional;
import org.pmiops.workbench.db.model.DbWgsExtractCromwellSubmission;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.springframework.data.repository.CrudRepository;

public interface WgsExtractCromwellSubmissionDao
    extends CrudRepository<DbWgsExtractCromwellSubmission, Long> {
  List<DbWgsExtractCromwellSubmission> findAllByWorkspace(DbWorkspace dbWorkspace);

  Optional<DbWgsExtractCromwellSubmission>
      findByWorkspaceWorkspaceIdAndWgsExtractCromwellSubmissionId(
          Long workspaceWorkspaceId, Long gsExtractCromwellSubmissionId);
}
