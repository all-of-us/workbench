package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.bitbucket.radistao.test.runner.BeforeAfterSpringTestRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.actionaudit.auditors.BillingProjectAuditor;
import org.pmiops.workbench.billing.FreeTierBillingService;
import org.pmiops.workbench.cohortbuilder.CohortBuilderService;
import org.pmiops.workbench.cohortbuilder.CohortQueryBuilder;
import org.pmiops.workbench.cohortbuilder.SearchGroupItemQueryBuilder;
import org.pmiops.workbench.cohortreview.CohortReviewServiceImpl;
import org.pmiops.workbench.cohortreview.ReviewQueryBuilder;
import org.pmiops.workbench.cohortreview.mapper.CohortReviewMapperImpl;
import org.pmiops.workbench.cohortreview.mapper.ParticipantCohortAnnotationMapperImpl;
import org.pmiops.workbench.cohortreview.mapper.ParticipantCohortStatusMapperImpl;
import org.pmiops.workbench.cohorts.CohortCloningService;
import org.pmiops.workbench.cohorts.CohortFactory;
import org.pmiops.workbench.cohorts.CohortMapperImpl;
import org.pmiops.workbench.cohorts.CohortService;
import org.pmiops.workbench.conceptset.ConceptSetService;
import org.pmiops.workbench.conceptset.mapper.ConceptSetMapperImpl;
import org.pmiops.workbench.dataset.DataSetService;
import org.pmiops.workbench.dataset.mapper.DataSetMapperImpl;
import org.pmiops.workbench.db.dao.AccessTierDao;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.CohortReviewDao;
import org.pmiops.workbench.db.dao.ParticipantCohortStatusDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserRecentResourceService;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbCohortReview;
import org.pmiops.workbench.db.model.DbParticipantCohortStatus;
import org.pmiops.workbench.db.model.DbParticipantCohortStatusKey;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceACL;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceAccessEntry;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceResponse;
import org.pmiops.workbench.model.CohortChartData;
import org.pmiops.workbench.model.CohortChartDataListResponse;
import org.pmiops.workbench.model.CohortReview;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.EmailVerificationStatus;
import org.pmiops.workbench.model.Filter;
import org.pmiops.workbench.model.FilterColumns;
import org.pmiops.workbench.model.FilterList;
import org.pmiops.workbench.model.Operator;
import org.pmiops.workbench.model.PageFilterRequest;
import org.pmiops.workbench.model.ParticipantChartData;
import org.pmiops.workbench.model.ParticipantChartDataListResponse;
import org.pmiops.workbench.model.ParticipantData;
import org.pmiops.workbench.model.ParticipantDataCountResponse;
import org.pmiops.workbench.model.ParticipantDataListResponse;
import org.pmiops.workbench.model.SortOrder;
import org.pmiops.workbench.model.Vocabulary;
import org.pmiops.workbench.model.VocabularyListResponse;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.test.SearchRequests;
import org.pmiops.workbench.testconfig.TestJpaConfig;
import org.pmiops.workbench.testconfig.TestWorkbenchConfig;
import org.pmiops.workbench.utils.TestMockFactory;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.utils.mappers.FirecloudMapperImpl;
import org.pmiops.workbench.utils.mappers.UserMapperImpl;
import org.pmiops.workbench.utils.mappers.WorkspaceMapperImpl;
import org.pmiops.workbench.workspaces.WorkspaceAuthService;
import org.pmiops.workbench.workspaces.WorkspaceServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;

@RunWith(BeforeAfterSpringTestRunner.class)
@Import({TestJpaConfig.class, CohortReviewController.class})
public class CohortReviewControllerBQTest extends BigQueryBaseTest {

