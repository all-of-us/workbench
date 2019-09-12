package org.pmiops.workbench.db.dao;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.TableResult;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.cdr.ConceptBigQueryService;
import org.pmiops.workbench.cohortbuilder.CohortQueryBuilder;
import org.pmiops.workbench.cohortbuilder.ParticipantCriteria;
import org.pmiops.workbench.config.CdrBigQuerySchemaConfig;
import org.pmiops.workbench.config.CdrBigQuerySchemaConfigService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.model.Cohort;
import org.pmiops.workbench.db.model.CommonStorageEnums;
import org.pmiops.workbench.db.model.ConceptSet;
import org.pmiops.workbench.db.model.DataSet;
import org.pmiops.workbench.db.model.DataSetValues;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.model.DataSetRequest;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.DomainValuePair;
import org.pmiops.workbench.model.KernelTypeEnum;
import org.pmiops.workbench.model.PrePackagedConceptSetEnum;
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

  private static final String SELCECT_ALL_FROM_DS_LINKING_WHERE_DOMAIN_MATCHES_LIST =
      "SELECT * FROM `${projectId}.${dataSetId}.ds_linking` "
      + "WHERE DOMAIN = @pDomain AND DENORMALIZED_NAME in unnest(@pValuesList)";

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
  private ConceptBigQueryService conceptBigQueryService;
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
      ConceptBigQueryService conceptBigQueryService,
      ConceptSetDao conceptSetDao,
      CohortQueryBuilder cohortQueryBuilder) {
    this.bigQueryService = bigQueryService;
    this.cdrBigQuerySchemaConfigService = cdrBigQuerySchemaConfigService;
    this.cohortDao = cohortDao;
    this.conceptBigQueryService = conceptBigQueryService;
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
      PrePackagedConceptSetEnum prePackagedConceptSetEnum,
      long creatorId,
      Timestamp creationTime) {
    final DataSet dataSetToSave = new DataSet();
    dataSetToSave.setName(name);
    dataSetToSave.setVersion(1);
    dataSetToSave.setIncludesAllParticipants(includesAllParticipants);
    dataSetToSave.setDescription(description);
    dataSetToSave.setWorkspaceId(workspaceId);
    dataSetToSave.setInvalid(false);
    dataSetToSave.setCreatorId(creatorId);
    dataSetToSave.setCreationTime(creationTime);
    dataSetToSave.setCohortSetId(cohortIdList);
    dataSetToSave.setConceptSetId(conceptIdList);
    dataSetToSave.setValues(values);
    dataSetToSave.setPrePackagedConceptSetEnum(prePackagedConceptSetEnum);

    return dataSetDao.save(dataSetToSave);
  }

  @Override
  public Map<String, QueryJobConfiguration> generateQuery(DataSetRequest dataSet) {
    final CdrBigQuerySchemaConfig bigQuerySchemaConfig = cdrBigQuerySchemaConfigService.getConfig();
    final boolean includesAllParticipants = dataSet.getIncludesAllParticipants();
    final List<Cohort> cohortsSelected = this.cohortDao.findAllByCohortIdIn(dataSet.getCohortIds());
    final List<org.pmiops.workbench.db.model.ConceptSet> conceptSetsSelected =
        this.conceptSetDao.findAllByConceptSetIdIn(dataSet.getConceptSetIds());

    if (((cohortsSelected == null || cohortsSelected.size() == 0) && !includesAllParticipants)
        || conceptSetsSelected == null
        || ((conceptSetsSelected.size() == 0
                && dataSet.getPrePackagedConceptSet().equals(PrePackagedConceptSetEnum.NONE))
            && dataSet.getValues().size() == 0)) {
      throw new BadRequestException("Data Sets must include at least one cohort and concept.");
    }

    Map<String, QueryParameterValue> cohortParameters = new HashMap<>();
    // Below constructs the union of all cohort queries
    String cohortQueries =
        Objects.requireNonNull(cohortsSelected).stream()
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
                          new ParticipantCriteria(searchRequest));
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
                                    .replaceAll("\\b".concat(npKey).concat("\\b"), newKey));
                            cohortParameters.put(newKey, npValue);
                          });
                  return participantQuery.get();
                })
            .collect(Collectors.joining(" UNION DISTINCT "));
    List<Domain> domainList =
        dataSet.getValues().stream().map(value -> value.getDomain()).collect(Collectors.toList());

    // If pre packaged all survey concept set is selected create a temp concept set with concept ids
    // of all survey question
    if (PrePackagedConceptSetEnum.SURVEY.equals(dataSet.getPrePackagedConceptSet())
        || PrePackagedConceptSetEnum.BOTH.equals(dataSet.getPrePackagedConceptSet())) {
      conceptSetsSelected.add(handlePrePackagedSurveyConceptSet());
    }

    // TODO: MAKE CLEARER
    final Map<String, QueryJobConfiguration> result = new HashMap<>();

    for (Domain domain : domainList) {
      // TODO: unused?
      final Map<String, Map<String, QueryParameterValue>> queryParameterMapsbyString = new HashMap<>();
      final StringBuilder queryBuilder = new StringBuilder("SELECT ");

      final List<DomainValuePair> valuePairsForCurrentDomain = dataSet.getValues().stream()
          .filter(valueSet -> valueSet.getDomain() == domain)
          .collect(Collectors.toList());

      final ValuesLinkingPair valuesLinkingPair = getValueSelectsAndJoins(valuePairsForCurrentDomain);

      queryBuilder.append(String.join(", ", valuesLinkingPair.getSelects()));
      queryBuilder.append(" ");
      queryBuilder.append(formatValuesLinkingPair(valuesLinkingPair));

      validateSelectedConceptSetsForDomain(conceptSetsSelected, domain);

      String conceptSetQueries =
          conceptSetsSelected.stream()
              .filter(cs -> domain == cs.getDomainEnum())
              .filter(cs -> domain != Domain.PERSON)
              .flatMap(cs -> cs.getConceptIds().stream().map(cid -> Long.toString(cid)))
              .collect(Collectors.joining(", "));
      String conceptSetListQuery = " IN (" + conceptSetQueries + ")";

      if (domain != Domain.PERSON) {
        Optional<DomainConceptIds> domainConceptIdsMaybe =
            bigQuerySchemaConfig.cohortTables.values().stream()
                .filter(config -> domain.toString().equals(config.domain))
                .map(
                    tableConfig ->
                        new DomainConceptIds(
                            getColumnName(tableConfig, "source"),
                            getColumnName(tableConfig, "standard")))
                .findFirst();

        final DomainConceptIds domainConceptIds = domainConceptIdsMaybe.orElseThrow(
            () -> new ServerErrorException(String.format(
                    "Couldn't find source and standard columns for domain: %s",
                    domain.toString())));

        // This adds the where clauses for cohorts and concept sets.
        queryBuilder.append(
            formatWhereClause(conceptSetListQuery, " WHERE \n(",
                domainConceptIds.getStandardConceptIdColumn(), " OR \n",
                domainConceptIds.getSourceConceptIdColumn(),
                conceptSetListQuery, ")"));
        if (!includesAllParticipants) {
          queryBuilder
              .append(" \nAND (PERSON_ID IN (")
              .append(cohortQueries)
              .append("))");
        }
      } else if (!includesAllParticipants) {
        queryBuilder
            .append(" \nWHERE PERSON_ID IN (")
            .append(cohortQueries)
            .append(")");
      }

      final String completeQuery = queryBuilder.toString();
      queryParameterMapsbyString.put(completeQuery, cohortParameters);

      final QueryJobConfiguration queryJobConfiguration =
          bigQueryService.filterBigQueryConfig(
              QueryJobConfiguration.newBuilder(completeQuery)
                  .setNamedParameters(cohortParameters)
                  .setUseLegacySql(false)
                  .build());
      result.put(domain.toString(), queryJobConfiguration);
    }
    return result;
  }

  private static String formatWhereClause(String conceptSetListQuery, String s,
      String standardConceptIdColumn, String s2, String sourceConceptIdColumn,
      String conceptSetListQuery2, String s3) {
    return s
        + standardConceptIdColumn
        + conceptSetListQuery
        + s2
        + sourceConceptIdColumn
        + conceptSetListQuery2
        + s3;
  }

  private void validateSelectedConceptSetsForDomain(List<ConceptSet> conceptSetsSelected,
      Domain domain) {
    conceptSetsSelected.stream()
        .map(ConceptSet::getDomain)
        .distinct()
        .filter(cs -> domain != Domain.PERSON)
        .forEach(
            currentDomain -> {
              if (conceptSetsSelected.stream()
                      .filter(conceptSet -> conceptSet.getDomain() == currentDomain)
                      .mapToLong(conceptSet -> conceptSet.getConceptIds().size())
                      .sum()
                  == 0) {
                throw new BadRequestException("Concept Sets must contain at least one concept");
              }
            });
  }

  private String formatValuesLinkingPair(ValuesLinkingPair valuesLinkingPair) {
    return valuesLinkingPair.getJoins().stream()
        .distinct()
        .collect(Collectors.joining(" "));
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
  public ValuesLinkingPair getValueSelectsAndJoins(List<DomainValuePair> valueSetList) {
    final Optional<Domain> domainMaybe = valueSetList.stream()
        .map(DomainValuePair::getDomain)
        .findFirst();
    if (!domainMaybe.isPresent()) {
      return new ValuesLinkingPair(Collections.emptyList(), Collections.emptyList());
    }

    final List<String> valuesUppercase =
        valueSetList.stream()
            .map(valueSet -> valueSet.getValue().toUpperCase())
            .collect(Collectors.toList());
    valuesUppercase.add(0, "CORE_TABLE_FOR_DOMAIN");

    final String domainName = domainMaybe.toString();
    final String domainTitleCase = toTitleCase(domainName);

    final ImmutableMap<String, QueryParameterValue> queryParameterValuesByDomain = ImmutableMap.of(
        "pDomain", QueryParameterValue.string(domainTitleCase),
        "pValuesList", QueryParameterValue.array(valuesUppercase.toArray(new String[0]), String.class));

    TableResult valuesLinking =
        bigQueryService.executeQuery(
            bigQueryService.filterBigQueryConfig(
                QueryJobConfiguration.newBuilder(
                    SELCECT_ALL_FROM_DS_LINKING_WHERE_DOMAIN_MATCHES_LIST)
                    .setNamedParameters(queryParameterValuesByDomain)
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

  // TODO(jaycarlton): replace with library function
  private String toTitleCase(String name) {
    if (name.isEmpty()) {
      return name;
    } else if (name.length() == 1) {
      return name;
    } else {
      return String.format("%s%s",
          name.charAt(0),
          name.substring(1).toLowerCase());
    }
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

    return formatWhereClause(namedParamsSection, sqlSection, "\n\n", "\n\n", dataFrameSection,
        "\n\n", displayHeadSection);
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
                ? ", arrayType = list(type = \"" + namedParameterValue.getArrayType() + "\")"
                : "")
            + "),\n"
            + "        parameterValue = list("
            + (isArrayParameter
                ? "arrayValues = list("
                    + arrayValues.stream()
                        .map(arrayValue -> "list(value = " + arrayValue.getValue() + ")")
                        .collect(Collectors.joining(","))
                    + ")"
                : "value = \"" + namedParameterValue.getValue() + "\"")
            + ")\n"
            + "      )";
      default:
        throw new BadRequestException("Language not supported");
    }
  }

  private ConceptSet handlePrePackagedSurveyConceptSet() {
    List<Long> conceptIds = conceptBigQueryService.getSurveyQuestionConceptIds();
    ConceptSet surveyConceptSets = new ConceptSet();
    surveyConceptSets.setName("All Surveys");
    surveyConceptSets.setDomain(CommonStorageEnums.domainToStorage(Domain.SURVEY));
    Set<Long> conceptIdsSet = new HashSet<Long>();
    conceptIdsSet.addAll(conceptIds);
    surveyConceptSets.setConceptIds(conceptIdsSet);
    return surveyConceptSets;
  }
}
