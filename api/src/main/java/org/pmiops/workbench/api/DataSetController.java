package org.pmiops.workbench.api;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.TableResult;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.Streams;
import com.google.gson.Gson;

import java.sql.Timestamp;
import java.time.Clock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.List;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Provider;

import org.pmiops.workbench.cdr.dao.ConceptDao;
import org.pmiops.workbench.cohortbuilder.ParticipantCounter;
import org.pmiops.workbench.cohortbuilder.ParticipantCriteria;
import org.pmiops.workbench.config.CdrBigQuerySchemaConfig;
import org.pmiops.workbench.config.CdrBigQuerySchemaConfigService;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.ConceptSetDao;
import org.pmiops.workbench.db.dao.DataSetService;
import org.pmiops.workbench.db.model.Cohort;
import org.pmiops.workbench.db.model.DataSetValues;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.model.ConceptSet;
import org.pmiops.workbench.model.DataSet;
import org.pmiops.workbench.model.DataSetRequest;
import org.pmiops.workbench.model.DataSetQuery;
import org.pmiops.workbench.model.DataSetQueryList;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.DomainValuePair;
import org.pmiops.workbench.model.NamedParameterEntry;
import org.pmiops.workbench.model.NamedParameterValue;
import org.pmiops.workbench.model.SearchRequest;
import org.pmiops.workbench.model.WorkspaceAccessLevel;

import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/*
 * A subclass used to store a source and a standard concept ID column name.
 */
class DomainConceptIds {
  private String sourceConceptIdColumn;
  private String standardConceptIdColumn;

  DomainConceptIds(String sourceConceptIdColumn, String standardConceptIdColumn) {
    this.sourceConceptIdColumn = sourceConceptIdColumn;
    this.standardConceptIdColumn = standardConceptIdColumn;
  }

  public String getSourceConceptIdColumn() {
    return this.sourceConceptIdColumn;
  }
  public String getStandardConceptIdColumn() {
    return this.standardConceptIdColumn;
  }
}

/*
 * A subclass to store the associated set of selects and joins for values for the data set builder.
 *
 * This is used to store the data pulled out of the linking table in bigquery.
 */
class ValuesLinkingPair {
  private List<String> selects;
  private List<String> joins;

  ValuesLinkingPair(List<String> selects, List<String> joins) {
    this.selects = selects;
    this.joins = joins;
  }

  public List<String> getSelects() {
    return this.selects;
  }

  public List<String> getJoins() {
    return this.joins;
  }
}


@RestController
public class DataSetController implements DataSetApiDelegate {

  private BigQueryService bigQueryService;
  private CdrBigQuerySchemaConfigService cdrBigQuerySchemaConfigService;
  private final Clock clock;
  private DataSetService dataSetService;
  private ParticipantCounter participantCounter;

  private Provider<User> userProvider;
  private final WorkspaceService workspaceService;


  @Autowired
  private final CohortDao cohortDao;

  @Autowired
  private ConceptDao conceptDao;

  @Autowired
  private ConceptSetDao conceptSetDao;

  @Autowired
  DataSetController(
      BigQueryService bigQueryService,
      CdrBigQuerySchemaConfigService cdrBigQuerySchemaConfigService,
      Clock clock,
      CohortDao cohortDao,
      ConceptDao conceptDao,
      ConceptSetDao conceptSetDao,
      DataSetService dataSetService,
      ParticipantCounter participantCounter,
      Provider<User> userProvider,
      WorkspaceService workspaceService) {
    this.bigQueryService = bigQueryService;
    this.cdrBigQuerySchemaConfigService = cdrBigQuerySchemaConfigService;
    this.clock = clock;
    this.cohortDao = cohortDao;
    this.conceptDao = conceptDao;
    this.conceptSetDao = conceptSetDao;
    this.dataSetService = dataSetService;
    this.participantCounter = participantCounter;
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

    CdrBigQuerySchemaConfig bigQuerySchemaConfig = cdrBigQuerySchemaConfigService.getConfig();


    workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);

    List<Cohort> cohortsSelected = this.cohortDao.findAllByCohortIdIn(dataSet.getCohortIds());
    List<org.pmiops.workbench.db.model.ConceptSet> conceptSetsSelected =
        this.conceptSetDao.findAllByConceptSetIdIn(dataSet.getConceptSetIds());
    if (cohortsSelected == null || cohortsSelected.size() == 0 || conceptSetsSelected == null || conceptSetsSelected.size() == 0) {
      throw new BadRequestException("Data Sets must include at least one cohort and concept.");
    }
    List<Domain> domainList = dataSet.getValues().stream().map(value -> value.getDomain()).collect(Collectors.toList());
    Map<String, QueryParameterValue> cohortParameters = new HashMap<>();
    // Below constructs the union of all cohort queries
    String cohortQueries = cohortsSelected.stream().map(c -> {
      String cohortDefinition = c.getCriteria();
      if (cohortDefinition == null) {
        throw new NotFoundException(
            String.format("Not Found: No Cohort definition matching cohortId: %s", c.getCohortId()));
      }
      SearchRequest searchRequest = new Gson().fromJson(cohortDefinition, SearchRequest.class);
      QueryJobConfiguration participantIdQuery = participantCounter.buildParticipantIdQuery(new ParticipantCriteria(searchRequest));
      QueryJobConfiguration participantQueryConfig = bigQueryService.filterBigQueryConfig(participantIdQuery);
      AtomicReference<String> participantQuery = new AtomicReference<>(participantQueryConfig.getQuery());

      participantQueryConfig.getNamedParameters().forEach((npKey, npValue) -> {
        String newKey = npKey + "_" + c.getCohortId();
        participantQuery.getAndSet(participantQuery.get().replaceAll("@".concat(npKey), "@".concat(newKey)));
        cohortParameters.put(newKey, npValue);
      });
      return participantQuery.get();
    }).collect(Collectors.joining(" OR PERSON_ID IN "));

