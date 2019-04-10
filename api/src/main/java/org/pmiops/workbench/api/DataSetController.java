package org.pmiops.workbench.api;

import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.TableResult;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
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
import org.pmiops.workbench.jira.JiraService;
import org.pmiops.workbench.model.DataSet;
import org.pmiops.workbench.model.DataSetQuery;
import org.pmiops.workbench.model.DataSetQueryResponse;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.SearchRequest;
import org.pmiops.workbench.model.ValueSet;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

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
    Map<Domain, List<ConceptSet>> domainConceptSetMap = new HashMap<Domain, List<ConceptSet>>();
    List<Domain> domainList = dataSet.getValues().stream().map(value -> value.getDomain()).collect(Collectors.toList());

    // Below constructs the union of all cohort queries
    String cohortQueries = cohortsSelected.stream().map(c -> {
        SearchRequest searchRequest = new Gson().fromJson(getCohortDefinition(c), SearchRequest.class);
        return participantCounter.buildParticipantIdQuery(new ParticipantCriteria(searchRequest)).getQuery();
      }).collect(Collectors.joining(" UNION "));


    DataSetQueryResponse resp = new DataSetQueryResponse();
    ArrayList<DataSetQuery> respQueryList = new ArrayList<DataSetQuery>();

    for (Domain d: domainList) {
      String query = "SELECT ";
      // VALUES HERE:
      Optional<ValueSet> valueSetOpt = dataSet.getValues().stream()
          .filter(valueSet -> valueSet.getDomain() == d)
          .findFirst();
      if (!valueSetOpt.isPresent()) {
        continue;
      }
      List<String> values = valueSetOpt.get()
          .getValues().getItems().stream()
          .map(domainValue -> "'" + domainValue.getValue() + "'").collect(Collectors.toList());

      String domainAsName = d.toString().charAt(0) + d.toString().substring(1).toLowerCase();

      String valuesQuery = "SELECT * FROM `${projectId}.${dataSetId}.ds_linking` WHERE DOMAIN = @pDomain AND DENORMALIZED_NAME unnest (@pValuesList)";
      Map<String, QueryParameterValue> valuesQueryParams = new HashMap<>();

      valuesQueryParams.put("pDomain", QueryParameterValue.string("'" + domainAsName + "'"));
      valuesQueryParams.put("pValuesList", QueryParameterValue.array(values.stream().toArray(s -> new String[s]), String.class));

      TableResult valuesLinking = bigQueryService.executeQuery(
          bigQueryService
              .filterBigQueryConfig(QueryJobConfiguration
                  .newBuilder(valuesQuery)
                  .setNamedParameters(valuesQueryParams)
                  .setUseLegacySql(false)
                  .build()));

      List<String> valueJoins = new ArrayList<String>();
      List<String> valueSelects = new ArrayList<String>();
      for (Iterator<FieldValueList> i = valuesLinking.getValues().iterator(); i.hasNext(); ) {
        FieldValueList value = i.next();
        valueJoins.add(value.get("JOIN_VALUE").getStringValue());
        valueSelects.add(value.get("OMOP_SQL").getStringValue());
      }
      query = query.concat(valueSelects.stream().collect(Collectors.joining(", ")));
      query = query.concat(" ");
      query = query.concat(valueJoins.stream().distinct().collect(Collectors.joining(" ")));

      // CONCEPT SETS HERE:
      String conceptSetQueries = conceptSetsSelected.stream().filter(cs -> d == cs.getDomainEnum())
          .flatMap(cs -> cs.getConceptIds().stream().map(cid -> Long.toString(cid)))
          .collect(Collectors.joining(", "));
      query = query.concat(" WHERE CONCEPT_ID IN (" + conceptSetQueries + ")");
      query = query.concat(" AND PERSON_ID IN (" + cohortQueries + ")");

      respQueryList.add(new DataSetQuery().domain(d).query(query));
    }
    log.log(Level.WARNING, Long.toString(respQueryList.size()));


    resp.setQueryList(respQueryList);
    return ResponseEntity.ok(new DataSetQueryResponse().queryList(respQueryList));
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
