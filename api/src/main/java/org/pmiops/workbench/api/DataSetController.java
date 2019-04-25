package org.pmiops.workbench.api;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.TableResult;

import com.google.common.base.Strings;
import com.google.common.collect.Streams;

import java.sql.Timestamp;
import java.time.Clock;

import java.util.ArrayList;
import java.util.Map;
import java.util.List;

import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Provider;

import org.pmiops.workbench.cdr.dao.ConceptDao;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.ConceptSetDao;
import org.pmiops.workbench.db.dao.DataSetService;
import org.pmiops.workbench.db.dao.WorkspaceService;
import org.pmiops.workbench.db.model.Cohort;
import org.pmiops.workbench.db.model.DataSetValues;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.model.ConceptSet;
import org.pmiops.workbench.model.DataSet;
import org.pmiops.workbench.model.DataSetPreviewList;
import org.pmiops.workbench.model.DataSetPreviewResponse;
import org.pmiops.workbench.model.DataSetPreviewValueList;
import org.pmiops.workbench.model.DataSetRequest;
import org.pmiops.workbench.model.DataSetQuery;
import org.pmiops.workbench.model.DataSetQueryList;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.DomainValuePair;
import org.pmiops.workbench.model.NamedParameterEntry;
import org.pmiops.workbench.model.NamedParameterValue;
import org.pmiops.workbench.model.WorkspaceAccessLevel;

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

  private static int NO_OF_PREIVEW_ROWS = 20;


  @Autowired
  private final CohortDao cohortDao;

  @Autowired
  private ConceptDao conceptDao;

  @Autowired
  private ConceptSetDao conceptSetDao;

  @Autowired
  DataSetController(
      BigQueryService bigQueryService,
      Clock clock,
      CohortDao cohortDao,
      ConceptDao conceptDao,
      ConceptSetDao conceptSetDao,
      DataSetService dataSetService,
      Provider<User> userProvider,
      WorkspaceService workspaceService) {
    this.bigQueryService = bigQueryService;
    this.clock = clock;
    this.cohortDao = cohortDao;
    this.conceptDao = conceptDao;
    this.conceptSetDao = conceptSetDao;
    this.dataSetService = dataSetService;
    this.userProvider = userProvider;
    this.workspaceService = workspaceService;
  }

  @Override
  public ResponseEntity<DataSet> createDataSet(String workspaceNamespace, String workspaceId,
      DataSetRequest dataSetRequest) {
    if (Strings.isNullOrEmpty(dataSetRequest.getName())) {
      throw new BadRequestException("Missing name");
    } else if (dataSetRequest.getConceptSetIds() == null || dataSetRequest.getConceptSetIds().size() == 0) {
      throw new BadRequestException("Missing concept set ids");
    } else if (dataSetRequest.getCohortIds() == null || dataSetRequest.getCohortIds().size() == 0) {
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
          dataSetRequest.getName(), dataSetRequest.getDescription(), wId, dataSetRequest.getCohortIds(),
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
          Iterable<org.pmiops.workbench.db.model.ConceptSet> conceptSets =
              conceptSetDao.findAll(dataSet.getConceptSetId());
          result.setConceptSets(Streams.stream(conceptSets)
              .map(conceptSet -> toClientConceptSet(conceptSet)).collect(Collectors.toList()));

          Iterable<Cohort> cohorts = cohortDao.findAll(dataSet.getCohortSetId());
          result.setCohorts(Streams.stream(cohorts)
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
      result.setConcepts(Streams.stream(concepts)
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
    List<DataSetQuery> respQueryList = new ArrayList<DataSetQuery>();
    List<NamedParameterEntry> parameters = new ArrayList<NamedParameterEntry>();

    Map<String, QueryJobConfiguration> bigQueryJobConfig = dataSetService.generateQuery(dataSet);

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
    int noOfValues = dataSet.getValues().size();

    bigQueryJobConfig.forEach((domain, queryJobConfiguration) -> {
      String query = queryJobConfiguration.getQuery().concat(" LIMIT "+ NO_OF_PREIVEW_ROWS);
      queryJobConfiguration = queryJobConfiguration.toBuilder().setQuery(query).build();

      TableResult valuesLinking = bigQueryService.executeQuery(bigQueryService
          .filterBigQueryConfig(queryJobConfiguration));

      List<DataSetPreviewValueList> valuePreviewList = new ArrayList<>();

      valuesLinking.getValues().forEach(valueLink -> {
        int index = 0;
        while (index < noOfValues) {
          if (valuePreviewList.size() <= index) {
            valuePreviewList.add(index, new DataSetPreviewValueList()
                .value(dataSet.getValues().get(index).getValue())
                .queryValue(new ArrayList<String>()));
          }
          valuePreviewList.get(index).getQueryValue().add(valueLink.get(index++).getValue().toString());
        }
      });
      previewQueryResponse.addDomainValueItem(new DataSetPreviewList().domain(domain).values(valuePreviewList));
    });
    return ResponseEntity.ok(previewQueryResponse);
  }

  private NamedParameterEntry generateResponseFromQueryParameter(String key, QueryParameterValue value) {
    if (value.getValue() != null) {
      return new NamedParameterEntry().key(key).value(new NamedParameterValue().name(key).parameterType(value.getType().toString()).parameterValue(value.getValue()));
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
