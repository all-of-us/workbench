package org.pmiops.workbench.db.dao;

import com.google.cloud.bigquery.QueryJobConfiguration;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.pmiops.workbench.db.model.DataSet;
import org.pmiops.workbench.db.model.DataSetValue;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.model.DataSetRequest;
import org.pmiops.workbench.model.KernelTypeEnum;
import org.pmiops.workbench.model.PrePackagedConceptSetEnum;
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
      List<DataSetValue> values,
      PrePackagedConceptSetEnum prePackagedConceptSetEnum,
      long creatorId,
      Timestamp creationTime);

  Map<String, QueryJobConfiguration> generateQueryJobConfigurationsByDomainName(
      DataSetRequest dataSet);

  List<String> generateCodeCells(
      KernelTypeEnum kernelTypeEnum,
      String dataSetName,
      String qualifier,
      Map<String, QueryJobConfiguration> queryJobConfigurationMap);

  DataSet cloneDataSetToWorkspace(
      DataSet fromDataSet, Workspace toWorkspace, Set<Long> cohortIds, Set<Long> conceptSetIds);

  List<DataSet> getDataSets(Workspace workspace);
}
