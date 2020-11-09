package org.pmiops.workbench.api;

import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.FieldList;
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
import org.pmiops.workbench.cdr.CdrVersionService;
import org.pmiops.workbench.dataset.BigQueryTableInfo;
import org.pmiops.workbench.dataset.DatasetConfig;
import org.pmiops.workbench.dataset.DatasetService;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.FailedPreconditionException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceResponse;
import org.pmiops.workbench.model.DataDictionaryEntry;
import org.pmiops.workbench.model.Dataset;
import org.pmiops.workbench.model.DatasetCodeResponse;
import org.pmiops.workbench.model.DatasetExportRequest;
import org.pmiops.workbench.model.DatasetExportRequest.GenomicsAnalysisToolEnum;
import org.pmiops.workbench.model.DatasetExportRequest.GenomicsDataTypeEnum;
import org.pmiops.workbench.model.DatasetListResponse;
import org.pmiops.workbench.model.DatasetPreviewRequest;
import org.pmiops.workbench.model.DatasetPreviewResponse;
import org.pmiops.workbench.model.DatasetPreviewValueList;
import org.pmiops.workbench.model.DatasetRequest;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.DomainValue;
import org.pmiops.workbench.model.DomainValuesResponse;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.KernelTypeEnum;
import org.pmiops.workbench.model.MarkDatasetRequest;
import org.pmiops.workbench.model.PrePackagedConceptSetEnum;
import org.pmiops.workbench.model.ResourceType;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.notebooks.NotebooksService;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * For API compatibility, keep generated parent interface name `DatasetApiDelegate` but rename to
 * `DatasetController` for now.
 */
@RestController
public class DatasetController implements DatasetApiDelegate {

  private BigQueryService bigQueryService;
  private DatasetService datasetService;

  private Provider<DbUser> userProvider;
  private Provider<String> prefixProvider;
  private final WorkspaceService workspaceService;

  // See https://cloud.google.com/appengine/articles/deadlineexceedederrors for details
  private static final long APP_ENGINE_HARD_TIMEOUT_MSEC_MINUS_FIVE_SEC = 55000L;

  private static final String DATE_FORMAT_STRING = "yyyy/MM/dd HH:mm:ss";
  public static final String EMPTY_CELL_MARKER = "";

  private static final Logger log = Logger.getLogger(DatasetController.class.getName());

  private final CdrVersionService cdrVersionService;
  private final FireCloudService fireCloudService;
  private final NotebooksService notebooksService;

  @Autowired
  DatasetController(
      BigQueryService bigQueryService,
      CdrVersionService cdrVersionService,
      DatasetService datasetService,
      FireCloudService fireCloudService,
      NotebooksService notebooksService,
      Provider<DbUser> userProvider,
      @Qualifier(DatasetConfig.DATASET_PREFIX_CODE) Provider<String> prefixProvider,
      WorkspaceService workspaceService) {
    this.bigQueryService = bigQueryService;
    this.cdrVersionService = cdrVersionService;
    this.datasetService = datasetService;
    this.fireCloudService = fireCloudService;
    this.notebooksService = notebooksService;
    this.userProvider = userProvider;
    this.prefixProvider = prefixProvider;
    this.workspaceService = workspaceService;
  }

  @Override
  public ResponseEntity<Dataset> createDataset(
      String workspaceNamespace, String workspaceFirecloudName, DatasetRequest datasetRequest) {
    validateDatasetCreateRequest(datasetRequest);
    final long workspaceId =
        workspaceService
            .getWorkspaceEnforceAccessLevelAndSetCdrVersion(
                workspaceNamespace, workspaceFirecloudName, WorkspaceAccessLevel.WRITER)
            .getWorkspaceId();
    datasetRequest.setWorkspaceId(workspaceId);
    return ResponseEntity.ok(
        datasetService.saveDataset(datasetRequest, userProvider.get().getUserId()));
  }

  private void validateDatasetCreateRequest(DatasetRequest datasetRequest) {
    boolean includesAllParticipants =
        Optional.ofNullable(datasetRequest.getIncludesAllParticipants()).orElse(false);
    if (Strings.isNullOrEmpty(datasetRequest.getName())) {
      throw new BadRequestException("Missing name");
    } else if (datasetRequest.getConceptSetIds() == null
        || (datasetRequest.getConceptSetIds().isEmpty()
            && datasetRequest.getPrePackagedConceptSet().equals(PrePackagedConceptSetEnum.NONE))) {
      throw new BadRequestException("Missing concept set ids");
    } else if ((datasetRequest.getCohortIds() == null || datasetRequest.getCohortIds().isEmpty())
        && !includesAllParticipants) {
      throw new BadRequestException("Missing cohort ids");
    } else if (datasetRequest.getDomainValuePairs() == null
        || datasetRequest.getDomainValuePairs().isEmpty()) {
      throw new BadRequestException("Missing values");
    }
  }

