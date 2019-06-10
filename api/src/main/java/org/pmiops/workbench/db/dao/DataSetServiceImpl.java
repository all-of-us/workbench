package org.pmiops.workbench.db.dao;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.TableResult;
import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.cohortbuilder.CohortQueryBuilder;
import org.pmiops.workbench.cohortbuilder.ParticipantCriteria;
import org.pmiops.workbench.config.CdrBigQuerySchemaConfig;
import org.pmiops.workbench.config.CdrBigQuerySchemaConfigService;
import org.pmiops.workbench.db.model.Cohort;
import org.pmiops.workbench.db.model.DataSet;
import org.pmiops.workbench.db.model.DataSetValues;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.model.DataSetRequest;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.DomainValuePair;
import org.pmiops.workbench.model.SearchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/*
 * A subclass to store the associated set of selects and joins for values for the data set builder.
 *
 * This is used to store the data pulled out of the linking table in bigquery.
 */

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

@Service
public class DataSetServiceImpl implements DataSetService {

  @VisibleForTesting
  public static class ValuesLinkingPair {
    private List<String> selects;
    private List<String> joins;

    public ValuesLinkingPair(List<String> selects, List<String> joins) {
      this.selects = selects;
      this.joins = joins;
    }

    private List<String> getSelects() {
      return this.selects;
    }

    private List<String> getJoins() {
      return this.joins;
    }
  }

  private CohortQueryBuilder cohortQueryBuilder;
  private BigQueryService bigQueryService;
  private CdrBigQuerySchemaConfigService cdrBigQuerySchemaConfigService;

  @Autowired DataSetDao dataSetDao;

  @Autowired ConceptSetDao conceptSetDao;

  @Autowired CohortDao cohortDao;

  @Autowired
  @VisibleForTesting
  public DataSetServiceImpl(
      BigQueryService bigQueryService,
      CdrBigQuerySchemaConfigService cdrBigQuerySchemaConfigService,
      CohortDao cohortDao,
      ConceptSetDao conceptSetDao,
      CohortQueryBuilder cohortQueryBuilder) {
    this.bigQueryService = bigQueryService;
    this.cdrBigQuerySchemaConfigService = cdrBigQuerySchemaConfigService;
    this.cohortDao = cohortDao;
    this.conceptSetDao = conceptSetDao;
    this.cohortQueryBuilder = cohortQueryBuilder;
  }

  @Override
  public DataSet saveDataSet(
      String name,
      Boolean includesAllParticipants,
      String description,
      long workspaceId,
      List<Long> cohortIdList,
      List<Long> conceptIdList,
      List<DataSetValues> values,
      long creatorId,
      Timestamp creationTime) {
    DataSet dataSetDb = new DataSet();
    dataSetDb.setName(name);
    dataSetDb.setVersion(1);
    dataSetDb.setIncludesAllParticipants(includesAllParticipants);
    dataSetDb.setDescription(description);
    dataSetDb.setWorkspaceId(workspaceId);
    dataSetDb.setInvalid(false);
    dataSetDb.setCreatorId(creatorId);
    dataSetDb.setCreationTime(creationTime);
    dataSetDb.setCohortSetId(cohortIdList);
    dataSetDb.setConceptSetId(conceptIdList);
    dataSetDb.setValues(values);
    dataSetDb = dataSetDao.save(dataSetDb);
    return dataSetDb;
  }

