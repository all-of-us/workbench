package org.pmiops.workbench.dataset;

import com.google.cloud.bigquery.QueryJobConfiguration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbConceptSet;
import org.pmiops.workbench.db.model.DbDataset;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.DataDictionaryEntry;
import org.pmiops.workbench.model.DataSet;
import org.pmiops.workbench.model.DataSetPreviewRequest;
import org.pmiops.workbench.model.DataSetRequest;
import org.pmiops.workbench.model.KernelTypeEnum;
import org.pmiops.workbench.model.ResourceType;

public interface DataSetService {

  DataSet saveDataSet(DataSetRequest dataSetRequest, Long userId);

  DataSet saveDataSet(DbDataset dataset);

  DataSet updateDataSet(DataSetRequest dataSetRequest, Long DataSetId);

  QueryJobConfiguration previewBigQueryJobConfig(DataSetPreviewRequest dataSetPreviewRequest);

  Map<String, QueryJobConfiguration> domainToBigQueryConfig(DataSetRequest dataSet);

  List<String> generateCodeCells(
      KernelTypeEnum kernelTypeEnum,
      String dataSetName,
      String cdrVersionName,
      String qualifier,
      Map<String, QueryJobConfiguration> queryJobConfigurationMap);

  List<String> generateMicroarrayCohortExtractCodeCells(
      DbWorkspace dbWorkspace,
      String qualifier,
      Map<String, QueryJobConfiguration> queryJobConfigurationMap);

  List<String> generatePlinkDemoCode(String qualifier);

  List<String> generateHailDemoCode(String qualifier);

  DbDataset cloneDataSetToWorkspace(
      DbDataset fromDataSet,
      DbWorkspace toWorkspace,
      Set<Long> cohortIds,
      Set<Long> conceptSetIds,
      List<Short> prePackagedConceptSets);

  List<DbDataset> getDataSets(DbWorkspace workspace);

  List<DbConceptSet> getConceptSetsForDataset(DbDataset dataSet);

  List<DbCohort> getCohortsForDataset(DbDataset dataSet);

  List<DataSet> getDataSets(ResourceType resourceType, long resourceId);

  void deleteDataSet(Long dataSetId);

  Optional<DataSet> getDbDataSet(Long dataSetId);

  void markDirty(ResourceType resourceType, long resourceId);

  DataDictionaryEntry findDataDictionaryEntry(
      String fieldName, String domain, DbCdrVersion cdrVersion);
}