  @VisibleForTesting
  public String generateRandomEightCharacterQualifier() {
    return prefixProvider.get();
  }

  public ResponseEntity<DatasetCodeResponse> generateCode(
      String workspaceNamespace,
      String workspaceId,
      String kernelTypeEnumString,
      DatasetRequest datasetRequest) {
    DbWorkspace dbWorkspace =
        workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    final KernelTypeEnum kernelTypeEnum = KernelTypeEnum.fromValue(kernelTypeEnumString);

    // Generate query per domain for the selected concept set, cohort and values
    // TODO(jaycarlton): return better error information form this function for common validation
    // scenarios
    if (datasetRequest.getWorkspaceId() == null) {
      datasetRequest.setWorkspaceId(dbWorkspace.getWorkspaceId());
    }
    final Map<String, QueryJobConfiguration> bigQueryJobConfigsByDomain =
        datasetService.domainToBigQueryConfig(datasetRequest);

    if (bigQueryJobConfigsByDomain.isEmpty()) {
      log.warning("Empty query map generated for this DatasetRequest");
    }

    String qualifier = generateRandomEightCharacterQualifier();

    final ImmutableList<String> codeCells =
        ImmutableList.copyOf(
            datasetService.generateCodeCells(
                kernelTypeEnum,
                datasetRequest.getName(),
                dbWorkspace.getCdrVersion().getName(),
                qualifier,
                bigQueryJobConfigsByDomain));
    final String generatedCode = String.join("\n\n", codeCells);

    return ResponseEntity.ok(
        new DatasetCodeResponse().code(generatedCode).kernelType(kernelTypeEnum));
  }

  @Override
  public ResponseEntity<DatasetPreviewResponse> previewDatasetByDomain(
      String workspaceNamespace, String workspaceId, DatasetPreviewRequest datasetPreviewRequest) {
    workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    List<DatasetPreviewValueList> valuePreviewList = new ArrayList<>();

    QueryJobConfiguration previewBigQueryJobConfig =
        datasetService.previewBigQueryJobConfig(datasetPreviewRequest);

    TableResult queryResponse =
        bigQueryService.executeQuery(
            bigQueryService.filterBigQueryConfig(previewBigQueryJobConfig),
            APP_ENGINE_HARD_TIMEOUT_MSEC_MINUS_FIVE_SEC);

    if (queryResponse.getTotalRows() != 0) {
      valuePreviewList.addAll(
          queryResponse.getSchema().getFields().stream()
              .map(fields -> new DatasetPreviewValueList().value(fields.getName()))
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
          Comparator.comparing(item -> datasetPreviewRequest.getValues().indexOf(item.getValue())));
    }
    return ResponseEntity.ok(
        new DatasetPreviewResponse()
            .domain(datasetPreviewRequest.getDomain())
            .values(valuePreviewList));
  }

  @VisibleForTesting
  public void addFieldValuesFromBigQueryToPreviewList(
      List<DatasetPreviewValueList> valuePreviewList, FieldValueList fieldValueList) {
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
  private void formatTimestampValues(List<DatasetPreviewValueList> valuePreviewList, Field field) {
    DatasetPreviewValueList previewValue =
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
      String workspaceNamespace, String workspaceId, DatasetExportRequest datasetExportRequest) {
    DbWorkspace dbWorkspace =
        workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER);
    workspaceService.validateActiveBilling(workspaceNamespace, workspaceId);
    // This suppresses 'may not be initialized errors. We will always init to something else before
    // used.
    JSONObject notebookFile = new JSONObject();
    FirecloudWorkspaceResponse workspace =
        fireCloudService.getWorkspace(workspaceNamespace, workspaceId);
    JSONObject metaData = new JSONObject();

    if (!datasetExportRequest.getNewNotebook()) {
      notebookFile =
          notebooksService.getNotebookContents(
              workspace.getWorkspace().getBucketName(), datasetExportRequest.getNotebookName());
      try {
        String language =
            Optional.of(notebookFile.getJSONObject("metadata"))
                .flatMap(metaDataObj -> Optional.of(metaDataObj.getJSONObject("kernelspec")))
                .map(kernelSpec -> kernelSpec.getString("language"))
                .orElse("Python");
        if ("R".equals(language)) {
          datasetExportRequest.setKernelType(KernelTypeEnum.R);
        } else {
          datasetExportRequest.setKernelType(KernelTypeEnum.PYTHON);
        }
      } catch (JSONException e) {
        // If we can't find metadata to parse, default to python.
        datasetExportRequest.setKernelType(KernelTypeEnum.PYTHON);
      }
    } else {
      switch (datasetExportRequest.getKernelType()) {
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
              "Kernel Type " + datasetExportRequest.getKernelType() + " is not supported");
      }
    }