  @Override
  public Map<String, QueryJobConfiguration> generateQuery(DataSetRequest dataSet) {
    CdrBigQuerySchemaConfig bigQuerySchemaConfig = cdrBigQuerySchemaConfigService.getConfig();

    boolean includesAllParticipants =
        Optional.of(dataSet.getIncludesAllParticipants()).orElse(false);

    Map<String, QueryJobConfiguration> dataSetUtil = new HashMap<>();
    List<Cohort> cohortsSelected = this.cohortDao.findAllByCohortIdIn(dataSet.getCohortIds());
    List<Long> conceptSetList = dataSet.getConceptSetIds();
    List<org.pmiops.workbench.db.model.ConceptSet> conceptSetsSelected =
        this.conceptSetDao.findAllByConceptSetIdIn(dataSet.getConceptSetIds());
    // conceptSetList -1 represents Demographics Concept Set which is a dummy concept Set to include
    // Person Domain values like RACE GENDER ETHNICITY DOB. As of now this is a dummy concept set
    // and does not contain any concept.
    if (((cohortsSelected == null || cohortsSelected.size() == 0) && !includesAllParticipants)
        || (conceptSetsSelected == null
            || conceptSetsSelected.size() == 0
                && !(conceptSetList.size() == 1 && conceptSetList.get(0) == -1l))) {
      throw new BadRequestException("Data Sets must include at least one cohort and concept.");
    }

    Map<String, QueryParameterValue> cohortParameters = new HashMap<>();
    // Below constructs the union of all cohort queries
    String cohortQueries =
        cohortsSelected.stream()
            .map(
                c -> {
                  String cohortDefinition = c.getCriteria();
                  if (cohortDefinition == null) {
                    throw new NotFoundException(
                        String.format(
                            "Not Found: No Cohort definition matching cohortId: %s",
                            c.getCohortId()));
                  }
                  SearchRequest searchRequest =
                      new Gson().fromJson(cohortDefinition, SearchRequest.class);
                  QueryJobConfiguration participantIdQuery =
                      cohortQueryBuilder.buildParticipantIdQuery(
                          new ParticipantCriteria(searchRequest, false));
                  QueryJobConfiguration participantQueryConfig =
                      bigQueryService.filterBigQueryConfig(participantIdQuery);
                  AtomicReference<String> participantQuery =
                      new AtomicReference<>(participantQueryConfig.getQuery());

                  participantQueryConfig
                      .getNamedParameters()
                      .forEach(
                          (npKey, npValue) -> {
                            String newKey = npKey + "_" + c.getCohortId();
                            participantQuery.getAndSet(
                                participantQuery
                                    .get()
                                    .replaceAll("@".concat(npKey), "@".concat(newKey)));
                            cohortParameters.put(newKey, npValue);
                          });
                  return participantQuery.get();
                })
            .collect(Collectors.joining(" OR PERSON_ID IN "));
    List<Domain> domainList =
        dataSet.getValues().stream().map(value -> value.getDomain()).collect(Collectors.toList());

    for (Domain d : domainList) {
      Map<String, Map<String, QueryParameterValue>> queryMap = new HashMap<>();
      StringBuffer query = new StringBuffer("SELECT ");
      // VALUES HERE:
      Optional<List<DomainValuePair>> valueSetOpt =
          Optional.of(
              dataSet.getValues().stream()
                  .filter(valueSet -> valueSet.getDomain() == d)
                  .collect(Collectors.toList()));
      if (!valueSetOpt.isPresent()) {
        continue;
      }

      ValuesLinkingPair valuesLinkingPair = this.getValueSelectsAndJoins(valueSetOpt.get(), d);

      query
          .append(valuesLinkingPair.getSelects().stream().collect(Collectors.joining(", ")))
          .append(" ")
          .append(
              valuesLinkingPair.getJoins().stream().distinct().collect(Collectors.joining(" ")));

      if (!d.equals(Domain.PERSON)) {
        // CONCEPT SETS HERE:
        if (conceptSetsSelected.stream().map(conceptSet -> conceptSet.getConceptIds()).count()
            == 0) {
          throw new BadRequestException("Concept Sets must contain at least one concept");
        }
        String conceptSetQueries =
            conceptSetsSelected.stream()
                .filter(cs -> d == cs.getDomainEnum())
                .flatMap(cs -> cs.getConceptIds().stream().map(cid -> Long.toString(cid)))
                .collect(Collectors.joining(", "));
        String conceptSetListQuery = " IN (" + conceptSetQueries + ")";

        Optional<DomainConceptIds> domainConceptIds =
            bigQuerySchemaConfig.cohortTables.values().stream()
                .filter(config -> d.toString().equals(config.domain))
                .map(
                    tableConfig ->
                        new DomainConceptIds(
                            getColumnName(tableConfig, "source"),
                            getColumnName(tableConfig, "standard")))
                .findFirst();
        if (!domainConceptIds.isPresent()) {
          throw new ServerErrorException(
              "Couldn't find source and standard columns for domain: " + d.toString());
        }
        DomainConceptIds columnNames = domainConceptIds.get();

        // This adds the where clauses for cohorts and concept sets.
        query.append(
            " WHERE \n("
                + columnNames.getStandardConceptIdColumn()
                + conceptSetListQuery
                + " OR \n"
                + columnNames.getSourceConceptIdColumn()
                + conceptSetListQuery
                + ")");
      }
      if (!includesAllParticipants) {
        if (d.equals(Domain.PERSON)) {
          query.append(" \nWHERE (");
        } else {
          query.append(" \nAND (");
        }
        query.append("PERSON_ID IN (" + cohortQueries + "))");
      }
      queryMap.put(query.toString(), cohortParameters);
      QueryJobConfiguration queryJobConfiguration =
          QueryJobConfiguration.newBuilder(query.toString())
              .setNamedParameters(cohortParameters)
              .setUseLegacySql(false)
              .build();
      dataSetUtil.put(d.toString(), queryJobConfiguration);
    }
    return dataSetUtil;
  }