  @TestConfiguration
  @Import({
    TestJpaConfig.class,
    CohortReviewController.class,
    BigQueryTestService.class,
    CohortCloningService.class,
    CohortMapperImpl.class,
    CohortQueryBuilder.class,
    CohortReviewMapperImpl.class,
    CohortReviewMapperImpl.class,
    CohortReviewServiceImpl.class,
    CommonMappers.class,
    CommonMappers.class,
    ConceptSetMapperImpl.class,
    DataSetMapperImpl.class,
    FirecloudMapperImpl.class,
    ParticipantCohortAnnotationMapperImpl.class,
    ParticipantCohortStatusMapperImpl.class,
    ReviewQueryBuilder.class,
    SearchGroupItemQueryBuilder.class,
    UserMapperImpl.class,
    WorkspaceMapperImpl.class,
    WorkspaceServiceImpl.class,
    WorkspaceAuthService.class
  })
  @MockBean({
    BillingProjectAuditor.class,
    CohortBuilderService.class,
    CohortFactory.class,
    CohortService.class,
    ConceptSetService.class,
    DataSetService.class,
    FireCloudService.class,
    FreeTierBillingService.class,
    UserRecentResourceService.class,
  })
  static class Configuration {

    @Bean
    @Scope("prototype")
    DbUser user() {
      return currentUser;
    }

    @Bean
    public Clock clock() {
      return CLOCK;
    }
  }

  private static final String NAMESPACE = "aou-test";
  private static final String NAME = "test";
  private static final Long PARTICIPANT_ID = 102246L;
  private static final Long PARTICIPANT_ID2 = 102247L;
  private static final FakeClock CLOCK = new FakeClock(Instant.now(), ZoneId.systemDefault());
  private DbCdrVersion cdrVersion;
  private DbWorkspace workspace;

  @Autowired private CohortReviewController controller;

  @Autowired private TestWorkbenchConfig testWorkbenchConfig;

  @Autowired private CohortDao cohortDao;

  @Autowired private CohortReviewDao cohortReviewDao;

  @Autowired private WorkspaceDao workspaceDao;

  @Autowired private CdrVersionDao cdrVersionDao;

  @Autowired private ParticipantCohortStatusDao participantCohortStatusDao;

  @Autowired private FireCloudService mockFireCloudService;

  @Autowired private UserDao userDao;

  @Autowired private AccessTierDao accessTierDao;

  private DbCohortReview reviewWithoutEHRData;
  private DbCohortReview reviewWithEHRData;
  private static DbUser currentUser;

  @Override
  public List<String> getTableNames() {
    return ImmutableList.of(
        "cb_review_all_events", "person", "cb_search_person", "cb_search_all_events", "death");
  }

  @Override
  public String getTestDataDirectory() {
    return CB_DATA;
  }

  @Before
  public void setUp() {
    DbUser dbUser = new DbUser();
    dbUser.setUsername("bob@gmail.com");
    dbUser.setUserId(123L);
    dbUser.setDisabled(false);
    dbUser.setEmailVerificationStatusEnum(EmailVerificationStatus.SUBSCRIBED);
    dbUser = userDao.save(dbUser);
    currentUser = dbUser;

    when(mockFireCloudService.getWorkspaceAclAsService(anyString(), anyString()))
        .thenReturn(
            new FirecloudWorkspaceACL()
                .acl(
                    ImmutableMap.of(
                        currentUser.getUsername(),
                        new FirecloudWorkspaceAccessEntry().accessLevel("OWNER"))));

    cdrVersion = TestMockFactory.createDefaultCdrVersion(cdrVersionDao, accessTierDao);
    cdrVersion.setBigqueryDataset(testWorkbenchConfig.bigquery.dataSetId);
    cdrVersion.setBigqueryProject(testWorkbenchConfig.bigquery.projectId);
    cdrVersion = cdrVersionDao.save(cdrVersion);

    workspace = new DbWorkspace();
    workspace.setCdrVersion(cdrVersion);
    workspace.setWorkspaceNamespace(NAMESPACE);
    workspace.setFirecloudName(NAME);
    workspace = workspaceDao.save(workspace);
    stubMockFirecloudGetWorkspace();
    stubMockFirecloudGetWorkspaceAcl();

    Gson gson = new Gson();
    DbCohort cohortWithoutEHRData = new DbCohort();
    cohortWithoutEHRData.setWorkspaceId(workspace.getWorkspaceId());
    cohortWithoutEHRData.setCriteria(gson.toJson(SearchRequests.males()));
    cohortWithoutEHRData = cohortDao.save(cohortWithoutEHRData);

    DbCohort cohortWithEHRData = new DbCohort();
    cohortWithEHRData.setWorkspaceId(workspace.getWorkspaceId());
    cohortWithEHRData.setCriteria(gson.toJson(SearchRequests.malesWithEHRData()));
    cohortWithEHRData = cohortDao.save(cohortWithEHRData);

    reviewWithoutEHRData = createCohortReview(cohortWithoutEHRData);

    saveParticipantCohortStatus(PARTICIPANT_ID, reviewWithoutEHRData.getCohortReviewId());
    saveParticipantCohortStatus(PARTICIPANT_ID2, reviewWithoutEHRData.getCohortReviewId());

    reviewWithEHRData = createCohortReview(cohortWithEHRData);

    saveParticipantCohortStatus(PARTICIPANT_ID, reviewWithEHRData.getCohortReviewId());
    saveParticipantCohortStatus(PARTICIPANT_ID2, reviewWithEHRData.getCohortReviewId());
  }

