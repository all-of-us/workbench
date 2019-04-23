package org.pmiops.workbench.db.dao;

import org.pmiops.workbench.db.model.DataSet;
import org.pmiops.workbench.db.model.DataSetValues;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;

@Service
public interface DataSetService {

  DataSet saveDataSet(String name, String description, long workspaceId, List<Long> cohortIdList,
      List<Long> conceptIdList, List<DataSetValues> values, long creatorId, Timestamp creationTime);
}
