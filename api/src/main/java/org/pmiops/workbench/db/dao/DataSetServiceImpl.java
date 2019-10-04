package org.pmiops.workbench.db.dao;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.TableResult;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.cdr.ConceptBigQueryService;
import org.pmiops.workbench.cohortbuilder.CohortQueryBuilder;
import org.pmiops.workbench.cohortbuilder.ParticipantCriteria;
import org.pmiops.workbench.config.CdrBigQuerySchemaConfig;
import org.pmiops.workbench.config.CdrBigQuerySchemaConfigService;
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

@Service
public class DataSetServiceImpl implements DataSetService {

  private static final String SELCECT_ALL_FROM_DS_LINKING_WHERE_DOMAIN_MATCHES_LIST =
      "SELECT * FROM `${projectId}.${dataSetId}.ds_linking` "
          + "WHERE DOMAIN = @pDomain AND DENORMALIZED_NAME in unnest(@pValuesList)";
  private static final ImmutableSet<PrePackagedConceptSetEnum>
      CONCEPT_SETS_NEEDING_PREPACKAGED_SURVEY =
          ImmutableSet.of(PrePackagedConceptSetEnum.SURVEY, PrePackagedConceptSetEnum.BOTH);

  private static final String PERSON_ID_COLUMN_NAME = "PERSON_ID";
  private static final int DATA_SET_VERSION = 1;

  /*
   * Stores the associated set of selects and joins for values for the data set builder,
   * pulled out of the linking table in Big Query.
   */
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

    static ValuesLinkingPair emptyPair() {
      return new ValuesLinkingPair(Collections.emptyList(), Collections.emptyList());
    }

    static final String JOIN_VALUE_KEY = "JOIN_VALUE";

