package org.pmiops.workbench.db.dao;

import java.util.List;
import java.util.Optional;
import org.pmiops.workbench.db.model.DbDataset;
import org.springframework.data.repository.CrudRepository;

public interface DataSetDao extends CrudRepository<DbDataset, Long> {
  List<DbDataset> findByWorkspaceIdAndInvalid(long workspaceId, boolean invalid);

  List<DbDataset> findDataSetsByCohortIdsAndWorkspaceIdAndInvalid(
      long cohortId, long workspaceId, boolean dirty);

  List<DbDataset> findDbDataSetsByCohortIdsAndWorkspaceId(long cohortId, long workspaceId);

  List<DbDataset> findDbDatasetsByConceptSetIdsAndWorkspaceId(long conceptId, long workspaceId);

  List<DbDataset> findDataSetsByConceptSetIdsAndWorkspaceIdAndInvalid(
      long conceptId, long workspaceId, boolean dirty);

  List<DbDataset> findByWorkspaceId(long workspaceId);

  Optional<DbDataset> findByDataSetIdAndWorkspaceId(long dataSetId, long workspaceId);

  int countByWorkspaceId(long workspaceId);
}
