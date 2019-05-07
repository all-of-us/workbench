package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;

import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.TableResult;
import com.google.common.collect.ImmutableMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.cdr.CdrVersionContext;
import org.pmiops.workbench.cdr.CdrVersionService;
import org.pmiops.workbench.cdr.cache.GenderRaceEthnicityConcept;
import org.pmiops.workbench.cohortbuilder.CohortQueryBuilder;
import org.pmiops.workbench.cohortbuilder.ParticipantCounter;
import org.pmiops.workbench.cohortbuilder.QueryBuilderFactory;
import org.pmiops.workbench.cohortbuilder.TemporalQueryBuilder;
import org.pmiops.workbench.cohortreview.CohortReviewServiceImpl;
import org.pmiops.workbench.cohortreview.ReviewQueryBuilder;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.CohortAnnotationDefinitionDao;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.CohortReviewDao;
import org.pmiops.workbench.db.dao.ParticipantCohortAnnotationDao;
import org.pmiops.workbench.db.dao.ParticipantCohortStatusDao;
import org.pmiops.workbench.db.dao.UserRecentResourceService;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.pmiops.workbench.db.model.CdrVersion;
import org.pmiops.workbench.db.model.Cohort;
import org.pmiops.workbench.db.model.CohortAnnotationDefinition;
import org.pmiops.workbench.db.model.CohortAnnotationEnumValue;
import org.pmiops.workbench.db.model.CohortReview;
import org.pmiops.workbench.db.model.ParticipantCohortStatus;
import org.pmiops.workbench.db.model.ParticipantCohortStatusKey;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.db.model.StorageEnums;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.AnnotationType;
import org.pmiops.workbench.model.CohortStatus;
import org.pmiops.workbench.model.CreateReviewRequest;
import org.pmiops.workbench.model.DataAccessLevel;
import org.pmiops.workbench.model.ModifyCohortStatusRequest;
import org.pmiops.workbench.model.ModifyParticipantCohortAnnotationRequest;
import org.pmiops.workbench.model.PageFilterRequest;
import org.pmiops.workbench.model.ParticipantCohortAnnotation;
import org.pmiops.workbench.model.ParticipantCohortAnnotationListResponse;
import org.pmiops.workbench.model.ParticipantCohortStatusColumns;
import org.pmiops.workbench.model.ParticipantCohortStatuses;
import org.pmiops.workbench.model.ReviewStatus;
import org.pmiops.workbench.model.SortOrder;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.test.FakeClock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.verify;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.Assert.fail;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
@ComponentScan(basePackages = "org.pmiops.workbench.cohortbuilder.*")
public class CohortReviewControllerTest {

  private static final String WORKSPACE_NAMESPACE = "namespace";
  private static final String WORKSPACE_NAME = "name";
  private CdrVersion cdrVersion;
  private CohortReview cohortReview;
  private Cohort cohort;
  private Cohort cohortWithoutReview;
  private ParticipantCohortStatus participantCohortStatus1;
  private ParticipantCohortStatus participantCohortStatus2;
  private Workspace workspace;
  private static final Instant NOW = Instant.now();
  private static final FakeClock CLOCK = new FakeClock(NOW, ZoneId.systemDefault());

  private CohortAnnotationDefinition stringAnnotationDefinition;
  private CohortAnnotationDefinition enumAnnotationDefinition;
  private CohortAnnotationDefinition dateAnnotationDefinition;
  private CohortAnnotationDefinition booleanAnnotationDefinition;
  private CohortAnnotationDefinition integerAnnotationDefinition;
  private org.pmiops.workbench.db.model.ParticipantCohortAnnotation participantAnnotation;

  @Autowired
  private CdrVersionDao cdrVersionDao;

  @Autowired
  private WorkspaceDao workspaceDao;

  @Autowired
  private CohortDao cohortDao;

  @Autowired
  private CohortReviewDao cohortReviewDao;

  @Autowired
  private ParticipantCohortStatusDao participantCohortStatusDao;

  @Autowired
  private CohortAnnotationDefinitionDao cohortAnnotationDefinitionDao;

  @Autowired
  private ParticipantCohortAnnotationDao participantCohortAnnotationDao;

  @Autowired
  private WorkspaceService workspaceService;

  @Autowired
  private UserRecentResourceService userRecentResourceService;

  @Autowired
  private CohortReviewController cohortReviewController;

  @Autowired
  private BigQueryService bigQueryService;

  private enum TestDemo {
    ASIAN("Asian", 8515),
    WHITE("White", 8527),
    MALE("MALE", 8507),
    FEMALE("FEMALE", 8532),
    NOT_HISPANIC("Not Hispanic or Latino", 38003564);

    private final String name;
    private final long conceptId;

