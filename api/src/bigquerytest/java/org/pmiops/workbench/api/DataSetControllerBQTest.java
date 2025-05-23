package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.*;
import static org.pmiops.workbench.utils.TestMockFactory.createDefaultCdrVersion;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableResult;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import jakarta.inject.Provider;
import jakarta.validation.constraints.NotNull;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.access.AccessTierService;
import org.pmiops.workbench.actionaudit.auditors.BillingProjectAuditor;
import org.pmiops.workbench.cdr.CdrVersionContext;
import org.pmiops.workbench.cdr.CdrVersionService;
import org.pmiops.workbench.cdr.ConceptBigQueryService;
import org.pmiops.workbench.cdr.dao.CBCriteriaDao;
import org.pmiops.workbench.cdr.dao.DSDataDictionaryDao;
import org.pmiops.workbench.cdr.dao.DSLinkingDao;
import org.pmiops.workbench.cdr.model.DbCriteria;
import org.pmiops.workbench.cdr.model.DbDSLinking;
import org.pmiops.workbench.cohortbuilder.CohortBuilderService;
import org.pmiops.workbench.cohortbuilder.CohortBuilderServiceImpl;
import org.pmiops.workbench.cohortbuilder.CohortQueryBuilder;
import org.pmiops.workbench.cohortbuilder.mapper.CohortBuilderMapperImpl;
import org.pmiops.workbench.cohorts.CohortCloningService;
import org.pmiops.workbench.cohorts.CohortMapperImpl;
import org.pmiops.workbench.cohorts.CohortService;
import org.pmiops.workbench.conceptset.ConceptSetService;
import org.pmiops.workbench.conceptset.mapper.ConceptSetMapperImpl;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.dataset.DataSetService;
import org.pmiops.workbench.dataset.DataSetServiceImpl;
import org.pmiops.workbench.dataset.DatasetConfig;
import org.pmiops.workbench.dataset.mapper.DataSetMapper;
import org.pmiops.workbench.dataset.mapper.DataSetMapperImpl;
import org.pmiops.workbench.db.dao.AccessTierDao;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.ConceptSetDao;
import org.pmiops.workbench.db.dao.DataSetDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WgsExtractCromwellSubmissionDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbConceptSet;
import org.pmiops.workbench.db.model.DbConceptSetConceptId;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.FireCloudServiceImpl;
import org.pmiops.workbench.genomics.GenomicExtractionService;
import org.pmiops.workbench.initialcredits.InitialCreditsService;
import org.pmiops.workbench.lab.notebooks.NotebooksService;
import org.pmiops.workbench.lab.notebooks.NotebooksServiceImpl;
import org.pmiops.workbench.model.AnalysisLanguage;
import org.pmiops.workbench.model.ArchivalStatus;
import org.pmiops.workbench.model.CriteriaSubType;
import org.pmiops.workbench.model.CriteriaType;
import org.pmiops.workbench.model.DataSet;
import org.pmiops.workbench.model.DataSetExportRequest;
import org.pmiops.workbench.model.DataSetPreviewRequest;
import org.pmiops.workbench.model.DataSetPreviewResponse;
import org.pmiops.workbench.model.DataSetPreviewValueList;
import org.pmiops.workbench.model.DataSetRequest;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.DomainValue;
import org.pmiops.workbench.model.DomainValuePair;
import org.pmiops.workbench.model.DomainWithDomainValues;
import org.pmiops.workbench.model.PrePackagedConceptSetEnum;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceAccessLevel;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceResponse;
import org.pmiops.workbench.tanagra.ApiException;
import org.pmiops.workbench.tanagra.api.TanagraApi;
import org.pmiops.workbench.tanagra.model.EntityOutputPreview;
import org.pmiops.workbench.tanagra.model.EntityOutputPreviewList;
import org.pmiops.workbench.tanagra.model.ExportPreviewRequest;
import org.pmiops.workbench.test.CohortDefinitions;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.test.TestBigQueryCdrSchemaConfig;
import org.pmiops.workbench.testconfig.TestJpaConfig;
import org.pmiops.workbench.testconfig.TestWorkbenchConfig;
import org.pmiops.workbench.utils.mappers.AnalysisLanguageMapper;
import org.pmiops.workbench.utils.mappers.AnalysisLanguageMapperImpl;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.utils.mappers.UserMapper;
import org.pmiops.workbench.utils.mappers.WorkspaceMapperImpl;
import org.pmiops.workbench.workspaces.WorkspaceAuthService;
import org.pmiops.workbench.workspaces.resources.UserRecentResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.transaction.annotation.Transactional;

@Import({TestJpaConfig.class, DataSetControllerBQTest.Configuration.class})
public class DataSetControllerBQTest extends BigQueryBaseTest {

  private static final FakeClock CLOCK = new FakeClock(Instant.now(), ZoneId.systemDefault());
  private static final String WORKSPACE_NAMESPACE = "namespace";
  private static final String TANAGRA_WORKSPACE_NAMESPACE = "tanagraNamespace";
  private static final String WORKSPACE_NAME = "name";
  private static final String TANAGRA_WORKSPACE_NAME = "tanagraName";
  private static final String DATASET_NAME = "Arbitrary Dataset v1.0";

  private static DbUser currentUser;

  @Autowired private AccessTierDao accessTierDao;
  @Autowired private AnalysisLanguageMapper analysisLanguageMapper;
  @Autowired private BigQueryService bigQueryService;
  @Autowired private CBCriteriaDao cbCriteriaDao;
  @Autowired private CdrVersionDao cdrVersionDao;
  @Autowired private CdrVersionService cdrVersionService;
  @Autowired private CohortBuilderService cohortBuilderService;
  @Autowired private CohortDao cohortDao;
  @Autowired private CohortQueryBuilder cohortQueryBuilder;
  @Autowired private CohortService cohortService;
  @Autowired private ConceptSetDao conceptSetDao;
  @Autowired private ConceptSetService conceptSetService;
  @Autowired private DSDataDictionaryDao dsDataDictionaryDao;
  @Autowired private DSLinkingDao dsLinkingDao;
  @Autowired private DataSetDao dataSetDao;
  @Autowired private DataSetMapper dataSetMapper;
  private DataSetService dataSetService;
  @Autowired private FireCloudService fireCloudService;
  @Autowired private GenomicExtractionService genomicExtractionService;
  @Autowired private NotebooksService notebooksService;
  @Autowired private Provider<DbUser> userProvider;
  @Autowired private Provider<WorkbenchConfig> workbenchConfigProvider;
  @Autowired private TestWorkbenchConfig testWorkbenchConfig;
  @Autowired private UserRecentResourceService userRecentResourceService;
  @Autowired private WgsExtractCromwellSubmissionDao submissionDao;
  @Autowired private WorkspaceAuthService workspaceAuthService;
  @Autowired private WorkspaceDao workspaceDao;
  private final Provider<TanagraApi> mockTanagraProvider = mock(Provider.class);
  @Autowired UserDao userDao;

  @Autowired
  @Qualifier(DatasetConfig.DATASET_PREFIX_CODE)
  Provider<String> prefixProvider;

  private DataSetController controller;

