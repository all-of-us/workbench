package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
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
import org.pmiops.workbench.billing.FreeTierBillingService;
import org.pmiops.workbench.cohortbuilder.CohortQueryBuilder;
import org.pmiops.workbench.cohortbuilder.SearchGroupItemQueryBuilder;
import org.pmiops.workbench.cohortreview.CohortReviewMapperImpl;
import org.pmiops.workbench.cohortreview.CohortReviewServiceImpl;
import org.pmiops.workbench.cohortreview.ReviewQueryBuilder;
import org.pmiops.workbench.cohorts.CohortCloningService;
import org.pmiops.workbench.cohorts.CohortFactory;
import org.pmiops.workbench.cohorts.CohortMapperImpl;
import org.pmiops.workbench.conceptset.ConceptSetMapperImpl;
import org.pmiops.workbench.conceptset.ConceptSetService;
import org.pmiops.workbench.dataset.DataSetMapperImpl;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.CohortReviewDao;
import org.pmiops.workbench.db.dao.DataSetService;
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
import org.pmiops.workbench.model.CohortStatus;
import org.pmiops.workbench.model.CreateReviewRequest;
import org.pmiops.workbench.model.DomainType;
import org.pmiops.workbench.model.EmailVerificationStatus;
import org.pmiops.workbench.model.PageFilterRequest;
import org.pmiops.workbench.model.ParticipantChartData;
import org.pmiops.workbench.model.ParticipantChartDataListResponse;
import org.pmiops.workbench.model.ParticipantData;
import org.pmiops.workbench.model.ParticipantDataListResponse;
import org.pmiops.workbench.model.ReviewStatus;
import org.pmiops.workbench.model.SortOrder;
import org.pmiops.workbench.model.Vocabulary;
import org.pmiops.workbench.model.VocabularyListResponse;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.test.SearchRequests;
import org.pmiops.workbench.testconfig.TestJpaConfig;
import org.pmiops.workbench.testconfig.TestWorkbenchConfig;
import org.pmiops.workbench.utils.WorkspaceMapperImpl;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.workspaces.WorkspaceServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;

@RunWith(BeforeAfterSpringTestRunner.class)
@Import({TestJpaConfig.class})
public class CohortReviewControllerBQTest extends BigQueryBaseTest {

