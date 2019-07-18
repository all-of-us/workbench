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
import javax.inject.Provider;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.cohortbuilder.CohortQueryBuilder;
import org.pmiops.workbench.cohortbuilder.ParticipantCriteria;
import org.pmiops.workbench.config.CdrBigQuerySchemaConfig;
import org.pmiops.workbench.config.CdrBigQuerySchemaConfigService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.model.Cohort;
import org.pmiops.workbench.db.model.DataSet;
import org.pmiops.workbench.db.model.DataSetValues;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.model.DataSetRequest;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.DomainValuePair;
import org.pmiops.workbench.model.KernelTypeEnum;
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
  private static class ValuesLinkingPair {
    private List<String> selects;
    private List<String> joins;

    private ValuesLinkingPair(List<String> selects, List<String> joins) {
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
  private Provider<WorkbenchConfig> configProvider;

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
      CohortQueryBuilder cohortQueryBuilder,
      Provider<WorkbenchConfig> configProvider) {
    this.bigQueryService = bigQueryService;
    this.cdrBigQuerySchemaConfigService = cdrBigQuerySchemaConfigService;
    this.cohortDao = cohortDao;
    this.conceptSetDao = conceptSetDao;
    this.cohortQueryBuilder = cohortQueryBuilder;
    this.configProvider = configProvider;
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
    List<org.pmiops.workbench.db.model.ConceptSet> conceptSetsSelected =
        this.conceptSetDao.findAllByConceptSetIdIn(dataSet.getConceptSetIds());

    if (((cohortsSelected == null || cohortsSelected.size() == 0) && !includesAllParticipants)
        || conceptSetsSelected == null
        || (conceptSetsSelected.size() == 0 &&
        (dataSet.getValues().size() == 0 || dataSet.getValues().get(0).getDomain() != Domain.PERSON))) {
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
                          new ParticipantCriteria(
                              searchRequest, configProvider.get().cohortbuilder.enableListSearch));
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
            .collect(Collectors.joining(" UNION DISTINCT "));
    List<Domain> domainList =
        dataSet.getValues().stream().map(value -> value.getDomain()).collect(Collectors.toList());

    for (Domain d : domainList) {
      Map<String, Map<String, QueryParameterValue>> queryMap = new HashMap<>();
      String query = "SELECT ";
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

      query =
          query
              .concat(valuesLinkingPair.getSelects().stream().collect(Collectors.joining(", ")))
              .concat(" ")
              .concat(
                  valuesLinkingPair.getJoins().stream()
                      .distinct()
                      .collect(Collectors.joining(" ")));

      // CONCEPT SETS HERE:
      conceptSetsSelected.stream()
          .map(conceptSet -> conceptSet.getDomain())
          .distinct()
          .filter(cs -> d != Domain.PERSON)
          .forEach(
              domain -> {
                if (conceptSetsSelected.stream()
                        .filter(conceptSet -> conceptSet.getDomain() == domain)
                        .mapToLong(conceptSet -> conceptSet.getConceptIds().size())
                        .sum()
                    == 0) {
                  throw new BadRequestException("Concept Sets must contain at least one concept");
                }
              });

      String conceptSetQueries =
          conceptSetsSelected.stream()
              .filter(cs -> d == cs.getDomainEnum())
              .filter(cs -> d != Domain.PERSON)
              .flatMap(cs -> cs.getConceptIds().stream().map(cid -> Long.toString(cid)))
              .collect(Collectors.joining(", "));
      String conceptSetListQuery = " IN (" + conceptSetQueries + ")";

      if (d != Domain.PERSON) {
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
        query =
            query.concat(
                " WHERE \n("
                    + columnNames.getStandardConceptIdColumn()
                    + conceptSetListQuery
                    + " OR \n"
                    + columnNames.getSourceConceptIdColumn()
                    + conceptSetListQuery
                    + ")");
        if (!includesAllParticipants) {
          query = query.concat(" \nAND (PERSON_ID IN (" + cohortQueries + "))");
        }
      } else if (!includesAllParticipants) {
        query = query.concat(" \nWHERE PERSON_ID IN (" + cohortQueries + ")");
      }

      queryMap.put(query, cohortParameters);
      QueryJobConfiguration queryJobConfiguration =
          QueryJobConfiguration.newBuilder(query)
              .setNamedParameters(cohortParameters)
              .setUseLegacySql(false)
              .build();
      dataSetUtil.put(d.toString(), queryJobConfiguration);
    }
    return dataSetUtil;
  }

  @Override
  public List<String> generateCodeCellPerDomainFromQueryAndKernelType(
      KernelTypeEnum kernelTypeEnum,
      String dataSetName,
      Map<String, QueryJobConfiguration> queryJobConfigurationMap) {
    String prerequisites;
    switch (kernelTypeEnum) {
      case R:
        prerequisites =
            "install.packages(\"reticulate\")\n"
                + "library(reticulate)\n"
                + "pd <- reticulate::import(\"pandas\")";
        break;
      case PYTHON:
        prerequisites = "import pandas";
        break;
      default:
        throw new BadRequestException(
            "Kernel Type " + kernelTypeEnum.toString() + " not supported");
    }
    return queryJobConfigurationMap.entrySet().stream()
        .map(
            entry ->
                prerequisites
                    + "\n\n"
                    + convertQueryToString(
                        entry.getValue(),
                        Domain.fromValue(entry.getKey()),
                        dataSetName,
                        kernelTypeEnum))
        .collect(Collectors.toList());
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

  private static String convertQueryToString(
      QueryJobConfiguration queryJobConfiguration,
      Domain domain,
      String prefix,
      KernelTypeEnum kernelTypeEnum) {

    // Define [namespace]_sql, [namespace]_query_config, and [namespace]_df variables
    String namespace =
        prefix.toLowerCase().replaceAll(" ", "_") + "_" + domain.toString().toLowerCase() + "_";
    String sqlSection;
    String namedParamsSection;
    String dataFrameSection;
    String displayHeadSection;

    switch (kernelTypeEnum) {
      case PYTHON:
        sqlSection = namespace + "sql = \"\"\"" + queryJobConfiguration.getQuery() + "\"\"\"";
        namedParamsSection =
            namespace
                + "query_config = {\n"
                + "  \'query\': {\n"
                + "  \'parameterMode\': \'NAMED\',\n"
                + "  \'queryParameters\': [\n"
                + queryJobConfiguration.getNamedParameters().entrySet().stream()
                    .map(
                        entry ->
                            convertNamedParameterToString(
                                entry.getKey(), entry.getValue(), KernelTypeEnum.PYTHON))
                    .collect(Collectors.joining(",\n"))
                + "\n"
                + "    ]\n"
                + "  }\n"
                + "}\n\n";
        dataFrameSection =
            namespace
                + "df = pandas.read_gbq("
                + namespace
                + "sql, dialect=\"standard\", configuration="
                + namespace
                + "query_config)";
        displayHeadSection = namespace + "df.head(5)";
        break;
      case R:
        sqlSection = namespace + "sql <- \"" + queryJobConfiguration.getQuery() + "\"";
        namedParamsSection =
            namespace
                + "query_config <- list(\n"
                + "  query = list(\n"
                + "    parameterMode = 'NAMED',\n"
                + "    queryParameters = list(\n"
                + queryJobConfiguration.getNamedParameters().entrySet().stream()
                    .map(
                        entry ->
                            convertNamedParameterToString(
                                entry.getKey(), entry.getValue(), KernelTypeEnum.R))
                    .collect(Collectors.joining(",\n"))
                + "\n"
                + "    )\n"
                + "  )\n"
                + ")";
        dataFrameSection =
            namespace
                + "df <- pd$read_gbq("
                + namespace
                + "sql, dialect=\"standard\", configuration="
                + namespace
                + "query_config)";
        displayHeadSection = "head(" + namespace + "df, 5)";
        break;
      default:
        throw new BadRequestException("Language " + kernelTypeEnum.toString() + " not supported.");
    }

    return sqlSection
        + "\n\n"
        + namedParamsSection
        + "\n\n"
        + dataFrameSection
        + "\n\n"
        + displayHeadSection;
  }

  // BigQuery api returns parameter values either as a list or an object.
  // To avoid warnings on our cast to list, we suppress those warnings here,
  // as they are expected.
  @SuppressWarnings("unchecked")
  private static String convertNamedParameterToString(
      String key, QueryParameterValue namedParameterValue, KernelTypeEnum kernelTypeEnum) {
    if (namedParameterValue == null) {
      return "";
    }
    boolean isArrayParameter = namedParameterValue.getArrayType() != null;

    List<QueryParameterValue> arrayValues =
        Optional.ofNullable(namedParameterValue.getArrayValues()).orElse(new ArrayList<>());

    switch (kernelTypeEnum) {
      case PYTHON:
        return "      {\n"
            + "        'name': \""
            + key
            + "\",\n"
            + "        'parameterType': {'type': \""
            + namedParameterValue.getType().toString()
            + "\""
            + (isArrayParameter
                ? ",'arrayType': {'type': \"" + namedParameterValue.getArrayType() + "\"},"
                : "")
            + "},\n"
            + "        \'parameterValue\': {"
            + (isArrayParameter
                ? "\'arrayValues\': ["
                    + arrayValues.stream()
                        .map(arrayValue -> "{\'value\': " + arrayValue.getValue() + "}")
                        .collect(Collectors.joining(","))
                    + "]"
                : "'value': \"" + namedParameterValue.getValue() + "\"")
            + "}\n"
            + "      }";
      case R:
        return "      list(\n"
            + "        name = \""
            + key
            + "\",\n"
            + "        parameterType = list(type = \""
            + namedParameterValue.getType().toString()
            + "\""
            + (isArrayParameter
                ? ", arrayType = list(type = " + namedParameterValue.getArrayType() + "),"
                : "")
            + "),\n"
            + "        parameterValue = list("
            + (isArrayParameter
                ? "arrayValues = list("
                    + arrayValues.stream()
                        .map(arrayValue -> "list(value = " + arrayValue.getValue() + ")")
                        .collect(Collectors.joining(","))
                    + ")"
                : "value = \"" + namedParameterValue.getValue())
            + "\""
            + ")\n"
            + "      )";
      default:
        throw new BadRequestException("Language not supported");
    }
  }
}
