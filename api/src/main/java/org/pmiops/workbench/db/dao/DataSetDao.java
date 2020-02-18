package org.pmiops.workbench.db.dao;

import java.util.List;
import java.util.Map;
import org.pmiops.workbench.db.model.DbDataset;
import org.pmiops.workbench.utils.DaoUtils;
import org.springframework.data.repository.CrudRepository;

public interface DataSetDao extends CrudRepository<DbDataset, Long> {
  List<DbDataset> findByWorkspaceIdAndInvalid(long workspaceId, boolean invalid);

  List<DbDataset> findDataSetsByCohortIds(long cohortId);

  List<DbDataset> findDataSetsByConceptSetIds(long conceptId);

  List<DbDataset> findByWorkspaceId(long workspaceId);

  default Map<Boolean, Long> getInvalidToCountMap() {
    return DaoUtils.getAttributeToCountMap(findAll(), DbDataset::getInvalid);
  }

  int countByWorkspaceId(long workspaceId);
}