  private DataSet oneCohortDataSet;
  private DataSet twoCohortDataSet;
  private DataSet conditionProcedureOneCohortDataSet;
  private DataSet allParticipantsConditionsDataSet;
  private DataSet cohort1PersonDataSet;
  private DataSet surveyDataSet;
  private DataSet basicsDataSet;
  private DataSet basicsFitbitDataSet;
  private DataSet pfhhDataSet;
  private DataSet allSurveysButPFHHDataSet;
  private DataSet heartRateLevelDataSet;
  private DbCdrVersion dbCdrVersion;
  private DbCdrVersion tanagraDBCdrVersion;
  private DbCohort dbCohort1;
  private DbCohort dbCohort2;
  private DbCohort dbCohort3;
  private DbConceptSet dbConditionConceptSet;
  private DbConceptSet dbConditionConceptSetForValues;
  private DbConceptSet dbConditionConceptSetForValues2;
  private DbConceptSet dbProcedureConceptSet;
  private DbConceptSet dbMeasurementConceptSet;
  private DbConceptSet dbPFHHConceptSet;
  private DbWorkspace dbWorkspace;
  private DbWorkspace tanagraDBWorkspace;
  private DbDSLinking conditionLinking1;
  private DbDSLinking conditionLinking2;
  private DbDSLinking personLinking1;
  private DbDSLinking personLinking2;
  private DbDSLinking surveyLinking1;
  private DbDSLinking surveyLinking2;
  private DbDSLinking procedureLinking1;
  private DbDSLinking procedureLinking2;
  private DbDSLinking fitbitHeartRateLinking1;
  private DbDSLinking fitbitHeartRateLinking2;

  @TestConfiguration
  @Import({
    AnalysisLanguageMapperImpl.class,
    BigQueryTestService.class,
    CdrVersionService.class,
    CohortBuilderMapperImpl.class,
    CohortBuilderServiceImpl.class,
    CohortMapperImpl.class,
    CohortQueryBuilder.class,
    CohortService.class,
    ConceptBigQueryService.class,
    ConceptSetMapperImpl.class,
    ConceptSetService.class,
    DataSetMapperImpl.class,
    DataSetServiceImpl.class,
    TestBigQueryCdrSchemaConfig.class,
    WorkspaceAuthService.class,
  })
  @MockBean({
    AccessTierService.class,
    BillingProjectAuditor.class,
    CdrVersionService.class,
    CohortCloningService.class,
    CommonMappers.class,
    FireCloudServiceImpl.class,
    InitialCreditsService.class,
    GenomicExtractionService.class,
    NotebooksServiceImpl.class,
    Provider.class,
    UserMapper.class,
    UserRecentResourceService.class,
    WorkspaceMapperImpl.class,
  })
  static class Configuration {
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    DbUser user() {
      return currentUser;
    }

    @Bean
    public Clock clock() {
      return CLOCK;
    }

    @Bean
    @Qualifier(DatasetConfig.DATASET_PREFIX_CODE)
    String prefixCode() {
      return "00000000";
    }
  }

  @Override
  public List<String> getTableNames() {
    return ImmutableList.of(
        "condition_occurrence",
        "procedure_occurrence",
        "concept",
        "cb_search_person",
        "cb_search_all_events",
        "cb_criteria",
        "ds_linking",
        "ds_survey",
        "person",
        "heart_rate_minute_level",
        "ds_activity_summary",
        "ds_heart_rate_minute_level",
        "ds_heart_rate_summary",
        "ds_steps_intraday",
        "ds_condition_occurrence",
        "ds_measurement",
        "ds_person",
        "ds_procedure_occurrence",
        "T_ENT_person");
  }

  @Override
  public String getTestDataDirectory() {
    return CB_DATA;
  }