  private String getColumnName(CdrBigQuerySchemaConfig.TableConfig config, String type) {
    Optional<CdrBigQuerySchemaConfig.ColumnConfig> conceptColumn =
        config.columns.stream().filter(column -> type.equals(column.domainConcept)).findFirst();
    if (!conceptColumn.isPresent()) {
      throw new ServerErrorException("Domain not supported");
    }
    return conceptColumn.get().name;
  }

  @VisibleForTesting
  public ValuesLinkingPair getValueSelectsAndJoins(List<DomainValuePair> valueSetList, Domain d) {
    List<String> values =
        valueSetList.stream().map(valueSet -> valueSet.getValue()).collect(Collectors.toList());
    values.add(0, "CORE_TABLE_FOR_DOMAIN");
    String domainAsName = d.toString().charAt(0) + d.toString().substring(1).toLowerCase();

    String valuesQuery =
        "SELECT * FROM `${projectId}.${dataSetId}.ds_linking` WHERE DOMAIN = @pDomain AND DENORMALIZED_NAME in unnest(@pValuesList)";
    Map<String, QueryParameterValue> valuesQueryParams = new HashMap<>();

    valuesQueryParams.put("pDomain", QueryParameterValue.string(domainAsName));
    valuesQueryParams.put(
        "pValuesList", QueryParameterValue.array(values.toArray(new String[0]), String.class));

    TableResult valuesLinking =
        bigQueryService.executeQuery(
            bigQueryService.filterBigQueryConfig(
                QueryJobConfiguration.newBuilder(valuesQuery)
                    .setNamedParameters(valuesQueryParams)
                    .setUseLegacySql(false)
                    .build()));

    List<String> valueJoins = new ArrayList<>();
    List<String> valueSelects = new ArrayList<>();
    valuesLinking
        .getValues()
        .forEach(
            (value) -> {
              valueJoins.add(value.get("JOIN_VALUE").getStringValue());
              if (!value.get("OMOP_SQL").getStringValue().equals("CORE_TABLE_FOR_DOMAIN")) {
                valueSelects.add(value.get("OMOP_SQL").getStringValue());
              }
            });

    return new ValuesLinkingPair(valueSelects, valueJoins);
  }
}