  @After
  public void tearDown() {
    workspaceDao.deleteById(workspace.getWorkspaceId());
    cdrVersionDao.deleteById(cdrVersion.getCdrVersionId());
  }

  private static ParticipantData expectedAllEvents1() {
    return new ParticipantData()
        .domain("Condition")
        .standardVocabulary("SNOMED")
        .standardCode("002")
        .sourceCode("0020")
        .sourceVocabulary("ICD9CM")
        .sourceName("Typhoid and paratyphoid fevers")
        .route("route")
        .dose("1.0")
        .strength("str")
        .unit("unit")
        .refRange("range")
        .numMentions("2")
        .firstMention("2008-07-22 05:00:00 UTC")
        .lastMention("2008-07-22 05:00:00 UTC")
        .visitType("visit")
        .value("1.0")
        .itemDate("2008-07-22 05:00:00 UTC")
        .standardName("SNOMED")
        .ageAtEvent(28)
        .standardConceptId(1L)
        .sourceConceptId(1L);
  }

  private static ParticipantData expectedAllEvents2() {
    return new ParticipantData()
        .domain("Condition")
        .standardVocabulary("SNOMED")
        .standardCode("002")
        .sourceCode("0021")
        .sourceVocabulary("ICD9CM")
        .sourceName("Typhoid and paratyphoid fevers")
        .route("route")
        .dose("1.0")
        .strength("str")
        .unit("unit")
        .refRange("range")
        .numMentions("2")
        .firstMention("2008-08-01 05:00:00 UTC")
        .lastMention("2008-08-01 05:00:00 UTC")
        .visitType("visit")
        .value("1.0")
        .itemDate("2008-08-01 05:00:00 UTC")
        .standardName("SNOMED")
        .ageAtEvent(28)
        .standardConceptId(1L)
        .sourceConceptId(1L);
  }

  private static ParticipantData expectedCondition1() {
    return new ParticipantData()
        .domain(Domain.CONDITION.toString())
        .value("1.0")
        .visitType("visit")
        .standardVocabulary("SNOMED")
        .standardCode("002")
        .sourceCode("0020")
        .sourceVocabulary("ICD9CM")
        .sourceName("Typhoid and paratyphoid fevers")
        .standardName("Typhoid and paratyphoid fevers")
        .itemDate("2008-07-22 05:00:00 UTC")
        .ageAtEvent(28)
        .standardConceptId(1L)
        .sourceConceptId(1L)
        .numMentions("2")
        .firstMention("2008-07-22 05:00:00 UTC")
        .lastMention("2008-07-22 05:00:00 UTC")
        .unit("unit")
        .dose("1.0")
        .strength("str")
        .route("route")
        .refRange("range");
  }

  private static ParticipantData expectedCondition2() {
    return new ParticipantData()
        .domain(Domain.CONDITION.toString())
        .value("1.0")
        .visitType("visit")
        .standardVocabulary("SNOMED")
        .standardCode("002")
        .sourceCode("0021")
        .sourceVocabulary("ICD9CM")
        .sourceName("Typhoid and paratyphoid fevers")
        .standardName("Typhoid and paratyphoid fevers")
        .itemDate("2008-08-01 05:00:00 UTC")
        .ageAtEvent(28)
        .standardConceptId(1L)
        .sourceConceptId(1L)
        .numMentions("2")
        .firstMention("2008-08-01 05:00:00 UTC")
        .lastMention("2008-08-01 05:00:00 UTC")
        .unit("unit")
        .dose("1.0")
        .strength("str")
        .route("route")
        .refRange("range");
  }

