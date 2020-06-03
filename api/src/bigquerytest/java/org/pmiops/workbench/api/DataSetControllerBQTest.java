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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.bitbucket.radistao.test.runner.BeforeAfterSpringTestRunner;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.billing.FreeTierBillingService;
import org.pmiops.workbench.cdr.ConceptBigQueryService;
import org.pmiops.workbench.cohortbuilder.CohortQueryBuilder;
import org.pmiops.workbench.cohorts.CohortCloningService;
import org.pmiops.workbench.concept.ConceptService;
import org.pmiops.workbench.conceptset.ConceptSetMapper;
import org.pmiops.workbench.conceptset.ConceptSetMapperImpl;
import org.pmiops.workbench.conceptset.ConceptSetService;
import org.pmiops.workbench.config.CdrBigQuerySchemaConfigService;
import org.pmiops.workbench.config.CommonConfig;
import org.pmiops.workbench.dataset.DataSetMapper;
import org.pmiops.workbench.dataset.DataSetMapperImpl;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.ConceptSetDao;
import org.pmiops.workbench.db.dao.DataDictionaryEntryDao;
import org.pmiops.workbench.db.dao.DataSetDao;
import org.pmiops.workbench.db.dao.DataSetService;
import org.pmiops.workbench.db.dao.DataSetServiceImpl;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbConceptSet;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.FireCloudServiceImpl;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceResponse;
import org.pmiops.workbench.model.ArchivalStatus;
import org.pmiops.workbench.model.DataAccessLevel;
import org.pmiops.workbench.model.DataSetRequest;
import org.pmiops.workbench.model.Domain;
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
  @Autowired private CohortDao cohortDao;
  @Autowired private ConceptService conceptService;
  @Autowired private ConceptSetDao conceptSetDao;
  @Autowired private ConceptSetMapper conceptSetMapper;
  @Autowired private DataDictionaryEntryDao dataDictionaryEntryDao;
  @Autowired private DataSetDao dataSetDao;
  @Autowired private DataSetMapper dataSetMapper;
  @Autowired private DataSetService dataSetService;
  @Autowired private FireCloudService fireCloudService;
  @Autowired private NotebooksService notebooksService;
  @Autowired private TestWorkbenchConfig testWorkbenchConfig;
  @Autowired private Provider<DbUser> userProvider;

  @Autowired
  @Qualifier(CommonConfig.DATASET_PREFIX_CODE)
  Provider<String> prefixProvider;

  @Autowired private WorkspaceDao workspaceDao;
  @Autowired private WorkspaceService workspaceService;

  private DbCdrVersion dbCdrVersion;
  private DbCohort dbCohort1;
  private DbCohort dbCohort2;
  private DbConceptSet dbConditionConceptSet;
  private DbConceptSet dbProcedureConceptSet;
  private DbWorkspace dbWorkspace;

  @TestConfiguration
  @Import({
    BigQueryTestService.class,
    CdrBigQuerySchemaConfigService.class,
    CohortQueryBuilder.class,
    ConceptBigQueryService.class,
    DataSetServiceImpl.class,
    TestBigQueryCdrSchemaConfig.class,
    WorkspaceServiceImpl.class
  })
  @MockBean({
    CohortCloningService.class,
    ConceptService.class,
    ConceptSetMapperImpl.class,
    ConceptSetService.class,
    DataSetMapperImpl.class,
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
    @Qualifier(CommonConfig.DATASET_PREFIX_CODE)
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
                CLOCK,
                cdrVersionDao,
                cohortDao,
                conceptService,
                conceptSetDao,
                dataDictionaryEntryDao,
                dataSetDao,
                dataSetMapper,
                dataSetService,
                fireCloudService,
                notebooksService,
                userProvider,
                prefixProvider,
                workspaceService,
                conceptSetMapper));

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
        conceptSetDao.save(
            DbConceptSet.builder()
                .addDomain(DbStorageEnums.domainToStorage(Domain.CONDITION))
                .addConceptIds(new HashSet<>(Collections.singletonList(1L)))
                .addWorkspaceId(dbWorkspace.getWorkspaceId())
                .build());
    dbProcedureConceptSet =
        conceptSetDao.save(
            DbConceptSet.builder()
                .addDomain(DbStorageEnums.domainToStorage(Domain.PROCEDURE))
                .addConceptIds(new HashSet<>(Collections.singletonList(1L)))
                .addWorkspaceId(dbWorkspace.getWorkspaceId())
                .build());

    dbCohort1 = new DbCohort();
    dbCohort1.setWorkspaceId(dbWorkspace.getWorkspaceId());
    dbCohort1.setCriteria(new Gson().toJson(SearchRequests.icd9CodeWithModifiers()));
    dbCohort1 = cohortDao.save(dbCohort1);

    dbCohort2 = new DbCohort();
    dbCohort2.setWorkspaceId(dbWorkspace.getWorkspaceId());
    dbCohort2.setCriteria(new Gson().toJson(SearchRequests.icd9Codes()));
    dbCohort2 = cohortDao.save(dbCohort2);

    when(controller.generateRandomEightCharacterQualifier()).thenReturn("00000000");
  }

  @After
  public void tearDown() {
    cohortDao.delete(dbCohort1.getCohortId());
    cohortDao.delete(dbCohort2.getCohortId());
    conceptSetDao.delete(dbConditionConceptSet.getConceptSetId());
    conceptSetDao.delete(dbProcedureConceptSet.getConceptSetId());
    workspaceDao.delete(dbWorkspace.getWorkspaceId());
    cdrVersionDao.delete(dbCdrVersion.getCdrVersionId());
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
                    PrePackagedConceptSetEnum.NONE))
            .getBody()
            .getCode();
    assertAndExecutePythonQuery(code, 1, Domain.CONDITION);
  }

  @Test
  public void testGenerateCodeR() {
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
                    PrePackagedConceptSetEnum.NONE))
            .getBody()
            .getCode();

    assertThat(code)
        .contains(
            "library(bigrquery)\n"
                + "\n# This query represents dataset \"null\" for domain \"condition\"\n"
                + "dataset_00000000_condition_sql <- paste(\"");
    String query = extractRQuery(code);

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
                    PrePackagedConceptSetEnum.NONE))
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
                    PrePackagedConceptSetEnum.NONE))
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
                    PrePackagedConceptSetEnum.NONE))
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
                    PrePackagedConceptSetEnum.DEMOGRAPHICS))
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
                    PrePackagedConceptSetEnum.SURVEY))
            .getBody()
            .getCode();

    assertAndExecutePythonQuery(code, 1, Domain.SURVEY);
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

  private DataSetRequest createDataSetRequest(
      List<DbConceptSet> dbConceptSets,
      List<DbCohort> dbCohorts,
      List<Domain> domains,
      boolean allParticipants,
      PrePackagedConceptSetEnum prePackagedConceptSetEnum) {
    return new DataSetRequest()
        .conceptSetIds(
            dbConceptSets.stream().map(DbConceptSet::getConceptSetId).collect(Collectors.toList()))
        .cohortIds(dbCohorts.stream().map(DbCohort::getCohortId).collect(Collectors.toList()))
        .includesAllParticipants(allParticipants)
        .prePackagedConceptSet(prePackagedConceptSetEnum)
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
  private String extractRQuery(String code) {
    String query = code.split("\"")[5];
    query =
        query.replace(
            "`condition_occurrence`",
            String.format(
                "`%s`",
                testWorkbenchConfig.bigquery.projectId
                    + "."
                    + testWorkbenchConfig.bigquery.dataSetId
                    + ".condition_occurrence"));
    return query;
  }
}
