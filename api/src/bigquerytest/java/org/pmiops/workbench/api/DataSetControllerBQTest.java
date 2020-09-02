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
import org.pmiops.workbench.billing.FreeTierBillingService;
import org.pmiops.workbench.cdr.CdrVersionService;
import org.pmiops.workbench.cdr.ConceptBigQueryService;
import org.pmiops.workbench.cdr.dao.DSLinkingDao;
import org.pmiops.workbench.cdr.model.DbDSLinking;
import org.pmiops.workbench.cohortbuilder.CohortQueryBuilder;
import org.pmiops.workbench.cohorts.CohortCloningService;
import org.pmiops.workbench.cohorts.CohortService;
import org.pmiops.workbench.concept.ConceptService;
import org.pmiops.workbench.conceptset.ConceptSetService;
import org.pmiops.workbench.conceptset.mapper.ConceptSetMapperImpl;
import org.pmiops.workbench.config.CdrBigQuerySchemaConfigService;
import org.pmiops.workbench.dataset.DataSetService;
import org.pmiops.workbench.dataset.DataSetServiceImpl;
import org.pmiops.workbench.dataset.DatasetConfig;
import org.pmiops.workbench.dataset.mapper.DataSetMapperImpl;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.ConceptSetDao;
import org.pmiops.workbench.db.dao.DataSetDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbConceptSet;
import org.pmiops.workbench.db.model.DbDataset;
import org.pmiops.workbench.db.model.DbDatasetValue;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.FireCloudServiceImpl;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceResponse;
import org.pmiops.workbench.model.ArchivalStatus;
import org.pmiops.workbench.model.DataAccessLevel;
import org.pmiops.workbench.model.DataSetGenerateCodeRequest;
import org.pmiops.workbench.model.Domain;
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
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.utils.mappers.UserMapper;
import org.pmiops.workbench.utils.mappers.WorkspaceMapperImpl;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.pmiops.workbench.workspaces.WorkspaceServiceImpl;
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

  private DataSetController controller;
  @Autowired private BigQueryService bigQueryService;
  @Autowired private CdrVersionDao cdrVersionDao;
  @Autowired private CdrVersionService cdrVersionService;
  @Autowired private CohortDao cohortDao;
  @Autowired private ConceptSetDao conceptSetDao;
  @Autowired private DSLinkingDao dsLinkingDao;
  @Autowired private DataSetService dataSetService;
  @Autowired private FireCloudService fireCloudService;
  @Autowired private NotebooksService notebooksService;
  @Autowired private TestWorkbenchConfig testWorkbenchConfig;
  @Autowired private Provider<DbUser> userProvider;
  @Autowired private DataSetDao dataSetDao;

  @Autowired
  @Qualifier(DatasetConfig.DATASET_PREFIX_CODE)
  Provider<String> prefixProvider;

  @Autowired private WorkspaceDao workspaceDao;
  @Autowired private WorkspaceService workspaceService;

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
    DataSetServiceImpl.class,
    DataSetMapperImpl.class,
    TestBigQueryCdrSchemaConfig.class,
    WorkspaceServiceImpl.class
  })
  @MockBean({
    CohortCloningService.class,
    CohortService.class,
    ConceptService.class,
    ConceptSetMapperImpl.class,
    CommonMappers.class,
    ConceptSetService.class,
    DataSetDao.class,
    FireCloudServiceImpl.class,
    FreeTierBillingService.class,
    NotebooksServiceImpl.class,
    Provider.class,
    UserMapper.class,
    WorkspaceMapperImpl.class
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
        "person");
  }

  @Override
  public String getTestDataDirectory() {
    return CB_DATA;
  }

  @Before
  public void setUp() {
    controller =
        spy(
            new DataSetController(
                bigQueryService,
                cdrVersionService,
                dataSetService,
                fireCloudService,
                notebooksService,
                userProvider,
                prefixProvider,
                workspaceService));

    FirecloudWorkspaceResponse fcResponse = new FirecloudWorkspaceResponse();
    fcResponse.setAccessLevel(WorkspaceAccessLevel.OWNER.name());
    when(fireCloudService.getWorkspace(WORKSPACE_NAMESPACE, WORKSPACE_NAME))
        .thenReturn(fcResponse)
        .thenReturn(fcResponse);

    dbCdrVersion = new DbCdrVersion();
    dbCdrVersion.setBigqueryDataset(testWorkbenchConfig.bigquery.dataSetId);
    dbCdrVersion.setBigqueryProject(testWorkbenchConfig.bigquery.projectId);
    dbCdrVersion.setDataAccessLevel(
        DbStorageEnums.dataAccessLevelToStorage(DataAccessLevel.REGISTERED));
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
    dbConceptSet.setConceptIds(new HashSet<>(Collections.singletonList(1L)));
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
    mockDbDataSet(
        ImmutableList.of(dbCohort1.getCohortId()),
        ImmutableList.of(dbConditionConceptSet.getConceptSetId()),
        ImmutableList.of(Domain.CONDITION),
        false,
        PrePackagedConceptSetEnum.NONE);

    String code =
        controller
            .generateCode(
                WORKSPACE_NAMESPACE,
                WORKSPACE_NAME,
                KernelTypeEnum.PYTHON.toString(),
                new DataSetGenerateCodeRequest())
            .getBody()
            .getCode();
    assertAndExecutePythonQuery(code, 1, Domain.CONDITION);
  }

  @Test
  public void testGenerateCodeR() {
    mockDbDataSet(
        ImmutableList.of(dbCohort1.getCohortId()),
        ImmutableList.of(dbConditionConceptSet.getConceptSetId()),
        ImmutableList.of(Domain.CONDITION),
        false,
        PrePackagedConceptSetEnum.NONE);
    String code =
        controller
            .generateCode(
                WORKSPACE_NAMESPACE,
                WORKSPACE_NAME,
                KernelTypeEnum.R.toString(),
                new DataSetGenerateCodeRequest())
            .getBody()
            .getCode();

    assertThat(code)
        .contains(
            "library(bigrquery)\n"
                + "\n# This query represents dataset \"null\" for domain \"condition\"\n"
                + "dataset_00000000_condition_sql <- paste(\"");
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
    mockDbDataSet(
        ImmutableList.of(dbCohort1.getCohortId()),
        ImmutableList.of(
            dbConditionConceptSet.getConceptSetId(), dbProcedureConceptSet.getConceptSetId()),
        ImmutableList.of(Domain.CONDITION, Domain.PERSON),
        false,
        PrePackagedConceptSetEnum.NONE);

    String code =
        controller
            .generateCode(
                WORKSPACE_NAMESPACE,
                WORKSPACE_NAME,
                KernelTypeEnum.PYTHON.toString(),
                new DataSetGenerateCodeRequest())
            .getBody()
            .getCode();

    assertAndExecutePythonQuery(code, 3, Domain.CONDITION);
  }

  @Test
  public void testGenerateCodeTwoCohorts() {
    mockDbDataSet(
        ImmutableList.of(dbCohort1.getCohortId(), dbCohort2.getCohortId()),
        ImmutableList.of(dbConditionConceptSet.getConceptSetId()),
        ImmutableList.of(Domain.CONDITION),
        false,
        PrePackagedConceptSetEnum.NONE);
    String code =
        controller
            .generateCode(
                WORKSPACE_NAMESPACE,
                WORKSPACE_NAME,
                KernelTypeEnum.PYTHON.toString(),
                new DataSetGenerateCodeRequest())
            .getBody()
            .getCode();

    assertAndExecutePythonQuery(code, 1, Domain.CONDITION);
  }

  @Test
  public void testGenerateCodeAllParticipants() {
    mockDbDataSet(
        ImmutableList.of(),
        ImmutableList.of(dbConditionConceptSet.getConceptSetId()),
        ImmutableList.of(Domain.CONDITION),
        true,
        PrePackagedConceptSetEnum.NONE);
    String code =
        controller
            .generateCode(
                WORKSPACE_NAMESPACE,
                WORKSPACE_NAME,
                KernelTypeEnum.PYTHON.toString(),
                new DataSetGenerateCodeRequest())
            .getBody()
            .getCode();

    assertAndExecutePythonQuery(code, 1, Domain.CONDITION);
  }

  @Test
  public void testGenerateCodePrepackagedCohortDemographics() {
    mockDbDataSet(
        ImmutableList.of(dbCohort1.getCohortId()),
        ImmutableList.of(),
        ImmutableList.of(Domain.PERSON),
        false,
        PrePackagedConceptSetEnum.PERSON);
    String code =
        controller
            .generateCode(
                WORKSPACE_NAMESPACE,
                WORKSPACE_NAME,
                KernelTypeEnum.PYTHON.toString(),
                new DataSetGenerateCodeRequest())
            .getBody()
            .getCode();

    assertAndExecutePythonQuery(code, 1, Domain.PERSON);
  }

  @Test
  public void testGenerateCodePrepackagedCohortSurveys() {
    mockDbDataSet(
        ImmutableList.of(dbCohort1.getCohortId()),
        ImmutableList.of(),
        ImmutableList.of(Domain.SURVEY),
        false,
        PrePackagedConceptSetEnum.SURVEY);
    String code =
        controller
            .generateCode(
                WORKSPACE_NAMESPACE,
                WORKSPACE_NAME,
                KernelTypeEnum.PYTHON.toString(),
                new DataSetGenerateCodeRequest())
            .getBody()
            .getCode();

    assertAndExecutePythonQuery(code, 1, Domain.SURVEY);
  }

  private void mockDbDataSet(
      List<Long> cohortIdList,
      List<Long> conceptIdList,
      List<Domain> domains,
      boolean includesAllParticipants,
      PrePackagedConceptSetEnum prePackagedConceptSetEnum) {
    DbDataset dbDataset = new DbDataset(1l, 1l, null, "", 1l, null, false);
    dbDataset.setIncludesAllParticipants(includesAllParticipants);
    dbDataset.setValues(
        domains.stream()
            .map(
                d -> {
                  DbDatasetValue s = new DbDatasetValue();
                  s.setValue("person_id");
                  s.setDomainId(DbStorageEnums.domainToStorage(d).toString());
                  return s;
                })
            .collect(Collectors.toList()));
    dbDataset.setCohortIds(cohortIdList);
    dbDataset.setConceptSetIds(conceptIdList);
    dbDataset.setPrePackagedConceptSetEnum(prePackagedConceptSetEnum);
    dbDataset.setPrePackagedConceptSet(
        DbStorageEnums.prePackagedConceptSetsToStorage(prePackagedConceptSetEnum));
    when(dataSetDao.findByNameAndWorkspaceId(null, dbWorkspace.getWorkspaceId()))
        .thenReturn(dbDataset);
  }

  private void assertAndExecutePythonQuery(String code, int index, Domain domain) {
    assertThat(code)
        .contains(
            "import pandas\n"
                + "import os\n"
                + "\n"
                + "# This query represents dataset \"null\" for domain \""
                + domain.toString().toLowerCase()
                + "\"\n"
                + "dataset_00000000_"
                + domain.toString().toLowerCase()
                + "_sql =");

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
}
