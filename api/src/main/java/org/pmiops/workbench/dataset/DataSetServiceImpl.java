package org.pmiops.workbench.dataset;

import static com.google.cloud.bigquery.StandardSQLTypeName.ARRAY;
import static org.pmiops.workbench.cohortbuilder.SearchGroupItemQueryBuilder.CHILD_LOOKUP_SQL;
import static org.pmiops.workbench.cohortbuilder.SearchGroupItemQueryBuilder.QUESTION_LOOKUP_SQL;
import static org.pmiops.workbench.model.PrePackagedConceptSetEnum.SURVEY;

import com.google.cloud.bigquery.FieldList;
import com.google.cloud.bigquery.LegacySQLTypeName;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import javax.inject.Provider;
import javax.persistence.OptimisticLockException;
import javax.transaction.Transactional;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.engine.jdbc.internal.BasicFormatterImpl;
import org.jetbrains.annotations.NotNull;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.api.Etags;
import org.pmiops.workbench.cdr.dao.DSDataDictionaryDao;
import org.pmiops.workbench.cdr.dao.DSLinkingDao;
import org.pmiops.workbench.cdr.model.DbDSDataDictionary;
import org.pmiops.workbench.cdr.model.DbDSLinking;
import org.pmiops.workbench.cohortbuilder.CohortBuilderService;
import org.pmiops.workbench.cohortbuilder.CohortQueryBuilder;
import org.pmiops.workbench.cohortbuilder.ParticipantCriteria;
import org.pmiops.workbench.cohortbuilder.QueryParameterUtil;
import org.pmiops.workbench.cohorts.CohortService;
import org.pmiops.workbench.conceptset.ConceptSetService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.dataset.mapper.DataSetMapper;
import org.pmiops.workbench.db.dao.DataSetDao;
import org.pmiops.workbench.db.dao.WgsExtractCromwellSubmissionDao;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbConceptSet;
import org.pmiops.workbench.db.model.DbConceptSetConceptId;
import org.pmiops.workbench.db.model.DbDataset;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.FailedPreconditionException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exceptions.NotImplementedException;
import org.pmiops.workbench.leonardo.LeonardoCustomEnvVarUtils;
import org.pmiops.workbench.model.Cohort;
import org.pmiops.workbench.model.CohortDefinition;
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
import org.pmiops.workbench.model.DomainWithDomainValues;
import org.pmiops.workbench.model.KernelTypeEnum;
import org.pmiops.workbench.model.PrePackagedConceptSetEnum;
import org.pmiops.workbench.model.ResourceType;
import org.pmiops.workbench.model.SearchGroup;
import org.pmiops.workbench.model.SearchGroupItem;
import org.pmiops.workbench.model.SearchParameter;
import org.pmiops.workbench.monitoring.GaugeDataCollector;
import org.pmiops.workbench.monitoring.MeasurementBundle;
import org.pmiops.workbench.monitoring.labels.MetricLabel;
import org.pmiops.workbench.monitoring.views.GaugeMetric;
import org.pmiops.workbench.workspaces.resources.UserRecentResourceService;
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
  private static final String MULTIPLE_DOMAIN_QUERY =
      "SELECT UPPER(domain) \nFROM `${projectId}.${dataSetId}.cb_search_all_events` se \nWHERE concept_id IN "
          + CHILD_LOOKUP_SQL
          + "\nGROUP BY domain";
  private static final String SOURCE_CONCEPT_DOMAIN_QUERY =
      "SELECT DISTINCT concept_id \nFROM `${projectId}.${dataSetId}.cb_search_all_events` \nWHERE concept_id IN "
          + CHILD_LOOKUP_SQL
          + "\nAND UPPER(domain) = %s";
  private static final String LIMIT_20 = " LIMIT 20";
  private static final String PERSON_ID_COLUMN_NAME = "PERSON_ID";
  private static final ImmutableList<Domain> OUTER_QUERY_DOMAIN =
      ImmutableList.of(
          Domain.CONDITION,
          Domain.DEVICE,
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
          Domain.FITBIT_SLEEP_DAILY_SUMMARY,
          Domain.FITBIT_SLEEP_LEVEL,
          Domain.WHOLE_GENOME_VARIANT,
          Domain.ZIP_CODE_SOCIOECONOMIC);

  private static final ImmutableList<Domain> CONCEPT_SET_TYPE_WITH_MULTIPLE_DOMAINS =
      ImmutableList.of(Domain.CONDITION, Domain.PROCEDURE);

  // See https://cloud.google.com/appengine/articles/deadlineexceedederrors for details
  private static final long APP_ENGINE_HARD_TIMEOUT_MSEC_MINUS_FIVE_SEC = 55000L;
  private final ImmutableMap<PrePackagedConceptSetEnum, Long> PRE_PACKAGED_SURVEY_CONCEPT_IDS =
      ImmutableMap.<PrePackagedConceptSetEnum, Long>builder()
          .put(PrePackagedConceptSetEnum.SURVEY_BASICS, 1586134L)
          .put(PrePackagedConceptSetEnum.SURVEY_LIFESTYLE, 1585855L)
          .put(PrePackagedConceptSetEnum.SURVEY_OVERALL_HEALTH, 1585710L)
          .put(PrePackagedConceptSetEnum.SURVEY_HEALTHCARE_ACCESS_UTILIZATION, 43528895L)
          .put(PrePackagedConceptSetEnum.SURVEY_COPE, 1333342L)
          .put(PrePackagedConceptSetEnum.SURVEY_SDOH, 40192389L)
          .put(PrePackagedConceptSetEnum.SURVEY_COVID_VACCINE, 1741006L)
          .put(PrePackagedConceptSetEnum.SURVEY_PFHH, 1740639L)
          .build();

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
  private final CohortBuilderService cohortBuilderService;
  private final CohortService cohortService;
  private final ConceptSetService conceptSetService;
  private final CohortQueryBuilder cohortQueryBuilder;
  private final DataSetDao dataSetDao;
  private final DSLinkingDao dsLinkingDao;
  private final DSDataDictionaryDao dsDataDictionaryDao;
  private final DataSetMapper dataSetMapper;
  private final WgsExtractCromwellSubmissionDao submissionDao;
  private final Provider<String> prefixProvider;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final UserRecentResourceService userRecentResourceService;
  private final Clock clock;
  private final Provider<DbUser> userProvider;

  @Autowired
  @VisibleForTesting
  public DataSetServiceImpl(
      BigQueryService bigQueryService,
      CohortBuilderService cohortBuilderService,
      CohortService cohortService,
      ConceptSetService conceptSetService,
      CohortQueryBuilder cohortQueryBuilder,
      DataSetDao dataSetDao,
      DSLinkingDao dsLinkingDao,
      DSDataDictionaryDao dsDataDictionaryDao,
      DataSetMapper dataSetMapper,
      WgsExtractCromwellSubmissionDao submissionDao,
      @Qualifier(DatasetConfig.DATASET_PREFIX_CODE) Provider<String> prefixProvider,
      UserRecentResourceService userRecentResourceService,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      Clock clock,
      Provider<DbUser> userProvider) {
    this.bigQueryService = bigQueryService;
    this.cohortBuilderService = cohortBuilderService;
    this.cohortService = cohortService;
    this.conceptSetService = conceptSetService;
    this.cohortQueryBuilder = cohortQueryBuilder;
    this.dataSetDao = dataSetDao;
    this.dsLinkingDao = dsLinkingDao;
    this.dsDataDictionaryDao = dsDataDictionaryDao;
    this.dataSetMapper = dataSetMapper;
    this.submissionDao = submissionDao;
    this.prefixProvider = prefixProvider;
    this.userRecentResourceService = userRecentResourceService;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.clock = clock;
    this.userProvider = userProvider;
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
      dataset.setLastModifiedBy(userProvider.get().getUsername());
      dataset = dataSetDao.save(dataset);
      userRecentResourceService.updateDataSetEntry(
          dataset.getWorkspaceId(), dataset.getCreatorId(), dataset.getDataSetId());
      return dataSetMapper.dbModelToClient(dataset);
    } catch (OptimisticLockException e) {
      throw new ConflictException("Failed due to concurrent concept set modification");
    } catch (DataIntegrityViolationException ex) {
      throw new ConflictException("Data set with the same name already exists");
    }
  }

  private Supplier<NotFoundException> noDataSetFound(long dataSetId, long workspaceId) {
    return () ->
        new NotFoundException(
            "No DataSet found for dataSetId " + dataSetId + " and workspaceId " + workspaceId);
  }

  @Override
  public DataSet updateDataSet(long workspaceId, long dataSetId, DataSetRequest request) {
    DbDataset dbDataSet =
        dataSetDao
            .findByDataSetIdAndWorkspaceId(dataSetId, workspaceId)
            .orElseThrow(noDataSetFound(dataSetId, workspaceId));

    if (dbDataSet.getVersion() != Etags.toVersion(request.getEtag())) {
      throw new ConflictException("Attempted to modify outdated data set version");
    }

    return saveDataSet(dataSetMapper.dataSetRequestToDb(request, dbDataSet, clock));
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
          .put(Domain.FITBIT_HEART_RATE_LEVEL, "heart_rate_minute_level")
          .put(Domain.FITBIT_INTRADAY_STEPS, "steps_intraday")
          .put(Domain.FITBIT_ACTIVITY, "activity_summary")
          .put(Domain.FITBIT_HEART_RATE_SUMMARY, "heart_rate_summary")
          .put(Domain.ZIP_CODE_SOCIOECONOMIC, "observation")
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
      Set<DbConceptSetConceptId> dbConceptSetConceptIds = new HashSet<>();
      List<Long> dbCriteriaAnswerIds = new ArrayList<>();
      switch (domain) {
        case SURVEY:
          if (!isPrepackagedAllSurveys(request)) {
            dbConceptSetConceptIds.addAll(
                findDomainConceptIds(request.getDomain(), request.getConceptSetIds()));
            List<Long> prePackagedSurveyConceptIds =
                request.getPrePackagedConceptSet().stream()
                    .map(PRE_PACKAGED_SURVEY_CONCEPT_IDS::get)
                    .collect(Collectors.toList());

            // add selected prePackaged survey question concept ids
            if (!prePackagedSurveyConceptIds.isEmpty()) {
              dbConceptSetConceptIds.addAll(
                  findSurveyQuestionConceptIds(prePackagedSurveyConceptIds));
            }
          }
          List<Long> questionConceptIds =
              dbConceptSetConceptIds.stream()
                  .map(DbConceptSetConceptId::getConceptId)
                  .collect(Collectors.toList());

          // find any questions that belong to PFHH survey. The PFHH survey should
          // only use answer ids when looking up participants.
          List<Long> pfhhSurveyQuestionIds = findPFHHSurveyQuestionIds(questionConceptIds);
          if (!pfhhSurveyQuestionIds.isEmpty()) {
            // need to filter out PFHH survey questions for other survey questions
            Set<DbConceptSetConceptId> dbNonPFHHSurveyQuestions =
                dbConceptSetConceptIds.stream()
                    .filter(cid -> !pfhhSurveyQuestionIds.contains(cid.getConceptId()))
                    .collect(Collectors.toSet());
            dbConceptSetConceptIds = dbNonPFHHSurveyQuestions;
            // find all answers for the questions
            dbCriteriaAnswerIds = findPFHHSurveyAnswerIds(pfhhSurveyQuestionIds);
          }
          break;
        default:
          // Get all source concepts and check to see if they cross this domain. Please see:
          // https://precisionmedicineinitiative.atlassian.net/browse/RW-7657
          dbConceptSetConceptIds.addAll(
              findMultipleDomainConceptIds(request.getDomain(), request.getConceptSetIds()));
          break;
      }
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
      if (Domain.SURVEY.equals(request.getDomain()) && !dbCriteriaAnswerIds.isEmpty()) {
        mergedQueryParameterValues.put(
            "answerConceptIds",
            QueryParameterValue.array(dbCriteriaAnswerIds.toArray(new Long[0]), Long.class));
        if (queryBuilder.toString().contains("question_concept_id IN unnest(@sourceConceptIds)")) {
          queryBuilder.append(" OR ");
        }
        queryBuilder.append("answer_concept_id IN unnest(@answerConceptIds)");
      }
      if (queryBuilder.toString().endsWith("WHERE (")) {
        queryBuilder.append(" 1 = 1 ");
      }
      queryBuilder.append(")");
    }

    if (!request.getIncludesAllParticipants()) {
      final ImmutableList<QueryAndParameters> queryMapEntries =
          cohortService.findAllByCohortIdIn(request.getCohortIds()).stream()
              .map(this::getCohortQueryStringAndCollectNamedParameters)
              .collect(ImmutableList.toImmutableList());

      final String unionedCohortQuery =
          queryMapEntries.stream()
              .map(QueryAndParameters::getQuery)
              .collect(Collectors.joining(" UNION DISTINCT "));
      queryBuilder.append(
          supportsConceptSets(domain)
              ? " AND person_id IN (" + unionedCohortQuery + ")"
              : " WHERE person_id IN (" + unionedCohortQuery + ")");

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
    return bigQueryService.executeQuery(
        bigQueryService.filterBigQueryConfig(previewBigQueryJobConfig),
        APP_ENGINE_HARD_TIMEOUT_MSEC_MINUS_FIVE_SEC);
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
        Boolean.TRUE.equals(dbDataset.getIncludesAllParticipants());
    final ImmutableList<DbCohort> cohortsSelected =
        ImmutableList.copyOf(cohortService.findAllByCohortIdIn(dbDataset.getCohortIds()));
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
        ImmutableList.copyOf(conceptSetService.findAllByConceptSetIdIn(conceptSetIds));
    final boolean noCohortsIncluded = selectedCohorts.isEmpty() && !includesAllParticipants;
    if (noCohortsIncluded
        || hasNoConcepts(prePackagedConceptSet, domainValuePairs, initialSelectedConceptSets)) {
      throw new BadRequestException("Data Sets must include at least one cohort and concept.");
    }

    final ImmutableList.Builder<DbConceptSet> selectedConceptSetsBuilder = ImmutableList.builder();
    selectedConceptSetsBuilder.addAll(initialSelectedConceptSets);

    if (prePackagedConceptSet.stream().anyMatch(PRE_PACKAGED_SURVEY_CONCEPT_IDS::containsKey)
        || prePackagedConceptSet.contains(PrePackagedConceptSetEnum.BOTH)) {
      selectedConceptSetsBuilder.addAll(buildPrePackagedSurveyConceptSets(prePackagedConceptSet));
    } else {
      // If pre packaged all survey concept set is selected create a temp concept set with concept
      // ids of all survey questions
      if (prePackagedConceptSet.contains(SURVEY)
          || prePackagedConceptSet.contains(PrePackagedConceptSetEnum.BOTH)) {
        selectedConceptSetsBuilder.add(buildPrePackagedAllSurveyConceptSet());
      }
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
    String dbCohortDescription = cohortDbModel.getCriteria();
    if (dbCohortDescription == null) {
      throw new NotFoundException(
          String.format(
              "Not Found: No Cohort definition matching cohortId: %s",
              cohortDbModel.getCohortId()));
    }
    final CohortDefinition cohortDefinition =
        new Gson().fromJson(dbCohortDescription, CohortDefinition.class);
    final QueryJobConfiguration participantIdQuery =
        cohortQueryBuilder.buildParticipantIdQuery(new ParticipantCriteria(cohortDefinition));
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
          .append(" FROM ( SELECT * ")
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
        if (queryBuilder.toString().contains("WHERE")) {
          queryBuilder.append(" \nAND (");
        } else {
          queryBuilder.append(" \nWHERE (");
        }
        queryBuilder.append(personIdQualified).append(" IN (").append(cohortQueries).append("))");
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
      queryBuilder.append("\nGROUP BY person_id");
      if (valuesLinkingPair.getSelects().stream().filter(select -> select.contains("DATE")).count()
          == 1) {
        queryBuilder.append(", date");
      }
    }

    if (domain == Domain.ZIP_CODE_SOCIOECONOMIC) {
      queryBuilder.append(
          "\nAND observation_source_concept_id = 1585250"
              + "\nAND observation.value_as_string NOT LIKE 'Res%'");
    }

    if (OUTER_QUERY_DOMAIN.contains(domain)) {
      queryBuilder.append(") ").append(valuesLinkingPair.getTableAlias());
      if (!valuesLinkingPair.formatJoins().equals("")) {
        queryBuilder.append(" ").append(valuesLinkingPair.formatJoins());
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
    return DOMAIN_WITHOUT_CONCEPT_SETS.stream().noneMatch(domain::equals);
  }

  // Gather all the concept IDs from the ConceptSets provided, taking account of
  // domain-specific rules.
  private Optional<String> buildConceptIdSqlInClause(
      Domain domain, List<DbConceptSet> conceptSets) {
    final List<DbConceptSet> dbConceptSets =
        conceptSets.stream()
            .filter(cs -> cs.getDomainEnum().equals(domain))
            .collect(Collectors.toList());
    final List<Long> dbConceptSetIds =
        conceptSets.stream().map(DbConceptSet::getConceptSetId).collect(Collectors.toList());
    Set<DbConceptSetConceptId> dbConceptSetConceptIds = new HashSet<>();
    List<Long> surveyConceptIds = new ArrayList<>();
    List<Long> dbCriteriaAnswerIds = new ArrayList<>();
    if (domain.equals(Domain.SURVEY)) {
      if (prePackagedAllSurveyConceptSet(dbConceptSets)) {
        return Optional.empty();
      }
      if (userSurveyConceptSet(dbConceptSets)) {
        dbConceptSetConceptIds.addAll(findDomainConceptIds(domain, dbConceptSetIds));
      }
      // handle prepackaged PFHH
      if (prePackagedPfhhSurveyConceptSet(dbConceptSets)) {
        dbConceptSetConceptIds.addAll(
            findSurveyQuestionConceptIds(
                ImmutableList.of(
                    PRE_PACKAGED_SURVEY_CONCEPT_IDS.get(PrePackagedConceptSetEnum.SURVEY_PFHH))));
      }
      // Resolve PFHH question_concept_ids to answer_concept_ids
      List<Long> questionConceptIds =
          dbConceptSetConceptIds.stream()
              .map(DbConceptSetConceptId::getConceptId)
              .collect(Collectors.toList());
      // find any questions that belong to PFHH survey. The PFHH survey should
      // only use answer ids when looking up participants.
      List<Long> pfhhSurveyQuestionIds = findPFHHSurveyQuestionIds(questionConceptIds);
      if (!pfhhSurveyQuestionIds.isEmpty()) {
        // need to filter out PFHH survey questions for other survey questions
        Set<DbConceptSetConceptId> dbNonPFHHSurveyQuestions =
            dbConceptSetConceptIds.stream()
                .filter(cid -> !pfhhSurveyQuestionIds.contains(cid.getConceptId()))
                .collect(Collectors.toSet());
        dbConceptSetConceptIds = dbNonPFHHSurveyQuestions;
        // find all answers for the questions
        dbCriteriaAnswerIds = findPFHHSurveyAnswerIds(pfhhSurveyQuestionIds);
      }
      if (prePackagedSurveyConceptSet(dbConceptSets)) {
        surveyConceptIds.addAll(
            dbConceptSets.stream()
                .filter(d -> d.getConceptSetId() == 0 && Domain.SURVEY.equals(d.getDomainEnum()))
                .filter(d -> !d.getName().equals(PrePackagedConceptSetEnum.SURVEY_PFHH.toString()))
                .map(
                    d ->
                        PRE_PACKAGED_SURVEY_CONCEPT_IDS.get(
                            PrePackagedConceptSetEnum.valueOf(d.getName())))
                .collect(Collectors.toList()));
      }
    } else {
      dbConceptSetConceptIds.addAll(findMultipleDomainConceptIds(domain, dbConceptSetIds));
    }

    if (dbConceptSetConceptIds.isEmpty()
        && dbCriteriaAnswerIds.isEmpty()
        && surveyConceptIds.isEmpty()) {
      return Optional.empty();
    } else {
      StringBuilder queryBuilder = new StringBuilder();
      Map<Boolean, List<DbConceptSetConceptId>> partitionSourceAndStandard =
          dbConceptSetConceptIds.stream()
              .collect(Collectors.partitioningBy(DbConceptSetConceptId::getStandard));
      String standardConceptIds =
          partitionSourceAndStandard.get(true).stream()
              .map(c -> c.getConceptId().toString())
              .sorted()
              .collect(Collectors.joining(", "));
      String sourceConceptIds =
          partitionSourceAndStandard.get(false).stream()
              .map(c -> c.getConceptId().toString())
              .sorted()
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
      if (Domain.SURVEY.equals(domain)) {
        if (!dbCriteriaAnswerIds.isEmpty()) {
          String answerConceptIds =
              dbCriteriaAnswerIds.stream().map(Object::toString).collect(Collectors.joining(","));
          if (queryBuilder.toString().contains("question_concept_id IN (")) {
            queryBuilder.append(" OR ");
          }
          queryBuilder.append(
              "answer_concept_id IN (@answerConceptIds)"
                  .replaceAll("@answerConceptIds", answerConceptIds));
        }
        if (!surveyConceptIds.isEmpty()) {
          if (queryBuilder.toString().contains("question_concept_id IN (")
              || queryBuilder.toString().contains("answer_concept_id IN (")) {
            queryBuilder.append(" OR question_concept_id IN ");
          } else {
            queryBuilder.append("question_concept_id IN ");
          }
          queryBuilder.append(
              QUESTION_LOOKUP_SQL.replaceAll(
                  "@surveyConceptIds",
                  surveyConceptIds.stream()
                      .map(c -> c.toString())
                      .collect(Collectors.joining(","))));
        }
      }
      return Optional.of("(" + queryBuilder + ")");
    }
  }

  private boolean userSurveyConceptSet(List<DbConceptSet> dbConceptSets) {
    return dbConceptSets.stream()
        .anyMatch(c -> c.getConceptSetId() > 0 && Domain.SURVEY.equals(c.getDomainEnum()));
  }

  private boolean prePackagedAllSurveyConceptSet(List<DbConceptSet> dbConceptSets) {
    return dbConceptSets.stream()
        .anyMatch(
            c ->
                c.getConceptSetId() == 0
                    && Domain.SURVEY.equals(c.getDomainEnum())
                    && c.getName().equals(SURVEY.toString()));
  }

  private boolean prePackagedSurveyConceptSet(List<DbConceptSet> dbConceptSets) {
    return dbConceptSets.stream()
        .anyMatch(c -> c.getConceptSetId() == 0 && Domain.SURVEY.equals(c.getDomainEnum()));
  }

  private boolean prePackagedPfhhSurveyConceptSet(List<DbConceptSet> dbConceptSets) {
    return dbConceptSets.stream()
        .anyMatch(
            c ->
                c.getConceptSetId() == 0
                    && Domain.SURVEY.equals(c.getDomainEnum())
                    && c.getName().equals(PrePackagedConceptSetEnum.SURVEY_PFHH.toString()));
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
    if (prePackagedSurveyConceptSet(conceptSetsSelected)) {
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

    return Stream.concat(
            queriesByDomain.entrySet().stream()
                .flatMap(
                    entry ->
                        generateDataframeNotebookCells(
                            entry.getValue(),
                            Domain.fromValue(entry.getKey()),
                            dataSetExportRequest.getDataSetRequest().getName(),
                            dbWorkspace.getCdrVersion().getName(),
                            qualifier,
                            dataSetExportRequest.getKernelType(),
                            bigQueryService.getTableFieldsFromDomain(
                                Domain.fromValue(entry.getKey())))
                            .stream()),
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

  private String generateExtractionManifestPollingCode(String qualifier) {
    return "import os\n"
        + "import subprocess\n\n"
        + "# The extraction workflow outputs a manifest file upon completion.\n"
        + "manifest_file = os.environ['"
        + generateVcfDirEnvName(qualifier)
        + "'] + '/manifest.txt'\n\n"
        + "assert subprocess.run(['gsutil', '-q', 'stat', manifest_file]).returncode == 0, (\n"
        + "  \"!\" * 100 + \"\\n\\n\" +\n"
        + "  \"VCF extraction has not completed.\\n\" +\n"
        + "  \"Please monitor the extraction sidepanel for completion before continuing.\\n\\n\" +\n"
        + "  \"!\" * 100\n"
        + ")\n\n"
        + "print(\"VCF extraction has completed, continuing\")\n";
  }

  private List<String> generateHailCode(
      String qualifier, DataSetExportRequest dataSetExportRequest) {
    final String matrixName = "mt_" + qualifier;

    return ImmutableList.of(
        generateExtractionDirCode(qualifier, dataSetExportRequest),
        generateExtractionManifestPollingCode(qualifier),
        "# Confirm Spark is installed.\n"
            + "try:\n"
            + "    import pyspark\n"
            + "except ModuleNotFoundError:\n"
            + "    print(\"!\" * 100 + \"\\n\\n\"\n"
            + "          \"In the Researcher Workbench, Hail can only be used on a Dataproc cluster.\\n\"\n"
            + "          \"Please use the 'Cloud Analysis Environment' side panel to update your runtime compute type.\\n\\n\" +\n"
            + "          \"!\" * 100)\n"
            + "\n"
            + "# Initialize Hail\n"
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
        generateExtractionManifestPollingCode(qualifier),
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
    return conceptSetService.findAllByConceptSetIdIn(
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
    return cohortService.findAllByCohortIdIn(
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
        DbCohort dbCohort = cohortService.findByCohortId(resourceId).orElse(null);
        if (dbCohort == null || dbCohort.getWorkspaceId() != workspaceId) {
          throw new NotFoundException("Resource does not belong to specified workspace");
        }
        dbDataSets =
            dataSetDao.findDataSetsByCohortIdsAndWorkspaceIdAndInvalid(
                resourceId, workspaceId, false);
        break;
      case CONCEPT_SET:
        DbConceptSet dbConceptSet = conceptSetService.findById(resourceId).orElse(null);
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
    DbDataset dbDataset =
        this.getDbDataSet(workspaceId, dataSetId)
            .orElseThrow(noDataSetFound(dataSetId, workspaceId));
    userRecentResourceService.deleteDataSetEntry(workspaceId, dbDataset.getCreatorId(), dataSetId);
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
    return dataSetMapper.dbDsModelToClient(dbDSDataDictionary);
  }

  private ValuesLinkingPair getValueSelectsAndJoins(List<DomainValuePair> domainValuePairs) {
    final Optional<Domain> domainMaybe =
        domainValuePairs.stream().map(DomainValuePair::getDomain).findFirst();
    if (domainMaybe.isEmpty()) {
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
        dsLinkingDao.findByDomainAndDenormalizedNameInOrderById(
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
                  new CohortDefinition().addIncludesItem(createHasWgsSearchGroup())));
    } else {
      participantCriteriaList =
          cohortService.findAllByCohortIdIn(dataSet.getCohortIds()).stream()
              .map(
                  cohort -> {
                    final CohortDefinition cohortDefinition =
                        new Gson().fromJson(cohort.getCriteria(), CohortDefinition.class);
                    // AND the existing search criteria with participants having genomics data.
                    cohortDefinition.addIncludesItem(createHasWgsSearchGroup());
                    return new ParticipantCriteria(cohortDefinition);
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
  public List<DomainWithDomainValues> getValueListFromDomain(
      Long conceptSetId, String domainValue) {
    Domain domain =
        Domain.PHYSICAL_MEASUREMENT_CSS.equals(Domain.valueOf(domainValue))
            ? Domain.MEASUREMENT
            : Domain.valueOf(domainValue);

    // If the domain is Condition/Procedure and the concept set contains a source concept then
    // it may have multiple domains.
    // Please see: https://precisionmedicineinitiative.atlassian.net/browse/RW-7657
    List<String> domains = findAllDomains(conceptSetId, domain);
    List<DomainWithDomainValues> returnList = new ArrayList<>();
    for (String d : domains) {
      FieldList fieldList = bigQueryService.getTableFieldsFromDomain(Domain.valueOf(d));
      returnList.add(
          new DomainWithDomainValues()
              .domain(d)
              .items(
                  fieldList.stream()
                      .map(field -> new DomainValue().value(field.getName().toLowerCase()))
                      .collect(Collectors.toList())));
    }

    return returnList;
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

  private static List<String> generateDataframeNotebookCells(
      QueryJobConfiguration queryJobConfiguration,
      Domain domain,
      String dataSetName,
      String cdrVersionName,
      String qualifier,
      KernelTypeEnum kernelTypeEnum,
      FieldList fieldList) {

    // Define [namespace]_sql, query parameters (as either [namespace]_query_config
    // or [namespace]_query_parameters), and [namespace]_df variables
    String domainAsString = domain.toString().toLowerCase();
    String namespace = "dataset_" + qualifier + "_" + domainAsString + "_";
    // Comments in R and Python have the same syntax
    String sqlComment =
        String.format(
            "# This query represents dataset \"%s\" for domain \"%s\" and was generated for %s",
            dataSetName, domainAsString, cdrVersionName);

    switch (kernelTypeEnum) {
      case PYTHON:
        return ImmutableList.of(
            "import pandas\n"
                + "import os\n\n"
                + sqlComment
                + "\n"
                + namespace
                + "sql = \"\"\""
                + fillInQueryParams(
                    generateSqlWithEnvironmentVariables(
                        queryJobConfiguration.getQuery(), kernelTypeEnum),
                    queryJobConfiguration.getNamedParameters())
                + "\"\"\"\n\n"
                + namespace
                + "df = pandas.read_gbq(\n"
                + "    "
                + namespace
                + "sql,\n"
                + "    dialect=\"standard\",\n"
                + "    use_bqstorage_api=(\""
                + LeonardoCustomEnvVarUtils.BIGQUERY_STORAGE_API_ENABLED_ENV_KEY
                + "\" in os.environ),\n"
                + "    progress_bar_type=\"tqdm_notebook\")\n\n"
                + namespace
                + "df.head(5)");
      case R:
        // Fix tidyverse read_csv problem. In R notebooks the tidyverse plugin tries
        // to dynamically determine the column types in a csv file. Sometimes it incorrectly
        // determines that a string column is a double/integer. This fix will force any
        // string columns to always be strings, so that merging of csv files won't fail
        // do to incompatible types.
        // https://precisionmedicineinitiative.atlassian.net/browse/DST-1056
        List<String> columns =
            fieldList.stream()
                .filter(
                    field ->
                        field.getType().equals(LegacySQLTypeName.STRING)
                            && StringUtils.containsIgnoreCase(
                                queryJobConfiguration.getQuery(), field.getName()))
                .map(field -> field.getName().toLowerCase() + " = col_character()")
                .collect(Collectors.toList());
        String colTypes =
            columns.isEmpty()
                ? "NULL"
                : "cols(" + columns.stream().collect(Collectors.joining(", ")) + ")";
        String exportName = domainAsString + "_" + qualifier;
        String exportPathVariable = exportName + "_path";
        return ImmutableList.of(
            "library(tidyverse)\nlibrary(bigrquery)\n\n"
                + sqlComment
                + "\n"
                + namespace
                + "sql <- paste(\""
                + fillInQueryParams(
                    generateSqlWithEnvironmentVariables(
                        queryJobConfiguration.getQuery(), kernelTypeEnum),
                    queryJobConfiguration.getNamedParameters())
                + "\", sep=\"\")\n\n"
                + "# Formulate a Cloud Storage destination path for the data exported from BigQuery.\n"
                + "# NOTE: By default data exported multiple times on the same day will overwrite older copies.\n"
                + "#       But data exported on a different days will write to a new location so that historical\n"
                + "#       copies can be kept as the dataset definition is changed.\n"
                + exportPathVariable
                + " <- file.path(\n"
                + "  Sys.getenv(\"WORKSPACE_BUCKET\"),\n"
                + "  \"bq_exports\",\n"
                + "  Sys.getenv(\"OWNER_EMAIL\"),\n"
                + "  strftime(lubridate::now(), \"%Y%m%d\"),  # Comment out this line if you want the export to always overwrite.\n"
                + "  \""
                + exportName
                + "\",\n"
                + "  \""
                + exportName
                + "_*.csv\")\n"
                + "message(str_glue('The data will be written to {"
                + exportPathVariable
                + "}. Use this path when reading ',\n"
                + "                 'the data into your notebooks in the future.'))\n\n"
                + "# Perform the query and export the dataset to Cloud Storage as CSV files.\n"
                + "# NOTE: You only need to run `bq_table_save` once. After that, you can\n"
                + "#       just read data from the CSVs in Cloud Storage.\n"
                + "bq_table_save(\n"
                + "  bq_dataset_query(Sys.getenv(\"WORKSPACE_CDR\"), "
                + namespace
                + "sql, billing = Sys.getenv(\"GOOGLE_PROJECT\")),\n"
                + "  "
                + exportPathVariable
                + ",\n"
                + "  destination_format = \"CSV\")\n\n",
            "# Read the data directly from Cloud Storage into memory.\n"
                + "# NOTE: Alternatively you can `gsutil -m cp {"
                + exportPathVariable
                + "}` to copy these files\n"
                + "#       to the Jupyter disk.\n"
                + "read_bq_export_from_workspace_bucket <- function(export_path) {\n"
                + "  col_types <- "
                + colTypes
                + "\n"
                + "  bind_rows(\n"
                + "    map(system2('gsutil', args = c('ls', export_path), stdout = TRUE, stderr = TRUE),\n"
                + "        function(csv) {\n"
                + "          message(str_glue('Loading {csv}.'))\n"
                + "          chunk <- read_csv(pipe(str_glue('gsutil cat {csv}')), col_types = col_types, show_col_types = FALSE)\n"
                + "          if (is.null(col_types)) {\n"
                + "            col_types <- spec(chunk)\n"
                + "          }\n"
                + "          chunk\n"
                + "        }))\n"
                + "}\n"
                + namespace
                + "df <- read_bq_export_from_workspace_bucket("
                + exportPathVariable
                + ")\n\n"
                + "dim("
                + namespace
                + "df)\n\n"
                + "head("
                + namespace
                + "df, 5)");
      default:
        throw new BadRequestException("Language " + kernelTypeEnum + " not supported.");
    }
  }

  private static <T> List<T> nullableListToEmpty(List<T> nullableList) {
    return Optional.ofNullable(nullableList).orElse(new ArrayList<>());
  }

  private DbConceptSet buildPrePackagedAllSurveyConceptSet() {
    final DbConceptSet surveyConceptSet = new DbConceptSet();
    surveyConceptSet.setName("SURVEY");
    surveyConceptSet.setDomain(DbStorageEnums.domainToStorage(Domain.SURVEY));
    return surveyConceptSet;
  }

  private DbConceptSet createSurveyDbConceptSet(PrePackagedConceptSetEnum surveyEnum) {
    final DbConceptSet surveyConceptSet = new DbConceptSet();
    surveyConceptSet.setName(surveyEnum.toString());
    surveyConceptSet.setDomain(DbStorageEnums.domainToStorage(Domain.SURVEY));
    return surveyConceptSet;
  }

  private List<DbConceptSet> buildPrePackagedSurveyConceptSets(
      List<PrePackagedConceptSetEnum> prePackagedConceptSet) {
    if (prePackagedConceptSet.contains(SURVEY)
        || prePackagedConceptSet.contains(PrePackagedConceptSetEnum.BOTH)) {
      return ImmutableList.of(createSurveyDbConceptSet(SURVEY));
    }
    List<DbConceptSet> returnList = prePackagedConceptSet.stream()
        .filter(d-> PRE_PACKAGED_SURVEY_CONCEPT_IDS.containsKey(d))
        .map(this::createSurveyDbConceptSet)
        .collect(Collectors.toList());
    return returnList;
  }

  /**
   * If the domain is Condition/Procedure and the concept set contains a source concept then it may
   * have multiple domains. Please see: <a
   * href="https://precisionmedicineinitiative.atlassian.net/browse/RW-7657">RW-7657</a>
   */
  @NotNull
  private List<String> findAllDomains(Long conceptSetId, Domain domain) {
    Set<String> domains = new HashSet<>();
    if (CONCEPT_SET_TYPE_WITH_MULTIPLE_DOMAINS.contains(domain)) {
      List<DbConceptSet> dbConceptSetList =
          conceptSetService.findAllByConceptSetIdIn(ImmutableList.of(conceptSetId));
      if (dbConceptSetList.isEmpty()) {
        throw new NotFoundException("No Concept Set found for conceptSetId " + conceptSetId);
      }
      // get all source concepts
      Map<Boolean, List<DbConceptSetConceptId>> partitionSourceAndStandard =
          dbConceptSetList.get(0).getConceptSetConceptIds().stream()
              .collect(Collectors.partitioningBy(DbConceptSetConceptId::getStandard));
      List<DbConceptSetConceptId> source = partitionSourceAndStandard.get(false);

      Long[] sourceConceptIds =
          source.stream().map(DbConceptSetConceptId::getConceptId).toArray(Long[]::new);

      // add query param for source concepts
      Map<String, QueryParameterValue> queryParams = new HashMap<>();
      String conceptIdsParam =
          QueryParameterUtil.addQueryParameterValue(
              queryParams, QueryParameterValue.array(sourceConceptIds, Long.class));
      String sourceParam =
          QueryParameterUtil.addQueryParameterValue(queryParams, QueryParameterValue.int64(0));

      // build query configuration
      QueryJobConfiguration queryJobConfiguration =
          buildQueryJobConfiguration(
              queryParams, String.format(MULTIPLE_DOMAIN_QUERY, conceptIdsParam, sourceParam));

      // get results
      domains =
          Streams.stream(
                  bigQueryService
                      .executeQuery(bigQueryService.filterBigQueryConfig(queryJobConfiguration))
                      .getValues())
              .map(domainId -> domainId.get(0).getValue().toString())
              .collect(Collectors.toSet());

      // add standard domains if they don't already exist
      if (!partitionSourceAndStandard.get(true).isEmpty()) {
        domains.add(domain.toString());
      }
    } else {
      domains.add(domain.toString());
    }
    return new ArrayList<>(domains);
  }

  @NotNull
  private List<DbConceptSetConceptId> findDomainConceptIds(
      Domain domain, List<Long> conceptSetIds) {
    return conceptSetService.findAllByConceptSetIdIn(conceptSetIds).stream()
        .filter(cs -> cs.getDomainEnum().equals(domain))
        .flatMap(cs -> cs.getConceptSetConceptIds().stream())
        .collect(Collectors.toList());
  }

  @NotNull
  private List<Long> findPFHHSurveyQuestionIds(List<Long> conceptIds) {
    return cohortBuilderService.findPFHHSurveyQuestionIds(conceptIds);
  }

  @NotNull
  private List<Long> findPFHHSurveyAnswerIds(List<Long> conceptIds) {
    return cohortBuilderService.findPFHHSurveyAnswerIds(conceptIds);
  }

  @NotNull
  private List<DbConceptSetConceptId> findSurveyQuestionConceptIds(List<Long> surveyConceptIds) {
    // Since we do not save prepackaged concept ids to user concept set,
    // we will convert all prepackaged concept ids to DbConceptSetConceptId objects
    return cohortBuilderService.findSurveyQuestionIds(surveyConceptIds).stream()
        .map(c -> DbConceptSetConceptId.builder().addConceptId(c).addStandard(false).build())
        .collect(Collectors.toList());
  }

  private List<DbConceptSetConceptId> findMultipleDomainConceptIds(
      Domain domain, List<Long> conceptSetIds) {
    List<DbConceptSetConceptId> dbConceptSetConceptIds =
        findDomainConceptIds(domain, conceptSetIds).stream()
            .filter(c -> c.getStandard() == Boolean.TRUE)
            .collect(Collectors.toList());
    List<DbConceptSetConceptId> dbPossibleSourceConceptIds =
        conceptSetService.findAllByConceptSetIdIn(conceptSetIds).stream()
            .flatMap(
                cs ->
                    cs.getConceptSetConceptIds().stream()
                        .filter(c -> c.getStandard() == Boolean.FALSE))
            .collect(Collectors.toList());

    Long[] sourceConceptIds =
        dbPossibleSourceConceptIds.stream()
            .map(DbConceptSetConceptId::getConceptId)
            .toArray(Long[]::new);

    // add query param for source concepts
    Map<String, QueryParameterValue> queryParams = new HashMap<>();
    String conceptIdsParam =
        QueryParameterUtil.addQueryParameterValue(
            queryParams, QueryParameterValue.array(sourceConceptIds, Long.class));
    String domainParam =
        QueryParameterUtil.addQueryParameterValue(
            queryParams, QueryParameterValue.string(domain.toString()));

    // build query configuration
    QueryJobConfiguration queryJobConfiguration =
        buildQueryJobConfiguration(
            queryParams,
            String.format(SOURCE_CONCEPT_DOMAIN_QUERY, conceptIdsParam, 0, domainParam));

    // get results
    List<DbConceptSetConceptId> sourceConceptIdsToAdd =
        Streams.stream(
                bigQueryService
                    .executeQuery(bigQueryService.filterBigQueryConfig(queryJobConfiguration))
                    .getValues())
            .map(
                conceptId ->
                    DbConceptSetConceptId.builder()
                        .addConceptId(conceptId.get(0).getLongValue())
                        .addStandard(Boolean.FALSE)
                        .build())
            .collect(Collectors.toList());

    dbConceptSetConceptIds.addAll(sourceConceptIdsToAdd);
    return dbConceptSetConceptIds;
  }

  private boolean isPrepackagedAllSurveys(DataSetPreviewRequest request) {
    final Domain domain = request.getDomain();
    return domain.equals(Domain.SURVEY) && request.getPrePackagedConceptSet().contains(SURVEY);
  }
}
