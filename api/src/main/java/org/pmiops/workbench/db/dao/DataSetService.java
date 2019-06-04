package org.pmiops.workbench.db.dao;

import com.google.cloud.bigquery.QueryJobConfiguration;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import org.pmiops.workbench.db.model.DataSet;
import org.pmiops.workbench.db.model.DataSetValues;
import org.pmiops.workbench.model.DataSetRequest;
import org.springframework.stereotype.Service;

@Service
public interface DataSetService {
  DataSet saveDataSet(
      String name,
      Boolean includesAllParticipants,
      String description,
      long workspaceId,
      List<Long> cohortIdList,
      List<Long> conceptIdList,
      List<DataSetValues> values,
      long creatorId,
      Timestamp creationTime);

  Map<String, QueryJobConfiguration> generateQuery(DataSetRequest dataSet);
}
