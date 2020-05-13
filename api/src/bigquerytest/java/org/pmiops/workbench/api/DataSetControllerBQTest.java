package org.pmiops.workbench.api;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import javax.inject.Provider;
import org.bitbucket.radistao.test.runner.BeforeAfterSpringTestRunner;
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
import org.pmiops.workbench.utils.WorkspaceMapperImpl;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.pmiops.workbench.workspaces.WorkspaceServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
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
  @Autowired private WorkspaceDao workspaceDao;
  @Autowired private WorkspaceService workspaceService;

  private DbCdrVersion dbCdrVersion;
  private DbCohort dbCohort;
  private DbConceptSet dbConditionConceptSet;
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
    WorkspaceMapperImpl.class
  })
  static class Configuration {
    @Bean
    public Clock clock() {
      return CLOCK;
    }
  }

  @Override
  public List<String> getTableNames() {
    return ImmutableList.of(
        "condition_occurrence",
        "concept",
        "cb_search_person",
        "cb_search_all_events",
        "cb_criteria",
        "ds_linking",
        "person");
  }

  @Override
  public String getTestDataDirectory() {
    return CB_DATA;
  }

  @Before
  public void setUp() {
    controller =
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
            workspaceService,
            conceptSetMapper);

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

    dbCohort = new DbCohort();
    dbCohort.setWorkspaceId(dbWorkspace.getWorkspaceId());
    dbCohort.setCriteria(new Gson().toJson(SearchRequests.icd9CodeWithModifiers()));
    dbCohort = cohortDao.save(dbCohort);
  }

  @After
  public void tearDown() {
    conceptSetDao.delete(dbConditionConceptSet.getConceptSetId());
    workspaceDao.delete(dbWorkspace.getWorkspaceId());
    cdrVersionDao.delete(dbCdrVersion.getCdrVersionId());
  }

  @Test
  public void testGenerateCode() {
    String code =
        controller
            .generateCode(
                WORKSPACE_NAMESPACE,
                WORKSPACE_NAME,
                KernelTypeEnum.PYTHON.toString(),
                createDataSetRequest(dbConditionConceptSet, false, PrePackagedConceptSetEnum.NONE))
            .getBody()
            .getCode();
    String query =
        code.replace(
                "\"\"\" + os.environ[\"WORKSPACE_CDR\"] + \"\"\"",
                testWorkbenchConfig.bigquery.projectId
                    + "."
                    + testWorkbenchConfig.bigquery.dataSetId)
            .split("\"\"\"")[1];

    // Testing that generateCode produces a valid BigQuery query since it rewrites the named
    // parameters produced by CohortBuilder.
    try {
      bigQueryService.executeQuery(
          QueryJobConfiguration.newBuilder(query).setUseLegacySql(false).build());
    } catch (Exception e) {
      fail("Problem generating BigQuery query for notebooks: " + e.getMessage());
    }
  }

  @Test
  public void testGenerateCodeAllParticipants() {
    String code =
        controller
            .generateCode(
                WORKSPACE_NAMESPACE,
                WORKSPACE_NAME,
                KernelTypeEnum.PYTHON.toString(),
                createDataSetRequest(dbConditionConceptSet, true, PrePackagedConceptSetEnum.NONE))
            .getBody()
            .getCode();
    String query =
        code.replace(
                "\"\"\" + os.environ[\"WORKSPACE_CDR\"] + \"\"\"",
                testWorkbenchConfig.bigquery.projectId
                    + "."
                    + testWorkbenchConfig.bigquery.dataSetId)
            .split("\"\"\"")[1];

    // Testing that generateCode produces a valid BigQuery query since it rewrites the named
    // parameters produced by CohortBuilder.
    try {
      bigQueryService.executeQuery(
          QueryJobConfiguration.newBuilder(query).setUseLegacySql(false).build());
    } catch (Exception e) {
      fail("Problem generating BigQuery query for notebooks: " + e.getMessage());
    }
  }

  @Test
  public void testGenerateCodePrepackagedCohort() {
    String code =
        controller
            .generateCode(
                WORKSPACE_NAMESPACE,
                WORKSPACE_NAME,
                KernelTypeEnum.PYTHON.toString(),
                createDataSetRequest(null, false, PrePackagedConceptSetEnum.DEMOGRAPHICS))
            .getBody()
            .getCode();
    String query =
        code.replace(
                "\"\"\" + os.environ[\"WORKSPACE_CDR\"] + \"\"\"",
                testWorkbenchConfig.bigquery.projectId
                    + "."
                    + testWorkbenchConfig.bigquery.dataSetId)
            .split("\"\"\"")[1];

    // Testing that generateCode produces a valid BigQuery query since it rewrites the named
    // parameters produced by CohortBuilder.
    try {
      bigQueryService.executeQuery(
          QueryJobConfiguration.newBuilder(query).setUseLegacySql(false).build());
    } catch (Exception e) {
      fail("Problem generating BigQuery query for notebooks: " + e.getMessage());
    }
  }

  private DataSetRequest createDataSetRequest(
      DbConceptSet dbConceptSet,
      boolean allParticipants,
      PrePackagedConceptSetEnum prePackagedConceptSetEnum) {
    Domain domain = dbConceptSet == null ? null : dbConceptSet.getDomainEnum();
    if (PrePackagedConceptSetEnum.DEMOGRAPHICS.equals(prePackagedConceptSetEnum)) {
      domain = Domain.PERSON;
    }
    if (PrePackagedConceptSetEnum.SURVEY.equals(prePackagedConceptSetEnum)) {
      domain = Domain.SURVEY;
    }
    DomainValuePair domainValuePair = new DomainValuePair().domain(domain).value("person_id");
    return new DataSetRequest()
        .conceptSetIds(
            PrePackagedConceptSetEnum.NONE.equals(prePackagedConceptSetEnum)
                ? Collections.singletonList(dbConceptSet.getConceptSetId())
                : new ArrayList<>())
        .addCohortIdsItem(dbCohort.getCohortId())
        .includesAllParticipants(allParticipants)
        .prePackagedConceptSet(prePackagedConceptSetEnum)
        .addDomainValuePairsItem(domainValuePair);
  }
}
