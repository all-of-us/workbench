package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.pmiops.workbench.utils.TestMockFactory.createDefaultCdrVersion;

import com.google.cloud.PageImpl;
import com.google.cloud.bigquery.*;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Table;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.access.AccessModuleService;
import org.pmiops.workbench.access.AccessTierService;
import org.pmiops.workbench.access.AccessTierServiceImpl;
import org.pmiops.workbench.actionaudit.auditors.UserServiceAuditor;
import org.pmiops.workbench.actionaudit.auditors.WorkspaceAuditor;
import org.pmiops.workbench.billing.FreeTierBillingService;
import org.pmiops.workbench.cdr.CdrVersionService;
import org.pmiops.workbench.cdr.dao.CBCriteriaDao;
import org.pmiops.workbench.cdr.model.DbCriteria;
import org.pmiops.workbench.cloudtasks.TaskQueueService;
import org.pmiops.workbench.cohortbuilder.CohortBuilderServiceImpl;
import org.pmiops.workbench.cohortbuilder.CohortQueryBuilder;
import org.pmiops.workbench.cohortbuilder.chart.ChartQueryBuilder;
import org.pmiops.workbench.cohortbuilder.chart.ChartServiceImpl;
import org.pmiops.workbench.cohortbuilder.mapper.CohortBuilderMapperImpl;
import org.pmiops.workbench.cohortreview.CohortReviewServiceImpl;
import org.pmiops.workbench.cohortreview.ReviewQueryBuilder;
import org.pmiops.workbench.cohortreview.mapper.CohortReviewMapper;
import org.pmiops.workbench.cohortreview.mapper.CohortReviewMapperImpl;
import org.pmiops.workbench.cohortreview.mapper.ParticipantCohortAnnotationMapper;
import org.pmiops.workbench.cohortreview.mapper.ParticipantCohortAnnotationMapperImpl;
import org.pmiops.workbench.cohortreview.mapper.ParticipantCohortStatusMapper;
import org.pmiops.workbench.cohortreview.mapper.ParticipantCohortStatusMapperImpl;
import org.pmiops.workbench.compliance.ComplianceService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.dataset.DataSetService;
import org.pmiops.workbench.db.dao.AccessTierDao;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.CohortAnnotationDefinitionDao;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.CohortReviewDao;
import org.pmiops.workbench.db.dao.ParticipantCohortAnnotationDao;
import org.pmiops.workbench.db.dao.ParticipantCohortStatusDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserService;
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
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceACL;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceAccessEntry;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceDetails;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceResponse;
import org.pmiops.workbench.google.CloudBillingClient;
import org.pmiops.workbench.google.CloudStorageClient;
import org.pmiops.workbench.google.DirectoryService;
import org.pmiops.workbench.iam.IamService;
import org.pmiops.workbench.leonardo.LeonardoApiClient;
import org.pmiops.workbench.mail.MailService;
import org.pmiops.workbench.model.AnnotationType;
import org.pmiops.workbench.model.CohortReview;
import org.pmiops.workbench.model.CohortStatus;
import org.pmiops.workbench.model.CreateReviewRequest;
import org.pmiops.workbench.model.CriteriaType;
import org.pmiops.workbench.model.DemoChartInfo;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.FilterColumns;
import org.pmiops.workbench.model.ModifyCohortStatusRequest;
import org.pmiops.workbench.model.ModifyParticipantCohortAnnotationRequest;
import org.pmiops.workbench.model.PageFilterRequest;
import org.pmiops.workbench.model.ParticipantChartData;
import org.pmiops.workbench.model.ParticipantCohortAnnotation;
import org.pmiops.workbench.model.ParticipantCohortAnnotationListResponse;
import org.pmiops.workbench.model.ParticipantCohortStatus;
import org.pmiops.workbench.model.ParticipantDataCountResponse;
import org.pmiops.workbench.model.ParticipantDataListResponse;
import org.pmiops.workbench.model.ResearchPurpose;
import org.pmiops.workbench.model.ReviewStatus;
import org.pmiops.workbench.model.SortOrder;
import org.pmiops.workbench.model.Vocabulary;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.monitoring.LogsBasedMetricServiceFakeImpl;
import org.pmiops.workbench.notebooks.NotebooksServiceImpl;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.test.FakeLongRandom;
import org.pmiops.workbench.testconfig.UserServiceTestConfiguration;
import org.pmiops.workbench.utils.TestMockFactory;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.utils.mappers.FirecloudMapperImpl;
import org.pmiops.workbench.utils.mappers.UserMapperImpl;
import org.pmiops.workbench.utils.mappers.WorkspaceMapperImpl;
import org.pmiops.workbench.workspaces.WorkspaceAuthService;
import org.pmiops.workbench.workspaces.WorkspaceOperationMapper;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.pmiops.workbench.workspaces.WorkspaceServiceImpl;
import org.pmiops.workbench.workspaces.resources.UserRecentResourceService;
import org.pmiops.workbench.workspaces.resources.WorkspaceResourcesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class CohortReviewControllerTest {

  private static final String WORKSPACE_NAMESPACE = "namespace";
  private static final String WORKSPACE_NAME = "name";
  private static final String WORKSPACE2_NAMESPACE = "namespace2";
  private static final String WORKSPACE2_NAME = "name2";
  private static final String USER_EMAIL = "bob@gmail.com";
  private static final String CDR_VERSION_NAME = "cdrVersion";
  private static final Instant NOW = Instant.now();
  private static final FakeClock CLOCK = new FakeClock(NOW, ZoneId.systemDefault());
  private static DbUser currentUser;

  DbCdrVersion cdrVersion;
  DbCohortReview cohortReview;
  DbCohortReview cohortReview2;
  DbCohort cohort;
  DbCohort cohortWithoutReview;
  DbParticipantCohortStatus participantCohortStatus1;
  DbParticipantCohortStatus participantCohortStatus2;
  Workspace workspace;
  Workspace workspace2;

  DbCohortAnnotationDefinition stringAnnotationDefinition;
  DbCohortAnnotationDefinition enumAnnotationDefinition;
  DbCohortAnnotationDefinition dateAnnotationDefinition;
  DbCohortAnnotationDefinition booleanAnnotationDefinition;
  DbCohortAnnotationDefinition integerAnnotationDefinition;
  DbParticipantCohortAnnotation participantAnnotation;
  DbParticipantCohortAnnotation participantAnnotationDate;

  @Autowired CdrVersionDao cdrVersionDao;
  @Autowired AccessTierDao accessTierDao;
  @Autowired CBCriteriaDao cbCriteriaDao;
  @Autowired WorkspaceDao workspaceDao;
  @Autowired UserDao userDao;
  @Autowired CohortDao cohortDao;
  @Autowired CohortReviewDao cohortReviewDao;
  @Autowired ParticipantCohortStatusDao participantCohortStatusDao;
  @Autowired CohortAnnotationDefinitionDao cohortAnnotationDefinitionDao;
  @Autowired ParticipantCohortAnnotationDao participantCohortAnnotationDao;
  @Autowired ParticipantCohortAnnotationMapper participantCohortAnnotationMapper;
  @Autowired ParticipantCohortStatusMapper participantCohortStatusMapper;
  @Autowired CohortReviewMapper cohortReviewMapper;
  @Autowired FireCloudService fireCloudService;
  @Autowired CloudStorageClient cloudStorageClient;
  @Autowired CloudBillingClient cloudBillingClient;
  @Autowired ComplianceService complianceService;
  @Autowired DataSetService dataSetService;
  @Autowired BigQueryService bigQueryService;
  @Autowired UserService userService;
  @Autowired UserRecentResourceService userRecentResourceService;
  @Autowired WorkspaceService workspaceService;
  @Autowired WorkspaceAuthService workspaceAuthService;

  @Autowired WorkspacesController workspacesController;
  @Autowired CohortReviewController cohortReviewController;

  private enum TestConcepts {
    RACE_ASIAN("Asian", 8515, CriteriaType.RACE),
    RACE_WHITE("White", 8527, CriteriaType.RACE),
    GENDER_MAN("Man", 45880669, CriteriaType.GENDER),
    GENDER_WOMAN("Woman", 45878463, CriteriaType.GENDER),
    ETHNICITY_NOT_HISPANIC("Not Hispanic or Latino", 38003564, CriteriaType.ETHNICITY),
    RACE_PREFER_NOT_TO_ANSWER("I Prefer not to answer", 1177221, CriteriaType.RACE),
    ETHNICITY_PREFER_NOT_TO_ANSWER("I Prefer not to answer", 1177221, CriteriaType.ETHNICITY),
    SEX_AT_BIRTH_MALE("Male", 45880669, CriteriaType.SEX),
    SEX_AT_BIRTH_FEMALE("Female", 45878463, CriteriaType.SEX);

    private final String name;
    private final long conceptId;
    private final CriteriaType criteriaType;

    TestConcepts(String name, long conceptId, CriteriaType criteriaType) {
      this.name = name;
      this.conceptId = conceptId;
      this.criteriaType = criteriaType;
    }

    public String getName() {
      return name;
    }

    public long getConceptId() {
      return conceptId;
    }

    public CriteriaType getType() {
      return criteriaType;
    }

    public static Table<Long, CriteriaType, String> asMap() {
      Table<Long, CriteriaType, String> demoTable = HashBasedTable.create();
      for (TestConcepts testConcepts : TestConcepts.values()) {
        demoTable.put(testConcepts.getConceptId(), testConcepts.getType(), testConcepts.getName());
      }
      return demoTable;
    }
  }

  @TestConfiguration
  @Import({
    FakeClockConfiguration.class,
    CdrVersionService.class,
    ChartServiceImpl.class,
    ChartQueryBuilder.class,
    CohortBuilderMapperImpl.class,
    CohortBuilderServiceImpl.class,
    CohortReviewController.class,
    CohortReviewMapperImpl.class,
    CohortReviewServiceImpl.class,
    CommonMappers.class,
    CohortQueryBuilder.class,
    ReviewQueryBuilder.class,
    ParticipantCohortAnnotationMapperImpl.class,
    ParticipantCohortStatusMapperImpl.class,
    FirecloudMapperImpl.class,
    LogsBasedMetricServiceFakeImpl.class,
    NotebooksServiceImpl.class,
    UserMapperImpl.class,
    UserServiceTestConfiguration.class,
    WorkspaceMapperImpl.class,
    WorkspaceServiceImpl.class,
    WorkspaceAuthService.class,
    WorkspacesController.class,
    AccessTierServiceImpl.class,
  })
  @MockBean({
    AccessModuleService.class,
    AccessTierService.class,
    BigQueryService.class,
    CdrVersionService.class,
    CloudBillingClient.class,
    CloudStorageClient.class,
    ComplianceService.class,
    DataSetService.class,
    DirectoryService.class,
    FireCloudService.class,
    FreeTierBillingService.class,
    IamService.class,
    LeonardoApiClient.class,
    MailService.class,
    TaskQueueService.class,
    UserRecentResourceService.class,
    UserServiceAuditor.class,
    WorkspaceAuditor.class,
    WorkspaceOperationMapper.class,
    WorkspaceResourcesService.class,
    WorkspaceService.class,
  })
  static class Configuration {
    @Bean
    Clock clock() {
      return CLOCK;
    }

    @Bean
    Random random() {
      return new FakeLongRandom(123);
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
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

  @BeforeEach
  public void setUp() {
    TestMockFactory.stubCreateBillingProject(fireCloudService);
    TestMockFactory.stubCreateFcWorkspace(fireCloudService);
    TestMockFactory.stubPollCloudBillingLinked(cloudBillingClient, "billing-account");

    DbUser user = new DbUser();
    user.setUsername(USER_EMAIL);
    user.setUserId(123L);
    user.setDisabled(false);
    currentUser = userDao.save(user);

    cdrVersion = createDefaultCdrVersion();
    accessTierDao.save(cdrVersion.getAccessTier());
    cdrVersion.setName(CDR_VERSION_NAME);
    cdrVersionDao.save(cdrVersion);

    workspace =
        createTestWorkspace(WORKSPACE_NAMESPACE, WORKSPACE_NAME, cdrVersion.getCdrVersionId());
    stubWorkspaceAccessLevel(workspace, WorkspaceAccessLevel.OWNER);

    workspace2 =
        createTestWorkspace(WORKSPACE2_NAMESPACE, WORKSPACE2_NAME, cdrVersion.getCdrVersionId());
    stubWorkspaceAccessLevel(workspace2, WorkspaceAccessLevel.OWNER);

    cbCriteriaDao.save(
        DbCriteria.builder()
            .addDomainId(Domain.PERSON.toString())
            .addType(CriteriaType.RACE.toString())
            .addParentId(1L)
            .addConceptId(String.valueOf(TestConcepts.RACE_ASIAN.conceptId))
            .addName(TestConcepts.RACE_ASIAN.name)
            .build());
    cbCriteriaDao.save(
        DbCriteria.builder()
            .addDomainId(Domain.PERSON.toString())
            .addType(CriteriaType.GENDER.toString())
            .addParentId(1L)
            .addConceptId(String.valueOf(TestConcepts.GENDER_WOMAN.conceptId))
            .addName(TestConcepts.GENDER_WOMAN.name)
            .build());
    cbCriteriaDao.save(
        DbCriteria.builder()
            .addDomainId(Domain.PERSON.toString())
            .addType(CriteriaType.GENDER.toString())
            .addParentId(1L)
            .addConceptId(String.valueOf(TestConcepts.GENDER_MAN.conceptId))
            .addName(TestConcepts.GENDER_MAN.name)
            .build());
    cbCriteriaDao.save(
        DbCriteria.builder()
            .addDomainId(Domain.PERSON.toString())
            .addType(CriteriaType.ETHNICITY.toString())
            .addParentId(1L)
            .addConceptId(String.valueOf(TestConcepts.ETHNICITY_NOT_HISPANIC.conceptId))
            .addName(TestConcepts.ETHNICITY_NOT_HISPANIC.name)
            .build());
    cbCriteriaDao.save(
        DbCriteria.builder()
            .addDomainId(Domain.PERSON.toString())
            .addType(CriteriaType.RACE.toString())
            .addParentId(1L)
            .addConceptId(String.valueOf(TestConcepts.RACE_WHITE.conceptId))
            .addName(TestConcepts.RACE_WHITE.name)
            .build());
    cbCriteriaDao.save(
        DbCriteria.builder()
            .addDomainId(Domain.PERSON.toString())
            .addType(CriteriaType.RACE.toString())
            .addParentId(1L)
            .addConceptId(String.valueOf(TestConcepts.RACE_PREFER_NOT_TO_ANSWER.conceptId))
            .addName(TestConcepts.RACE_PREFER_NOT_TO_ANSWER.name)
            .build());
    cbCriteriaDao.save(
        DbCriteria.builder()
            .addDomainId(Domain.PERSON.toString())
            .addType(CriteriaType.RACE.toString())
            .addParentId(1L)
            .addConceptId(String.valueOf(TestConcepts.ETHNICITY_PREFER_NOT_TO_ANSWER.conceptId))
            .addName(TestConcepts.ETHNICITY_PREFER_NOT_TO_ANSWER.name)
            .build());
    cbCriteriaDao.save(
        DbCriteria.builder()
            .addDomainId(Domain.PERSON.toString())
            .addType(CriteriaType.SEX.toString())
            .addParentId(1L)
            .addConceptId(String.valueOf(TestConcepts.SEX_AT_BIRTH_MALE.conceptId))
            .addName(TestConcepts.SEX_AT_BIRTH_MALE.name)
            .build());
    cbCriteriaDao.save(
        DbCriteria.builder()
            .addDomainId(Domain.PERSON.toString())
            .addType(CriteriaType.SEX.toString())
            .addParentId(1L)
            .addConceptId(String.valueOf(TestConcepts.SEX_AT_BIRTH_FEMALE.conceptId))
            .addName(TestConcepts.SEX_AT_BIRTH_FEMALE.name)
            .build());

    cohort = new DbCohort();
    cohort.setWorkspaceId(1L);

    String criteria =
        "{\"includes\":[{\"id\":\"includes_kl4uky6kh\",\"items\":[{\"id\":\"items_58myrn9iz\",\"type\":\"CONDITION\",\"searchParameters\":[{"
            + "\"parameterId\":\"param1567486C34\",\"name\":\"Malignant neoplasm of bronchus and lung\",\"domain\":\"CONDITION\",\"type\": "
            + "\"ICD10CM\",\"group\":false,\"attributes\":[],\"ancestorData\":false,\"standard\":false,\"conceptId\":1567486,\"value\":\"C34\"}],"
            + "\"modifiers\":[]}],\"temporal\":false}],\"excludes\":[]}";
    cohort.setCriteria(criteria);
    cohortDao.save(cohort);

    DbCohort cohort2 = new DbCohort();
    cohort2.setWorkspaceId(1L);
    cohort2.setCriteria(criteria);
    cohortDao.save(cohort2);

    cohortWithoutReview = new DbCohort();
    cohortWithoutReview.setWorkspaceId(1L);
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

    cohortReview2 =
        cohortReviewDao.save(
            new DbCohortReview()
                .cohortId(cohort2.getCohortId())
                .cdrVersionId(cdrVersion.getCdrVersionId())
                .reviewSize(0)
                .creationTime(today));

    DbParticipantCohortStatusKey key1 =
        new DbParticipantCohortStatusKey()
            .cohortReviewId(cohortReview.getCohortReviewId())
            .participantId(10L);

    DbParticipantCohortStatusKey key2 =
        new DbParticipantCohortStatusKey()
            .cohortReviewId(cohortReview.getCohortReviewId())
            .participantId(20L);

    participantCohortStatus1 =
        participantCohortStatusDao.save(
            new DbParticipantCohortStatus()
                .status(DbStorageEnums.cohortStatusToStorage(CohortStatus.NOT_REVIEWED))
                .participantKey(key1)
                .genderConceptId(TestConcepts.GENDER_MAN.getConceptId())
                .raceConceptId(TestConcepts.RACE_ASIAN.getConceptId())
                .ethnicityConceptId(TestConcepts.ETHNICITY_NOT_HISPANIC.getConceptId())
                .sexAtBirthConceptId(TestConcepts.SEX_AT_BIRTH_MALE.getConceptId())
                .birthDate(new java.sql.Date(today.getTime()))
                .deceased(false));

    participantCohortStatus2 =
        participantCohortStatusDao.save(
            new DbParticipantCohortStatus()
                .status(DbStorageEnums.cohortStatusToStorage(CohortStatus.NEEDS_FURTHER_REVIEW))
                .participantKey(key2)
                .genderConceptId(TestConcepts.GENDER_WOMAN.getConceptId())
                .raceConceptId(TestConcepts.RACE_WHITE.getConceptId())
                .ethnicityConceptId(TestConcepts.ETHNICITY_NOT_HISPANIC.getConceptId())
                .sexAtBirthConceptId(TestConcepts.SEX_AT_BIRTH_FEMALE.getConceptId())
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

    participantAnnotationDate =
        participantCohortAnnotationDao.save(
            new DbParticipantCohortAnnotation()
                .cohortReviewId(cohortReview.getCohortReviewId())
                .participantId(participantCohortStatus1.getParticipantKey().getParticipantId())
                .annotationValueDateString("2022-02-21")
                .cohortAnnotationDefinitionId(
                    dateAnnotationDefinition.getCohortAnnotationDefinitionId()));
  }

  ////////// createCohortReview  //////////
  @Test
  public void createCohortReviewNoName() {
    // use existing cohort
    Throwable exception =
        assertThrows(
            BadRequestException.class,
            () ->
                cohortReviewController.createCohortReview(
                    workspace.getNamespace(),
                    workspace.getId(),
                    cohort.getCohortId(),
                    new CreateReviewRequest().size(1)));

    assertThat(exception)
        .hasMessageThat()
        .isEqualTo("Bad Request: Cohort Review name cannot be null");
  }

  @Test
  public void createCohortReviewLessThanMinSize() {
    // use existing cohort
    Throwable exception =
        assertThrows(
            BadRequestException.class,
            () ->
                cohortReviewController.createCohortReview(
                    workspace.getNamespace(),
                    workspace.getId(),
                    cohort.getCohortId(),
                    new CreateReviewRequest().size(0).name("review1")));

    assertThat(exception)
        .hasMessageThat()
        .isEqualTo("Bad Request: Cohort Review size must be between 1 and 10000");
  }

  @Test
  public void createCohortReviewMoreThanMaxSize() {
    // use existing cohort
    Throwable exception =
        assertThrows(
            BadRequestException.class,
            () ->
                cohortReviewController.createCohortReview(
                    workspace.getNamespace(),
                    workspace.getId(),
                    cohort.getCohortId(),
                    new CreateReviewRequest().size(10001).name("review1")));

    assertThat(exception)
        .hasMessageThat()
        .isEqualTo("Bad Request: Cohort Review size must be between 1 and 10000");
  }

  @Test
  public void createCohortReviewAlreadyExists() {
    stubWorkspaceAccessLevel(workspace, WorkspaceAccessLevel.OWNER);
    // use existing cohort
    stubBigQueryCreateCohortReview();
    // create a new review
    cohortReviewController.createCohortReview(
        workspace.getNamespace(),
        workspace.getId(),
        cohort.getCohortId(),
        new CreateReviewRequest().size(1).name("review1"));
    List<CohortReview> cohortReviewList =
        cohortReviewController
            .getCohortReviewsByCohortId(
                workspace.getNamespace(), workspace.getId(), cohort.getCohortId())
            .getBody()
            .getItems();

    assertThat(cohortReviewList).hasSize(2);
  }

  @Test
  public void createCohortReviewNoCohortException() {
    Long cohortId = cohort.getCohortId() + 99L;
    stubWorkspaceAccessLevel(workspace, WorkspaceAccessLevel.WRITER);

    Throwable exception =
        assertThrows(
            NotFoundException.class,
            () ->
                cohortReviewController.createCohortReview(
                    workspace.getNamespace(),
                    workspace.getId(),
                    cohortId,
                    new CreateReviewRequest().size(1).name("review1")));

    assertNotFoundExceptionNoCohort(cohortId, exception);
  }

  @ParameterizedTest(name = "createCohortReviewAllowedAccessLevel WorkspaceAccessLevel={0}")
  @EnumSource(
      value = WorkspaceAccessLevel.class,
      names = {"OWNER", "WRITER"})
  public void createCohortReviewAllowedAccessLevel(WorkspaceAccessLevel workspaceAccessLevel) {
    stubBigQueryCreateCohortReview();
    // change access, call and check
    stubWorkspaceAccessLevel(workspace, workspaceAccessLevel);
    String reviewName = "review1";
    CohortReview cohortReview =
        cohortReviewController
            .createCohortReview(
                workspace.getNamespace(),
                workspace.getId(),
                cohortWithoutReview.getCohortId(),
                new CreateReviewRequest().size(1).name(reviewName))
            .getBody();

    assertNewlyCreatedCohortReview(Objects.requireNonNull(cohortReview), reviewName);
  }

  @ParameterizedTest(name = "createCohortReviewAllowedAccessLevel WorkspaceAccessLevel={0}")
  @EnumSource(
      value = WorkspaceAccessLevel.class,
      names = {"READER", "NO_ACCESS"})
  public void createCohortReviewAllowedForbiddenAccessLevel(
      WorkspaceAccessLevel workspaceAccessLevel) {
    // change access, call and check
    stubWorkspaceAccessLevel(workspace, workspaceAccessLevel);

    Throwable exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                cohortReviewController.createCohortReview(
                    workspace.getNamespace(),
                    workspace.getId(),
                    cohortWithoutReview.getCohortId(),
                    new CreateReviewRequest().size(1).name("review1")));

    assertForbiddenException(exception);
  }

  ////////// updateCohortReview  //////////
  @Test
  public void updateCohortReviewNoEtag() {
    stubWorkspaceAccessLevel(workspace2, WorkspaceAccessLevel.WRITER);

    CohortReview requestCohortReview =
        new CohortReview()
            .cohortReviewId(cohortReview.getCohortReviewId())
            .cohortId(cohortReview.getCohortId());

    Throwable exception =
        assertThrows(
            ConflictException.class,
            () ->
                cohortReviewController.updateCohortReview(
                    workspace.getNamespace(),
                    workspace.getId(),
                    requestCohortReview.getCohortReviewId(),
                    requestCohortReview));

    assertThat(exception).hasMessageThat().isEqualTo("missing required update field 'etag'");
  }

  @Test
  public void updateCohortReviewEtagDifferent() {
    stubWorkspaceAccessLevel(workspace2, WorkspaceAccessLevel.WRITER);

    CohortReview requestCohortReview =
        new CohortReview()
            .cohortReviewId(cohortReview.getCohortReviewId())
            .cohortId(cohortReview.getCohortId())
            .etag(Etags.fromVersion(cohortReview.getVersion() + 10));

    Throwable exception =
        assertThrows(
            ConflictException.class,
            () ->
                cohortReviewController.updateCohortReview(
                    workspace.getNamespace(),
                    workspace.getId(),
                    requestCohortReview.getCohortReviewId(),
                    requestCohortReview));

    assertThat(exception)
        .hasMessageThat()
        .isEqualTo("Attempted to modify outdated cohort review version");
  }

  @Test
  public void updateCohortReviewWrongWorkspace() {
    stubWorkspaceAccessLevel(workspace2, WorkspaceAccessLevel.WRITER);

    CohortReview requestCohortReview =
        new CohortReview()
            .cohortReviewId(cohortReview.getCohortReviewId())
            .cohortId(cohortReview.getCohortId())
            .etag(Etags.fromVersion(cohortReview.getVersion()));

    Throwable exception =
        assertThrows(
            NotFoundException.class,
            () ->
                cohortReviewController.updateCohortReview(
                    workspace2.getNamespace(),
                    workspace2.getId(),
                    requestCohortReview.getCohortReviewId(),
                    requestCohortReview));

    assertNotFoundExceptionNoCohortReviewAndCohort(
        requestCohortReview.getCohortReviewId(), exception);
  }

  @ParameterizedTest(name = "updateCohortReviewAllowedAccessLevel WorkspaceAccessLevel={0}")
  @EnumSource(
      value = WorkspaceAccessLevel.class,
      names = {"OWNER", "WRITER"})
  public void updateCohortReviewAllowedAccessLevel(WorkspaceAccessLevel workspaceAccessLevel) {
    // change access, call and check
    stubWorkspaceAccessLevel(workspace, workspaceAccessLevel);

    CohortReview requestCohortReview =
        new CohortReview()
            .cohortReviewId(cohortReview.getCohortReviewId())
            .cohortId(cohortReview.getCohortId())
            .etag(Etags.fromVersion(cohortReview.getVersion()));
    requestCohortReview.setCohortName(cohortReview.getCohortName() + "_Updated");
    requestCohortReview.setDescription(cohortReview.getDescription() + "_Updated");

    CohortReview updated =
        cohortReviewController
            .updateCohortReview(
                workspace.getNamespace(),
                workspace.getId(),
                requestCohortReview.getCohortReviewId(),
                requestCohortReview)
            .getBody();

    assertThat(Objects.requireNonNull(updated).getCohortName())
        .isEqualTo(requestCohortReview.getCohortName());
    assertThat(updated.getDescription()).isEqualTo(requestCohortReview.getDescription());
    assertThat(updated.getLastModifiedTime()).isEqualTo(CLOCK.instant().toEpochMilli());
  }

  @ParameterizedTest(name = "updateCohortReviewForbiddenAccessLevel WorkspaceAccessLevel={0}")
  @EnumSource(
      value = WorkspaceAccessLevel.class,
      names = {"READER", "NO_ACCESS"})
  public void updateCohortReviewForbiddenAccessLevel(WorkspaceAccessLevel workspaceAccessLevel) {
    // change access, call and check
    stubWorkspaceAccessLevel(workspace, workspaceAccessLevel);

    CohortReview requestCohortReview =
        new CohortReview()
            .cohortReviewId(cohortReview.getCohortReviewId())
            .cohortId(cohortReview.getCohortId())
            .etag(Etags.fromVersion(cohortReview.getVersion()));
    requestCohortReview.setCohortName(cohortReview.getCohortName() + "_Updated");
    requestCohortReview.setDescription(cohortReview.getDescription() + "_Updated");

    Throwable exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                cohortReviewController.updateCohortReview(
                    workspace.getNamespace(),
                    workspace.getId(),
                    requestCohortReview.getCohortReviewId(),
                    requestCohortReview));

    assertForbiddenException(exception);
  }

  ////////// deleteCohortReview  //////////
  @Test
  public void deleteCohortReviewWrongWorkspace() {
    stubWorkspaceAccessLevel(workspace2, WorkspaceAccessLevel.WRITER);

    CohortReview requestCohortReview =
        new CohortReview()
            .cohortReviewId(cohortReview.getCohortReviewId())
            .etag(Etags.fromVersion(cohortReview.getVersion()));

    Throwable exception =
        assertThrows(
            NotFoundException.class,
            () ->
                cohortReviewController.deleteCohortReview(
                    workspace2.getNamespace(),
                    workspace2.getId(),
                    requestCohortReview.getCohortReviewId()));

    assertNotFoundExceptionNoCohortReviewAndCohort(
        requestCohortReview.getCohortReviewId(), exception);
  }

  @ParameterizedTest(name = "deleteCohortReviewAllowedAccessLevel WorkspaceAccessLevel={0}")
  @EnumSource(
      value = WorkspaceAccessLevel.class,
      names = {"OWNER", "WRITER"})
  public void deleteCohortReviewAllowedAccessLevel(WorkspaceAccessLevel workspaceAccessLevel) {
    stubWorkspaceAccessLevel(workspace, workspaceAccessLevel);

    CohortReview requestCohortReview =
        new CohortReview()
            .cohortReviewId(cohortReview.getCohortReviewId())
            .etag(Etags.fromVersion(cohortReview.getVersion()));
    ResponseEntity<EmptyResponse> response =
        cohortReviewController.deleteCohortReview(
            workspace.getNamespace(), workspace.getId(), requestCohortReview.getCohortReviewId());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @ParameterizedTest(name = "deleteCohortReviewForbiddenAccessLevel WorkspaceAccessLevel={0}")
  @EnumSource(
      value = WorkspaceAccessLevel.class,
      names = {"READER", "NO_ACCESS"})
  public void deleteCohortReviewForbiddenAccessLevel(WorkspaceAccessLevel workspaceAccessLevel) {
    stubWorkspaceAccessLevel(workspace, workspaceAccessLevel);

    CohortReview requestCohortReview =
        new CohortReview()
            .cohortReviewId(cohortReview.getCohortReviewId())
            .etag(Etags.fromVersion(cohortReview.getVersion()));

    Throwable exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                cohortReviewController.deleteCohortReview(
                    workspace.getNamespace(),
                    workspace.getId(),
                    requestCohortReview.getCohortReviewId()));

    assertForbiddenException(exception);
  }

  ////////// createParticipantCohortAnnotation  //////////
  @Test
  public void createParticipantCohortAnnotationNoAnnotationDefinitionFound() {
    Long participantId = participantCohortStatus1.getParticipantKey().getParticipantId();

    stubWorkspaceAccessLevel(workspace, WorkspaceAccessLevel.WRITER);

    Throwable exception =
        assertThrows(
            NotFoundException.class,
            () ->
                cohortReviewController.createParticipantCohortAnnotation(
                    workspace.getNamespace(),
                    workspace.getId(),
                    cohortReview.getCohortReviewId(),
                    participantId,
                    new ParticipantCohortAnnotation()
                        .cohortReviewId(cohortReview.getCohortReviewId())
                        .participantId(participantId)
                        .annotationValueString("test")
                        .cohortAnnotationDefinitionId(9999L)));

    assertThat(exception)
        .hasMessageThat()
        .isEqualTo("Not Found: No cohort annotation definition found for id: 9999");
  }

  @Test
  public void createParticipantCohortAnnotationAllTypesNoAnnotationValue() {
    Long cohortReviewId = cohortReview.getCohortReviewId();
    Long participantId = participantCohortStatus1.getParticipantKey().getParticipantId();
    // for different AnnotationType(s)
    for (AnnotationType annotationType : AnnotationType.values()) {
      ParticipantCohortAnnotation request =
          buildNoValueParticipantCohortAnnotationForType(
              cohortReviewId, participantId, annotationType);
      Throwable exception =
          assertThrows(
              ConflictException.class,
              () ->
                  cohortReviewController.createParticipantCohortAnnotation(
                      workspace.getNamespace(),
                      workspace.getId(),
                      cohortReviewId,
                      participantId,
                      request));

      assertThat(exception)
          .hasMessageThat()
          .isEqualTo(
              String.format(
                  "Conflict Exception: Please provide a valid %s value for annotation definition id: %d",
                  annotationType, request.getCohortAnnotationDefinitionId()));
    }
  }

  @Test
  public void createParticipantCohortAnnotationExists() {
    Long cohortReviewId = cohortReview.getCohortReviewId();
    Long participantId = participantCohortStatus1.getParticipantKey().getParticipantId();

    ParticipantCohortAnnotation request =
        buildValidParticipantCohortAnnotationForType(
            cohortReviewId, participantId, AnnotationType.STRING);
    Throwable exception =
        assertThrows(
            ConflictException.class,
            () ->
                cohortReviewController.createParticipantCohortAnnotation(
                    workspace.getNamespace(),
                    workspace.getId(),
                    cohortReview.getCohortReviewId(),
                    participantId,
                    request));
    assertThat(exception)
        .hasMessageThat()
        .isEqualTo(
            String.format(
                "Cohort annotation definition exists for id: %d",
                request.getCohortAnnotationDefinitionId()));
  }

  @ParameterizedTest(name = "createParticipantCohortAnnotationForAnnotationType AnnotationType={0}")
  @EnumSource(value = AnnotationType.class)
  public void createParticipantCohortAnnotationForAnnotationType(AnnotationType annotationType) {
    // for different AnnotationType(s)
    Long cohortReviewId = cohortReview.getCohortReviewId();
    Long participantId = participantCohortStatus1.getParticipantKey().getParticipantId();

    deleteExistingParticipantCohortAnnotations();

    ParticipantCohortAnnotation request =
        buildValidParticipantCohortAnnotationForType(cohortReviewId, participantId, annotationType);
    ParticipantCohortAnnotation response =
        cohortReviewController
            .createParticipantCohortAnnotation(
                workspace.getNamespace(), workspace.getId(), cohortReviewId, participantId, request)
            .getBody();

    assertCreatedParticipantCohortAnnotation(Objects.requireNonNull(response), request);
  }

  @ParameterizedTest(
      name = "createParticipantCohortAnnotationAllowedAccessLevel WorkspaceAccessLevel={0}")
  @EnumSource(
      value = WorkspaceAccessLevel.class,
      names = {"OWNER", "WRITER"})
  public void createParticipantCohortAnnotationAllowedAccessLevel(
      WorkspaceAccessLevel workspaceAccessLevel) {
    deleteExistingParticipantCohortAnnotations();
    // change access, call and check
    stubWorkspaceAccessLevel(workspace, workspaceAccessLevel);

    Long cohortReviewId = cohortReview.getCohortReviewId();
    Long participantId = participantCohortStatus1.getParticipantKey().getParticipantId();
    // use stringAnnotationType
    ParticipantCohortAnnotation request =
        buildValidParticipantCohortAnnotationForType(
            cohortReviewId, participantId, AnnotationType.STRING);
    ParticipantCohortAnnotation response =
        cohortReviewController
            .createParticipantCohortAnnotation(
                workspace.getNamespace(),
                workspace.getId(),
                cohortReview.getCohortReviewId(),
                participantId,
                request)
            .getBody();

    assertCreatedParticipantCohortAnnotation(Objects.requireNonNull(response), request);
  }

  @ParameterizedTest(
      name = "createParticipantCohortAnnotationForbiddenAccessLevels WorkspaceAccessLevel={0}")
  @EnumSource(
      value = WorkspaceAccessLevel.class,
      names = {"READER", "NO_ACCESS"})
  public void createParticipantCohortAnnotationForbiddenAccessLevels(
      WorkspaceAccessLevel workspaceAccessLevel) {
    // change access, call and check
    stubWorkspaceAccessLevel(workspace, workspaceAccessLevel);

    Long cohortReviewId = cohortReview.getCohortReviewId();
    Long participantId = participantCohortStatus1.getParticipantKey().getParticipantId();
    // use stringAnnotationType
    ParticipantCohortAnnotation request =
        buildValidParticipantCohortAnnotationForType(
            cohortReviewId, participantId, AnnotationType.STRING);
    Throwable exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                cohortReviewController.createParticipantCohortAnnotation(
                    workspace.getNamespace(),
                    workspace.getId(),
                    cohortReview.getCohortReviewId(),
                    participantId,
                    request));
    assertForbiddenException(exception);
  }

  ////////// updateParticipantCohortAnnotation  //////////
  @Test
  public void updateParticipantCohortAnnotationNoCohortReview() {
    stubWorkspaceAccessLevel(workspace, WorkspaceAccessLevel.WRITER);
    Long wrongCohortReviewId = -1L;
    Throwable exception =
        assertThrows(
            NotFoundException.class,
            () ->
                cohortReviewController.updateParticipantCohortAnnotation(
                    workspace.getNamespace(),
                    workspace.getId(),
                    wrongCohortReviewId,
                    participantCohortStatus1.getParticipantKey().getParticipantId(),
                    participantAnnotation.getAnnotationId(),
                    new ModifyParticipantCohortAnnotationRequest().annotationValueString("test1")));

    assertNotFoundExceptionCohortReview(wrongCohortReviewId, exception);
  }

  @Test
  public void updateParticipantCohortAnnotationNoParticipantCohortStatus() {
    stubWorkspaceAccessLevel(workspace, WorkspaceAccessLevel.WRITER);
    Long wrongParticipantId = -1L;
    Long cohortReviewId = cohortReview.getCohortReviewId();
    Throwable exception =
        assertThrows(
            NotFoundException.class,
            () ->
                cohortReviewController.updateParticipantCohortAnnotation(
                    workspace.getNamespace(),
                    workspace.getId(),
                    cohortReviewId,
                    wrongParticipantId,
                    participantAnnotation.getAnnotationId(),
                    new ModifyParticipantCohortAnnotationRequest().annotationValueString("test1")));

    assertNotFoundExceptionParticipantCohortStatus(cohortReviewId, wrongParticipantId, exception);
  }

  @Test
  public void updateParticipantCohortAnnotationNoAnnotation() {
    long wrongAnnotationId = -1L;
    stubWorkspaceAccessLevel(workspace, WorkspaceAccessLevel.WRITER);

    Throwable exception =
        assertThrows(
            NotFoundException.class,
            () ->
                cohortReviewController.updateParticipantCohortAnnotation(
                    workspace.getNamespace(),
                    workspace.getId(),
                    cohortReview.getCohortReviewId(),
                    participantCohortStatus1.getParticipantKey().getParticipantId(),
                    wrongAnnotationId,
                    new ModifyParticipantCohortAnnotationRequest().annotationValueString("test1")));

    assertThat(exception)
        .hasMessageThat()
        .isEqualTo(
            String.format(
                "Not Found: Participant Cohort Annotation does not exist for annotationId: %d, cohortReviewId: %d, participantId: %d",
                wrongAnnotationId,
                cohortReview.getCohortReviewId(),
                participantCohortStatus1.getParticipantKey().getParticipantId()));
  }

  @Test
  public void updateParticipantCohortAnnotationIncorrectTypeForString() {
    // participationAnnotation.getAnnotationId is for String type
    Throwable exception =
        assertThrows(
            ConflictException.class,
            () ->
                cohortReviewController.updateParticipantCohortAnnotation(
                    workspace.getNamespace(),
                    workspace.getId(),
                    cohortReview.getCohortReviewId(),
                    participantCohortStatus1.getParticipantKey().getParticipantId(),
                    participantAnnotation.getAnnotationId(),
                    new ModifyParticipantCohortAnnotationRequest()
                        .annotationValueDate("2022-02-21")));

    assertThat(exception)
        .hasMessageThat()
        .isEqualTo(
            String.format(
                "Conflict Exception: Please provide a valid %s value for annotation definition id: %d",
                AnnotationType.STRING,
                stringAnnotationDefinition.getCohortAnnotationDefinitionId()));
  }

  @Test
  public void updateParticipantCohortAnnotationIncorrectDateString() {
    // participationAnnotationDate.getAnnotationId is for date-string
    Throwable exception =
        assertThrows(
            BadRequestException.class,
            () ->
                cohortReviewController.updateParticipantCohortAnnotation(
                    workspace.getNamespace(),
                    workspace.getId(),
                    cohortReview.getCohortReviewId(),
                    participantCohortStatus1.getParticipantKey().getParticipantId(),
                    participantAnnotationDate.getAnnotationId(),
                    new ModifyParticipantCohortAnnotationRequest()
                        .annotationValueDate("22-02-2022")));

    assertThat(exception)
        .hasMessageThat()
        .isEqualTo(
            String.format(
                "Bad Request: Please provide a valid %s value (yyyy-MM-dd) for annotation definition id: %d",
                AnnotationType.DATE, dateAnnotationDefinition.getCohortAnnotationDefinitionId()));
  }

  @ParameterizedTest(name = "updateParticipantCohortAnnotationStringNullOrEmpty value={0}")
  @NullAndEmptySource
  public void updateParticipantCohortAnnotationStringNullOrEmpty(String value) {
    // participationAnnotation.getAnnotationId is for String type
    Throwable exception =
        assertThrows(
            ConflictException.class,
            () ->
                cohortReviewController.updateParticipantCohortAnnotation(
                    workspace.getNamespace(),
                    workspace.getId(),
                    cohortReview.getCohortReviewId(),
                    participantCohortStatus1.getParticipantKey().getParticipantId(),
                    participantAnnotation.getAnnotationId(),
                    new ModifyParticipantCohortAnnotationRequest().annotationValueString(value)));

    assertThat(exception)
        .hasMessageThat()
        .isEqualTo(
            String.format(
                "Conflict Exception: Please provide a valid %s value for annotation definition id: %d",
                AnnotationType.STRING,
                stringAnnotationDefinition.getCohortAnnotationDefinitionId()));
  }

  @ParameterizedTest(
      name = "updateParticipantCohortAnnotationAllowedAccessLevel WorkspaceAccessLevel={0}")
  @EnumSource(
      value = WorkspaceAccessLevel.class,
      names = {"OWNER", "WRITER"})
  public void updateParticipantCohortAnnotationAllowedAccessLevel(
      WorkspaceAccessLevel workspaceAccessLevel) {
    // change access, call and check
    stubWorkspaceAccessLevel(workspace, workspaceAccessLevel);

    ParticipantCohortAnnotation participantCohortAnnotation =
        cohortReviewController
            .updateParticipantCohortAnnotation(
                workspace.getNamespace(),
                workspace.getId(),
                cohortReview.getCohortReviewId(),
                participantCohortStatus1.getParticipantKey().getParticipantId(),
                participantAnnotation.getAnnotationId(),
                new ModifyParticipantCohortAnnotationRequest().annotationValueString("test1"))
            .getBody();

    assertThat(Objects.requireNonNull(participantCohortAnnotation).getAnnotationValueString())
        .isEqualTo("test1");
  }

  @ParameterizedTest(
      name = "updateParticipantCohortAnnotationForbiddenAccessLevel WorkspaceAccessLevel={0}")
  @EnumSource(
      value = WorkspaceAccessLevel.class,
      names = {"READER", "NO_ACCESS"})
  public void updateParticipantCohortAnnotationForbiddenAccessLevel(
      WorkspaceAccessLevel workspaceAccessLevel) {
    // change access, call and check
    stubWorkspaceAccessLevel(workspace, workspaceAccessLevel);

    Throwable exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                cohortReviewController.updateParticipantCohortAnnotation(
                    workspace.getNamespace(),
                    workspace.getId(),
                    cohortReview.getCohortReviewId(),
                    participantCohortStatus1.getParticipantKey().getParticipantId(),
                    participantAnnotation.getAnnotationId(),
                    new ModifyParticipantCohortAnnotationRequest().annotationValueString("test1")));

    assertForbiddenException(exception);
  }

  ////////// deleteParticipantCohortAnnotation  //////////
  @Test
  public void deleteParticipantCohortAnnotationWrongWorkspace() {
    stubWorkspaceAccessLevel(workspace2, WorkspaceAccessLevel.WRITER);

    DbParticipantCohortAnnotation annotation =
        new DbParticipantCohortAnnotation()
            .cohortReviewId(cohortReview.getCohortReviewId())
            .participantId(participantCohortStatus1.getParticipantKey().getParticipantId())
            .annotationValueString("test")
            .cohortAnnotationDefinitionId(
                stringAnnotationDefinition.getCohortAnnotationDefinitionId());

    Throwable exception =
        assertThrows(
            NotFoundException.class,
            () ->
                cohortReviewController.deleteParticipantCohortAnnotation(
                    workspace2.getNamespace(),
                    workspace2.getId(),
                    cohortReview.getCohortReviewId(),
                    participantCohortStatus1.getParticipantKey().getParticipantId(),
                    annotation.getAnnotationId()));

    assertNotFoundExceptionNoCohortReviewAndCohort(cohortReview.getCohortReviewId(), exception);
  }

  @Test
  public void deleteParticipantCohortAnnotationNoAnnotation() {
    Long participantId = participantCohortStatus1.getParticipantKey().getParticipantId();
    saveTestParticipantCohortAnnotation();
    Long wrongAnnotationId = -1L;

    stubWorkspaceAccessLevel(workspace, WorkspaceAccessLevel.WRITER);

    Throwable exception =
        assertThrows(
            NotFoundException.class,
            () ->
                cohortReviewController.deleteParticipantCohortAnnotation(
                    workspace.getNamespace(),
                    workspace.getId(),
                    cohortReview.getCohortReviewId(),
                    participantId,
                    wrongAnnotationId));

    assertThat(exception)
        .hasMessageThat()
        .isEqualTo(
            "Not Found: No participant cohort annotation found for annotationId: "
                + wrongAnnotationId
                + ", cohortReviewId: "
                + cohortReview.getCohortReviewId()
                + ", participantId: "
                + participantId);
  }

  @ParameterizedTest(
      name = "deleteParticipantCohortAnnotationAllowedAccessLevel WorkspaceAccessLevel={0}")
  @EnumSource(
      value = WorkspaceAccessLevel.class,
      names = {"OWNER", "WRITER"})
  public void deleteParticipantCohortAnnotationAllowedAccessLevel(
      WorkspaceAccessLevel workspaceAccessLevel) {
    DbParticipantCohortAnnotation annotation = saveTestParticipantCohortAnnotation();
    // change access, call and check
    stubWorkspaceAccessLevel(workspace, workspaceAccessLevel);

    ResponseEntity<EmptyResponse> response =
        cohortReviewController.deleteParticipantCohortAnnotation(
            workspace.getNamespace(),
            workspace.getId(),
            cohortReview.getCohortReviewId(),
            participantCohortStatus1.getParticipantKey().getParticipantId(),
            annotation.getAnnotationId());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @ParameterizedTest(
      name = "deleteParticipantCohortAnnotationForbiddenAccessLevel WorkspaceAccessLevel={0}")
  @EnumSource(
      value = WorkspaceAccessLevel.class,
      names = {"READER", "NO_ACCESS"})
  public void deleteParticipantCohortAnnotationForbiddenAccessLevel(
      WorkspaceAccessLevel workspaceAccessLevel) {
    DbParticipantCohortAnnotation annotation = saveTestParticipantCohortAnnotation();
    // change access, call and check
    stubWorkspaceAccessLevel(workspace, workspaceAccessLevel);

    Throwable exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                cohortReviewController.deleteParticipantCohortAnnotation(
                    workspace.getNamespace(),
                    workspace.getId(),
                    cohortReview.getCohortReviewId(),
                    participantCohortStatus1.getParticipantKey().getParticipantId(),
                    annotation.getAnnotationId()));

    assertForbiddenException(exception);
  }

  ////////// updateParticipantCohortStatus  //////////
  @Test
  public void updateParticipantCohortStatusWrongWorkspace() {
    stubWorkspaceAccessLevel(workspace2, WorkspaceAccessLevel.WRITER);

    Throwable exception =
        assertThrows(
            NotFoundException.class,
            () ->
                cohortReviewController.updateParticipantCohortStatus(
                    workspace2.getNamespace(),
                    workspace2.getId(),
                    cohortReview.getCohortReviewId(),
                    participantCohortStatus1.getParticipantKey().getParticipantId(),
                    new ModifyCohortStatusRequest().status(CohortStatus.INCLUDED)));

    assertNotFoundExceptionNoCohortReviewAndCohort(cohortReview.getCohortReviewId(), exception);
  }

  @Test
  public void updateParticipantCohortStatusNoCohortReview() {
    stubWorkspaceAccessLevel(workspace, WorkspaceAccessLevel.WRITER);
    Long wrongCohortReviewId = -1L;
    Throwable exception =
        assertThrows(
            NotFoundException.class,
            () ->
                cohortReviewController.updateParticipantCohortStatus(
                    workspace.getNamespace(),
                    workspace.getId(),
                    wrongCohortReviewId,
                    participantCohortStatus1.getParticipantKey().getParticipantId(),
                    new ModifyCohortStatusRequest().status(CohortStatus.INCLUDED)));

    assertNotFoundExceptionCohortReview(wrongCohortReviewId, exception);
  }

  @Test
  public void updateParticipantCohortStatusNoParticipantId() {
    stubWorkspaceAccessLevel(workspace, WorkspaceAccessLevel.WRITER);
    Long cohortReviewId = cohortReview.getCohortReviewId();
    Long wrongParticipantId = -1L;
    Throwable exception =
        assertThrows(
            NotFoundException.class,
            () ->
                cohortReviewController.updateParticipantCohortStatus(
                    workspace.getNamespace(),
                    workspace.getId(),
                    cohortReviewId,
                    wrongParticipantId,
                    new ModifyCohortStatusRequest().status(CohortStatus.INCLUDED)));

    assertNotFoundExceptionParticipantCohortStatus(cohortReviewId, wrongParticipantId, exception);
  }

  @ParameterizedTest(
      name = "updateParticipantCohortStatusAllowedAccessLevel WorkspaceAccessLevel={0}")
  @EnumSource(
      value = WorkspaceAccessLevel.class,
      names = {"OWNER", "WRITER"})
  public void updateParticipantCohortStatusAllowedAccessLevel(
      WorkspaceAccessLevel workspaceAccessLevel) {
    // change access, call and check
    stubWorkspaceAccessLevel(workspace, workspaceAccessLevel);

    ParticipantCohortStatus participantCohortStatus =
        cohortReviewController
            .updateParticipantCohortStatus(
                workspace.getNamespace(),
                workspace.getId(),
                cohortReview.getCohortReviewId(),
                participantCohortStatus1.getParticipantKey().getParticipantId(),
                new ModifyCohortStatusRequest().status(CohortStatus.INCLUDED))
            .getBody();

    assertThat(Objects.requireNonNull(participantCohortStatus).getStatus())
        .isEqualTo(CohortStatus.INCLUDED);
  }

  @ParameterizedTest(name = "updateParticipantCohortStatusForbiddenLevels WorkspaceAccessLevel={0}")
  @EnumSource(
      value = WorkspaceAccessLevel.class,
      names = {"READER", "NO_ACCESS"})
  public void updateParticipantCohortStatusForbiddenLevels(
      WorkspaceAccessLevel workspaceAccessLevel) {
    // change access, call and check
    stubWorkspaceAccessLevel(workspace, workspaceAccessLevel);

    Throwable exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                cohortReviewController.updateParticipantCohortStatus(
                    workspace.getNamespace(),
                    workspace.getId(),
                    cohortReview.getCohortReviewId(),
                    participantCohortStatus1.getParticipantKey().getParticipantId(),
                    new ModifyCohortStatusRequest().status(CohortStatus.INCLUDED)));

    assertForbiddenException(exception);
  }

  ////////// getParticipantCohortAnnotations  //////////
  @Test
  public void getParticipantCohortAnnotationsWrongWorkspace() {
    stubWorkspaceAccessLevel(workspace2, WorkspaceAccessLevel.READER);

    Throwable exception =
        assertThrows(
            NotFoundException.class,
            () ->
                cohortReviewController.getParticipantCohortAnnotations(
                    workspace2.getNamespace(),
                    workspace2.getId(),
                    cohortReview.getCohortReviewId(),
                    participantCohortStatus1.getParticipantKey().getParticipantId()));

    assertNotFoundExceptionNoCohortReviewAndCohort(cohortReview.getCohortReviewId(), exception);
  }

  @Test
  public void getParticipantCohortAnnotationsNoCohortReview() {
    stubWorkspaceAccessLevel(workspace, WorkspaceAccessLevel.READER);
    long wrongCohortReviewId = -1L;

    Throwable exception =
        assertThrows(
            NotFoundException.class,
            () ->
                cohortReviewController.getParticipantCohortAnnotations(
                    workspace.getNamespace(),
                    workspace.getId(),
                    wrongCohortReviewId,
                    participantCohortStatus1.getParticipantKey().getParticipantId()));

    assertNotFoundExceptionCohortReview(wrongCohortReviewId, exception);
  }

  @ParameterizedTest(
      name = "getParticipantCohortAnnotationsAllowedAccessLevel WorkspaceAccessLevel={0}")
  @EnumSource(
      value = WorkspaceAccessLevel.class,
      names = {"OWNER", "WRITER", "READER"})
  public void getParticipantCohortAnnotationsAllowedAccessLevel(
      WorkspaceAccessLevel workspaceAccessLevel) {
    // change access, call and check
    stubWorkspaceAccessLevel(workspace, workspaceAccessLevel);

    ParticipantCohortAnnotationListResponse response =
        cohortReviewController
            .getParticipantCohortAnnotations(
                workspace.getNamespace(),
                workspace.getId(),
                cohortReview.getCohortReviewId(),
                participantCohortStatus1.getParticipantKey().getParticipantId())
            .getBody();

    assertThat(Objects.requireNonNull(response).getItems().size()).isEqualTo(2);
    List<ParticipantCohortAnnotation> expected =
        ImmutableList.of(
            participantCohortAnnotationMapper.dbModelToClient(participantAnnotation),
            participantCohortAnnotationMapper.dbModelToClient(participantAnnotationDate));

    assertThat(response.getItems()).containsExactlyElementsIn(expected);
  }

  @ParameterizedTest(
      name = "getParticipantCohortAnnotationsForbiddenAccessLevel WorkspaceAccessLevel={0}")
  @EnumSource(
      value = WorkspaceAccessLevel.class,
      names = {"NO_ACCESS"})
  public void getParticipantCohortAnnotationsForbiddenAccessLevel(
      WorkspaceAccessLevel workspaceAccessLevel) {
    // change access, call and check
    stubWorkspaceAccessLevel(workspace, workspaceAccessLevel);

    Throwable exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                cohortReviewController.getParticipantCohortAnnotations(
                    workspace.getNamespace(),
                    workspace.getId(),
                    cohortReview.getCohortReviewId(),
                    participantCohortStatus1.getParticipantKey().getParticipantId()));

    assertForbiddenException(exception);
  }

  ////////// getParticipantCohortStatus  //////////
  @Test
  public void getParticipantCohortStatusWrongWorkspace() {
    stubWorkspaceAccessLevel(workspace2, WorkspaceAccessLevel.READER);

    Throwable exception =
        assertThrows(
            NotFoundException.class,
            () ->
                cohortReviewController.getParticipantCohortStatus(
                    workspace2.getNamespace(),
                    workspace2.getId(),
                    cohortReview.getCohortReviewId(),
                    participantCohortStatus1.getParticipantKey().getParticipantId()));

    assertNotFoundExceptionNoCohortReviewAndCohort(cohortReview.getCohortReviewId(), exception);
  }

  @Test
  public void getParticipantCohortStatusNoCohortReview() {
    stubWorkspaceAccessLevel(workspace, WorkspaceAccessLevel.READER);
    long wrongCohortReviewId = -1L;

    Throwable exception =
        assertThrows(
            NotFoundException.class,
            () ->
                cohortReviewController.getParticipantCohortStatus(
                    workspace.getNamespace(),
                    workspace.getId(),
                    wrongCohortReviewId,
                    participantCohortStatus1.getParticipantKey().getParticipantId()));

    assertNotFoundExceptionCohortReview(wrongCohortReviewId, exception);
  }

  @ParameterizedTest(name = "getParticipantCohortStatusAllowedAccessLevel WorkspaceAccessLevel={0}")
  @EnumSource(
      value = WorkspaceAccessLevel.class,
      names = {"OWNER", "WRITER", "READER"})
  public void getParticipantCohortStatusAllowedAccessLevel(
      WorkspaceAccessLevel workspaceAccessLevel) {
    stubWorkspaceAccessLevel(workspace, workspaceAccessLevel);

    ParticipantCohortStatus response =
        cohortReviewController
            .getParticipantCohortStatus(
                workspace.getNamespace(),
                workspace.getId(),
                cohortReview.getCohortReviewId(),
                participantCohortStatus1.getParticipantKey().getParticipantId())
            .getBody();

    assertThat(Objects.requireNonNull(response).getParticipantId())
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

  @ParameterizedTest(
      name = "getParticipantCohortStatusForbiddenAccessLevel WorkspaceAccessLevel={0}")
  @EnumSource(
      value = WorkspaceAccessLevel.class,
      names = {"NO_ACCESS"})
  public void getParticipantCohortStatusForbiddenAccessLevel(
      WorkspaceAccessLevel workspaceAccessLevel) {
    stubWorkspaceAccessLevel(workspace, workspaceAccessLevel);

    Throwable exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                cohortReviewController.getParticipantCohortStatus(
                    workspace.getNamespace(),
                    workspace.getId(),
                    cohortReview.getCohortReviewId(),
                    participantCohortStatus1.getParticipantKey().getParticipantId()));

    assertForbiddenException(exception);
  }
  ////////// getParticipantCount  //////////
  @Test
  public void getParticipantCountWrongWorkspace() {
    stubWorkspaceAccessLevel(workspace2, WorkspaceAccessLevel.READER);

    Throwable exception =
        assertThrows(
            NotFoundException.class,
            () ->
                cohortReviewController.getParticipantCount(
                    workspace2.getNamespace(),
                    workspace2.getId(),
                    cohortReview.getCohortReviewId(),
                    participantCohortStatus1.getParticipantKey().getParticipantId(),
                    new PageFilterRequest().domain(Domain.CONDITION)));

    assertNotFoundExceptionNoCohortReviewAndCohort(cohortReview.getCohortReviewId(), exception);
  }

  @Test
  public void getParticipantCountNoCohortReview() {
    stubWorkspaceAccessLevel(workspace, WorkspaceAccessLevel.READER);
    long wrongCohortReviewId = -1L;

    Throwable exception =
        assertThrows(
            NotFoundException.class,
            () ->
                cohortReviewController.getParticipantCount(
                    workspace.getNamespace(),
                    workspace.getId(),
                    wrongCohortReviewId,
                    participantCohortStatus1.getParticipantKey().getParticipantId(),
                    new PageFilterRequest().domain(Domain.CONDITION)));

    assertNotFoundExceptionCohortReview(wrongCohortReviewId, exception);
  }

  @Test
  public void getParticipantCountNullDomain() {
    stubWorkspaceAccessLevel(workspace, WorkspaceAccessLevel.READER);
    long cohortReviewId = cohortReview.getCohortReviewId();
    Throwable exception =
        assertThrows(
            BadRequestException.class,
            () ->
                cohortReviewController.getParticipantCount(
                    workspace.getNamespace(),
                    workspace.getId(),
                    cohortReviewId,
                    participantCohortStatus1.getParticipantKey().getParticipantId(),
                    new PageFilterRequest().domain(null)));
    assertThat(exception).hasMessageThat().isEqualTo("Domain cannot be null");
  }

  @ParameterizedTest(name = "getParticipantCountByUnsupportedDomain Domain={0}")
  @EnumSource(
      value = Domain.class,
      mode = Mode.EXCLUDE,
      names = {
        "SURVEY",
        "ALL_EVENTS",
        "CONDITION",
        "DEVICE",
        "DRUG",
        "LAB",
        "OBSERVATION",
        "PHYSICAL_MEASUREMENT",
        "PROCEDURE",
        "VISIT",
        "VITAL"
      })
  public void getParticipantCountByUnsupportedDomain(Domain domain) {
    stubWorkspaceAccessLevel(workspace, WorkspaceAccessLevel.READER);
    long cohortReviewId = cohortReview.getCohortReviewId();
    Throwable exception =
        assertThrows(
            BadRequestException.class,
            () ->
                cohortReviewController.getParticipantCount(
                    workspace.getNamespace(),
                    workspace.getId(),
                    cohortReviewId,
                    participantCohortStatus1.getParticipantKey().getParticipantId(),
                    new PageFilterRequest().domain(domain)));

    assertThat(exception).hasMessageThat().isEqualTo("Not supported for domain named: " + domain);
  }

  @ParameterizedTest(name = "getParticipantCountAllowedAccessLevel WorkspaceAccessLevel={0}")
  @EnumSource(
      value = WorkspaceAccessLevel.class,
      names = {"OWNER", "WRITER", "READER"})
  public void getParticipantCountAllowedAccessLevel(WorkspaceAccessLevel workspaceAccessLevel) {
    // change access, call and check
    stubWorkspaceAccessLevel(workspace, workspaceAccessLevel);
    long cohortReviewId = cohortReview.getCohortReviewId();
    stubBigQueryParticipantCount();
    ParticipantDataCountResponse actual =
        cohortReviewController
            .getParticipantCount(
                workspace.getNamespace(),
                workspace.getId(),
                cohortReviewId,
                participantCohortStatus1.getParticipantKey().getParticipantId(),
                new PageFilterRequest().domain(Domain.CONDITION))
            .getBody();

    assertThat(Objects.requireNonNull(actual).getCount()).isEqualTo(0L);
  }

  @ParameterizedTest(name = "getParticipantCountForbiddenAccessLevel WorkspaceAccessLevel={0}")
  @EnumSource(
      value = WorkspaceAccessLevel.class,
      names = {"NO_ACCESS"})
  public void getParticipantCountForbiddenAccessLevel(WorkspaceAccessLevel workspaceAccessLevel) {
    // change access, call and check
    stubWorkspaceAccessLevel(workspace, workspaceAccessLevel);
    long cohortReviewId = cohortReview.getCohortReviewId();

    Throwable exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                cohortReviewController.getParticipantCount(
                    workspace.getNamespace(),
                    workspace.getId(),
                    cohortReviewId,
                    participantCohortStatus1.getParticipantKey().getParticipantId(),
                    new PageFilterRequest().domain(Domain.CONDITION)));
    assertForbiddenException(exception);
  }

  ////////// getParticipantData  //////////
  @Test
  public void getParticipantDataWrongWorkspace() {
    stubWorkspaceAccessLevel(workspace2, WorkspaceAccessLevel.READER);

    Throwable exception =
        assertThrows(
            NotFoundException.class,
            () ->
                cohortReviewController.getParticipantData(
                    workspace2.getNamespace(),
                    workspace2.getId(),
                    cohortReview.getCohortReviewId(),
                    participantCohortStatus1.getParticipantKey().getParticipantId(),
                    new PageFilterRequest().domain(Domain.CONDITION)));

    assertNotFoundExceptionNoCohortReviewAndCohort(cohortReview.getCohortReviewId(), exception);
  }

  @Test
  public void getParticipantDataNoCohortReview() {
    stubWorkspaceAccessLevel(workspace, WorkspaceAccessLevel.READER);
    long wrongCohortReviewId = -1L;

    Throwable exception =
        assertThrows(
            NotFoundException.class,
            () ->
                cohortReviewController.getParticipantData(
                    workspace.getNamespace(),
                    workspace.getId(),
                    wrongCohortReviewId,
                    participantCohortStatus1.getParticipantKey().getParticipantId(),
                    new PageFilterRequest().domain(Domain.CONDITION)));

    assertNotFoundExceptionCohortReview(wrongCohortReviewId, exception);
  }

  @Test
  public void getParticipantDataNullDomain() {
    stubWorkspaceAccessLevel(workspace, WorkspaceAccessLevel.READER);
    long cohortReviewId = cohortReview.getCohortReviewId();
    Throwable exception =
        assertThrows(
            BadRequestException.class,
            () ->
                cohortReviewController.getParticipantData(
                    workspace.getNamespace(),
                    workspace.getId(),
                    cohortReviewId,
                    participantCohortStatus1.getParticipantKey().getParticipantId(),
                    new PageFilterRequest().domain(null)));
    assertThat(exception).hasMessageThat().isEqualTo("Domain cannot be null");
  }

  @ParameterizedTest(name = "getParticipantCountByUnsupportedDomain Domain={0}")
  @EnumSource(
      value = Domain.class,
      mode = Mode.EXCLUDE,
      names = {
        "SURVEY",
        "ALL_EVENTS",
        "CONDITION",
        "DEVICE",
        "DRUG",
        "LAB",
        "OBSERVATION",
        "PHYSICAL_MEASUREMENT",
        "PROCEDURE",
        "VISIT",
        "VITAL"
      })
  public void getParticipantDataByUnsupportedDomain(Domain domain) {
    stubWorkspaceAccessLevel(workspace, WorkspaceAccessLevel.READER);
    long cohortReviewId = cohortReview.getCohortReviewId();
    Throwable exception =
        assertThrows(
            BadRequestException.class,
            () ->
                cohortReviewController.getParticipantData(
                    workspace.getNamespace(),
                    workspace.getId(),
                    cohortReviewId,
                    participantCohortStatus1.getParticipantKey().getParticipantId(),
                    new PageFilterRequest().domain(domain)));

    assertThat(exception).hasMessageThat().isEqualTo("Not supported for domain named: " + domain);
  }

  @ParameterizedTest(name = "getParticipantDataAllowedAccessLevel WorkspaceAccessLevel={0}")
  @EnumSource(
      value = WorkspaceAccessLevel.class,
      names = {"OWNER", "WRITER", "READER"})
  public void getParticipantDataAllowedAccessLevel(WorkspaceAccessLevel workspaceAccessLevel) {
    // change access, call and check
    stubWorkspaceAccessLevel(workspace, workspaceAccessLevel);
    long cohortReviewId = cohortReview.getCohortReviewId();
    stubBigQueryParticipantData();
    ParticipantDataListResponse actual =
        cohortReviewController
            .getParticipantData(
                workspace.getNamespace(),
                workspace.getId(),
                cohortReviewId,
                participantCohortStatus1.getParticipantKey().getParticipantId(),
                new PageFilterRequest().domain(Domain.OBSERVATION))
            .getBody();
    assertThat(Objects.requireNonNull(actual).getItems().size()).isEqualTo(1);
  }

  @ParameterizedTest(name = "getParticipantDataForbiddenAccessLevel WorkspaceAccessLevel={0}")
  @EnumSource(
      value = WorkspaceAccessLevel.class,
      names = {"NO_ACCESS"})
  public void getParticipantDataForbiddenAccessLevel(WorkspaceAccessLevel workspaceAccessLevel) {
    // change access, call and check
    stubWorkspaceAccessLevel(workspace, workspaceAccessLevel);
    long cohortReviewId = cohortReview.getCohortReviewId();

    Throwable exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                cohortReviewController.getParticipantData(
                    workspace.getNamespace(),
                    workspace.getId(),
                    cohortReviewId,
                    participantCohortStatus1.getParticipantKey().getParticipantId(),
                    new PageFilterRequest().domain(Domain.SURVEY)));
    assertForbiddenException(exception);
  }

  ////////// getParticipantCohortStatuses  //////////
  @Test
  public void getParticipantCohortStatusesWrongWorkspace() {
    stubWorkspaceAccessLevel(workspace2, WorkspaceAccessLevel.READER);

    Throwable exception =
        assertThrows(
            NotFoundException.class,
            () ->
                cohortReviewController.getParticipantCohortStatuses(
                    workspace2.getNamespace(),
                    workspace2.getId(),
                    cohortReview.getCohortReviewId(),
                    new PageFilterRequest()));

    // No CohortReview exists for cohortReviewId: 1 and cohortId: 1
    assertNotFoundExceptionNoCohortReviewAndCohort(cohortReview.getCohortReviewId(), exception);
  }

  @ParameterizedTest(
      name = "getParticipantCohortStatusesSortByFilterColumn SortOrder={0}, FilterColumns={1}")
  @MethodSource("paramsSortByFilterColumn")
  public void getParticipantCohortStatusesSortByFilterColumn(
      SortOrder sortOrder, FilterColumns filterColumns) {
    CohortReview expectedReview =
        createCohortReview(
            cohortReview, ImmutableList.of(participantCohortStatus1, participantCohortStatus2));
    // default filterColumn=participant_id
    PageFilterRequest pageFilterRequest =
        new PageFilterRequest().sortOrder(sortOrder).sortColumn(filterColumns);

    CohortReview actualReview =
        Objects.requireNonNull(
                cohortReviewController
                    .getParticipantCohortStatuses(
                        workspace.getNamespace(),
                        workspace.getId(),
                        cohortReview.getCohortReviewId(),
                        pageFilterRequest)
                    .getBody())
            .getCohortReview();

    verify(userRecentResourceService, atLeastOnce())
        .updateCohortReviewEntry(anyLong(), anyLong(), anyLong());
    assertCohortReviewParticipantCohortStatuses(
        actualReview, expectedReview, filterColumns, sortOrder);
  }

  @ParameterizedTest(
      name = "getParticipantCohortStatusesAllowedAccessLevel WorkspaceAccessLevel={0}")
  @EnumSource(
      value = WorkspaceAccessLevel.class,
      names = {"OWNER", "WRITER", "READER"})
  public void getParticipantCohortStatusesAllowedAccessLevel(
      WorkspaceAccessLevel workspaceAccessLevel) {
    CohortReview expectedReview =
        createCohortReview(
            cohortReview, ImmutableList.of(participantCohortStatus1, participantCohortStatus2));

    // change access, call and check
    stubWorkspaceAccessLevel(workspace, workspaceAccessLevel);

    CohortReview actualReview =
        Objects.requireNonNull(
                cohortReviewController
                    .getParticipantCohortStatuses(
                        workspace.getNamespace(),
                        workspace.getId(),
                        cohortReview.getCohortReviewId(),
                        new PageFilterRequest())
                    .getBody())
            .getCohortReview();

    verify(userRecentResourceService, atLeastOnce())
        .updateCohortReviewEntry(anyLong(), anyLong(), anyLong());
    // PageFilterRequest defaults to participantId, ascending (if not given)
    assertCohortReviewParticipantCohortStatuses(
        actualReview, expectedReview, FilterColumns.PARTICIPANTID, SortOrder.ASC);
  }

  @ParameterizedTest(
      name = "getParticipantCohortStatusesForbiddenAccessLevel WorkspaceAccessLevel={0}")
  @EnumSource(
      value = WorkspaceAccessLevel.class,
      names = {"NO_ACCESS"})
  public void getParticipantCohortStatusesForbiddenAccessLevel(
      WorkspaceAccessLevel workspaceAccessLevel) {
    createCohortReview(
        cohortReview, ImmutableList.of(participantCohortStatus1, participantCohortStatus2));

    // change access, call and check
    stubWorkspaceAccessLevel(workspace, workspaceAccessLevel);

    Throwable exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                cohortReviewController.getParticipantCohortStatuses(
                    workspace.getNamespace(),
                    workspace.getId(),
                    cohortReview.getCohortReviewId(),
                    new PageFilterRequest()));

    assertForbiddenException(exception);
  }

  ////////// getCohortReviewsInWorkspace  //////////
  @ParameterizedTest(
      name = "getCohortReviewsInWorkspaceAllowedAccessLevel WorkspaceAccessLevel={0}")
  @EnumSource(
      value = WorkspaceAccessLevel.class,
      names = {"OWNER", "WRITER", "READER"})
  public void getCohortReviewsInWorkspaceAllowedAccessLevel(
      WorkspaceAccessLevel workspaceAccessLevel) {
    List<CohortReview> expected =
        ImmutableList.of(
            cohortReviewMapper.dbModelToClient(cohortReview),
            cohortReviewMapper.dbModelToClient(cohortReview2));

    // change access, call and check
    stubWorkspaceAccessLevel(workspace, workspaceAccessLevel);

    List<CohortReview> actual =
        Objects.requireNonNull(
                cohortReviewController
                    .getCohortReviewsInWorkspace(workspace.getNamespace(), workspace.getId())
                    .getBody())
            .getItems();

    assertThat(actual).isEqualTo(expected);
  }

  @ParameterizedTest(
      name = "getCohortReviewsInWorkspaceForbiddenAccessLevel WorkspaceAccessLevel={0}")
  @EnumSource(
      value = WorkspaceAccessLevel.class,
      names = {"NO_ACCESS"})
  public void getCohortReviewsInWorkspaceForbiddenAccessLevel(
      WorkspaceAccessLevel workspaceAccessLevel) {

    // change access, call and check
    stubWorkspaceAccessLevel(workspace, workspaceAccessLevel);

    Throwable exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                cohortReviewController.getCohortReviewsInWorkspace(
                    workspace.getNamespace(), workspace.getId()));

    assertForbiddenException(exception);
  }

  ////////// getCohortReviewsByCohortId  //////////
  @ParameterizedTest(name = "getCohortReviewsByCohortIdAllowedAccessLevel WorkspaceAccessLevel={0}")
  @EnumSource(
      value = WorkspaceAccessLevel.class,
      names = {"OWNER", "WRITER", "READER"})
  public void getCohortReviewsByCohortIdAllowedAccessLevel(
      WorkspaceAccessLevel workspaceAccessLevel) {
    DbCohortReview cohortReviewMultiple =
        cohortReviewDao.save(
            new DbCohortReview()
                .cohortId(cohort.getCohortId())
                .cdrVersionId(cdrVersion.getCdrVersionId())
                .reviewSize(10)
                .creationTime(new Timestamp(new Date().getTime())));

    List<CohortReview> expected =
        ImmutableList.of(
            cohortReviewMapper.dbModelToClient(cohortReview),
            cohortReviewMapper.dbModelToClient(cohortReviewMultiple));

    // change access, call and check
    stubWorkspaceAccessLevel(workspace, workspaceAccessLevel);

    List<CohortReview> actual =
        Objects.requireNonNull(
                cohortReviewController
                    .getCohortReviewsByCohortId(
                        workspace.getNamespace(), workspace.getId(), cohort.getCohortId())
                    .getBody())
            .getItems();

    assertThat(actual).isEqualTo(expected);
  }

  @ParameterizedTest(
      name = "getCohortReviewsByCohortIdForbiddenAccessLevel WorkspaceAccessLevel={0}")
  @EnumSource(
      value = WorkspaceAccessLevel.class,
      names = {"NO_ACCESS"})
  public void getCohortReviewsByCohortIdForbiddenAccessLevel(
      WorkspaceAccessLevel workspaceAccessLevel) {

    // change access, call and check
    stubWorkspaceAccessLevel(workspace, workspaceAccessLevel);

    Throwable exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                cohortReviewController.getCohortReviewsByCohortId(
                    workspace.getNamespace(), workspace.getId(), cohort.getCohortId()));

    assertForbiddenException(exception);
  }

  @ParameterizedTest(name = "getCohortReviewsByCohortIdWrongWorkspace WorkspaceAccessLevel={0}")
  @EnumSource(
      value = WorkspaceAccessLevel.class,
      names = {"READER"})
  public void getCohortReviewsByCohortIdWrongWorkspace(WorkspaceAccessLevel workspaceAccessLevel) {
    // change access, call and check
    stubWorkspaceAccessLevel(workspace2, workspaceAccessLevel);

    Throwable exception =
        assertThrows(
            NotFoundException.class,
            () ->
                cohortReviewController.getCohortReviewsByCohortId(
                    workspace2.getNamespace(), workspace2.getId(), cohort.getCohortId()));
    assertNotFoundExceptionNoCohort(cohort.getCohortId(), exception);
  }

  ////////// getParticipantChartData - See CohortReviewControllerBQTest   //////////
  @ParameterizedTest(name = "getParticipantChartDataAllowedAccessLevel WorkspaceAccessLevel={0}")
  @EnumSource(
      value = WorkspaceAccessLevel.class,
      names = {"OWNER", "WRITER", "READER"})
  public void getParticipantChartDataAllowedAccessLevel(WorkspaceAccessLevel workspaceAccessLevel) {
    // change access, call and check
    stubWorkspaceAccessLevel(workspace, workspaceAccessLevel);
    stubBigQueryParticipantChartData();

    List<ParticipantChartData> actual =
        Objects.requireNonNull(
                cohortReviewController
                    .getParticipantChartData(
                        workspace.getNamespace(),
                        workspace.getId(),
                        cohortReview.getCohortReviewId(),
                        participantCohortStatus1.getParticipantKey().getParticipantId(),
                        Domain.CONDITION.toString())
                    .getBody())
            .getItems();

    assertThat(actual.size()).isEqualTo(1);
  }

  @ParameterizedTest(name = "getParticipantChartDataForbiddenAccessLevel WorkspaceAccessLevel={0}")
  @EnumSource(
      value = WorkspaceAccessLevel.class,
      names = {"NO_ACCESS"})
  public void getParticipantChartDataForbiddenAccessLevel(
      WorkspaceAccessLevel workspaceAccessLevel) {
    // change access, call and check
    stubWorkspaceAccessLevel(workspace, workspaceAccessLevel);

    Throwable exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                cohortReviewController.getParticipantChartData(
                    workspace.getNamespace(),
                    workspace.getId(),
                    cohortReview.getCohortReviewId(),
                    participantCohortStatus1.getParticipantKey().getParticipantId(),
                    Domain.CONDITION.toString()));

    assertForbiddenException(exception);
  }

  ////////// findCohortReviewDemoChartInfo  //////////
  @ParameterizedTest(
      name = "findCohortReviewDemoChartInfoAllowedAccessLevel WorkspaceAccessLevel={0}")
  @EnumSource(
      value = WorkspaceAccessLevel.class,
      names = {"OWNER", "WRITER", "READER"})
  public void findCohortReviewDemoChartInfoAllowedAccessLevel(
      WorkspaceAccessLevel workspaceAccessLevel) {
    // change access, call and check
    stubWorkspaceAccessLevel(workspace, workspaceAccessLevel);
    stubBigQueryDemoChartData();

    List<DemoChartInfo> actual =
        Objects.requireNonNull(
                cohortReviewController
                    .findCohortReviewDemoChartInfo(
                        workspace.getNamespace(),
                        workspace.getId(),
                        cohortReview.getCohortReviewId())
                    .getBody())
            .getItems();

    assertThat(actual.size()).isEqualTo(1);
  }

  @ParameterizedTest(
      name = "findCohortReviewDemoChartInfoForbiddenAccessLevel WorkspaceAccessLevel={0}")
  @EnumSource(
      value = WorkspaceAccessLevel.class,
      names = {"NO_ACCESS"})
  public void findCohortReviewDemoChartInfoForbiddenAccessLevel(
      WorkspaceAccessLevel workspaceAccessLevel) {
    // change access, call and check
    stubWorkspaceAccessLevel(workspace, workspaceAccessLevel);

    Throwable exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                cohortReviewController.findCohortReviewDemoChartInfo(
                    workspace.getNamespace(), workspace.getId(), cohortReview.getCohortReviewId()));

    assertForbiddenException(exception);
  }

  @ParameterizedTest(name = "findCohortReviewDemoChartInfoWrongWorkspace WorkspaceAccessLevel={0}")
  @EnumSource(
      value = WorkspaceAccessLevel.class,
      names = {"READER"})
  public void findCohortReviewDemoChartInfoWrongWorkspace(
      WorkspaceAccessLevel workspaceAccessLevel) {
    // change access, call and check
    stubWorkspaceAccessLevel(workspace2, workspaceAccessLevel);

    Throwable exception =
        assertThrows(
            NotFoundException.class,
            () ->
                cohortReviewController.findCohortReviewDemoChartInfo(
                    workspace2.getNamespace(),
                    workspace2.getId(),
                    cohortReview.getCohortReviewId()));
    assertNotFoundExceptionNoCohortReviewAndCohort(cohortReview.getCohortReviewId(), exception);
  }

  ////////// getVocabularies - See CohortReviewControllerBQTest   //////////
  @ParameterizedTest(name = "getVocabulariesAllowedAccessLevel WorkspaceAccessLevel={0}")
  @EnumSource(
      value = WorkspaceAccessLevel.class,
      names = {"OWNER", "WRITER", "READER"})
  public void getVocabulariesAllowedAccessLevel(WorkspaceAccessLevel workspaceAccessLevel) {
    // change access, call and check
    stubWorkspaceAccessLevel(workspace, workspaceAccessLevel);
    stubBigQueryVocabulary();

    List<Vocabulary> actual =
        Objects.requireNonNull(
                cohortReviewController
                    .getVocabularies(workspace.getNamespace(), workspace.getId())
                    .getBody())
            .getItems();

    assertThat(actual.size()).isEqualTo(1);
  }

  @ParameterizedTest(name = "getVocabulariesForbiddenAccessLevel WorkspaceAccessLevel={0}")
  @EnumSource(
      value = WorkspaceAccessLevel.class,
      names = {"NO_ACCESS"})
  public void getVocabulariesForbiddenAccessLevel(WorkspaceAccessLevel workspaceAccessLevel) {
    // change access, call and check
    stubWorkspaceAccessLevel(workspace, workspaceAccessLevel);

    Throwable exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                cohortReviewController.getVocabularies(
                    workspace.getNamespace(), workspace.getId()));

    assertForbiddenException(exception);
  }
  ////////// helper methods  //////////

  private static Stream<Arguments> paramsSortByFilterColumn() {
    List<Arguments> argsList =
        Arrays.asList(
            arguments(SortOrder.ASC, FilterColumns.PARTICIPANTID),
            arguments(SortOrder.ASC, FilterColumns.STATUS),
            arguments(SortOrder.DESC, FilterColumns.PARTICIPANTID),
            arguments(SortOrder.DESC, FilterColumns.STATUS));
    Collections.shuffle(argsList);
    return argsList.stream();
  }

  private void assertCohortReviewParticipantCohortStatuses(
      CohortReview actual, CohortReview expected, FilterColumns filterColumn, SortOrder sortOrder) {
    // change expected based on sortOrder and filterColumn
    if (SortOrder.DESC == sortOrder && FilterColumns.PARTICIPANTID == filterColumn) {
      expected
          .getParticipantCohortStatuses()
          .sort(Comparator.comparing(ParticipantCohortStatus::getParticipantId).reversed());
    } else if (SortOrder.DESC == sortOrder && FilterColumns.STATUS == filterColumn) {
      expected
          .getParticipantCohortStatuses()
          .sort(Comparator.comparing(ParticipantCohortStatus::getStatus).reversed());
    } else if (SortOrder.ASC == sortOrder && FilterColumns.PARTICIPANTID == filterColumn) {
      expected
          .getParticipantCohortStatuses()
          .sort(Comparator.comparing(ParticipantCohortStatus::getParticipantId));
    } else if (SortOrder.ASC == sortOrder && FilterColumns.STATUS == filterColumn) {
      expected
          .getParticipantCohortStatuses()
          .sort(Comparator.comparing(ParticipantCohortStatus::getStatus));
    }

    assertThat(actual).isEqualTo(expected);
  }

  private void assertNewlyCreatedCohortReview(CohortReview cohortReview, String reviewName) {
    assertThat(cohortReview.getReviewStatus()).isEqualTo(ReviewStatus.CREATED);
    assertThat(cohortReview.getCohortName()).isEqualTo(reviewName);
    assertThat(cohortReview.getDescription()).isEqualTo(cohortWithoutReview.getDescription());
    assertThat(cohortReview.getReviewSize()).isEqualTo(1);
    assertThat(cohortReview.getParticipantCohortStatuses().size()).isEqualTo(1);
    assertThat(cohortReview.getParticipantCohortStatuses().get(0).getStatus())
        .isEqualTo(CohortStatus.NOT_REVIEWED);
  }

  private void assertNotFoundExceptionParticipantCohortStatus(
      Long cohortReviewId, Long participantId, Throwable exception) {
    assertThat(exception)
        .hasMessageThat()
        .isEqualTo(
            "Not Found: Participant Cohort Status does not exist for cohortReviewId: "
                + cohortReviewId
                + ", participantId: "
                + participantId);
  }

  private void assertNotFoundExceptionCohortReview(Long cohortReviewId, Throwable exception) {
    assertThat(exception)
        .hasMessageThat()
        .isEqualTo("Not Found: Cohort Review does not exist for cohortReviewId: " + cohortReviewId);
  }

  private void assertNotFoundExceptionNoCohort(Long cohortId, Throwable exception) {
    assertThat(exception)
        .hasMessageThat()
        .isEqualTo("Not Found: No Cohort exists for cohortId: " + cohortId);
  }

  private void assertNotFoundExceptionNoCohortReviewAndCohort(
      Long cohortReviewId, Throwable exception) {
    assertThat(exception)
        .hasMessageThat()
        .containsMatch(
            "Not Found: No CohortReview exists for cohortReviewId: "
                + cohortReviewId
                + " and cohortId:.*");
  }

  private void assertForbiddenException(Throwable exception) {
    assertThat(exception)
        .hasMessageThat()
        .containsMatch("You do not have sufficient permissions to access");
  }

  /** Helper method to consolidate assertions for all the {@link AnnotationType}s. */
  private void assertCreatedParticipantCohortAnnotation(
      ParticipantCohortAnnotation response, ParticipantCohortAnnotation expected) {
    assertThat(response.getAnnotationValueString()).isEqualTo(expected.getAnnotationValueString());
    assertThat(response.getAnnotationValueBoolean())
        .isEqualTo(expected.getAnnotationValueBoolean());
    assertThat(response.getAnnotationValueEnum()).isEqualTo(expected.getAnnotationValueEnum());
    assertThat(response.getAnnotationValueDate()).isEqualTo(expected.getAnnotationValueDate());
    assertThat(response.getAnnotationValueInteger())
        .isEqualTo(expected.getAnnotationValueInteger());
    assertThat(response.getParticipantId()).isEqualTo(expected.getParticipantId());
    assertThat(response.getCohortReviewId()).isEqualTo(expected.getCohortReviewId());
    assertThat(response.getCohortAnnotationDefinitionId())
        .isEqualTo(expected.getCohortAnnotationDefinitionId());
  }

  private ParticipantCohortAnnotation buildNoValueParticipantCohortAnnotationForType(
      Long cohortReviewId, Long participantId, AnnotationType annotationType) {
    ParticipantCohortAnnotation participantCohortAnnotation =
        new ParticipantCohortAnnotation()
            .cohortReviewId(cohortReviewId)
            .participantId(participantId);
    switch (annotationType) {
      case STRING:
        return participantCohortAnnotation.cohortAnnotationDefinitionId(
            stringAnnotationDefinition.getCohortAnnotationDefinitionId());
      case BOOLEAN:
        return participantCohortAnnotation.cohortAnnotationDefinitionId(
            booleanAnnotationDefinition.getCohortAnnotationDefinitionId());
      case INTEGER:
        return participantCohortAnnotation.cohortAnnotationDefinitionId(
            integerAnnotationDefinition.getCohortAnnotationDefinitionId());
      case ENUM:
        return participantCohortAnnotation.cohortAnnotationDefinitionId(
            enumAnnotationDefinition.getCohortAnnotationDefinitionId());
      case DATE:
        return participantCohortAnnotation.cohortAnnotationDefinitionId(
            dateAnnotationDefinition.getCohortAnnotationDefinitionId());
      default:
    }
    return participantCohortAnnotation;
  }

  private ParticipantCohortAnnotation buildValidParticipantCohortAnnotationForType(
      Long cohortReviewId, Long participantId, AnnotationType annotationType) {
    ParticipantCohortAnnotation participantCohortAnnotation =
        buildNoValueParticipantCohortAnnotationForType(
            cohortReviewId, participantId, annotationType);
    switch (annotationType) {
      case STRING:
        return participantCohortAnnotation.annotationValueString("test");
      case BOOLEAN:
        return participantCohortAnnotation.annotationValueBoolean(true);
      case INTEGER:
        return participantCohortAnnotation.annotationValueInteger(1);
      case ENUM:
        return participantCohortAnnotation.annotationValueEnum("test");
      case DATE:
        return participantCohortAnnotation.annotationValueDate("2022-02-21");
      default:
    }
    return participantCohortAnnotation;
  }

  private void stubBigQueryDemoChartData() {
    Field name = Field.of("name", LegacySQLTypeName.STRING);
    Field race = Field.of("race", LegacySQLTypeName.STRING);
    Field ageRange = Field.of("ageRange", LegacySQLTypeName.STRING);
    Field count = Field.of("count", LegacySQLTypeName.INTEGER);

    Schema schema = Schema.of(name, race, ageRange, count);

    FieldValue nameValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "Male");
    FieldValue raceValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "Hispanic");
    FieldValue ageRangeValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "44-60");
    FieldValue countValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "10");

    List<FieldValueList> tableRows =
        Collections.singletonList(
            FieldValueList.of(Arrays.asList(nameValue, raceValue, ageRangeValue, countValue)));
    TableResult result =
        new TableResult(schema, tableRows.size(), new PageImpl<>(() -> null, null, tableRows));

    when(bigQueryService.filterBigQueryConfigAndExecuteQuery(any())).thenReturn(result);
  }

  private void stubBigQueryParticipantChartData() {
    // construct the first TableResult call
    Field standardName = Field.of("standardName", LegacySQLTypeName.STRING);
    Field standardVocabulary = Field.of("standardVocabulary", LegacySQLTypeName.STRING);
    Field startDate = Field.of("startDate", LegacySQLTypeName.DATETIME);
    Field ageAtEvent = Field.of("ageAtEvent", LegacySQLTypeName.INTEGER);
    Field rank = Field.of("rank", LegacySQLTypeName.INTEGER);
    Schema schema = Schema.of(standardName, standardVocabulary, startDate, ageAtEvent, rank);
    FieldValue standardNameValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "standardName");
    FieldValue standardVocabularyValue =
        FieldValue.of(FieldValue.Attribute.PRIMITIVE, "standardVocabulary");
    FieldValue startDateValue =
        FieldValue.of(FieldValue.Attribute.PRIMITIVE, "1944-01-12 00:00:00 UTC");
    FieldValue ageAtEventValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "3");
    FieldValue rankValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "1");
    List<FieldValueList> tableRows =
        Collections.singletonList(
            FieldValueList.of(
                Arrays.asList(
                    standardNameValue,
                    standardVocabularyValue,
                    startDateValue,
                    ageAtEventValue,
                    rankValue)));
    TableResult result =
        new TableResult(schema, tableRows.size(), new PageImpl<>(() -> null, null, tableRows));

    // return the TableResult calls in order of call
    when(bigQueryService.filterBigQueryConfigAndExecuteQuery(any())).thenReturn(result);
  }

  private void stubBigQueryVocabulary() {
    // construct the first TableResult call
    Field type = Field.of("type", LegacySQLTypeName.STRING);
    Field domain = Field.of("domain", LegacySQLTypeName.STRING);
    Field vocabulary = Field.of("vocabulary", LegacySQLTypeName.STRING);
    Schema schema = Schema.of(type, domain, vocabulary);
    FieldValue typeValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "type");
    FieldValue domainValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "domain");
    FieldValue vocabularyValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "vocabulary");
    List<FieldValueList> tableRows =
        Collections.singletonList(
            FieldValueList.of(Arrays.asList(typeValue, domainValue, vocabularyValue)));
    TableResult result =
        new TableResult(schema, tableRows.size(), new PageImpl<>(() -> null, null, tableRows));

    // return the TableResult calls in order of call
    when(bigQueryService.filterBigQueryConfigAndExecuteQuery(any())).thenReturn(result);
  }

  private void stubBigQueryParticipantCount() {
    // construct the first TableResult call
    Field count = Field.of("count", LegacySQLTypeName.INTEGER);
    Field birthDatetime = Field.of("birth_datetime", LegacySQLTypeName.STRING);

    Schema schema = Schema.of(count, birthDatetime);
    FieldValue countValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "0");
    FieldValue birthDatetimeValue =
        FieldValue.of(FieldValue.Attribute.PRIMITIVE, String.valueOf(NOW.getEpochSecond()));
    List<FieldValueList> tableRows =
        Collections.singletonList(FieldValueList.of(Arrays.asList(countValue, birthDatetimeValue)));

    TableResult result =
        new TableResult(schema, tableRows.size(), new PageImpl<>(() -> null, null, tableRows));

    // return the TableResult calls in order of call
    when(bigQueryService.filterBigQueryConfigAndExecuteQuery(any())).thenReturn(result);
  }

  private void stubBigQueryParticipantData() {
    // construct the first TableResult call
    Field startDatetime = Field.of("START_DATETIME", LegacySQLTypeName.DATETIME);
    Field domain = Field.of("DOMAIN", LegacySQLTypeName.STRING);
    Field standardName = Field.of("STANDARD_NAME", LegacySQLTypeName.STRING);
    Field ageAtEvent = Field.of("AGE_AT_EVENT", LegacySQLTypeName.INTEGER);
    Field standardConceptId = Field.of("STANDARD_CONCEPT_ID", LegacySQLTypeName.INTEGER);
    Field sourceConceptId = Field.of("SOURCE_CONCEPT_ID", LegacySQLTypeName.INTEGER);
    Field standardVocabulary = Field.of("STANDARD_VOCABULARY", LegacySQLTypeName.STRING);
    Field sourceVocabulary = Field.of("SOURCE_VOCABULARY", LegacySQLTypeName.STRING);
    Field sourceName = Field.of("SOURCE_NAME", LegacySQLTypeName.STRING);
    Field sourceCode = Field.of("SOURCE_CODE", LegacySQLTypeName.STRING);
    Field standardCode = Field.of("STANDARD_CODE", LegacySQLTypeName.STRING);
    Field valueAsNumber = Field.of("VALUE_AS_NUMBER", LegacySQLTypeName.STRING);
    Field visitType = Field.of("VISIT_TYPE", LegacySQLTypeName.STRING);
    Field numMentions = Field.of("NUM_MENTIONS", LegacySQLTypeName.STRING);
    Field firstMention = Field.of("FIRST_MENTION", LegacySQLTypeName.STRING);
    Field lastMention = Field.of("LAST_MENTION", LegacySQLTypeName.STRING);
    Field unit = Field.of("UNIT", LegacySQLTypeName.STRING);
    Field dose = Field.of("DOSE", LegacySQLTypeName.STRING);
    Field strength = Field.of("STRENGTH", LegacySQLTypeName.STRING);
    Field route = Field.of("ROUTE", LegacySQLTypeName.STRING);
    Field refRange = Field.of("REF_RANGE", LegacySQLTypeName.STRING);
    Schema schema =
        Schema.of(
            startDatetime,
            domain,
            standardName,
            ageAtEvent,
            standardConceptId,
            sourceConceptId,
            standardVocabulary,
            sourceVocabulary,
            sourceName,
            sourceCode,
            standardCode,
            valueAsNumber,
            visitType,
            numMentions,
            firstMention,
            lastMention,
            unit,
            dose,
            strength,
            route,
            refRange);
    FieldValue startDatetimeValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "1");
    FieldValue domainValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "domain");
    FieldValue standardNameValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "standardName");
    FieldValue ageAtEventValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "1");
    FieldValue standardConceptIdValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "1");
    FieldValue sourceConceptIdValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "2");
    FieldValue standardVocabularyValue =
        FieldValue.of(FieldValue.Attribute.PRIMITIVE, "standardVocabulary");
    FieldValue sourceVocabularyValue =
        FieldValue.of(FieldValue.Attribute.PRIMITIVE, "sourceVocabulary");
    FieldValue sourceNameValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "sourceName");
    FieldValue sourceCodeValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "sourceCode");
    FieldValue standardCodeValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "standardCode");
    FieldValue valueAsNumberValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "valueAsNumber");
    FieldValue visitTypeValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "visitType");
    FieldValue numMentionsValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "3");
    FieldValue firstMentionValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "3");
    FieldValue lastMentionValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "3");
    FieldValue unitValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "1");
    FieldValue doseValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "2");
    FieldValue strengthValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "3");
    FieldValue routeValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "4");
    FieldValue refRangeValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "5");
    List<FieldValueList> tableRows =
        Collections.singletonList(
            FieldValueList.of(
                Arrays.asList(
                    startDatetimeValue,
                    domainValue,
                    standardNameValue,
                    ageAtEventValue,
                    standardConceptIdValue,
                    sourceConceptIdValue,
                    standardVocabularyValue,
                    sourceVocabularyValue,
                    sourceNameValue,
                    sourceCodeValue,
                    standardCodeValue,
                    valueAsNumberValue,
                    visitTypeValue,
                    numMentionsValue,
                    firstMentionValue,
                    lastMentionValue,
                    unitValue,
                    doseValue,
                    strengthValue,
                    routeValue,
                    refRangeValue)));
    TableResult result =
        new TableResult(schema, tableRows.size(), new PageImpl<>(() -> null, null, tableRows));

    // return the TableResult calls in order of call
    when(bigQueryService.filterBigQueryConfigAndExecuteQuery(any())).thenReturn(result);
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

  private DbParticipantCohortAnnotation saveTestParticipantCohortAnnotation() {
    return participantCohortAnnotationDao.save(
        new DbParticipantCohortAnnotation()
            .cohortReviewId(cohortReview.getCohortReviewId())
            .participantId(participantCohortStatus1.getParticipantKey().getParticipantId())
            .annotationValueString("test")
            .cohortAnnotationDefinitionId(
                stringAnnotationDefinition.getCohortAnnotationDefinitionId()));
  }

  private void deleteExistingParticipantCohortAnnotations() {
    // to be called before testing for createParticipantAnnotation
    participantCohortAnnotationDao.delete(participantAnnotation);
    participantCohortAnnotationDao.delete(participantAnnotationDate);
  }

  private ParticipantCohortStatus dbParticipantCohortStatusToApi(
      DbParticipantCohortStatus dbStatus) {
    Table<Long, CriteriaType, String> demoTable = TestConcepts.asMap();
    return new ParticipantCohortStatus()
        .birthDate(dbStatus.getBirthDate().toString())
        .ethnicityConceptId(dbStatus.getEthnicityConceptId())
        .ethnicity(demoTable.get(dbStatus.getEthnicityConceptId(), CriteriaType.ETHNICITY))
        .genderConceptId(dbStatus.getGenderConceptId())
        .gender(demoTable.get(dbStatus.getGenderConceptId(), CriteriaType.GENDER))
        .participantId(dbStatus.getParticipantKey().getParticipantId())
        .raceConceptId(dbStatus.getRaceConceptId())
        .race(demoTable.get(dbStatus.getRaceConceptId(), CriteriaType.RACE))
        .sexAtBirthConceptId(dbStatus.getSexAtBirthConceptId())
        .sexAtBirth(demoTable.get(dbStatus.getSexAtBirthConceptId(), CriteriaType.SEX))
        .status(DbStorageEnums.cohortStatusFromStorage(dbStatus.getStatus()))
        .deceased(dbStatus.getDeceased());
  }

  private void stubBigQueryCreateCohortReview() {
    // construct the first TableResult call
    Field count = Field.of("count", LegacySQLTypeName.INTEGER);
    Schema schema = Schema.of(count);
    FieldValue countValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "1");
    List<FieldValueList> tableRows =
        Collections.singletonList(FieldValueList.of(Collections.singletonList(countValue)));
    TableResult result =
        new TableResult(schema, tableRows.size(), new PageImpl<>(() -> null, null, tableRows));

    // construct the second TableResult call
    Field personId = Field.of("person_id", LegacySQLTypeName.STRING);
    Field birthDatetime = Field.of("birth_datetime", LegacySQLTypeName.DATETIME);
    Field genderConceptId = Field.of("gender_concept_id", LegacySQLTypeName.INTEGER);
    Field raceConceptId = Field.of("race_concept_id", LegacySQLTypeName.INTEGER);
    Field ethnicityConceptId = Field.of("ethnicity_concept_id", LegacySQLTypeName.INTEGER);
    Field sexAtBirthConceptId = Field.of("sex_at_birth_concept_id", LegacySQLTypeName.INTEGER);
    Field deceased = Field.of("deceased", LegacySQLTypeName.BOOLEAN);
    Schema schema2 =
        Schema.of(
            personId,
            birthDatetime,
            genderConceptId,
            raceConceptId,
            ethnicityConceptId,
            sexAtBirthConceptId,
            deceased);
    FieldValue personIdValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "1");
    FieldValue birthDatetimeValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "1");
    FieldValue genderConceptIdValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "2");
    FieldValue raceConceptIdValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "3");
    FieldValue ethnicityConceptIdValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "4");
    FieldValue sexAtBirthConceptIdValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "5");
    FieldValue deceasedValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "false");
    List<FieldValueList> tableRows2 =
        Collections.singletonList(
            FieldValueList.of(
                Arrays.asList(
                    personIdValue,
                    birthDatetimeValue,
                    genderConceptIdValue,
                    raceConceptIdValue,
                    ethnicityConceptIdValue,
                    sexAtBirthConceptIdValue,
                    deceasedValue)));
    TableResult result2 =
        new TableResult(schema2, tableRows2.size(), new PageImpl<>(() -> null, null, tableRows2));

    // return the TableResult calls in order of call
    when(bigQueryService.filterBigQueryConfigAndExecuteQuery(any())).thenReturn(result, result2);
  }

  private void stubWorkspaceAccessLevel(
      Workspace workspace, WorkspaceAccessLevel workspaceAccessLevel) {
    stubGetWorkspace(workspace.getNamespace(), workspace.getName(), workspaceAccessLevel);
    stubGetWorkspaceAcl(workspace.getNamespace(), workspace.getName(), workspaceAccessLevel);
  }

  private void stubGetWorkspace(String ns, String name, WorkspaceAccessLevel workspaceAccessLevel) {
    FirecloudWorkspaceDetails fcWorkspace = new FirecloudWorkspaceDetails();
    fcWorkspace.setNamespace(ns);
    fcWorkspace.setName(name);
    fcWorkspace.setCreatedBy(USER_EMAIL);
    FirecloudWorkspaceResponse fcResponse = new FirecloudWorkspaceResponse();
    fcResponse.setWorkspace(fcWorkspace);
    fcResponse.setAccessLevel(workspaceAccessLevel.toString());
    when(fireCloudService.getWorkspace(ns, name)).thenReturn(fcResponse);
  }

  private void stubGetWorkspaceAcl(
      String ns, String name, WorkspaceAccessLevel workspaceAccessLevel) {
    FirecloudWorkspaceACL workspaceAccessLevelResponse = new FirecloudWorkspaceACL();
    FirecloudWorkspaceAccessEntry accessLevelEntry =
        new FirecloudWorkspaceAccessEntry().accessLevel(workspaceAccessLevel.toString());
    Map<String, FirecloudWorkspaceAccessEntry> userEmailToAccessEntry =
        ImmutableMap.of(USER_EMAIL, accessLevelEntry);
    workspaceAccessLevelResponse.setAcl(userEmailToAccessEntry);
    when(fireCloudService.getWorkspaceAclAsService(ns, name))
        .thenReturn(workspaceAccessLevelResponse);
  }

  private Workspace createTestWorkspace(
      String workspaceNamespace, String workspaceName, long cdrVersionId) {
    Workspace tmpWorkspace = new Workspace();
    tmpWorkspace.setName(workspaceName);
    tmpWorkspace.setNamespace(workspaceNamespace);
    tmpWorkspace.setResearchPurpose(new ResearchPurpose());
    tmpWorkspace.setCdrVersionId(String.valueOf(cdrVersionId));
    tmpWorkspace.setBillingAccountName("billing-account");

    TestMockFactory.stubCreateFcWorkspace(fireCloudService);

    tmpWorkspace = workspacesController.createWorkspace(tmpWorkspace).getBody();
    stubWorkspaceAccessLevel(Objects.requireNonNull(tmpWorkspace), WorkspaceAccessLevel.OWNER);

    return tmpWorkspace;
  }
}
