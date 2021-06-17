package org.pmiops.workbench.dataset;

import static com.google.cloud.bigquery.StandardSQLTypeName.ARRAY;
import static org.pmiops.workbench.model.PrePackagedConceptSetEnum.SURVEY;

import com.google.cloud.bigquery.FieldList;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.TableResult;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;
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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import javax.inject.Provider;
import javax.persistence.OptimisticLockException;
import javax.transaction.Transactional;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.engine.jdbc.internal.BasicFormatterImpl;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.api.Etags;
import org.pmiops.workbench.cdr.ConceptBigQueryService;
import org.pmiops.workbench.cdr.dao.DSDataDictionaryDao;
import org.pmiops.workbench.cdr.dao.DSLinkingDao;
import org.pmiops.workbench.cdr.model.DbDSDataDictionary;
import org.pmiops.workbench.cdr.model.DbDSLinking;
import org.pmiops.workbench.cohortbuilder.CohortQueryBuilder;
import org.pmiops.workbench.cohortbuilder.ParticipantCriteria;
import org.pmiops.workbench.cohorts.CohortService;
import org.pmiops.workbench.conceptset.ConceptSetService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.dataset.mapper.DataSetMapper;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.ConceptSetDao;
import org.pmiops.workbench.db.dao.DataSetDao;
import org.pmiops.workbench.db.dao.WgsExtractCromwellSubmissionDao;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbConceptSet;
import org.pmiops.workbench.db.model.DbConceptSetConceptId;
import org.pmiops.workbench.db.model.DbDataset;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.FailedPreconditionException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exceptions.NotImplementedException;
import org.pmiops.workbench.model.Cohort;
import org.pmiops.workbench.model.ConceptSet;
import org.pmiops.workbench.model.CriteriaType;
import org.pmiops.workbench.model.DataDictionaryEntry;
import org.pmiops.workbench.model.DataSet;
import org.pmiops.workbench.model.DataSetExportRequest;
import org.pmiops.workbench.model.DataSetPreviewRequest;
import org.pmiops.workbench.model.DataSetRequest;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.DomainValue;
import org.pmiops.workbench.model.DomainValuePair;
import org.pmiops.workbench.model.KernelTypeEnum;
import org.pmiops.workbench.model.PrePackagedConceptSetEnum;
import org.pmiops.workbench.model.ResourceType;
import org.pmiops.workbench.model.SearchGroup;
import org.pmiops.workbench.model.SearchGroupItem;
import org.pmiops.workbench.model.SearchParameter;
import org.pmiops.workbench.model.SearchRequest;
import org.pmiops.workbench.monitoring.GaugeDataCollector;
import org.pmiops.workbench.monitoring.MeasurementBundle;
import org.pmiops.workbench.monitoring.labels.MetricLabel;
import org.pmiops.workbench.monitoring.views.GaugeMetric;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public class DataSetServiceImpl implements DataSetService, GaugeDataCollector {

  private static final String MISSING_EXTRACTION_DIR_PLACEHOLDER =
      "\"WORKSPACE_STORAGE_VCF_DIRECTORY_GOES_HERE\"";

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

  private static final ImmutableList<Domain> DOMAIN_WITHOUT_CONCEPT_SETS =
      ImmutableList.of(
          Domain.PERSON,
          Domain.FITBIT_ACTIVITY,
          Domain.FITBIT_HEART_RATE_LEVEL,
          Domain.FITBIT_HEART_RATE_SUMMARY,
          Domain.FITBIT_INTRADAY_STEPS,
          Domain.WHOLE_GENOME_VARIANT);

  // See https://cloud.google.com/appengine/articles/deadlineexceedederrors for details
  private static final long APP_ENGINE_HARD_TIMEOUT_MSEC_MINUS_FIVE_SEC = 55000L;

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
  private final CohortDao cohortDao;
  private final CohortService cohortService;
  private final ConceptBigQueryService conceptBigQueryService;
  private final ConceptSetDao conceptSetDao;
  private final ConceptSetService conceptSetService;
  private final CohortQueryBuilder cohortQueryBuilder;
  private final DataSetDao dataSetDao;
  private final DSLinkingDao dsLinkingDao;
  private final DSDataDictionaryDao dsDataDictionaryDao;
  private final DataSetMapper dataSetMapper;
  private final WgsExtractCromwellSubmissionDao submissionDao;
  private final Provider<String> prefixProvider;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final Clock clock;

  @Autowired
  @VisibleForTesting
  public DataSetServiceImpl(
      BigQueryService bigQueryService,
      CohortDao cohortDao,
      CohortService cohortService,
      ConceptBigQueryService conceptBigQueryService,
      ConceptSetDao conceptSetDao,
      ConceptSetService conceptSetService,
      CohortQueryBuilder cohortQueryBuilder,
      DataSetDao dataSetDao,
      DSLinkingDao dsLinkingDao,
      DSDataDictionaryDao dsDataDictionaryDao,
      DataSetMapper dataSetMapper,
      WgsExtractCromwellSubmissionDao submissionDao,
      @Qualifier(DatasetConfig.DATASET_PREFIX_CODE) Provider<String> prefixProvider,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      Clock clock) {
    this.bigQueryService = bigQueryService;
    this.cohortDao = cohortDao;
    this.cohortService = cohortService;
    this.conceptBigQueryService = conceptBigQueryService;
    this.conceptSetDao = conceptSetDao;
    this.conceptSetService = conceptSetService;
    this.cohortQueryBuilder = cohortQueryBuilder;
    this.dataSetDao = dataSetDao;
    this.dsLinkingDao = dsLinkingDao;
    this.dsDataDictionaryDao = dsDataDictionaryDao;
    this.dataSetMapper = dataSetMapper;
    this.submissionDao = submissionDao;
    this.prefixProvider = prefixProvider;
    this.workbenchConfigProvider = workbenchConfigProvider;
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
  public DataSet updateDataSet(long workspaceId, long dataSetId, DataSetRequest request) {
    Optional<DbDataset> dbDataSet =
        dataSetDao.findByDataSetIdAndWorkspaceId(dataSetId, workspaceId);

    if (!dbDataSet.isPresent()) {
      throw new NotFoundException(
          "No DataSet found for dataSetId " + dataSetId + " and workspaceId " + workspaceId);
    }

    int version = Etags.toVersion(request.getEtag());
    if (dbDataSet.get().getVersion() != version) {
      throw new ConflictException("Attempted to modify outdated data set version");
    }
    DbDataset dbMappingConvert = dataSetMapper.dataSetRequestToDb(request, dbDataSet.get(), clock);
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
  public TableResult previewBigQueryJobConfig(DataSetPreviewRequest request) {
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
    QueryJobConfiguration previewBigQueryJobConfig =
        buildQueryJobConfiguration(mergedQueryParameterValues, queryBuilder.toString());
    TableResult queryResponse =
        bigQueryService.executeQuery(
            bigQueryService.filterBigQueryConfig(previewBigQueryJobConfig),
            APP_ENGINE_HARD_TIMEOUT_MSEC_MINUS_FIVE_SEC);
    return queryResponse;
  }

  @Override
  public Map<String, QueryJobConfiguration> domainToBigQueryConfig(DataSetRequest dataSetRequest) {
    DbDataset dbDataset;
    if (dataSetRequest.getDataSetId() != null) {
      dbDataset = dataSetDao.findById(dataSetRequest.getDataSetId()).orElse(null);
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
            dataSetMapper.prePackagedConceptSetsFromStorage(dbDataset.getPrePackagedConceptSet()),
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
        .filter( // This filter can be removed once valid SQL is generated for WGS
            domain -> !domain.equals(Domain.WHOLE_GENOME_VARIANT))
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
    return DOMAIN_WITHOUT_CONCEPT_SETS.stream().filter(d -> domain.equals(d)).count() == 0;
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
  public DbDataset mustGetDbDataset(long workspaceId, long dataSetId) {
    return getDbDataSet(workspaceId, dataSetId)
        .<NotFoundException>orElseThrow(
            () -> {
              throw new NotFoundException(
                  String.format(
                      "No DataSet found for workspaceId: %s, dataSetId: %s",
                      workspaceId, dataSetId));
            });
  }

  private String generateRandomEightCharacterQualifier() {
    return prefixProvider.get();
  }

  @Override
  public List<String> generateCodeCells(
      DataSetExportRequest dataSetExportRequest, DbWorkspace dbWorkspace) {
    // TODO(calbach): Verify whether the request payload is ever expected to include a different
    // workspace ID.
    dataSetExportRequest.getDataSetRequest().setWorkspaceId(dbWorkspace.getWorkspaceId());

    validateDataSetRequestResources(
        dbWorkspace.getWorkspaceId(), dataSetExportRequest.getDataSetRequest());

    Map<String, QueryJobConfiguration> queriesByDomain =
        domainToBigQueryConfig(dataSetExportRequest.getDataSetRequest());

    String qualifier = generateRandomEightCharacterQualifier();

    String prerequisites;
    switch (dataSetExportRequest.getKernelType()) {
      case R:
        prerequisites = "library(bigrquery)";
        break;
      case PYTHON:
        prerequisites = "import pandas\n" + "import os";
        break;
      default:
        throw new BadRequestException(
            "Kernel Type " + dataSetExportRequest.getKernelType().toString() + " not supported");
    }

    return Stream.concat(
            queriesByDomain.entrySet().stream()
                .map(
                    entry ->
                        prerequisites
                            + "\n\n"
                            + generateNotebookUserCode(
                                entry.getValue(),
                                Domain.fromValue(entry.getKey()),
                                dataSetExportRequest.getDataSetRequest().getName(),
                                dbWorkspace.getCdrVersion().getName(),
                                qualifier,
                                dataSetExportRequest.getKernelType())),
            generateWgsCode(dataSetExportRequest, dbWorkspace, qualifier).stream())
        .collect(Collectors.toList());
  }

  private List<String> generateWgsCode(
      DataSetExportRequest dataSetExportRequest, DbWorkspace dbWorkspace, String qualifier) {
    if (!dataSetExportRequest.getGenerateGenomicsAnalysisCode()) {
      return new ArrayList<>();
    }

    if (!workbenchConfigProvider.get().featureFlags.enableGenomicExtraction) {
      throw new NotImplementedException();
    }

    if (Strings.isNullOrEmpty(dbWorkspace.getCdrVersion().getWgsBigqueryDataset())) {
      throw new FailedPreconditionException(
          "The workspace CDR version does not have whole genome data");
    }

    if (dataSetExportRequest.getKernelType().equals(KernelTypeEnum.R)) {
      return generateGenomicsAnalysisCommentForR();
    }

    // TODO RW-6806: Add some code to print a user friendly message if the extracted VCF files are
    // not ready yet
    switch (dataSetExportRequest.getGenomicsAnalysisTool()) {
      case HAIL:
        return generateHailCode(qualifier, dataSetExportRequest);
      case PLINK:
        return generatePlinkCode(qualifier, dataSetExportRequest);
      case NONE:
        return generateDownloadVcfCode(qualifier, dataSetExportRequest);
      default:
        throw new BadRequestException("Invalid Genomics Analysis Tool");
    }
  }

  // ericsong: I really dislike using @VisibleForTesting but I couldn't help it until the
  // refactoring in RW-6808 is complete. Then this function should be part of the public
  // interface for GenomicsExtractionService instead of just a private implementation detail
  // of DataSetService's generateCodeCells
  @VisibleForTesting
  public Optional<String> getExtractionDirectory(Long datasetId) {
    return dataSetDao
        .findById(datasetId)
        .flatMap(submissionDao::findMostRecentValidExtractionByDataset)
        .map(submission -> submission.getOutputDir())
        .map(dir -> dir.replaceFirst("/$", ""))
        .filter(dir -> !dir.isEmpty());
  }

  private String generateVcfDirEnvName(String qualifier) {
    return "DATASET_" + qualifier + "_VCF_DIR";
  }

  private String generateExtractionDirCode(
      String qualifier, DataSetExportRequest dataSetExportRequest) {
    String extractionDir =
        getExtractionDirectory(dataSetExportRequest.getDataSetRequest().getDataSetId())
            .orElse(MISSING_EXTRACTION_DIR_PLACEHOLDER);

    String noExtractionDirComment =
        MISSING_EXTRACTION_DIR_PLACEHOLDER.equals(extractionDir)
            ? "# VCF files for this dataset do not exist\n"
                + "# Run a Genomic Extraction from a Dataset to generate VCF files\n"
            : "";

    return noExtractionDirComment
        + "%env "
        + generateVcfDirEnvName(qualifier)
        + "="
        + extractionDir;
  }

  private List<String> generateHailCode(
      String qualifier, DataSetExportRequest dataSetExportRequest) {
    final String matrixName = "mt_" + qualifier;

    return ImmutableList.of(
        generateExtractionDirCode(qualifier, dataSetExportRequest),
        "# Initialize Hail\n"
            + "# Note: Hail must be run from a \"Hail Genomics Analysis\" cloud analysis environment"
            + "\n"
            + "import hail as hl\n"
            + "import os\n"
            + "from hail.plot import show\n"
            + "\n"
            + "hl.init(default_reference='GRCh38')\n"
            + "hl.plot.output_notebook()",
        "# Create Hail Matrix table\n"
            + "# This can take a few hours for a dataset with hundreds of participants\n"
            + "workspace_bucket = os.environ['WORKSPACE_BUCKET']\n"
            + "vcf_dir = os.environ['"
            + generateVcfDirEnvName(qualifier)
            + "']\n"
            + "hail_matrix_table_gcs = f'{workspace_bucket}/dataset_"
            + qualifier
            + ".mt'\n"
            // TODO: handle the case where matrix table has already been imported. Currently - it
            // throws a write error
            + "hl.import_vcf(f'{vcf_dir}/*.vcf.gz', force_bgz=True).write(hail_matrix_table_gcs)\n",
        "# Read Hail Matrix table\n"
            + matrixName
            + " = hl.read_matrix_table(hail_matrix_table_gcs)",
        "# Select variants\n" + matrixName + ".rows().select().show(5)",
        "# Select sample names\n" + matrixName + ".s.show(5)");
  }

  private List<String> generatePlinkCode(
      String qualifier, DataSetExportRequest dataSetExportRequest) {
    final String localVcfDir = "dataset_" + qualifier + "_vcfs";
    final String mergedVcfFilename = "dataset_" + qualifier + "_merged.vcf.gz";
    final String mergedVcfFilepath = localVcfDir + "/" + mergedVcfFilename;
    final String plinkBinaryPrefix = "dataset_" + qualifier + "_plink";

    List<String> plinkCode =
        ImmutableList.of(
            "# Create a single merged VCF file\n"
                + "# This can take a few hours for a dataset with hundreds of participants\n"
                + "\n"
                + "!bcftools concat -a "
                + localVcfDir
                + "/*.vcf.gz -o "
                + mergedVcfFilepath,
            "!plink --vcf " + mergedVcfFilepath + " --make-bed --out " + plinkBinaryPrefix,
            "# Plink binary input files. Optionally - delete "
                + localVcfDir
                + "/ if you plan to only use Plink\n"
                + "# and no longer need the VCF files\n"
                + "!ls "
                + plinkBinaryPrefix
                + ".*");

    return Stream.concat(
            generateDownloadVcfCode(qualifier, dataSetExportRequest).stream(), plinkCode.stream())
        .collect(Collectors.toList());
  }

  private List<String> generateDownloadVcfCode(
      String qualifier, DataSetExportRequest dataSetExportRequest) {
    final String localVcfDir = "dataset_" + qualifier + "_vcfs";

    // TODO: Add a check to see if sufficient disk space is available in the Runtime
    return ImmutableList.of(
        generateExtractionDirCode(qualifier, dataSetExportRequest),
        "%%bash\n"
            + "\n"
            + "# Download VCFs\n"
            + "\n"
            + "mkdir "
            + localVcfDir
            + "\n"
            + "gsutil -m cp ${"
            + generateVcfDirEnvName(qualifier)
            + "}/* "
            + localVcfDir
            + "/");
  }

  private List<String> generateGenomicsAnalysisCommentForR() {
    return ImmutableList.of(
        "# Code generation for genomic analysis tools is not supported in R\n"
            + "# The Google Cloud Storage location of extracted VCF files can be found in the Genomics Extraction History side panel");
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
        dataSetDao
            .findById(dataSet.getDataSetId())
            .orElseThrow(
                () ->
                    new NotFoundException(
                        String.format("ConceptSets %s does not exist", dataSet.getDataSetId())))
            .getCohortIds());
  }

  @Transactional
  @Override
  public List<DbCohort> getCohortsForDataset(DbDataset dataSet) {
    return cohortDao.findAllByCohortIdIn(
        dataSetDao
            .findById(dataSet.getDataSetId())
            .orElseThrow(
                () ->
                    new NotFoundException(
                        String.format("Cohorts %s does not exist", dataSet.getDataSetId())))
            .getCohortIds());
  }

  public List<DataSet> getDataSets(long workspaceId, ResourceType resourceType, long resourceId) {
    return getDbDataSets(workspaceId, resourceType, resourceId).stream()
        .map(dataSetMapper::dbModelToClient)
        .collect(Collectors.toList());
  }

  private List<DbDataset> getDbDataSets(
      long workspaceId, ResourceType resourceType, long resourceId) {
    List<DbDataset> dbDataSets = new ArrayList<>();
    switch (resourceType) {
      case COHORT:
        DbCohort dbCohort = cohortDao.findById(resourceId).orElse(null);
        if (dbCohort == null || dbCohort.getWorkspaceId() != workspaceId) {
          throw new NotFoundException("Resource does not belong to specified workspace");
        }
        dbDataSets =
            dataSetDao.findDataSetsByCohortIdsAndWorkspaceIdAndInvalid(
                resourceId, workspaceId, false);
        break;
      case CONCEPT_SET:
        DbConceptSet dbConceptSet = conceptSetDao.findById(resourceId).orElse(null);
        if (dbConceptSet == null || dbConceptSet.getWorkspaceId() != workspaceId) {
          throw new NotFoundException("Resource does not belong to specified workspace");
        }
        dbDataSets =
            dataSetDao.findDataSetsByConceptSetIdsAndWorkspaceIdAndInvalid(
                resourceId, workspaceId, false);
        break;
    }
    return dbDataSets;
  }

  @Override
  public void deleteDataSet(long workspaceId, long dataSetId) {
    Optional<DbDataset> dbDataset = this.getDbDataSet(workspaceId, dataSetId);
    if (!dbDataset.isPresent()) {
      throw new NotFoundException(
          "No DataSet found for dataSetId " + dataSetId + " and workspaceId " + workspaceId);
    }
    dataSetDao.deleteById(dataSetId);
  }

  @Override
  public Optional<DataSet> getDataSet(long workspaceId, long dataSetId) {
    return dataSetDao
        .findByDataSetIdAndWorkspaceId(dataSetId, workspaceId)
        .map(dataSetMapper::dbModelToClient);
  }

  @Override
  public Optional<DbDataset> getDbDataSet(long workspaceId, long dataSetId) {
    return dataSetDao.findByDataSetIdAndWorkspaceId(dataSetId, workspaceId);
  }

  @Override
  public void markDirty(long workspaceId, ResourceType resourceType, long resourceId) {
    List<DbDataset> dbDataSetList = getDbDataSets(workspaceId, resourceType, resourceId);
    dbDataSetList.forEach(dataSet -> dataSet.setInvalid(true));
    try {
      dataSetDao.saveAll(dbDataSetList);
    } catch (OptimisticLockException e) {
      throw new ConflictException("Failed due to concurrent data set modification");
    }
  }

  @Override
  public DataDictionaryEntry findDataDictionaryEntry(String fieldName, String domain) {
    DbDSDataDictionary dbDSDataDictionary =
        dsDataDictionaryDao.findFirstByFieldNameAndDomain(fieldName, domain);
    if (dbDSDataDictionary == null) {
      throw new NotFoundException(
          "No Data Dictionary Entry found for field " + fieldName + "and domain: " + domain);
    }
    DataDictionaryEntry dataDictionaryEntry = dataSetMapper.dbDsModelToClient(dbDSDataDictionary);
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

  @Override
  public List<String> getPersonIdsWithWholeGenome(DbDataset dataSet) {
    List<ParticipantCriteria> participantCriteriaList;
    if (Boolean.TRUE.equals(dataSet.getIncludesAllParticipants())) {
      // Select all participants with WGS data.
      participantCriteriaList =
          ImmutableList.of(
              new ParticipantCriteria(
                  new SearchRequest().addIncludesItem(createHasWgsSearchGroup())));
    } else {
      participantCriteriaList =
          this.cohortDao.findAllByCohortIdIn(dataSet.getCohortIds()).stream()
              .map(
                  cohort -> {
                    final SearchRequest searchRequest =
                        new Gson().fromJson(cohort.getCriteria(), SearchRequest.class);
                    // AND the existing search criteria with participants having genomics data.
                    searchRequest.addIncludesItem(createHasWgsSearchGroup());
                    return new ParticipantCriteria(searchRequest);
                  })
              .collect(Collectors.toList());
    }

    final QueryJobConfiguration participantIdQuery =
        cohortQueryBuilder.buildUnionedParticipantIdQuery(participantCriteriaList);

    return Streams.stream(
            bigQueryService
                .executeQuery(bigQueryService.filterBigQueryConfig(participantIdQuery))
                .getValues())
        .map(personId -> personId.get(0).getValue().toString())
        .collect(Collectors.toList());
  }

  @Override
  public List<DomainValue> getValueListFromDomain(String domainValue) {
    Domain domain =
        Domain.PHYSICAL_MEASUREMENT_CSS.equals(Domain.valueOf(domainValue))
            ? Domain.MEASUREMENT
            : Domain.valueOf(domainValue);
    FieldList fieldList = bigQueryService.getTableFieldsFromDomain(domain);
    return fieldList.stream()
        .map(field -> new DomainValue().value(field.getName().toLowerCase()))
        .collect(Collectors.toList());
  }

  @Override
  public void validateDataSetPreviewRequestResources(
      long workspaceId, DataSetPreviewRequest request) {
    validateCohortsInWorkspace(workspaceId, request.getCohortIds());
    validateConceptSetsInWorkspace(workspaceId, request.getConceptSetIds());
  }

  /** Validate that the requested resources are contained by the given workspace. */
  private void validateDataSetRequestResources(long workspaceId, DataSetRequest request) {
    if (request.getDataSetId() != null) {
      mustGetDbDataset(workspaceId, request.getDataSetId());
    } else {
      validateCohortsInWorkspace(workspaceId, request.getCohortIds());
      validateConceptSetsInWorkspace(workspaceId, request.getConceptSetIds());
    }
  }

  private void validateCohortsInWorkspace(long workspaceId, @Nullable List<Long> cohortIds) {
    if (CollectionUtils.isEmpty(cohortIds)) {
      return;
    }
    List<Long> workspaceCohortIds =
        cohortService.findByWorkspaceId(workspaceId).stream()
            .map(Cohort::getId)
            .collect(Collectors.toList());

    if (!workspaceCohortIds.containsAll(cohortIds)) {
      throw new NotFoundException("one or more of the requested cohorts were not found");
    }
  }

  private void validateConceptSetsInWorkspace(
      long workspaceId, @Nullable List<Long> conceptSetIds) {
    if (CollectionUtils.isEmpty(conceptSetIds)) {
      return;
    }
    List<Long> workspaceConceptSetIds =
        conceptSetService.findByWorkspaceId(workspaceId).stream()
            .map(ConceptSet::getId)
            .collect(Collectors.toList());
    if (!workspaceConceptSetIds.containsAll(conceptSetIds)) {
      throw new NotFoundException("one or more of the requested concept sets were not found");
    }
  }

  private SearchGroup createHasWgsSearchGroup() {
    return new SearchGroup()
        .items(
            ImmutableList.of(
                new SearchGroupItem()
                    .type(Domain.WHOLE_GENOME_VARIANT.toString())
                    .addSearchParametersItem(
                        new SearchParameter()
                            .domain(Domain.WHOLE_GENOME_VARIANT.toString())
                            .type(CriteriaType.PPI.toString())
                            .group(false))));
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
            namespace
                + "df = pandas.read_gbq("
                + namespace
                + "sql, dialect=\"standard\", progress_bar_type=\"tqdm_notebook\")";
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