  @Test
  public void getCohortReviewsInWorkspace() {
    CohortReview expectedReview =
        new CohortReview()
            .cohortReviewId(reviewWithoutEHRData.getCohortReviewId())
            .reviewSize(reviewWithoutEHRData.getReviewSize())
            .reviewStatus(reviewWithoutEHRData.getReviewStatusEnum())
            .cdrVersionId(reviewWithoutEHRData.getCdrVersionId())
            .cohortDefinition(reviewWithoutEHRData.getCohortDefinition())
            .cohortName(reviewWithoutEHRData.getCohortName())
            .cohortId(reviewWithoutEHRData.getCohortId())
            .creationTime(reviewWithoutEHRData.getCreationTime().getTime())
            .lastModifiedTime(reviewWithoutEHRData.getLastModifiedTime().getTime())
            .matchedParticipantCount(reviewWithoutEHRData.getMatchedParticipantCount())
            .reviewedCount(reviewWithoutEHRData.getReviewedCount())
            .etag(Etags.fromVersion(reviewWithoutEHRData.getVersion()));
    assertThat(controller.getCohortReviewsInWorkspace(NAMESPACE, NAME).getBody().getItems().get(0))
        .isEqualTo(expectedReview);
  }

  @Test
  public void getParticipantConditionsSorting() {
    PageFilterRequest testFilter = new PageFilterRequest().domain(Domain.CONDITION);

    // no sort order or column
    ParticipantDataListResponse response =
        controller
            .getParticipantData(
                NAMESPACE,
                NAME,
                reviewWithoutEHRData.getCohortReviewId(),
                PARTICIPANT_ID,
                testFilter)
            .getBody();

    assertResponse(response, ImmutableList.of(expectedCondition1(), expectedCondition2()));

    // added sort order
    testFilter.sortOrder(SortOrder.DESC);
    response =
        controller
            .getParticipantData(
                NAMESPACE,
                NAME,
                reviewWithoutEHRData.getCohortReviewId(),
                PARTICIPANT_ID,
                testFilter)
            .getBody();

    assertResponse(response, ImmutableList.of(expectedCondition2(), expectedCondition1()));
  }

  @Test
  public void getParticipantConditionsCount() {
    PageFilterRequest testFilter = new PageFilterRequest().domain(Domain.CONDITION);

    // no sort order or column
    ParticipantDataCountResponse response =
        controller
            .getParticipantCount(
                NAMESPACE,
                NAME,
                reviewWithoutEHRData.getCohortReviewId(),
                PARTICIPANT_ID,
                testFilter)
            .getBody();
    assertThat(response.getCount()).isEqualTo(2);
  }

  @Test
  public void getParticipantConditionsFiltering() {
    List<Filter> filters =
        ImmutableList.of(
            new Filter()
                .operator(Operator.LIKE)
                .property(FilterColumns.STANDARD_NAME)
                .values(ImmutableList.of("typhoid")),
            new Filter()
                .operator(Operator.BETWEEN)
                .property(FilterColumns.AGE_AT_EVENT)
                .values(ImmutableList.of("20", "30")),
            new Filter()
                .operator(Operator.IN)
                .property(FilterColumns.STANDARD_VOCABULARY)
                .values(ImmutableList.of("ICD9CM", "SNOMED")));
    PageFilterRequest testFilter =
        new PageFilterRequest().domain(Domain.CONDITION).filters(new FilterList().items(filters));

    // no sort order or column
    ParticipantDataListResponse response =
        controller
            .getParticipantData(
                NAMESPACE,
                NAME,
                reviewWithoutEHRData.getCohortReviewId(),
                PARTICIPANT_ID,
                testFilter)
            .getBody();

    assertResponse(response, ImmutableList.of(expectedCondition1(), expectedCondition2()));

    // added sort order
    testFilter.sortOrder(SortOrder.DESC);
    response =
        controller
            .getParticipantData(
                NAMESPACE,
                NAME,
                reviewWithoutEHRData.getCohortReviewId(),
                PARTICIPANT_ID,
                testFilter)
            .getBody();

    assertResponse(response, ImmutableList.of(expectedCondition2(), expectedCondition1()));
  }

