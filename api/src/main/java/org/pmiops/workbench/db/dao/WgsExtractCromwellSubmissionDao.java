package org.pmiops.workbench.db.dao;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Optional;
import org.pmiops.workbench.db.model.DbDataset;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.db.model.DbWgsExtractCromwellSubmission;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.TerraJobStatus;
import org.springframework.data.repository.CrudRepository;

public interface WgsExtractCromwellSubmissionDao
    extends CrudRepository<DbWgsExtractCromwellSubmission, Long> {
  List<DbWgsExtractCromwellSubmission> findAllByWorkspace(DbWorkspace dbWorkspace);

  Optional<DbWgsExtractCromwellSubmission>
      findByWorkspaceWorkspaceIdAndWgsExtractCromwellSubmissionId(
          Long workspaceWorkspaceId, Long gsExtractCromwellSubmissionId);

  Optional<DbWgsExtractCromwellSubmission>
      findFirstByDatasetAndTerraStatusInOrderByCreationTimeDesc(
          DbDataset dataset, List<Short> terraStatuses);

  default Optional<DbWgsExtractCromwellSubmission> findMostRecentValidExtractionByDataset(
      DbDataset dataset) {
    return findFirstByDatasetAndTerraStatusInOrderByCreationTimeDesc(
        dataset,
        ImmutableList.of(
            DbStorageEnums.terraJobStatusToStorage(TerraJobStatus.RUNNING),
            DbStorageEnums.terraJobStatusToStorage(TerraJobStatus.SUCCEEDED)));
  }
}
