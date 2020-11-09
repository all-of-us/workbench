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
import org.pmiops.workbench.model.Dataset;
import org.pmiops.workbench.model.DatasetPreviewRequest;
import org.pmiops.workbench.model.DatasetRequest;
import org.pmiops.workbench.model.KernelTypeEnum;
import org.pmiops.workbench.model.ResourceType;

public interface DatasetService {

  Dataset saveDataset(DatasetRequest datasetRequest, Long userId);

  Dataset saveDataset(DbDataset dataset);

  Dataset updateDataset(DatasetRequest datasetRequest, Long DatasetId);

  QueryJobConfiguration previewBigQueryJobConfig(DatasetPreviewRequest datasetPreviewRequest);

  Map<String, QueryJobConfiguration> domainToBigQueryConfig(DatasetRequest dataset);

  List<String> generateCodeCells(
      KernelTypeEnum kernelTypeEnum,
      String datasetName,
      String cdrVersionName,
      String qualifier,
      Map<String, QueryJobConfiguration> queryJobConfigurationMap);

  List<String> generateMicroarrayCohortExtractCodeCells(
      DbWorkspace dbWorkspace,
      String qualifier,
      Map<String, QueryJobConfiguration> queryJobConfigurationMap);

  List<String> generatePlinkDemoCode(String qualifier);

  List<String> generateHailDemoCode(String qualifier);

  DbDataset cloneDatasetToWorkspace(
      DbDataset fromDataset, DbWorkspace toWorkspace, Set<Long> cohortIds, Set<Long> conceptSetIds);

  List<DbDataset> getDatasets(DbWorkspace workspace);

  List<DbConceptSet> getConceptSetsForDataset(DbDataset dataset);

  List<DbCohort> getCohortsForDataset(DbDataset dataset);

  List<Dataset> getDatasets(ResourceType resourceType, long resourceId);

  void deleteDataset(Long datasetId);

  Optional<Dataset> getDbDataset(Long datasetId);

  void markDirty(ResourceType resourceType, long resourceId);

  DataDictionaryEntry findDataDictionaryEntry(String fieldName, DbCdrVersion cdrVersion);
}