  @BeforeEach
  public void setUp() {
    dataSetService =
        new DataSetServiceImpl(
            bigQueryService,
            cohortBuilderService,
            cohortService,
            conceptSetService,
            cohortQueryBuilder,
            dataSetDao,
            dsLinkingDao,
            dsDataDictionaryDao,
            dataSetMapper,
            submissionDao,
            prefixProvider,
            userRecentResourceService,
            workbenchConfigProvider,
            CLOCK,
            userProvider,
            mockTanagraProvider);
    controller =
        spy(
            new DataSetController(
                analysisLanguageMapper,
                cdrVersionService,
                dataSetService,
                fireCloudService,
                notebooksService,
                userProvider,
                genomicExtractionService,
                workspaceAuthService,
                workbenchConfigProvider));

    currentUser = createUser();

    RawlsWorkspaceResponse fcResponse = new RawlsWorkspaceResponse();
    fcResponse.setAccessLevel(RawlsWorkspaceAccessLevel.OWNER);
    when(fireCloudService.getWorkspace(WORKSPACE_NAMESPACE, WORKSPACE_NAME))
        .thenReturn(fcResponse)
        .thenReturn(fcResponse);

    dbCdrVersion = createDefaultCdrVersion();
    accessTierDao.save(dbCdrVersion.getAccessTier());
    dbCdrVersion.setBigqueryDataset(testWorkbenchConfig.bigquery.dataSetId);
    dbCdrVersion.setBigqueryProject(testWorkbenchConfig.bigquery.projectId);
    dbCdrVersion.setArchivalStatus(DbStorageEnums.archivalStatusToStorage(ArchivalStatus.LIVE));
    dbCdrVersion.setTanagraEnabled(false);
    dbCdrVersion = cdrVersionDao.save(dbCdrVersion);
    CdrVersionContext.setCdrVersionNoCheckAuthDomain(dbCdrVersion);

    dbWorkspace = new DbWorkspace();
    dbWorkspace.setWorkspaceNamespace(WORKSPACE_NAMESPACE);
    dbWorkspace.setFirecloudName(WORKSPACE_NAME);
    dbWorkspace.setCdrVersion(dbCdrVersion);
    dbWorkspace = workspaceDao.save(dbWorkspace);

    tanagraDBCdrVersion = new DbCdrVersion();
    tanagraDBCdrVersion.setCdrVersionId(2L);
    tanagraDBCdrVersion.setName("1");
    tanagraDBCdrVersion.setBigqueryDataset(testWorkbenchConfig.bigquery.dataSetId);
    tanagraDBCdrVersion.setBigqueryProject(testWorkbenchConfig.bigquery.projectId);
    tanagraDBCdrVersion.setArchivalStatus(
        DbStorageEnums.archivalStatusToStorage(ArchivalStatus.LIVE));
    tanagraDBCdrVersion.setTanagraEnabled(true);
    tanagraDBCdrVersion = cdrVersionDao.save(tanagraDBCdrVersion);

    tanagraDBWorkspace = new DbWorkspace();
    tanagraDBWorkspace.setWorkspaceNamespace(TANAGRA_WORKSPACE_NAMESPACE);
    tanagraDBWorkspace.setFirecloudName(TANAGRA_WORKSPACE_NAME);
    tanagraDBWorkspace.setCdrVersion(tanagraDBCdrVersion);
    tanagraDBWorkspace.setUsesTanagra(true);
    tanagraDBWorkspace = workspaceDao.save(tanagraDBWorkspace);

    dbConditionConceptSet =
        conceptSetDao.save(
            createConceptSet(Domain.CONDITION, dbWorkspace.getWorkspaceId(), 1L, Boolean.FALSE));
    dbConditionConceptSetForValues =
        conceptSetDao.save(
            createConceptSet(
                Domain.CONDITION, dbWorkspace.getWorkspaceId(), 44823922L, Boolean.FALSE));
    dbConditionConceptSetForValues2 =
        conceptSetDao.save(
            createConceptSet(Domain.CONDITION, dbWorkspace.getWorkspaceId(), 6L, Boolean.FALSE));
    dbProcedureConceptSet =
        conceptSetDao.save(
            createConceptSet(Domain.PROCEDURE, dbWorkspace.getWorkspaceId(), 1L, Boolean.FALSE));
    dbMeasurementConceptSet =
        conceptSetDao.save(
            createConceptSet(Domain.MEASUREMENT, dbWorkspace.getWorkspaceId(), 3L, Boolean.TRUE));
    dbPFHHConceptSet =
        conceptSetDao.save(
            createConceptSet(
                Domain.SURVEY, dbWorkspace.getWorkspaceId(), 43530446L, Boolean.FALSE));

    // adding pfhh survey module
    DbCriteria pfhhSurveyModule =
        cbCriteriaDao.save(
            DbCriteria.builder()
                .addDomainId(Domain.SURVEY.toString())
                .addType(CriteriaType.PPI.toString())
                .addSubtype(CriteriaSubType.SURVEY.toString())
                .addGroup(true)
                .addConceptId("1740639")
                .addStandard(false)
                .addSelectable(true)
                .addName("Personal and Family Health History")
                .build());
    // adding pfhh survey module path
    pfhhSurveyModule.setPath(String.valueOf(pfhhSurveyModule.getId()));
    cbCriteriaDao.save(pfhhSurveyModule);
    // adding pfhh survey question
    DbCriteria pfhhSurveyQuestion =
        cbCriteriaDao.save(
            DbCriteria.builder()
                .addDomainId(Domain.SURVEY.toString())
                .addType(CriteriaType.PPI.toString())
                .addSubtype(CriteriaSubType.QUESTION.toString())
                .addGroup(true)
                .addConceptId("43530446")
                .addStandard(false)
                .addSelectable(true)
                .addName("Question")
                .build());
    // setting the correct path on pfhh question
    pfhhSurveyQuestion.setPath(pfhhSurveyModule.getId() + "." + pfhhSurveyQuestion.getId());
    pfhhSurveyQuestion = cbCriteriaDao.save(pfhhSurveyQuestion);
    // adding a pfhh survey answer
    DbCriteria pfhhSurveyAnswer =
        cbCriteriaDao.save(
            DbCriteria.builder()
                .addDomainId(Domain.SURVEY.toString())
                .addType(CriteriaType.PPI.toString())
                .addSubtype(CriteriaSubType.ANSWER.toString())
                .addGroup(false)
                .addConceptId("43530446")
                .addStandard(false)
                .addSelectable(true)
                .addName("Answer")
                .addValue("1385558")
                .build());
    // setting the correct path on pfhh answer
    pfhhSurveyAnswer.setPath(
        pfhhSurveyModule.getId()
            + "."
            + pfhhSurveyQuestion.getId()
            + "."
            + pfhhSurveyAnswer.getId());
    cbCriteriaDao.save(pfhhSurveyAnswer);

    dbCohort1 = new DbCohort();
    dbCohort1.setWorkspaceId(dbWorkspace.getWorkspaceId());
    dbCohort1.setCriteria(new Gson().toJson(CohortDefinitions.icd9CodeWithModifiers()));
    dbCohort1 = cohortDao.save(dbCohort1);

    dbCohort2 = new DbCohort();
    dbCohort2.setWorkspaceId(dbWorkspace.getWorkspaceId());
    dbCohort2.setCriteria(new Gson().toJson(CohortDefinitions.icd9Codes()));
    dbCohort2 = cohortDao.save(dbCohort2);

    dbCohort3 = new DbCohort();
    dbCohort3.setWorkspaceId(dbWorkspace.getWorkspaceId());
    dbCohort3.setCriteria(new Gson().toJson(CohortDefinitions.conditionPreviewCodes()));
    dbCohort3 = cohortDao.save(dbCohort3);

    DataSetRequest oneCohortDataSetRequest =
        new DataSetRequest()
            .name(DATASET_NAME)
            .conceptSetIds(ImmutableList.of(dbConditionConceptSet.getConceptSetId()))
            .cohortIds(ImmutableList.of(dbCohort1.getCohortId()))
            .includesAllParticipants(false)
            .prePackagedConceptSet(ImmutableList.of(PrePackagedConceptSetEnum.NONE))
            .domainValuePairs(
                ImmutableList.of(Domain.CONDITION).stream()
                    .map(d -> new DomainValuePair().domain(d).value("person_id"))
                    .collect(Collectors.toList()));
    oneCohortDataSet =
        controller
            .createDataSet(WORKSPACE_NAMESPACE, WORKSPACE_NAME, oneCohortDataSetRequest)
            .getBody();

    DataSetRequest twoCohortDataSetRequest =
        new DataSetRequest()
            .name(DATASET_NAME)
            .conceptSetIds(
                ImmutableList.of(
                    dbConditionConceptSet.getConceptSetId(),
                    dbProcedureConceptSet.getConceptSetId()))
            .cohortIds(ImmutableList.of(dbCohort1.getCohortId()))
            .includesAllParticipants(false)
            .prePackagedConceptSet(ImmutableList.of(PrePackagedConceptSetEnum.NONE))
            .domainValuePairs(
                ImmutableList.of(Domain.CONDITION).stream()
                    .map(d -> new DomainValuePair().domain(d).value("person_id"))
                    .collect(Collectors.toList()));
    twoCohortDataSet =
        controller
            .createDataSet(WORKSPACE_NAMESPACE, WORKSPACE_NAME, twoCohortDataSetRequest)
            .getBody();

    DataSetRequest conditionProcedureOneCohortDataSetRequest =
        new DataSetRequest()
            .name(DATASET_NAME)
            .conceptSetIds(ImmutableList.of(dbConditionConceptSet.getConceptSetId()))
            .cohortIds(ImmutableList.of(dbCohort1.getCohortId(), dbCohort2.getCohortId()))
            .includesAllParticipants(false)
            .prePackagedConceptSet(ImmutableList.of(PrePackagedConceptSetEnum.NONE))
            .domainValuePairs(
                ImmutableList.of(Domain.CONDITION, Domain.PROCEDURE).stream()
                    .map(d -> new DomainValuePair().domain(d).value("person_id"))
                    .collect(Collectors.toList()));
    conditionProcedureOneCohortDataSet =
        controller
            .createDataSet(
                WORKSPACE_NAMESPACE, WORKSPACE_NAME, conditionProcedureOneCohortDataSetRequest)
            .getBody();

    DataSetRequest allParticipantsConditionsDataSetRequest =
        new DataSetRequest()
            .name(DATASET_NAME)
            .conceptSetIds(ImmutableList.of(dbConditionConceptSet.getConceptSetId()))
            .cohortIds(ImmutableList.of())
            .includesAllParticipants(true)
            .prePackagedConceptSet(ImmutableList.of(PrePackagedConceptSetEnum.NONE))
            .domainValuePairs(
                ImmutableList.of(Domain.CONDITION).stream()
                    .map(d -> new DomainValuePair().domain(d).value("person_id"))
                    .collect(Collectors.toList()));
    allParticipantsConditionsDataSet =
        controller
            .createDataSet(
                WORKSPACE_NAMESPACE, WORKSPACE_NAME, allParticipantsConditionsDataSetRequest)
            .getBody();

    DataSetRequest cohort1PersonDataSetRequest =
        new DataSetRequest()
            .name(DATASET_NAME)
            .conceptSetIds(ImmutableList.of())
            .cohortIds(ImmutableList.of(dbCohort1.getCohortId()))
            .includesAllParticipants(false)
            .prePackagedConceptSet(ImmutableList.of(PrePackagedConceptSetEnum.PERSON))
            .domainValuePairs(
                ImmutableList.of(Domain.PERSON).stream()
                    .map(d -> new DomainValuePair().domain(d).value("person_id"))
                    .collect(Collectors.toList()));
    cohort1PersonDataSet =
        controller
            .createDataSet(WORKSPACE_NAMESPACE, WORKSPACE_NAME, cohort1PersonDataSetRequest)
            .getBody();

    DataSetRequest surveyDataSetRequest =
        new DataSetRequest()
            .name(DATASET_NAME)
            .conceptSetIds(ImmutableList.of())
            .cohortIds(ImmutableList.of(dbCohort1.getCohortId()))
            .includesAllParticipants(false)
            .prePackagedConceptSet(ImmutableList.of(PrePackagedConceptSetEnum.SURVEY))
            .domainValuePairs(
                ImmutableList.of(Domain.SURVEY).stream()
                    .map(d -> new DomainValuePair().domain(d).value("person_id"))
                    .collect(Collectors.toList()));
    surveyDataSet =
        controller
            .createDataSet(WORKSPACE_NAMESPACE, WORKSPACE_NAME, surveyDataSetRequest)
            .getBody();

    DataSetRequest basicsDataSetRequest =
        new DataSetRequest()
            .name(DATASET_NAME)
            .conceptSetIds(ImmutableList.of())
            .cohortIds(ImmutableList.of(dbCohort1.getCohortId()))
            .includesAllParticipants(false)
            .prePackagedConceptSet(ImmutableList.of(PrePackagedConceptSetEnum.SURVEY_BASICS))
            .domainValuePairs(
                ImmutableList.of(Domain.SURVEY).stream()
                    .map(d -> new DomainValuePair().domain(d).value("person_id"))
                    .collect(Collectors.toList()));
    basicsDataSet =
        controller
            .createDataSet(WORKSPACE_NAMESPACE, WORKSPACE_NAME, basicsDataSetRequest)
            .getBody();

    DataSetRequest basicsFitbitDataSetRequest =
        new DataSetRequest()
            .name(DATASET_NAME)
            .conceptSetIds(ImmutableList.of())
            .cohortIds(ImmutableList.of())
            .includesAllParticipants(true)
            .prePackagedConceptSet(
                ImmutableList.of(
                    PrePackagedConceptSetEnum.SURVEY_BASICS,
                    PrePackagedConceptSetEnum.FITBIT_HEART_RATE_LEVEL))
            .domainValuePairs(
                ImmutableList.of(Domain.SURVEY, Domain.FITBIT_HEART_RATE_LEVEL).stream()
                    .map(d -> new DomainValuePair().domain(d).value("person_id"))
                    .collect(Collectors.toList()));
    basicsFitbitDataSet =
        controller
            .createDataSet(WORKSPACE_NAMESPACE, WORKSPACE_NAME, basicsFitbitDataSetRequest)
            .getBody();

    DataSetRequest pfhhDataSetRequest =
        new DataSetRequest()
            .name(DATASET_NAME)
            .conceptSetIds(ImmutableList.of(dbPFHHConceptSet.getConceptSetId()))
            .cohortIds(ImmutableList.of(dbCohort3.getCohortId()))
            .includesAllParticipants(false)
            .prePackagedConceptSet(ImmutableList.of(PrePackagedConceptSetEnum.NONE))
            .domainValuePairs(
                ImmutableList.of(Domain.SURVEY).stream()
                    .map(d -> new DomainValuePair().domain(d).value("person_id"))
                    .collect(Collectors.toList()));
    pfhhDataSet =
        controller.createDataSet(WORKSPACE_NAMESPACE, WORKSPACE_NAME, pfhhDataSetRequest).getBody();

    DataSetRequest allSurveysButPFHHDataSetRequest =
        new DataSetRequest()
            .name(DATASET_NAME)
            .conceptSetIds(ImmutableList.of())
            .cohortIds(ImmutableList.of(dbCohort1.getCohortId()))
            .includesAllParticipants(false)
            .prePackagedConceptSet(
                ImmutableList.of(
                    PrePackagedConceptSetEnum.SURVEY_BASICS,
                    PrePackagedConceptSetEnum.SURVEY_LIFESTYLE,
                    PrePackagedConceptSetEnum.SURVEY_OVERALL_HEALTH,
                    PrePackagedConceptSetEnum.SURVEY_HEALTHCARE_ACCESS_UTILIZATION,
                    PrePackagedConceptSetEnum.SURVEY_COPE,
                    PrePackagedConceptSetEnum.SURVEY_SDOH,
                    PrePackagedConceptSetEnum.SURVEY_COVID_VACCINE))
            .domainValuePairs(
                ImmutableList.of(Domain.SURVEY).stream()
                    .map(d -> new DomainValuePair().domain(d).value("person_id"))
                    .collect(Collectors.toList()));
    allSurveysButPFHHDataSet =
        controller
            .createDataSet(WORKSPACE_NAMESPACE, WORKSPACE_NAME, allSurveysButPFHHDataSetRequest)
            .getBody();

    DataSetRequest heartRateLevelDataSetRequest =
        new DataSetRequest()
            .name(DATASET_NAME)
            .conceptSetIds(ImmutableList.of())
            .cohortIds(ImmutableList.of(dbCohort1.getCohortId()))
            .includesAllParticipants(false)
            .prePackagedConceptSet(
                ImmutableList.of(PrePackagedConceptSetEnum.FITBIT_HEART_RATE_LEVEL))
            .domainValuePairs(
                ImmutableList.of(Domain.FITBIT_HEART_RATE_LEVEL).stream()
                    .map(d -> new DomainValuePair().domain(d).value("person_id"))
                    .collect(Collectors.toList()));
    heartRateLevelDataSet =
        controller
            .createDataSet(WORKSPACE_NAMESPACE, WORKSPACE_NAME, heartRateLevelDataSetRequest)
            .getBody();

    conditionLinking1 =
        DbDSLinking.builder()
            .addDenormalizedName("CORE_TABLE_FOR_DOMAIN")
            .addOmopSql("CORE_TABLE_FOR_DOMAIN")
            .addJoinValue("from `${projectId}.${dataSetId}.condition_occurrence` c_occurrence")
            .addDomain("Condition")
            .build();
    dsLinkingDao.save(conditionLinking1);
    conditionLinking2 =
        DbDSLinking.builder()
            .addDenormalizedName("PERSON_ID")
            .addOmopSql("c_occurrence.PERSON_ID")
            .addJoinValue("from `${projectId}.${dataSetId}.condition_occurrence` c_occurrence")
            .addDomain("Condition")
            .build();
    dsLinkingDao.save(conditionLinking2);

    personLinking1 =
        DbDSLinking.builder()
            .addDenormalizedName("CORE_TABLE_FOR_DOMAIN")
            .addOmopSql("CORE_TABLE_FOR_DOMAIN")
            .addJoinValue("FROM `${projectId}.${dataSetId}.person` person")
            .addDomain("Person")
            .build();
    dsLinkingDao.save(personLinking1);
    personLinking2 =
        DbDSLinking.builder()
            .addDenormalizedName("PERSON_ID")
            .addOmopSql("person.PERSON_ID")
            .addJoinValue("FROM `${projectId}.${dataSetId}.person` person")
            .addDomain("Person")
            .build();
    dsLinkingDao.save(personLinking2);

    surveyLinking1 =
        DbDSLinking.builder()
            .addDenormalizedName("CORE_TABLE_FOR_DOMAIN")
            .addOmopSql("CORE_TABLE_FOR_DOMAIN")
            .addJoinValue("FROM `${projectId}.${dataSetId}.ds_survey` answer")
            .addDomain("Survey")
            .build();
    dsLinkingDao.save(surveyLinking1);
    surveyLinking2 =
        DbDSLinking.builder()
            .addDenormalizedName("PERSON_ID")
            .addOmopSql("answer.PERSON_ID")
            .addJoinValue("")
            .addDomain("Survey")
            .build();
    dsLinkingDao.save(surveyLinking2);

    procedureLinking1 =
        DbDSLinking.builder()
            .addDenormalizedName("CORE_TABLE_FOR_DOMAIN")
            .addOmopSql("CORE_TABLE_FOR_DOMAIN")
            .addJoinValue("from `${projectId}.${dataSetId}.procedure_occurrence` procedure")
            .addDomain("Procedure")
            .build();
    dsLinkingDao.save(procedureLinking1);
    procedureLinking2 =
        DbDSLinking.builder()
            .addDenormalizedName("PERSON_ID")
            .addOmopSql("procedure.PERSON_ID")
            .addJoinValue("from `${projectId}.${dataSetId}.procedure_occurrence` procedure")
            .addDomain("Procedure")
            .build();
    dsLinkingDao.save(procedureLinking2);

    fitbitHeartRateLinking1 =
        DbDSLinking.builder()
            .addDenormalizedName("PERSON_ID")
            .addOmopSql("heart_rate_minute_level.PERSON_ID")
            .addJoinValue(
                "from `${projectId}.${dataSetId}.heart_rate_minute_level` heart_rate_minute_level")
            .addDomain("Fitbit_heart_rate_level")
            .build();
    dsLinkingDao.save(fitbitHeartRateLinking1);
    fitbitHeartRateLinking2 =
        DbDSLinking.builder()
            .addDenormalizedName("CORE_TABLE_FOR_DOMAIN")
            .addOmopSql("CORE_TABLE_FOR_DOMAIN")
            .addJoinValue(
                "from `${projectId}.${dataSetId}.heart_rate_minute_level` heart_rate_minute_level")
            .addDomain("Fitbit_heart_rate_level")
            .build();
    dsLinkingDao.save(fitbitHeartRateLinking2);
  }

