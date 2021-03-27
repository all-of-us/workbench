package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableResult;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.bitbucket.radistao.test.runner.BeforeAfterSpringTestRunner;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.access.AccessTierService;
import org.pmiops.workbench.actionaudit.auditors.BillingProjectAuditor;
import org.pmiops.workbench.billing.FreeTierBillingService;
import org.pmiops.workbench.cdr.CdrVersionService;
import org.pmiops.workbench.cdr.ConceptBigQueryService;
import org.pmiops.workbench.cdr.dao.DSDataDictionaryDao;
import org.pmiops.workbench.cdr.dao.DSLinkingDao;
import org.pmiops.workbench.cdr.model.DbDSLinking;
import org.pmiops.workbench.cohortbuilder.CohortQueryBuilder;
import org.pmiops.workbench.cohorts.CohortCloningService;
import org.pmiops.workbench.cohorts.CohortService;
import org.pmiops.workbench.conceptset.ConceptSetService;
import org.pmiops.workbench.conceptset.mapper.ConceptSetMapperImpl;
import org.pmiops.workbench.config.CdrBigQuerySchemaConfigService;
import org.pmiops.workbench.dataset.DataSetServiceImpl;
import org.pmiops.workbench.dataset.DatasetConfig;
import org.pmiops.workbench.dataset.mapper.DataSetMapperImpl;
import org.pmiops.workbench.db.dao.AccessTierDao;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.ConceptSetDao;
import org.pmiops.workbench.db.dao.DataSetDao;
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
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceResponse;
import org.pmiops.workbench.model.ArchivalStatus;
import org.pmiops.workbench.model.DataSetRequest;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.DomainValue;
import org.pmiops.workbench.model.DomainValuePair;
import org.pmiops.workbench.model.KernelTypeEnum;
import org.pmiops.workbench.model.PrePackagedConceptSetEnum;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.notebooks.NotebooksService;
import org.pmiops.workbench.notebooks.NotebooksServiceImpl;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.test.SearchRequests;
import org.pmiops.workbench.test.TestBigQueryCdrSchemaConfig;
import org.pmiops.workbench.testconfig.TestJpaConfig;
import org.pmiops.workbench.testconfig.TestWorkbenchConfig;
import org.pmiops.workbench.utils.TestMockFactory;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.utils.mappers.UserMapper;
import org.pmiops.workbench.utils.mappers.WorkspaceMapperImpl;
import org.pmiops.workbench.workspaces.WorkspaceAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@RunWith(BeforeAfterSpringTestRunner.class)
@Import({TestJpaConfig.class})
public class DataSetControllerBQTest extends BigQueryBaseTest {

  private static final FakeClock CLOCK = new FakeClock(Instant.now(), ZoneId.systemDefault());
  private static final String WORKSPACE_NAMESPACE = "namespace";
  private static final String WORKSPACE_NAME = "name";
  private static final String DATASET_NAME = "Arbitrary Dataset v1.0";

  @Autowired private AccessTierDao accessTierDao;
  @Autowired private BigQueryService bigQueryService;
  @Autowired private CdrBigQuerySchemaConfigService cdrBigQuerySchemaConfigService;
  @Autowired private CdrVersionDao cdrVersionDao;
  @Autowired private CdrVersionService cdrVersionService;
  @Autowired private CohortDao cohortDao;
  @Autowired private CohortQueryBuilder cohortQueryBuilder;
  @Autowired private ConceptBigQueryService conceptBigQueryService;
  @Autowired private ConceptSetDao conceptSetDao;
  @Autowired private DSLinkingDao dsLinkingDao;
  @Autowired private DataSetDao dataSetDao;
  @Autowired private DSDataDictionaryDao dsDataDictionaryDao;
  @Autowired private DataSetMapperImpl dataSetMapper;
  @Autowired private FireCloudService fireCloudService;
  @Autowired private NotebooksService notebooksService;
  @Autowired private Provider<DbUser> userProvider;
  @Autowired private TestWorkbenchConfig testWorkbenchConfig;
  @Autowired private WorkspaceDao workspaceDao;
  @Autowired private WorkspaceAuthService workspaceAuthService;

  @Autowired
  @Qualifier(DatasetConfig.DATASET_PREFIX_CODE)
  Provider<String> prefixProvider;