  @Test
  public void getParticipantConditionsPagination() {
    stubMockFirecloudGetWorkspace();

    PageFilterRequest testFilter =
        new PageFilterRequest().domain(Domain.CONDITION).page(0).pageSize(1);

    // page 1 should have 1 item
    ParticipantDataListResponse response =
        controller
            .getParticipantData(
                NAMESPACE,
                NAME,
                reviewWithoutEHRData.getCohortReviewId(),
                PARTICIPANT_ID,
                testFilter)
            .getBody();

    assertResponse(response, ImmutableList.of(expectedCondition1()));

    // page 2 should have 1 item
    testFilter.page(1);
    response =
        controller
            .getParticipantData(
                NAMESPACE,
                NAME,
                reviewWithoutEHRData.getCohortReviewId(),
                PARTICIPANT_ID,
                testFilter)
            .getBody();
    assertResponse(response, ImmutableList.of(expectedCondition2()));
  }

  @Test
  public void getParticipantAllEventsPagination() {
    PageFilterRequest testFilter =
        new PageFilterRequest().domain(Domain.ALL_EVENTS).page(0).pageSize(1);

    // page 1 should have 1 item
    ParticipantDataListResponse response =
        controller
            .getParticipantData(
                NAMESPACE,
                NAME,
                reviewWithoutEHRData.getCohortReviewId(),
                PARTICIPANT_ID2,
                testFilter)
            .getBody();

    assertResponse(response, ImmutableList.of(expectedAllEvents1()));

    // page 2 should have 1 item
    testFilter.page(1);
    response =
        controller
            .getParticipantData(
                NAMESPACE,
                NAME,
                reviewWithoutEHRData.getCohortReviewId(),
                PARTICIPANT_ID2,
                testFilter)
            .getBody();

    assertResponse(response, ImmutableList.of(expectedAllEvents2()));
  }

  @Test
  public void getParticipantAllEventsSorting() {
    PageFilterRequest testFilter = new PageFilterRequest().domain(Domain.ALL_EVENTS);

    // no sort order or column
    ParticipantDataListResponse response =
        controller
            .getParticipantData(
                NAMESPACE,
                NAME,
                reviewWithoutEHRData.getCohortReviewId(),
                PARTICIPANT_ID2,
                testFilter)
            .getBody();

    assertResponse(response, ImmutableList.of(expectedAllEvents1(), expectedAllEvents2()));

    // added sort order
    testFilter.sortOrder(SortOrder.DESC);
    response =
        controller
            .getParticipantData(
                NAMESPACE,
                NAME,
                reviewWithoutEHRData.getCohortReviewId(),
                PARTICIPANT_ID2,
                testFilter)
            .getBody();

    assertResponse(response, ImmutableList.of(expectedAllEvents2(), expectedAllEvents1()));
  }

  @Test
  public void getParticipantChartData() {
    ParticipantChartDataListResponse response =
        controller
            .getParticipantChartData(
                NAMESPACE,
                NAME,
                reviewWithoutEHRData.getCohortReviewId(),
                PARTICIPANT_ID,
                Domain.CONDITION.name(),
                null)
            .getBody();

    ParticipantChartData expectedData1 =
        new ParticipantChartData()
            .ageAtEvent(28)
            .rank(1)
            .standardName("Typhoid and paratyphoid fevers")
            .standardVocabulary("SNOMED")
            .startDate("2008-07-22");
    ParticipantChartData expectedData2 =
        new ParticipantChartData()
            .ageAtEvent(28)
            .rank(1)
            .standardName("Typhoid and paratyphoid fevers")
            .standardVocabulary("SNOMED")
            .startDate("2008-08-01");
    assertThat(response.getItems().size()).isEqualTo(2);
    assertThat(expectedData1).isIn(response.getItems());
    assertThat(expectedData2).isIn(response.getItems());
  }

  @Test
  public void getParticipantChartDataBadLimit() {
    try {
      controller.getParticipantChartData(
          NAMESPACE,
          NAME,
          reviewWithoutEHRData.getCohortReviewId(),
          PARTICIPANT_ID,
          Domain.CONDITION.name(),
          -1);
      fail("Should have thrown a BadRequestException!");
    } catch (BadRequestException bre) {
      // Success
      assertThat(bre.getMessage())
          .isEqualTo("Bad Request: Please provide a chart limit between 1 and 20.");
    }
  }

