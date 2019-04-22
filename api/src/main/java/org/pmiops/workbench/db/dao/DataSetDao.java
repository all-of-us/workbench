package org.pmiops.workbench.db.dao;

import org.pmiops.workbench.db.model.DataSet;
import org.springframework.data.repository.CrudRepository;

public interface DataSetDao extends CrudRepository<DataSet, Long> {

}
