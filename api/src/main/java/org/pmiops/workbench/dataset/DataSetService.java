package org.pmiops.workbench.dataset;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableResult;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbConceptSet;
import org.pmiops.workbench.db.model.DbDataset;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.DataDictionaryEntry;
import org.pmiops.workbench.model.DataSet;
import org.pmiops.workbench.model.DataSetPreviewRequest;
import org.pmiops.workbench.model.DataSetRequest;
import org.pmiops.workbench.model.DomainValue;
import org.pmiops.workbench.model.KernelTypeEnum;
import org.pmiops.workbench.model.ResourceType;

public interface DataSetService {

  DataSet saveDataSet(DataSetRequest dataSetRequest, Long userId);

  DataSet saveDataSet(DbDataset dataset);

  DataSet updateDataSet(long workspaceId, long dataSetId, DataSetRequest dataSetRequest);

  TableResult previewBigQueryJobConfig(DataSetPreviewRequest dataSetPreviewRequest);

  Map<String, QueryJobConfiguration> domainToBigQueryConfig(DataSetRequest dataSet);

  ImmutableList<String> generateCodeCells(
      KernelTypeEnum kernelTypeEnum,
      String dataSetName,
      String cdrVersionName,
      Map<String, QueryJobConfiguration> queryJobConfigurationMap);

  DbDataset cloneDataSetToWorkspace(
      DbDataset fromDataSet,
      DbWorkspace toWorkspace,
      Set<Long> cohortIds,
      Set<Long> conceptSetIds,
      List<Short> prePackagedConceptSets);

  List<DbDataset> getDataSets(DbWorkspace workspace);

  List<DbConceptSet> getConceptSetsForDataset(DbDataset dataSet);

  List<DbCohort> getCohortsForDataset(DbDataset dataSet);

  List<DataSet> getDataSets(long workspaceId, ResourceType resourceType, long resourceId);

  void deleteDataSet(long workspaceId, long dataSetId);

  Optional<DataSet> getDataSet(long workspaceId, long dataSetId);

  Optional<DbDataset> getDbDataSet(long workspaceId, long dataSetId);

  void markDirty(long workspaceId, ResourceType resourceType, long resourceId);

  DataDictionaryEntry findDataDictionaryEntry(String fieldName, String domain);

  List<String> getPersonIdsWithWholeGenome(DbDataset dataSet);

  List<DomainValue> getValueListFromDomain(String domain);
}
