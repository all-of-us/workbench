package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.TableResult;
import com.google.common.collect.ImmutableMap;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.inject.Provider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.pmiops.workbench.cdr.CdrVersionContext;
import org.pmiops.workbench.cdr.CdrVersionService;
import org.pmiops.workbench.cdr.dao.CBCriteriaDao;
import org.pmiops.workbench.cdr.model.CBCriteria;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.CohortAnnotationDefinitionDao;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.CohortReviewDao;
import org.pmiops.workbench.db.dao.ParticipantCohortAnnotationDao;
import org.pmiops.workbench.db.dao.ParticipantCohortStatusDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserRecentResourceService;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.CdrVersion;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbCohortAnnotationDefinition;
import org.pmiops.workbench.db.model.DbCohortAnnotationEnumValue;
import org.pmiops.workbench.db.model.DbCohortReview;
import org.pmiops.workbench.db.model.DbParticipantCohortAnnotation;
import org.pmiops.workbench.db.model.DbParticipantCohortStatus;
import org.pmiops.workbench.db.model.DbParticipantCohortStatusKey;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.AnnotationType;
import org.pmiops.workbench.model.CohortStatus;
import org.pmiops.workbench.model.CreateReviewRequest;
import org.pmiops.workbench.model.CriteriaType;
import org.pmiops.workbench.model.DataAccessLevel;
import org.pmiops.workbench.model.DomainType;
import org.pmiops.workbench.model.EmailVerificationStatus;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.FilterColumns;
import org.pmiops.workbench.model.ModifyCohortStatusRequest;
import org.pmiops.workbench.model.ModifyParticipantCohortAnnotationRequest;
import org.pmiops.workbench.model.PageFilterRequest;
import org.pmiops.workbench.model.ParticipantCohortAnnotation;
import org.pmiops.workbench.model.ParticipantCohortAnnotationListResponse;
import org.pmiops.workbench.model.ReviewStatus;
import org.pmiops.workbench.model.SortOrder;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ComponentScan(
    basePackages = {"org.pmiops.workbench.cohortbuilder", "org.pmiops.workbench.cohortreview"})
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class CohortReviewControllerTest {

  private static final String WORKSPACE_NAMESPACE = "namespace";
  private static final String WORKSPACE_NAME = "name";
  private CdrVersion cdrVersion;
  private DbCohortReview cohortReview;
  private DbCohort cohort;
  private DbCohort cohortWithoutReview;
  private DbParticipantCohortStatus participantCohortStatus1;
  private DbParticipantCohortStatus participantCohortStatus2;
  private DbWorkspace workspace;
  private static final Instant NOW = Instant.now();
  private static final FakeClock CLOCK = new FakeClock(NOW, ZoneId.systemDefault());

  private DbCohortAnnotationDefinition stringAnnotationDefinition;
  private DbCohortAnnotationDefinition enumAnnotationDefinition;
  private DbCohortAnnotationDefinition dateAnnotationDefinition;
  private DbCohortAnnotationDefinition booleanAnnotationDefinition;
  private DbCohortAnnotationDefinition integerAnnotationDefinition;
  private DbParticipantCohortAnnotation participantAnnotation;

  @Autowired private CdrVersionDao cdrVersionDao;

  @Autowired private CBCriteriaDao cbCriteriaDao;

  @Autowired private WorkspaceDao workspaceDao;

  @Autowired private CohortDao cohortDao;

  @Autowired private CohortReviewDao cohortReviewDao;

  @Autowired private ParticipantCohortStatusDao participantCohortStatusDao;

  @Autowired private CohortAnnotationDefinitionDao cohortAnnotationDefinitionDao;

  @Autowired private ParticipantCohortAnnotationDao participantCohortAnnotationDao;

  @Autowired private WorkspaceService workspaceService;

  @Autowired private UserRecentResourceService userRecentResourceService;

  @Autowired private CohortReviewController cohortReviewController;

  @Autowired private BigQueryService bigQueryService;

  @Autowired private UserDao userDao;

  @Mock private Provider<DbUser> userProvider;

  private enum TestDemo {
    ASIAN("Asian", 8515),
    WHITE("White", 8527),
    MALE("MALE", 8507),
    FEMALE("FEMALE", 8532),
    NOT_HISPANIC("Not Hispanic or Latino", 38003564);

    private final String name;
    private final long conceptId;

    TestDemo(String name, long conceptId) {
      this.name = name;
      this.conceptId = conceptId;
    }

    public String getName() {
      return name;
    }

    public long getConceptId() {
      return conceptId;
    }
  }

  @TestConfiguration
  @Import({CdrVersionService.class, CohortReviewController.class})
  @MockBean({
    BigQueryService.class,
    FireCloudService.class,
    UserRecentResourceService.class,
    WorkspaceService.class
  })
  static class Configuration {
    @Bean
    Clock clock() {
      return CLOCK;
    }
  }

  @Before
  public void setUp() {
    DbUser user = new DbUser();
    user.setEmail("bob@gmail.com");
    user.setUserId(123L);
    user.setDisabled(false);
    user.setEmailVerificationStatusEnum(EmailVerificationStatus.SUBSCRIBED);
    user = userDao.save(user);
    when(userProvider.get()).thenReturn(user);
    cohortReviewController.setUserProvider(userProvider);

    cbCriteriaDao.save(
        new CBCriteria()
            .domainId(DomainType.PERSON.toString())
            .type(CriteriaType.RACE.toString())
            .parentId(1L)
            .conceptId(String.valueOf(TestDemo.ASIAN.conceptId))
            .name(TestDemo.ASIAN.name));
    cbCriteriaDao.save(
        new CBCriteria()
            .domainId(DomainType.PERSON.toString())
            .type(CriteriaType.GENDER.toString())
            .parentId(1L)
            .conceptId(String.valueOf(TestDemo.FEMALE.conceptId))
            .name(TestDemo.FEMALE.name));
    cbCriteriaDao.save(
        new CBCriteria()
            .domainId(DomainType.PERSON.toString())
            .type(CriteriaType.GENDER.toString())
            .parentId(1L)
            .conceptId(String.valueOf(TestDemo.MALE.conceptId))
            .name(TestDemo.MALE.name));
    cbCriteriaDao.save(
        new CBCriteria()
            .domainId(DomainType.PERSON.toString())
            .type(CriteriaType.ETHNICITY.toString())
            .parentId(1L)
            .conceptId(String.valueOf(TestDemo.NOT_HISPANIC.conceptId))
            .name(TestDemo.NOT_HISPANIC.name));
    cbCriteriaDao.save(
        new CBCriteria()
            .domainId(DomainType.PERSON.toString())
            .type(CriteriaType.RACE.toString())
            .parentId(1L)
            .conceptId(String.valueOf(TestDemo.WHITE.conceptId))
            .name(TestDemo.WHITE.name));

    cdrVersion = new CdrVersion();
    cdrVersion.setBigqueryDataset("dataSetId");
    cdrVersion.setBigqueryProject("projectId");
    cdrVersionDao.save(cdrVersion);
    CdrVersionContext.setCdrVersionNoCheckAuthDomain(cdrVersion);

    workspace = new DbWorkspace();
    workspace.setCdrVersion(cdrVersion);
    workspace.setWorkspaceNamespace(WORKSPACE_NAMESPACE);
    workspace.setName(WORKSPACE_NAME);
    workspace.setFirecloudName(WORKSPACE_NAME);
    workspace.setDataAccessLevelEnum(DataAccessLevel.PROTECTED);
    workspaceDao.save(workspace);

    cohort = new DbCohort();
    cohort.setWorkspaceId(workspace.getWorkspaceId());
    String criteria =
        "{\"includes\":[{\"id\":\"includes_kl4uky6kh\",\"items\":[{\"id\":\"items_58myrn9iz\",\"type\":\"CONDITION\",\"searchParameters\":[{"
            + "\"parameterId\":\"param1567486C34\",\"name\":\"Malignant neoplasm of bronchus and lung\",\"domain\":\"CONDITION\",\"type\": "
            + "\"ICD10CM\",\"group\":true,\"attributes\":[],\"ancestorData\":false,\"standard\":false,\"conceptId\":1567486,\"value\":\"C34\"}],"
            + "\"modifiers\":[]}],\"temporal\":false}],\"excludes\":[]}";
    cohort.setCriteria(criteria);
    cohortDao.save(cohort);

    cohortWithoutReview = new DbCohort();
    cohortWithoutReview.setWorkspaceId(workspace.getWorkspaceId());
    cohortWithoutReview.setName("test");
    cohortWithoutReview.setDescription("test desc");
    cohortWithoutReview.setCriteria(criteria);
    cohortDao.save(cohortWithoutReview);

    Timestamp today = new Timestamp(new Date().getTime());
    cohortReview =
        cohortReviewDao.save(
            new DbCohortReview()
                .cohortId(cohort.getCohortId())
                .cdrVersionId(cdrVersion.getCdrVersionId())
                .reviewSize(2)
                .creationTime(today));

    DbParticipantCohortStatusKey key1 =
        new DbParticipantCohortStatusKey()
            .cohortReviewId(cohortReview.getCohortReviewId())
            .participantId(1L);
    DbParticipantCohortStatusKey key2 =
        new DbParticipantCohortStatusKey()
            .cohortReviewId(cohortReview.getCohortReviewId())
            .participantId(2L);

    participantCohortStatus1 =
        new DbParticipantCohortStatus()
            .statusEnum(CohortStatus.NOT_REVIEWED)
            .participantKey(key1)
            .genderConceptId(TestDemo.MALE.getConceptId())
            .gender(TestDemo.MALE.getName())
            .raceConceptId(TestDemo.ASIAN.getConceptId())
            .race(TestDemo.ASIAN.getName())
            .ethnicityConceptId(TestDemo.NOT_HISPANIC.getConceptId())
            .ethnicity(TestDemo.NOT_HISPANIC.getName())
            .birthDate(new java.sql.Date(today.getTime()))
            .deceased(false);
    participantCohortStatus2 =
        new DbParticipantCohortStatus()
            .statusEnum(CohortStatus.NOT_REVIEWED)
            .participantKey(key2)
            .genderConceptId(TestDemo.FEMALE.getConceptId())
            .gender(TestDemo.FEMALE.getName())
            .raceConceptId(TestDemo.WHITE.getConceptId())
            .race(TestDemo.WHITE.getName())
            .ethnicityConceptId(TestDemo.NOT_HISPANIC.getConceptId())
            .ethnicity(TestDemo.NOT_HISPANIC.getName())
            .birthDate(new java.sql.Date(today.getTime()))
            .deceased(false);

    participantCohortStatusDao.save(participantCohortStatus1);
    participantCohortStatusDao.save(participantCohortStatus2);

    stringAnnotationDefinition =
        new DbCohortAnnotationDefinition()
            .annotationType(DbStorageEnums.annotationTypeToStorage(AnnotationType.STRING))
            .columnName("test")
            .cohortId(cohort.getCohortId());
    cohortAnnotationDefinitionDao.save(stringAnnotationDefinition);
    enumAnnotationDefinition =
        new DbCohortAnnotationDefinition()
            .annotationType(DbStorageEnums.annotationTypeToStorage(AnnotationType.ENUM))
            .columnName("test")
            .cohortId(cohort.getCohortId());
    SortedSet<DbCohortAnnotationEnumValue> enumValues = new TreeSet<DbCohortAnnotationEnumValue>();
    enumValues.add(
        new DbCohortAnnotationEnumValue()
            .name("test")
            .cohortAnnotationDefinition(enumAnnotationDefinition));
    cohortAnnotationDefinitionDao.save(enumAnnotationDefinition.enumValues(enumValues));
    dateAnnotationDefinition =
        new DbCohortAnnotationDefinition()
            .annotationType(DbStorageEnums.annotationTypeToStorage(AnnotationType.DATE))
            .columnName("test")
            .cohortId(cohort.getCohortId());
    cohortAnnotationDefinitionDao.save(dateAnnotationDefinition);
    booleanAnnotationDefinition =
        new DbCohortAnnotationDefinition()
            .annotationType(DbStorageEnums.annotationTypeToStorage(AnnotationType.BOOLEAN))
            .columnName("test")
            .cohortId(cohort.getCohortId());
    cohortAnnotationDefinitionDao.save(booleanAnnotationDefinition);
    integerAnnotationDefinition =
        new DbCohortAnnotationDefinition()
            .annotationType(DbStorageEnums.annotationTypeToStorage(AnnotationType.INTEGER))
            .columnName("test")
            .cohortId(cohort.getCohortId());
    cohortAnnotationDefinitionDao.save(integerAnnotationDefinition);

    participantAnnotation =
        new DbParticipantCohortAnnotation()
            .cohortReviewId(cohortReview.getCohortReviewId())
            .participantId(participantCohortStatus1.getParticipantKey().getParticipantId())
            .annotationValueString("test")
            .cohortAnnotationDefinitionId(
                stringAnnotationDefinition.getCohortAnnotationDefinitionId());
    participantCohortAnnotationDao.save(participantAnnotation);
  }

  @Test
  public void createCohortReviewLessThanMinSize() throws Exception {
    try {
      cohortReviewController.createCohortReview(
          WORKSPACE_NAMESPACE,
          WORKSPACE_NAME,
          cohort.getCohortId(),
          cdrVersion.getCdrVersionId(),
          new CreateReviewRequest().size(0));
      fail("Should have thrown a BadRequestException!");
    } catch (BadRequestException bre) {
      // success
      assertThat(bre.getMessage())
          .isEqualTo("Bad Request: Cohort Review size must be between 0 and 10000");
    }
  }

  @Test
  public void createCohortReviewMoreThanMaxSize() throws Exception {
    try {
      cohortReviewController.createCohortReview(
          WORKSPACE_NAMESPACE,
          WORKSPACE_NAME,
          cohort.getCohortId(),
          cdrVersion.getCdrVersionId(),
          new CreateReviewRequest().size(10001));
      fail("Should have thrown a BadRequestException!");
    } catch (BadRequestException bre) {
      // success
      assertThat(bre.getMessage())
          .isEqualTo("Bad Request: Cohort Review size must be between 0 and 10000");
    }
  }

  @Test
  public void createCohortReviewAlreadyExists() throws Exception {
    when(workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            WORKSPACE_NAMESPACE, WORKSPACE_NAME, WorkspaceAccessLevel.WRITER))
        .thenReturn(workspace);

    stubBigQueryCohortCalls();

    try {
      cohortReviewController.createCohortReview(
          WORKSPACE_NAMESPACE,
          WORKSPACE_NAME,
          cohort.getCohortId(),
          cdrVersion.getCdrVersionId(),
          new CreateReviewRequest().size(1));
      fail("Should have thrown a BadRequestException!");
    } catch (BadRequestException bre) {
      // success
      assertThat(bre.getMessage())
          .isEqualTo(
              "Bad Request: Cohort Review already created for cohortId: "
                  + cohort.getCohortId()
                  + ", cdrVersionId: "
                  + cdrVersion.getCdrVersionId());
    }
  }

  @Test
  public void createCohortReview() throws Exception {
    when(workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            WORKSPACE_NAMESPACE, WORKSPACE_NAME, WorkspaceAccessLevel.WRITER))
        .thenReturn(workspace);

    stubBigQueryCohortCalls();

    org.pmiops.workbench.model.CohortReview cohortReview =
        cohortReviewController
            .createCohortReview(
                WORKSPACE_NAMESPACE,
                WORKSPACE_NAME,
                cohortWithoutReview.getCohortId(),
                cdrVersion.getCdrVersionId(),
                new CreateReviewRequest().size(1))
            .getBody();

    assertThat(cohortReview.getReviewStatus()).isEqualTo(ReviewStatus.CREATED);
    assertThat(cohortReview.getCohortName()).isEqualTo(cohortWithoutReview.getName());
    assertThat(cohortReview.getDescription()).isEqualTo(cohortWithoutReview.getDescription());
    assertThat(cohortReview.getReviewSize()).isEqualTo(1);
    assertThat(cohortReview.getParticipantCohortStatuses().size()).isEqualTo(1);
    assertThat(cohortReview.getParticipantCohortStatuses().get(0).getStatus())
        .isEqualTo(CohortStatus.NOT_REVIEWED);
  }

  @Test
  public void createCohortReviewNoCohortException() throws Exception {
    long cohortId = 99;
    when(workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            WORKSPACE_NAMESPACE, WORKSPACE_NAME, WorkspaceAccessLevel.WRITER))
        .thenReturn(workspace);

    stubBigQueryCohortCalls();

    try {
      cohortReviewController
          .createCohortReview(
              WORKSPACE_NAMESPACE,
              WORKSPACE_NAME,
              cohortId,
              cdrVersion.getCdrVersionId(),
              new CreateReviewRequest().size(1))
          .getBody();
      fail("Should have thrown NotFoundException!");
    } catch (NotFoundException nfe) {
      assertEquals("Not Found: No Cohort exists for cohortId: " + cohortId, nfe.getMessage());
    }
  }

  @Test
  public void createCohortReviewNoMatchingWorkspaceException() throws Exception {
    String badWorkspaceName = WORKSPACE_NAME + "bad";
    when(workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            WORKSPACE_NAMESPACE, WORKSPACE_NAME, WorkspaceAccessLevel.WRITER))
        .thenReturn(workspace);

    stubBigQueryCohortCalls();

    try {
      cohortReviewController
          .createCohortReview(
              WORKSPACE_NAMESPACE,
              badWorkspaceName,
              cohortWithoutReview.getCohortId(),
              cdrVersion.getCdrVersionId(),
              new CreateReviewRequest().size(1))
          .getBody();
      fail("Should have thrown NotFoundException!");
    } catch (NotFoundException nfe) {
      assertEquals(
          "Not Found: No workspace matching workspaceNamespace: "
              + WORKSPACE_NAMESPACE
              + ", workspaceId: "
              + badWorkspaceName,
          nfe.getMessage());
    }
  }

  @Test
  public void updateCohortReview() throws Exception {
    when(workspaceService.enforceWorkspaceAccessLevel(
            WORKSPACE_NAMESPACE, WORKSPACE_NAME, WorkspaceAccessLevel.WRITER))
        .thenReturn(WorkspaceAccessLevel.WRITER);

    org.pmiops.workbench.model.CohortReview requestCohortReview =
        new org.pmiops.workbench.model.CohortReview()
            .cohortReviewId(cohortReview.getCohortReviewId())
            .etag(Etags.fromVersion(cohortReview.getVersion()));
    requestCohortReview.setCohortName("blahblah");
    requestCohortReview.setDescription("new desc");
    org.pmiops.workbench.model.CohortReview responseCohortReview =
        cohortReviewController
            .updateCohortReview(
                WORKSPACE_NAMESPACE,
                WORKSPACE_NAME,
                requestCohortReview.getCohortReviewId(),
                requestCohortReview)
            .getBody();

    assertThat(responseCohortReview.getCohortName()).isEqualTo(requestCohortReview.getCohortName());
    assertThat(responseCohortReview.getDescription())
        .isEqualTo(requestCohortReview.getDescription());
    assertThat(responseCohortReview.getLastModifiedTime()).isNotNull();
  }

  @Test
  public void deleteCohortReview() throws Exception {
    when(workspaceService.enforceWorkspaceAccessLevel(
            WORKSPACE_NAMESPACE, WORKSPACE_NAME, WorkspaceAccessLevel.WRITER))
        .thenReturn(WorkspaceAccessLevel.WRITER);

    org.pmiops.workbench.model.CohortReview requestCohortReview =
        new org.pmiops.workbench.model.CohortReview()
            .cohortReviewId(cohortReview.getCohortReviewId())
            .etag(Etags.fromVersion(cohortReview.getVersion()));
    EmptyResponse emptyResponse =
        cohortReviewController
            .deleteCohortReview(
                WORKSPACE_NAMESPACE, WORKSPACE_NAME, requestCohortReview.getCohortReviewId())
            .getBody();

    assertThat(emptyResponse).isNotNull();
  }

  @Test
  public void createParticipantCohortAnnotationNoAnnotationDefinitionFound() throws Exception {
    Long participantId = participantCohortStatus1.getParticipantKey().getParticipantId();

    when(workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            WORKSPACE_NAMESPACE, WORKSPACE_NAME, WorkspaceAccessLevel.WRITER))
        .thenReturn(workspace);

    try {
      cohortReviewController
          .createParticipantCohortAnnotation(
              WORKSPACE_NAMESPACE,
              WORKSPACE_NAME,
              cohortReview.getCohortReviewId(),
              participantId,
              new ParticipantCohortAnnotation()
                  .cohortReviewId(cohortReview.getCohortReviewId())
                  .participantId(participantId)
                  .annotationValueString("test")
                  .cohortAnnotationDefinitionId(9999L))
          .getBody();
      fail("Should have thrown a NotFoundException!");
    } catch (NotFoundException nfe) {
      // Success
      assertThat(nfe.getMessage())
          .isEqualTo("Not Found: No cohort annotation definition found for id: 9999");
    }
  }

  @Test
  public void createParticipantCohortAnnotationNoAnnotationValue() throws Exception {
    Long participantId = participantCohortStatus1.getParticipantKey().getParticipantId();
    assertBadRequestExceptionForAnnotationType(
        participantId, stringAnnotationDefinition.getCohortAnnotationDefinitionId(), "STRING");
    assertBadRequestExceptionForAnnotationType(
        participantId, enumAnnotationDefinition.getCohortAnnotationDefinitionId(), "ENUM");
    assertBadRequestExceptionForAnnotationType(
        participantId, dateAnnotationDefinition.getCohortAnnotationDefinitionId(), "DATE");
    assertBadRequestExceptionForAnnotationType(
        participantId, booleanAnnotationDefinition.getCohortAnnotationDefinitionId(), "BOOLEAN");
    assertBadRequestExceptionForAnnotationType(
        participantId, integerAnnotationDefinition.getCohortAnnotationDefinitionId(), "INTEGER");
  }

  @Test
  public void createParticipantCohortAnnotation() throws Exception {
    Long participantId = participantCohortStatus1.getParticipantKey().getParticipantId();
    participantCohortAnnotationDao.delete(participantAnnotation);

    assertCreateParticipantCohortAnnotation(
        participantId,
        stringAnnotationDefinition.getCohortAnnotationDefinitionId(),
        new ParticipantCohortAnnotation()
            .cohortReviewId(cohortReview.getCohortReviewId())
            .participantId(participantId)
            .annotationValueString("test")
            .cohortAnnotationDefinitionId(
                stringAnnotationDefinition.getCohortAnnotationDefinitionId()));
    assertCreateParticipantCohortAnnotation(
        participantId,
        enumAnnotationDefinition.getCohortAnnotationDefinitionId(),
        new ParticipantCohortAnnotation()
            .cohortReviewId(cohortReview.getCohortReviewId())
            .participantId(participantId)
            .annotationValueEnum("test")
            .cohortAnnotationDefinitionId(
                enumAnnotationDefinition.getCohortAnnotationDefinitionId()));
    assertCreateParticipantCohortAnnotation(
        participantId,
        dateAnnotationDefinition.getCohortAnnotationDefinitionId(),
        new ParticipantCohortAnnotation()
            .cohortReviewId(cohortReview.getCohortReviewId())
            .participantId(participantId)
            .annotationValueDate("2018-02-02")
            .cohortAnnotationDefinitionId(
                dateAnnotationDefinition.getCohortAnnotationDefinitionId()));
    assertCreateParticipantCohortAnnotation(
        participantId,
        booleanAnnotationDefinition.getCohortAnnotationDefinitionId(),
        new ParticipantCohortAnnotation()
            .cohortReviewId(cohortReview.getCohortReviewId())
            .participantId(participantId)
            .annotationValueBoolean(true)
            .cohortAnnotationDefinitionId(
                booleanAnnotationDefinition.getCohortAnnotationDefinitionId()));
    assertCreateParticipantCohortAnnotation(
        participantId,
        integerAnnotationDefinition.getCohortAnnotationDefinitionId(),
        new ParticipantCohortAnnotation()
            .cohortReviewId(cohortReview.getCohortReviewId())
            .participantId(participantId)
            .annotationValueInteger(1)
            .cohortAnnotationDefinitionId(
                integerAnnotationDefinition.getCohortAnnotationDefinitionId()));
  }

  @Test
  public void deleteParticipantCohortAnnotation() throws Exception {
    DbParticipantCohortAnnotation annotation =
        new DbParticipantCohortAnnotation()
            .cohortReviewId(cohortReview.getCohortReviewId())
            .participantId(participantCohortStatus1.getParticipantKey().getParticipantId())
            .annotationValueString("test")
            .cohortAnnotationDefinitionId(
                stringAnnotationDefinition.getCohortAnnotationDefinitionId());
    participantCohortAnnotationDao.save(annotation);

    when(workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            WORKSPACE_NAMESPACE, WORKSPACE_NAME, WorkspaceAccessLevel.WRITER))
        .thenReturn(workspace);

    cohortReviewController.deleteParticipantCohortAnnotation(
        WORKSPACE_NAMESPACE,
        WORKSPACE_NAME,
        cohortReview.getCohortReviewId(),
        participantCohortStatus1.getParticipantKey().getParticipantId(),
        annotation.getAnnotationId());

    assertThat(participantCohortAnnotationDao.findOne(annotation.getAnnotationId()))
        .isEqualTo(null);
  }

  @Test
  public void deleteParticipantCohortAnnotationNoAnnotation() throws Exception {
    Long participantId = participantCohortStatus1.getParticipantKey().getParticipantId();
    Long annotationId = 9999L;

    when(workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            WORKSPACE_NAMESPACE, WORKSPACE_NAME, WorkspaceAccessLevel.WRITER))
        .thenReturn(workspace);

    try {
      cohortReviewController.deleteParticipantCohortAnnotation(
          WORKSPACE_NAMESPACE,
          WORKSPACE_NAME,
          cohortReview.getCohortReviewId(),
          participantId,
          annotationId);
      fail("Should have thrown a NotFoundException!");
    } catch (NotFoundException nfe) {
      // Success
      assertThat(nfe.getMessage())
          .isEqualTo(
              "Not Found: No participant cohort annotation found for annotationId: "
                  + annotationId
                  + ", cohortReviewId: "
                  + cohortReview.getCohortReviewId()
                  + ", participantId: "
                  + participantId);
    }
  }

  @Test
  public void getParticipantCohortAnnotations() throws Exception {
    when(workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            WORKSPACE_NAMESPACE, WORKSPACE_NAME, WorkspaceAccessLevel.READER))
        .thenReturn(workspace);

    ParticipantCohortAnnotationListResponse response =
        cohortReviewController
            .getParticipantCohortAnnotations(
                WORKSPACE_NAMESPACE,
                WORKSPACE_NAME,
                cohortReview.getCohortReviewId(),
                participantCohortStatus1.getParticipantKey().getParticipantId())
            .getBody();

    assertThat(response.getItems().size()).isEqualTo(1);
    assertThat(response.getItems().get(0).getCohortReviewId())
        .isEqualTo(cohortReview.getCohortReviewId());
    assertThat(response.getItems().get(0).getParticipantId())
        .isEqualTo(participantCohortStatus1.getParticipantKey().getParticipantId());
  }

  @Test
  public void getParticipantCohortStatus() throws Exception {
    when(workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            WORKSPACE_NAMESPACE, WORKSPACE_NAME, WorkspaceAccessLevel.READER))
        .thenReturn(workspace);

    org.pmiops.workbench.model.ParticipantCohortStatus response =
        cohortReviewController
            .getParticipantCohortStatus(
                WORKSPACE_NAMESPACE,
                WORKSPACE_NAME,
                cohortReview.getCohortReviewId(),
                participantCohortStatus1.getParticipantKey().getParticipantId())
            .getBody();

    assertThat(response.getParticipantId())
        .isEqualTo(participantCohortStatus1.getParticipantKey().getParticipantId());
    assertThat(response.getStatus())
        .isEqualTo(DbStorageEnums.cohortStatusFromStorage(participantCohortStatus1.getStatus()));
    assertThat(response.getEthnicityConceptId())
        .isEqualTo(participantCohortStatus1.getEthnicityConceptId());
    assertThat(response.getRaceConceptId()).isEqualTo(participantCohortStatus1.getRaceConceptId());
    assertThat(response.getGenderConceptId())
        .isEqualTo(participantCohortStatus1.getGenderConceptId());
  }

  @Test
  public void getParticipantCohortStatuses() throws Exception {
    int page = 0;
    int pageSize = 25;
    org.pmiops.workbench.model.CohortReview expectedReview1 =
        createCohortReview(
            cohortReview,
            Arrays.asList(participantCohortStatus1, participantCohortStatus2),
            page,
            pageSize,
            SortOrder.DESC,
            FilterColumns.STATUS);
    org.pmiops.workbench.model.CohortReview expectedReview2 =
        createCohortReview(
            cohortReview,
            Arrays.asList(participantCohortStatus2, participantCohortStatus1),
            page,
            pageSize,
            SortOrder.DESC,
            FilterColumns.PARTICIPANTID);
    org.pmiops.workbench.model.CohortReview expectedReview3 =
        createCohortReview(
            cohortReview,
            Arrays.asList(participantCohortStatus1, participantCohortStatus2),
            page,
            pageSize,
            SortOrder.ASC,
            FilterColumns.STATUS);
    org.pmiops.workbench.model.CohortReview expectedReview4 =
        createCohortReview(
            cohortReview,
            Arrays.asList(participantCohortStatus1, participantCohortStatus2),
            page,
            pageSize,
            SortOrder.ASC,
            FilterColumns.PARTICIPANTID);

    when(workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            WORKSPACE_NAMESPACE, WORKSPACE_NAME, WorkspaceAccessLevel.READER))
        .thenReturn(workspace);

    assertParticipantCohortStatuses(
        expectedReview1, page, pageSize, SortOrder.DESC, FilterColumns.STATUS);
    verify(userRecentResourceService)
        .updateCohortEntry(anyLong(), anyLong(), anyLong(), any(Timestamp.class));
    assertParticipantCohortStatuses(
        expectedReview2, page, pageSize, SortOrder.DESC, FilterColumns.PARTICIPANTID);
    assertParticipantCohortStatuses(expectedReview3, null, null, null, FilterColumns.STATUS);
    assertParticipantCohortStatuses(expectedReview4, null, null, SortOrder.ASC, null);
    assertParticipantCohortStatuses(expectedReview4, null, pageSize, null, null);
    assertParticipantCohortStatuses(expectedReview4, page, null, null, null);
    assertParticipantCohortStatuses(expectedReview4, null, null, null, null);
  }

  @Test
  public void updateParticipantCohortAnnotation() throws Exception {
    when(workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            WORKSPACE_NAMESPACE, WORKSPACE_NAME, WorkspaceAccessLevel.WRITER))
        .thenReturn(workspace);

    ParticipantCohortAnnotation participantCohortAnnotation =
        cohortReviewController
            .updateParticipantCohortAnnotation(
                WORKSPACE_NAMESPACE,
                WORKSPACE_NAME,
                cohortReview.getCohortReviewId(),
                participantCohortStatus1.getParticipantKey().getParticipantId(),
                participantAnnotation.getAnnotationId(),
                new ModifyParticipantCohortAnnotationRequest().annotationValueString("test1"))
            .getBody();

    assertThat(participantCohortAnnotation.getAnnotationValueString()).isEqualTo("test1");
  }

  @Test
  public void updateParticipantCohortAnnotationNoAnnotationForIdException() throws Exception {
    long badAnnotationId = 99;
    when(workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            WORKSPACE_NAMESPACE, WORKSPACE_NAME, WorkspaceAccessLevel.WRITER))
        .thenReturn(workspace);

    try {
      cohortReviewController
          .updateParticipantCohortAnnotation(
              WORKSPACE_NAMESPACE,
              WORKSPACE_NAME,
              cohortReview.getCohortReviewId(),
              participantCohortStatus1.getParticipantKey().getParticipantId(),
              badAnnotationId,
              new ModifyParticipantCohortAnnotationRequest().annotationValueString("test1"))
          .getBody();
    } catch (NotFoundException nfe) {
      assertEquals(
          "Not Found: Participant Cohort Annotation does not exist for annotationId: "
              + badAnnotationId
              + ", cohortReviewId: "
              + cohortReview.getCohortReviewId()
              + ", participantId: "
              + participantCohortStatus1.getParticipantKey().getParticipantId(),
          nfe.getMessage());
    }
  }

  @Test
  public void updateParticipantCohortStatus() throws Exception {
    when(workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            WORKSPACE_NAMESPACE, WORKSPACE_NAME, WorkspaceAccessLevel.WRITER))
        .thenReturn(workspace);

    org.pmiops.workbench.model.ParticipantCohortStatus participantCohortStatus =
        cohortReviewController
            .updateParticipantCohortStatus(
                WORKSPACE_NAMESPACE,
                WORKSPACE_NAME,
                cohortReview.getCohortReviewId(),
                participantCohortStatus1.getParticipantKey().getParticipantId(),
                new ModifyCohortStatusRequest().status(CohortStatus.INCLUDED))
            .getBody();

    assertThat(participantCohortStatus.getStatus()).isEqualTo(CohortStatus.INCLUDED);
  }

  /**
   * Helper method to consolidate assertions for all the {@link AnnotationType}s.
   *
   * @param participantId
   * @param annotationDefinitionId
   * @param request
   */
  private void assertCreateParticipantCohortAnnotation(
      Long participantId, Long annotationDefinitionId, ParticipantCohortAnnotation request) {
    when(workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            WORKSPACE_NAMESPACE, WORKSPACE_NAME, WorkspaceAccessLevel.WRITER))
        .thenReturn(workspace);

    ParticipantCohortAnnotation response =
        cohortReviewController
            .createParticipantCohortAnnotation(
                WORKSPACE_NAMESPACE,
                WORKSPACE_NAME,
                cohortReview.getCohortReviewId(),
                participantId,
                request)
            .getBody();

    assertThat(response.getAnnotationValueString()).isEqualTo(request.getAnnotationValueString());
    assertThat(response.getAnnotationValueBoolean()).isEqualTo(request.getAnnotationValueBoolean());
    assertThat(response.getAnnotationValueEnum()).isEqualTo(request.getAnnotationValueEnum());
    assertThat(response.getAnnotationValueDate()).isEqualTo(request.getAnnotationValueDate());
    assertThat(response.getAnnotationValueInteger()).isEqualTo(request.getAnnotationValueInteger());
    assertThat(response.getParticipantId()).isEqualTo(participantId);
    assertThat(response.getCohortReviewId()).isEqualTo(cohortReview.getCohortReviewId());
    assertThat(response.getCohortAnnotationDefinitionId()).isEqualTo(annotationDefinitionId);
  }

  /**
   * Helper method to consolidate assertions for {@link BadRequestException}s for all {@link
   * AnnotationType}s.
   *
   * @param participantId
   * @param cohortAnnotationDefId
   * @param type
   */
  private void assertBadRequestExceptionForAnnotationType(
      Long participantId, Long cohortAnnotationDefId, String type) {
    when(workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            WORKSPACE_NAMESPACE, WORKSPACE_NAME, WorkspaceAccessLevel.WRITER))
        .thenReturn(workspace);

    try {
      cohortReviewController
          .createParticipantCohortAnnotation(
              WORKSPACE_NAMESPACE,
              WORKSPACE_NAME,
              cohortReview.getCohortReviewId(),
              participantId,
              new ParticipantCohortAnnotation()
                  .cohortReviewId(cohortReview.getCohortReviewId())
                  .participantId(participantId)
                  .cohortAnnotationDefinitionId(cohortAnnotationDefId))
          .getBody();
      fail("Should have thrown a BadRequestException!");
    } catch (BadRequestException bre) {
      // Success
      assertThat(bre.getMessage())
          .isEqualTo(
              "Bad Request: Please provide a valid "
                  + type
                  + " value for annotation defintion id: "
                  + cohortAnnotationDefId);
    }
  }

  /**
   * Helper method to assert results for {@link
   * CohortReviewController#getParticipantCohortStatuses(String, String, Long, Long,
   * PageFilterRequest)}.
   *
   * @param expectedReview
   * @param page
   * @param pageSize
   * @param sortOrder
   * @param sortColumn
   */
  private void assertParticipantCohortStatuses(
      org.pmiops.workbench.model.CohortReview expectedReview,
      Integer page,
      Integer pageSize,
      SortOrder sortOrder,
      FilterColumns sortColumn) {
    org.pmiops.workbench.model.CohortReview actualReview =
        cohortReviewController
            .getParticipantCohortStatuses(
                WORKSPACE_NAMESPACE,
                WORKSPACE_NAME,
                cohort.getCohortId(),
                cdrVersion.getCdrVersionId(),
                new PageFilterRequest()
                    .sortColumn(sortColumn)
                    .page(page)
                    .pageSize(pageSize)
                    .sortOrder(sortOrder))
            .getBody();

    assertThat(actualReview).isEqualTo(expectedReview);
  }

  private void stubBigQueryCohortCalls() {
    TableResult queryResult = mock(TableResult.class);
    Iterable testIterable =
        new Iterable() {
          @Override
          public Iterator iterator() {
            List<FieldValue> list = new ArrayList<>();
            list.add(null);
            return list.iterator();
          }
        };
    Map<String, Integer> rm =
        ImmutableMap.<String, Integer>builder()
            .put("person_id", 0)
            .put("birth_datetime", 1)
            .put("gender_concept_id", 2)
            .put("race_concept_id", 3)
            .put("ethnicity_concept_id", 4)
            .put("count", 5)
            .put("deceased", 6)
            .build();

    when(bigQueryService.filterBigQueryConfig(null)).thenReturn(null);
    when(bigQueryService.executeQuery(null)).thenReturn(queryResult);
    when(bigQueryService.getResultMapper(queryResult)).thenReturn(rm);
    when(queryResult.iterateAll()).thenReturn(testIterable);
    when(queryResult.getValues()).thenReturn(testIterable);
    when(bigQueryService.getLong(null, 0)).thenReturn(0L);
    when(bigQueryService.getString(null, 1)).thenReturn("1");
    when(bigQueryService.getLong(null, 2)).thenReturn(0L);
    when(bigQueryService.getLong(null, 3)).thenReturn(0L);
    when(bigQueryService.getLong(null, 4)).thenReturn(0L);
    when(bigQueryService.getLong(null, 5)).thenReturn(0L);
  }

  private org.pmiops.workbench.model.CohortReview createCohortReview(
      DbCohortReview actualReview,
      List<DbParticipantCohortStatus> participantCohortStatusList,
      Integer page,
      Integer pageSize,
      SortOrder sortOrder,
      FilterColumns sortColumn) {
    List<org.pmiops.workbench.model.ParticipantCohortStatus> newParticipantCohortStatusList =
        new ArrayList<>();
    for (DbParticipantCohortStatus participantCohortStatus : participantCohortStatusList) {
      newParticipantCohortStatusList.add(
          new org.pmiops.workbench.model.ParticipantCohortStatus()
              .birthDate(participantCohortStatus.getBirthDate().toString())
              .ethnicityConceptId(participantCohortStatus.getEthnicityConceptId())
              .ethnicity(participantCohortStatus.getEthnicity())
              .genderConceptId(participantCohortStatus.getGenderConceptId())
              .gender(participantCohortStatus.getGender())
              .participantId(participantCohortStatus.getParticipantKey().getParticipantId())
              .raceConceptId(participantCohortStatus.getRaceConceptId())
              .race(participantCohortStatus.getRace())
              .status(participantCohortStatus.getStatusEnum())
              .deceased(participantCohortStatus.getDeceased()));
    }
    return new org.pmiops.workbench.model.CohortReview()
        .cohortReviewId(actualReview.getCohortReviewId())
        .cohortId(actualReview.getCohortId())
        .cdrVersionId(actualReview.getCdrVersionId())
        .creationTime(actualReview.getCreationTime().toString())
        .matchedParticipantCount(actualReview.getMatchedParticipantCount())
        .reviewSize(actualReview.getReviewSize())
        .reviewedCount(actualReview.getReviewedCount())
        .queryResultSize(2L)
        .participantCohortStatuses(newParticipantCohortStatusList)
        .page(page)
        .pageSize(pageSize)
        .sortOrder(sortOrder.toString())
        .sortColumn(sortColumn.name());
  }
}