    ArrayList<DataSetQuery> respQueryList = new ArrayList<>();

    for (Domain d: domainList) {
      String query = "SELECT ";
      // VALUES HERE:
      Optional<List<DomainValuePair>> valueSetOpt =  Optional.of(dataSet.getValues().stream()
          .filter(valueSet -> valueSet.getDomain() == d).collect(Collectors.toList()));
      if (!valueSetOpt.isPresent()) {
        continue;
      }
      List<NamedParameterEntry> parameters = new ArrayList<>();
      cohortParameters.forEach((key, value) -> parameters.add(generateResponseFromQueryParameter(key, value)));

      ValuesLinkingPair valuesLinkingPair = this.getValueSelectsAndJoins(valueSetOpt.get(), d);

      query = query.concat(valuesLinkingPair.getSelects().stream().collect(Collectors.joining(", ")))
          .concat(" ")
          .concat(valuesLinkingPair.getJoins().stream().distinct().collect(Collectors.joining(" ")));

      // CONCEPT SETS HERE:
      String conceptSetQueries = conceptSetsSelected.stream().filter(cs -> d == cs.getDomainEnum())
          .flatMap(cs -> cs.getConceptIds().stream().map(cid -> Long.toString(cid)))
          .collect(Collectors.joining(", "));
      String conceptSetListQuery = " IN (" + conceptSetQueries + ")";

      Optional<DomainConceptIds> domainConceptIds = bigQuerySchemaConfig.cohortTables.values().stream().filter(config -> d.toString().equals(config.domain))
          .map(tableConfig -> new DomainConceptIds(getColumnName(tableConfig, "source"), getColumnName(tableConfig, "standard")))
          .findFirst();
      if (!domainConceptIds.isPresent()) {
        throw new ServerErrorException("Couldn't find source and standard columns for domain: " + d.toString());
      }
      DomainConceptIds columnNames = domainConceptIds.get();

      // This adds the where clauses for cohorts and concept sets.
      query = query.concat(" WHERE (" + columnNames.getStandardConceptIdColumn() + conceptSetListQuery
          + " OR " + columnNames.getSourceConceptIdColumn() + conceptSetListQuery + ") AND (PERSON_ID IN ("
          + cohortQueries + "))");

      respQueryList.add(new DataSetQuery().domain(d).query(query).namedParameters(parameters));
    }

    return ResponseEntity.ok(new DataSetQueryList().queryList(respQueryList));
  }

  @VisibleForTesting
  ValuesLinkingPair getValueSelectsAndJoins(List<DomainValuePair> valueSetList, Domain d) {
    List<String> values = valueSetList.stream().map(valueSet -> valueSet.getValue())
        .collect(Collectors.toList());
    values.add(0, "SENTINEL_PLACEHOLDER_VALUE");
    String domainAsName = d.toString().charAt(0) + d.toString().substring(1).toLowerCase();

    String valuesQuery = "SELECT * FROM `${projectId}.${dataSetId}.ds_linking` WHERE DOMAIN = @pDomain AND DENORMALIZED_NAME in unnest(@pValuesList)";
    Map<String, QueryParameterValue> valuesQueryParams = new HashMap<>();

    valuesQueryParams.put("pDomain", QueryParameterValue.string(domainAsName));
    valuesQueryParams.put("pValuesList", QueryParameterValue.array(values.toArray(new String[0]), String.class));

    TableResult valuesLinking = bigQueryService.executeQuery(
        bigQueryService
            .filterBigQueryConfig(QueryJobConfiguration
                .newBuilder(valuesQuery)
                .setNamedParameters(valuesQueryParams)
                .setUseLegacySql(false)
                .build()));

    List<String> valueJoins = new ArrayList<>();
    List<String> valueSelects = new ArrayList<>();
    valuesLinking.getValues().forEach((value) -> {
      valueJoins.add(value.get("JOIN_VALUE").getStringValue());
      if (!value.get("OMOP_SQL").getStringValue().equals("CORE_TABLE_FOR_DOMAIN")) {
        valueSelects.add(value.get("OMOP_SQL").getStringValue());
      }
    });

    return new ValuesLinkingPair(valueSelects, valueJoins);
  }

  private String getColumnName(CdrBigQuerySchemaConfig.TableConfig config, String type) {
    Optional<CdrBigQuerySchemaConfig.ColumnConfig> conceptColumn = config.columns
        .stream().filter(column -> type.equals(column.domainConcept))
        .findFirst();
    if (!conceptColumn.isPresent()) {
      throw new ServerErrorException("Domain not supported");
    }
    return conceptColumn.get().name;
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