  private DbConceptSet createConceptSet(
      Domain domain, long workspaceId, long conceptId, Boolean isStandard) {
    DbConceptSet dbConceptSet = new DbConceptSet();
    dbConceptSet.setDomain(DbStorageEnums.domainToStorage(domain));
    DbConceptSetConceptId dbConceptSetConceptId =
        DbConceptSetConceptId.builder().addConceptId(conceptId).addStandard(isStandard).build();
    dbConceptSet.setConceptSetConceptIds(
        new HashSet<>(Collections.singletonList(dbConceptSetConceptId)));
    dbConceptSet.setWorkspaceId(workspaceId);
    return dbConceptSet;
  }

  @AfterEach
  public void tearDown() {
    cohortDao.deleteById(dbCohort1.getCohortId());
    cohortDao.deleteById(dbCohort2.getCohortId());
    conceptSetDao.deleteById(dbConditionConceptSet.getConceptSetId());
    conceptSetDao.deleteById(dbConditionConceptSetForValues.getConceptSetId());
    conceptSetDao.deleteById(dbConditionConceptSetForValues2.getConceptSetId());
    conceptSetDao.deleteById(dbProcedureConceptSet.getConceptSetId());
    workspaceDao.deleteById(dbWorkspace.getWorkspaceId());
    workspaceDao.deleteById(tanagraDBWorkspace.getWorkspaceId());
    cdrVersionDao.deleteById(dbCdrVersion.getCdrVersionId());
    cdrVersionDao.deleteById(tanagraDBCdrVersion.getCdrVersionId());
    dsLinkingDao.delete(conditionLinking1);
    dsLinkingDao.delete(conditionLinking2);
    dsLinkingDao.delete(personLinking1);
    dsLinkingDao.delete(personLinking2);
    dsLinkingDao.delete(surveyLinking1);
    dsLinkingDao.delete(surveyLinking2);
    dsLinkingDao.delete(procedureLinking1);
    dsLinkingDao.delete(procedureLinking2);
    dsLinkingDao.delete(fitbitHeartRateLinking1);
    dsLinkingDao.delete(fitbitHeartRateLinking2);
  }