  private DataSetController controller;

  private DbCdrVersion dbCdrVersion;
  private DbCohort dbCohort1;
  private DbCohort dbCohort2;
  private DbConceptSet dbConditionConceptSet;
  private DbConceptSet dbProcedureConceptSet;
  private DbWorkspace dbWorkspace;
  private DbDSLinking conditionLinking1;
  private DbDSLinking conditionLinking2;
  private DbDSLinking personLinking1;
  private DbDSLinking personLinking2;
  private DbDSLinking surveyLinking1;
  private DbDSLinking surveyLinking2;
  private DbDSLinking procedureLinking1;
  private DbDSLinking procedureLinking2;

  @TestConfiguration
  @Import({
    BigQueryTestService.class,
    CdrBigQuerySchemaConfigService.class,
    CdrVersionService.class,
    CohortQueryBuilder.class,
    ConceptBigQueryService.class,
    DataSetMapperImpl.class,
    DataSetServiceImpl.class,
    TestBigQueryCdrSchemaConfig.class,
    WorkspaceAuthService.class
  })
  @MockBean({
    BillingProjectAuditor.class,
    CohortCloningService.class,
    CohortService.class,
    CommonMappers.class,
    ConceptSetMapperImpl.class,
    ConceptSetService.class,
    FireCloudServiceImpl.class,
    FreeTierBillingService.class,
    NotebooksServiceImpl.class,
    Provider.class,
    UserMapper.class,
    WorkspaceMapperImpl.class,
    AccessTierService.class,
    CdrVersionService.class,
  })
  static class Configuration {
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
        "ds_steps_intraday");
  }

  @Override
  public String getTestDataDirectory() {
    return CB_DATA;
  }

  @Before
  public void setUp() {
    DataSetServiceImpl dataSetServiceImpl =
        new DataSetServiceImpl(
            bigQueryService,
            cdrBigQuerySchemaConfigService,
            cohortDao,
            conceptBigQueryService,
            conceptSetDao,
            cohortQueryBuilder,
            dataSetDao,
            dsLinkingDao,
            dsDataDictionaryDao,
            dataSetMapper,
            CLOCK);
    controller =
        spy(
            new DataSetController(
                bigQueryService,
                cdrVersionService,
                dataSetServiceImpl,
                fireCloudService,
                notebooksService,
                userProvider,
                prefixProvider,
                workspaceAuthService));

    FirecloudWorkspaceResponse fcResponse = new FirecloudWorkspaceResponse();
    fcResponse.setAccessLevel(WorkspaceAccessLevel.OWNER.name());
    when(fireCloudService.getWorkspace(WORKSPACE_NAMESPACE, WORKSPACE_NAME))
        .thenReturn(fcResponse)
        .thenReturn(fcResponse);

    dbCdrVersion = TestMockFactory.createDefaultCdrVersion(cdrVersionDao, accessTierDao);
    dbCdrVersion.setBigqueryDataset(testWorkbenchConfig.bigquery.dataSetId);
    dbCdrVersion.setBigqueryProject(testWorkbenchConfig.bigquery.projectId);
    dbCdrVersion.setArchivalStatus(DbStorageEnums.archivalStatusToStorage(ArchivalStatus.LIVE));
    dbCdrVersion = cdrVersionDao.save(dbCdrVersion);

    dbWorkspace = new DbWorkspace();
    dbWorkspace.setWorkspaceNamespace(WORKSPACE_NAMESPACE);
    dbWorkspace.setFirecloudName(WORKSPACE_NAME);
    dbWorkspace.setCdrVersion(dbCdrVersion);
    dbWorkspace = workspaceDao.save(dbWorkspace);
    dbConditionConceptSet =
        conceptSetDao.save(createConceptSet(Domain.CONDITION, dbWorkspace.getWorkspaceId()));
    dbProcedureConceptSet =
        conceptSetDao.save(createConceptSet(Domain.PROCEDURE, dbWorkspace.getWorkspaceId()));

    dbCohort1 = new DbCohort();
    dbCohort1.setWorkspaceId(dbWorkspace.getWorkspaceId());
    dbCohort1.setCriteria(new Gson().toJson(SearchRequests.icd9CodeWithModifiers()));
    dbCohort1 = cohortDao.save(dbCohort1);

    dbCohort2 = new DbCohort();
    dbCohort2.setWorkspaceId(dbWorkspace.getWorkspaceId());
    dbCohort2.setCriteria(new Gson().toJson(SearchRequests.icd9Codes()));
    dbCohort2 = cohortDao.save(dbCohort2);

    when(controller.generateRandomEightCharacterQualifier()).thenReturn("00000000");

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
  }

  private DbConceptSet createConceptSet(Domain domain, long workspaceId) {
    DbConceptSet dbConceptSet = new DbConceptSet();
    dbConceptSet.setDomain(DbStorageEnums.domainToStorage(domain));
    DbConceptSetConceptId dbConceptSetConceptId =
        DbConceptSetConceptId.builder().addConceptId(1L).addStandard(false).build();
    dbConceptSet.setConceptSetConceptIds(
        new HashSet<>(Collections.singletonList(dbConceptSetConceptId)));
    dbConceptSet.setWorkspaceId(workspaceId);
    return dbConceptSet;
  }

  @After
  public void tearDown() {
    cohortDao.delete(dbCohort1.getCohortId());
    cohortDao.delete(dbCohort2.getCohortId());
    conceptSetDao.delete(dbConditionConceptSet.getConceptSetId());
    conceptSetDao.delete(dbProcedureConceptSet.getConceptSetId());
    workspaceDao.delete(dbWorkspace.getWorkspaceId());
    cdrVersionDao.delete(dbCdrVersion.getCdrVersionId());
    dsLinkingDao.delete(conditionLinking1);
    dsLinkingDao.delete(conditionLinking2);
    dsLinkingDao.delete(personLinking1);
    dsLinkingDao.delete(personLinking2);
    dsLinkingDao.delete(surveyLinking1);
    dsLinkingDao.delete(surveyLinking2);
    dsLinkingDao.delete(procedureLinking1);
    dsLinkingDao.delete(procedureLinking2);
  }

  @Test
  public void testGenerateCodePython() {
    String code =
        controller
            .generateCode(
                WORKSPACE_NAMESPACE,
                WORKSPACE_NAME,
                KernelTypeEnum.PYTHON.toString(),
                createDataSetRequest(
                    ImmutableList.of(dbConditionConceptSet),
                    ImmutableList.of(dbCohort1),
                    ImmutableList.of(Domain.CONDITION),
                    false,
                    ImmutableList.of(PrePackagedConceptSetEnum.NONE)))
            .getBody()
            .getCode();
    assertAndExecutePythonQuery(code, 1, Domain.CONDITION);
  }

  @Test
  public void testGenerateCodeR() {
    String expected =
        String.format(
            "library(bigrquery)\n"
                + "\n# This query represents dataset \"%s\" for domain \"condition\" and was generated for %s\n"
                + "dataset_00000000_condition_sql <- paste(\"",
            DATASET_NAME, dbCdrVersion.getName());

    String code =
        controller
            .generateCode(
                WORKSPACE_NAMESPACE,
                WORKSPACE_NAME,
                KernelTypeEnum.R.toString(),
                createDataSetRequest(
                    ImmutableList.of(dbConditionConceptSet),
                    ImmutableList.of(dbCohort1),
                    ImmutableList.of(Domain.CONDITION),
                    false,
                    ImmutableList.of(PrePackagedConceptSetEnum.NONE)))
            .getBody()
            .getCode();
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
      fail("Problem generating BigQuery query for notebooks: " + e.getCause().getMessage());
    }
  }

  @Test
  public void testGenerateCodeTwoConceptSets() {
    String code =
        controller
            .generateCode(
                WORKSPACE_NAMESPACE,
                WORKSPACE_NAME,
                KernelTypeEnum.PYTHON.toString(),
                createDataSetRequest(
                    ImmutableList.of(dbConditionConceptSet, dbProcedureConceptSet),
                    ImmutableList.of(dbCohort1),
                    ImmutableList.of(Domain.CONDITION, Domain.PROCEDURE),
                    false,
                    ImmutableList.of(PrePackagedConceptSetEnum.NONE)))
            .getBody()
            .getCode();

    assertAndExecutePythonQuery(code, 3, Domain.CONDITION);
  }

  @Test
  public void testGenerateCodeTwoCohorts() {
    String code =
        controller
            .generateCode(
                WORKSPACE_NAMESPACE,
                WORKSPACE_NAME,
                KernelTypeEnum.PYTHON.toString(),
                createDataSetRequest(
                    ImmutableList.of(dbConditionConceptSet),
                    ImmutableList.of(dbCohort1, dbCohort2),
                    ImmutableList.of(Domain.CONDITION),
                    false,
                    ImmutableList.of(PrePackagedConceptSetEnum.NONE)))
            .getBody()
            .getCode();

    assertAndExecutePythonQuery(code, 1, Domain.CONDITION);
  }

  @Test
  public void testGenerateCodeAllParticipants() {
    String code =
        controller
            .generateCode(
                WORKSPACE_NAMESPACE,
                WORKSPACE_NAME,
                KernelTypeEnum.PYTHON.toString(),
                createDataSetRequest(
                    ImmutableList.of(dbConditionConceptSet),
                    ImmutableList.of(),
                    ImmutableList.of(Domain.CONDITION),
                    true,
                    ImmutableList.of(PrePackagedConceptSetEnum.NONE)))
            .getBody()
            .getCode();

    assertAndExecutePythonQuery(code, 1, Domain.CONDITION);
  }

  @Test
  public void testGenerateCodePrepackagedCohortDemographics() {
    String code =
        controller
            .generateCode(
                WORKSPACE_NAMESPACE,
                WORKSPACE_NAME,
                KernelTypeEnum.PYTHON.toString(),
                createDataSetRequest(
                    ImmutableList.of(),
                    ImmutableList.of(dbCohort1),
                    ImmutableList.of(Domain.PERSON),
                    false,
                    ImmutableList.of(PrePackagedConceptSetEnum.PERSON)))
            .getBody()
            .getCode();

    assertAndExecutePythonQuery(code, 1, Domain.PERSON);
  }

  @Test
  public void testGenerateCodePrepackagedCohortSurveys() {
    String code =
        controller
            .generateCode(
                WORKSPACE_NAMESPACE,
                WORKSPACE_NAME,
                KernelTypeEnum.PYTHON.toString(),
                createDataSetRequest(
                    ImmutableList.of(),
                    ImmutableList.of(dbCohort1),
                    ImmutableList.of(Domain.SURVEY),
                    false,
                    ImmutableList.of(PrePackagedConceptSetEnum.SURVEY)))
            .getBody()
            .getCode();

    assertAndExecutePythonQuery(code, 1, Domain.SURVEY);
  }

  @Test
  public void testGenerateCodePrepackagedConceptSetFitBit() {
    addFitbitInfoToDsLinkingTable();
    String code =
        controller
            .generateCode(
                WORKSPACE_NAMESPACE,
                WORKSPACE_NAME,
                KernelTypeEnum.PYTHON.toString(),
                createDataSetRequest(
                    ImmutableList.of(),
                    ImmutableList.of(dbCohort1),
                    ImmutableList.of(Domain.FITBIT_HEART_RATE_LEVEL),
                    false,
                    ImmutableList.of(PrePackagedConceptSetEnum.NONE)))
            .getBody()
            .getCode();

    assertAndExecutePythonQuery(code, 1, Domain.FITBIT_HEART_RATE_LEVEL);
  }

  @Test
  public void testGenerateCodeTwoPrePackagedConceptSet() {
    String code =
        controller
            .generateCode(
                WORKSPACE_NAMESPACE,
                WORKSPACE_NAME,
                KernelTypeEnum.PYTHON.toString(),
                createDataSetRequest(
                    ImmutableList.of(dbConditionConceptSet, dbProcedureConceptSet),
                    ImmutableList.of(dbCohort1),
                    ImmutableList.of(Domain.CONDITION),
                    false,
                    ImmutableList.of(
                        PrePackagedConceptSetEnum.PERSON,
                        PrePackagedConceptSetEnum.FITBIT_HEART_RATE_LEVEL)))
            .getBody()
            .getCode();

    assertAndExecutePythonQuery(code, 1, Domain.CONDITION);
  }

  @Test
  public void getValuesFromDomainActivitySummary() {
    List<DomainValue> domainValues =
        controller
            .getValuesFromDomain(
                WORKSPACE_NAMESPACE, WORKSPACE_NAME, Domain.FITBIT_ACTIVITY.toString())
            .getBody()
            .getItems();
    assertThat(
            domainValues.containsAll(
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
                    new DomainValue().value("person_id"))))
        .isEqualTo(true);
  }

  @Test
  public void getValuesFromDomainHeartRateLevel() {
    List<DomainValue> domainValues =
        controller
            .getValuesFromDomain(
                WORKSPACE_NAMESPACE, WORKSPACE_NAME, Domain.FITBIT_HEART_RATE_LEVEL.toString())
            .getBody()
            .getItems();
    assertThat(
            domainValues.containsAll(
                ImmutableList.of(
                    new DomainValue().value("datetime"),
                    new DomainValue().value("person_id"),
                    new DomainValue().value("heart_rate_value"))))
        .isEqualTo(true);
  }

  @Test
  public void getValuesFromDomainHeartRateSummary() {
    List<DomainValue> domainValues =
        controller
            .getValuesFromDomain(
                WORKSPACE_NAMESPACE, WORKSPACE_NAME, Domain.FITBIT_HEART_RATE_SUMMARY.toString())
            .getBody()
            .getItems();
    assertThat(
            domainValues.containsAll(
                ImmutableList.of(
                    new DomainValue().value("zone_name"),
                    new DomainValue().value("min_heart_rate"),
                    new DomainValue().value("max_heart_rate"),
                    new DomainValue().value("minute_in_zone"),
                    new DomainValue().value("calorie_count"))))
        .isEqualTo(true);
  }

  @Test
  public void getValuesFromDomainStepsIntraday() {
    List<DomainValue> domainValues =
        controller
            .getValuesFromDomain(
                WORKSPACE_NAMESPACE, WORKSPACE_NAME, Domain.FITBIT_INTRADAY_STEPS.toString())
            .getBody()
            .getItems();
    assertThat(
            domainValues.containsAll(
                ImmutableList.of(
                    new DomainValue().value("datetime"),
                    new DomainValue().value("steps"),
                    new DomainValue().value("person_id"))))
        .isEqualTo(true);
  }

  private void assertAndExecutePythonQuery(String code, int index, Domain domain) {
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
      assertThat(result.getTotalRows()).isEqualTo(1L);
    } catch (Exception e) {
      fail("Problem generating BigQuery query for notebooks: " + e.getCause().getMessage());
    }
  }

  private DataSetRequest createDataSetRequest(
      List<DbConceptSet> dbConceptSets,
      List<DbCohort> dbCohorts,
      List<Domain> domains,
      boolean allParticipants,
      List<PrePackagedConceptSetEnum> prePackagedConceptSetEnumList) {
    return new DataSetRequest()
        .name(DATASET_NAME)
        .conceptSetIds(
            dbConceptSets.stream().map(DbConceptSet::getConceptSetId).collect(Collectors.toList()))
        .cohortIds(dbCohorts.stream().map(DbCohort::getCohortId).collect(Collectors.toList()))
        .includesAllParticipants(allParticipants)
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

  private void addFitbitInfoToDsLinkingTable() {
    DbDSLinking fitbitHeartRateLinking_personId = new DbDSLinking();
    fitbitHeartRateLinking_personId.setOmopSql("heart_rate_minute_level.PERSON_ID");
    fitbitHeartRateLinking_personId.setJoinValue(
        "from `${projectId}.${dataSetId}.heart_rate_minute_level` heart_rate_minute_level");
    fitbitHeartRateLinking_personId.setDomain("Fitbit_heart_rate_level");
    fitbitHeartRateLinking_personId.setDenormalizedName("PERSON_ID");

    dsLinkingDao.save(fitbitHeartRateLinking_personId);

    DbDSLinking fitbitHeartRateLinking_coreTable = new DbDSLinking();
    fitbitHeartRateLinking_coreTable.setOmopSql("CORE_TABLE_FOR_DOMAIN");
    fitbitHeartRateLinking_coreTable.setJoinValue(
        "from `${projectId}.${dataSetId}.heart_rate_minute_level` heart_rate_minute_level");
    fitbitHeartRateLinking_coreTable.setDomain("Fitbit_heart_rate_level");
    fitbitHeartRateLinking_coreTable.setDenormalizedName("CORE_TABLE_FOR_DOMAIN");

    dsLinkingDao.save(fitbitHeartRateLinking_coreTable);
  }
}
