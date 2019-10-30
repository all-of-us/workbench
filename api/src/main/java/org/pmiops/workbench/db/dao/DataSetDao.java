package org.pmiops.workbench.db.dao;

import java.util.List;
import org.pmiops.workbench.db.model.ConceptSet;
import org.pmiops.workbench.db.model.DataSet;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface DataSetDao extends CrudRepository<DataSet, Long> {
  List<DataSet> findByWorkspaceIdAndInvalid(long workspaceId, boolean invalid);

  List<DataSet> findDataSetsByCohortIds(long cohortId);

  List<DataSet> findDataSetsByConceptSetIds(long conceptId);

  List<DataSet> findByWorkspaceId(long workspaceId);
}
