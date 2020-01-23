package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;
import static org.pmiops.workbench.api.ConceptsControllerTest.makeConcept;

import com.google.api.services.cloudbilling.Cloudbilling;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
import javax.inject.Provider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.actionaudit.auditors.UserServiceAuditor;
import org.pmiops.workbench.actionaudit.auditors.WorkspaceAuditor;
import org.pmiops.workbench.billing.BillingProjectBufferService;
import org.pmiops.workbench.cdr.ConceptBigQueryService;
import org.pmiops.workbench.cdr.dao.ConceptDao;
import org.pmiops.workbench.cdr.model.DbConcept;
import org.pmiops.workbench.cohorts.CohortCloningService;
import org.pmiops.workbench.cohorts.CohortFactoryImpl;
import org.pmiops.workbench.compliance.ComplianceService;
import org.pmiops.workbench.conceptset.ConceptSetService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.ConceptSetDao;
import org.pmiops.workbench.db.dao.DataSetService;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserRecentResourceService;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.dao.UserServiceImpl;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspace;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceACL;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceAccessEntry;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceResponse;
import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.google.DirectoryService;
import org.pmiops.workbench.model.Concept;
import org.pmiops.workbench.model.ConceptSet;
import org.pmiops.workbench.model.CreateConceptSetRequest;
import org.pmiops.workbench.model.DataAccessLevel;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.EmailVerificationStatus;
import org.pmiops.workbench.model.ResearchPurpose;
import org.pmiops.workbench.model.Surveys;
import org.pmiops.workbench.model.UpdateConceptSetRequest;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.notebooks.NotebooksService;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.test.FakeLongRandom;
import org.pmiops.workbench.utils.TestMockFactory;
import org.pmiops.workbench.utils.WorkspaceMapperImpl;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.pmiops.workbench.workspaces.WorkspaceServiceImpl;
import org.pmiops.workbench.workspaces.WorkspacesController;
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

  private static final Concept CLIENT_CONCEPT_1 =
      new Concept()
          .conceptId(123L)
          .conceptName("a concept")
          .standardConcept(true)
          .conceptCode("conceptA")
          .conceptClassId("classId")
          .vocabularyId("V1")
          .domainId("Condition")
          .countValue(123L)
          .prevalence(0.2F)
          .conceptSynonyms(new ArrayList<String>());

  private static final Concept CLIENT_CONCEPT_2 =
      new Concept()
          .conceptId(456L)
          .standardConcept(false)
          .conceptName("b concept")
          .conceptCode("conceptB")
          .conceptClassId("classId2")
          .vocabularyId("V2")
          .domainId("Measurement")
          .countValue(456L)
          .prevalence(0.3F)
          .conceptSynonyms(new ArrayList<String>());

  private static final Concept CLIENT_CONCEPT_3 =
      new Concept()
          .conceptId(789L)
          .standardConcept(false)
          .conceptName("multi word concept")
          .conceptCode("conceptC")
          .conceptClassId("classId3")
          .vocabularyId("V3")
          .domainId("Condition")
          .countValue(789L)
          .prevalence(0.4F)
          .conceptSynonyms(new ArrayList<String>());

  private static final Concept CLIENT_CONCEPT_4 =
      new Concept()
          .conceptId(7890L)
          .standardConcept(false)
          .conceptName("conceptD test concept")
          .standardConcept(true)
          .conceptCode("conceptE")
          .conceptClassId("classId5")
          .vocabularyId("V5")
          .domainId("Condition")
          .countValue(7890L)
          .prevalence(0.9F)
          .conceptSynonyms(new ArrayList<String>());

  private static final Concept CLIENT_SURVEY_CONCEPT_1 =
      new Concept()
          .conceptId(987L)
          .conceptName("a concept")
          .standardConcept(true)
          .conceptCode("conceptA")
          .conceptClassId("classId")
          .vocabularyId("V1")
          .domainId("Observation")
          .countValue(123L)
          .prevalence(0.2F)
          .conceptSynonyms(new ArrayList<String>());

  private static final DbConcept CONCEPT_1 = makeConcept(CLIENT_CONCEPT_1);
  private static final DbConcept CONCEPT_2 = makeConcept(CLIENT_CONCEPT_2);
  private static final DbConcept CONCEPT_3 = makeConcept(CLIENT_CONCEPT_3);
  private static final DbConcept CONCEPT_4 = makeConcept(CLIENT_CONCEPT_4);

  private static final String USER_EMAIL = "bob@gmail.com";
  private static final String WORKSPACE_NAMESPACE = "ns";
  private static final String WORKSPACE_NAME = "name";
  private static final String WORKSPACE_NAME_2 = "name2";
  private static final Instant NOW = Instant.now();
  private static final FakeClock CLOCK = new FakeClock(NOW, ZoneId.systemDefault());
  private static DbUser currentUser;
  private Workspace workspace;
  private Workspace workspace2;
  private TestMockFactory testMockFactory;

  @Autowired BillingProjectBufferService billingProjectBufferService;

  @Autowired WorkspaceService workspaceService;

  @Autowired ConceptSetDao conceptSetDao;

  @Autowired CdrVersionDao cdrVersionDao;

  @Autowired ConceptDao conceptDao;

  @Autowired DataSetService dataSetService;

  @Autowired WorkspaceDao workspaceDao;

  @Autowired UserDao userDao;

  @Autowired CloudStorageService cloudStorageService;

  @Autowired NotebooksService notebooksService;

  @Autowired UserService userService;

  @Autowired FireCloudService fireCloudService;

  @Autowired ConceptSetsController conceptSetsController;

  @Autowired UserRecentResourceService userRecentResourceService;

  @Autowired ConceptBigQueryService conceptBigQueryService;

  @Autowired Provider<WorkbenchConfig> workbenchConfigProvider;

  @Autowired WorkspacesController workspacesController;

  @Autowired WorkspaceAuditor workspaceAuditor;

  @TestConfiguration
  @Import({
    WorkspaceServiceImpl.class,
    CohortCloningService.class,
    CohortFactoryImpl.class,
    UserServiceImpl.class,
    ConceptSetsController.class,
    WorkspacesController.class,
    ConceptSetService.class,
    WorkspaceMapperImpl.class
  })
  @MockBean({
    BillingProjectBufferService.class,
    CloudStorageService.class,
    ComplianceService.class,
    ConceptBigQueryService.class,
    ConceptSetService.class,
    DataSetService.class,
    DirectoryService.class,
    FireCloudService.class,
    NotebooksService.class,
    UserRecentResourceService.class,
    WorkspaceAuditor.class,
    UserServiceAuditor.class
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
      WorkbenchConfig workbenchConfig = new WorkbenchConfig();
      workbenchConfig.featureFlags = new WorkbenchConfig.FeatureFlagsConfig();
      return workbenchConfig;
    }
  }

  @Before
  public void setUp() throws Exception {
    testMockFactory = new TestMockFactory();

    testMockFactory.stubBufferBillingProject(billingProjectBufferService);
    testMockFactory.stubCreateFcWorkspace(fireCloudService);

    DbUser user = new DbUser();
    user.setUsername(USER_EMAIL);
    user.setUserId(123L);
    user.setDisabled(false);
    user.setEmailVerificationStatusEnum(EmailVerificationStatus.SUBSCRIBED);
    user = userDao.save(user);
    currentUser = user;

    DbCdrVersion cdrVersion = new DbCdrVersion();
    cdrVersion.setName("1");
    // set the db name to be empty since test cases currently
    // run in the workbench schema only.
    cdrVersion.setCdrDbName("");
    cdrVersion = cdrVersionDao.save(cdrVersion);

    workspace = new Workspace();
    workspace.setName(WORKSPACE_NAME);
    workspace.setNamespace(WORKSPACE_NAMESPACE);
    workspace.setDataAccessLevel(DataAccessLevel.PROTECTED);
    workspace.setResearchPurpose(new ResearchPurpose());
    workspace.setCdrVersionId(String.valueOf(cdrVersion.getCdrVersionId()));
    workspace.setBillingAccountName("billing-account");

    workspace2 = new Workspace();
    workspace2.setName(WORKSPACE_NAME_2);
    workspace2.setNamespace(WORKSPACE_NAMESPACE);
    workspace2.setDataAccessLevel(DataAccessLevel.PROTECTED);
    workspace2.setResearchPurpose(new ResearchPurpose());
    workspace2.setCdrVersionId(String.valueOf(cdrVersion.getCdrVersionId()));
    workspace2.setBillingAccountName("billing-account");

    workspace = workspacesController.createWorkspace(workspace).getBody();
    workspace2 = workspacesController.createWorkspace(workspace2).getBody();
    stubGetWorkspace(
        workspace.getNamespace(), WORKSPACE_NAME, USER_EMAIL, WorkspaceAccessLevel.OWNER);
    stubGetWorkspaceAcl(
        workspace.getNamespace(), WORKSPACE_NAME, USER_EMAIL, WorkspaceAccessLevel.OWNER);
    stubGetWorkspace(
        workspace2.getNamespace(), WORKSPACE_NAME_2, USER_EMAIL, WorkspaceAccessLevel.OWNER);
    stubGetWorkspaceAcl(
        workspace2.getNamespace(), WORKSPACE_NAME_2, USER_EMAIL, WorkspaceAccessLevel.OWNER);

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
                    .addAddedIdsItem(CLIENT_CONCEPT_1.getConceptId()))
            .getBody();
    assertThat(conceptSet.getConcepts()).isNotNull();
    assertThat(conceptSet.getCreationTime()).isEqualTo(NOW.toEpochMilli());
    assertThat(conceptSet.getDescription()).isEqualTo("desc 1");
    assertThat(conceptSet.getDomain()).isEqualTo(Domain.CONDITION);
    assertThat(conceptSet.getEtag()).isEqualTo(Etags.fromVersion(1));
    assertThat(conceptSet.getLastModifiedTime()).isEqualTo(NOW.toEpochMilli());
    assertThat(conceptSet.getName()).isEqualTo("concept set 1");
    assertThat(conceptSet.getParticipantCount()).isEqualTo(0);

    assertThat(
            conceptSetsController
                .getConceptSet(workspace.getNamespace(), WORKSPACE_NAME, conceptSet.getId())
                .getBody())
        .isEqualTo(conceptSet);
    // Get concept sets will not return the full information, because concepts can have a lot of
    // information.
    assertThat(
            conceptSetsController
                .getConceptSetsInWorkspace(workspace.getNamespace(), WORKSPACE_NAME)
                .getBody()
                .getItems())
        .contains(conceptSet.concepts(null));
    assertThat(
            conceptSetsController
                .getConceptSetsInWorkspace(workspace2.getNamespace(), WORKSPACE_NAME_2)
                .getBody()
                .getItems())
        .isEmpty();
  }

  @Test
  public void testGetSurveyConceptSet() {
    ConceptSet surveyConceptSet = makeSurveyConceptSet1();
    assertThat(
            conceptSetsController
                .getConceptSet(workspace.getNamespace(), WORKSPACE_NAME, surveyConceptSet.getId())
                .getBody())
        .isEqualTo(surveyConceptSet);
    assertThat(
            conceptSetsController
                .getConceptSetsInWorkspace(workspace.getNamespace(), WORKSPACE_NAME)
                .getBody()
                .getItems())
        .contains(surveyConceptSet.concepts(null));
    assertThat(
            conceptSetsController
                .getConceptSetsInWorkspace(workspace2.getNamespace(), WORKSPACE_NAME_2)
                .getBody()
                .getItems())
        .isEmpty();
  }

  @Test(expected = NotFoundException.class)
  public void testGetSurveyConceptSetWrongWorkspace() {
    ConceptSet conceptSet = makeSurveyConceptSet1();
    conceptSetsController.getConceptSet(
        workspace2.getNamespace(), WORKSPACE_NAME_2, conceptSet.getId());
  }

  @Test
  public void testGetConceptSet() {
    ConceptSet conceptSet = makeConceptSet1();
    assertThat(
            conceptSetsController
                .getConceptSet(workspace.getNamespace(), WORKSPACE_NAME, conceptSet.getId())
                .getBody())
        .isEqualTo(conceptSet);
    // Get concept sets will not return the full information, because concepts can have a lot of
    // information.
    assertThat(
            conceptSetsController
                .getConceptSetsInWorkspace(workspace.getNamespace(), WORKSPACE_NAME)
                .getBody()
                .getItems())
        .contains(conceptSet.concepts(null));
    assertThat(
            conceptSetsController
                .getConceptSetsInWorkspace(workspace2.getNamespace(), WORKSPACE_NAME_2)
                .getBody()
                .getItems())
        .isEmpty();
  }

  @Test(expected = NotFoundException.class)
  public void testGetConceptSetWrongWorkspace() {
    ConceptSet conceptSet = makeConceptSet1();
    conceptSetsController.getConceptSet(
        workspace2.getNamespace(), WORKSPACE_NAME_2, conceptSet.getId());
  }

  @Test
  public void testUpdateConceptSet() {
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
    assertThat(updatedConceptSet.getConcepts()).isNotNull();
    assertThat(updatedConceptSet.getCreationTime()).isEqualTo(NOW.toEpochMilli());
    assertThat(updatedConceptSet.getDescription()).isEqualTo("new description");
    assertThat(updatedConceptSet.getDomain()).isEqualTo(Domain.CONDITION);
    assertThat(updatedConceptSet.getEtag()).isEqualTo(Etags.fromVersion(2));
    assertThat(updatedConceptSet.getLastModifiedTime()).isEqualTo(newInstant.toEpochMilli());
    assertThat(updatedConceptSet.getParticipantCount()).isEqualTo(0);
    assertThat(conceptSet.getName()).isEqualTo("new name");

    assertThat(
            conceptSetsController
                .getConceptSet(workspace.getNamespace(), WORKSPACE_NAME, conceptSet.getId())
                .getBody())
        .isEqualTo(updatedConceptSet);
    // Get concept sets will not return the full information, because concepts can have a lot of
    // information.
    assertThat(
            conceptSetsController
                .getConceptSetsInWorkspace(workspace.getNamespace(), WORKSPACE_NAME)
                .getBody()
                .getItems())
        .contains(updatedConceptSet.concepts(null));
    assertThat(
            conceptSetsController
                .getConceptSetsInWorkspace(workspace2.getNamespace(), WORKSPACE_NAME_2)
                .getBody()
                .getItems())
        .isEmpty();
  }

  @Test(expected = BadRequestException.class)
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
    when(conceptBigQueryService.getParticipantCountForConcepts(
            "condition_occurrence", ImmutableSet.of(CLIENT_CONCEPT_1.getConceptId())))
        .thenReturn(123);
    when(conceptBigQueryService.getParticipantCountForConcepts(
            "condition_occurrence",
            ImmutableSet.of(CLIENT_CONCEPT_1.getConceptId(), CLIENT_CONCEPT_3.getConceptId())))
        .thenReturn(246);
    ConceptSet conceptSet = makeConceptSet1();
    ConceptSet updated =
        conceptSetsController
            .updateConceptSetConcepts(
                workspace.getNamespace(),
                WORKSPACE_NAME,
                conceptSet.getId(),
                addConceptsRequest(conceptSet.getEtag(), CLIENT_CONCEPT_3.getConceptId()))
            .getBody();
    assertThat(updated.getConcepts()).contains(CLIENT_CONCEPT_3);
    assertThat(updated.getEtag()).isNotEqualTo(conceptSet.getEtag());
    assertThat(updated.getParticipantCount()).isEqualTo(246);

    ConceptSet removed =
        conceptSetsController
            .updateConceptSetConcepts(
                workspace.getNamespace(),
                WORKSPACE_NAME,
                conceptSet.getId(),
                removeConceptsRequest(updated.getEtag(), CLIENT_CONCEPT_3.getConceptId()))
            .getBody();
    assertThat(removed.getConcepts().size()).isEqualTo(1);
    assertThat(removed.getEtag()).isNotEqualTo(conceptSet.getEtag());
    assertThat(removed.getEtag()).isNotEqualTo(updated.getEtag());
    assertThat(removed.getParticipantCount()).isEqualTo(123);
  }

  @Test
  public void testUpdateConceptSetConceptsAddMany() {
    saveConcepts();
    when(conceptBigQueryService.getParticipantCountForConcepts(
            "condition_occurrence",
            ImmutableSet.of(
                CLIENT_CONCEPT_1.getConceptId(),
                CLIENT_CONCEPT_3.getConceptId(),
                CLIENT_CONCEPT_4.getConceptId())))
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
                    CLIENT_CONCEPT_1.getConceptId(),
                    CLIENT_CONCEPT_3.getConceptId(),
                    CLIENT_CONCEPT_4.getConceptId()))
            .getBody();
    assertThat(updated.getConcepts())
        .containsExactly(CLIENT_CONCEPT_1, CLIENT_CONCEPT_4, CLIENT_CONCEPT_3);
    assertThat(updated.getEtag()).isNotEqualTo(conceptSet.getEtag());
    assertThat(updated.getParticipantCount()).isEqualTo(456);

    when(conceptBigQueryService.getParticipantCountForConcepts(
            "condition_occurrence", ImmutableSet.of(CLIENT_CONCEPT_1.getConceptId())))
        .thenReturn(123);
    ConceptSet removed =
        conceptSetsController
            .updateConceptSetConcepts(
                workspace.getNamespace(),
                WORKSPACE_NAME,
                conceptSet.getId(),
                removeConceptsRequest(
                    updated.getEtag(),
                    CLIENT_CONCEPT_3.getConceptId(),
                    CLIENT_CONCEPT_4.getConceptId()))
            .getBody();
    assertThat(removed.getConcepts()).containsExactly(CLIENT_CONCEPT_1);
    assertThat(removed.getEtag()).isNotEqualTo(conceptSet.getEtag());
    assertThat(removed.getEtag()).isNotEqualTo(updated.getEtag());
    assertThat(removed.getParticipantCount()).isEqualTo(123);
  }

  @Test
  public void testUpdateConceptSetConceptsAddManyOnCreate() {
    saveConcepts();
    when(conceptBigQueryService.getParticipantCountForConcepts(
            "condition_occurrence",
            ImmutableSet.of(
                CLIENT_CONCEPT_1.getConceptId(),
                CLIENT_CONCEPT_3.getConceptId(),
                CLIENT_CONCEPT_4.getConceptId())))
        .thenReturn(456);
    ConceptSet conceptSet =
        makeConceptSet1(
            CLIENT_CONCEPT_1.getConceptId(),
            CLIENT_CONCEPT_3.getConceptId(),
            CLIENT_CONCEPT_4.getConceptId());
    assertThat(conceptSet.getConcepts())
        .containsExactly(CLIENT_CONCEPT_1, CLIENT_CONCEPT_4, CLIENT_CONCEPT_3);
    assertThat(conceptSet.getParticipantCount()).isEqualTo(456);
  }

  @Test(expected = BadRequestException.class)
  public void testUpdateConceptSetConceptsAddTooMany() {
    saveConcepts();
    ConceptSet conceptSet = makeConceptSet1();
    conceptSetsController.maxConceptsPerSet = 2;
    conceptSetsController
        .updateConceptSetConcepts(
            workspace.getNamespace(),
            WORKSPACE_NAME,
            conceptSet.getId(),
            addConceptsRequest(
                conceptSet.getEtag(),
                CLIENT_CONCEPT_1.getConceptId(),
                CLIENT_CONCEPT_3.getConceptId(),
                CLIENT_CONCEPT_4.getConceptId()))
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
        addConceptsRequest(Etags.fromVersion(2), CLIENT_CONCEPT_1.getConceptId()));
  }

  @Test(expected = BadRequestException.class)
  public void testUpdateConceptSetConceptsAddWrongDomain() {
    saveConcepts();
    ConceptSet conceptSet = makeConceptSet1();
    conceptSetsController.updateConceptSetConcepts(
        workspace.getNamespace(),
        WORKSPACE_NAME,
        conceptSet.getId(),
        addConceptsRequest(
            conceptSet.getEtag(),
            CLIENT_CONCEPT_1.getConceptId(),
            CLIENT_CONCEPT_2.getConceptId()));
  }

  @Test
  public void testDeleteConceptSet() {
    saveConcepts();
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
                    CLIENT_CONCEPT_1.getConceptId(),
                    CLIENT_CONCEPT_3.getConceptId(),
                    CLIENT_CONCEPT_4.getConceptId()))
            .getBody();
    ConceptSet updatedConceptSet2 =
        conceptSetsController
            .updateConceptSetConcepts(
                workspace.getNamespace(),
                WORKSPACE_NAME,
                conceptSet2.getId(),
                addConceptsRequest(conceptSet2.getEtag(), CLIENT_CONCEPT_2.getConceptId()))
            .getBody();
    assertThat(updatedConceptSet.getConcepts())
        .containsExactly(CLIENT_CONCEPT_1, CLIENT_CONCEPT_3, CLIENT_CONCEPT_4);
    assertThat(updatedConceptSet2.getConcepts()).containsExactly(CLIENT_CONCEPT_2);

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
    assertThat(conceptSet2.getConcepts()).containsExactly(CLIENT_CONCEPT_2);
  }

  @Test(expected = NotFoundException.class)
  public void testDeleteConceptSetNotFound() {
    conceptSetsController.deleteConceptSet(workspace.getNamespace(), WORKSPACE_NAME, 1L);
  }

  private UpdateConceptSetRequest addConceptsRequest(String etag, Long... conceptIds) {
    UpdateConceptSetRequest request = new UpdateConceptSetRequest();
    request.setEtag(etag);
    request.setAddedIds(ImmutableList.copyOf(conceptIds));
    return request;
  }

  private UpdateConceptSetRequest removeConceptsRequest(String etag, Long... conceptIds) {
    UpdateConceptSetRequest request = new UpdateConceptSetRequest();
    request.setEtag(etag);
    request.setRemovedIds(ImmutableList.copyOf(conceptIds));
    return request;
  }

  private ConceptSet makeSurveyConceptSet1(Long... addedIds) {
    ConceptSet conceptSet = new ConceptSet();
    conceptSet.setDescription("description 1");
    conceptSet.setName("Survey Concept set 1");
    conceptSet.setDomain(Domain.OBSERVATION);
    conceptSet.setSurvey(Surveys.THE_BASICS);
    CreateConceptSetRequest request =
        new CreateConceptSetRequest()
            .conceptSet(conceptSet)
            .addAddedIdsItem(CLIENT_SURVEY_CONCEPT_1.getConceptId());
    if (addedIds.length > 0) {
      request = request.addedIds(ImmutableList.copyOf(addedIds));
    }
    return conceptSetsController
        .createConceptSet(workspace.getNamespace(), WORKSPACE_NAME, request)
        .getBody();
  }

  private ConceptSet makeConceptSet1(Long... addedIds) {
    ConceptSet conceptSet = new ConceptSet();
    conceptSet.setDescription("desc 1");
    conceptSet.setName("concept set 1");
    conceptSet.setDomain(Domain.CONDITION);
    CreateConceptSetRequest request =
        new CreateConceptSetRequest()
            .conceptSet(conceptSet)
            .addAddedIdsItem(CLIENT_CONCEPT_1.getConceptId());
    if (addedIds.length > 0) {
      request = request.addedIds(ImmutableList.copyOf(addedIds));
    }
    return conceptSetsController
        .createConceptSet(workspace.getNamespace(), WORKSPACE_NAME, request)
        .getBody();
  }

  private ConceptSet makeConceptSet2() {
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
                .addAddedIdsItem(CLIENT_CONCEPT_2.getConceptId()))
        .getBody();
  }

  private void stubGetWorkspace(String ns, String name, String creator, WorkspaceAccessLevel access)
      throws Exception {
    FirecloudWorkspace fcWorkspace = new FirecloudWorkspace();
    fcWorkspace.setNamespace(ns);
    fcWorkspace.setName(name);
    fcWorkspace.setCreatedBy(creator);
    FirecloudWorkspaceResponse fcResponse = new FirecloudWorkspaceResponse();
    fcResponse.setWorkspace(fcWorkspace);
    fcResponse.setAccessLevel(access.toString());
    when(fireCloudService.getWorkspace(ns, name)).thenReturn(fcResponse);
  }

  private void stubGetWorkspaceAcl(
      String ns, String name, String creator, WorkspaceAccessLevel access) {
    FirecloudWorkspaceACL workspaceAccessLevelResponse = new FirecloudWorkspaceACL();
    FirecloudWorkspaceAccessEntry accessLevelEntry =
        new FirecloudWorkspaceAccessEntry().accessLevel(access.toString());
    Map<String, FirecloudWorkspaceAccessEntry> userEmailToAccessEntry =
        ImmutableMap.of(creator, accessLevelEntry);
    workspaceAccessLevelResponse.setAcl(userEmailToAccessEntry);
    when(fireCloudService.getWorkspaceAcl(ns, name)).thenReturn(workspaceAccessLevelResponse);
  }

  private void saveConcepts() {
    conceptDao.save(CONCEPT_1);
    conceptDao.save(CONCEPT_2);
    conceptDao.save(CONCEPT_3);
    conceptDao.save(CONCEPT_4);
  }
}