    private TestDemo(String name, long conceptId) {
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
  @Import({
    CdrVersionService.class,
    CohortReviewController.class,
    CohortReviewServiceImpl.class,
    ParticipantCounter.class,
    CohortQueryBuilder.class,
    ReviewQueryBuilder.class,
    QueryBuilderFactory.class,
    TemporalQueryBuilder.class
  })
  @MockBean({
    BigQueryService.class,
    FireCloudService.class,
    UserRecentResourceService.class,
    WorkspaceService.class,
    User.class
  })
  static class Configuration {
    @Bean
    Clock clock() {
      return CLOCK;
    }

    @Bean
    public GenderRaceEthnicityConcept getGenderRaceEthnicityConcept() {
      Map<String, Map<Long, String>> concepts = new HashMap<>();
      concepts.put(ParticipantCohortStatusColumns.RACE.name(),
        ImmutableMap.of(TestDemo.ASIAN.getConceptId(), TestDemo.ASIAN.getName(),
          TestDemo.WHITE.getConceptId(), TestDemo.WHITE.getName()));
      concepts.put(ParticipantCohortStatusColumns.GENDER.name(),
        ImmutableMap.of(TestDemo.MALE.getConceptId(), TestDemo.MALE.getName(),
          TestDemo.FEMALE.getConceptId(), TestDemo.FEMALE.getName()));
      concepts.put(ParticipantCohortStatusColumns.ETHNICITY.name(),
        ImmutableMap.of(TestDemo.NOT_HISPANIC.getConceptId(), TestDemo.NOT_HISPANIC.getName()));
      return new GenderRaceEthnicityConcept(concepts);
    }
  }

  @Before
  public void setUp() {
    cdrVersion = new CdrVersion();
    cdrVersion.setBigqueryDataset("dataSetId");
    cdrVersion.setBigqueryProject("projectId");
    cdrVersionDao.save(cdrVersion);
    CdrVersionContext.setCdrVersionNoCheckAuthDomain(cdrVersion);

    workspace = new Workspace();
    workspace.setCdrVersion(cdrVersion);
    workspace.setName("name");
    workspace.setDataAccessLevelEnum(DataAccessLevel.PROTECTED);
    workspaceDao.save(workspace);

    cohort = new Cohort();
    cohort.setWorkspaceId(workspace.getWorkspaceId());
    cohort.setCriteria("{\"includes\":[{\"id\":\"includes_9bdr91i2t\",\"items\":[{\"id\":\"items_r0tsp87r4\",\"type\":\"CONDITION\",\"searchParameters\":[{\"parameterId\":\"param25164\"," +
      "\"name\":\"Malignant neoplasm of bronchus and lung\",\"value\":\"C34\",\"type\":\"ICD10\",\"subtype\":\"CM\",\"group\":true,\"domainId\":\"\"}],\"modifiers\":[]}]}],\"excludes\":[]}");
    cohortDao.save(cohort);

    cohortWithoutReview = new Cohort();
    cohortWithoutReview.setWorkspaceId(workspace.getWorkspaceId());
    cohortWithoutReview.setCriteria("{\"includes\":[{\"id\":\"includes_9bdr91i2t\",\"items\":[{\"id\":\"items_r0tsp87r4\",\"type\":\"CONDITION\",\"searchParameters\":[{\"parameterId\":\"param25164\"," +
      "\"name\":\"Malignant neoplasm of bronchus and lung\",\"value\":\"C34\",\"type\":\"ICD10\",\"subtype\":\"CM\",\"group\":false,\"domainId\":\"Condition\",\"conceptId\":\"1\"}],\"modifiers\":[]}]}],\"excludes\":[]}");
    cohortDao.save(cohortWithoutReview);

    Timestamp today = new Timestamp(new Date().getTime());
    cohortReview = cohortReviewDao.save(
      new CohortReview()
      .cohortId(cohort.getCohortId())
      .cdrVersionId(cdrVersion.getCdrVersionId()))
      .reviewSize(2)
      .creationTime(today);

    ParticipantCohortStatusKey key1 = new ParticipantCohortStatusKey()
      .cohortReviewId(cohortReview.getCohortReviewId())
      .participantId(1L);
    ParticipantCohortStatusKey key2 = new ParticipantCohortStatusKey()
      .cohortReviewId(cohortReview.getCohortReviewId())
      .participantId(2L);

    participantCohortStatus1 = new ParticipantCohortStatus()
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
    participantCohortStatus2 = new ParticipantCohortStatus()
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
      new CohortAnnotationDefinition()
        .annotationType(StorageEnums.annotationTypeToStorage(AnnotationType.STRING))
        .columnName("test")
        .cohortId(cohort.getCohortId());
    cohortAnnotationDefinitionDao.save(stringAnnotationDefinition);
    enumAnnotationDefinition =
      new CohortAnnotationDefinition()
        .annotationType(StorageEnums.annotationTypeToStorage(AnnotationType.ENUM))
        .columnName("test")
        .cohortId(cohort.getCohortId());
    SortedSet<CohortAnnotationEnumValue> enumValues = new TreeSet<CohortAnnotationEnumValue>();
    enumValues.add(new CohortAnnotationEnumValue()
      .name("test")
      .cohortAnnotationDefinition(enumAnnotationDefinition));
    cohortAnnotationDefinitionDao.save(enumAnnotationDefinition.enumValues(enumValues));
    dateAnnotationDefinition =
      new CohortAnnotationDefinition()
        .annotationType(StorageEnums.annotationTypeToStorage(AnnotationType.DATE))
        .columnName("test")
        .cohortId(cohort.getCohortId());
    cohortAnnotationDefinitionDao.save(dateAnnotationDefinition);
    booleanAnnotationDefinition =
      new CohortAnnotationDefinition()
        .annotationType(StorageEnums.annotationTypeToStorage(AnnotationType.BOOLEAN))
        .columnName("test")
        .cohortId(cohort.getCohortId());
    cohortAnnotationDefinitionDao.save(booleanAnnotationDefinition);
    integerAnnotationDefinition =
      new CohortAnnotationDefinition()
        .annotationType(StorageEnums.annotationTypeToStorage(AnnotationType.INTEGER))
        .columnName("test")
        .cohortId(cohort.getCohortId());
    cohortAnnotationDefinitionDao.save(integerAnnotationDefinition);

    participantAnnotation =
      new org.pmiops.workbench.db.model.ParticipantCohortAnnotation()
        .cohortReviewId(cohortReview.getCohortReviewId())
        .participantId(participantCohortStatus1.getParticipantKey().getParticipantId())
        .annotationValueString("test")
        .cohortAnnotationDefinitionId(stringAnnotationDefinition.getCohortAnnotationDefinitionId());
    participantCohortAnnotationDao.save(participantAnnotation);
  }

  @After
  public void tearDown() {
    cohortAnnotationDefinitionDao.delete(stringAnnotationDefinition);
    cohortAnnotationDefinitionDao.delete(enumAnnotationDefinition);
    cohortAnnotationDefinitionDao.delete(dateAnnotationDefinition);
    cohortAnnotationDefinitionDao.delete(booleanAnnotationDefinition);
    cohortAnnotationDefinitionDao.delete(integerAnnotationDefinition);
    participantCohortStatusDao.delete(participantCohortStatus1);
    participantCohortStatusDao.delete(participantCohortStatus2);
    cohortReviewDao.delete(cohortReview);
    cohortDao.delete(cohort);
    cohortDao.delete(cohortWithoutReview);
    workspaceDao.delete(workspace);
    cdrVersionDao.delete(cdrVersion);
    participantCohortAnnotationDao.delete(participantAnnotation);
  }

  @Test
  public void createCohortReviewLessThanMinSize() throws Exception {
    try {
      cohortReviewController.createCohortReview(WORKSPACE_NAMESPACE,
        WORKSPACE_NAME,
        cohort.getCohortId(),
        cdrVersion.getCdrVersionId(),
        new CreateReviewRequest()
          .size(0));
      fail("Should have thrown a BadRequestException!");
    } catch (BadRequestException bre) {
      //success
      assertThat(bre.getMessage())
        .isEqualTo("Bad Request: Cohort Review size must be between 0 and 10000");
    }
  }

  @Test
  public void createCohortReviewMoreThanMaxSize() throws Exception {
    try {
      cohortReviewController.createCohortReview(WORKSPACE_NAMESPACE,
        WORKSPACE_NAME,
        cohort.getCohortId(),
        cdrVersion.getCdrVersionId(),
        new CreateReviewRequest()
          .size(10001));
      fail("Should have thrown a BadRequestException!");
    } catch (BadRequestException bre) {
      //success
      assertThat(bre.getMessage())
        .isEqualTo("Bad Request: Cohort Review size must be between 0 and 10000");
    }
  }

  @Test
  public void createCohortReviewAlreadyExists() throws Exception {
    when(workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(WORKSPACE_NAMESPACE,
      WORKSPACE_NAME, WorkspaceAccessLevel.WRITER)).thenReturn(workspace);

    stubBigQueryCohortCalls();

    try {
      cohortReviewController.createCohortReview(WORKSPACE_NAMESPACE,
        WORKSPACE_NAME,
        cohort.getCohortId(),
        cdrVersion.getCdrVersionId(),
        new CreateReviewRequest()
          .size(1));
      fail("Should have thrown a BadRequestException!");
    } catch (BadRequestException bre) {
      //success
      assertThat(bre.getMessage())
        .isEqualTo("Bad Request: Cohort Review already created for cohortId: " +
          cohort.getCohortId() + ", cdrVersionId: " + cdrVersion.getCdrVersionId());
    }
  }

  @Test
  public void createCohortReview() throws Exception {
    when(workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(WORKSPACE_NAMESPACE,
      WORKSPACE_NAME, WorkspaceAccessLevel.WRITER)).thenReturn(workspace);

    stubBigQueryCohortCalls();

    org.pmiops.workbench.model.CohortReview cohortReview =
      cohortReviewController.createCohortReview(WORKSPACE_NAMESPACE,
      WORKSPACE_NAME,
      cohortWithoutReview.getCohortId(),
      cdrVersion.getCdrVersionId(),
      new CreateReviewRequest()
        .size(1)).getBody();

    assertThat(cohortReview.getReviewStatus()).isEqualTo(ReviewStatus.CREATED);
    assertThat(cohortReview.getReviewSize()).isEqualTo(1);
    assertThat(cohortReview.getParticipantCohortStatuses().size()).isEqualTo(1);
    assertThat(cohortReview.getParticipantCohortStatuses().get(0).getStatus()).isEqualTo(CohortStatus.NOT_REVIEWED);
  }

  @Test
  public void createCohortReviewNoCohortException() throws Exception {
    long cohortId = 99;
    when(workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(WORKSPACE_NAMESPACE,
      WORKSPACE_NAME, WorkspaceAccessLevel.WRITER)).thenReturn(workspace);

    stubBigQueryCohortCalls();

    try {
      cohortReviewController.createCohortReview(WORKSPACE_NAMESPACE,
        WORKSPACE_NAME,
        cohortId,
        cdrVersion.getCdrVersionId(),
        new CreateReviewRequest()
          .size(1)).getBody();
      fail("Should have thrown NotFoundException!");
    } catch (NotFoundException nfe) {
      assertEquals("Not Found: No Cohort exists for cohortId: " + cohortId, nfe.getMessage());
    }
  }

  @Test
  public void createCohortReviewNoMatchingWorkspaceException() throws Exception {
    String badWorkspaceName = WORKSPACE_NAME + "bad";
    when(workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(WORKSPACE_NAMESPACE,
      WORKSPACE_NAME, WorkspaceAccessLevel.WRITER)).thenReturn(workspace);

    stubBigQueryCohortCalls();

    try {
      cohortReviewController.createCohortReview(WORKSPACE_NAMESPACE,
        badWorkspaceName,
        cohortWithoutReview.getCohortId(),
        cdrVersion.getCdrVersionId(),
        new CreateReviewRequest()
          .size(1)).getBody();
      fail("Should have thrown NotFoundException!");
    } catch (NotFoundException nfe) {
      assertEquals("Not Found: No workspace matching workspaceNamespace: " +
        WORKSPACE_NAMESPACE + ", workspaceId: " + badWorkspaceName, nfe.getMessage());
    }
  }

  @Test
  public void createParticipantCohortAnnotationNoAnnotationDefinitionId() throws Exception {
    Long participantId = participantCohortStatus1.getParticipantKey().getParticipantId();

    try {
      cohortReviewController.createParticipantCohortAnnotation(WORKSPACE_NAMESPACE,
        WORKSPACE_NAME,
        cohort.getCohortId(),
        cdrVersion.getCdrVersionId(),
        participantId,
        new ParticipantCohortAnnotation()).getBody();
      fail("Should have thrown a BadRequestException!");
    } catch (BadRequestException bre) {
      //Success
      assertThat(bre.getMessage())
        .isEqualTo("Bad Request: Please provide a valid cohort annotation definition id.");
    }

  }

  @Test
  public void createParticipantCohortAnnotationNoAnnotationDefinitionFound() throws Exception {
    Long participantId = participantCohortStatus1.getParticipantKey().getParticipantId();

    when(workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(WORKSPACE_NAMESPACE,
      WORKSPACE_NAME, WorkspaceAccessLevel.WRITER)).thenReturn(workspace);

    try {
      cohortReviewController.createParticipantCohortAnnotation(WORKSPACE_NAMESPACE,
        WORKSPACE_NAME,
        cohort.getCohortId(),
        cdrVersion.getCdrVersionId(),
        participantId,
        new ParticipantCohortAnnotation()
          .cohortReviewId(cohortReview.getCohortReviewId())
          .participantId(participantId)
          .annotationValueString("test")
          .cohortAnnotationDefinitionId(9999L)).getBody();
      fail("Should have thrown a NotFoundException!");
    } catch (NotFoundException nfe) {
      //Success
      assertThat(nfe.getMessage())
        .isEqualTo("Not Found: No cohort annotation definition found for id: 9999");
    }
  }

  @Test
  public void createParticipantCohortAnnotationNoParticipantId() throws Exception {
    Long participantId = participantCohortStatus1.getParticipantKey().getParticipantId();

    when(workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(WORKSPACE_NAMESPACE,
      WORKSPACE_NAME, WorkspaceAccessLevel.WRITER)).thenReturn(workspace);

    try {
      cohortReviewController.createParticipantCohortAnnotation(WORKSPACE_NAMESPACE,
        WORKSPACE_NAME,
        cohort.getCohortId(),
        cdrVersion.getCdrVersionId(),
        participantId,
        new ParticipantCohortAnnotation()
          .cohortReviewId(cohortReview.getCohortReviewId())
          .annotationValueString("test")
          .cohortAnnotationDefinitionId(stringAnnotationDefinition.getCohortAnnotationDefinitionId())).getBody();
      fail("Should have thrown a BadRequestException!");
    } catch (BadRequestException bre) {
      //Success
      assertThat(bre.getMessage())
        .isEqualTo("Bad Request: Please provide a valid participant id.");
    }
  }

  @Test
  public void createParticipantCohortAnnotationNoReviewId() throws Exception {
    Long participantId = participantCohortStatus1.getParticipantKey().getParticipantId();

    when(workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(WORKSPACE_NAMESPACE,
      WORKSPACE_NAME, WorkspaceAccessLevel.WRITER)).thenReturn(workspace);

    try {
      cohortReviewController.createParticipantCohortAnnotation(WORKSPACE_NAMESPACE,
        WORKSPACE_NAME,
        cohort.getCohortId(),
        cdrVersion.getCdrVersionId(),
        participantId,
        new ParticipantCohortAnnotation()
          .participantId(participantId)
          .annotationValueString("test")
          .cohortAnnotationDefinitionId(stringAnnotationDefinition.getCohortAnnotationDefinitionId())).getBody();
      fail("Should have thrown a BadRequestException!");
    } catch (BadRequestException bre) {
      //Success
      assertThat(bre.getMessage())
        .isEqualTo("Bad Request: Please provide a valid cohort review id.");
    }
  }

  @Test
  public void createParticipantCohortAnnotationNoAnnotationValue() throws Exception {
    Long participantId = participantCohortStatus1.getParticipantKey().getParticipantId();
    assertBadRequestExceptionForAnnotationType(participantId,
      stringAnnotationDefinition.getCohortAnnotationDefinitionId(), "STRING");
    assertBadRequestExceptionForAnnotationType(participantId,
      enumAnnotationDefinition.getCohortAnnotationDefinitionId(), "ENUM");
    assertBadRequestExceptionForAnnotationType(participantId,
      dateAnnotationDefinition.getCohortAnnotationDefinitionId(), "DATE");
    assertBadRequestExceptionForAnnotationType(participantId,
      booleanAnnotationDefinition.getCohortAnnotationDefinitionId(), "BOOLEAN");
    assertBadRequestExceptionForAnnotationType(participantId,
      integerAnnotationDefinition.getCohortAnnotationDefinitionId(), "INTEGER");
  }

  @Test
  public void createParticipantCohortAnnotation() throws Exception {
    Long participantId = participantCohortStatus1.getParticipantKey().getParticipantId();
    participantCohortAnnotationDao.delete(participantAnnotation);

    assertCreateParticipantCohortAnnotation(participantId,
      stringAnnotationDefinition.getCohortAnnotationDefinitionId(),
      new ParticipantCohortAnnotation()
        .cohortReviewId(cohortReview.getCohortReviewId())
        .participantId(participantId)
        .annotationValueString("test")
        .cohortAnnotationDefinitionId(stringAnnotationDefinition.getCohortAnnotationDefinitionId()));
    assertCreateParticipantCohortAnnotation(participantId,
      enumAnnotationDefinition.getCohortAnnotationDefinitionId(),
      new ParticipantCohortAnnotation()
        .cohortReviewId(cohortReview.getCohortReviewId())
        .participantId(participantId)
        .annotationValueEnum("test")
        .cohortAnnotationDefinitionId(enumAnnotationDefinition.getCohortAnnotationDefinitionId()));
    assertCreateParticipantCohortAnnotation(participantId,
      dateAnnotationDefinition.getCohortAnnotationDefinitionId(),
      new ParticipantCohortAnnotation()
        .cohortReviewId(cohortReview.getCohortReviewId())
        .participantId(participantId)
        .annotationValueDate("2018-02-02")
        .cohortAnnotationDefinitionId(dateAnnotationDefinition.getCohortAnnotationDefinitionId()));
    assertCreateParticipantCohortAnnotation(participantId,
      booleanAnnotationDefinition.getCohortAnnotationDefinitionId(),
      new ParticipantCohortAnnotation()
        .cohortReviewId(cohortReview.getCohortReviewId())
        .participantId(participantId)
        .annotationValueBoolean(true)
        .cohortAnnotationDefinitionId(booleanAnnotationDefinition.getCohortAnnotationDefinitionId()));
    assertCreateParticipantCohortAnnotation(participantId,
      integerAnnotationDefinition.getCohortAnnotationDefinitionId(),
      new ParticipantCohortAnnotation()
        .cohortReviewId(cohortReview.getCohortReviewId())
        .participantId(participantId)
        .annotationValueInteger(1)
        .cohortAnnotationDefinitionId(integerAnnotationDefinition.getCohortAnnotationDefinitionId()));
  }

  @Test
  public void deleteParticipantCohortAnnotation() throws Exception {
    org.pmiops.workbench.db.model.ParticipantCohortAnnotation annotation =
      new org.pmiops.workbench.db.model.ParticipantCohortAnnotation()
        .cohortReviewId(cohortReview.getCohortReviewId())
        .participantId(participantCohortStatus1.getParticipantKey().getParticipantId())
        .annotationValueString("test")
        .cohortAnnotationDefinitionId(stringAnnotationDefinition.getCohortAnnotationDefinitionId());
    participantCohortAnnotationDao.save(annotation);

    when(workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(WORKSPACE_NAMESPACE,
      WORKSPACE_NAME, WorkspaceAccessLevel.WRITER)).thenReturn(workspace);

    cohortReviewController.deleteParticipantCohortAnnotation(WORKSPACE_NAMESPACE,
      WORKSPACE_NAME,
      cohort.getCohortId(),
      cdrVersion.getCdrVersionId(),
      participantCohortStatus1.getParticipantKey().getParticipantId(),
      annotation.getAnnotationId());

    assertThat(participantCohortAnnotationDao.findOne(annotation.getAnnotationId())).isEqualTo(null);
  }

  @Test
  public void deleteParticipantCohortAnnotationNullAnnotationId() throws Exception {
    try {
      cohortReviewController.deleteParticipantCohortAnnotation(WORKSPACE_NAMESPACE,
        WORKSPACE_NAME,
        cohort.getCohortId(),
        cdrVersion.getCdrVersionId(),
        participantCohortStatus1.getParticipantKey().getParticipantId(),
        null);
      fail("Should have thrown a BadRequestException!");
    } catch (BadRequestException bre) {
      //Success
      assertThat(bre.getMessage())
        .isEqualTo("Bad Request: Please provide a valid cohort annotation definition id.");
    }
  }

  @Test
  public void deleteParticipantCohortAnnotationNoAnnotation() throws Exception {
    Long participantId = participantCohortStatus1.getParticipantKey().getParticipantId();
    Long annotationId = 9999L;

    when(workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(WORKSPACE_NAMESPACE,
      WORKSPACE_NAME, WorkspaceAccessLevel.WRITER)).thenReturn(workspace);

    try {
      cohortReviewController.deleteParticipantCohortAnnotation(WORKSPACE_NAMESPACE,
        WORKSPACE_NAME,
        cohort.getCohortId(),
        cdrVersion.getCdrVersionId(),
        participantId,
        annotationId);
      fail("Should have thrown a NotFoundException!");
    } catch (NotFoundException nfe) {
      //Success
      assertThat(nfe.getMessage())
        .isEqualTo("Not Found: No participant cohort annotation found for annotationId: " +
          annotationId + ", cohortReviewId: " + cohortReview.getCohortReviewId() + ", participantId: " + participantId);
    }
  }

  @Test
  public void deleteParticipantCohortAnnotationNullParticipantId() throws Exception {
    when(workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(WORKSPACE_NAMESPACE,
      WORKSPACE_NAME, WorkspaceAccessLevel.WRITER)).thenReturn(workspace);

    try {
      cohortReviewController.deleteParticipantCohortAnnotation(WORKSPACE_NAMESPACE,
        WORKSPACE_NAME,
        cohort.getCohortId(),
        cdrVersion.getCdrVersionId(),
        null,
        1L);
      fail("Should have thrown a BadRequestException!");
    } catch (BadRequestException bre) {
      //Success
      assertThat(bre.getMessage()).isEqualTo("Bad Request: Please provide a valid participant id.");
    }
  }

  @Test
  public void deleteParticipantCohortAnnotationNoParticipant() throws Exception {
    Long participantId = 9999L;

    when(workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(WORKSPACE_NAMESPACE,
      WORKSPACE_NAME, WorkspaceAccessLevel.WRITER)).thenReturn(workspace);

    try {
      cohortReviewController.deleteParticipantCohortAnnotation(WORKSPACE_NAMESPACE,
        WORKSPACE_NAME,
        cohort.getCohortId(),
        cdrVersion.getCdrVersionId(),
        participantId,
        1L);
      fail("Should have thrown a NotFoundException!");
    } catch (NotFoundException nfe) {
      //Success
      assertThat(nfe.getMessage())
        .isEqualTo("Not Found: Participant Cohort Status does not exist for cohortReviewId: " +
          cohortReview.getCohortReviewId() + ", participantId: " + participantId);
    }
  }

  @Test
  public void getParticipantCohortAnnotations() throws Exception {
    when(workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(WORKSPACE_NAMESPACE,
      WORKSPACE_NAME, WorkspaceAccessLevel.READER)).thenReturn(workspace);

    ParticipantCohortAnnotationListResponse response =
      cohortReviewController.getParticipantCohortAnnotations(WORKSPACE_NAMESPACE,
      WORKSPACE_NAME,
      cohort.getCohortId(),
      cdrVersion.getCdrVersionId(),
      participantCohortStatus1.getParticipantKey().getParticipantId()).getBody();

    assertThat(response.getItems().size()).isEqualTo(1);
    assertThat(response.getItems().get(0).getCohortReviewId()).isEqualTo(cohortReview.getCohortReviewId());
    assertThat(response.getItems().get(0).getParticipantId()).isEqualTo(participantCohortStatus1.getParticipantKey().getParticipantId());
  }

  @Test
  public void getParticipantCohortStatus() throws Exception {
    when(workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(WORKSPACE_NAMESPACE,
      WORKSPACE_NAME, WorkspaceAccessLevel.READER)).thenReturn(workspace);

    org.pmiops.workbench.model.ParticipantCohortStatus response =
    cohortReviewController.getParticipantCohortStatus(WORKSPACE_NAMESPACE,
      WORKSPACE_NAME,
      cohort.getCohortId(),
      cdrVersion.getCdrVersionId(),
      participantCohortStatus1.getParticipantKey().getParticipantId()).getBody();

    assertThat(response.getParticipantId()).isEqualTo(participantCohortStatus1.getParticipantKey().getParticipantId());
    assertThat(response.getStatus()).isEqualTo(StorageEnums.cohortStatusFromStorage(participantCohortStatus1.getStatus()));
    assertThat(response.getEthnicityConceptId()).isEqualTo(participantCohortStatus1.getEthnicityConceptId());
    assertThat(response.getRaceConceptId()).isEqualTo(participantCohortStatus1.getRaceConceptId());
    assertThat(response.getGenderConceptId()).isEqualTo(participantCohortStatus1.getGenderConceptId());
  }

  @Test
  public void getParticipantCohortStatuses() throws Exception {
    int page = 0;
    int pageSize = 25;
    org.pmiops.workbench.model.CohortReview expectedReview1 =
      createCohortReview(cohortReview,
        Arrays.asList(participantCohortStatus1, participantCohortStatus2),
        page,
        pageSize,
        SortOrder.DESC,
        ParticipantCohortStatusColumns.STATUS);
    org.pmiops.workbench.model.CohortReview expectedReview2 =
      createCohortReview(cohortReview,
        Arrays.asList(participantCohortStatus2, participantCohortStatus1),
        page,
        pageSize,
        SortOrder.DESC,
        ParticipantCohortStatusColumns.PARTICIPANTID);
    org.pmiops.workbench.model.CohortReview expectedReview3 =
      createCohortReview(cohortReview,
        Arrays.asList(participantCohortStatus1, participantCohortStatus2),
        page,
        pageSize,
        SortOrder.ASC,
        ParticipantCohortStatusColumns.STATUS);
    org.pmiops.workbench.model.CohortReview expectedReview4 =
      createCohortReview(cohortReview,
        Arrays.asList(participantCohortStatus1, participantCohortStatus2),
        page,
        pageSize,
        SortOrder.ASC,
        ParticipantCohortStatusColumns.PARTICIPANTID);

    when(workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(WORKSPACE_NAMESPACE,
        WORKSPACE_NAME, WorkspaceAccessLevel.READER)).thenReturn(workspace);

    assertParticipantCohortStatuses(expectedReview1, page, pageSize, SortOrder.DESC, ParticipantCohortStatusColumns.STATUS);
    verify(userRecentResourceService).updateCohortEntry(anyLong(), anyLong(), anyLong(), any(Timestamp.class));
    assertParticipantCohortStatuses(expectedReview2, page, pageSize, SortOrder.DESC, ParticipantCohortStatusColumns.PARTICIPANTID);
    assertParticipantCohortStatuses(expectedReview3, null, null, null, ParticipantCohortStatusColumns.STATUS);
    assertParticipantCohortStatuses(expectedReview4, null, null, SortOrder.ASC, null);
    assertParticipantCohortStatuses(expectedReview4, null, pageSize, null, null);
    assertParticipantCohortStatuses(expectedReview4, page, null, null, null);
    assertParticipantCohortStatuses(expectedReview4, null, null, null, null);
  }

  @Test
  public void updateParticipantCohortAnnotation() throws Exception {
    when(workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(WORKSPACE_NAMESPACE,
      WORKSPACE_NAME, WorkspaceAccessLevel.WRITER)).thenReturn(workspace);

    ParticipantCohortAnnotation participantCohortAnnotation =
      cohortReviewController.updateParticipantCohortAnnotation(WORKSPACE_NAMESPACE,
      WORKSPACE_NAME,
      cohort.getCohortId(),
      cdrVersion.getCdrVersionId(),
      participantCohortStatus1.getParticipantKey().getParticipantId(),
      participantAnnotation.getAnnotationId(),
      new ModifyParticipantCohortAnnotationRequest().annotationValueString("test1")).getBody();

    assertThat(participantCohortAnnotation.getAnnotationValueString()).isEqualTo("test1");
  }

  @Test
  public void updateParticipantCohortAnnotationNoAnnotationForIdException() throws Exception {
    long badAnnotationId = 99;
    when(workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(WORKSPACE_NAMESPACE,
      WORKSPACE_NAME, WorkspaceAccessLevel.WRITER)).thenReturn(workspace);

    try {
      cohortReviewController.updateParticipantCohortAnnotation(WORKSPACE_NAMESPACE,
        WORKSPACE_NAME,
        cohort.getCohortId(),
        cdrVersion.getCdrVersionId(),
        participantCohortStatus1.getParticipantKey().getParticipantId(),
        badAnnotationId,
        new ModifyParticipantCohortAnnotationRequest().annotationValueString("test1")).getBody();
    } catch (NotFoundException nfe) {
      assertEquals("Not Found: Participant Cohort Annotation does not exist for annotationId: " + badAnnotationId + ", cohortReviewId: "
        + cohortReview.getCohortReviewId() + ", participantId: " + participantCohortStatus1.getParticipantKey().getParticipantId(), nfe.getMessage());
    }
  }

  @Test
  public void updateParticipantCohortStatus() throws Exception {
    when(workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(WORKSPACE_NAMESPACE,
      WORKSPACE_NAME, WorkspaceAccessLevel.WRITER)).thenReturn(workspace);

    org.pmiops.workbench.model.ParticipantCohortStatus participantCohortStatus =
      cohortReviewController.updateParticipantCohortStatus(WORKSPACE_NAMESPACE,
      WORKSPACE_NAME,
      cohort.getCohortId(),
      cdrVersion.getCdrVersionId(),
      participantCohortStatus1.getParticipantKey().getParticipantId(),
      new ModifyCohortStatusRequest().status(CohortStatus.INCLUDED)).getBody();

    assertThat(participantCohortStatus.getStatus()).isEqualTo(CohortStatus.INCLUDED);
  }

  /**
   * Helper method to consolidate assertions for all the {@link AnnotationType}s.
   *
   * @param participantId
   * @param annotationDefinitionId
   * @param request
   */
  private void assertCreateParticipantCohortAnnotation(Long participantId, Long annotationDefinitionId, ParticipantCohortAnnotation request) {
    when(workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(WORKSPACE_NAMESPACE,
      WORKSPACE_NAME, WorkspaceAccessLevel.WRITER)).thenReturn(workspace);

    ParticipantCohortAnnotation response =
      cohortReviewController.createParticipantCohortAnnotation(WORKSPACE_NAMESPACE,
        WORKSPACE_NAME,
        cohort.getCohortId(),
        cdrVersion.getCdrVersionId(),
        participantId,
        request).getBody();

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
   * Helper method to consolidate assertions for {@link BadRequestException}s for all {@link AnnotationType}s.
   *
   * @param participantId
   * @param cohortAnnotationDefId
   * @param type
   */
  private void assertBadRequestExceptionForAnnotationType(Long participantId, Long cohortAnnotationDefId, String type) {
    when(workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(WORKSPACE_NAMESPACE,
      WORKSPACE_NAME, WorkspaceAccessLevel.WRITER)).thenReturn(workspace);

    try {
      cohortReviewController.createParticipantCohortAnnotation(WORKSPACE_NAMESPACE,
        WORKSPACE_NAME,
        cohort.getCohortId(),
        cdrVersion.getCdrVersionId(),
        participantId,
        new ParticipantCohortAnnotation()
          .cohortReviewId(cohortReview.getCohortReviewId())
          .participantId(participantId)
          .cohortAnnotationDefinitionId(cohortAnnotationDefId)).getBody();
      fail("Should have thrown a BadRequestException!");
    } catch (BadRequestException bre) {
      //Success
      assertThat(bre.getMessage())
        .isEqualTo("Bad Request: Please provide a valid " + type + " value for annotation defintion id: "
          + cohortAnnotationDefId);
    }
  }

  /**
   * Helper method to assert results for
   * {@link CohortReviewController#getParticipantCohortStatuses(String, String, Long, Long, PageFilterRequest)}.
   *
   * @param expectedReview
   * @param page
   * @param pageSize
   * @param sortOrder
   * @param sortColumn
   */
  private void assertParticipantCohortStatuses(org.pmiops.workbench.model.CohortReview expectedReview,
                                               Integer page,
                                               Integer pageSize,
                                               SortOrder sortOrder,
                                               ParticipantCohortStatusColumns sortColumn) {
    org.pmiops.workbench.model.CohortReview actualReview =
      cohortReviewController.getParticipantCohortStatuses(WORKSPACE_NAMESPACE,
      WORKSPACE_NAME,
        cohort.getCohortId(),
        cdrVersion.getCdrVersionId(),
        new ParticipantCohortStatuses()
          .sortColumn(sortColumn)
          .page(page)
          .pageSize(pageSize)
          .sortOrder(sortOrder)).getBody();

    assertThat(actualReview).isEqualTo(expectedReview);
  }

  private void stubBigQueryCohortCalls() {
    TableResult queryResult = mock(TableResult.class);
    Iterable testIterable = new Iterable() {
      @Override
      public Iterator iterator() {
        List<FieldValue> list = new ArrayList<>();
        list.add(null);
        return list.iterator();
      }
    };
    Map<String, Integer> rm = ImmutableMap.<String, Integer>builder()
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

  private org.pmiops.workbench.model.CohortReview createCohortReview(CohortReview actualReview,
                                                                     List<ParticipantCohortStatus> participantCohortStatusList,
                                                                     Integer page,
                                                                     Integer pageSize,
                                                                     SortOrder sortOrder,
                                                                     ParticipantCohortStatusColumns sortColumn) {
    List<org.pmiops.workbench.model.ParticipantCohortStatus> newParticipantCohortStatusList = new ArrayList<>();
    for (ParticipantCohortStatus participantCohortStatus : participantCohortStatusList) {
      newParticipantCohortStatusList.add(new org.pmiops.workbench.model.ParticipantCohortStatus()
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
