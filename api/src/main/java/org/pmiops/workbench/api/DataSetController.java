package org.pmiops.workbench.api;

import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.LegacySQLTypeName;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableResult;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.inject.Provider;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.pmiops.workbench.cdr.CdrVersionContext;
import org.pmiops.workbench.cdr.CdrVersionService;
import org.pmiops.workbench.dataset.BigQueryTableInfo;
import org.pmiops.workbench.dataset.DataSetService;
import org.pmiops.workbench.dataset.DatasetConfig;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbDataset;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.FailedPreconditionException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceResponse;
import org.pmiops.workbench.genomics.GenomicExtractionService;
import org.pmiops.workbench.model.DataDictionaryEntry;
import org.pmiops.workbench.model.DataSet;
import org.pmiops.workbench.model.DataSetCodeResponse;
import org.pmiops.workbench.model.DataSetExportRequest;
import org.pmiops.workbench.model.DataSetExportRequest.GenomicsAnalysisToolEnum;
import org.pmiops.workbench.model.DataSetExportRequest.GenomicsDataTypeEnum;
import org.pmiops.workbench.model.DataSetListResponse;
import org.pmiops.workbench.model.DataSetPreviewRequest;
import org.pmiops.workbench.model.DataSetPreviewResponse;
import org.pmiops.workbench.model.DataSetPreviewValueList;
import org.pmiops.workbench.model.DataSetRequest;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.DomainValue;
import org.pmiops.workbench.model.DomainValuesResponse;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.GenomicExtractionJob;
import org.pmiops.workbench.model.GenomicExtractionJobListResponse;
import org.pmiops.workbench.model.KernelTypeEnum;
import org.pmiops.workbench.model.MarkDataSetRequest;
import org.pmiops.workbench.model.PrePackagedConceptSetEnum;
import org.pmiops.workbench.model.ResourceType;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.notebooks.NotebooksService;
import org.pmiops.workbench.workspaces.WorkspaceAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DataSetController implements DataSetApiDelegate {

  private static final String DATE_FORMAT_STRING = "yyyy/MM/dd HH:mm:ss";
  public static final String EMPTY_CELL_MARKER = "";
  public static final String WHOLE_GENOME_VALUE = "VCF Files(s)";

  private static final Logger log = Logger.getLogger(DataSetController.class.getName());

  private final DataSetService dataSetService;

  private final Provider<DbUser> userProvider;
  private final Provider<String> prefixProvider;

  private final CdrVersionService cdrVersionService;
  private final FireCloudService fireCloudService;
  private final NotebooksService notebooksService;
  private final GenomicExtractionService genomicExtractionService;
  private final WorkspaceAuthService workspaceAuthService;

  @Autowired
  DataSetController(
      //      BigQueryService bigQueryService,
      CdrVersionService cdrVersionService,
      DataSetService dataSetService,
      FireCloudService fireCloudService,
      NotebooksService notebooksService,
      Provider<DbUser> userProvider,
      @Qualifier(DatasetConfig.DATASET_PREFIX_CODE) Provider<String> prefixProvider,
      GenomicExtractionService genomicExtractionService,
      WorkspaceAuthService workspaceAuthService) {
    //    this.bigQueryService = bigQueryService;
    this.cdrVersionService = cdrVersionService;
    this.dataSetService = dataSetService;
    this.fireCloudService = fireCloudService;
    this.notebooksService = notebooksService;
    this.userProvider = userProvider;
    this.prefixProvider = prefixProvider;
    this.genomicExtractionService = genomicExtractionService;
    this.workspaceAuthService = workspaceAuthService;
  }

  @Override
  public ResponseEntity<DataSet> createDataSet(
      String workspaceNamespace, String workspaceFirecloudName, DataSetRequest dataSetRequest) {
    validateDataSetCreateRequest(dataSetRequest);
    final long workspaceId =
        workspaceAuthService
            .getWorkspaceEnforceAccessLevelAndSetCdrVersion(
                workspaceNamespace, workspaceFirecloudName, WorkspaceAccessLevel.WRITER)
            .getWorkspaceId();
    dataSetRequest.setWorkspaceId(workspaceId);
    return ResponseEntity.ok(
        dataSetService.saveDataSet(dataSetRequest, userProvider.get().getUserId()));
  }

  private void validateDataSetCreateRequest(DataSetRequest dataSetRequest) {
    boolean includesAllParticipants =
        Optional.ofNullable(dataSetRequest.getIncludesAllParticipants()).orElse(false);
    if (Strings.isNullOrEmpty(dataSetRequest.getName())) {
      throw new BadRequestException("Missing name");
    } else if (CollectionUtils.isEmpty(dataSetRequest.getConceptSetIds())
        && dataSetRequest
            .getPrePackagedConceptSet()
            .containsAll(ImmutableList.of(PrePackagedConceptSetEnum.NONE))) {
      throw new BadRequestException("Missing concept set ids");
    } else if (CollectionUtils.isEmpty(dataSetRequest.getCohortIds()) && !includesAllParticipants) {
      throw new BadRequestException("Missing cohort ids");
    } else if (CollectionUtils.isEmpty(dataSetRequest.getDomainValuePairs())) {
      throw new BadRequestException("Missing values");
    }
  }

  @VisibleForTesting
  public String generateRandomEightCharacterQualifier() {
    return prefixProvider.get();
  }

  public ResponseEntity<DataSetCodeResponse> generateCode(
      String workspaceNamespace,
      String workspaceId,
      String kernelTypeEnumString,
      DataSetRequest dataSetRequest) {
    DbWorkspace dbWorkspace =
        workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    final KernelTypeEnum kernelTypeEnum = KernelTypeEnum.fromValue(kernelTypeEnumString);

    // Generate query per domain for the selected concept set, cohort and values
    // TODO(jaycarlton): return better error information form this function for common validation
    // scenarios
    if (dataSetRequest.getWorkspaceId() == null) {
      dataSetRequest.setWorkspaceId(dbWorkspace.getWorkspaceId());
    }
    final Map<String, QueryJobConfiguration> bigQueryJobConfigsByDomain =
        dataSetService.domainToBigQueryConfig(dataSetRequest);

    if (bigQueryJobConfigsByDomain.isEmpty()) {
      log.warning("Empty query map generated for this DataSetRequest");
    }

    String qualifier = generateRandomEightCharacterQualifier();

    final ImmutableList<String> codeCells =
        ImmutableList.copyOf(
            dataSetService.generateCodeCells(
                kernelTypeEnum,
                dataSetRequest.getName(),
                dbWorkspace.getCdrVersion().getName(),
                qualifier,
                bigQueryJobConfigsByDomain));
    final String generatedCode = String.join("\n\n", codeCells);

    return ResponseEntity.ok(
        new DataSetCodeResponse().code(generatedCode).kernelType(kernelTypeEnum));
  }

  @Override
  public ResponseEntity<DataSetPreviewResponse> previewDataSetByDomain(
      String workspaceNamespace, String workspaceId, DataSetPreviewRequest dataSetPreviewRequest) {
    workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    List<DataSetPreviewValueList> valuePreviewList = new ArrayList<>();

    TableResult queryResponse = dataSetService.previewBigQueryJobConfig(dataSetPreviewRequest);

    if (queryResponse.getTotalRows() != 0) {
      valuePreviewList.addAll(
          queryResponse.getSchema().getFields().stream()
              .map(fields -> new DataSetPreviewValueList().value(fields.getName()))
              .collect(Collectors.toList()));

      queryResponse
          .getValues()
          .forEach(
              fieldValueList ->
                  addFieldValuesFromBigQueryToPreviewList(valuePreviewList, fieldValueList));

      queryResponse
          .getSchema()
          .getFields()
          .forEach(fields -> formatTimestampValues(valuePreviewList, fields));

      Collections.sort(
          valuePreviewList,
          Comparator.comparing(item -> dataSetPreviewRequest.getValues().indexOf(item.getValue())));
    }
    return ResponseEntity.ok(
        new DataSetPreviewResponse()
            .domain(dataSetPreviewRequest.getDomain())
            .values(valuePreviewList));
  }

  @VisibleForTesting
  public void addFieldValuesFromBigQueryToPreviewList(
      List<DataSetPreviewValueList> valuePreviewList, FieldValueList fieldValueList) {
    IntStream.range(0, fieldValueList.size())
        .forEach(
            columnNumber ->
                valuePreviewList
                    .get(columnNumber)
                    .addQueryValueItem(
                        Optional.ofNullable(fieldValueList.get(columnNumber).getValue())
                            .map(Object::toString)
                            .orElse(EMPTY_CELL_MARKER)));
  }

  // Iterates through all values associated with a specific field, and converts all timestamps
  // to a timestamp formatted string.
  private void formatTimestampValues(List<DataSetPreviewValueList> valuePreviewList, Field field) {
    DataSetPreviewValueList previewValue =
        valuePreviewList.stream()
            .filter(preview -> preview.getValue().equalsIgnoreCase(field.getName()))
            .findFirst()
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Value should be present when it is not in dataset preview request"));
    if (field.getType() == LegacySQLTypeName.TIMESTAMP) {
      List<String> queryValues = new ArrayList<>();
      DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT_STRING);
      previewValue
          .getQueryValue()
          .forEach(
              value -> {
                if (!value.equals(EMPTY_CELL_MARKER)) {
                  Double fieldValue = Double.parseDouble(value);
                  queryValues.add(
                      dateFormat.format(Date.from(Instant.ofEpochSecond(fieldValue.longValue()))));
                } else {
                  queryValues.add(value);
                }
              });
      previewValue.setQueryValue(queryValues);
    }
  }

  @Override
  public ResponseEntity<EmptyResponse> exportToNotebook(
      String workspaceNamespace, String workspaceId, DataSetExportRequest dataSetExportRequest) {
    DbWorkspace dbWorkspace =
        workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER);
    workspaceAuthService.validateActiveBilling(workspaceNamespace, workspaceId);
    // This suppresses 'may not be initialized errors. We will always init to something else before
    // used.
    JSONObject notebookFile = new JSONObject();
    FirecloudWorkspaceResponse workspace =
        fireCloudService.getWorkspace(workspaceNamespace, workspaceId);
    JSONObject metaData = new JSONObject();

    if (!dataSetExportRequest.getNewNotebook()) {
      notebookFile =
          notebooksService.getNotebookContents(
              workspace.getWorkspace().getBucketName(), dataSetExportRequest.getNotebookName());
      try {
        String language =
            Optional.of(notebookFile.getJSONObject("metadata"))
                .flatMap(metaDataObj -> Optional.of(metaDataObj.getJSONObject("kernelspec")))
                .map(kernelSpec -> kernelSpec.getString("language"))
                .orElse("Python");
        if ("R".equals(language)) {
          dataSetExportRequest.setKernelType(KernelTypeEnum.R);
        } else {
          dataSetExportRequest.setKernelType(KernelTypeEnum.PYTHON);
        }
      } catch (JSONException e) {
        // If we can't find metadata to parse, default to python.
        dataSetExportRequest.setKernelType(KernelTypeEnum.PYTHON);
      }
    } else {
      switch (dataSetExportRequest.getKernelType()) {
        case PYTHON:
          break;
        case R:
          metaData
              .put(
                  "kernelspec",
                  new JSONObject().put("display_name", "R").put("language", "R").put("name", "ir"))
              .put(
                  "language_info",
                  new JSONObject()
                      .put("codemirror_mode", "r")
                      .put("file_extension", ".r")
                      .put("mimetype", "text/x-r-source")
                      .put("name", "r")
                      .put("pygments_lexer", "r")
                      .put("version", "3.4.4"));
          break;
        default:
          throw new BadRequestException(
              "Kernel Type " + dataSetExportRequest.getKernelType() + " is not supported");
      }
    }

    if (dataSetExportRequest.getDataSetRequest().getWorkspaceId() == null) {
      dataSetExportRequest.getDataSetRequest().setWorkspaceId(dbWorkspace.getWorkspaceId());
    }
    Map<String, QueryJobConfiguration> queriesByDomain =
        dataSetService.domainToBigQueryConfig(dataSetExportRequest.getDataSetRequest());

    String qualifier = generateRandomEightCharacterQualifier();

    List<String> queriesAsStrings =
        dataSetService.generateCodeCells(
            dataSetExportRequest.getKernelType(),
            dataSetExportRequest.getDataSetRequest().getName(),
            dbWorkspace.getCdrVersion().getName(),
            qualifier,
            queriesByDomain);

    if (GenomicsDataTypeEnum.MICROARRAY.equals(dataSetExportRequest.getGenomicsDataType())) {
      if (dbWorkspace.getCdrVersion().getMicroarrayBigqueryDataset() == null) {
        throw new FailedPreconditionException(
            "The workspace CDR version does not have microarray data");
      }
      if (!dataSetExportRequest.getKernelType().equals(KernelTypeEnum.PYTHON)) {
        throw new BadRequestException("Genomics code generation is only supported in Python");
      }

      queriesAsStrings.addAll(
          dataSetService.generateMicroarrayCohortExtractCodeCells(
              dbWorkspace, qualifier, queriesByDomain));

      if (GenomicsAnalysisToolEnum.PLINK.equals(dataSetExportRequest.getGenomicsAnalysisTool())) {
        queriesAsStrings.addAll(dataSetService.generatePlinkDemoCode(qualifier));
      } else if (GenomicsAnalysisToolEnum.HAIL.equals(
          dataSetExportRequest.getGenomicsAnalysisTool())) {
        queriesAsStrings.addAll(dataSetService.generateHailDemoCode(qualifier));
      }
    }

    if (dataSetExportRequest.getNewNotebook()) {
      notebookFile =
          new JSONObject()
              .put("cells", new JSONArray())
              .put("metadata", metaData)
              // nbformat and nbformat_minor are the notebook major and minor version we are
              // creating.
              // Specifically, here we create notebook version 4.2 (I believe)
              // See https://nbformat.readthedocs.io/en/latest/api.html
              .put("nbformat", 4)
              .put("nbformat_minor", 2);
    }
    for (String query : queriesAsStrings) {
      notebookFile.getJSONArray("cells").put(createNotebookCodeCellWithString(query));
    }

    notebooksService.saveNotebook(
        workspace.getWorkspace().getBucketName(),
        dataSetExportRequest.getNotebookName(),
        notebookFile);

    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  public ResponseEntity<Boolean> markDirty(
      String workspaceNamespace, String workspaceId, MarkDataSetRequest markDataSetRequest) {
    workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER);
    dataSetService.markDirty(markDataSetRequest.getResourceType(), markDataSetRequest.getId());
    return ResponseEntity.ok(true);
  }

  @Override
  public ResponseEntity<EmptyResponse> deleteDataSet(
      String workspaceNamespace, String workspaceId, Long dataSetId) {
    workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER);

    dataSetService.deleteDataSet(dataSetId);
    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  public ResponseEntity<DataSet> updateDataSet(
      String workspaceNamespace, String workspaceId, Long dataSetId, DataSetRequest request) {
    if (Strings.isNullOrEmpty(request.getEtag())) {
      throw new BadRequestException("missing required update field 'etag'");
    }
    long dataSetWorkspaceId =
        workspaceAuthService
            .getWorkspaceEnforceAccessLevelAndSetCdrVersion(
                workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER)
            .getWorkspaceId();
    request.setWorkspaceId(dataSetWorkspaceId);
    return ResponseEntity.ok(dataSetService.updateDataSet(request, dataSetId));
  }

  @Override
  public ResponseEntity<DataSet> getDataSet(
      String workspaceNamespace, String workspaceId, Long dataSetId) {
    workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);

    DataSet dataSet =
        dataSetService
            .getDataSet(dataSetId)
            .<NotFoundException>orElseThrow(
                () -> {
                  throw new NotFoundException("No DataSet found for dataSetId: " + dataSetId);
                });
    return ResponseEntity.ok(dataSet);
  }

  @Override
  public ResponseEntity<DataSetListResponse> getDataSetByResourceId(
      String workspaceNamespace, String workspaceId, ResourceType resourceType, Long id) {
    workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);

    return ResponseEntity.ok(
        new DataSetListResponse().items(dataSetService.getDataSets(resourceType, id)));
  }

  @Override
  public ResponseEntity<DataDictionaryEntry> getDataDictionaryEntry(
      Long cdrVersionId, String domain, String value) {
    DbCdrVersion cdrVersion =
        cdrVersionService
            .findByCdrVersionId(cdrVersionId)
            .<BadRequestException>orElseThrow(
                () -> {
                  throw new BadRequestException("Invalid CDR Version");
                });
    CdrVersionContext.setCdrVersionNoCheckAuthDomain(cdrVersion);

    if (BigQueryTableInfo.getTableName(Domain.fromValue(domain)) == null) {
      throw new BadRequestException("Invalid Domain");
    }

    return ResponseEntity.ok(dataSetService.findDataDictionaryEntry(value, domain));
  }

  @Override
  public ResponseEntity<DomainValuesResponse> getValuesFromDomain(
      String workspaceNamespace, String workspaceId, String domainValue) {
    workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    DomainValuesResponse response = new DomainValuesResponse();
    if (domainValue.equals(Domain.WHOLE_GENOME_VARIANT.toString())) {
      response.addItemsItem(new DomainValue().value(WHOLE_GENOME_VALUE));
    } else {
      response.setItems(dataSetService.getValueListFromDomain(domainValue));
    }

    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<GenomicExtractionJob> extractGenomicData(
      String workspaceNamespace, String workspaceId, Long dataSetId) {
    DbWorkspace workspace =
        workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER);
    if (workspace.getCdrVersion().getWgsBigqueryDataset() == null) {
      throw new BadRequestException("Workspace CDR does not have access to WGS data");
    }

    DbDataset dataSet =
        dataSetService
            .getDbDataSet(dataSetId)
            .<NotFoundException>orElseThrow(
                () -> {
                  throw new NotFoundException("No DataSet found for dataSetId: " + dataSetId);
                });
    try {
      return ResponseEntity.ok(
          genomicExtractionService.submitGenomicExtractionJob(workspace, dataSet));
    } catch (org.pmiops.workbench.firecloud.ApiException e) {
      // Our usage of Terra is an internal implementation detail to the client. Any error returned
      // from Firecloud is either a bug within our Cromwell integration or a backend failure.
      throw new ServerErrorException(e);
    }
  }

  @Override
  public ResponseEntity<GenomicExtractionJobListResponse> getGenomicExtractionJobs(
      String workspaceNamespace, String workspaceId) {
    return ResponseEntity.ok(
        new GenomicExtractionJobListResponse()
            .jobs(
                genomicExtractionService.getGenomicExtractionJobs(
                    workspaceNamespace, workspaceId)));
  }

  // TODO(jaycarlton) create a class that knows about code cells and their properties,
  // then give it a toJson() method to replace this one.
  private JSONObject createNotebookCodeCellWithString(String cellInformation) {
    return new JSONObject()
        .put("cell_type", "code")
        .put("metadata", new JSONObject())
        .put("execution_count", JSONObject.NULL)
        .put("outputs", new JSONArray())
        .put("source", new JSONArray().put(cellInformation));
  }
}