  private String joinCodeCells(List<String> codeCells) {
    return String.join("\n\n", codeCells);
  }

  @Test
  @Transactional
  public void testGenerateCodePython() {
    String code =
        joinCodeCells(
            dataSetService.generateCodeCells(
                new DataSetExportRequest()
                    .analysisLanguage(AnalysisLanguage.PYTHON)
                    .dataSetRequest(
                        createDataSetRequest(
                            oneCohortDataSet.getId(),
                            ImmutableList.of(Domain.CONDITION),
                            false,
                            ImmutableList.of(PrePackagedConceptSetEnum.NONE))),
                dbWorkspace));

    assertAndExecutePythonQuery(code, 1, Domain.CONDITION, 1L);
  }

  @Test
  @Transactional
  public void testGenerateCodePythonTanagra() throws ApiException {
    EntityOutputPreview preview =
        new EntityOutputPreview()
            .entity("person")
            .sourceSql("SELECT person_id FROM `project.dataset`.T_ENT_person");
    EntityOutputPreviewList previewList =
        new EntityOutputPreviewList().addEntityOutputsItem(preview);

    TanagraApi mockTanagraApi = mock(TanagraApi.class);
    when(mockTanagraProvider.get()).thenReturn(mockTanagraApi);
    when(mockTanagraApi.describeExport(any(ExportPreviewRequest.class), anyString()))
        .thenReturn(previewList);
    String code =
        joinCodeCells(
            dataSetService.generateCodeCells(
                new DataSetExportRequest()
                    .analysisLanguage(AnalysisLanguage.PYTHON)
                    .dataSetRequest(
                        createDataSetRequestTanagra(
                            ImmutableList.of(Domain.PERSON),
                            false,
                            ImmutableList.of(PrePackagedConceptSetEnum.NONE))),
                tanagraDBWorkspace));

    String expected =
        String.format(
            "import pandas\n"
                + "import os\n"
                + "\n"
                + "# This query represents dataset \"Arbitrary Dataset v1.0\" for domain \"person\" and was generated for 1\n"
                + "dataset_00000000_person_sql = \"\"\"\n"
                + "    SELECT\n"
                + "        person_id \n"
                + "    FROM\n"
                + "        `project.dataset.T_ENT_person`\"\"\"\n"
                + "\n"
                + "dataset_00000000_person_df = pandas.read_gbq(\n"
                + "    dataset_00000000_person_sql,\n"
                + "    dialect=\"standard\",\n"
                + "    use_bqstorage_api=(\"BIGQUERY_STORAGE_API_ENABLED\" in os.environ),\n"
                + "    progress_bar_type=\"tqdm_notebook\")\n"
                + "\n"
                + "dataset_00000000_person_df.head(5)",
            DATASET_NAME,
            Domain.PERSON.toString().toLowerCase(),
            dbCdrVersion.getName(),
            Domain.PERSON.toString().toLowerCase());
    assertThat(code).isEqualTo(expected);
  }

  @Test
  @Transactional
  public void testGenerateCodeR() {
    String expected =
        String.format(
            "library(bigrquery)\n"
                + "\n# This query represents dataset \"%s\" for domain \"condition\" and was generated for %s\n"
                + "dataset_00000000_condition_sql <- paste(\"",
            DATASET_NAME, dbCdrVersion.getName());

    String code =
        joinCodeCells(
            dataSetService.generateCodeCells(
                new DataSetExportRequest()
                    .analysisLanguage(AnalysisLanguage.R)
                    .dataSetRequest(
                        createDataSetRequest(
                            oneCohortDataSet.getId(),
                            ImmutableList.of(Domain.CONDITION),
                            false,
                            ImmutableList.of(PrePackagedConceptSetEnum.NONE))),
                workspaceDao.getRequired(WORKSPACE_NAMESPACE, WORKSPACE_NAME)));

    assertThat(code).contains(expected);

    String query =
        extractRQuery(
            code,
            "condition_occurrence",
            "cb_search_person",
            "cb_search_all_events",
            "cb_criteria");

    try {
      TableResult result =
          bigQueryService.executeQuery(
              QueryJobConfiguration.newBuilder(query).setUseLegacySql(false).build());
      assertThat(result.getTotalRows()).isEqualTo(1L);
    } catch (Exception e) {
      Assertions.fail(
          "Problem generating BigQuery query for notebooks: " + e.getCause().getMessage());
    }
  }

  @Test
  @Transactional
  public void testGenerateCodeTwoConceptSets() {
    String code =
        joinCodeCells(
            dataSetService.generateCodeCells(
                new DataSetExportRequest()
                    .analysisLanguage(AnalysisLanguage.PYTHON)
                    .dataSetRequest(
                        createDataSetRequest(
                            conditionProcedureOneCohortDataSet.getId(),
                            ImmutableList.of(Domain.CONDITION, Domain.PROCEDURE),
                            false,
                            ImmutableList.of(PrePackagedConceptSetEnum.NONE))),
                workspaceDao.getRequired(WORKSPACE_NAMESPACE, WORKSPACE_NAME)));

    assertAndExecutePythonQuery(code, 3, Domain.CONDITION, 1L);
  }

