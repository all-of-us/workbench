package org.pmiops.workbench.dataset;

import static com.google.cloud.bigquery.StandardSQLTypeName.ARRAY;
import static org.pmiops.workbench.model.PrePackagedConceptSetEnum.SURVEY;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.persistence.OptimisticLockException;
import javax.transaction.Transactional;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.engine.jdbc.internal.BasicFormatterImpl;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.api.Etags;
import org.pmiops.workbench.cdr.CdrVersionContext;
import org.pmiops.workbench.cdr.ConceptBigQueryService;
import org.pmiops.workbench.cdr.dao.DSDataDictionaryDao;
import org.pmiops.workbench.cdr.dao.DSLinkingDao;
import org.pmiops.workbench.cdr.model.DbDSDataDictionary;
import org.pmiops.workbench.cdr.model.DbDSLinking;
import org.pmiops.workbench.cohortbuilder.CohortQueryBuilder;
import org.pmiops.workbench.cohortbuilder.ParticipantCriteria;
import org.pmiops.workbench.config.CdrBigQuerySchemaConfigService;
import org.pmiops.workbench.dataset.mapper.DataSetMapper;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.ConceptSetDao;
import org.pmiops.workbench.db.dao.DataSetDao;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbConceptSet;
import org.pmiops.workbench.db.model.DbConceptSetConceptId;
import org.pmiops.workbench.db.model.DbDataset;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.DataDictionaryEntry;
import org.pmiops.workbench.model.DataSet;
import org.pmiops.workbench.model.DataSetPreviewRequest;
import org.pmiops.workbench.model.DataSetRequest;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.DomainValuePair;
import org.pmiops.workbench.model.KernelTypeEnum;
import org.pmiops.workbench.model.PrePackagedConceptSetEnum;
import org.pmiops.workbench.model.ResourceType;
import org.pmiops.workbench.model.SearchRequest;
import org.pmiops.workbench.monitoring.GaugeDataCollector;
import org.pmiops.workbench.monitoring.MeasurementBundle;
import org.pmiops.workbench.monitoring.labels.MetricLabel;
import org.pmiops.workbench.monitoring.views.GaugeMetric;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class DataSetServiceImpl implements DataSetService, GaugeDataCollector {

  private static final String CDR_STRING = "\\$\\{projectId}.\\$\\{dataSetId}.";
  private static final String PYTHON_CDR_ENV_VARIABLE =
      "\"\"\" + os.environ[\"WORKSPACE_CDR\"] + \"\"\".";
  // This is implicitly handled by bigrquery, so we don't need this variable.
  private static final String R_CDR_ENV_VARIABLE = "";
  private static final Map<KernelTypeEnum, String> KERNEL_TYPE_TO_ENV_VARIABLE_MAP =
      ImmutableMap.of(
          KernelTypeEnum.R, R_CDR_ENV_VARIABLE, KernelTypeEnum.PYTHON, PYTHON_CDR_ENV_VARIABLE);
  private static final String PREVIEW_QUERY =
      "SELECT ${columns} \nFROM `${projectId}.${dataSetId}.${tableName}`";
  private static final String LIMIT_20 = " LIMIT 20";
  private static final String PERSON_ID_COLUMN_NAME = "PERSON_ID";
  private static final ImmutableList<Domain> OUTER_QUERY_DOMAIN =
      ImmutableList.of(
          Domain.CONDITION,
          Domain.DRUG,
          Domain.MEASUREMENT,
          Domain.OBSERVATION,
          Domain.PROCEDURE,
          Domain.PHYSICAL_MEASUREMENT_CSS);

  @Override
  public Collection<MeasurementBundle> getGaugeData() {
    Map<Boolean, Long> invalidToCount = dataSetDao.getInvalidToCountMap();
    return ImmutableSet.of(
        MeasurementBundle.builder()
            .addMeasurement(GaugeMetric.DATASET_COUNT, invalidToCount.getOrDefault(false, 0L))
            .addTag(MetricLabel.DATASET_INVALID, Boolean.valueOf(false).toString())
            .build(),
        MeasurementBundle.builder()
            .addMeasurement(GaugeMetric.DATASET_COUNT, invalidToCount.getOrDefault(true, 0L))
            .addTag(MetricLabel.DATASET_INVALID, Boolean.valueOf(true).toString())
            .build());
  }

  /*
   * Stores the associated set of selects and joins for values for the data set builder,
   * pulled out of the linking table in Big Query.
   */
  @VisibleForTesting
  private static class ValuesLinkingPair {

    private final List<String> selects;
    private final List<String> joins;
    private final String domainTable;

    private ValuesLinkingPair(List<String> selects, List<String> joins, String domainTable) {
      this.selects = selects;
      this.joins = joins;
      this.domainTable = domainTable;
    }

    private List<String> getSelects() {
      return this.selects;
    }

    private List<String> getJoins() {
      return this.joins;
    }

    static ValuesLinkingPair emptyPair() {
      return new ValuesLinkingPair(Collections.emptyList(), Collections.emptyList(), "");
    }

    public String formatJoins() {
      return getJoins().stream().distinct().collect(Collectors.joining(" "));
    }

    public String getDomainTable() {
      return domainTable;
    }

    public String getTableAlias() {
      String[] parts = domainTable.split(" ");
      return parts[parts.length - 1];
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

    public String getQuery() {
      return query;
    }

    public Map<String, QueryParameterValue> getNamedParameterValues() {
      return namedParameterValues;
    }
  }

  private final BigQueryService bigQueryService;
  private final CdrBigQuerySchemaConfigService cdrBigQuerySchemaConfigService;
  private final CohortDao cohortDao;
  private final ConceptBigQueryService conceptBigQueryService;
  private final ConceptSetDao conceptSetDao;
  private final CohortQueryBuilder cohortQueryBuilder;
  private final DataSetDao dataSetDao;
  private final DSLinkingDao dsLinkingDao;
  private final DSDataDictionaryDao dsDataDictionaryDao;
  private final DataSetMapper dataSetMapper;
  private final Clock clock;

  @Autowired
  @VisibleForTesting
  public DataSetServiceImpl(
      BigQueryService bigQueryService,
      CdrBigQuerySchemaConfigService cdrBigQuerySchemaConfigService,
      CohortDao cohortDao,
      ConceptBigQueryService conceptBigQueryService,
      ConceptSetDao conceptSetDao,
      CohortQueryBuilder cohortQueryBuilder,
      DataSetDao dataSetDao,
      DSLinkingDao dsLinkingDao,
      DSDataDictionaryDao dsDataDictionaryDao,
      DataSetMapper dataSetMapper,
      Clock clock) {
    this.bigQueryService = bigQueryService;
    this.cdrBigQuerySchemaConfigService = cdrBigQuerySchemaConfigService;
    this.cohortDao = cohortDao;
    this.conceptBigQueryService = conceptBigQueryService;
    this.conceptSetDao = conceptSetDao;
    this.cohortQueryBuilder = cohortQueryBuilder;
    this.dataSetDao = dataSetDao;
    this.dsLinkingDao = dsLinkingDao;
    this.dsDataDictionaryDao = dsDataDictionaryDao;
    this.dataSetMapper = dataSetMapper;
    this.clock = clock;
  }

  @Override
  public DataSet saveDataSet(DataSetRequest dataSetRequest, Long userId) {
    DbDataset dbDataset = dataSetMapper.dataSetRequestToDb(dataSetRequest, null, clock);
    dbDataset.setCreatorId(userId);
    return saveDataSet(dbDataset);
  }

  @Override
  public DataSet saveDataSet(DbDataset dataset) {
    try {
      return dataSetMapper.dbModelToClient(dataSetDao.save(dataset));
    } catch (OptimisticLockException e) {
      throw new ConflictException("Failed due to concurrent concept set modification");
    } catch (DataIntegrityViolationException ex) {
      throw new ConflictException("Data set with the same name already exists");
    }
  }

  @Override
  public DataSet updateDataSet(DataSetRequest request, Long dataSetId) {
    DbDataset dbDataSet = dataSetDao.findOne(dataSetId);

    int version = Etags.toVersion(request.getEtag());
    if (dbDataSet.getVersion() != version) {
      throw new ConflictException("Attempted to modify outdated data set version");
    }
    DbDataset dbMappingConvert = dataSetMapper.dataSetRequestToDb(request, dbDataSet, clock);
    return saveDataSet(dbMappingConvert);
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
          .put(Domain.SURVEY, "answer")
          .put(Domain.VISIT, "visit")
          .put(Domain.PHYSICAL_MEASUREMENT_CSS, "measurement")
          .build();

  @Override
  public QueryJobConfiguration previewBigQueryJobConfig(DataSetPreviewRequest request) {
    final Domain domain = request.getDomain();
    final List<String> values = request.getValues();
    Map<String, QueryParameterValue> mergedQueryParameterValues = new HashMap<>();

    final List<String> domainValues =
        bigQueryService
            .getTableFieldsFromDomain(
                Domain.PHYSICAL_MEASUREMENT_CSS.equals(domain) ? Domain.MEASUREMENT : domain)
            .stream()
            .map(field -> field.getName().toLowerCase())
            .collect(Collectors.toList());

    final List<String> filteredDomainColumns =
        values.stream().distinct().filter(domainValues::contains).collect(Collectors.toList());

    final StringBuilder queryBuilder =
        new StringBuilder(
            PREVIEW_QUERY
                .replace("${columns}", String.join(", ", filteredDomainColumns))
                .replace("${tableName}", BigQueryDataSetTableInfo.getTableName(domain)));

    if (supportsConceptSets(domain)) {
      final List<DbConceptSetConceptId> dbConceptSetConceptIds =
          (domain.equals(Domain.SURVEY) && request.getPrePackagedConceptSet().contains(SURVEY))
              ? conceptBigQueryService.getSurveyQuestionConceptIds().stream()
                  .map(
                      c ->
                          DbConceptSetConceptId.builder()
                              .addConceptId(c)
                              .addStandard(false)
                              .build())
                  .collect(Collectors.toList())
              : conceptSetDao.findAllByConceptSetIdIn(request.getConceptSetIds()).stream()
                  .filter(
                      cs ->
                          cs.getDomainEnum().equals(domain)
                              || (cs.getDomainEnum().equals(Domain.PHYSICAL_MEASUREMENT_CSS)
                                  && domain.equals(Domain.MEASUREMENT)))
                  .flatMap(cs -> cs.getConceptSetConceptIds().stream())
                  .collect(Collectors.toList());
      Map<Boolean, List<DbConceptSetConceptId>> partitionSourceAndStandard =
          dbConceptSetConceptIds.stream()
              .collect(Collectors.partitioningBy(DbConceptSetConceptId::getStandard));
      List<DbConceptSetConceptId> standard = partitionSourceAndStandard.get(true);
      List<DbConceptSetConceptId> source = partitionSourceAndStandard.get(false);
      queryBuilder.append(" \nWHERE (");
      if (!standard.isEmpty()) {
        mergedQueryParameterValues.put(
            "standardConceptIds",
            QueryParameterValue.array(
                standard.stream().map(DbConceptSetConceptId::getConceptId).toArray(Long[]::new),
                Long.class));
        queryBuilder.append(BigQueryDataSetTableInfo.getConceptIdIn(domain, true));
      }
      if (!source.isEmpty()) {
        mergedQueryParameterValues.put(
            "sourceConceptIds",
            QueryParameterValue.array(
                source.stream().map(DbConceptSetConceptId::getConceptId).toArray(Long[]::new),
                Long.class));
        if (!standard.isEmpty()) {
          queryBuilder.append(" OR ");
        }
        queryBuilder.append(BigQueryDataSetTableInfo.getConceptIdIn(domain, false));
      }
      queryBuilder.append(")");
    }

    if (!request.getIncludesAllParticipants()) {
      final ImmutableList<QueryAndParameters> queryMapEntries =
          cohortDao.findAllByCohortIdIn(request.getCohortIds()).stream()
              .map(this::getCohortQueryStringAndCollectNamedParameters)
              .collect(ImmutableList.toImmutableList());

      final String unionedCohortQuery =
          queryMapEntries.stream()
              .map(QueryAndParameters::getQuery)
              .collect(Collectors.joining(" UNION DISTINCT "));
      queryBuilder.append(
          supportsConceptSets(domain)
              ? " AND PERSON_ID in (" + unionedCohortQuery + ")"
              : " WHERE PERSON_ID in (" + unionedCohortQuery + ")");

      // now merge all the individual maps from each configuration
      mergedQueryParameterValues.putAll(
          queryMapEntries.stream()
              .map(QueryAndParameters::getNamedParameterValues)
              .flatMap(m -> m.entrySet().stream())
              .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }
    queryBuilder.append(LIMIT_20);

    return buildQueryJobConfiguration(mergedQueryParameterValues, queryBuilder.toString());
  }

  @Override
  public Map<String, QueryJobConfiguration> domainToBigQueryConfig(DataSetRequest dataSetRequest) {
    DbDataset dbDataset;
    if (dataSetRequest.getDataSetId() != null) {
      dbDataset = dataSetDao.findOne(dataSetRequest.getDataSetId());
      // In case wrong dataSetId is passed to Api
      if (dbDataset == null) {
        throw new BadRequestException("Data Set Generate code Failed: Data set not found");
      }
    } else {
      dbDataset = dataSetMapper.dataSetRequestToDb(dataSetRequest, null, clock);
    }
    return buildQueriesByDomain(dbDataset);
  }

  private Map<String, QueryJobConfiguration> buildQueriesByDomain(DbDataset dbDataset) {
    final boolean includesAllParticipants =
        getBuiltinBooleanFromNullable(dbDataset.getIncludesAllParticipants());
    final ImmutableList<DbCohort> cohortsSelected =
        ImmutableList.copyOf(this.cohortDao.findAllByCohortIdIn(dbDataset.getCohortIds()));
    final ImmutableList<DomainValuePair> domainValuePairs =
        ImmutableList.copyOf(
            dbDataset.getValues().stream()
                .map(dataSetMapper::createDomainValuePair)
                .collect(Collectors.toList()));

    final ImmutableList<DbConceptSet> expandedSelectedConceptSets =
        getExpandedConceptSetSelections(
            dataSetMapper.prePackagedConceptSetFromStorage(dbDataset.getPrePackagedConceptSet()),
            dbDataset.getConceptSetIds(),
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
  private ImmutableList<DbConceptSet> getExpandedConceptSetSelections(
      List<PrePackagedConceptSetEnum> prePackagedConceptSet,
      List<Long> conceptSetIds,
      List<DbCohort> selectedCohorts,
      boolean includesAllParticipants,
      List<DomainValuePair> domainValuePairs) {
    final ImmutableList<DbConceptSet> initialSelectedConceptSets =
        ImmutableList.copyOf(this.conceptSetDao.findAllByConceptSetIdIn(conceptSetIds));
    final boolean noCohortsIncluded = selectedCohorts.isEmpty() && !includesAllParticipants;
    if (noCohortsIncluded
        || hasNoConcepts(prePackagedConceptSet, domainValuePairs, initialSelectedConceptSets)) {
      throw new BadRequestException("Data Sets must include at least one cohort and concept.");
    }

    final ImmutableList.Builder<DbConceptSet> selectedConceptSetsBuilder = ImmutableList.builder();
    selectedConceptSetsBuilder.addAll(initialSelectedConceptSets);

    // If pre packaged all survey concept set is selected create a temp concept set with concept ids
    // of all survey questions
    if (prePackagedConceptSet.contains(SURVEY)
        || prePackagedConceptSet.contains(PrePackagedConceptSetEnum.BOTH)) {
      selectedConceptSetsBuilder.add(buildPrePackagedSurveyConceptSet());
    }
    return selectedConceptSetsBuilder.build();
  }

  private static boolean hasNoConcepts(
      List<PrePackagedConceptSetEnum> prePackagedConceptSet,
      List<DomainValuePair> domainValuePairs,
      ImmutableList<DbConceptSet> initialSelectedConceptSets) {
    return initialSelectedConceptSets.isEmpty()
        && domainValuePairs.isEmpty()
        && prePackagedConceptSet.size() == 1
        && prePackagedConceptSet.get(0).equals(PrePackagedConceptSetEnum.NONE);
  }

  @VisibleForTesting
  public QueryAndParameters getCohortQueryStringAndCollectNamedParameters(DbCohort cohortDbModel) {
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
    final AtomicReference<String> participantQuery =
        new AtomicReference<>(participantIdQuery.getQuery());
    final ImmutableMap.Builder<String, QueryParameterValue> cohortNamedParametersBuilder =
        new ImmutableMap.Builder<>();
    participantIdQuery
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
  private String buildReplacementKey(DbCohort cohort, String npKey) {
    return String.format("%s_%d", npKey, cohort.getCohortId());
  }

  private Map<String, QueryJobConfiguration> buildQueriesByDomain(
      ImmutableSet<Domain> uniqueDomains,
      ImmutableList<DomainValuePair> domainValuePairs,
      ImmutableMap<String, QueryParameterValue> cohortParameters,
      boolean includesAllParticipants,
      ImmutableList<DbConceptSet> conceptSetsSelected,
      String cohortQueries) {

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
                        cohortQueries)));
  }

  private QueryJobConfiguration buildQueryJobConfigForDomain(
      Domain domain,
      List<DomainValuePair> domainValuePairs,
      Map<String, QueryParameterValue> cohortParameters,
      boolean includesAllParticipants,
      List<DbConceptSet> conceptSetsSelected,
      String cohortQueries) {
    validateConceptSetSelection(domain, conceptSetsSelected);

    final StringBuilder queryBuilder = new StringBuilder("SELECT ");
    final String personIdQualified = getQualifiedColumnName(domain, PERSON_ID_COLUMN_NAME);

    final List<DomainValuePair> domainValuePairsForCurrentDomain =
        domainValuePairs.stream()
            .filter(dvp -> dvp.getDomain() == domain)
            .collect(Collectors.toList());

    final ValuesLinkingPair valuesLinkingPair =
        getValueSelectsAndJoins(domainValuePairsForCurrentDomain);

    if (OUTER_QUERY_DOMAIN.contains(domain)) {
      queryBuilder
          .append(String.join(", ", valuesLinkingPair.getSelects()))
          .append(" from ( SELECT * ")
          .append(valuesLinkingPair.getDomainTable());
    } else {
      queryBuilder
          .append(String.join(", ", valuesLinkingPair.getSelects()))
          .append(" ")
          .append(valuesLinkingPair.getDomainTable())
          .append(" ")
          .append(valuesLinkingPair.formatJoins());
    }

    final Optional<String> conceptSetSqlInClauseMaybe =
        buildConceptIdListClause(domain, conceptSetsSelected);

    if (supportsConceptSets(domain)) {
      // This adds the where clauses for cohorts and concept sets.
      conceptSetSqlInClauseMaybe.ifPresent(
          clause -> queryBuilder.append(" WHERE \n").append(clause));

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

    if (domain == Domain.FITBIT_HEART_RATE_LEVEL || domain == Domain.FITBIT_INTRADAY_STEPS) {
      queryBuilder.append("\nGROUP BY PERSON_ID");
      if (valuesLinkingPair.getSelects().stream().filter(select -> select.contains("DATE")).count()
          == 1) {
        queryBuilder.append(", DATE");
      }
    }

    if (OUTER_QUERY_DOMAIN.contains(domain)) {
      queryBuilder.append(") " + valuesLinkingPair.getTableAlias());
      if (!valuesLinkingPair.formatJoins().equals("")) {
        queryBuilder.append(" " + valuesLinkingPair.formatJoins());
      }
    }
    return buildQueryJobConfiguration(cohortParameters, queryBuilder.toString());
  }

  private void validateConceptSetSelection(Domain domain, List<DbConceptSet> conceptSetsSelected) {
    if (supportsConceptSets(domain)
        && !conceptSetSelectionIsNonemptyAndEachDomainHasAtLeastOneConcept(conceptSetsSelected)) {
      throw new BadRequestException("Concept Sets must contain at least one concept");
    }
  }

  // In some cases, we don't require concept IDs, and in others their absense is fatal.
  // Even if Concept IDs have been selected, these don't work with all domains.
  @VisibleForTesting
  public Optional<String> buildConceptIdListClause(
      Domain domain, List<DbConceptSet> conceptSetsSelected) {
    final Optional<String> conceptSetSqlInClauseMaybe;
    if (supportsConceptSets(domain)) {
      conceptSetSqlInClauseMaybe = buildConceptIdSqlInClause(domain, conceptSetsSelected);
    } else {
      conceptSetSqlInClauseMaybe = Optional.empty();
    }
    return conceptSetSqlInClauseMaybe;
  }

  private boolean supportsConceptSets(Domain domain) {
    return domain != Domain.PERSON
        && domain != Domain.FITBIT_ACTIVITY
        && domain != Domain.FITBIT_HEART_RATE_LEVEL
        && domain != Domain.FITBIT_HEART_RATE_SUMMARY
        && domain != Domain.FITBIT_INTRADAY_STEPS;
  }

  // Gather all the concept IDs from the ConceptSets provided, taking account of
  // domain-specific rules.
  private Optional<String> buildConceptIdSqlInClause(
      Domain domain, List<DbConceptSet> conceptSets) {
    final List<DbConceptSet> dbConceptSets =
        conceptSets.stream()
            .filter(
                cs ->
                    cs.getDomainEnum().equals(domain)
                        || (cs.getDomainEnum().equals(Domain.PHYSICAL_MEASUREMENT)
                            && domain.equals(Domain.MEASUREMENT)))
            .collect(Collectors.toList());
    if (preDefinedSurveyConceptSet(dbConceptSets)) {
      return Optional.of(
          "("
              + BigQueryDataSetTableInfo.getConceptIdIn(domain, false)
                  .replace("unnest", "")
                  .replace(
                      "@sourceConceptIds",
                      ConceptBigQueryService.SURVEY_QUESTION_CONCEPT_ID_SQL_TEMPLATE)
              + ")");
    } else {
      final List<DbConceptSetConceptId> dbConceptSetConceptIds =
          dbConceptSets.stream()
              .flatMap(cs -> cs.getConceptSetConceptIds().stream())
              .collect(Collectors.toList());
      if (dbConceptSetConceptIds.isEmpty()) {
        return Optional.empty();
      } else {
        StringBuilder queryBuilder = new StringBuilder();
        Map<Boolean, List<DbConceptSetConceptId>> partitionSourceAndStandard =
            dbConceptSetConceptIds.stream()
                .collect(Collectors.partitioningBy(DbConceptSetConceptId::getStandard));
        String standardConceptIds =
            partitionSourceAndStandard.get(true).stream()
                .map(c -> c.getConceptId().toString())
                .collect(Collectors.joining(", "));
        String sourceConceptIds =
            partitionSourceAndStandard.get(false).stream()
                .map(c -> c.getConceptId().toString())
                .collect(Collectors.joining(", "));
        if (!standardConceptIds.isEmpty()) {
          queryBuilder.append(
              BigQueryDataSetTableInfo.getConceptIdIn(domain, true)
                  .replaceAll("unnest", "")
                  .replaceAll("(@standardConceptIds)", standardConceptIds));
        }
        if (!sourceConceptIds.isEmpty()) {
          if (!standardConceptIds.isEmpty()) {
            queryBuilder.append(" OR ");
          }
          queryBuilder.append(
              BigQueryDataSetTableInfo.getConceptIdIn(domain, false)
                  .replaceAll("unnest", "")
                  .replaceAll("(@sourceConceptIds)", sourceConceptIds));
        }
        return Optional.of("(" + queryBuilder.toString() + ")");
      }
    }
  }

  private boolean preDefinedSurveyConceptSet(List<DbConceptSet> dbConceptSets) {
    return dbConceptSets.stream()
        .anyMatch(c -> c.getConceptSetId() == 0 && Domain.SURVEY.equals(c.getDomainEnum()));
  }

  private QueryJobConfiguration buildQueryJobConfiguration(
      Map<String, QueryParameterValue> namedCohortParameters, String query) {
    return QueryJobConfiguration.newBuilder(query)
        .setNamedParameters(namedCohortParameters)
        .setUseLegacySql(false)
        .build();
  }

  @VisibleForTesting
  public boolean conceptSetSelectionIsNonemptyAndEachDomainHasAtLeastOneConcept(
      List<DbConceptSet> conceptSetsSelected) {
    if (preDefinedSurveyConceptSet(conceptSetsSelected)) {
      return true;
    }
    if (conceptSetsSelected.isEmpty()) {
      return false;
    }
    return conceptSetsSelected.stream()
        .collect(Collectors.groupingBy(DbConceptSet::getDomain, Collectors.toList()))
        .values()
        .stream()
        .map(csl -> csl.stream().mapToLong(cs -> cs.getConceptSetConceptIds().size()).sum())
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
      String cdrVersionName,
      String qualifier,
      Map<String, QueryJobConfiguration> queryJobConfigurationMap) {
    String prerequisites;
    switch (kernelTypeEnum) {
      case R:
        prerequisites = "library(bigrquery)";
        break;
      case PYTHON:
        prerequisites = "import pandas\n" + "import os";
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
                        cdrVersionName,
                        qualifier,
                        kernelTypeEnum))
        .collect(Collectors.toList());
  }

  @Override
  public List<String> generateMicroarrayCohortExtractCodeCells(
      DbWorkspace dbWorkspace,
      String qualifier,
      Map<String, QueryJobConfiguration> queriesByDomain) {
    String joinedDatasetVariableNames =
        queriesByDomain.entrySet().stream()
            .map(
                e ->
                    "dataset_"
                        + qualifier
                        + "_"
                        + Domain.fromValue(e.getKey()).toString().toLowerCase()
                        + "_df")
            .collect(Collectors.joining(", "));

    final String cohortSampleNamesFilename = "cohort_sample_names_" + qualifier + ".txt";
    final String cohortSampleMapFilename = "cohort_sample_map_" + qualifier + ".csv";
    final String cohortVcfFilename = "cohort_" + qualifier + ".vcf";
    // TODO(RW-5735): Writing to the "tmp" dataset is a temporary workaround.
    final String cohortExtractTable =
        "fc-aou-cdr-synth-test.tmp_shared_cohort_extract."
            + UUID.randomUUID().toString().replace("-", "_");

    return ImmutableList.of(
        "person_ids = set()\n"
            + "datasets = ["
            + joinedDatasetVariableNames
            + "]\n"
            + "\n"
            + "for dataset in datasets:\n"
            + "    if 'PERSON_ID' in dataset:\n"
            + "        person_ids = person_ids.union(dataset['PERSON_ID'])\n"
            + "    elif 'person_id' in dataset:\n"
            + "        person_ids = person_ids.union(dataset['person_id']) \n"
            + "\n\n"
            + "with open('"
            + cohortSampleNamesFilename
            + "', 'w') as cohort_file:\n"
            + "    for person_id in person_ids:\n"
            + "        cohort_file.write(str(person_id) + '\\n')\n"
            + "    cohort_file.close()\n",
        "!python3 /usr/local/share/raw_array_cohort_extract.py \\\n"
            + "          --dataset fc-aou-cdr-synth-test.synthetic_microarray_data \\\n"
            + "          --fq_destination_table "
            + cohortExtractTable
            + " \\\n"
            + "          --query_project ${GOOGLE_PROJECT} \\\n"
            // TODO: Replace hardcoded dataset reference: RW-5748
            + "          --fq_sample_mapping_table fc-aou-cdr-synth-test.synthetic_microarray_data.sample_list \\\n"
            + "          --cohort_sample_names_file "
            + cohortSampleNamesFilename
            + " \\\n"
            + "          --sample_map_outfile "
            + cohortSampleMapFilename
            + "\n",
        "!java -jar ${GATK_LOCAL_JAR} ArrayExtractCohort \\\n"
            // TODO: This value will need to be tuned per environment.
            + "        -R gs://fc-aou-cdr-synth-test-genomics/extract_resources/Homo_sapiens_assembly19.fasta \\\n"
            + "        -O "
            + cohortVcfFilename
            + " \\\n"
            + "        --probe-info-table fc-aou-cdr-synth-test.synthetic_microarray_data.probe_info \\\n"
            + "        --read-project-id ${GOOGLE_PROJECT} \\\n"
            + "        --cohort-sample-file "
            + cohortSampleMapFilename
            + " \\\n"
            + "        --use-compressed-data \"false\" \\\n"
            + "        --cohort-extract-table "
            + cohortExtractTable
            + "\n",
        "!gsutil cp " + cohortVcfFilename + " ${WORKSPACE_BUCKET}/cohort-extract/");
  }

  @Override
  public List<String> generatePlinkDemoCode(String qualifier) {
    final String cohortQualifier = "cohort_" + qualifier;
    final String phenotypeFilename = "phenotypes_" + qualifier + ".phe";
    final String cohortVcfFilename = cohortQualifier + ".vcf";

    return ImmutableList.of(
        "import random\n\n"
            + "phenotypes_table = []\n"
            + "for person_id in person_ids:\n"
            + "    family_id = 0 # Family ID is set to 0 for all participants because we do not provide familial information at this time\n"
            + "    person_id = person_id\n"
            + "    phenotype_1 = random.randint(0, 2) # Change this value to what makes sense for your research by looking through the dataset(s)\n"
            + "    phenotype_2 = random.randint(0, 2) # Change this value as well or remove if you are only processing one phenotype \n"
            + "    phenotypes_table.append([family_id, person_id, phenotype_1, phenotype_2])\n"
            + "\n"
            + "cohort_phenotypes = pandas.DataFrame(phenotypes_table) \n"
            + "cohort_phenotypes.to_csv('"
            + phenotypeFilename
            + "', header=False, index=False, sep=' ')",
        "%%bash\n\n"
            + "# Convert VCF info plink binary files \n"
            + "plink --vcf-half-call m --const-fid 0 --vcf "
            + cohortVcfFilename
            + " --out "
            + cohortQualifier
            + "\n"
            + "# Run GWAS \n"
            + "plink --bfile "
            + cohortQualifier
            + " --pheno "
            + phenotypeFilename
            + " --all-pheno --allow-no-sex --assoc --out results\n"
            + "\n"
            + "head results.P1.assoc\n"
            + "head results.P2.assoc");
  }

  @Override
  public List<String> generateHailDemoCode(String qualifier) {
    final String phenotypeFilename = "phenotypes_annotations_" + qualifier + ".tsv";
    final String cohortQualifier = "cohort_" + qualifier;
    final String cohortVcfFilename = cohortQualifier + ".vcf";
    final String cohortMatrixFilename = cohortQualifier + ".mt";

    return ImmutableList.of(
        "import subprocess, os\n"
            + "import random\n"
            + "\n"
            + "# Creating phenotype annotations file\n"
            + "phenotypes_table = []\n"
            + "for person_id in person_ids:\n"
            + "    phenotype_1 = random.randint(0, 2) # Change this value to what makes sense for your research by looking through the dataset(s)\n"
            + "    phenotype_2 = random.randint(0, 2) # Change this value as well or remove if you are only processing one phenotype \n"
            + "    phenotypes_table.append([person_id, phenotype_1, phenotype_2])\n"
            + "\n"
            + "cohort_phenotypes = pandas.DataFrame(phenotypes_table,columns=[\"sample_name\", \"phenotype1\", \"phenotype2\"]) \n"
            + "cohort_phenotypes.to_csv('"
            + phenotypeFilename
            + "', index=False, sep='\\t')\n"
            + "\n"
            + "subprocess.run([\"gsutil\", \"cp\", \""
            + phenotypeFilename
            + "\", os.environ['WORKSPACE_BUCKET']])",
        "import hail as hl\n"
            + "import os\n"
            + "from hail.plot import show\n"
            + "\n"
            + "hl.plot.output_notebook()\n"
            + "bucket = os.environ['WORKSPACE_BUCKET']\n"
            + "hl.import_vcf(f'{bucket}/cohort-extract/"
            + cohortVcfFilename
            + "').write(f'{bucket}/"
            + cohortMatrixFilename
            + "')\n"
            + "table = hl.import_table(f'{bucket}/"
            + phenotypeFilename
            + "', types={'sample_name': hl.tstr}, impute=True, key='sample_name')\n"
            + "\n"
            + "mt = hl.read_matrix_table(f'{bucket}/"
            + cohortMatrixFilename
            + "');\n"
            + "mt = mt.annotate_cols(pheno = table[mt.s])\n"
            + "\n"
            + "covariates = [1, mt.pheno.phenotype2]\n"
            + "gwas = hl.linear_regression_rows(y=mt.pheno.phenotype1, x=mt.GT.n_alt_alleles(), covariates=covariates)\n"
            + "p = hl.plot.manhattan(gwas.p_value)\n"
            + "show(p)");
  }

  @Override
  @Transactional
  public DbDataset cloneDataSetToWorkspace(
      DbDataset fromDataSet,
      DbWorkspace toWorkspace,
      Set<Long> cohortIds,
      Set<Long> conceptSetIds,
      List<Short> prePackagedConceptSets) {
    DbDataset toDataSet = new DbDataset(fromDataSet);
    toDataSet.setWorkspaceId(toWorkspace.getWorkspaceId());
    toDataSet.setCreatorId(toWorkspace.getCreator().getUserId());
    toDataSet.setLastModifiedTime(toWorkspace.getLastModifiedTime());
    toDataSet.setCreationTime(toWorkspace.getCreationTime());

    toDataSet.setConceptSetIds(new ArrayList<>(conceptSetIds));
    toDataSet.setCohortIds(new ArrayList<>(cohortIds));
    toDataSet.setPrePackagedConceptSet(prePackagedConceptSets);
    return dataSetDao.save(toDataSet);
  }

  @Override
  public List<DbDataset> getDataSets(DbWorkspace workspace) {
    // Allows for fetching data sets for a workspace once its collection is no longer
    // bound to a session.
    return dataSetDao.findByWorkspaceId(workspace.getWorkspaceId());
  }

  @Transactional
  @Override
  public List<DbConceptSet> getConceptSetsForDataset(DbDataset dataSet) {
    return conceptSetDao.findAllByConceptSetIdIn(
        dataSetDao.findOne(dataSet.getDataSetId()).getCohortIds());
  }

  @Transactional
  @Override
  public List<DbCohort> getCohortsForDataset(DbDataset dataSet) {
    return cohortDao.findAllByCohortIdIn(dataSetDao.findOne(dataSet.getDataSetId()).getCohortIds());
  }

  public List<DataSet> getDataSets(ResourceType resourceType, long resourceId) {
    return getDbDataSets(resourceType, resourceId).stream()
        .map(dataSetMapper::dbModelToClient)
        .collect(Collectors.toList());
  }

  public List<DbDataset> getDbDataSets(ResourceType resourceType, long resourceId) {
    List<DbDataset> dbDataSets = new ArrayList<>();
    switch (resourceType) {
      case COHORT:
        dbDataSets = dataSetDao.findDataSetsByCohortIds(resourceId);
        break;
      case CONCEPT_SET:
        dbDataSets = dataSetDao.findDataSetsByConceptSetIds(resourceId);
        break;
    }
    return dbDataSets;
  }

  @Override
  public void deleteDataSet(Long dataSetId) {
    dataSetDao.delete(dataSetId);
  }

  @Override
  public Optional<DataSet> getDbDataSet(Long dataSetId) {
    return Optional.of(dataSetMapper.dbModelToClient(dataSetDao.findOne(dataSetId)));
  }

  @Override
  public void markDirty(ResourceType resourceType, long resourceId) {
    List<DbDataset> dbDataSetList = getDbDataSets(resourceType, resourceId);
    dbDataSetList.forEach(dataSet -> dataSet.setInvalid(true));
    try {
      dataSetDao.save(dbDataSetList);
    } catch (OptimisticLockException e) {
      throw new ConflictException("Failed due to concurrent data set modification");
    }
  }

  @Override
  public DataDictionaryEntry findDataDictionaryEntry(
      String fieldName, String domain, DbCdrVersion cdrVersion) {
    CdrVersionContext.setCdrVersionNoCheckAuthDomain(cdrVersion);
    DbDSDataDictionary dbDSDataDictionary =
        dsDataDictionaryDao.findDbDSDataDictionaryByFieldNameAndDomain(fieldName, domain);
    if (dbDSDataDictionary == null) {
      throw new NotFoundException(
          "No Data Dictionary Entry found for domain: "
              + fieldName
              + " cdr version: "
              + cdrVersion);
    }
    DataDictionaryEntry dataDictionaryEntry = dataSetMapper.dbDsModelToClient(dbDSDataDictionary);
    dataDictionaryEntry.setCdrVersionId(cdrVersion.getCdrVersionId());
    return dataDictionaryEntry;
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
    final String domainFirstCharacterCapitalized =
        capitalizeFirstCharacterOnly(
            Domain.PHYSICAL_MEASUREMENT_CSS.toString().equals(domainName)
                ? Domain.MEASUREMENT.toString()
                : domainName);

    final List<DbDSLinking> valuesLinkingTableResult =
        dsLinkingDao.findByDomainAndDenormalizedNameIn(
            domainFirstCharacterCapitalized, valuesUppercaseBuilder.build());

    final ImmutableList<String> valueSelects =
        valuesLinkingTableResult.stream()
            .filter(fieldValue -> !fieldValue.getOmopSql().equals("CORE_TABLE_FOR_DOMAIN"))
            .map(DbDSLinking::getOmopSql)
            .collect(ImmutableList.toImmutableList());

    final ImmutableList<String> coreTable =
        valuesLinkingTableResult.stream()
            .filter(fieldValue -> fieldValue.getOmopSql().equals("CORE_TABLE_FOR_DOMAIN"))
            .map(DbDSLinking::getJoinValue)
            .collect(ImmutableList.toImmutableList());
    String domainTable = coreTable.isEmpty() ? "" : coreTable.get(0);

    final ImmutableList<String> valueJoins =
        valuesLinkingTableResult.stream()
            .filter(fieldValue -> !fieldValue.getJoinValue().equals(domainTable))
            .map(DbDSLinking::getJoinValue)
            .collect(ImmutableList.toImmutableList());

    return new ValuesLinkingPair(valueSelects, valueJoins, domainTable);
  }

  // Capitalizes the first letter of a string and lowers the remaining ones.
  // Assumes a single word, so you'd get "A tale of two cities" instead of
  // "A Tale Of Two Cities"
  @VisibleForTesting
  public static String capitalizeFirstCharacterOnly(String text) {
    return StringUtils.capitalize(text.toLowerCase());
  }

  private static String generateSqlWithEnvironmentVariables(
      String query, KernelTypeEnum kernelTypeEnum) {
    return new BasicFormatterImpl()
        .format(query.replaceAll(CDR_STRING, KERNEL_TYPE_TO_ENV_VARIABLE_MAP.get(kernelTypeEnum)));
  }

  // This takes the query, and string replaces in the values for each of the named
  // parameters generated. For example:
  //    SELECT * FROM cdr.dataset.person WHERE criteria IN unnest(@p1_1)
  // becomes:
  //    SELECT * FROM cdr.dataset.person WHERE criteria IN (1, 2, 3)
  private static String fillInQueryParams(
      String query, Map<String, QueryParameterValue> queryParameterValueMap) {
    return queryParameterValueMap.entrySet().stream()
        .map(param -> (Function<String, String>) s -> replaceParameter(s, param))
        .reduce(Function.identity(), Function::andThen)
        .apply(query)
        .replaceAll("unnest", "");
  }

  private static String replaceParameter(
      String s, Map.Entry<String, QueryParameterValue> parameter) {
    String value =
        ARRAY.equals(parameter.getValue().getType())
            ? nullableListToEmpty(parameter.getValue().getArrayValues()).stream()
                .map(DataSetServiceImpl::convertSqlTypeToString)
                .collect(Collectors.joining(", "))
            : convertSqlTypeToString(parameter.getValue());
    String key = String.format("@%s", parameter.getKey());
    return s.replaceAll(key, value);
  }

  private static String convertSqlTypeToString(QueryParameterValue parameter) {
    switch (parameter.getType()) {
      case BOOL:
        return Boolean.valueOf(parameter.getValue()) ? "1" : "0";
      case INT64:
      case FLOAT64:
      case NUMERIC:
        return parameter.getValue();
      case STRING:
      case TIMESTAMP:
      case DATE:
        return String.format("'%s'", parameter.getValue());
      default:
        throw new RuntimeException();
    }
  }

  private static String generateNotebookUserCode(
      QueryJobConfiguration queryJobConfiguration,
      Domain domain,
      String dataSetName,
      String cdrVersionName,
      String qualifier,
      KernelTypeEnum kernelTypeEnum) {

    // Define [namespace]_sql, query parameters (as either [namespace]_query_config
    // or [namespace]_query_parameters), and [namespace]_df variables
    String domainAsString = domain.toString().toLowerCase();
    String namespace = "dataset_" + qualifier + "_" + domainAsString + "_";
    // Comments in R and Python have the same syntax
    String descriptiveComment =
        String.format(
            "# This query represents dataset \"%s\" for domain \"%s\" and was generated for %s",
            dataSetName, domainAsString, cdrVersionName);
    String sqlSection;
    String dataFrameSection;
    String displayHeadSection;

    switch (kernelTypeEnum) {
      case PYTHON:
        sqlSection =
            namespace
                + "sql = \"\"\""
                + fillInQueryParams(
                    generateSqlWithEnvironmentVariables(
                        queryJobConfiguration.getQuery(), kernelTypeEnum),
                    queryJobConfiguration.getNamedParameters())
                + "\"\"\"";
        dataFrameSection =
            namespace + "df = pandas.read_gbq(" + namespace + "sql, dialect=\"standard\")";
        displayHeadSection = namespace + "df.head(5)";
        break;
      case R:
        sqlSection =
            namespace
                + "sql <- paste(\""
                + fillInQueryParams(
                    generateSqlWithEnvironmentVariables(
                        queryJobConfiguration.getQuery(), kernelTypeEnum),
                    queryJobConfiguration.getNamedParameters())
                + "\", sep=\"\")";
        dataFrameSection =
            namespace
                + "df <- bq_table_download(bq_dataset_query(Sys.getenv(\"WORKSPACE_CDR\"), "
                + namespace
                + "sql, billing=Sys.getenv(\"GOOGLE_PROJECT\")), bigint=\"integer64\")";
        displayHeadSection = "head(" + namespace + "df, 5)";
        break;
      default:
        throw new BadRequestException("Language " + kernelTypeEnum.toString() + " not supported.");
    }

    return descriptiveComment
        + "\n"
        + sqlSection
        + "\n\n"
        + dataFrameSection
        + "\n\n"
        + displayHeadSection;
  }

  private static <T> List<T> nullableListToEmpty(List<T> nullableList) {
    return Optional.ofNullable(nullableList).orElse(new ArrayList<>());
  }

  private static boolean getBuiltinBooleanFromNullable(Boolean boo) {
    return Optional.ofNullable(boo).orElse(false);
  }

  private DbConceptSet buildPrePackagedSurveyConceptSet() {
    final DbConceptSet surveyConceptSet = new DbConceptSet();
    surveyConceptSet.setName("All Surveys");
    surveyConceptSet.setDomain(DbStorageEnums.domainToStorage(Domain.SURVEY));
    return surveyConceptSet;
  }
}
