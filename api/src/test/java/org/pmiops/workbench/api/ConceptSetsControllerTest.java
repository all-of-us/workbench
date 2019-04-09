package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;
import static org.pmiops.workbench.api.ConceptsControllerTest.makeConcept;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Random;

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.pmiops.workbench.cdr.ConceptBigQueryService;
import org.pmiops.workbench.cdr.dao.ConceptDao;
import org.pmiops.workbench.cohorts.CohortFactory;
import org.pmiops.workbench.cohorts.CohortFactoryImpl;
import org.pmiops.workbench.compliance.ComplianceService;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.CohortCloningService;
import org.pmiops.workbench.db.dao.ConceptSetDao;
import org.pmiops.workbench.db.dao.ConceptSetService;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserRecentResourceService;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.dao.WorkspaceService;
import org.pmiops.workbench.db.dao.WorkspaceServiceImpl;
import org.pmiops.workbench.db.model.CdrVersion;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.model.Concept;
import org.pmiops.workbench.model.ConceptSet;
import org.pmiops.workbench.model.CreateConceptSetRequest;
import org.pmiops.workbench.model.DataAccessLevel;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.EmailVerificationStatus;
import org.pmiops.workbench.model.ResearchPurpose;
import org.pmiops.workbench.model.UpdateConceptSetRequest;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.test.FakeLongRandom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class ConceptSetsControllerTest {

  private static final Concept CLIENT_CONCEPT_1 = new Concept()
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

  private static final Concept CLIENT_CONCEPT_2 = new Concept()
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

  private static final Concept CLIENT_CONCEPT_3 = new Concept()
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

  private static final Concept CLIENT_CONCEPT_4 = new Concept()
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

  private static final org.pmiops.workbench.cdr.model.Concept CONCEPT_1 =
      makeConcept(CLIENT_CONCEPT_1);
  private static final org.pmiops.workbench.cdr.model.Concept CONCEPT_2 =
      makeConcept(CLIENT_CONCEPT_2);
  private static final org.pmiops.workbench.cdr.model.Concept CONCEPT_3 =
      makeConcept(CLIENT_CONCEPT_3);
  private static final org.pmiops.workbench.cdr.model.Concept CONCEPT_4 =
      makeConcept(CLIENT_CONCEPT_4);

  private static final String USER_EMAIL = "bob@gmail.com";
  private static final String WORKSPACE_NAMESPACE = "ns";
  private static final String WORKSPACE_NAME = "name";
  private static final String WORKSPACE_NAME_2 = "name2";
  private static final Instant NOW = Instant.now();
  private static final FakeClock CLOCK = new FakeClock(NOW, ZoneId.systemDefault());

  @Autowired
  WorkspaceService workspaceService;

  @Autowired
  ConceptSetDao conceptSetDao;

  @Autowired
  CdrVersionDao cdrVersionDao;

  @Autowired
  ConceptDao conceptDao;

  @Autowired
  CohortFactory cohortFactory;

  @Autowired
  CohortDao cohortDao;

  @Autowired
  WorkspaceDao workspaceDao;

  @Autowired
  UserDao userDao;

  @Autowired
  CloudStorageService cloudStorageService;

  @Autowired
  UserService userService;

  @Autowired
  FireCloudService fireCloudService;

  private ConceptSetsController conceptSetsController;

  @Autowired
  UserRecentResourceService userRecentResourceService;

  @Autowired
  ConceptBigQueryService conceptBigQueryService;

  @Mock
  Provider<User> userProvider;


  @TestConfiguration
  @Import({WorkspaceServiceImpl.class, CohortCloningService.class, CohortFactoryImpl.class,
      UserService.class, ConceptSetsController.class, WorkspacesController.class, ConceptSetService.class})
  @MockBean({ConceptBigQueryService.class, FireCloudService.class, CloudStorageService.class,
      ConceptSetService.class, UserRecentResourceService.class, ComplianceService.class})
  static class Configuration {
    @Bean
    Clock clock() {
      return CLOCK;
    }

    @Bean
    Random random() {
      return new FakeLongRandom(123);
    }
  }

  @Before
  public void setUp() throws Exception {

    conceptSetsController = new ConceptSetsController(workspaceService, conceptSetDao, conceptDao,
        conceptBigQueryService, userRecentResourceService, userProvider, CLOCK);
    WorkspacesController workspacesController =
        new WorkspacesController(workspaceService, cdrVersionDao, cohortDao, cohortFactory, conceptSetDao,
                userDao, userProvider, fireCloudService, cloudStorageService, CLOCK, userService,
                userRecentResourceService);

    User user = new User();
    user.setEmail(USER_EMAIL);
    user.setUserId(123L);
    user.setDisabled(false);
    user.setEmailVerificationStatusEnum(EmailVerificationStatus.SUBSCRIBED);
    user = userDao.save(user);
    when(userProvider.get()).thenReturn(user);

    CdrVersion cdrVersion = new CdrVersion();
    cdrVersion.setName("1");
    //set the db name to be empty since test cases currently
    //run in the workbench schema only.
    cdrVersion.setCdrDbName("");
    cdrVersion = cdrVersionDao.save(cdrVersion);

    Workspace workspace = new Workspace();
    workspace.setName(WORKSPACE_NAME);
    workspace.setNamespace(WORKSPACE_NAMESPACE);
    workspace.setDataAccessLevel(DataAccessLevel.PROTECTED);
    workspace.setResearchPurpose(new ResearchPurpose());
    workspace.setCdrVersionId(String.valueOf(cdrVersion.getCdrVersionId()));

    Workspace workspace2 = new Workspace();
    workspace2.setName(WORKSPACE_NAME_2);
    workspace2.setNamespace(WORKSPACE_NAMESPACE);
    workspace2.setDataAccessLevel(DataAccessLevel.PROTECTED);
    workspace2.setResearchPurpose(new ResearchPurpose());
    workspace2.setCdrVersionId(String.valueOf(cdrVersion.getCdrVersionId()));

    workspacesController.setUserProvider(userProvider);
    conceptSetsController.setUserProvider(userProvider);

    stubGetWorkspace(WORKSPACE_NAMESPACE, WORKSPACE_NAME, USER_EMAIL, WorkspaceAccessLevel.OWNER);
    stubGetWorkspace(WORKSPACE_NAMESPACE, WORKSPACE_NAME_2, USER_EMAIL, WorkspaceAccessLevel.OWNER);
    workspacesController.createWorkspace(workspace);
    workspacesController.createWorkspace(workspace2);

    org.pmiops.workbench.firecloud.model.WorkspaceResponse fcResponse =
        new org.pmiops.workbench.firecloud.model.WorkspaceResponse();
    fcResponse.setAccessLevel(WorkspaceAccessLevel.OWNER.name());
    when(fireCloudService.getWorkspace(WORKSPACE_NAMESPACE, WORKSPACE_NAME))
        .thenReturn(fcResponse);
    when(fireCloudService.getWorkspace(WORKSPACE_NAMESPACE, WORKSPACE_NAME_2))
        .thenReturn(fcResponse);
  }

  @Test
  public void testGetConceptSetsInWorkspaceEmpty() {
    assertThat(conceptSetsController.getConceptSetsInWorkspace(WORKSPACE_NAMESPACE, WORKSPACE_NAME)
        .getBody().getItems()).isEmpty();
  }

  @Test(expected = NotFoundException.class)
  public void testGetConceptSetNotExists() {
    conceptSetsController.getConceptSet(WORKSPACE_NAMESPACE, WORKSPACE_NAME, 1L);
  }

  @Test(expected = NotFoundException.class)
  public void testUpdateConceptSetNotExists() {
    ConceptSet conceptSet = new ConceptSet();
    conceptSet.setDescription("desc 1");
    conceptSet.setName("concept set 1");
    conceptSet.setDomain(Domain.CONDITION);
    conceptSet.setId(1L);
    conceptSet.setEtag(Etags.fromVersion(1));

    conceptSetsController.updateConceptSet(WORKSPACE_NAMESPACE, WORKSPACE_NAME, 1L,
        conceptSet);
  }

  @Test
  public void testCreateConceptSet() {
    ConceptSet conceptSet = new ConceptSet();
    conceptSet.setDescription("desc 1");
    conceptSet.setName("concept set 1");
    conceptSet.setDomain(Domain.CONDITION);
    conceptSet = conceptSetsController.createConceptSet(WORKSPACE_NAMESPACE, WORKSPACE_NAME,
        new CreateConceptSetRequest().conceptSet(conceptSet))
        .getBody();
    assertThat(conceptSet.getConcepts()).isNull();
    assertThat(conceptSet.getCreationTime()).isEqualTo(NOW.toEpochMilli());
    assertThat(conceptSet.getDescription()).isEqualTo("desc 1");
    assertThat(conceptSet.getDomain()).isEqualTo(Domain.CONDITION);
    assertThat(conceptSet.getEtag()).isEqualTo(Etags.fromVersion(1));
    assertThat(conceptSet.getLastModifiedTime()).isEqualTo(NOW.toEpochMilli());
    assertThat(conceptSet.getName()).isEqualTo("concept set 1");
    assertThat(conceptSet.getParticipantCount()).isEqualTo(0);

    assertThat(conceptSetsController.getConceptSet(WORKSPACE_NAMESPACE, WORKSPACE_NAME,
        conceptSet.getId()).getBody()).isEqualTo(conceptSet);
    assertThat(conceptSetsController.getConceptSetsInWorkspace(WORKSPACE_NAMESPACE, WORKSPACE_NAME)
        .getBody().getItems()).contains(conceptSet);
    assertThat(conceptSetsController.getConceptSetsInWorkspace(WORKSPACE_NAMESPACE, WORKSPACE_NAME_2)
        .getBody().getItems()).isEmpty();
  }

  @Test
  public void testGetConceptSet() {
    ConceptSet conceptSet = makeConceptSet1();
    assertThat(conceptSetsController.getConceptSet(WORKSPACE_NAMESPACE, WORKSPACE_NAME,
        conceptSet.getId()).getBody()).isEqualTo(conceptSet);
    assertThat(conceptSetsController.getConceptSetsInWorkspace(WORKSPACE_NAMESPACE, WORKSPACE_NAME)
        .getBody().getItems()).contains(conceptSet);
    assertThat(conceptSetsController.getConceptSetsInWorkspace(WORKSPACE_NAMESPACE, WORKSPACE_NAME_2)
        .getBody().getItems()).isEmpty();
  }

  @Test(expected = NotFoundException.class)
  public void testGetConceptSetWrongWorkspace() {
    ConceptSet conceptSet = makeConceptSet1();
    conceptSetsController.getConceptSet(WORKSPACE_NAMESPACE, WORKSPACE_NAME_2,
        conceptSet.getId());
  }

  @Test
  public void testUpdateConceptSet() {
    ConceptSet conceptSet = makeConceptSet1();
    conceptSet.setDescription("new description");
    conceptSet.setName("new name");
    Instant newInstant = NOW.plusMillis(1);
    CLOCK.setInstant(newInstant);
    ConceptSet updatedConceptSet =
        conceptSetsController.updateConceptSet(WORKSPACE_NAMESPACE, WORKSPACE_NAME, conceptSet.getId(),
            conceptSet).getBody();
    assertThat(updatedConceptSet.getCreator()).isEqualTo(USER_EMAIL);
    assertThat(updatedConceptSet.getConcepts()).isNull();
    assertThat(updatedConceptSet.getCreationTime()).isEqualTo(NOW.toEpochMilli());
    assertThat(updatedConceptSet.getDescription()).isEqualTo("new description");
    assertThat(updatedConceptSet.getDomain()).isEqualTo(Domain.CONDITION);
    assertThat(updatedConceptSet.getEtag()).isEqualTo(Etags.fromVersion(2));
    assertThat(updatedConceptSet.getLastModifiedTime()).isEqualTo(newInstant.toEpochMilli());
    assertThat(updatedConceptSet.getParticipantCount()).isEqualTo(0);
    assertThat(conceptSet.getName()).isEqualTo("new name");

    assertThat(conceptSetsController.getConceptSet(WORKSPACE_NAMESPACE, WORKSPACE_NAME,
        conceptSet.getId()).getBody()).isEqualTo(updatedConceptSet);
    assertThat(conceptSetsController.getConceptSetsInWorkspace(WORKSPACE_NAMESPACE, WORKSPACE_NAME)
        .getBody().getItems()).contains(updatedConceptSet);
    assertThat(conceptSetsController.getConceptSetsInWorkspace(WORKSPACE_NAMESPACE, WORKSPACE_NAME_2)
        .getBody().getItems()).isEmpty();
  }

  @Test(expected = BadRequestException.class)
  public void testUpdateConceptSetDomainChange() {
    ConceptSet conceptSet = makeConceptSet1();
    conceptSet.setDescription("new description");
    conceptSet.setName("new name");
    conceptSet.setDomain(Domain.DEATH);
    conceptSetsController.updateConceptSet(WORKSPACE_NAMESPACE, WORKSPACE_NAME, conceptSet.getId(),
        conceptSet);
  }

  @Test(expected = ConflictException.class)
  public void testUpdateConceptSetWrongEtag() {
    ConceptSet conceptSet = makeConceptSet1();
    conceptSet.setDescription("new description");
    conceptSet.setName("new name");
    conceptSet.setEtag(Etags.fromVersion(2));
    conceptSetsController.updateConceptSet(WORKSPACE_NAMESPACE, WORKSPACE_NAME, conceptSet.getId(),
        conceptSet);
  }

  @Test
  public void testUpdateConceptSetConceptsAddAndRemove() {
    saveConcepts();
    when(conceptBigQueryService.getParticipantCountForConcepts("condition_occurrence",
        ImmutableSet.of(CLIENT_CONCEPT_1.getConceptId()))).thenReturn(123);
    ConceptSet conceptSet = makeConceptSet1();
    ConceptSet updated =
        conceptSetsController.updateConceptSetConcepts(WORKSPACE_NAMESPACE, WORKSPACE_NAME,
            conceptSet.getId(), addConceptsRequest(conceptSet.getEtag(),
                CLIENT_CONCEPT_1.getConceptId())).getBody();
    assertThat(updated.getConcepts()).contains(CLIENT_CONCEPT_1);
    assertThat(updated.getEtag()).isNotEqualTo(conceptSet.getEtag());
    assertThat(updated.getParticipantCount()).isEqualTo(123);

    ConceptSet removed =
        conceptSetsController.updateConceptSetConcepts(WORKSPACE_NAMESPACE, WORKSPACE_NAME,
          conceptSet.getId(), removeConceptsRequest(updated.getEtag(),
              CLIENT_CONCEPT_1.getConceptId())).getBody();
    assertThat(removed.getConcepts()).isNull();
    assertThat(removed.getEtag()).isNotEqualTo(conceptSet.getEtag());
    assertThat(removed.getEtag()).isNotEqualTo(updated.getEtag());
    assertThat(removed.getParticipantCount()).isEqualTo(0);
  }

  @Test
  public void testUpdateConceptSetConceptsAddMany() {
    saveConcepts();
    when(conceptBigQueryService.getParticipantCountForConcepts("condition_occurrence",
        ImmutableSet.of(CLIENT_CONCEPT_1.getConceptId(), CLIENT_CONCEPT_3.getConceptId(),
            CLIENT_CONCEPT_4.getConceptId()))).thenReturn(456);
    ConceptSet conceptSet = makeConceptSet1();
    ConceptSet updated =
        conceptSetsController.updateConceptSetConcepts(WORKSPACE_NAMESPACE, WORKSPACE_NAME,
            conceptSet.getId(), addConceptsRequest(conceptSet.getEtag(),
                CLIENT_CONCEPT_1.getConceptId(),
                CLIENT_CONCEPT_3.getConceptId(),
                CLIENT_CONCEPT_4.getConceptId())).getBody();
    assertThat(updated.getConcepts()).containsExactly(CLIENT_CONCEPT_1, CLIENT_CONCEPT_4,
        CLIENT_CONCEPT_3);
    assertThat(updated.getEtag()).isNotEqualTo(conceptSet.getEtag());
    assertThat(updated.getParticipantCount()).isEqualTo(456);

    when(conceptBigQueryService.getParticipantCountForConcepts("condition_occurrence",
        ImmutableSet.of(CLIENT_CONCEPT_1.getConceptId()))).thenReturn(123);
    ConceptSet removed =
        conceptSetsController.updateConceptSetConcepts(WORKSPACE_NAMESPACE, WORKSPACE_NAME,
            conceptSet.getId(), removeConceptsRequest(updated.getEtag(),
                CLIENT_CONCEPT_3.getConceptId(),
                CLIENT_CONCEPT_4.getConceptId())).getBody();
    assertThat(removed.getConcepts()).containsExactly(CLIENT_CONCEPT_1);
    assertThat(removed.getEtag()).isNotEqualTo(conceptSet.getEtag());
    assertThat(removed.getEtag()).isNotEqualTo(updated.getEtag());
    assertThat(removed.getParticipantCount()).isEqualTo(123);
  }

  @Test
  public void testUpdateConceptSetConceptsAddManyOnCreate() {
    saveConcepts();
    when(conceptBigQueryService.getParticipantCountForConcepts("condition_occurrence",
        ImmutableSet.of(CLIENT_CONCEPT_1.getConceptId(), CLIENT_CONCEPT_3.getConceptId(),
            CLIENT_CONCEPT_4.getConceptId()))).thenReturn(456);
    ConceptSet conceptSet = makeConceptSet1(CLIENT_CONCEPT_1.getConceptId(),
                CLIENT_CONCEPT_3.getConceptId(),
                CLIENT_CONCEPT_4.getConceptId());
    assertThat(conceptSet.getConcepts()).containsExactly(CLIENT_CONCEPT_1, CLIENT_CONCEPT_4,
        CLIENT_CONCEPT_3);
    assertThat(conceptSet.getParticipantCount()).isEqualTo(456);
  }

  @Test(expected = BadRequestException.class)
  public void testUpdateConceptSetConceptsAddTooMany() {
    saveConcepts();
    ConceptSet conceptSet = makeConceptSet1();
    conceptSetsController.maxConceptsPerSet = 2;
    conceptSetsController.updateConceptSetConcepts(WORKSPACE_NAMESPACE, WORKSPACE_NAME,
        conceptSet.getId(), addConceptsRequest(conceptSet.getEtag(),
            CLIENT_CONCEPT_1.getConceptId(),
            CLIENT_CONCEPT_3.getConceptId(),
            CLIENT_CONCEPT_4.getConceptId())).getBody();
  }

  @Test(expected = ConflictException.class)
  public void testUpdateConceptSetConceptsWrongEtag() {
    saveConcepts();
    ConceptSet conceptSet = makeConceptSet1();
    conceptSetsController.updateConceptSetConcepts(WORKSPACE_NAMESPACE, WORKSPACE_NAME,
              conceptSet.getId(), addConceptsRequest(Etags.fromVersion(2),
            CLIENT_CONCEPT_1.getConceptId()));
  }

  @Test(expected = BadRequestException.class)
  public void testUpdateConceptSetConceptsAddWrongDomain() {
    saveConcepts();
    ConceptSet conceptSet = makeConceptSet1();
    conceptSetsController.updateConceptSetConcepts(WORKSPACE_NAMESPACE, WORKSPACE_NAME,
        conceptSet.getId(), addConceptsRequest(conceptSet.getEtag(),
            CLIENT_CONCEPT_1.getConceptId(),
            CLIENT_CONCEPT_2.getConceptId()));
  }

  @Test
  public void testDeleteConceptSet() {
    saveConcepts();
    ConceptSet conceptSet1 = makeConceptSet1();
    ConceptSet conceptSet2 = makeConceptSet2();
    ConceptSet updatedConceptSet =
        conceptSetsController.updateConceptSetConcepts(WORKSPACE_NAMESPACE, WORKSPACE_NAME,
            conceptSet1.getId(), addConceptsRequest(conceptSet1.getEtag(),
                CLIENT_CONCEPT_1.getConceptId(),
                CLIENT_CONCEPT_3.getConceptId(),
                CLIENT_CONCEPT_4.getConceptId())).getBody();
    ConceptSet updatedConceptSet2 =
        conceptSetsController.updateConceptSetConcepts(WORKSPACE_NAMESPACE, WORKSPACE_NAME,
            conceptSet2.getId(), addConceptsRequest(conceptSet2.getEtag(),
                CLIENT_CONCEPT_2.getConceptId())).getBody();
    assertThat(updatedConceptSet.getConcepts()).containsExactly(CLIENT_CONCEPT_1, CLIENT_CONCEPT_3,
        CLIENT_CONCEPT_4);
    assertThat(updatedConceptSet2.getConcepts()).containsExactly(CLIENT_CONCEPT_2);

    conceptSetsController.deleteConceptSet(WORKSPACE_NAMESPACE, WORKSPACE_NAME, conceptSet1.getId());
    try {
      conceptSetsController.getConceptSet(WORKSPACE_NAMESPACE, WORKSPACE_NAME, conceptSet1.getId());
      fail("NotFoundException expected");
    } catch (NotFoundException e) {
      // expected
    }
    conceptSet2 = conceptSetsController.getConceptSet(WORKSPACE_NAMESPACE, WORKSPACE_NAME,
        conceptSet2.getId()).getBody();
    assertThat(conceptSet2.getConcepts()).containsExactly(CLIENT_CONCEPT_2);
  }

  @Test(expected = NotFoundException.class)
  public void testDeleteConceptSetNotFound() {
    conceptSetsController.deleteConceptSet(WORKSPACE_NAMESPACE, WORKSPACE_NAME, 1L);
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

  private ConceptSet makeConceptSet1(Long... addedIds) {
    ConceptSet conceptSet = new ConceptSet();
    conceptSet.setDescription("desc 1");
    conceptSet.setName("concept set 1");
    conceptSet.setDomain(Domain.CONDITION);
    CreateConceptSetRequest request = new CreateConceptSetRequest().conceptSet(conceptSet);
    if (addedIds.length > 0) {
      request = request.addedIds(ImmutableList.copyOf(addedIds));
    }
    return conceptSetsController.createConceptSet(WORKSPACE_NAMESPACE, WORKSPACE_NAME,
        request).getBody();
  }

  private ConceptSet makeConceptSet2() {
    ConceptSet conceptSet = new ConceptSet();
    conceptSet.setDescription("desc 2");
    conceptSet.setName("concept set 2");
    conceptSet.setDomain(Domain.MEASUREMENT);
    return conceptSetsController.createConceptSet(WORKSPACE_NAMESPACE, WORKSPACE_NAME,
        new CreateConceptSetRequest().conceptSet(conceptSet))
        .getBody();

  }

  private void stubGetWorkspace(String ns, String name, String creator,
      WorkspaceAccessLevel access) throws Exception {
    org.pmiops.workbench.firecloud.model.Workspace fcWorkspace =
        new org.pmiops.workbench.firecloud.model.Workspace();
    fcWorkspace.setNamespace(ns);
    fcWorkspace.setName(name);
    fcWorkspace.setCreatedBy(creator);
    org.pmiops.workbench.firecloud.model.WorkspaceResponse fcResponse =
        new org.pmiops.workbench.firecloud.model.WorkspaceResponse();
    fcResponse.setWorkspace(fcWorkspace);
    fcResponse.setAccessLevel(access.toString());
    when(fireCloudService.getWorkspace(ns, name)).thenReturn(
        fcResponse
    );
  }

  private void saveConcepts() {
    conceptDao.save(CONCEPT_1);
    conceptDao.save(CONCEPT_2);
    conceptDao.save(CONCEPT_3);
    conceptDao.save(CONCEPT_4);
  }
}
