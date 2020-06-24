package org.pmiops.workbench.db.dao;

import com.google.cloud.bigquery.QueryJobConfiguration;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbConceptSet;
import org.pmiops.workbench.db.model.DbDataset;
import org.pmiops.workbench.db.model.DbDatasetValue;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.DataSetPreviewRequest;
import org.pmiops.workbench.model.DataSetRequest;
import org.pmiops.workbench.model.KernelTypeEnum;
import org.pmiops.workbench.model.PrePackagedConceptSetEnum;
import org.pmiops.workbench.model.ResourceType;

public interface DataSetService {
  DbDataset saveDataSet(
      String name,
      Boolean includesAllParticipants,
      String description,
      long workspaceId,
      List<Long> cohortIdList,
      List<Long> conceptIdList,
      List<DbDatasetValue> values,
      PrePackagedConceptSetEnum prePackagedConceptSetEnum,
      long creatorId,
      Timestamp creationTime);

  DbDataset saveDataSet(DbDataset dataset);

  QueryJobConfiguration previewBigQueryJobConfig(DataSetPreviewRequest dataSetPreviewRequest);

  Map<String, QueryJobConfiguration> domainToBigQueryConfig(DataSetRequest dataSet);

  List<String> generateCodeCells(
      KernelTypeEnum kernelTypeEnum,
      String dataSetName,
      String qualifier,
      Map<String, QueryJobConfiguration> queryJobConfigurationMap);

  DbDataset cloneDataSetToWorkspace(
      DbDataset fromDataSet, DbWorkspace toWorkspace, Set<Long> cohortIds, Set<Long> conceptSetIds);

  List<DbDataset> getDataSets(DbWorkspace workspace);

  List<DbConceptSet> getConceptSetsForDataset(DbDataset dataSet);

  List<DbCohort> getCohortsForDataset(DbDataset dataSet);

  List<DbDataset> getDataSets(ResourceType resourceType, long resourceId);

  List<DbDataset> getInvalidDataSetsByWorkspace(DbWorkspace dbWorkspace);

  void deleteDataSet(DbWorkspace dbWorkspace, Long dataSetId);

  Optional<DbDataset> getDbDataSet(DbWorkspace dbWorkspace, Long dataSetId);

  void markDirty(ResourceType resourceType, long resourceId);
}
