package org.pmiops.workbench.db.dao;

import org.pmiops.workbench.db.model.DataSet;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface DataSetDao extends CrudRepository<DataSet, Long> {
  List<DataSet> findByWorkspaceId(long workspaceId);
}
