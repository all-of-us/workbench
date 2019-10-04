package org.pmiops.workbench.db.dao;

import java.util.List;
import org.pmiops.workbench.db.model.DataSet;
import org.springframework.data.repository.CrudRepository;

public interface DataSetDao extends CrudRepository<DataSet, Long> {
  List<DataSet> findByWorkspaceIdAndInvalid(long workspaceId, boolean invalid);

  List<DataSet> findDataSetsByCohortSetId(long cohortId);

  List<DataSet> findDataSetsByConceptSetId(long conceptId);
}