  @Test
  @Transactional
  public void testGenerateCodeTwoCohorts() {
    String code =
        joinCodeCells(
            dataSetService.generateCodeCells(
                new DataSetExportRequest()
                    .analysisLanguage(AnalysisLanguage.PYTHON)
                    .dataSetRequest(
                        createDataSetRequest(
                            twoCohortDataSet.getId(),
                            ImmutableList.of(Domain.CONDITION),
                            false,
                            ImmutableList.of(PrePackagedConceptSetEnum.NONE))),
                workspaceDao.getRequired(WORKSPACE_NAMESPACE, WORKSPACE_NAME)));

    assertAndExecutePythonQuery(code, 1, Domain.CONDITION, 1L);
  }

  @Test
  @Transactional
  public void testGenerateCodeAllParticipants() {
    String code =
        joinCodeCells(
            dataSetService.generateCodeCells(
                new DataSetExportRequest()
                    .analysisLanguage(AnalysisLanguage.PYTHON)
                    .dataSetRequest(
                        createDataSetRequest(
                            allParticipantsConditionsDataSet.getId(),
                            ImmutableList.of(Domain.CONDITION),
                            true,
                            ImmutableList.of(PrePackagedConceptSetEnum.NONE))),
                workspaceDao.getRequired(WORKSPACE_NAMESPACE, WORKSPACE_NAME)));

    assertAndExecutePythonQuery(code, 1, Domain.CONDITION, 1L);
  }

  @Test
  @Transactional
  public void testGenerateCodePrepackagedCohortDemographics() {
    String code =
        joinCodeCells(
            dataSetService.generateCodeCells(
                new DataSetExportRequest()
                    .analysisLanguage(AnalysisLanguage.PYTHON)
                    .dataSetRequest(
                        createDataSetRequest(
                            cohort1PersonDataSet.getId(),
                            ImmutableList.of(Domain.PERSON),
                            false,
                            ImmutableList.of(PrePackagedConceptSetEnum.PERSON))),
                workspaceDao.getRequired(WORKSPACE_NAMESPACE, WORKSPACE_NAME)));

    assertAndExecutePythonQuery(code, 1, Domain.PERSON, 1L);
  }

  @Test
  @Transactional
  public void testGenerateCodePrepackagedCohortSurveys() {
    String code =
        joinCodeCells(
            dataSetService.generateCodeCells(
                new DataSetExportRequest()
                    .analysisLanguage(AnalysisLanguage.PYTHON)
                    .dataSetRequest(
                        createDataSetRequest(
                            surveyDataSet.getId(),
                            ImmutableList.of(Domain.SURVEY),
                            false,
                            ImmutableList.of(PrePackagedConceptSetEnum.SURVEY))),
                workspaceDao.getRequired(WORKSPACE_NAMESPACE, WORKSPACE_NAME)));

    assertAndExecutePythonQuery(code, 1, Domain.SURVEY, 2L);
  }

  @Test
  @Transactional
  public void testGenerateCodePrepackagedCohortSurveyBasics() {
    final String expectedConceptId = "concept_id IN (1586134)";
    String code =
        joinCodeCells(
            dataSetService.generateCodeCells(
                new DataSetExportRequest()
                    .analysisLanguage(AnalysisLanguage.PYTHON)
                    .dataSetRequest(
                        createDataSetRequest(
                            basicsDataSet.getId(),
                            ImmutableList.of(Domain.SURVEY),
                            false,
                            ImmutableList.of(PrePackagedConceptSetEnum.SURVEY_BASICS))),
                workspaceDao.getRequired(WORKSPACE_NAMESPACE, WORKSPACE_NAME)));

    assertThat(code.replaceAll("[ \\s]", "")).contains(expectedConceptId.replaceAll(" ", ""));
  }

  @Test
  @Transactional
  public void testGenerateCodePrepackagedCohortSurveyBasicsAndFitbitHeartRate() {
    final String expectedConceptId = "concept_id IN (1586134)"; // for survey_basics
    final String expectedFitbitHrLevel = ".heart_rate_minute_level";
    String code =
        joinCodeCells(
            dataSetService.generateCodeCells(
                new DataSetExportRequest()
                    .analysisLanguage(AnalysisLanguage.PYTHON)
                    .dataSetRequest(
                        createDataSetRequest(
                            basicsFitbitDataSet.getId(),
                            ImmutableList.of(Domain.SURVEY, Domain.FITBIT_HEART_RATE_LEVEL),
                            true,
                            ImmutableList.of(
                                PrePackagedConceptSetEnum.SURVEY_BASICS,
                                PrePackagedConceptSetEnum.FITBIT_HEART_RATE_LEVEL))),
                workspaceDao.getRequired(WORKSPACE_NAMESPACE, WORKSPACE_NAME)));

    assertThat(code.replaceAll("[ \\s]", "")).contains(expectedConceptId.replaceAll(" ", ""));
    assertThat(code).contains(expectedFitbitHrLevel);
  }

  @Test
  @Transactional
  public void testGenerateCodePrepackagedCohortAllIndividualSurveysExceptPfhhInOrder() {
    final ImmutableList<PrePackagedConceptSetEnum> prePackagedConceptSetEnumList =
        ImmutableList.of(
            PrePackagedConceptSetEnum.SURVEY_BASICS,
            PrePackagedConceptSetEnum.SURVEY_LIFESTYLE,
            PrePackagedConceptSetEnum.SURVEY_OVERALL_HEALTH,
            PrePackagedConceptSetEnum.SURVEY_HEALTHCARE_ACCESS_UTILIZATION,
            PrePackagedConceptSetEnum.SURVEY_COPE,
            PrePackagedConceptSetEnum.SURVEY_SDOH,
            PrePackagedConceptSetEnum.SURVEY_COVID_VACCINE);

    final String expectedConceptId =
        "concept_id IN (1586134,1585855,1585710,43528895,1333342,40192389,1741006)";
    String code =
        joinCodeCells(
            dataSetService.generateCodeCells(
                new DataSetExportRequest()
                    .analysisLanguage(AnalysisLanguage.PYTHON)
                    .dataSetRequest(
                        createDataSetRequest(
                            allSurveysButPFHHDataSet.getId(),
                            ImmutableList.of(Domain.SURVEY),
                            false,
                            prePackagedConceptSetEnumList)),
                workspaceDao.getRequired(WORKSPACE_NAMESPACE, WORKSPACE_NAME)));

    assertThat(code.replaceAll("[ \\s]", "")).contains(expectedConceptId.replaceAll(" ", ""));
  }

  @Test
  @Transactional
  public void testGenerateCodePrepackagedConceptSetFitBit() {
    String code =
        joinCodeCells(
            dataSetService.generateCodeCells(
                new DataSetExportRequest()
                    .analysisLanguage(AnalysisLanguage.PYTHON)
                    .dataSetRequest(
                        createDataSetRequest(
                            heartRateLevelDataSet.getId(),
                            ImmutableList.of(Domain.FITBIT_HEART_RATE_LEVEL),
                            false,
                            ImmutableList.of(PrePackagedConceptSetEnum.NONE))),
                workspaceDao.getRequired(WORKSPACE_NAMESPACE, WORKSPACE_NAME)));

    assertAndExecutePythonQuery(code, 1, Domain.FITBIT_HEART_RATE_LEVEL, 1L);
  }

  @Test
  @Transactional
  public void testGenerateCodeTwoPrePackagedConceptSet() {
    String code =
        joinCodeCells(
            dataSetService.generateCodeCells(
                new DataSetExportRequest()
                    .analysisLanguage(AnalysisLanguage.PYTHON)
                    .dataSetRequest(
                        createDataSetRequest(
                            twoCohortDataSet.getId(),
                            ImmutableList.of(Domain.CONDITION),
                            false,
                            ImmutableList.of(
                                PrePackagedConceptSetEnum.PERSON,
                                PrePackagedConceptSetEnum.FITBIT_HEART_RATE_LEVEL))),
                workspaceDao.getRequired(WORKSPACE_NAMESPACE, WORKSPACE_NAME)));

    assertAndExecutePythonQuery(code, 1, Domain.CONDITION, 1L);
  }