    public String formatJoins() {
      return getJoins().stream().distinct().collect(Collectors.joining(" "));
    }
  }

  /*
   * A subclass used to store a source and a standard concept ID column name.
   */
  private static class DomainConceptIdInfo {
    private String sourceConceptIdColumn;
    private String standardConceptIdColumn;

    DomainConceptIdInfo(String sourceConceptIdColumn, String standardConceptIdColumn) {
      this.sourceConceptIdColumn = sourceConceptIdColumn;
      this.standardConceptIdColumn = standardConceptIdColumn;
    }

    String getSourceConceptIdColumn() {
      return this.sourceConceptIdColumn;
    }

    String getStandardConceptIdColumn() {
      return this.standardConceptIdColumn;
    }
  }

  @VisibleForTesting
  public static class QueryAndParameters {
    private final String query;
    private final Map<String, QueryParameterValue> namedParameterValues;

    QueryAndParameters(String query, Map<String, QueryParameterValue> namedParameterValues) {
      this.query = query;
      this.namedParameterValues = namedParameterValues;
    }

    String getQuery() {
      return query;
    }

    Map<String, QueryParameterValue> getNamedParameterValues() {
      return namedParameterValues;
    }
  }

  private CohortQueryBuilder cohortQueryBuilder;
  private ConceptBigQueryService conceptBigQueryService;
  private BigQueryService bigQueryService;
  private CdrBigQuerySchemaConfigService cdrBigQuerySchemaConfigService;

  private final DataSetDao dataSetDao;
  private final ConceptSetDao conceptSetDao;
  private final CohortDao cohortDao;

  @Autowired
  @VisibleForTesting
  public DataSetServiceImpl(
      BigQueryService bigQueryService,
      CdrBigQuerySchemaConfigService cdrBigQuerySchemaConfigService,
      CohortDao cohortDao,
      ConceptBigQueryService conceptBigQueryService,
      ConceptSetDao conceptSetDao,
      CohortQueryBuilder cohortQueryBuilder,
      DataSetDao dataSetDao) {
    this.bigQueryService = bigQueryService;
    this.cdrBigQuerySchemaConfigService = cdrBigQuerySchemaConfigService;
    this.cohortDao = cohortDao;
    this.conceptBigQueryService = conceptBigQueryService;
    this.conceptSetDao = conceptSetDao;
    this.cohortQueryBuilder = cohortQueryBuilder;
    this.dataSetDao = dataSetDao;
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
    final DataSet dataSetModel = new DataSet();
    dataSetModel.setName(name);
    dataSetModel.setVersion(DATA_SET_VERSION);
    dataSetModel.setIncludesAllParticipants(includesAllParticipants);
    dataSetModel.setDescription(description);
    dataSetModel.setWorkspaceId(workspaceId);
    dataSetModel.setInvalid(false);
    dataSetModel.setCreatorId(creatorId);
    dataSetModel.setCreationTime(creationTime);
    dataSetModel.setCohortSetId(cohortIdList);
    dataSetModel.setConceptSetId(conceptIdList);
    dataSetModel.setValues(values);
    dataSetModel.setPrePackagedConceptSetEnum(prePackagedConceptSetEnum);

    return dataSetDao.save(dataSetModel);
  }

  // For domains for which we've assigned a base table in BigQuery, we keep a map here
  // so that we can use the base table name to qualify, for example, PERSON_ID in clauses
  // after SELECT. This map is nearly identical to
  // org.pmiops.workbench.db.dao.ConceptSetDao.DOMAIN_TO_TABLE_NAME,
  // but in some cases we use a different shorthand than the base table name.
  private static final ImmutableMap<Domain, String> DOMAIN_TO_BASE_TABLE_SHORTHAND =
      new ImmutableMap.Builder<Domain, String>()
          .put(Domain.CONDITION, "c_occurrence")
          .put(Domain.DEATH, "death")
          .put(Domain.DRUG, "d_exposure")
          .put(Domain.MEASUREMENT, "measurement")
          .put(Domain.OBSERVATION, "observation")
          .put(Domain.PERSON, "person")
          .put(Domain.PROCEDURE, "procedure")
          .put(Domain.SURVEY, "survey")
          .put(Domain.VISIT, "visit")
          .build();

  @Override
  public Map<String, QueryJobConfiguration> generateQueryJobConfigurationsByDomainName(
      DataSetRequest dataSetRequest) {
    final boolean includesAllParticipants =
        getBuiltinBooleanFromNullable(dataSetRequest.getIncludesAllParticipants());
    final ImmutableList<Cohort> cohortsSelected =
        ImmutableList.copyOf(this.cohortDao.findAllByCohortIdIn(dataSetRequest.getCohortIds()));
    final ImmutableList<DomainValuePair> domainValuePairs =
        ImmutableList.copyOf(dataSetRequest.getDomainValuePairs());

    final ImmutableList<org.pmiops.workbench.db.model.ConceptSet> expandedSelectedConceptSets =
        getExpandedConceptSetSelections(
            dataSetRequest.getPrePackagedConceptSet(),
            dataSetRequest.getConceptSetIds(),
            cohortsSelected,
            includesAllParticipants,
            domainValuePairs);

    // Below constructs the union of all cohort queries
    final ImmutableList<QueryAndParameters> queryMapEntries =
        cohortsSelected.stream()
            .map(this::getCohortQueryStringAndCollectNamedParameters)
            .collect(ImmutableList.toImmutableList());

    final String unionedCohortQueries =
        queryMapEntries.stream()
            .map(QueryAndParameters::getQuery)
            .collect(Collectors.joining(" UNION DISTINCT "));

    final ImmutableSet<Domain> domainSet =
        domainValuePairs.stream()
            .map(DomainValuePair::getDomain)
            .collect(ImmutableSet.toImmutableSet());

    // now merge all the individual maps from each configuration
    final ImmutableMap<String, QueryParameterValue> mergedQueryParameterValues =
        queryMapEntries.stream()
            .map(QueryAndParameters::getNamedParameterValues)
            .flatMap(m -> m.entrySet().stream())
            .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

    return buildQueriesByDomain(
        domainSet,
        domainValuePairs,
        mergedQueryParameterValues,
        includesAllParticipants,
        expandedSelectedConceptSets,
        unionedCohortQueries);
  }

  // note: ImmutableList is OK return type on private methods, but should be avoided in public
  // signatures.
  private ImmutableList<ConceptSet> getExpandedConceptSetSelections(
      PrePackagedConceptSetEnum prePackagedConceptSet,
      List<Long> conceptSetIds,
      List<Cohort> selectedCohorts,
      boolean includesAllParticipants,
      List<DomainValuePair> domainValuePairs) {
    final ImmutableList<org.pmiops.workbench.db.model.ConceptSet> initialSelectedConceptSets =
        ImmutableList.copyOf(this.conceptSetDao.findAllByConceptSetIdIn(conceptSetIds));
    final boolean noCohortsIncluded = selectedCohorts.isEmpty() && !includesAllParticipants;
    if (noCohortsIncluded
        || hasNoConcepts(prePackagedConceptSet, domainValuePairs, initialSelectedConceptSets)) {
      throw new BadRequestException("Data Sets must include at least one cohort and concept.");
    }

    final ImmutableList.Builder<org.pmiops.workbench.db.model.ConceptSet>
        selectedConceptSetsBuilder = ImmutableList.builder();
    selectedConceptSetsBuilder.addAll(initialSelectedConceptSets);

    // If pre packaged all survey concept set is selected create a temp concept set with concept ids
    // of all survey questions
    if (CONCEPT_SETS_NEEDING_PREPACKAGED_SURVEY.contains(prePackagedConceptSet)) {
      selectedConceptSetsBuilder.add(buildPrePackagedSurveyConceptSet());
    }
    return selectedConceptSetsBuilder.build();
  }

  private static boolean hasNoConcepts(
      PrePackagedConceptSetEnum prePackagedConceptSet,
      List<DomainValuePair> domainValuePairs,
      ImmutableList<ConceptSet> initialSelectedConceptSets) {
    return initialSelectedConceptSets.isEmpty()
        && domainValuePairs.isEmpty()
        && prePackagedConceptSet.equals(PrePackagedConceptSetEnum.NONE);
  }

  @VisibleForTesting
  public QueryAndParameters getCohortQueryStringAndCollectNamedParameters(Cohort cohortDbModel) {
    String cohortDefinition = cohortDbModel.getCriteria();
    if (cohortDefinition == null) {
      throw new NotFoundException(
          String.format(
              "Not Found: No Cohort definition matching cohortId: %s",
              cohortDbModel.getCohortId()));
    }
    final SearchRequest searchRequest = new Gson().fromJson(cohortDefinition, SearchRequest.class);
    final QueryJobConfiguration participantIdQuery =
        cohortQueryBuilder.buildParticipantIdQuery(new ParticipantCriteria(searchRequest));
    final QueryJobConfiguration participantQueryConfig =
        bigQueryService.filterBigQueryConfig(participantIdQuery);
    final AtomicReference<String> participantQuery =
        new AtomicReference<>(participantQueryConfig.getQuery());
    final ImmutableMap.Builder<String, QueryParameterValue> cohortNamedParametersBuilder =
        new ImmutableMap.Builder<>();
    participantQueryConfig
        .getNamedParameters()
        .forEach(
            (npKey, npValue) -> {
              final String newKey = buildReplacementKey(cohortDbModel, npKey);
              // replace the original key (when found as a word)
              participantQuery.getAndSet(
                  participantQuery.get().replaceAll("\\b".concat(npKey).concat("\\b"), newKey));
              cohortNamedParametersBuilder.put(newKey, npValue);
            });
    return new QueryAndParameters(participantQuery.get(), cohortNamedParametersBuilder.build());
  }

  // Build a snake_case parameter key from this named parameter key and cohort ID.
  private String buildReplacementKey(Cohort cohort, String npKey) {
    return String.format("%s_%d", npKey, cohort.getCohortId());
  }

  private Map<String, QueryJobConfiguration> buildQueriesByDomain(
      ImmutableSet<Domain> uniqueDomains,
      ImmutableList<DomainValuePair> domainValuePairs,
      ImmutableMap<String, QueryParameterValue> cohortParameters,
      boolean includesAllParticipants,
      ImmutableList<ConceptSet> conceptSetsSelected,
      String cohortQueries) {
    final CdrBigQuerySchemaConfig bigQuerySchemaConfig = cdrBigQuerySchemaConfigService.getConfig();

    return uniqueDomains.stream()
        .collect(
            ImmutableMap.toImmutableMap(
                Domain::toString,
                domain ->
                    buildQueryJobConfigForDomain(
                        domain,
                        domainValuePairs,
                        cohortParameters,
                        includesAllParticipants,
                        conceptSetsSelected,
                        cohortQueries,
                        bigQuerySchemaConfig)));
  }

  private QueryJobConfiguration buildQueryJobConfigForDomain(
      Domain domain,
      List<DomainValuePair> domainValuePairs,
      Map<String, QueryParameterValue> cohortParameters,
      boolean includesAllParticipants,
      List<ConceptSet> conceptSetsSelected,
      String cohortQueries,
      CdrBigQuerySchemaConfig bigQuerySchemaConfig) {
    validateConceptSetSelection(domain, conceptSetsSelected);

    final StringBuilder queryBuilder = new StringBuilder("SELECT ");
    final String personIdQualified = getQualifiedColumnName(domain, PERSON_ID_COLUMN_NAME);

    final List<DomainValuePair> domainValuePairsForCurrentDomain =
        domainValuePairs.stream()
            .filter(dvp -> dvp.getDomain() == domain)
            .collect(Collectors.toList());

    final ValuesLinkingPair valuesLinkingPair =
        getValueSelectsAndJoins(domainValuePairsForCurrentDomain);

    queryBuilder
        .append(String.join(", ", valuesLinkingPair.getSelects()))
        .append(" ")
        .append(valuesLinkingPair.formatJoins());

    final Optional<String> conceptSetSqlInClauseMaybe =
        buildConceptIdListClause(domain, conceptSetsSelected);

    if (supportsConceptSets(domain)) {
      final Optional<DomainConceptIdInfo> domainConceptIdInfoMaybe =
          bigQuerySchemaConfig.cohortTables.values().stream()
              .filter(config -> domain.toString().equals(config.domain))
              .map(
                  tableConfig ->
                      new DomainConceptIdInfo(
                          getColumnName(tableConfig, "source"),
                          getColumnName(tableConfig, "standard")))
              .findFirst();

      final DomainConceptIdInfo domainConceptIdInfo =
          domainConceptIdInfoMaybe.orElseThrow(
              () ->
                  new ServerErrorException(
                      String.format(
                          "Couldn't find source and standard columns for domain: %s",
                          domain.toString())));

      // This adds the where clauses for cohorts and concept sets.
      conceptSetSqlInClauseMaybe.ifPresent(
          clause ->
              queryBuilder
                  .append(" WHERE \n(")
                  .append(domainConceptIdInfo.getStandardConceptIdColumn())
                  .append(clause)
                  .append(" OR \n")
                  .append(domainConceptIdInfo.getSourceConceptIdColumn())
                  .append(clause)
                  .append(")"));

      if (!includesAllParticipants) {
        queryBuilder
            .append(" \nAND (")
            .append(personIdQualified)
            .append(" IN (")
            .append(cohortQueries)
            .append("))");
      }
    } else if (!includesAllParticipants) {
      queryBuilder
          .append(" \nWHERE ")
          .append(personIdQualified)
          .append(" IN (")
          .append(cohortQueries)
          .append(")");
    }

    final String completeQuery = queryBuilder.toString();

    return buildQueryJobConfiguration(cohortParameters, completeQuery);
  }

  private void validateConceptSetSelection(Domain domain, List<ConceptSet> conceptSetsSelected) {
    if (supportsConceptSets(domain)
        && !conceptSetSelectionIsNonemptyAndEachDomainHasAtLeastOneConcept(conceptSetsSelected)) {
      throw new BadRequestException("Concept Sets must contain at least one concept");
    }
  }

  // In some cases, we don't require concept IDs, and in others their absense is fatal.
  // Even if Concept IDs have been selected, these don't work with all domains.
  @VisibleForTesting
  public Optional<String> buildConceptIdListClause(
      Domain domain, List<ConceptSet> conceptSetsSelected) {
    final Optional<String> conceptSetSqlInClauseMaybe;
    if (supportsConceptSets(domain)) {
      conceptSetSqlInClauseMaybe = buildConceptIdSqlInClause(domain, conceptSetsSelected);
    } else {
      conceptSetSqlInClauseMaybe = Optional.empty();
    }
    return conceptSetSqlInClauseMaybe;
  }

  private boolean supportsConceptSets(Domain domain) {
    return domain != Domain.PERSON;
  }

  // Gather all the concept IDs from the ConceptSets provided, taking account of
  // domain-specific rules.
  private Optional<String> buildConceptIdSqlInClause(Domain domain, List<ConceptSet> conceptSets) {
    final String conceptSetIDs =
        conceptSets.stream()
            .filter(cs -> domain == cs.getDomainEnum())
            .flatMap(cs -> cs.getConceptIds().stream().map(cid -> Long.toString(cid)))
            .collect(Collectors.joining(", "));
    if (conceptSetIDs.isEmpty()) {
      return Optional.empty();
    } else {
      return Optional.of(String.format(" IN (%s)", conceptSetIDs));
    }
  }

  private QueryJobConfiguration buildQueryJobConfiguration(
      Map<String, QueryParameterValue> namedCohortParameters, String query) {
    return bigQueryService.filterBigQueryConfig(
        QueryJobConfiguration.newBuilder(query)
            .setNamedParameters(namedCohortParameters)
            .setUseLegacySql(false)
            .build());
  }

  @VisibleForTesting
  public boolean conceptSetSelectionIsNonemptyAndEachDomainHasAtLeastOneConcept(
      List<ConceptSet> conceptSetsSelected) {
    if (conceptSetsSelected.isEmpty()) {
      return false;
    }
    return conceptSetsSelected.stream()
        .collect(Collectors.groupingBy(ConceptSet::getDomain, Collectors.toList()))
        .values()
        .stream()
        .map(csl -> csl.stream().mapToLong(cs -> cs.getConceptIds().size()).sum())
        .allMatch(count -> count > 0);
  }

  private String getQualifiedColumnName(Domain currentDomain, String columnName) {
    final String tableAbbreviation = DOMAIN_TO_BASE_TABLE_SHORTHAND.get(currentDomain);
    return tableAbbreviation == null
        ? columnName
        : String.format("%s.%s", tableAbbreviation, columnName);
  }

  @Override
  public List<String> generateCodeCells(
      KernelTypeEnum kernelTypeEnum,
      String dataSetName,
      Map<String, QueryJobConfiguration> queryJobConfigurationMap) {
    String prerequisites;
    switch (kernelTypeEnum) {
      case R:
        prerequisites =
            "if(! \"reticulate\" %in% installed.packages()) { install.packages(\"reticulate\") }\n"
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
                    + generateNotebookUserCode(
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

  private ValuesLinkingPair getValueSelectsAndJoins(List<DomainValuePair> domainValuePairs) {
    final Optional<Domain> domainMaybe =
        domainValuePairs.stream().map(DomainValuePair::getDomain).findFirst();
    if (!domainMaybe.isPresent()) {
      return ValuesLinkingPair.emptyPair();
    }

    final ImmutableList.Builder<String> valuesUppercaseBuilder = new Builder<>();
    valuesUppercaseBuilder.add("CORE_TABLE_FOR_DOMAIN");
    valuesUppercaseBuilder.addAll(
        domainValuePairs.stream()
            .map(DomainValuePair::getValue)
            .map(String::toUpperCase)
            .collect(Collectors.toList()));

    final String domainName = domainMaybe.get().toString();
    final String domainFirstCharacterCapitalized = capitalizeFirstCharacterOnly(domainName);

    final ImmutableMap<String, QueryParameterValue> queryParameterValuesByDomain =
        ImmutableMap.of(
            "pDomain", QueryParameterValue.string(domainFirstCharacterCapitalized),
            "pValuesList",
                QueryParameterValue.array(
                    valuesUppercaseBuilder.build().toArray(new String[0]), String.class));

    final TableResult valuesLinkingTableResult =
        bigQueryService.executeQuery(
            buildQueryJobConfiguration(
                queryParameterValuesByDomain,
                SELCECT_ALL_FROM_DS_LINKING_WHERE_DOMAIN_MATCHES_LIST));

    final ImmutableList<String> valueSelects =
        StreamSupport.stream(valuesLinkingTableResult.getValues().spliterator(), false)
            .filter(
                fieldValue ->
                    !fieldValue.get("OMOP_SQL").getStringValue().equals("CORE_TABLE_FOR_DOMAIN"))
            .map(fieldValue -> fieldValue.get("OMOP_SQL").getStringValue())
            .collect(ImmutableList.toImmutableList());

    final ImmutableList<String> valueJoins =
        StreamSupport.stream(valuesLinkingTableResult.getValues().spliterator(), false)
            .map(fieldValue -> fieldValue.get(ValuesLinkingPair.JOIN_VALUE_KEY).getStringValue())
            .collect(ImmutableList.toImmutableList());

    return new ValuesLinkingPair(valueSelects, valueJoins);
  }

  // Capitalizes the first letter of a string and lowers the remaining ones.
  // Assumes a single word, so you'd get "A tale of two cities" instead of
  // "A Tale Of Two Cities"
  @VisibleForTesting
  public static String capitalizeFirstCharacterOnly(String text) {
    if (text.isEmpty()) {
      return text;
    } else if (text.length() == 1) {
      return text.toUpperCase();
    } else {
      return String.format(
          "%s%s",
          Character.toString(text.charAt(0)).toUpperCase(), text.substring(1).toLowerCase());
    }
  }

  private static String generateNotebookUserCode(
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
    final boolean isArrayParameter = namedParameterValue.getArrayType() != null;

    final List<QueryParameterValue> arrayValues =
        nullableListToEmpty(namedParameterValue.getArrayValues());

    switch (kernelTypeEnum) {
      case PYTHON:
        return buildPythonNamedParameterQuery(
            key, namedParameterValue, isArrayParameter, arrayValues);
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

  private static String buildPythonNamedParameterQuery(
      String key,
      QueryParameterValue namedParameterValue,
      boolean isArrayParameter,
      List<QueryParameterValue> arrayValues) {
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
  }

  private static <T> List<T> nullableListToEmpty(List<T> nullableList) {
    return Optional.ofNullable(nullableList).orElse(new ArrayList<>());
  }

  private static boolean getBuiltinBooleanFromNullable(Boolean boo) {
    return Optional.ofNullable(boo).orElse(false);
  }

  private ConceptSet buildPrePackagedSurveyConceptSet() {
    final ImmutableList<Long> conceptIds =
        ImmutableList.copyOf(conceptBigQueryService.getSurveyQuestionConceptIds());
    final ConceptSet surveyConceptSet = new ConceptSet();
    surveyConceptSet.setName("All Surveys");
    surveyConceptSet.setDomain(CommonStorageEnums.domainToStorage(Domain.SURVEY));
    surveyConceptSet.setConceptIds(ImmutableSet.copyOf(conceptIds));
    return surveyConceptSet;
  }
}
