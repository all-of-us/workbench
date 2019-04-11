package org.pmiops.workbench.db.dao;

import org.pmiops.workbench.db.model.DataSet;
import org.pmiops.workbench.db.model.DataSetValues;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;

@Service
public class DataSetServiceImpl implements DataSetService {

  @Autowired
  DataSetDao dataSetDao;

  @Override
  public DataSet saveDataSet(String name, String description, long workspaceId,
      List<Long> cohortIdList, List<Long> conceptIdList, List<DataSetValues> values, long creatorId,
      Timestamp creationTime) {
    DataSet dataSetDb = new DataSet();
    dataSetDb.setName(name);
    dataSetDb.setDescription(description);
    dataSetDb.setWorkspaceId(workspaceId);
    dataSetDb.setInvalid(true);
    dataSetDb.setCreatorId(creatorId);
    dataSetDb.setCreationTime(creationTime);
    dataSetDb.setCohortSetId(cohortIdList);
    dataSetDb.setConceptSetId(conceptIdList);
    dataSetDb.setValues(values);
    try {
      dataSetDb = dataSetDao.save(dataSetDb);
    } catch (Exception ex) {
    }
    return dataSetDb;
  }
}
