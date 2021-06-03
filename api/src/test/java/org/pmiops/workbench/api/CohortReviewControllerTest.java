package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.TableResult;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.SpringTest;
import org.pmiops.workbench.access.AccessTierService;
import org.pmiops.workbench.cdr.CdrVersionContext;
import org.pmiops.workbench.cdr.CdrVersionService;
import org.pmiops.workbench.cdr.dao.CBCriteriaDao;
import org.pmiops.workbench.cdr.model.DbCriteria;
import org.pmiops.workbench.cohortbuilder.CohortBuilderServiceImpl;
import org.pmiops.workbench.cohortbuilder.CohortQueryBuilder;
import org.pmiops.workbench.cohortbuilder.mapper.CohortBuilderMapper;
import org.pmiops.workbench.cohortreview.CohortReviewServiceImpl;
import org.pmiops.workbench.cohortreview.ReviewQueryBuilder;
import org.pmiops.workbench.cohortreview.mapper.CohortReviewMapperImpl;
import org.pmiops.workbench.cohortreview.mapper.ParticipantCohortAnnotationMapperImpl;
import org.pmiops.workbench.cohortreview.mapper.ParticipantCohortStatusMapperImpl;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.CohortAnnotationDefinitionDao;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.CohortReviewDao;
import org.pmiops.workbench.db.dao.ParticipantCohortAnnotationDao;
import org.pmiops.workbench.db.dao.ParticipantCohortStatusDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserRecentResourceService;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbCdrVersion;
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
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.AnnotationType;
import org.pmiops.workbench.model.CohortReview;
import org.pmiops.workbench.model.CohortStatus;
import org.pmiops.workbench.model.CreateReviewRequest;
import org.pmiops.workbench.model.CriteriaType;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.EmailVerificationStatus;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.FilterColumns;
import org.pmiops.workbench.model.ModifyCohortStatusRequest;
import org.pmiops.workbench.model.ModifyParticipantCohortAnnotationRequest;
import org.pmiops.workbench.model.PageFilterRequest;
import org.pmiops.workbench.model.ParticipantCohortAnnotation;
import org.pmiops.workbench.model.ParticipantCohortAnnotationListResponse;
import org.pmiops.workbench.model.ParticipantCohortStatus;
import org.pmiops.workbench.model.ReviewStatus;
import org.pmiops.workbench.model.SortOrder;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.workspaces.WorkspaceAuthService;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class CohortReviewControllerTest extends SpringTest {

  private static final String WORKSPACE_NAMESPACE = "namespace";
  private static final String WORKSPACE_NAME = "name";
  private static final String WORKSPACE_NAMESPACE2 = "namespace";
  private static final String WORKSPACE_NAME2 = "name2";
  private DbCdrVersion cdrVersion;
  private DbCohortReview cohortReview;
  private DbCohort cohort;
  private DbCohort cohortWithoutReview;
  private DbParticipantCohortStatus participantCohortStatus1;
  private DbParticipantCohortStatus participantCohortStatus2;
  private DbWorkspace workspace;
  private DbWorkspace workspace2;
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

  @Autowired private WorkspaceAuthService workspaceAuthService;

  @Autowired private UserRecentResourceService userRecentResourceService;

  @Autowired private CohortReviewController cohortReviewController;

  @Autowired private BigQueryService bigQueryService;

  @Autowired private UserDao userDao;

  private enum TestConcepts {
    ASIAN("Asian", 8515),
    WHITE("White", 8527),
    MALE("MALE", 8507),
    FEMALE("FEMALE", 8532),
    NOT_HISPANIC("Not Hispanic or Latino", 38003564),
    PREFER_NOT_TO_ANSWER_RACE("I Prefer not to answer", 1177221),
    PREFER_NOT_TO_ANSWER_ETH("I Prefer not to answer", 1177221),
    SEX_AT_BIRTH("MALE", 45880669);

    private final String name;
    private final long conceptId;

    TestConcepts(String name, long conceptId) {
      this.name = name;
      this.conceptId = conceptId;
    }

    public String getName() {
      return name;
    }

    public long getConceptId() {
      return conceptId;
    }

    public static Map<Long, String> asMap() {
      return Arrays.stream(TestConcepts.values())
          .collect(
              Collectors.toMap(
                  TestConcepts::getConceptId,
                  TestConcepts::getName,
                  (oldValue, newValue) -> oldValue));
    }
  }

  private static DbUser user;

  @TestConfiguration
  @Import({
    CdrVersionService.class,
    CohortBuilderServiceImpl.class,
    CohortReviewController.class,
    CohortReviewServiceImpl.class,
    CohortQueryBuilder.class,
    ReviewQueryBuilder.class,
    ParticipantCohortStatusMapperImpl.class,
    CohortReviewMapperImpl.class,
    ParticipantCohortAnnotationMapperImpl.class,
    CommonMappers.class,
  })
  @MockBean({
    BigQueryService.class,
    CohortBuilderMapper.class,
    FireCloudService.class,
    UserRecentResourceService.class,
    WorkspaceService.class,
    WorkspaceAuthService.class,
    AccessTierService.class,
    CdrVersionService.class,
  })
  static class Configuration {
    @Bean
    Clock clock() {
      return CLOCK;
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    DbUser user() {
      return user;
    }
  }

  @BeforeEach
  public void setUp() {
    user = new DbUser();
    user.setUsername("bob@gmail.com");
    user.setUserId(123L);
    user.setDisabled(false);
    user.setEmailVerificationStatusEnum(EmailVerificationStatus.SUBSCRIBED);
    user = userDao.save(user);

    cbCriteriaDao.save(
        DbCriteria.builder()
            .addDomainId(Domain.PERSON.toString())
            .addType(CriteriaType.RACE.toString())
            .addParentId(1L)
            .addConceptId(String.valueOf(TestConcepts.ASIAN.conceptId))
            .addName(TestConcepts.ASIAN.name)
            .build());
    cbCriteriaDao.save(
        DbCriteria.builder()
            .addDomainId(Domain.PERSON.toString())
            .addType(CriteriaType.GENDER.toString())
            .addParentId(1L)
            .addConceptId(String.valueOf(TestConcepts.FEMALE.conceptId))
            .addName(TestConcepts.FEMALE.name)
            .build());
    cbCriteriaDao.save(
        DbCriteria.builder()
            .addDomainId(Domain.PERSON.toString())
            .addType(CriteriaType.GENDER.toString())
            .addParentId(1L)
            .addConceptId(String.valueOf(TestConcepts.MALE.conceptId))
            .addName(TestConcepts.MALE.name)
            .build());
    cbCriteriaDao.save(
        DbCriteria.builder()
            .addDomainId(Domain.PERSON.toString())
            .addType(CriteriaType.ETHNICITY.toString())
            .addParentId(1L)
            .addConceptId(String.valueOf(TestConcepts.NOT_HISPANIC.conceptId))
            .addName(TestConcepts.NOT_HISPANIC.name)
            .build());
    cbCriteriaDao.save(
        DbCriteria.builder()
            .addDomainId(Domain.PERSON.toString())
            .addType(CriteriaType.RACE.toString())
            .addParentId(1L)
            .addConceptId(String.valueOf(TestConcepts.WHITE.conceptId))
            .addName(TestConcepts.WHITE.name)
            .build());
    cbCriteriaDao.save(
        DbCriteria.builder()
            .addDomainId(Domain.PERSON.toString())
            .addType(CriteriaType.RACE.toString())
            .addParentId(1L)
            .addConceptId(String.valueOf(TestConcepts.PREFER_NOT_TO_ANSWER_RACE.conceptId))
            .addName(TestConcepts.PREFER_NOT_TO_ANSWER_RACE.name)
            .build());
    cbCriteriaDao.save(
        DbCriteria.builder()
            .addDomainId(Domain.PERSON.toString())
            .addType(CriteriaType.RACE.toString())
            .addParentId(1L)
            .addConceptId(String.valueOf(TestConcepts.PREFER_NOT_TO_ANSWER_ETH.conceptId))
            .addName(TestConcepts.PREFER_NOT_TO_ANSWER_ETH.name)
            .build());
    cbCriteriaDao.save(
        DbCriteria.builder()
            .addDomainId(Domain.PERSON.toString())
            .addType(CriteriaType.SEX.toString())
            .addParentId(1L)
            .addConceptId(String.valueOf(TestConcepts.SEX_AT_BIRTH.conceptId))
            .addName(TestConcepts.SEX_AT_BIRTH.name)
            .build());

    cdrVersion = new DbCdrVersion();
    cdrVersion.setBigqueryDataset("dataSetId");
    cdrVersion.setBigqueryProject("projectId");
    cdrVersionDao.save(cdrVersion);
    CdrVersionContext.setCdrVersionNoCheckAuthDomain(cdrVersion);

    workspace = new DbWorkspace();
    workspace.setCdrVersion(cdrVersion);
    workspace.setWorkspaceNamespace(WORKSPACE_NAMESPACE);
    workspace.setName(WORKSPACE_NAME);
    workspace.setFirecloudName(WORKSPACE_NAME);
    workspace.setWorkspaceId(1L);
    workspaceDao.save(workspace);

    workspace2 = new DbWorkspace();
    workspace2.setCdrVersion(cdrVersion);
    workspace2.setWorkspaceNamespace(WORKSPACE_NAMESPACE2);
    workspace2.setName(WORKSPACE_NAME2);
    workspace2.setFirecloudName(WORKSPACE_NAME2);
    workspace2.setWorkspaceId(2L);
    workspaceDao.save(workspace2);

    cohort = new DbCohort();
    cohort.setWorkspaceId(workspace.getWorkspaceId());
    String criteria =
        "{\"includes\":[{\"id\":\"includes_kl4uky6kh\",\"items\":[{\"id\":\"items_58myrn9iz\",\"type\":\"CONDITION\",\"searchParameters\":[{"
            + "\"parameterId\":\"param1567486C34\",\"name\":\"Malignant neoplasm of bronchus and lung\",\"domain\":\"CONDITION\",\"type\": "
            + "\"ICD10CM\",\"group\":false,\"attributes\":[],\"ancestorData\":false,\"standard\":false,\"conceptId\":1567486,\"value\":\"C34\"}],"
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
        participantCohortStatusDao.save(
            new DbParticipantCohortStatus()
                .status(DbStorageEnums.cohortStatusToStorage(CohortStatus.NOT_REVIEWED))
                .participantKey(key1)
                .genderConceptId(TestConcepts.MALE.getConceptId())
                .raceConceptId(TestConcepts.ASIAN.getConceptId())
                .ethnicityConceptId(TestConcepts.NOT_HISPANIC.getConceptId())
                .sexAtBirthConceptId(TestConcepts.SEX_AT_BIRTH.getConceptId())
                .birthDate(new java.sql.Date(today.getTime()))
                .deceased(false));

    participantCohortStatus2 =
        participantCohortStatusDao.save(
            new DbParticipantCohortStatus()
                .status(DbStorageEnums.cohortStatusToStorage(CohortStatus.NOT_REVIEWED))
                .participantKey(key2)
                .genderConceptId(TestConcepts.FEMALE.getConceptId())
                .raceConceptId(TestConcepts.WHITE.getConceptId())
                .ethnicityConceptId(TestConcepts.NOT_HISPANIC.getConceptId())
                .sexAtBirthConceptId(TestConcepts.SEX_AT_BIRTH.getConceptId())
                .birthDate(new java.sql.Date(today.getTime()))
                .deceased(false));

    stringAnnotationDefinition =
        cohortAnnotationDefinitionDao.save(
            new DbCohortAnnotationDefinition()
                .annotationType(DbStorageEnums.annotationTypeToStorage(AnnotationType.STRING))
                .columnName("test")
                .cohortId(cohort.getCohortId()));
    enumAnnotationDefinition =
        new DbCohortAnnotationDefinition()
            .annotationType(DbStorageEnums.annotationTypeToStorage(AnnotationType.ENUM))
            .columnName("test")
            .cohortId(cohort.getCohortId());
    SortedSet<DbCohortAnnotationEnumValue> enumValues = new TreeSet<>();
    enumValues.add(
        new DbCohortAnnotationEnumValue()
            .name("test")
            .cohortAnnotationDefinition(enumAnnotationDefinition));
    enumAnnotationDefinition =
        cohortAnnotationDefinitionDao.save(enumAnnotationDefinition.enumValues(enumValues));
    dateAnnotationDefinition =
        new DbCohortAnnotationDefinition()
            .annotationType(DbStorageEnums.annotationTypeToStorage(AnnotationType.DATE))
            .columnName("test")
            .cohortId(cohort.getCohortId());
    cohortAnnotationDefinitionDao.save(dateAnnotationDefinition);

    booleanAnnotationDefinition =
        cohortAnnotationDefinitionDao.save(
            new DbCohortAnnotationDefinition()
                .annotationType(DbStorageEnums.annotationTypeToStorage(AnnotationType.BOOLEAN))
                .columnName("test")
                .cohortId(cohort.getCohortId()));

    integerAnnotationDefinition =
        cohortAnnotationDefinitionDao.save(
            new DbCohortAnnotationDefinition()
                .annotationType(DbStorageEnums.annotationTypeToStorage(AnnotationType.INTEGER))
                .columnName("test")
                .cohortId(cohort.getCohortId()));

    participantAnnotation =
        participantCohortAnnotationDao.save(
            new DbParticipantCohortAnnotation()
                .cohortReviewId(cohortReview.getCohortReviewId())
                .participantId(participantCohortStatus1.getParticipantKey().getParticipantId())
                .annotationValueString("test")
                .cohortAnnotationDefinitionId(
                    stringAnnotationDefinition.getCohortAnnotationDefinitionId()));
  }

  @Test
  public void createCohortReviewLessThanMinSize() {
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
  public void createCohortReviewMoreThanMaxSize() {
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
  public void createCohortReviewAlreadyExists() {
    when(workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
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
  public void createCohortReview() {
    when(workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            WORKSPACE_NAMESPACE, WORKSPACE_NAME, WorkspaceAccessLevel.WRITER))
        .thenReturn(workspace);

    stubBigQueryCohortCalls();

    CohortReview cohortReview =
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
  public void createCohortReviewNoCohortException() {
    long cohortId = 99;
    when(workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
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
      assertThat(nfe.getMessage())
          .isEqualTo("Not Found: No Cohort exists for cohortId: " + cohortId);
    }
  }

  @Test
  public void updateCohortReview() {
    when(workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            WORKSPACE_NAMESPACE, WORKSPACE_NAME, WorkspaceAccessLevel.WRITER))
        .thenReturn(workspace);

    CohortReview requestCohortReview =
        new CohortReview()
            .cohortReviewId(cohortReview.getCohortReviewId())
            .cohortId(cohortReview.getCohortId())
            .etag(Etags.fromVersion(cohortReview.getVersion()));
    requestCohortReview.setCohortName("blahblah");
    requestCohortReview.setDescription("new desc");
    CohortReview responseCohortReview =
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
  public void deleteCohortReviewWrongWorkspace() {
    when(workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            WORKSPACE_NAMESPACE2, WORKSPACE_NAME2, WorkspaceAccessLevel.WRITER))
        .thenReturn(workspace2);
    CohortReview requestCohortReview =
        new CohortReview()
            .cohortReviewId(cohortReview.getCohortReviewId())
            .etag(Etags.fromVersion(cohortReview.getVersion()));

    assertThrows(
        NotFoundException.class,
        () ->
            cohortReviewController.deleteCohortReview(
                WORKSPACE_NAMESPACE2, WORKSPACE_NAME2, requestCohortReview.getCohortReviewId()));
  }

  @Test
  public void deleteParticipantCohortAnnotationWrongWorkspace() {
    when(workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            WORKSPACE_NAMESPACE2, WORKSPACE_NAME2, WorkspaceAccessLevel.WRITER))
        .thenReturn(workspace2);

    DbParticipantCohortAnnotation annotation =
        new DbParticipantCohortAnnotation()
            .cohortReviewId(cohortReview.getCohortReviewId())
            .participantId(participantCohortStatus1.getParticipantKey().getParticipantId())
            .annotationValueString("test")
            .cohortAnnotationDefinitionId(
                stringAnnotationDefinition.getCohortAnnotationDefinitionId());

    assertThrows(
        NotFoundException.class,
        () ->
            cohortReviewController.deleteParticipantCohortAnnotation(
                WORKSPACE_NAMESPACE2,
                WORKSPACE_NAME2,
                cohortReview.getCohortReviewId(),
                participantCohortStatus1.getParticipantKey().getParticipantId(),
                annotation.getAnnotationId()));
  }

  @Test
  public void getParticipantCohortAnnotationsWrongWorkspace() {
    when(workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            WORKSPACE_NAMESPACE2, WORKSPACE_NAME2, WorkspaceAccessLevel.READER))
        .thenReturn(workspace2);
    assertThrows(
        NotFoundException.class,
        () ->
            cohortReviewController.getParticipantCohortAnnotations(
                WORKSPACE_NAMESPACE2,
                WORKSPACE_NAME2,
                cohortReview.getCohortReviewId(),
                participantCohortStatus1.getParticipantKey().getParticipantId()));
  }

  @Test
  public void getParticipantCohortStatusWrongWorkspace() {
    when(workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            WORKSPACE_NAMESPACE2, WORKSPACE_NAME2, WorkspaceAccessLevel.READER))
        .thenReturn(workspace2);
    assertThrows(
        NotFoundException.class,
        () ->
            cohortReviewController.getParticipantCohortStatus(
                WORKSPACE_NAMESPACE2,
                WORKSPACE_NAME2,
                cohortReview.getCohortReviewId(),
                participantCohortStatus1.getParticipantKey().getParticipantId()));
  }

  @Test
  public void updateCohortReviewWrongWorkspace() {
    when(workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            WORKSPACE_NAMESPACE2, WORKSPACE_NAME2, WorkspaceAccessLevel.WRITER))
        .thenReturn(workspace2);
    CohortReview requestCohortReview =
        new CohortReview()
            .cohortReviewId(cohortReview.getCohortReviewId())
            .cohortId(cohortReview.getCohortId())
            .etag(Etags.fromVersion(cohortReview.getVersion()));
    requestCohortReview.setCohortName("blahblah");
    requestCohortReview.setDescription("new desc");

    assertThrows(
        NotFoundException.class,
        () ->
            cohortReviewController.updateCohortReview(
                WORKSPACE_NAMESPACE2,
                WORKSPACE_NAME2,
                requestCohortReview.getCohortReviewId(),
                requestCohortReview));
  }

  @Test
  public void updateParticipantCohortStatusWrongWorkspace() {
    when(workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            WORKSPACE_NAMESPACE2, WORKSPACE_NAME2, WorkspaceAccessLevel.WRITER))
        .thenReturn(workspace2);
    ;
    assertThrows(
        NotFoundException.class,
        () ->
            cohortReviewController.updateParticipantCohortStatus(
                WORKSPACE_NAMESPACE2,
                WORKSPACE_NAME2,
                cohortReview.getCohortReviewId(),
                participantCohortStatus1.getParticipantKey().getParticipantId(),
                new ModifyCohortStatusRequest().status(CohortStatus.INCLUDED)));
  }

  @Test
  public void deleteCohortReview() {
    when(workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            WORKSPACE_NAMESPACE, WORKSPACE_NAME, WorkspaceAccessLevel.WRITER))
        .thenReturn(workspace);

    CohortReview requestCohortReview =
        new CohortReview()
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
  public void createParticipantCohortAnnotationNoAnnotationDefinitionFound() {
    Long participantId = participantCohortStatus1.getParticipantKey().getParticipantId();

    when(workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
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
  public void createParticipantCohortAnnotationNoAnnotationValue() {
    Long participantId = participantCohortStatus1.getParticipantKey().getParticipantId();
    assertConflictExceptionForAnnotationType(
        participantId, stringAnnotationDefinition.getCohortAnnotationDefinitionId(), "STRING");
    assertConflictExceptionForAnnotationType(
        participantId, enumAnnotationDefinition.getCohortAnnotationDefinitionId(), "ENUM");
    assertConflictExceptionForAnnotationType(
        participantId, dateAnnotationDefinition.getCohortAnnotationDefinitionId(), "DATE");
    assertConflictExceptionForAnnotationType(
        participantId, booleanAnnotationDefinition.getCohortAnnotationDefinitionId(), "BOOLEAN");
    assertConflictExceptionForAnnotationType(
        participantId, integerAnnotationDefinition.getCohortAnnotationDefinitionId(), "INTEGER");
  }

  @Test
  public void createParticipantCohortAnnotation() {
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
  public void deleteParticipantCohortAnnotation() {
    DbParticipantCohortAnnotation annotation =
        new DbParticipantCohortAnnotation()
            .cohortReviewId(cohortReview.getCohortReviewId())
            .participantId(participantCohortStatus1.getParticipantKey().getParticipantId())
            .annotationValueString("test")
            .cohortAnnotationDefinitionId(
                stringAnnotationDefinition.getCohortAnnotationDefinitionId());
    participantCohortAnnotationDao.save(annotation);

    when(workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            WORKSPACE_NAMESPACE, WORKSPACE_NAME, WorkspaceAccessLevel.WRITER))
        .thenReturn(workspace);

    cohortReviewController.deleteParticipantCohortAnnotation(
        WORKSPACE_NAMESPACE,
        WORKSPACE_NAME,
        cohortReview.getCohortReviewId(),
        participantCohortStatus1.getParticipantKey().getParticipantId(),
        annotation.getAnnotationId());

    assertThat(participantCohortAnnotationDao.findById(annotation.getAnnotationId()).isPresent())
        .isFalse();
  }

  @Test
  public void deleteParticipantCohortAnnotationNoAnnotation() {
    Long participantId = participantCohortStatus1.getParticipantKey().getParticipantId();
    Long annotationId = 9999L;

    when(workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
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
  public void getParticipantCohortAnnotations() {
    when(workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
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
  public void getParticipantCohortStatus() {
    when(workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            WORKSPACE_NAMESPACE, WORKSPACE_NAME, WorkspaceAccessLevel.READER))
        .thenReturn(workspace);

    ParticipantCohortStatus response =
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
    assertThat(response.getSexAtBirthConceptId())
        .isEqualTo(participantCohortStatus1.getSexAtBirthConceptId());
  }

  @Test
  public void getParticipantCohortStatuses() {
    int page = 0;
    int pageSize = 25;
    CohortReview expectedReview1 =
        createCohortReview(
            cohortReview, ImmutableList.of(participantCohortStatus1, participantCohortStatus2));
    CohortReview expectedReview2 =
        createCohortReview(
            cohortReview, ImmutableList.of(participantCohortStatus2, participantCohortStatus1));
    CohortReview expectedReview3 =
        createCohortReview(
            cohortReview, ImmutableList.of(participantCohortStatus1, participantCohortStatus2));
    CohortReview expectedReview4 =
        createCohortReview(
            cohortReview, ImmutableList.of(participantCohortStatus1, participantCohortStatus2));

    when(workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            WORKSPACE_NAMESPACE, WORKSPACE_NAME, WorkspaceAccessLevel.READER))
        .thenReturn(workspace);

    assertParticipantCohortStatuses(
        expectedReview1, page, pageSize, SortOrder.DESC, FilterColumns.STATUS);

    assertParticipantCohortStatuses(
        expectedReview2, page, pageSize, SortOrder.DESC, FilterColumns.PARTICIPANTID);

    assertParticipantCohortStatuses(expectedReview3, null, null, null, FilterColumns.STATUS);

    assertParticipantCohortStatuses(expectedReview4, null, null, SortOrder.ASC, null);

    assertParticipantCohortStatuses(expectedReview4, null, pageSize, null, null);

    assertParticipantCohortStatuses(expectedReview4, page, null, null, null);

    assertParticipantCohortStatuses(expectedReview4, null, null, null, null);
  }

  @Test
  public void updateParticipantCohortAnnotation() {
    when(workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
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
  public void updateParticipantCohortAnnotationNoAnnotationForIdException() {
    long badAnnotationId = 99;
    when(workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
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
      assertThat(nfe.getMessage())
          .isEqualTo(
              "Not Found: Participant Cohort Annotation does not exist for annotationId: "
                  + badAnnotationId
                  + ", cohortReviewId: "
                  + cohortReview.getCohortReviewId()
                  + ", participantId: "
                  + participantCohortStatus1.getParticipantKey().getParticipantId());
    }
  }

  @Test
  public void updateParticipantCohortStatus() {
    when(workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            WORKSPACE_NAMESPACE, WORKSPACE_NAME, WorkspaceAccessLevel.WRITER))
        .thenReturn(workspace);

    ParticipantCohortStatus participantCohortStatus =
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

  /** Helper method to consolidate assertions for all the {@link AnnotationType}s. */
  private void assertCreateParticipantCohortAnnotation(
      Long participantId, Long annotationDefinitionId, ParticipantCohortAnnotation request) {
    when(workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
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
   */
  private void assertConflictExceptionForAnnotationType(
      Long participantId, Long cohortAnnotationDefId, String type) {
    when(workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
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
      fail("Should have thrown a ConflictException!");
    } catch (ConflictException ce) {
      // Success
      assertThat(ce.getMessage())
          .isEqualTo(
              "Conflict Exception: Please provide a valid "
                  + type
                  + " value for annotation defintion id: "
                  + cohortAnnotationDefId);
    }
  }

  /**
   * Helper method to assert results for {@link
   * CohortReviewController#getParticipantCohortStatuses(String, String, Long, Long,
   * PageFilterRequest)}.
   */
  private void assertParticipantCohortStatuses(
      CohortReview expectedReview,
      Integer page,
      Integer pageSize,
      SortOrder sortOrder,
      FilterColumns sortColumn) {
    CohortReview actualReview =
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
            .getBody()
            .getCohortReview();
    verify(userRecentResourceService, atLeastOnce())
        .updateCohortEntry(anyLong(), anyLong(), anyLong());
    assertThat(actualReview).isEqualTo(expectedReview);
  }

  private void stubBigQueryCohortCalls() {
    TableResult queryResult = mock(TableResult.class);
    Iterable testIterable =
        () -> {
          List<FieldValue> list = new ArrayList<>();
          list.add(null);
          return list.iterator();
        };
    Map<String, Integer> rm =
        ImmutableMap.<String, Integer>builder()
            .put("person_id", 0)
            .put("birth_datetime", 1)
            .put("gender_concept_id", 2)
            .put("race_concept_id", 3)
            .put("ethnicity_concept_id", 4)
            .put("sex_at_birth_concept_id", 5)
            .put("count", 6)
            .put("deceased", 7)
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
    when(bigQueryService.getLong(null, 6)).thenReturn(0L);
  }

  private CohortReview createCohortReview(
      DbCohortReview actualReview, List<DbParticipantCohortStatus> participantCohortStatusList) {
    List<ParticipantCohortStatus> newParticipantCohortStatusList =
        participantCohortStatusList.stream()
            .map(this::dbParticipantCohortStatusToApi)
            .collect(Collectors.toList());

    return new CohortReview()
        .cohortReviewId(actualReview.getCohortReviewId())
        .etag(Etags.fromVersion(actualReview.getVersion()))
        .cohortId(actualReview.getCohortId())
        .cdrVersionId(actualReview.getCdrVersionId())
        .creationTime(actualReview.getCreationTime().getTime())
        .matchedParticipantCount(actualReview.getMatchedParticipantCount())
        .reviewSize(actualReview.getReviewSize())
        .reviewedCount(actualReview.getReviewedCount())
        .participantCohortStatuses(newParticipantCohortStatusList);
  }

  private ParticipantCohortStatus dbParticipantCohortStatusToApi(
      DbParticipantCohortStatus dbStatus) {
    Map<Long, String> demographicsMap = TestConcepts.asMap();
    return new ParticipantCohortStatus()
        .birthDate(dbStatus.getBirthDate().toString())
        .ethnicityConceptId(dbStatus.getEthnicityConceptId())
        .ethnicity(demographicsMap.get(dbStatus.getEthnicityConceptId()))
        .genderConceptId(dbStatus.getGenderConceptId())
        .gender(demographicsMap.get(dbStatus.getGenderConceptId()))
        .participantId(dbStatus.getParticipantKey().getParticipantId())
        .raceConceptId(dbStatus.getRaceConceptId())
        .race(demographicsMap.get(dbStatus.getRaceConceptId()))
        .sexAtBirthConceptId(dbStatus.getSexAtBirthConceptId())
        .sexAtBirth(demographicsMap.get(dbStatus.getSexAtBirthConceptId()))
        .status(DbStorageEnums.cohortStatusFromStorage(dbStatus.getStatus()))
        .deceased(dbStatus.getDeceased());
  }
}