    if (datasetExportRequest.getDatasetRequest().getWorkspaceId() == null) {
      datasetExportRequest.getDatasetRequest().setWorkspaceId(dbWorkspace.getWorkspaceId());
    }
    Map<String, QueryJobConfiguration> queriesByDomain =
        datasetService.domainToBigQueryConfig(datasetExportRequest.getDatasetRequest());

    String qualifier = generateRandomEightCharacterQualifier();

    List<String> queriesAsStrings =
        datasetService.generateCodeCells(
            datasetExportRequest.getKernelType(),
            datasetExportRequest.getDatasetRequest().getName(),
            dbWorkspace.getCdrVersion().getName(),
            qualifier,
            queriesByDomain);

    if (GenomicsDataTypeEnum.MICROARRAY.equals(datasetExportRequest.getGenomicsDataType())) {
      if (dbWorkspace.getCdrVersion().getMicroarrayBigqueryDataset() == null) {
        throw new FailedPreconditionException(
            "The workspace CDR version does not have microarray data");
      }
      if (!datasetExportRequest.getKernelType().equals(KernelTypeEnum.PYTHON)) {
        throw new BadRequestException("Genomics code generation is only supported in Python");
      }

      queriesAsStrings.addAll(
          datasetService.generateMicroarrayCohortExtractCodeCells(
              dbWorkspace, qualifier, queriesByDomain));

      if (GenomicsAnalysisToolEnum.PLINK.equals(datasetExportRequest.getGenomicsAnalysisTool())) {
        queriesAsStrings.addAll(datasetService.generatePlinkDemoCode(qualifier));
      } else if (GenomicsAnalysisToolEnum.HAIL.equals(
          datasetExportRequest.getGenomicsAnalysisTool())) {
        queriesAsStrings.addAll(datasetService.generateHailDemoCode(qualifier));
      }
    }

    if (datasetExportRequest.getNewNotebook()) {
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
        datasetExportRequest.getNotebookName(),
        notebookFile);

    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  public ResponseEntity<Boolean> markDirty(
      String workspaceNamespace, String workspaceId, MarkDatasetRequest markDatasetRequest) {
    workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER);
    datasetService.markDirty(markDatasetRequest.getResourceType(), markDatasetRequest.getId());
    return ResponseEntity.ok(true);
  }

  @Override
  public ResponseEntity<EmptyResponse> deleteDataset(
      String workspaceNamespace, String workspaceId, Long datasetId) {
    workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER);

    datasetService.deleteDataset(datasetId);
    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  public ResponseEntity<Dataset> updateDataset(
      String workspaceNamespace, String workspaceId, Long datasetId, DatasetRequest request) {
    if (Strings.isNullOrEmpty(request.getEtag())) {
      throw new BadRequestException("missing required update field 'etag'");
    }
    long datasetWorkspaceId =
        workspaceService
            .getWorkspaceEnforceAccessLevelAndSetCdrVersion(
                workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER)
            .getWorkspaceId();
    request.setWorkspaceId(datasetWorkspaceId);
    return ResponseEntity.ok(datasetService.updateDataset(request, datasetId));
  }

  @Override
  public ResponseEntity<Dataset> getDataset(
      String workspaceNamespace, String workspaceId, Long datasetId) {
    workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);

    Dataset dataset =
        datasetService
            .getDbDataset(datasetId)
            .<BadRequestException>orElseThrow(
                () -> {
                  throw new NotFoundException("No Dataset found for datasetId: " + datasetId);
                });
    return ResponseEntity.ok(dataset);
  }

  @Override
  public ResponseEntity<DatasetListResponse> getDatasetByResourceId(
      String workspaceNamespace, String workspaceId, ResourceType resourceType, Long id) {
    workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);

    return ResponseEntity.ok(
        new DatasetListResponse().items(datasetService.getDatasets(resourceType, id)));
  }

  @Override
  public ResponseEntity<DataDictionaryEntry> getDataDictionaryEntry(
      Long cdrVersionId, String domain, String domainValue) {
    DbCdrVersion cdrVersion =
        cdrVersionService
            .findByCdrVersionId(cdrVersionId)
            .<BadRequestException>orElseThrow(
                () -> {
                  throw new BadRequestException("Invalid CDR Version");
                });

    if (BigQueryTableInfo.getTableName(Domain.fromValue(domain)) == null) {
      throw new BadRequestException("Invalid Domain");
    }

    return ResponseEntity.ok(datasetService.findDataDictionaryEntry(domainValue, cdrVersion));
  }

  @Override
  public ResponseEntity<DomainValuesResponse> getValuesFromDomain(
      String workspaceNamespace, String workspaceId, String domainValue) {
    workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    DomainValuesResponse response = new DomainValuesResponse();

    FieldList fieldList = bigQueryService.getTableFieldsFromDomain(Domain.valueOf(domainValue));
    response.setItems(
        fieldList.stream()
            .map(field -> new DomainValue().value(field.getName().toLowerCase()))
            .collect(Collectors.toList()));

    return ResponseEntity.ok(response);
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