  @Test
  @Transactional
  public void testGenerateCodePFHHSurveyConceptSet() {
    String code =
        joinCodeCells(
            dataSetService.generateCodeCells(
                new DataSetExportRequest()
                    .analysisLanguage(AnalysisLanguage.PYTHON)
                    .dataSetRequest(
                        createDataSetRequest(
                            pfhhDataSet.getId(),
                            ImmutableList.of(Domain.SURVEY),
                            false,
                            ImmutableList.of())),
                workspaceDao.getRequired(WORKSPACE_NAMESPACE, WORKSPACE_NAME)));

    assertAndExecutePythonQuery(code, 1, Domain.SURVEY, 1L);
  }

  @Test
  public void getValuesFromDomainCondition() {
    List<DomainWithDomainValues> domainWithDomainValues =
        Objects.requireNonNull(
                controller
                    .getValuesFromDomain(
                        WORKSPACE_NAMESPACE,
                        WORKSPACE_NAME,
                        Domain.CONDITION.toString(),
                        dbConditionConceptSetForValues2.getConceptSetId())
                    .getBody())
            .getItems();
    assertThat(
            domainWithDomainValues.containsAll(
                ImmutableList.of(
                    new DomainWithDomainValues()
                        .domain(Domain.CONDITION.toString())
                        .items(
                            ImmutableList.of(
                                new DomainValue().value("person_id"),
                                new DomainValue().value("condition_concept_id"),
                                new DomainValue().value("standard_concept_name"),
                                new DomainValue().value("standard_concept_code"),
                                new DomainValue().value("standard_vocabulary"),
                                new DomainValue().value("condition_start_datetime"),
                                new DomainValue().value("condition_end_datetime"),
                                new DomainValue().value("condition_type_concept_id"),
                                new DomainValue().value("condition_type_concept_name"),
                                new DomainValue().value("stop_reason"),
                                new DomainValue().value("visit_occurrence_id"),
                                new DomainValue().value("visit_occurrence_concept_name"),
                                new DomainValue().value("condition_source_value"),
                                new DomainValue().value("condition_source_concept_id"),
                                new DomainValue().value("source_concept_name"),
                                new DomainValue().value("source_concept_code"),
                                new DomainValue().value("source_vocabulary"),
                                new DomainValue().value("condition_status_source_value"),
                                new DomainValue().value("condition_status_concept_id"),
                                new DomainValue().value("condition_status_concept_name"))),
                    new DomainWithDomainValues()
                        .domain(Domain.MEASUREMENT.toString())
                        .items(
                            ImmutableList.of(
                                new DomainValue().value("person_id"),
                                new DomainValue().value("measurement_concept_id"),
                                new DomainValue().value("standard_concept_name"),
                                new DomainValue().value("standard_concept_code"),
                                new DomainValue().value("standard_vocabulary"),
                                new DomainValue().value("measurement_datetime"),
                                new DomainValue().value("measurement_type_concept_id"),
                                new DomainValue().value("measurement_type_concept_name"),
                                new DomainValue().value("operator_concept_id"),
                                new DomainValue().value("operator_concept_name"),
                                new DomainValue().value("value_as_number"),
                                new DomainValue().value("value_as_concept_id"),
                                new DomainValue().value("value_as_concept_name"),
                                new DomainValue().value("unit_concept_id"),
                                new DomainValue().value("unit_concept_name"),
                                new DomainValue().value("range_low"),
                                new DomainValue().value("range_high"),
                                new DomainValue().value("visit_occurrence_id"),
                                new DomainValue().value("visit_occurrence_concept_name"),
                                new DomainValue().value("measurement_source_value"),
                                new DomainValue().value("measurement_source_concept_id"),
                                new DomainValue().value("source_concept_name"),
                                new DomainValue().value("source_concept_code"),
                                new DomainValue().value("source_vocabulary"),
                                new DomainValue().value("unit_source_value"),
                                new DomainValue().value("value_source_value"))))))
        .isEqualTo(true);
  }

  @Test
  public void getValuesFromDomainActivitySummary() {
    List<DomainWithDomainValues> domainWithDomainValues =
        Objects.requireNonNull(
                controller
                    .getValuesFromDomain(
                        WORKSPACE_NAMESPACE, WORKSPACE_NAME, Domain.FITBIT_ACTIVITY.toString(), 1L)
                    .getBody())
            .getItems();
    assertThat(
            domainWithDomainValues.containsAll(
                ImmutableList.of(
                    new DomainWithDomainValues()
                        .domain(Domain.FITBIT_ACTIVITY.toString())
                        .items(
                            ImmutableList.of(
                                new DomainValue().value("date"),
                                new DomainValue().value("activity_calories"),
                                new DomainValue().value("calories_bmr"),
                                new DomainValue().value("calories_out"),
                                new DomainValue().value("elevation"),
                                new DomainValue().value("fairly_active_minutes"),
                                new DomainValue().value("floors"),
                                new DomainValue().value("lightly_active_minutes"),
                                new DomainValue().value("marginal_calories"),
                                new DomainValue().value("sedentary_minutes"),
                                new DomainValue().value("steps"),
                                new DomainValue().value("very_active_minutes"),
                                new DomainValue().value("person_id"))))))
        .isEqualTo(true);
  }

  @Test
  public void getValuesFromDomainHeartRateLevel() {
    List<DomainWithDomainValues> domainWithDomainValues =
        Objects.requireNonNull(
                controller
                    .getValuesFromDomain(
                        WORKSPACE_NAMESPACE,
                        WORKSPACE_NAME,
                        Domain.FITBIT_HEART_RATE_LEVEL.toString(),
                        1L)
                    .getBody())
            .getItems();

    assertThat(
            domainWithDomainValues.containsAll(
                ImmutableList.of(
                    new DomainWithDomainValues()
                        .domain(Domain.FITBIT_HEART_RATE_LEVEL.toString())
                        .items(
                            ImmutableList.of(
                                new DomainValue().value("datetime"),
                                new DomainValue().value("person_id"),
                                new DomainValue().value("heart_rate_value"))))))
        .isEqualTo(true);
  }

  @Test
  public void getValuesFromDomainHeartRateSummary() {
    List<DomainWithDomainValues> domainWithDomainValues =
        Objects.requireNonNull(
                controller
                    .getValuesFromDomain(
                        WORKSPACE_NAMESPACE,
                        WORKSPACE_NAME,
                        Domain.FITBIT_HEART_RATE_SUMMARY.toString(),
                        1L)
                    .getBody())
            .getItems();

    assertThat(
            domainWithDomainValues.containsAll(
                ImmutableList.of(
                    new DomainWithDomainValues()
                        .domain(Domain.FITBIT_HEART_RATE_SUMMARY.toString())
                        .items(
                            ImmutableList.of(
                                new DomainValue().value("person_id"),
                                new DomainValue().value("date"),
                                new DomainValue().value("zone_name"),
                                new DomainValue().value("min_heart_rate"),
                                new DomainValue().value("max_heart_rate"),
                                new DomainValue().value("minute_in_zone"),
                                new DomainValue().value("calorie_count"))))))
        .isEqualTo(true);
  }

