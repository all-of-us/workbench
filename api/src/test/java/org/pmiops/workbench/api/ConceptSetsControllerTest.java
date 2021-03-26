package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import com.google.api.services.cloudbilling.Cloudbilling;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.access.AccessTierServiceImpl;
import org.pmiops.workbench.actionaudit.auditors.BillingProjectAuditor;
import org.pmiops.workbench.actionaudit.auditors.UserServiceAuditor;
import org.pmiops.workbench.actionaudit.auditors.WorkspaceAuditor;
import org.pmiops.workbench.billing.BillingProjectBufferService;
import org.pmiops.workbench.billing.FreeTierBillingService;
import org.pmiops.workbench.cdr.ConceptBigQueryService;
import org.pmiops.workbench.cdr.dao.CBCriteriaDao;
import org.pmiops.workbench.cdr.model.DbCriteria;
import org.pmiops.workbench.cdrselector.WorkspaceResourcesServiceImpl;
import org.pmiops.workbench.cohortbuilder.CohortBuilderServiceImpl;
import org.pmiops.workbench.cohortbuilder.CohortQueryBuilder;
import org.pmiops.workbench.cohortbuilder.mapper.CohortBuilderMapperImpl;
import org.pmiops.workbench.cohortreview.CohortReviewServiceImpl;
import org.pmiops.workbench.cohortreview.mapper.CohortReviewMapperImpl;
import org.pmiops.workbench.cohorts.CohortCloningService;
import org.pmiops.workbench.cohorts.CohortFactoryImpl;
import org.pmiops.workbench.cohorts.CohortMapperImpl;
import org.pmiops.workbench.cohorts.CohortService;
import org.pmiops.workbench.compliance.ComplianceService;
import org.pmiops.workbench.conceptset.ConceptSetService;
import org.pmiops.workbench.conceptset.mapper.ConceptSetMapperImpl;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.dataset.DataSetService;
import org.pmiops.workbench.dataset.mapper.DataSetMapperImpl;
import org.pmiops.workbench.db.dao.AccessTierDao;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserRecentResourceService;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbConceptSetConceptId;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspace;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceACL;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceAccessEntry;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceResponse;
import org.pmiops.workbench.google.CloudStorageClient;
import org.pmiops.workbench.google.DirectoryService;
import org.pmiops.workbench.model.ConceptSet;
import org.pmiops.workbench.model.ConceptSetConceptId;
import org.pmiops.workbench.model.CreateConceptSetRequest;
import org.pmiops.workbench.model.Criteria;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.EmailVerificationStatus;
import org.pmiops.workbench.model.ResearchPurpose;
import org.pmiops.workbench.model.Surveys;
import org.pmiops.workbench.model.UpdateConceptSetRequest;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.monitoring.LogsBasedMetricServiceFakeImpl;
import org.pmiops.workbench.notebooks.NotebooksService;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.test.FakeLongRandom;
import org.pmiops.workbench.testconfig.UserServiceTestConfiguration;
import org.pmiops.workbench.utils.TestMockFactory;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.utils.mappers.FirecloudMapperImpl;
import org.pmiops.workbench.utils.mappers.UserMapperImpl;
import org.pmiops.workbench.utils.mappers.WorkspaceMapperImpl;
import org.pmiops.workbench.workspaces.WorkspaceServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringRunner.class)
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class ConceptSetsControllerTest {

  private static final Criteria CLIENT_CRITERIA_1 =
      new Criteria()
          .conceptId(123L)
          .name("a concept")
          .isStandard(true)
          .code("conceptA")
          .type("V1")
          .domainId(Domain.CONDITION.toString())
          .childCount(123L)
          .parentCount(123L);

  private static final Criteria CLIENT_CRITERIA_2 =
      new Criteria()
          .conceptId(456L)
          .isStandard(true)
          .name("b concept")
          .code("conceptB")
          .type("V2")
          .domainId(Domain.MEASUREMENT.toString())
          .childCount(456L)
          .parentCount(456L);

  private static final Criteria CLIENT_CRITERIA_3 =
      new Criteria()
          .conceptId(789L)
          .isStandard(true)
          .name("multi word concept")
          .code("conceptC")
          .type("V3")
          .domainId(Domain.CONDITION.toString())
          .childCount(789L)
          .parentCount(789L);

  private static final Criteria CLIENT_CRITERIA_4 =
      new Criteria()
          .conceptId(7890L)
          .isStandard(true)
          .name("conceptD test concept")
          .code("conceptE")
          .type("V5")
          .domainId(Domain.CONDITION.toString())
          .childCount(7890L)
          .parentCount(7890L);

  private static final Criteria CLIENT_SURVEY_CONCEPT_1 =
      new Criteria()
          .conceptId(987L)
          .name("a concept")
          .isStandard(true)
          .code("conceptA")
          .type("V1")
          .domainId("Observation")
          .childCount(123L)
          .parentCount(123L);

  private static final DbCriteria DB_CRITERIA_1 = makeDbCriteria(CLIENT_CRITERIA_1);
  private static final DbCriteria DB_CRITERIA_2 = makeDbCriteria(CLIENT_CRITERIA_2);
  private static final DbCriteria DB_CRITERIA_3 = makeDbCriteria(CLIENT_CRITERIA_3);
  private static final DbCriteria DB_CRITERIA_4 = makeDbCriteria(CLIENT_CRITERIA_4);

  private static final String USER_EMAIL = "bob@gmail.com";
  private static final String WORKSPACE_NAMESPACE = "ns";
  private static final String WORKSPACE_NAME = "name";
  private static final String WORKSPACE_NAME_2 = "name2";
  private static final Instant NOW = Instant.now();
  private static final FakeClock CLOCK = new FakeClock(NOW, ZoneId.systemDefault());
  private static DbUser currentUser;
  private Workspace workspace;
  private Workspace workspace2;

  @Autowired BillingProjectBufferService billingProjectBufferService;

  @Autowired AccessTierDao accessTierDao;

  @Autowired CdrVersionDao cdrVersionDao;

  @Autowired CBCriteriaDao cbCriteriaDao;

  @Autowired UserDao userDao;

  @Autowired FireCloudService fireCloudService;

  @Autowired ConceptSetsController conceptSetsController;

  @Autowired ConceptBigQueryService conceptBigQueryService;

  @Autowired WorkspacesController workspacesController;

  @TestConfiguration
  @Import({
    CommonMappers.class,
    CohortBuilderMapperImpl.class,
    CohortBuilderServiceImpl.class,
    ConceptSetMapperImpl.class,
    ConceptSetService.class,
    ConceptSetsController.class,
    LogsBasedMetricServiceFakeImpl.class,
    UserMapperImpl.class,
    UserServiceTestConfiguration.class,
    WorkspaceMapperImpl.class,
    WorkspaceResourcesServiceImpl.class,
    WorkspaceServiceImpl.class,
    WorkspacesController.class,
    AccessTierServiceImpl.class,
  })
  @MockBean({
    BigQueryService.class,
    BillingProjectAuditor.class,
    BillingProjectBufferService.class,
    CloudStorageClient.class,
    CohortCloningService.class,
    CohortFactoryImpl.class,
    CohortMapperImpl.class,
    CohortReviewMapperImpl.class,
    CohortReviewServiceImpl.class,
    CohortService.class,
    CohortQueryBuilder.class,
    ComplianceService.class,
    ConceptBigQueryService.class,
    DataSetMapperImpl.class,
    DataSetService.class,
    DirectoryService.class,
    FireCloudService.class,
    FirecloudMapperImpl.class,
    FreeTierBillingService.class,
    NotebooksService.class,
    UserRecentResourceService.class,
    UserServiceAuditor.class,
    WorkspaceAuditor.class,
  })
  static class Configuration {

    @Bean
    Cloudbilling cloudbilling() {
      return TestMockFactory.createMockedCloudbilling();
    }

    @Bean
    Clock clock() {
      return CLOCK;
    }

    @Bean
    Random random() {
      return new FakeLongRandom(123);
    }

    @Bean
    @Scope("prototype")
    DbUser user() {
      return currentUser;
    }

    @Bean
    WorkbenchConfig workbenchConfig() {
      WorkbenchConfig workbenchConfig = WorkbenchConfig.createEmptyConfig();
      workbenchConfig.billing.accountId = "free-tier";
      return workbenchConfig;
    }
  }

  @Before
  public void setUp() {
    TestMockFactory testMockFactory = new TestMockFactory();

    testMockFactory.stubBufferBillingProject(billingProjectBufferService);
    TestMockFactory.stubCreateFcWorkspace(fireCloudService);

    DbUser user = new DbUser();
    user.setUsername(USER_EMAIL);
    user.setUserId(123L);
    user.setDisabled(false);
    user.setEmailVerificationStatusEnum(EmailVerificationStatus.SUBSCRIBED);
    user = userDao.save(user);
    currentUser = user;

    DbCdrVersion cdrVersion = TestMockFactory.createDefaultCdrVersion(cdrVersionDao, accessTierDao);

    workspace = new Workspace();
    workspace.setName(WORKSPACE_NAME);
    workspace.setNamespace(WORKSPACE_NAMESPACE);
    workspace.setResearchPurpose(new ResearchPurpose());
    workspace.setCdrVersionId(String.valueOf(cdrVersion.getCdrVersionId()));
    workspace.setBillingAccountName("billing-account");

    workspace2 = new Workspace();
    workspace2.setName(WORKSPACE_NAME_2);
    workspace2.setNamespace(WORKSPACE_NAMESPACE);
    workspace2.setResearchPurpose(new ResearchPurpose());
    workspace2.setCdrVersionId(String.valueOf(cdrVersion.getCdrVersionId()));
    workspace2.setBillingAccountName("billing-account");

    workspace = workspacesController.createWorkspace(workspace).getBody();
    workspace2 = workspacesController.createWorkspace(workspace2).getBody();
    stubGetWorkspace(workspace.getNamespace(), WORKSPACE_NAME);
    stubGetWorkspaceAcl(workspace.getNamespace(), WORKSPACE_NAME);
    stubGetWorkspace(workspace2.getNamespace(), WORKSPACE_NAME_2);
    stubGetWorkspaceAcl(workspace2.getNamespace(), WORKSPACE_NAME_2);

    FirecloudWorkspaceResponse fcResponse = new FirecloudWorkspaceResponse();
    fcResponse.setAccessLevel(WorkspaceAccessLevel.OWNER.name());
    when(fireCloudService.getWorkspace(workspace.getNamespace(), WORKSPACE_NAME))
        .thenReturn(fcResponse);
    when(fireCloudService.getWorkspace(workspace2.getNamespace(), WORKSPACE_NAME_2))
        .thenReturn(fcResponse);
  }

  @Test
  public void testGetConceptSetsInWorkspaceEmpty() {
    assertThat(
            conceptSetsController
                .getConceptSetsInWorkspace(workspace.getNamespace(), WORKSPACE_NAME)
                .getBody()
                .getItems())
        .isEmpty();
  }

  @Test(expected = NotFoundException.class)
  public void testGetConceptSetNotExists() {
    conceptSetsController.getConceptSet(workspace.getNamespace(), WORKSPACE_NAME, 1L);
  }

  @Test(expected = NotFoundException.class)
  public void testUpdateConceptSetNotExists() {
    ConceptSet conceptSet = new ConceptSet();
    conceptSet.setDescription("desc 1");
    conceptSet.setName("concept set 1");
    conceptSet.setDomain(Domain.CONDITION);
    conceptSet.setId(1L);
    conceptSet.setEtag(Etags.fromVersion(1));

    conceptSetsController.updateConceptSet(
        workspace.getNamespace(), WORKSPACE_NAME, 1L, conceptSet);
  }

  @Test
  public void testCreateConceptSet() {
    saveConcepts();
    ConceptSetConceptId conceptSetConceptId = new ConceptSetConceptId();
    conceptSetConceptId.setConceptId(CLIENT_CRITERIA_1.getConceptId());
    conceptSetConceptId.setStandard(true);
    ConceptSet conceptSet = new ConceptSet();
    conceptSet.setDescription("desc 1");
    conceptSet.setName("concept set 1");
    conceptSet.setDomain(Domain.CONDITION);
    conceptSet =
        conceptSetsController
            .createConceptSet(
                workspace.getNamespace(),
                WORKSPACE_NAME,
                new CreateConceptSetRequest()
                    .conceptSet(conceptSet)
                    .addAddedConceptSetConceptIdsItem(conceptSetConceptId))
            .getBody();
    assertThat(conceptSet.getCriteriums()).isNotNull();
    assertThat(conceptSet.getCreationTime()).isEqualTo(NOW.toEpochMilli());
    assertThat(conceptSet.getDescription()).isEqualTo("desc 1");
    assertThat(conceptSet.getDomain()).isEqualTo(Domain.CONDITION);
    assertThat(conceptSet.getEtag()).isEqualTo(Etags.fromVersion(1));
    assertThat(conceptSet.getLastModifiedTime()).isEqualTo(NOW.toEpochMilli());
    assertThat(conceptSet.getName()).isEqualTo("concept set 1");
  }

  @Test
  public void testGetConceptSet() {
    ConceptSet surveyConceptSet = makeSurveyConceptSet1();
    surveyConceptSet.setParticipantCount(0);
    assertThat(
            conceptSetsController
                .getConceptSet(workspace.getNamespace(), WORKSPACE_NAME, surveyConceptSet.getId())
                .getBody())
        .isEqualTo(surveyConceptSet);
  }

  @Test
  public void testGetConceptSetsInWorkspace() {
    ConceptSet surveyConceptSet = makeSurveyConceptSet1();
    assertThat(
            conceptSetsController
                .getConceptSetsInWorkspace(workspace.getNamespace(), WORKSPACE_NAME)
                .getBody()
                .getItems())
        .contains(surveyConceptSet);
    assertThat(
            conceptSetsController
                .getConceptSetsInWorkspace(workspace2.getNamespace(), WORKSPACE_NAME_2)
                .getBody()
                .getItems())
        .isEmpty();
  }

  @Test(expected = NotFoundException.class)
  public void testGetSurveyConceptSetWrongConceptId() {
    makeSurveyConceptSet1();
    conceptSetsController.getConceptSet(workspace2.getNamespace(), WORKSPACE_NAME_2, 99L);
  }

  @Test(expected = NotFoundException.class)
  public void testGetConceptSetWrongConceptSetId() {
    makeConceptSet1();
    conceptSetsController.getConceptSet(workspace2.getNamespace(), WORKSPACE_NAME_2, 99L);
  }

  @Test
  public void testUpdateConceptSet() {
    cbCriteriaDao.save(DB_CRITERIA_1);
    ConceptSet conceptSet = makeConceptSet1();
    conceptSet.setDescription("new description");
    conceptSet.setName("new name");
    Instant newInstant = NOW.plusMillis(1);
    CLOCK.setInstant(newInstant);
    ConceptSet updatedConceptSet =
        conceptSetsController
            .updateConceptSet(
                workspace.getNamespace(), WORKSPACE_NAME, conceptSet.getId(), conceptSet)
            .getBody();
    assertThat(updatedConceptSet.getCreator()).isEqualTo(USER_EMAIL);
    assertThat(updatedConceptSet.getCriteriums()).isNotNull();
    assertThat(updatedConceptSet.getCreationTime()).isEqualTo(NOW.toEpochMilli());
    assertThat(updatedConceptSet.getDescription()).isEqualTo("new description");
    assertThat(updatedConceptSet.getDomain()).isEqualTo(Domain.CONDITION);
    assertThat(updatedConceptSet.getEtag()).isEqualTo(Etags.fromVersion(2));
    assertThat(updatedConceptSet.getLastModifiedTime()).isEqualTo(newInstant.toEpochMilli());
    assertThat(conceptSet.getName()).isEqualTo("new name");
  }

  @Test(expected = ConflictException.class)
  public void testUpdateConceptSetDomainChange() {
    ConceptSet conceptSet = makeConceptSet1();
    conceptSet.setDescription("new description");
    conceptSet.setName("new name");
    conceptSet.setDomain(Domain.DEATH);
    conceptSetsController.updateConceptSet(
        workspace.getNamespace(), WORKSPACE_NAME, conceptSet.getId(), conceptSet);
  }

  @Test(expected = ConflictException.class)
  public void testUpdateConceptSetWrongEtag() {
    ConceptSet conceptSet = makeConceptSet1();
    conceptSet.setDescription("new description");
    conceptSet.setName("new name");
    conceptSet.setEtag(Etags.fromVersion(2));
    conceptSetsController.updateConceptSet(
        workspace.getNamespace(), WORKSPACE_NAME, conceptSet.getId(), conceptSet);
  }

  @Test
  public void testUpdateConceptSetConceptsAddAndRemove() {
    saveConcepts();
    DbConceptSetConceptId dbConceptSetConceptId1 =
        DbConceptSetConceptId.builder()
            .addConceptId(CLIENT_CRITERIA_1.getConceptId())
            .addStandard(true)
            .build();
    DbConceptSetConceptId dbConceptSetConceptId2 =
        DbConceptSetConceptId.builder()
            .addConceptId(CLIENT_CRITERIA_3.getConceptId())
            .addStandard(true)
            .build();
    when(conceptBigQueryService.getParticipantCountForConcepts(
            Domain.CONDITION, ImmutableSet.of(dbConceptSetConceptId1)))
        .thenReturn(123);
    when(conceptBigQueryService.getParticipantCountForConcepts(
            Domain.CONDITION, ImmutableSet.of(dbConceptSetConceptId1, dbConceptSetConceptId2)))
        .thenReturn(246);
    ConceptSet conceptSet = makeConceptSet1();
    ConceptSet updated =
        conceptSetsController
            .updateConceptSetConcepts(
                workspace.getNamespace(),
                WORKSPACE_NAME,
                conceptSet.getId(),
                addConceptsRequest(conceptSet.getEtag(), CLIENT_CRITERIA_3.getConceptId()))
            .getBody();
    assertThat(updated.getCriteriums()).hasSize(2);
    assertThat(updated.getCriteriums().get(0).getConceptId())
        .isEqualTo(CLIENT_CRITERIA_1.getConceptId());
    assertThat(updated.getEtag()).isNotEqualTo(conceptSet.getEtag());

    ConceptSet removed =
        conceptSetsController
            .updateConceptSetConcepts(
                workspace.getNamespace(),
                WORKSPACE_NAME,
                conceptSet.getId(),
                removeConceptsRequest(updated.getEtag(), CLIENT_CRITERIA_3.getConceptId()))
            .getBody();
    assertThat(removed.getCriteriums()).hasSize(1);
    assertThat(removed.getEtag()).isNotEqualTo(conceptSet.getEtag());
    assertThat(removed.getEtag()).isNotEqualTo(updated.getEtag());
  }

  @Test
  public void testUpdateConceptSetConceptsAddMany() {
    saveConcepts();
    DbConceptSetConceptId dbConceptSetConceptId1 =
        DbConceptSetConceptId.builder()
            .addConceptId(CLIENT_CRITERIA_1.getConceptId())
            .addStandard(true)
            .build();
    DbConceptSetConceptId dbConceptSetConceptId2 =
        DbConceptSetConceptId.builder()
            .addConceptId(CLIENT_CRITERIA_3.getConceptId())
            .addStandard(true)
            .build();
    DbConceptSetConceptId dbConceptSetConceptId3 =
        DbConceptSetConceptId.builder()
            .addConceptId(CLIENT_CRITERIA_4.getConceptId())
            .addStandard(true)
            .build();
    ConceptSetService.MAX_CONCEPTS_PER_SET = 1000;
    when(conceptBigQueryService.getParticipantCountForConcepts(
            Domain.CONDITION,
            ImmutableSet.of(
                dbConceptSetConceptId1, dbConceptSetConceptId2, dbConceptSetConceptId3)))
        .thenReturn(456);
    ConceptSet conceptSet = makeConceptSet1();
    ConceptSet updated =
        conceptSetsController
            .updateConceptSetConcepts(
                workspace.getNamespace(),
                WORKSPACE_NAME,
                conceptSet.getId(),
                addConceptsRequest(
                    conceptSet.getEtag(),
                    CLIENT_CRITERIA_1.getConceptId(),
                    CLIENT_CRITERIA_3.getConceptId(),
                    CLIENT_CRITERIA_4.getConceptId()))
            .getBody();
    assertThat(updated.getCriteriums()).hasSize(3);
    assertThat(updated.getCriteriums().get(0).getConceptId())
        .isEqualTo(CLIENT_CRITERIA_1.getConceptId());
    assertThat(updated.getCriteriums().get(1).getConceptId())
        .isEqualTo(CLIENT_CRITERIA_4.getConceptId());
    assertThat(updated.getCriteriums().get(2).getConceptId())
        .isEqualTo(CLIENT_CRITERIA_3.getConceptId());
    assertThat(updated.getEtag()).isNotEqualTo(conceptSet.getEtag());

    when(conceptBigQueryService.getParticipantCountForConcepts(
            Domain.CONDITION, ImmutableSet.of(dbConceptSetConceptId1)))
        .thenReturn(123);
    ConceptSet removed =
        conceptSetsController
            .updateConceptSetConcepts(
                workspace.getNamespace(),
                WORKSPACE_NAME,
                conceptSet.getId(),
                removeConceptsRequest(
                    updated.getEtag(),
                    CLIENT_CRITERIA_3.getConceptId(),
                    CLIENT_CRITERIA_4.getConceptId()))
            .getBody();
    assertThat(removed.getCriteriums()).hasSize(1);
    assertThat(removed.getCriteriums().get(0).getConceptId())
        .isEqualTo(CLIENT_CRITERIA_1.getConceptId());
    assertThat(removed.getEtag()).isNotEqualTo(conceptSet.getEtag());
    assertThat(removed.getEtag()).isNotEqualTo(updated.getEtag());
  }

  @Test
  public void testUpdateConceptSetConceptsAddManyOnCreate() {
    saveConcepts();
    DbConceptSetConceptId dbConceptSetConceptId1 =
        DbConceptSetConceptId.builder()
            .addConceptId(CLIENT_CRITERIA_1.getConceptId())
            .addStandard(true)
            .build();
    DbConceptSetConceptId dbConceptSetConceptId2 =
        DbConceptSetConceptId.builder()
            .addConceptId(CLIENT_CRITERIA_3.getConceptId())
            .addStandard(true)
            .build();
    DbConceptSetConceptId dbConceptSetConceptId3 =
        DbConceptSetConceptId.builder()
            .addConceptId(CLIENT_CRITERIA_4.getConceptId())
            .addStandard(true)
            .build();
    when(conceptBigQueryService.getParticipantCountForConcepts(
            Domain.CONDITION,
            ImmutableSet.of(
                dbConceptSetConceptId1, dbConceptSetConceptId2, dbConceptSetConceptId3)))
        .thenReturn(456);
    ConceptSet conceptSet =
        makeConceptSet1(
            CLIENT_CRITERIA_1.getConceptId(),
            CLIENT_CRITERIA_3.getConceptId(),
            CLIENT_CRITERIA_4.getConceptId());
    assertThat(conceptSet.getCriteriums()).hasSize(3);
    assertThat(conceptSet.getCriteriums().get(0).getConceptId())
        .isEqualTo(CLIENT_CRITERIA_1.getConceptId());
    assertThat(conceptSet.getCriteriums().get(1).getConceptId())
        .isEqualTo(CLIENT_CRITERIA_4.getConceptId());
    assertThat(conceptSet.getCriteriums().get(2).getConceptId())
        .isEqualTo(CLIENT_CRITERIA_3.getConceptId());
  }

  @Test(expected = ConflictException.class)
  public void testUpdateConceptSetConceptsAddTooMany() {
    saveConcepts();
    ConceptSet conceptSet = makeConceptSet1();
    ConceptSetService.MAX_CONCEPTS_PER_SET = 2;
    conceptSetsController
        .updateConceptSetConcepts(
            workspace.getNamespace(),
            WORKSPACE_NAME,
            conceptSet.getId(),
            addConceptsRequest(
                conceptSet.getEtag(),
                CLIENT_CRITERIA_1.getConceptId(),
                CLIENT_CRITERIA_3.getConceptId(),
                CLIENT_CRITERIA_4.getConceptId()))
        .getBody();
  }

  @Test(expected = ConflictException.class)
  public void testUpdateConceptSetConceptsWrongEtag() {
    saveConcepts();
    ConceptSet conceptSet = makeConceptSet1();
    conceptSetsController.updateConceptSetConcepts(
        workspace.getNamespace(),
        WORKSPACE_NAME,
        conceptSet.getId(),
        addConceptsRequest(Etags.fromVersion(2), CLIENT_CRITERIA_1.getConceptId()));
  }

  @Test
  public void testDeleteConceptSet() {
    saveConcepts();
    ConceptSetService.MAX_CONCEPTS_PER_SET = 1000;
    ConceptSet conceptSet1 = makeConceptSet1();
    ConceptSet conceptSet2 = makeConceptSet2();
    ConceptSet updatedConceptSet =
        conceptSetsController
            .updateConceptSetConcepts(
                workspace.getNamespace(),
                WORKSPACE_NAME,
                conceptSet1.getId(),
                addConceptsRequest(
                    conceptSet1.getEtag(),
                    CLIENT_CRITERIA_1.getConceptId(),
                    CLIENT_CRITERIA_3.getConceptId(),
                    CLIENT_CRITERIA_4.getConceptId()))
            .getBody();
    ConceptSet updatedConceptSet2 =
        conceptSetsController
            .updateConceptSetConcepts(
                workspace.getNamespace(),
                WORKSPACE_NAME,
                conceptSet2.getId(),
                addConceptsRequest(conceptSet2.getEtag(), CLIENT_CRITERIA_2.getConceptId()))
            .getBody();
    assertThat(updatedConceptSet.getCriteriums()).hasSize(3);
    assertThat(updatedConceptSet.getCriteriums().get(0).getConceptId())
        .isEqualTo(CLIENT_CRITERIA_1.getConceptId());
    assertThat(updatedConceptSet.getCriteriums().get(1).getConceptId())
        .isEqualTo(CLIENT_CRITERIA_4.getConceptId());
    assertThat(updatedConceptSet.getCriteriums().get(2).getConceptId())
        .isEqualTo(CLIENT_CRITERIA_3.getConceptId());
    assertThat(updatedConceptSet2.getCriteriums()).hasSize(1);
    assertThat(updatedConceptSet2.getCriteriums().get(0).getConceptId())
        .isEqualTo(CLIENT_CRITERIA_2.getConceptId());

    conceptSetsController.deleteConceptSet(
        workspace.getNamespace(), WORKSPACE_NAME, conceptSet1.getId());
    try {
      conceptSetsController.getConceptSet(
          workspace.getNamespace(), WORKSPACE_NAME, conceptSet1.getId());
      fail("NotFoundException expected");
    } catch (NotFoundException e) {
      // expected
    }
    conceptSet2 =
        conceptSetsController
            .getConceptSet(workspace.getNamespace(), WORKSPACE_NAME, conceptSet2.getId())
            .getBody();
    assertThat(conceptSet2.getCriteriums()).hasSize(1);
    assertThat(conceptSet2.getCriteriums().get(0).getConceptId())
        .isEqualTo(CLIENT_CRITERIA_2.getConceptId());
  }

  private UpdateConceptSetRequest addConceptsRequest(String etag, Long... conceptIds) {
    List<ConceptSetConceptId> conceptSetConceptIdList =
        Arrays.stream(conceptIds)
            .map(
                c -> {
                  ConceptSetConceptId conceptSetConceptId = new ConceptSetConceptId();
                  conceptSetConceptId.setConceptId(c);
                  conceptSetConceptId.setStandard(true);
                  return conceptSetConceptId;
                })
            .collect(Collectors.toList());
    UpdateConceptSetRequest request = new UpdateConceptSetRequest();
    request.setEtag(etag);
    request.setAddedConceptSetConceptIds(conceptSetConceptIdList);
    return request;
  }

  private UpdateConceptSetRequest removeConceptsRequest(String etag, Long... conceptIds) {
    List<ConceptSetConceptId> conceptSetConceptIdList =
        Arrays.stream(conceptIds)
            .map(
                c -> {
                  ConceptSetConceptId conceptSetConceptId = new ConceptSetConceptId();
                  conceptSetConceptId.setConceptId(c);
                  conceptSetConceptId.setStandard(true);
                  return conceptSetConceptId;
                })
            .collect(Collectors.toList());
    UpdateConceptSetRequest request = new UpdateConceptSetRequest();
    request.setEtag(etag);
    request.setRemovedConceptSetConceptIds(conceptSetConceptIdList);
    return request;
  }

  private ConceptSet makeSurveyConceptSet1(Long... addedIds) {
    List<ConceptSetConceptId> conceptSetConceptIdList =
        Arrays.stream(addedIds)
            .map(
                c -> {
                  ConceptSetConceptId conceptSetConceptId = new ConceptSetConceptId();
                  conceptSetConceptId.setConceptId(c);
                  conceptSetConceptId.setStandard(true);
                  return conceptSetConceptId;
                })
            .collect(Collectors.toList());
    ConceptSetConceptId conceptSetConceptId = new ConceptSetConceptId();
    conceptSetConceptId.setConceptId(CLIENT_SURVEY_CONCEPT_1.getConceptId());
    conceptSetConceptId.setStandard(true);
    ConceptSet conceptSet = new ConceptSet();
    conceptSet.setDescription("description 1");
    conceptSet.setName("Survey Concept set 1");
    conceptSet.setDomain(Domain.OBSERVATION);
    conceptSet.setSurvey(Surveys.THE_BASICS);
    CreateConceptSetRequest request =
        new CreateConceptSetRequest()
            .conceptSet(conceptSet)
            .addAddedConceptSetConceptIdsItem(conceptSetConceptId);
    if (addedIds.length > 0) {
      request = request.addedConceptSetConceptIds(conceptSetConceptIdList);
    }
    return conceptSetsController
        .createConceptSet(workspace.getNamespace(), WORKSPACE_NAME, request)
        .getBody();
  }

  private ConceptSet makeConceptSet1(Long... addedIds) {
    List<ConceptSetConceptId> conceptSetConceptIdList =
        Arrays.stream(addedIds)
            .map(c -> new ConceptSetConceptId().conceptId(c).standard(true))
            .collect(Collectors.toList());
    ConceptSet conceptSet = new ConceptSet();
    conceptSet.setDescription("desc 1");
    conceptSet.setName("concept set 1");
    conceptSet.setDomain(Domain.CONDITION);
    CreateConceptSetRequest request =
        new CreateConceptSetRequest()
            .conceptSet(conceptSet)
            .addAddedConceptSetConceptIdsItem(
                new ConceptSetConceptId()
                    .conceptId(CLIENT_CRITERIA_1.getConceptId())
                    .standard(true));
    if (addedIds.length > 0) {
      request = request.addedConceptSetConceptIds(conceptSetConceptIdList);
    }
    return conceptSetsController
        .createConceptSet(workspace.getNamespace(), WORKSPACE_NAME, request)
        .getBody();
  }

  private ConceptSet makeConceptSet2() {
    ConceptSetConceptId conceptSetConceptId = new ConceptSetConceptId();
    conceptSetConceptId.setConceptId(CLIENT_CRITERIA_2.getConceptId());
    conceptSetConceptId.setStandard(true);
    ConceptSet conceptSet = new ConceptSet();
    conceptSet.setDescription("desc 2");
    conceptSet.setName("concept set 2");
    conceptSet.setDomain(Domain.MEASUREMENT);
    return conceptSetsController
        .createConceptSet(
            workspace.getNamespace(),
            WORKSPACE_NAME,
            new CreateConceptSetRequest()
                .conceptSet(conceptSet)
                .addAddedConceptSetConceptIdsItem(conceptSetConceptId))
        .getBody();
  }

  private void stubGetWorkspace(String ns, String name) {
    FirecloudWorkspace fcWorkspace = new FirecloudWorkspace();
    fcWorkspace.setNamespace(ns);
    fcWorkspace.setName(name);
    fcWorkspace.setCreatedBy(USER_EMAIL);
    FirecloudWorkspaceResponse fcResponse = new FirecloudWorkspaceResponse();
    fcResponse.setWorkspace(fcWorkspace);
    fcResponse.setAccessLevel(WorkspaceAccessLevel.OWNER.toString());
    when(fireCloudService.getWorkspace(ns, name)).thenReturn(fcResponse);
  }

  private void stubGetWorkspaceAcl(String ns, String name) {
    FirecloudWorkspaceACL workspaceAccessLevelResponse = new FirecloudWorkspaceACL();
    FirecloudWorkspaceAccessEntry accessLevelEntry =
        new FirecloudWorkspaceAccessEntry().accessLevel(WorkspaceAccessLevel.OWNER.toString());
    Map<String, FirecloudWorkspaceAccessEntry> userEmailToAccessEntry =
        ImmutableMap.of(USER_EMAIL, accessLevelEntry);
    workspaceAccessLevelResponse.setAcl(userEmailToAccessEntry);
    when(fireCloudService.getWorkspaceAclAsService(ns, name))
        .thenReturn(workspaceAccessLevelResponse);
  }

  private void saveConcepts() {
    cbCriteriaDao.save(DB_CRITERIA_1);
    cbCriteriaDao.save(DB_CRITERIA_2);
    cbCriteriaDao.save(DB_CRITERIA_3);
    cbCriteriaDao.save(DB_CRITERIA_4);
  }

  private static DbCriteria makeDbCriteria(Criteria criteria) {
    return DbCriteria.builder()
        .addConceptId(criteria.getConceptId().toString())
        .addParentCount(criteria.getParentCount())
        .addChildCount(criteria.getChildCount())
        .addType(criteria.getType())
        .addDomainId(criteria.getDomainId())
        .addCode(criteria.getCode())
        .addStandard(criteria.getIsStandard())
        .addName(criteria.getName())
        .addFullText("+[" + criteria.getDomainId() + "_rank1]")
        .build();
  }
}
