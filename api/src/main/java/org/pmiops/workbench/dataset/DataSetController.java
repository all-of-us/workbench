package org.pmiops.workbench.dataset;

import com.google.cloud.bigquery.LegacySQLTypeName;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableResult;
import com.google.common.base.Strings;
import java.sql.Date;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;
import javax.inject.Provider;
import javax.persistence.OptimisticLockException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.api.CohortsController;
import org.pmiops.workbench.api.ConceptSetsController;
import org.pmiops.workbench.api.ConceptsController;
import org.pmiops.workbench.api.DataSetApiDelegate;
import org.pmiops.workbench.api.Etags;
import org.pmiops.workbench.cdr.dao.ConceptDao;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.ConceptSetDao;
import org.pmiops.workbench.db.dao.DataSetDao;
import org.pmiops.workbench.db.model.DataSetValues;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.GatewayTimeoutException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.WorkspaceResponse;
import org.pmiops.workbench.model.ConceptSet;
import org.pmiops.workbench.model.DataSet;
import org.pmiops.workbench.model.DataSetCodeResponse;
import org.pmiops.workbench.model.DataSetExportRequest;
import org.pmiops.workbench.model.DataSetListResponse;
import org.pmiops.workbench.model.DataSetPreviewList;
import org.pmiops.workbench.model.DataSetPreviewResponse;
import org.pmiops.workbench.model.DataSetPreviewValueList;
import org.pmiops.workbench.model.DataSetRequest;
import org.pmiops.workbench.model.DomainValuePair;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.KernelTypeEnum;
import org.pmiops.workbench.model.MarkDataSetRequest;
import org.pmiops.workbench.model.PrePackagedConceptSetEnum;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.notebooks.NotebooksService;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DataSetController implements DataSetApiDelegate {

  private BigQueryService bigQueryService;
  private final Clock clock;
  private DataSetService dataSetService;

  private Provider<User> userProvider;
  private final WorkspaceService workspaceService;

  private static int NO_OF_PREVIEW_ROWS = 20;
  private static int PREVIEW_RETRY_LIMIT = 2;
  // See https://cloud.google.com/appengine/articles/deadlineexceedederrors for details
  private static long APP_ENGINE_HARD_TIMEOUT_MSEC_MINUS_FIVE_SEC = 55000l;
  private static String CONCEPT_SET = "conceptSet";
  private static String COHORT = "cohort";

  private static final Logger log = Logger.getLogger(DataSetController.class.getName());

  @Autowired private final CohortDao cohortDao;

  @Autowired private ConceptDao conceptDao;

  @Autowired private ConceptSetDao conceptSetDao;

  @Autowired private DataSetDao dataSetDao;

  @Autowired private FireCloudService fireCloudService;

  @Autowired private NotebooksService notebooksService;

  @Autowired
  DataSetController(
      BigQueryService bigQueryService,
      Clock clock,
      CohortDao cohortDao,
      ConceptDao conceptDao,
      ConceptSetDao conceptSetDao,
      DataSetDao dataSetDao,
      DataSetService dataSetService,
      FireCloudService fireCloudService,
      NotebooksService notebooksService,
      Provider<User> userProvider,
      WorkspaceService workspaceService) {
    this.bigQueryService = bigQueryService;
    this.clock = clock;
    this.cohortDao = cohortDao;
    this.conceptDao = conceptDao;
    this.conceptSetDao = conceptSetDao;
    this.dataSetDao = dataSetDao;
    this.dataSetService = dataSetService;
    this.fireCloudService = fireCloudService;
    this.notebooksService = notebooksService;
    this.userProvider = userProvider;
    this.workspaceService = workspaceService;
  }

  @Override
  public ResponseEntity<DataSet> createDataSet(
      String workspaceNamespace, String workspaceId, DataSetRequest dataSetRequest) {
    boolean includesAllParticipants =
        Optional.of(dataSetRequest.getIncludesAllParticipants()).orElse(false);
    if (Strings.isNullOrEmpty(dataSetRequest.getName())) {
      throw new BadRequestException("Missing name");
    } else if (dataSetRequest.getConceptSetIds() == null
        || (dataSetRequest.getConceptSetIds().size() == 0
            && dataSetRequest.getPrePackagedConceptSet().equals(PrePackagedConceptSetEnum.NONE))) {
      throw new BadRequestException("Missing concept set ids");
    } else if ((dataSetRequest.getCohortIds() == null || dataSetRequest.getCohortIds().size() == 0)
        && !includesAllParticipants) {
      throw new BadRequestException("Missing cohort ids");
    } else if (dataSetRequest.getValues() == null || dataSetRequest.getValues().size() == 0) {
      throw new BadRequestException("Missing values");
    }
    Timestamp now = new Timestamp(clock.instant().toEpochMilli());
    workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER);
    long wId = workspaceService.get(workspaceNamespace, workspaceId).getWorkspaceId();
    List<DataSetValues> dataSetValuesList =
        dataSetRequest.getValues().stream()
            .map(
                (domainValueSet) -> {
                  DataSetValues dataSetValues =
                      new DataSetValues(
                          domainValueSet.getDomain().name(), domainValueSet.getValue());
                  dataSetValues.setDomainEnum(domainValueSet.getDomain());
                  return dataSetValues;
                })
            .collect(Collectors.toList());
    try {
      org.pmiops.workbench.db.model.DataSet savedDataSet =
          dataSetService.saveDataSet(
              dataSetRequest.getName(),
              dataSetRequest.getIncludesAllParticipants(),
              dataSetRequest.getDescription(),
              wId,
              dataSetRequest.getCohortIds(),
              dataSetRequest.getConceptSetIds(),
              dataSetValuesList,
              dataSetRequest.getPrePackagedConceptSet(),
              userProvider.get().getUserId(),
              now);
      return ResponseEntity.ok(TO_CLIENT_DATA_SET.apply(savedDataSet));
    } catch (DataIntegrityViolationException ex) {
      throw new ConflictException("Data set with the same name already exists");
    }
  }

  private final Function<org.pmiops.workbench.db.model.DataSet, DataSet> TO_CLIENT_DATA_SET =
      new Function<org.pmiops.workbench.db.model.DataSet, DataSet>() {
        @Override
        public DataSet apply(org.pmiops.workbench.db.model.DataSet dataSet) {
          DataSet result =
              new DataSet()
                  .name(dataSet.getName())
                  .includesAllParticipants(dataSet.getIncludesAllParticipants())
                  .id(dataSet.getDataSetId())
                  .etag(Etags.fromVersion(dataSet.getVersion()))
                  .description(dataSet.getDescription())
                  .prePackagedConceptSet(dataSet.getPrePackagedConceptSetEnum());
          if (dataSet.getLastModifiedTime() != null) {
            result.setLastModifiedTime(dataSet.getLastModifiedTime().getTime());
          }
          result.setConceptSets(
              StreamSupport.stream(
                      conceptSetDao
                          .findAll(
                              dataSet.getConceptSetId().stream()
                                  .filter((concept) -> concept != null)
                                  .collect(Collectors.toList()))
                          .spliterator(),
                      false)
                  .map(conceptSet -> toClientConceptSet(conceptSet))
                  .collect(Collectors.toList()));
          result.setCohorts(
              StreamSupport.stream(cohortDao.findAll(dataSet.getCohortSetId()).spliterator(), false)
                  .map(CohortsController.TO_CLIENT_COHORT)
                  .collect(Collectors.toList()));
          result.setValues(
              dataSet.getValues().stream()
                  .map(TO_CLIENT_DOMAIN_VALUE)
                  .collect(Collectors.toList()));
          return result;
        }
      };

  private ConceptSet toClientConceptSet(org.pmiops.workbench.db.model.ConceptSet conceptSet) {
    ConceptSet result = ConceptSetsController.TO_CLIENT_CONCEPT_SET.apply(conceptSet);
    if (!conceptSet.getConceptIds().isEmpty()) {
      Iterable<org.pmiops.workbench.cdr.model.Concept> concepts =
          conceptDao.findAll(conceptSet.getConceptIds());
      result.setConcepts(
          StreamSupport.stream(concepts.spliterator(), false)
              .map(ConceptsController.TO_CLIENT_CONCEPT)
              .collect(Collectors.toList()));
    }
    return result;
  }

  static final Function<DataSetValues, DomainValuePair> TO_CLIENT_DOMAIN_VALUE =
      new Function<DataSetValues, DomainValuePair>() {
        @Override
        public DomainValuePair apply(DataSetValues dataSetValue) {
          DomainValuePair domainValuePair = new DomainValuePair();
          domainValuePair.setValue(dataSetValue.getValue());
          domainValuePair.setDomain(dataSetValue.getDomainEnum());
          return domainValuePair;
        }
      };

  public ResponseEntity<DataSetCodeResponse> generateCode(
      String workspaceNamespace,
      String workspaceId,
      String kernelTypeEnum,
      DataSetRequest dataSet) {
    workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    KernelTypeEnum kernelType = KernelTypeEnum.fromValue(kernelTypeEnum);

    // Generate query per domain for the selected concept set, cohort and values
    Map<String, QueryJobConfiguration> bigQueryJobConfig = dataSetService.generateQuery(dataSet);

    return ResponseEntity.ok(
        new DataSetCodeResponse()
            .code(
                dataSetService
                    .generateCodeCellPerDomainFromQueryAndKernelType(
                        kernelType, dataSet.getName(), bigQueryJobConfig)
                    .stream()
                    .collect(Collectors.joining("\n\n")))
            .kernelType(kernelType));
  }

  @Override
  public ResponseEntity<DataSetPreviewResponse> previewQuery(
      String workspaceNamespace, String workspaceId, DataSetRequest dataSet) {
    workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    DataSetPreviewResponse previewQueryResponse = new DataSetPreviewResponse();
    Map<String, QueryJobConfiguration> bigQueryJobConfig = dataSetService.generateQuery(dataSet);
    bigQueryJobConfig.forEach(
        (domain, queryJobConfiguration) -> {
          int retry = 0, rowsRequested = NO_OF_PREVIEW_ROWS;
          List<DataSetPreviewValueList> valuePreviewList = new ArrayList<>();
          String originalQuery = queryJobConfiguration.getQuery();
          do {
            try {
              String query = originalQuery.concat(" LIMIT " + rowsRequested);

              queryJobConfiguration = queryJobConfiguration.toBuilder().setQuery(query).build();

              /* Google appengine has a 60 second timeout, we want to make sure this endpoint completes
               * before that limit is exceeded, or we get a 500 error with the following type:
               * com.google.apphosting.runtime.HardDeadlineExceededError
               * See https://cloud.google.com/appengine/articles/deadlineexceedederrors for details
               */
              TableResult queryResponse =
                  bigQueryService.executeQuery(
                      bigQueryService.filterBigQueryConfig(queryJobConfiguration),
                      APP_ENGINE_HARD_TIMEOUT_MSEC_MINUS_FIVE_SEC / PREVIEW_RETRY_LIMIT + 1);
              queryResponse
                  .getSchema()
                  .getFields()
                  .forEach(
                      fields -> {
                        valuePreviewList.add(new DataSetPreviewValueList().value(fields.getName()));
                      });

              queryResponse
                  .getValues()
                  .forEach(
                      fieldValueList -> {
                        IntStream.range(0, fieldValueList.size())
                            .forEach(
                                columnNumber -> {
                                  try {
                                    valuePreviewList
                                        .get(columnNumber)
                                        .addQueryValueItem(
                                            fieldValueList.get(columnNumber).getValue().toString());
                                  } catch (NullPointerException ex) {
                                    log.severe(
                                        String.format(
                                            "Null pointer exception while retriving value for query: Column %s ",
                                            columnNumber));
                                    valuePreviewList.get(columnNumber).addQueryValueItem("");
                                  }
                                });
                      });
              queryResponse
                  .getSchema()
                  .getFields()
                  .forEach(
                      fields -> {
                        DataSetPreviewValueList previewValue =
                            valuePreviewList.stream()
                                .filter(
                                    preview ->
                                        preview.getValue().equalsIgnoreCase(fields.getName()))
                                .findFirst()
                                .get();
                        if (fields.getType() == LegacySQLTypeName.TIMESTAMP) {
                          List<String> queryValues = new ArrayList<String>();
                          DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                          previewValue
                              .getQueryValue()
                              .forEach(
                                  value -> {
                                    try {
                                      Double fieldValue = Double.parseDouble(value);
                                      queryValues.add(
                                          dateFormat.format(new Date(fieldValue.longValue())));
                                    } catch (NumberFormatException ex) {
                                      queryValues.add("");
                                    }
                                  });
                          previewValue.setQueryValue(queryValues);
                        }
                      });
              break;
            } catch (Exception ex) {
              if ((ex.getCause() != null
                  && ex.getCause().getMessage() != null
                  && ex.getCause().getMessage().contains("Read timed out"))) {
                rowsRequested = (rowsRequested / 2);
                if (rowsRequested == 0) {
                  throw new GatewayTimeoutException(
                      "Timeout while querying the CDR to pull preview information.");
                }
                retry++;
              } else {
                throw ex;
              }
            }
          } while (retry < PREVIEW_RETRY_LIMIT);

          if (retry == PREVIEW_RETRY_LIMIT) {
            throw new GatewayTimeoutException(
                "Timeout while querying the CDR to pull preview information.");
          }

          Collections.sort(
              valuePreviewList,
              Comparator.comparing(item -> dataSet.getValues().indexOf(item.getValue())));

          previewQueryResponse.addDomainValueItem(
              new DataSetPreviewList().domain(domain).values(valuePreviewList));
        });
    return ResponseEntity.ok(previewQueryResponse);
  }

  @Override
  public ResponseEntity<EmptyResponse> exportToNotebook(
      String workspaceNamespace, String workspaceId, DataSetExportRequest dataSetExportRequest) {
    workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER);
    // This suppresses 'may not be initialized errors. We will always init to something else before
    // used.
    JSONObject notebookFile = new JSONObject();
    WorkspaceResponse workspace = fireCloudService.getWorkspace(workspaceNamespace, workspaceId);
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

    Map<String, QueryJobConfiguration> queryList =
        dataSetService.generateQuery(dataSetExportRequest.getDataSetRequest());
    List<String> queriesAsStrings =
        dataSetService.generateCodeCellPerDomainFromQueryAndKernelType(
            dataSetExportRequest.getKernelType(),
            dataSetExportRequest.getDataSetRequest().getName(),
            queryList);

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
  public ResponseEntity<DataSetListResponse> getDataSetsInWorkspace(
      String workspaceNamespace, String workspaceId) {
    Workspace workspace =
        workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);

    List<org.pmiops.workbench.db.model.DataSet> dataSets =
        dataSetDao.findByWorkspaceIdAndInvalid(workspace.getWorkspaceId(), false);
    DataSetListResponse response = new DataSetListResponse();

    response.setItems(
        dataSets.stream()
            .map(TO_CLIENT_DATA_SET)
            .sorted(Comparator.comparing(DataSet::getName))
            .collect(Collectors.toList()));
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<Boolean> markDirty(
      String workspaceNamespace, String workspaceId, MarkDataSetRequest markDataSetRequest) {
    workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER);
    List<org.pmiops.workbench.db.model.DataSet> dbDataSetList = new ArrayList<>();
    if (markDataSetRequest.getResourceType().equalsIgnoreCase(COHORT)) {
      dbDataSetList = dataSetDao.findDataSetsByCohortSetId(markDataSetRequest.getId());
    } else if (markDataSetRequest.getResourceType().equalsIgnoreCase(CONCEPT_SET)) {
      dbDataSetList = dataSetDao.findDataSetsByConceptSetId(markDataSetRequest.getId());
    }
    dbDataSetList =
        dbDataSetList.stream()
            .map(
                dataSet -> {
                  dataSet.setInvalid(true);
                  return dataSet;
                })
            .collect(Collectors.toList());
    try {
      dataSetDao.save(dbDataSetList);
    } catch (OptimisticLockException e) {
      throw new ConflictException("Failed due to concurrent data set modification");
    }

    return ResponseEntity.ok(true);
  }

  @Override
  public ResponseEntity<EmptyResponse> deleteDataSet(
      String workspaceNamespace, String workspaceId, Long dataSetId) {
    org.pmiops.workbench.db.model.DataSet dataSet =
        getDbDataSet(workspaceNamespace, workspaceId, dataSetId, WorkspaceAccessLevel.WRITER);
    dataSetDao.delete(dataSet.getDataSetId());
    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  public ResponseEntity<DataSet> updateDataSet(
      String workspaceNamespace, String workspaceId, Long dataSetId, DataSetRequest request) {
    org.pmiops.workbench.db.model.DataSet dbDataSet =
        getDbDataSet(workspaceNamespace, workspaceId, dataSetId, WorkspaceAccessLevel.WRITER);
    if (Strings.isNullOrEmpty(request.getEtag())) {
      throw new BadRequestException("missing required update field 'etag'");
    }
    int version = Etags.toVersion(request.getEtag());
    if (dbDataSet.getVersion() != version) {
      throw new ConflictException("Attempted to modify outdated data set version");
    }
    Timestamp now = new Timestamp(clock.instant().toEpochMilli());
    dbDataSet.setLastModifiedTime(now);
    dbDataSet.setIncludesAllParticipants(request.getIncludesAllParticipants());
    dbDataSet.setCohortSetId(request.getCohortIds());
    dbDataSet.setConceptSetId(request.getConceptSetIds());
    dbDataSet.setDescription(request.getDescription());
    dbDataSet.setName(request.getName());
    dbDataSet.setPrePackagedConceptSetEnum(request.getPrePackagedConceptSet());
    dbDataSet.setValues(
        request.getValues().stream()
            .map(
                (domainValueSet) -> {
                  DataSetValues dataSetValues =
                      new DataSetValues(
                          domainValueSet.getDomain().name(), domainValueSet.getValue());
                  dataSetValues.setDomainEnum(domainValueSet.getDomain());
                  return dataSetValues;
                })
            .collect(Collectors.toList()));
    try {
      dbDataSet = dataSetDao.save(dbDataSet);
      // TODO: add recent resource entry for data sets
    } catch (OptimisticLockException e) {
      throw new ConflictException("Failed due to concurrent concept set modification");
    } catch (DataIntegrityViolationException ex) {
      throw new ConflictException("Data set with the same name already exists");
    }

    return ResponseEntity.ok(TO_CLIENT_DATA_SET.apply(dbDataSet));
  }

  @Override
  public ResponseEntity<DataSet> getDataSet(
      String workspaceNamespace, String workspaceId, Long dataSetId) {
    org.pmiops.workbench.db.model.DataSet dataSet =
        getDbDataSet(workspaceNamespace, workspaceId, dataSetId, WorkspaceAccessLevel.READER);
    return ResponseEntity.ok(TO_CLIENT_DATA_SET.apply(dataSet));
  }

  @Override
  public ResponseEntity<DataSetListResponse> getDataSetByResourceId(
      String workspaceNamespace, String workspaceId, String resourceType, Long id) {
    workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);

    List<org.pmiops.workbench.db.model.DataSet> dbDataSets =
        new ArrayList<org.pmiops.workbench.db.model.DataSet>();
    if (resourceType.equals(COHORT)) {
      dbDataSets = dataSetDao.findDataSetsByCohortSetId(id);
    } else if (resourceType.equals(CONCEPT_SET)) {
      dbDataSets = dataSetDao.findDataSetsByConceptSetId(id);
    }
    DataSetListResponse dataSetResponse =
        new DataSetListResponse()
            .items(
                dbDataSets.stream()
                    .map(dbDataSet -> TO_CLIENT_DATA_SET.apply(dbDataSet))
                    .collect(Collectors.toList()));
    return ResponseEntity.ok(dataSetResponse);
  }

  private JSONObject createNotebookCodeCellWithString(String cellInformation) {
    return new JSONObject()
        .put("cell_type", "code")
        .put("metadata", new JSONObject())
        .put("execution_count", JSONObject.NULL)
        .put("outputs", new JSONArray())
        .put("source", new JSONArray().put(cellInformation));
  }

  private org.pmiops.workbench.db.model.DataSet getDbDataSet(
      String workspaceNamespace,
      String workspaceId,
      Long dataSetId,
      WorkspaceAccessLevel workspaceAccessLevel) {
    Workspace workspace =
        workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            workspaceNamespace, workspaceId, workspaceAccessLevel);

    org.pmiops.workbench.db.model.DataSet dataSet = dataSetDao.findOne(dataSetId);
    if (dataSet == null || workspace.getWorkspaceId() != dataSet.getWorkspaceId()) {
      throw new NotFoundException(
          String.format(
              "No data set with ID %s in workspace %s.", dataSet, workspace.getFirecloudName()));
    }
    return dataSet;
  }
}