  @TestConfiguration
  @Import({
    WorkspaceServiceImpl.class,
    CohortMapperImpl.class,
    CohortReviewMapperImpl.class,
    CohortReviewServiceImpl.class,
    CohortReviewController.class,
    ConceptSetMapperImpl.class,
    DataSetMapperImpl.class,
    BigQueryTestService.class,
    ReviewQueryBuilder.class,
    CohortCloningService.class,
    CohortQueryBuilder.class,
    SearchGroupItemQueryBuilder.class,
    WorkspaceMapperImpl.class,
    CommonMappers.class
  })
  @MockBean({
    FireCloudService.class,
    UserRecentResourceService.class,
    CohortFactory.class,
    ConceptSetService.class,
    DataSetService.class,
    FreeTierBillingService.class
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

  private DbCohort cohortWithoutEHRData;
  private DbCohort cohortWithEHRData;
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

    when(mockFireCloudService.getWorkspaceAcl(anyString(), anyString()))
        .thenReturn(
            new FirecloudWorkspaceACL()
                .acl(
                    ImmutableMap.of(
                        currentUser.getUsername(),
                        new FirecloudWorkspaceAccessEntry().accessLevel("OWNER"))));

    cdrVersion = new DbCdrVersion();
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
    cohortWithoutEHRData = new DbCohort();
    cohortWithoutEHRData.setWorkspaceId(workspace.getWorkspaceId());
    cohortWithoutEHRData.setCriteria(gson.toJson(SearchRequests.males()));
    cohortWithoutEHRData = cohortDao.save(cohortWithoutEHRData);

    cohortWithEHRData = new DbCohort();
    cohortWithEHRData.setWorkspaceId(workspace.getWorkspaceId());
    cohortWithEHRData.setCriteria(gson.toJson(SearchRequests.malesWithEHRData()));
    cohortWithEHRData = cohortDao.save(cohortWithEHRData);

    reviewWithoutEHRData =
        new DbCohortReview()
            .cdrVersionId(cdrVersion.getCdrVersionId())
            .matchedParticipantCount(212)
            .creationTime(new Timestamp(new Date().getTime()))
            .lastModifiedTime(new Timestamp(new Date().getTime()))
            .cohortId(cohortWithoutEHRData.getCohortId());
    reviewWithoutEHRData = cohortReviewDao.save(reviewWithoutEHRData);

    DbParticipantCohortStatusKey key =
        new DbParticipantCohortStatusKey()
            .participantId(PARTICIPANT_ID)
            .cohortReviewId(reviewWithoutEHRData.getCohortReviewId());
    participantCohortStatusDao.save(new DbParticipantCohortStatus().participantKey(key));

    DbParticipantCohortStatusKey key2 =
        new DbParticipantCohortStatusKey()
            .participantId(PARTICIPANT_ID2)
            .cohortReviewId(reviewWithoutEHRData.getCohortReviewId());
    participantCohortStatusDao.save(new DbParticipantCohortStatus().participantKey(key2));

    reviewWithEHRData =
        new DbCohortReview()
            .cdrVersionId(cdrVersion.getCdrVersionId())
            .matchedParticipantCount(212)
            .creationTime(new Timestamp(new Date().getTime()))
            .lastModifiedTime(new Timestamp(new Date().getTime()))
            .cohortId(cohortWithEHRData.getCohortId());
    reviewWithEHRData = cohortReviewDao.save(reviewWithEHRData);

    DbParticipantCohortStatusKey key3 =
        new DbParticipantCohortStatusKey()
            .participantId(PARTICIPANT_ID)
            .cohortReviewId(reviewWithEHRData.getCohortReviewId());
    participantCohortStatusDao.save(new DbParticipantCohortStatus().participantKey(key3));

    DbParticipantCohortStatusKey key4 =
        new DbParticipantCohortStatusKey()
            .participantId(PARTICIPANT_ID2)
            .cohortReviewId(reviewWithEHRData.getCohortReviewId());
    participantCohortStatusDao.save(new DbParticipantCohortStatus().participantKey(key4));
  }

  @After
  public void tearDown() {
    workspaceDao.delete(workspace.getWorkspaceId());
    cdrVersionDao.delete(cdrVersion.getCdrVersionId());
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
        .domain(DomainType.CONDITION.toString())
        .value("1.0")
        .visitType("visit")
        .standardVocabulary("SNOMED")
        .standardCode("002")
        .sourceCode("0020")
        .sourceVocabulary("ICD9CM")
        .sourceName("Typhoid and paratyphoid fevers")
        .itemDate("2008-07-22 05:00:00 UTC")
        .standardName("SNOMED")
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
        .domain(DomainType.CONDITION.toString())
        .value("1.0")
        .visitType("visit")
        .standardVocabulary("SNOMED")
        .standardCode("002")
        .sourceCode("0021")
        .sourceVocabulary("ICD9CM")
        .sourceName("Typhoid and paratyphoid fevers")
        .itemDate("2008-08-01 05:00:00 UTC")
        .standardName("SNOMED")
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
            .creationTime(reviewWithoutEHRData.getCreationTime().toString())
            .lastModifiedTime(reviewWithoutEHRData.getLastModifiedTime().getTime())
            .matchedParticipantCount(reviewWithoutEHRData.getMatchedParticipantCount())
            .reviewedCount(reviewWithoutEHRData.getReviewedCount())
            .etag(Etags.fromVersion(reviewWithoutEHRData.getVersion()));
    assertEquals(
        expectedReview,
        controller.getCohortReviewsInWorkspace(NAMESPACE, NAME).getBody().getItems().get(0));
  }

  @Test
  public void createCohortReview() {
    DbCohort cohortWithoutReview = new DbCohort();
    cohortWithoutReview.setWorkspaceId(workspace.getWorkspaceId());
    String criteria =
        "{\"includes\":[{\"id\":\"includes_kl4uky6kh\",\"items\":[{\"id\":\"items_58myrn9iz\",\"type\":\"CONDITION\",\"searchParameters\":[{"
            + "\"parameterId\":\"param1567486C34\",\"name\":\"Malignant neoplasm of bronchus and lung\",\"domain\":\"CONDITION\",\"type\": "
            + "\"ICD10CM\",\"group\":true,\"attributes\":[],\"ancestorData\":false,\"standard\":false,\"conceptId\":1,\"value\":\"C34\"}],"
            + "\"modifiers\":[]}],\"temporal\":false}],\"excludes\":[]}";
    cohortWithoutReview.setCriteria(criteria);
    cohortWithoutReview = cohortDao.save(cohortWithoutReview);

    CohortReview cohortReview =
        controller
            .createCohortReview(
                NAMESPACE,
                NAME,
                cohortWithoutReview.getCohortId(),
                cdrVersion.getCdrVersionId(),
                new CreateReviewRequest().size(1))
            .getBody();

    assertThat(cohortReview.getReviewStatus()).isEqualTo(ReviewStatus.CREATED);
    assertThat(cohortReview.getReviewSize()).isEqualTo(1);
    assertThat(cohortReview.getParticipantCohortStatuses().size()).isEqualTo(1);
    assertThat(cohortReview.getParticipantCohortStatuses().get(0).getStatus())
        .isEqualTo(CohortStatus.NOT_REVIEWED);
    assertThat(cohortReview.getParticipantCohortStatuses().get(0).getDeceased()).isEqualTo(false);
  }

  @Test
  public void createCohortReviewWithEHRData() {
    DbCohort cohortWithoutReview = new DbCohort();
    cohortWithoutReview.setWorkspaceId(workspace.getWorkspaceId());
    String criteria =
        "{\"includes\":[{\"id\":\"includes_kl4uky6kh\",\"items\":[{\"id\":\"items_58myrn9iz\",\"type\":\"CONDITION\",\"searchParameters\":[{"
            + "\"parameterId\":\"param1567486C34\",\"name\":\"Malignant neoplasm of bronchus and lung\",\"domain\":\"CONDITION\",\"type\": "
            + "\"ICD10CM\",\"group\":true,\"attributes\":[],\"ancestorData\":false,\"standard\":false,\"conceptId\":1,\"value\":\"C34\"}],"
            + "\"modifiers\":[]}],\"temporal\":false}],\"excludes\":[],\"dataFilters\":[\"HAS_EHR_DATA\"]}";
    cohortWithoutReview.setCriteria(criteria);
    cohortWithoutReview = cohortDao.save(cohortWithoutReview);

    CohortReview cohortReview =
        controller
            .createCohortReview(
                NAMESPACE,
                NAME,
                cohortWithoutReview.getCohortId(),
                cdrVersion.getCdrVersionId(),
                new CreateReviewRequest().size(1))
            .getBody();

    assertThat(cohortReview.getReviewStatus()).isEqualTo(ReviewStatus.CREATED);
    assertThat(cohortReview.getReviewSize()).isEqualTo(1);
    assertThat(cohortReview.getParticipantCohortStatuses().size()).isEqualTo(1);
    assertThat(cohortReview.getParticipantCohortStatuses().get(0).getStatus())
        .isEqualTo(CohortStatus.NOT_REVIEWED);
    assertThat(cohortReview.getParticipantCohortStatuses().get(0).getDeceased()).isEqualTo(false);
  }

  @Test
  public void getParticipantConditionsSorting() {
    PageFilterRequest testFilter = new PageFilterRequest().domain(DomainType.CONDITION);

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

    assertResponse(response, ImmutableList.of(expectedCondition1(), expectedCondition2()), 2);

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

    assertResponse(response, ImmutableList.of(expectedCondition2(), expectedCondition1()), 2);
  }

  @Test
  public void getParticipantConditionsPagination() {
    stubMockFirecloudGetWorkspace();

    PageFilterRequest testFilter =
        new PageFilterRequest().domain(DomainType.CONDITION).page(0).pageSize(1);

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

    assertResponse(response, ImmutableList.of(expectedCondition1()), 2);

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
    assertResponse(response, ImmutableList.of(expectedCondition2()), 2);
  }

  @Test
  public void getParticipantAllEventsPagination() {
    PageFilterRequest testFilter =
        new PageFilterRequest().domain(DomainType.ALL_EVENTS).page(0).pageSize(1);

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

    assertResponse(response, ImmutableList.of(expectedAllEvents1()), 2);

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

    assertResponse(response, ImmutableList.of(expectedAllEvents2()), 2);
  }

  @Test
  public void getParticipantAllEventsSorting() {
    PageFilterRequest testFilter = new PageFilterRequest().domain(DomainType.ALL_EVENTS);

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

    assertResponse(response, ImmutableList.of(expectedAllEvents1(), expectedAllEvents2()), 2);

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

    assertResponse(response, ImmutableList.of(expectedAllEvents2(), expectedAllEvents1()), 2);
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
                DomainType.CONDITION.name(),
                null)
            .getBody();

    ParticipantChartData expectedData1 =
        new ParticipantChartData()
            .ageAtEvent(28)
            .rank(1)
            .standardName("SNOMED")
            .standardVocabulary("SNOMED")
            .startDate("2008-07-22");
    ParticipantChartData expectedData2 =
        new ParticipantChartData()
            .ageAtEvent(28)
            .rank(1)
            .standardName("SNOMED")
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
          DomainType.CONDITION.name(),
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
          DomainType.CONDITION.name(),
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
          NAMESPACE,
          NAME,
          reviewWithoutEHRData.getCohortReviewId(),
          DomainType.CONDITION.name(),
          -1);
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
          NAMESPACE,
          NAME,
          reviewWithoutEHRData.getCohortReviewId(),
          DomainType.CONDITION.name(),
          101);
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
                NAMESPACE,
                NAME,
                reviewWithoutEHRData.getCohortReviewId(),
                DomainType.LAB.name(),
                10)
            .getBody();
    assertEquals(3, response.getItems().size());
    assertEquals(
        new CohortChartData().name("name10").conceptId(10L).count(1L), response.getItems().get(0));
    assertEquals(
        new CohortChartData().name("name3").conceptId(3L).count(1L), response.getItems().get(1));
    assertEquals(
        new CohortChartData().name("name9").conceptId(9L).count(1L), response.getItems().get(2));
  }

  @Test
  public void getCohortChartDataLabWithEHRData() {
    CohortChartDataListResponse response =
        controller
            .getCohortChartData(
                NAMESPACE, NAME, reviewWithEHRData.getCohortReviewId(), DomainType.LAB.name(), 10)
            .getBody();
    assertEquals(3, response.getItems().size());
    assertEquals(
        new CohortChartData().name("name10").conceptId(10L).count(1L), response.getItems().get(0));
    assertEquals(
        new CohortChartData().name("name3").conceptId(3L).count(1L), response.getItems().get(1));
    assertEquals(
        new CohortChartData().name("name9").conceptId(9L).count(1L), response.getItems().get(2));
  }

  @Test
  public void getCohortChartDataDrug() {
    CohortChartDataListResponse response =
        controller
            .getCohortChartData(
                NAMESPACE,
                NAME,
                reviewWithoutEHRData.getCohortReviewId(),
                DomainType.DRUG.name(),
                10)
            .getBody();
    assertEquals(1, response.getItems().size());
    assertEquals(
        new CohortChartData().name("name11").conceptId(1L).count(1L), response.getItems().get(0));
  }

  @Test
  public void getCohortChartDataCondition() {
    CohortChartDataListResponse response =
        controller
            .getCohortChartData(
                NAMESPACE,
                NAME,
                reviewWithoutEHRData.getCohortReviewId(),
                DomainType.CONDITION.name(),
                10)
            .getBody();
    assertEquals(2, response.getItems().size());
    assertEquals(
        new CohortChartData().name("name1").conceptId(1L).count(1L), response.getItems().get(0));
    assertEquals(
        new CohortChartData().name("name7").conceptId(7L).count(1L), response.getItems().get(1));
  }

  @Test
  public void getCohortChartDataProcedure() {
    CohortChartDataListResponse response =
        controller
            .getCohortChartData(
                NAMESPACE,
                NAME,
                reviewWithoutEHRData.getCohortReviewId(),
                DomainType.PROCEDURE.name(),
                10)
            .getBody();
    assertEquals(3, response.getItems().size());
    assertEquals(
        new CohortChartData().name("name2").conceptId(2L).count(1L), response.getItems().get(0));
    assertEquals(
        new CohortChartData().name("name4").conceptId(4L).count(1L), response.getItems().get(1));
    assertEquals(
        new CohortChartData().name("name8").conceptId(8L).count(1L), response.getItems().get(2));
  }

  @Test
  public void getVocabularies() {
    VocabularyListResponse response =
        controller
            .getVocabularies(NAMESPACE, NAME, reviewWithoutEHRData.getCohortReviewId())
            .getBody();
    assertEquals(20, response.getItems().size());
    assertEquals(
        new Vocabulary().type("Source").domain("ALL_EVENTS").vocabulary("CPT4"),
        response.getItems().get(0));
    assertEquals(
        new Vocabulary().type("Source").domain("ALL_EVENTS").vocabulary("ICD10CM"),
        response.getItems().get(1));
    assertEquals(
        new Vocabulary().type("Source").domain("ALL_EVENTS").vocabulary("ICD9CM"),
        response.getItems().get(2));
  }

  private void assertResponse(
      ParticipantDataListResponse response, List<ParticipantData> expectedData, int totalCount) {
    List<ParticipantData> data = response.getItems();
    assertThat(response.getCount()).isEqualTo(totalCount);
    assertThat(data.size()).isEqualTo(expectedData.size());
    int i = 0;
    for (ParticipantData actualData : data) {
      ParticipantData expected = expectedData.get(i++);
      assertThat(actualData).isEqualTo(expected);
      assertThat(actualData.getItemDate()).isEqualTo(expected.getItemDate());
    }
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
    when(mockFireCloudService.getWorkspaceAcl(NAMESPACE, NAME))
        .thenReturn(workspaceAccessLevelResponse);
  }
}