  @Test
  public void getValuesFromDomainStepsIntraday() {
    List<DomainWithDomainValues> domainWithDomainValues =
        Objects.requireNonNull(
                controller
                    .getValuesFromDomain(
                        WORKSPACE_NAMESPACE,
                        WORKSPACE_NAME,
                        Domain.FITBIT_INTRADAY_STEPS.toString(),
                        1L)
                    .getBody())
            .getItems();

    assertThat(
            domainWithDomainValues.containsAll(
                ImmutableList.of(
                    new DomainWithDomainValues()
                        .domain(Domain.FITBIT_INTRADAY_STEPS.toString())
                        .items(
                            ImmutableList.of(
                                new DomainValue().value("datetime"),
                                new DomainValue().value("steps"),
                                new DomainValue().value("person_id"))))))
        .isEqualTo(true);
  }

  @Test
  public void previewDataSetByDomainCondition() {
    DataSetPreviewResponse dataSetPreviewResponse =
        Objects.requireNonNull(
            controller
                .previewDataSetByDomain(
                    dbWorkspace.getWorkspaceNamespace(),
                    dbWorkspace.getFirecloudName(),
                    new DataSetPreviewRequest()
                        .domain(Domain.CONDITION)
                        .addConceptSetIdsItem(dbConditionConceptSetForValues.getConceptSetId())
                        .addValuesItem("person_id")
                        .addCohortIdsItem(dbCohort3.getCohortId()))
                .getBody());

    assertThat(dataSetPreviewResponse)
        .isEqualTo(
            new DataSetPreviewResponse()
                .domain(Domain.CONDITION)
                .addValuesItem(
                    new DataSetPreviewValueList().addQueryValueItem("1").value("person_id")));
  }

  @Test
  public void previewDataSetByDomainSourceConditionWithMeasurementDomain() {
    DataSetPreviewResponse dataSetPreviewResponse =
        Objects.requireNonNull(
            controller
                .previewDataSetByDomain(
                    dbWorkspace.getWorkspaceNamespace(),
                    dbWorkspace.getFirecloudName(),
                    new DataSetPreviewRequest()
                        .domain(Domain.MEASUREMENT)
                        .addConceptSetIdsItem(dbConditionConceptSetForValues.getConceptSetId())
                        .addConceptSetIdsItem(dbMeasurementConceptSet.getConceptSetId())
                        .addValuesItem("person_id")
                        .addCohortIdsItem(dbCohort3.getCohortId()))
                .getBody());

    assertThat(dataSetPreviewResponse)
        .isEqualTo(
            new DataSetPreviewResponse()
                .domain(Domain.MEASUREMENT)
                .addValuesItem(
                    new DataSetPreviewValueList().addQueryValueItem("1").value("person_id")));
  }

  @Test
  public void previewDataSetByDomainSourceConditionAndMeasurementConceptSet() {
    DataSetPreviewResponse dataSetPreviewResponse =
        Objects.requireNonNull(
            controller
                .previewDataSetByDomain(
                    dbWorkspace.getWorkspaceNamespace(),
                    dbWorkspace.getFirecloudName(),
                    new DataSetPreviewRequest()
                        .domain(Domain.MEASUREMENT)
                        .addConceptSetIdsItem(dbConditionConceptSetForValues.getConceptSetId())
                        .addValuesItem("person_id")
                        .addCohortIdsItem(dbCohort3.getCohortId()))
                .getBody());

    assertThat(dataSetPreviewResponse)
        .isEqualTo(
            new DataSetPreviewResponse()
                .domain(Domain.MEASUREMENT)
                .addValuesItem(
                    new DataSetPreviewValueList().addQueryValueItem("1").value("person_id")));
  }

  @Test
  public void previewDataSetByDomainPFHHSurveyConceptSet() {
    DataSetPreviewRequest pfhhDataSetRequest =
        new DataSetPreviewRequest()
            .domain(Domain.SURVEY)
            .addConceptSetIdsItem(dbPFHHConceptSet.getConceptSetId())
            .addValuesItem("person_id")
            .addCohortIdsItem(dbCohort3.getCohortId());
    pfhhDataSetRequest.setPrePackagedConceptSet(ImmutableList.of());
    DataSetPreviewResponse dataSetPreviewResponse =
        Objects.requireNonNull(
            controller
                .previewDataSetByDomain(
                    dbWorkspace.getWorkspaceNamespace(),
                    dbWorkspace.getFirecloudName(),
                    pfhhDataSetRequest)
                .getBody());

    assertThat(dataSetPreviewResponse)
        .isEqualTo(
            new DataSetPreviewResponse()
                .domain(Domain.SURVEY)
                .addValuesItem(
                    new DataSetPreviewValueList().addQueryValueItem("1").value("person_id")));
  }

  private void assertAndExecutePythonQuery(String code, int index, Domain domain, long count) {
    String expected =
        String.format(
            "import pandas\n"
                + "import os\n"
                + "\n"
                + "# This query represents dataset \"%s\" for domain \"%s\" and was generated for %s\n"
                + "dataset_00000000_%s_sql =",
            DATASET_NAME,
            domain.toString().toLowerCase(),
            dbCdrVersion.getName(),
            domain.toString().toLowerCase());
    assertThat(code).contains(expected);

    String query = extractPythonQuery(code, index);

    try {
      TableResult result =
          bigQueryService.executeQuery(
              QueryJobConfiguration.newBuilder(query).setUseLegacySql(false).build());
      assertThat(result.getTotalRows()).isEqualTo(count);
    } catch (Exception e) {
      Assertions.fail(
          "Problem generating BigQuery query for notebooks: " + e.getCause().getMessage());
    }
  }

  private DataSetRequest createDataSetRequest(
      Long dataSetId,
      List<Domain> domains,
      boolean allParticipants,
      List<PrePackagedConceptSetEnum> prePackagedConceptSetEnumList) {
    return new DataSetRequest()
        .name(DATASET_NAME)
        .dataSetId(dataSetId)
        .includesAllParticipants(allParticipants)
        .prePackagedConceptSet(prePackagedConceptSetEnumList)
        .domainValuePairs(
            domains.stream()
                .map(d -> new DomainValuePair().domain(d).value("person_id"))
                .collect(Collectors.toList()));
  }

  private DataSetRequest createDataSetRequestTanagra(
      List<Domain> domains,
      boolean allParticipants,
      List<PrePackagedConceptSetEnum> prePackagedConceptSetEnumList) {
    return new DataSetRequest()
        .name(DATASET_NAME)
        .includesAllParticipants(allParticipants)
        .tanagraCohortIds(ImmutableList.of("tanagraCohortId"))
        .tanagraFeatureSetIds(ImmutableList.of("tanagraConceptSetId"))
        .tanagraAllParticipantsCohort(false)
        .prePackagedConceptSet(prePackagedConceptSetEnumList)
        .domainValuePairs(
            domains.stream()
                .map(d -> new DomainValuePair().domain(d).value("person_id"))
                .collect(Collectors.toList()));
  }

  @NotNull
  private String extractPythonQuery(String code, int index) {
    code =
        code.replace(
            "\"\"\" + os.environ[\"WORKSPACE_CDR\"] + \"\"\"",
            testWorkbenchConfig.bigquery.projectId + "." + testWorkbenchConfig.bigquery.dataSetId);
    return code.split("\"\"\"")[index];
  }

  @NotNull
  private String extractRQuery(String code, String... tableNames) {
    String query = code.split("\"")[5];
    return Arrays.stream(tableNames)
        .map(
            tableName ->
                (Function<String, String>)
                    s ->
                        replaceTableName(
                            s,
                            tableName,
                            testWorkbenchConfig.bigquery.projectId,
                            testWorkbenchConfig.bigquery.dataSetId))
        .reduce(Function.identity(), Function::andThen)
        .apply(query);
  }

  private static String replaceTableName(
      String s, String tableName, String projectId, String dataSetId) {
    return s.replace(
        "`" + tableName + "`",
        String.format("`%s`", projectId + "." + dataSetId + "." + tableName));
  }

  private DbUser createUser() {
    DbUser user = new DbUser();
    user.setUsername("bob@gmail.com");
    user.setDisabled(false);
    return userDao.save(user);
  }
}
