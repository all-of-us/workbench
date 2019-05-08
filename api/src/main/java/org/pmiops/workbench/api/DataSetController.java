package org.pmiops.workbench.api;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.TableResult;

import com.google.common.base.Strings;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Clock;

import java.util.ArrayList;
import java.util.Map;
import java.util.List;

import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import javax.inject.Provider;

import org.json.JSONArray;
import org.json.JSONObject;
import org.pmiops.workbench.cdr.dao.ConceptDao;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.ConceptSetDao;
import org.pmiops.workbench.db.dao.DataSetService;
import org.pmiops.workbench.db.model.Cohort;
import org.pmiops.workbench.db.model.DataSetValues;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.WorkspaceResponse;
import org.pmiops.workbench.model.ConceptSet;
import org.pmiops.workbench.model.DataSet;
import org.pmiops.workbench.model.DataSetExportRequest;
import org.pmiops.workbench.model.DataSetPreviewList;
import org.pmiops.workbench.model.DataSetPreviewResponse;
import org.pmiops.workbench.model.DataSetPreviewValueList;
import org.pmiops.workbench.model.DataSetRequest;
import org.pmiops.workbench.model.DataSetQuery;
import org.pmiops.workbench.model.DataSetQueryList;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.DomainValuePair;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.NamedParameterEntry;
import org.pmiops.workbench.model.NamedParameterValue;
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
  private static final Logger log = Logger.getLogger(DataSetController.class.getName());

  @Autowired
  private final CohortDao cohortDao;

  @Autowired
  private ConceptDao conceptDao;

  @Autowired
  private ConceptSetDao conceptSetDao;

  @Autowired
  private FireCloudService fireCloudService;

  @Autowired
  private NotebooksService notebooksService;

  @Autowired
  DataSetController(
      BigQueryService bigQueryService,
      Clock clock,
      CohortDao cohortDao,
      ConceptDao conceptDao,
      ConceptSetDao conceptSetDao,
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
    this.dataSetService = dataSetService;
    this.fireCloudService = fireCloudService;
    this.notebooksService = notebooksService;
    this.userProvider = userProvider;
    this.workspaceService = workspaceService;
  }

  @Override
  public ResponseEntity<DataSet> createDataSet(String workspaceNamespace, String workspaceId,
      DataSetRequest dataSetRequest) {
    boolean includesAllParticipants = Optional.of(dataSetRequest.getIncludesAllParticipants()).orElse(false);
    if (Strings.isNullOrEmpty(dataSetRequest.getName())) {
      throw new BadRequestException("Missing name");
    } else if (dataSetRequest.getConceptSetIds() == null || dataSetRequest.getConceptSetIds().size() == 0) {
      throw new BadRequestException("Missing concept set ids");
    } else if ((dataSetRequest.getCohortIds() == null || dataSetRequest.getCohortIds().size() == 0) && !includesAllParticipants) {
      throw new BadRequestException("Missing cohort ids");
    } else if (dataSetRequest.getValues() == null || dataSetRequest.getValues().size() == 0) {
      throw new BadRequestException("Missing values");
    }
    Timestamp now = new Timestamp(clock.instant().toEpochMilli());
    workspaceService
        .getWorkspaceEnforceAccessLevelAndSetCdrVersion(workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER);
    long wId = workspaceService.get(workspaceNamespace, workspaceId).getWorkspaceId();
    List<DataSetValues> dataSetValuesList = dataSetRequest.getValues().stream().map(
        (domainValueSet) -> {
          DataSetValues dataSetValues = new DataSetValues(domainValueSet.getDomain().name(), domainValueSet.getValue());
          dataSetValues.setDomainEnum(domainValueSet.getDomain());
          return dataSetValues;
        }).collect(Collectors.toList());
    try {
      org.pmiops.workbench.db.model.DataSet savedDataSet = dataSetService.saveDataSet(
          dataSetRequest.getName(), dataSetRequest.getIncludesAllParticipants(),
          dataSetRequest.getDescription(), wId, dataSetRequest.getCohortIds(),
          dataSetRequest.getConceptSetIds(), dataSetValuesList, userProvider.get().getUserId(), now);
      return ResponseEntity.ok(TO_CLIENT_DATA_SET.apply(savedDataSet));
    } catch (DataIntegrityViolationException ex) {
      throw new ConflictException("Data set with the same name already exist");
    }
  }

  private final Function<org.pmiops.workbench.db.model.DataSet, DataSet> TO_CLIENT_DATA_SET =
      new Function<org.pmiops.workbench.db.model.DataSet, DataSet>() {
        @Override
        public DataSet apply(org.pmiops.workbench.db.model.DataSet dataSet) {
          DataSet result = new DataSet();
          result.setName(dataSet.getName());
          result.setIncludesAllParticipants(dataSet.getIncludesAllParticipants());
          Iterable<org.pmiops.workbench.db.model.ConceptSet> conceptSets =
              conceptSetDao.findAll(dataSet.getConceptSetId());
          result.setConceptSets(StreamSupport.stream(conceptSets.spliterator(), false)
              .map(conceptSet -> toClientConceptSet(conceptSet)).collect(Collectors.toList()));

          Iterable<Cohort> cohorts = cohortDao.findAll(dataSet.getCohortSetId());
          result.setCohorts(StreamSupport.stream(cohorts.spliterator(), false)
                .map(CohortsController.TO_CLIENT_COHORT)
                .collect(Collectors.toList()));
          result.setDescription(dataSet.getDescription());
          result.setValues(dataSet.getValues()
                  .stream()
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
      result.setConcepts(StreamSupport.stream(concepts.spliterator(), false)
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


  public ResponseEntity<DataSetQueryList> generateQuery(String workspaceNamespace, String workspaceId, DataSetRequest dataSet) {
    workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    List<DataSetQuery> respQueryList = new ArrayList<>();
    List<NamedParameterEntry> parameters = new ArrayList<>();

    // Generate query per domain for the selected concept set, cohort and values
    Map<String, QueryJobConfiguration> bigQueryJobConfig = dataSetService.generateQuery(dataSet);

    // Loop through and run the query for each domain and create LIST of
    // domain, value and their corresponding query result
    bigQueryJobConfig.forEach((domain, queryJobConfiguration) -> {
      queryJobConfiguration.getNamedParameters().forEach((key, value) ->
              parameters.add(generateResponseFromQueryParameter(key, value)));
      respQueryList.add(new DataSetQuery()
          .domain(Domain.fromValue(domain))
          .query(queryJobConfiguration.getQuery())
          .namedParameters(parameters));
    });
    return ResponseEntity.ok(new DataSetQueryList().queryList(respQueryList));
  }

  @Override
  public ResponseEntity<DataSetPreviewResponse> previewQuery(String workspaceNamespace,
      String workspaceId, DataSetRequest dataSet) {
    workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    DataSetPreviewResponse previewQueryResponse = new DataSetPreviewResponse();
    Map<String, QueryJobConfiguration> bigQueryJobConfig = dataSetService.generateQuery(dataSet);
    bigQueryJobConfig.forEach((domain, queryJobConfiguration) -> {
      String query = queryJobConfiguration.getQuery().concat(" LIMIT "+ NO_OF_PREVIEW_ROWS);
      queryJobConfiguration = queryJobConfiguration.toBuilder().setQuery(query).build();

      TableResult queryResponse = bigQueryService.executeQuery(bigQueryService
          .filterBigQueryConfig(queryJobConfiguration));

      // Construct a list of all values with empty results.
      List<DataSetPreviewValueList> valuePreviewList =
          dataSet.getValues().stream().filter(value -> value.getDomain().toString().equals(domain))
              .map(value -> new DataSetPreviewValueList().value(value.getValue())).collect(Collectors.toList());

      queryResponse.getValues().forEach(fieldValueList -> {
        IntStream.range(0, fieldValueList.size()).forEach(columnNumber -> {
          try {
            valuePreviewList.get(columnNumber).addQueryValueItem(
                fieldValueList.get(columnNumber).getValue().toString());
          } catch (NullPointerException ex) {
            log.severe(
                String.format("Null pointer exception while retriving value for query: Column %s ", columnNumber));
            valuePreviewList.get(columnNumber).addQueryValueItem("");
          }
        });
      });
      previewQueryResponse.addDomainValueItem(new DataSetPreviewList().domain(domain).values(valuePreviewList));
    });
    return ResponseEntity.ok(previewQueryResponse);
  }

  @Override
  public ResponseEntity<EmptyResponse> exportToNotebook(String workspaceNamespace, String workspaceId, DataSetExportRequest dataSetExportRequest) {
    workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER);
    DataSetQueryList queryList = generateQuery(workspaceNamespace, workspaceId, dataSetExportRequest.getDataSetRequest()).getBody();
    String queriesAsStrings = "import pandas\n\n" + queryList.getQueryList().stream()
        .map(query -> convertQueryToString(query, dataSetExportRequest.getDataSetRequest().getName()))
        .collect(Collectors.joining("\n\n"));
    JSONObject notebookFile;
    WorkspaceResponse workspace = fireCloudService.getWorkspace(workspaceNamespace, workspaceId);
    if (dataSetExportRequest.getNewNotebook()) {
       notebookFile = new JSONObject()
          .put("cells", new JSONArray()
              .put(createNotebookCodeCellWithString(queriesAsStrings)))
          .put("metadata", new JSONObject())
          // nbformat and nbformat_minor are the notebook major and minor version we are creating.
          // Specifically, here we create notebook version 4.2 (I believe)
          // See https://nbformat.readthedocs.io/en/latest/api.html
          .put("nbformat", 4)
          .put("nbformat_minor", 2);
    } else {
      notebookFile = notebooksService.getNotebookContents(
          workspace.getWorkspace().getBucketName(),
          dataSetExportRequest.getNotebookName());
      notebookFile.getJSONArray("cells").put(createNotebookCodeCellWithString(queriesAsStrings));
    }

    notebooksService.saveNotebook(workspace.getWorkspace().getBucketName(),
        dataSetExportRequest.getNotebookName(), notebookFile);

    return ResponseEntity.ok(new EmptyResponse());
  }

  private JSONObject createNotebookCodeCellWithString(String cellInformation) {
    return new JSONObject()
        .put("cell_type", "code")
        .put("metadata", new JSONObject())
        .put("execution_count", JSONObject.NULL)
        .put("outputs", new JSONArray())
        .put("source", new JSONArray()
            .put(cellInformation));
  }

  private String convertQueryToString(DataSetQuery query, String prefix) {
    String namespace = prefix
        .toLowerCase()
        .replaceAll(" ", "_") +
        "_" +
        query.getDomain()
            .toString().toLowerCase() + "_";
    String sqlSection = namespace + "sql = \"\"\"" + query.getQuery() + "\"\"\"";

    String namedParamsSection = namespace +
        "query_config = {\n" +
        "  \'query\': {\n" +
        "  \'parameterMode\': \'NAMED\',\n" +
        "  \'queryParameters\': [\n" +
        query.getNamedParameters().stream().map(namedParameterEntry -> convertNamedParameterToString(namedParameterEntry) + "\n").collect(Collectors.joining("")) +
        "    ]\n" +
        "  }\n" +
        "}\n\n";

    String dataFrameSection = namespace + "df = pandas.read_gbq(" + namespace + "sql, dialect=\"standard\", configuration=" + namespace + "query_config)";

    return sqlSection + "\n\n" + namedParamsSection + "\n\n" + dataFrameSection;
  }

  private String convertNamedParameterToString(NamedParameterEntry namedParameterEntry) {
    if (namedParameterEntry.getValue() == null) {
      return "";
    }
    boolean isArrayParameter = !(namedParameterEntry.getValue().getParameterValue() instanceof String);

    List<NamedParameterValue> arrayValues = new ArrayList<>();
    if (namedParameterEntry.getValue().getParameterValue() instanceof List){
      arrayValues = (List<NamedParameterValue>) namedParameterEntry.getValue().getParameterValue();
    }
    return "      {\n" +
        "        'name': \"" + namedParameterEntry.getValue().getName() + "\",\n" +
        "        'parameterType': {'type': \"" + namedParameterEntry.getValue().getParameterType() + "\"" +
        (isArrayParameter ? ",'arrayType': {'type': \"" + namedParameterEntry.getValue().getArrayType() + "\"}," : "") + "},\n" +
        "        \'parameterValue\': {" + (isArrayParameter ? "\'arrayValues\': [" + (
            arrayValues)
        .stream().map(namedParameterValue -> "{\'value\': " + namedParameterValue.getParameterValue() + "}") + "]" :
        "'value': \"" + namedParameterEntry.getValue().getParameterValue() + "\"") + "}\n" +
        "      },";
  }

  private NamedParameterEntry generateResponseFromQueryParameter(String key, QueryParameterValue value) {
    if (value.getValue() != null) {
      return new NamedParameterEntry().key(key)
          .value(new NamedParameterValue()
              .name(key)
              .parameterType(value.getType().toString())
              .parameterValue(value.getValue()));
    } else if (value.getArrayValues() != null) {
      List<NamedParameterValue> values = value.getArrayValues().stream()
          .map(arrayValue -> generateResponseFromQueryParameter(key, arrayValue).getValue())
          .collect(Collectors.toList());
      return new NamedParameterEntry()
          .key(key)
          .value(new NamedParameterValue()
              .name(key)
              .parameterType(value.getType().toString())
              .arrayType(value.getArrayType() == null ? null : value.getArrayType().toString())
              .parameterValue(values));
    } else {
      throw new ServerErrorException("Unsupported query parameter type in query generation: " + value.getType().toString());
    }
  }
}