  @Test
  public void getParticipantChartDataBadLimitOverHundred() {
    try {
      controller.getParticipantChartData(
          NAMESPACE,
          NAME,
          reviewWithoutEHRData.getCohortReviewId(),
          PARTICIPANT_ID,
          Domain.CONDITION.name(),
          101);
      fail("Should have thrown a BadRequestException!");
    } catch (BadRequestException bre) {
      // Success
      assertThat(bre.getMessage())
          .isEqualTo("Bad Request: Please provide a chart limit between 1 and 20.");
    }
  }

  @Test
  public void getCohortChartDataBadLimit() {
    try {
      controller.getCohortChartData(
          NAMESPACE, NAME, reviewWithoutEHRData.getCohortReviewId(), Domain.CONDITION.name(), -1);
      fail("Should have thrown a BadRequestException!");
    } catch (BadRequestException bre) {
      // Success
      assertThat(bre.getMessage())
          .isEqualTo("Bad Request: Please provide a chart limit between 1 and 20.");
    }
  }

  @Test
  public void getCohortChartDataBadLimitOverHundred() {
    try {
      controller.getCohortChartData(
          NAMESPACE, NAME, reviewWithoutEHRData.getCohortReviewId(), Domain.CONDITION.name(), 101);
      fail("Should have thrown a BadRequestException!");
    } catch (BadRequestException bre) {
      // Success
      assertThat(bre.getMessage())
          .isEqualTo("Bad Request: Please provide a chart limit between 1 and 20.");
    }
  }

  @Test
  public void getCohortChartDataLab() {
    CohortChartDataListResponse response =
        controller
            .getCohortChartData(
                NAMESPACE, NAME, reviewWithoutEHRData.getCohortReviewId(), Domain.LAB.name(), 10)
            .getBody();
    assertThat(response.getItems().size()).isEqualTo(3);
    assertThat(response.getItems().get(0))
        .isEqualTo(new CohortChartData().name("name10").conceptId(10L).count(1L));
    assertThat(response.getItems().get(0))
        .isEqualTo(new CohortChartData().name("name10").conceptId(10L).count(1L));
    assertThat(response.getItems().get(1))
        .isEqualTo(new CohortChartData().name("name3").conceptId(3L).count(1L));
    assertThat(response.getItems().get(2))
        .isEqualTo(new CohortChartData().name("name9").conceptId(9L).count(1L));
  }

  @Test
  public void getCohortChartDataLabWithEHRData() {
    CohortChartDataListResponse response =
        controller
            .getCohortChartData(
                NAMESPACE, NAME, reviewWithEHRData.getCohortReviewId(), Domain.LAB.name(), 10)
            .getBody();
    assertThat(response.getItems().size()).isEqualTo(3);
    assertThat(response.getItems().get(0))
        .isEqualTo(new CohortChartData().name("name10").conceptId(10L).count(1L));
    assertThat(response.getItems().get(1))
        .isEqualTo(new CohortChartData().name("name3").conceptId(3L).count(1L));
    assertThat(response.getItems().get(2))
        .isEqualTo(new CohortChartData().name("name9").conceptId(9L).count(1L));
  }

  @Test
  public void getCohortChartDataDrug() {
    CohortChartDataListResponse response =
        controller
            .getCohortChartData(
                NAMESPACE, NAME, reviewWithoutEHRData.getCohortReviewId(), Domain.DRUG.name(), 10)
            .getBody();
    assertThat(response.getItems().size()).isEqualTo(1);
    assertThat(response.getItems().get(0))
        .isEqualTo(new CohortChartData().name("name11").conceptId(1L).count(1L));
  }

  @Test
  public void getCohortChartDataCondition() {
    CohortChartDataListResponse response =
        controller
            .getCohortChartData(
                NAMESPACE,
                NAME,
                reviewWithoutEHRData.getCohortReviewId(),
                Domain.CONDITION.name(),
                10)
            .getBody();
    assertThat(response.getItems().size()).isEqualTo(2);
    assertThat(response.getItems().get(0))
        .isEqualTo(new CohortChartData().name("name1").conceptId(1L).count(1L));
    assertThat(response.getItems().get(1))
        .isEqualTo(new CohortChartData().name("name7").conceptId(7L).count(1L));
  }

