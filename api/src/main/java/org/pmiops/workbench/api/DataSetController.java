package org.pmiops.workbench.api;

import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.TableResult;
import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.pmiops.workbench.cohortbuilder.ParticipantCounter;
import org.pmiops.workbench.cohortbuilder.ParticipantCriteria;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.ConceptSetDao;
import org.pmiops.workbench.db.dao.WorkspaceService;
import org.pmiops.workbench.db.model.Cohort;
import org.pmiops.workbench.db.model.ConceptSet;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.model.DataSet;
import org.pmiops.workbench.model.DataSetQuery;
import org.pmiops.workbench.model.DataSetQueryResponse;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.NamedParameterEntry;
import org.pmiops.workbench.model.NamedParameterValue;
import org.pmiops.workbench.model.SearchRequest;
import org.pmiops.workbench.model.ValueSet;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

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
  private static final Logger log = Logger.getLogger(DataSetController.class.getName());

  private BigQueryService bigQueryService;

  private Provider<User> userProvider;

  private CohortDao cohortDao;

  private ConceptSetDao conceptSetDao;

  private ParticipantCounter participantCounter;

  private WorkspaceService workspaceService;

  @Autowired
  DataSetController(
      BigQueryService bigQueryService,
      Provider<User> userProvider,
      CohortDao cohortDao,
      ConceptSetDao conceptSetDao,
      ParticipantCounter participantCounter,
      WorkspaceService workspaceService) {
    this.bigQueryService = bigQueryService;
    this.userProvider = userProvider;
    this.cohortDao = cohortDao;
    this.conceptSetDao = conceptSetDao;
    this.participantCounter = participantCounter;
    this.workspaceService = workspaceService;
  }

  @VisibleForTesting
  void setUserProvider(Provider<User> userProvider) {
    this.userProvider = userProvider;
  }

  public ResponseEntity<DataSetQueryResponse> getQueryFromDataSet(String ns, String wsid, DataSet dataSet) {
    workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(ns, wsid, WorkspaceAccessLevel.READER);

    List<Cohort> cohortsSelected = this.cohortDao.findAllByCohortIdIn(dataSet.getCohortIds());
    List<ConceptSet> conceptSetsSelected = this.conceptSetDao.findAllByConceptSetIdIn(dataSet.getConceptSetIds());
    if (cohortsSelected == null || cohortsSelected.size() == 0 || conceptSetsSelected == null || conceptSetsSelected.size() == 0) {
      throw new BadRequestException("Data Sets must include at least one cohort and concept.");
    }
    List<Domain> domainList = dataSet.getValues().stream().map(value -> value.getDomain()).collect(Collectors.toList());
    Map<String, QueryParameterValue> cohortParameters = new HashMap<>();
    // Below constructs the union of all cohort queries
    String cohortQueries = cohortsSelected.stream().map(c -> {
      SearchRequest searchRequest = new Gson().fromJson(getCohortDefinition(c), SearchRequest.class);
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

    DataSetQueryResponse resp = new DataSetQueryResponse();
    ArrayList<DataSetQuery> respQueryList = new ArrayList<>();

    for (Domain d: domainList) {
      String query = "SELECT ";
      // VALUES HERE:
      Optional<ValueSet> valueSetOpt = dataSet.getValues().stream()
          .filter(valueSet -> valueSet.getDomain() == d)
          .findFirst();
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
      query = query.concat(" WHERE " + d.toString() + "_CONCEPT_ID IN (" + conceptSetQueries + ")");
      query = query.concat(" AND (PERSON_ID IN (" + cohortQueries + "))");

      respQueryList.add(new DataSetQuery().domain(d).query(query).namedParameters(parameters));
    }

    resp.setQueryList(respQueryList);
    return ResponseEntity.ok(new DataSetQueryResponse().queryList(respQueryList));
  }

  @VisibleForTesting
  ValuesLinkingPair getValueSelectsAndJoins(ValueSet valueSet, Domain d) {
    List<String> values = valueSet
        .getValues().getItems().stream()
        .map(domainValue -> domainValue.getValue()).collect(Collectors.toList());

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

    boolean doesNotContainFrom = true;

    for (Iterator<FieldValueList> i = valuesLinking.getValues().iterator(); i.hasNext(); ) {
      FieldValueList value = i.next();
      valueJoins.add(value.get("JOIN_VALUE").getStringValue());
      if (value.get("JOIN_VALUE").getStringValue().contains(" from ")) {
        doesNotContainFrom = false;
      }
      valueSelects.add(value.get("OMOP_SQL").getStringValue());
    }

    if (doesNotContainFrom) {
      throw new BadRequestException("You must include at least one value from the parent table");
    }

    return new ValuesLinkingPair(valueSelects, valueJoins);
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


  /**
   * Helper to method that consolidates access to Cohort Definition. Will throw a
   * {@link NotFoundException} if {@link Cohort#getCriteria()} return null.
   *
   * @param cohort
   * @return
   */
  private String getCohortDefinition(Cohort cohort) {
    String definition = cohort.getCriteria();
    if (definition == null) {
      throw new NotFoundException(
          String.format("Not Found: No Cohort definition matching cohortId: %s", cohort.getCohortId()));
    }
    return definition;
  }
}