  @Test
  public void getCohortChartDataProcedure() {
    CohortChartDataListResponse response =
        controller
            .getCohortChartData(
                NAMESPACE,
                NAME,
                reviewWithoutEHRData.getCohortReviewId(),
                Domain.PROCEDURE.name(),
                10)
            .getBody();

    assertThat(response.getItems().size()).isEqualTo(3);
    assertThat(response.getItems().get(0))
        .isEqualTo(new CohortChartData().name("name2").conceptId(2L).count(1L));
    assertThat(response.getItems().get(1))
        .isEqualTo(new CohortChartData().name("name4").conceptId(4L).count(1L));
    assertThat(response.getItems().get(2))
        .isEqualTo(new CohortChartData().name("name8").conceptId(8L).count(1L));
  }

  @Test
  public void getVocabularies() {
    VocabularyListResponse response =
        controller
            .getVocabularies(NAMESPACE, NAME, reviewWithoutEHRData.getCohortReviewId())
            .getBody();
    assertThat(response.getItems().size()).isEqualTo(20);
    assertThat(response.getItems().get(0))
        .isEqualTo(new Vocabulary().type("Source").domain("ALL_EVENTS").vocabulary("CPT4"));
    assertThat(response.getItems().get(1))
        .isEqualTo(new Vocabulary().type("Source").domain("ALL_EVENTS").vocabulary("ICD10CM"));
    assertThat(response.getItems().get(2))
        .isEqualTo(new Vocabulary().type("Source").domain("ALL_EVENTS").vocabulary("ICD9CM"));
  }

  private void saveParticipantCohortStatus(Long participantId, Long reviewId) {
    participantCohortStatusDao.save(
        new DbParticipantCohortStatus()
            .participantKey(
                new DbParticipantCohortStatusKey()
                    .participantId(participantId)
                    .cohortReviewId(reviewId)));
  }

  private void assertResponse(
      ParticipantDataListResponse response, List<ParticipantData> expectedData) {
    List<ParticipantData> data = response.getItems();
    assertThat(data.size()).isEqualTo(expectedData.size());
    int i = 0;
    for (ParticipantData actualData : data) {
      ParticipantData expected = expectedData.get(i++);
      assertThat(actualData).isEqualTo(expected);
      assertThat(actualData.getItemDate()).isEqualTo(expected.getItemDate());
    }
  }

  private DbCohortReview createCohortReview(DbCohort dbCohort) {
    return cohortReviewDao.save(
        new DbCohortReview()
            .cdrVersionId(cdrVersion.getCdrVersionId())
            .matchedParticipantCount(212)
            .creationTime(new Timestamp(new Date().getTime()))
            .lastModifiedTime(new Timestamp(new Date().getTime()))
            .cohortId(dbCohort.getCohortId()));
  }

  private void stubMockFirecloudGetWorkspace() {
    FirecloudWorkspaceResponse workspaceResponse = new FirecloudWorkspaceResponse();
    workspaceResponse.setAccessLevel(WorkspaceAccessLevel.WRITER.toString());
    when(mockFireCloudService.getWorkspace(NAMESPACE, NAME)).thenReturn(workspaceResponse);
  }

  private void stubMockFirecloudGetWorkspaceAcl() {
    FirecloudWorkspaceACL workspaceAccessLevelResponse = new FirecloudWorkspaceACL();
    FirecloudWorkspaceAccessEntry accessLevelEntry =
        new FirecloudWorkspaceAccessEntry().accessLevel(WorkspaceAccessLevel.WRITER.toString());
    Map<String, FirecloudWorkspaceAccessEntry> userEmailToAccessEntry =
        ImmutableMap.of(currentUser.getUsername(), accessLevelEntry);
    workspaceAccessLevelResponse.setAcl(userEmailToAccessEntry);
    when(mockFireCloudService.getWorkspaceAclAsService(NAMESPACE, NAME))
        .thenReturn(workspaceAccessLevelResponse);
  }
}
