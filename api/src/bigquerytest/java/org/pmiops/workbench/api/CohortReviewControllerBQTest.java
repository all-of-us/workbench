package org.pmiops.workbench.api;

import org.bitbucket.radistao.test.runner.BeforeAfterSpringTestRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.cdr.cache.GenderRaceEthnicityConcept;
import org.pmiops.workbench.cohortbuilder.CohortQueryBuilder;
import org.pmiops.workbench.cohortbuilder.ParticipantCounter;
import org.pmiops.workbench.cohortreview.CohortReviewServiceImpl;
import org.pmiops.workbench.cohortreview.ReviewQueryBuilder;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.CohortReviewDao;
import org.pmiops.workbench.db.dao.CohortService;
import org.pmiops.workbench.db.dao.ConceptSetService;
import org.pmiops.workbench.db.dao.ParticipantCohortStatusDao;
import org.pmiops.workbench.db.dao.UserRecentResourceService;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.dao.WorkspaceServiceImpl;
import org.pmiops.workbench.db.model.CdrVersion;
import org.pmiops.workbench.db.model.Cohort;
import org.pmiops.workbench.db.model.CohortReview;
import org.pmiops.workbench.db.model.ParticipantCohortStatus;
import org.pmiops.workbench.db.model.ParticipantCohortStatusKey;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.firecloud.ApiException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.WorkspaceResponse;
import org.pmiops.workbench.model.*;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.testconfig.TestJpaConfig;
import org.pmiops.workbench.testconfig.TestWorkbenchConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

import java.time.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

@RunWith(BeforeAfterSpringTestRunner.class)
@Import({TestJpaConfig.class})
@ComponentScan(basePackages = "org.pmiops.workbench.cohortreview.*")
public class CohortReviewControllerBQTest extends BigQueryBaseTest {

  private static final String NAMESPACE = "aou-test";
  private static final String NAME = "test";
  private static final Long PARTICIPANT_ID = 102246L;
  private static final FakeClock CLOCK = new FakeClock(Instant.now(), ZoneId.systemDefault());
  private ParticipantData expectedCondition1;
  private ParticipantData expectedCondition2;
  private ParticipantData expectedPhysicalMeasure1;
  private ParticipantData expectedPhysicalMeasure2;
  private ParticipantData expectedMeasurement1;
  private ParticipantData expectedMeasurement2;
  private ParticipantData expectedProcedure1;
  private ParticipantData expectedProcedure2;
  private ParticipantData expectedObservation1;
  private ParticipantData expectedObservation2;
  private ParticipantData expectedDrug1;
  private ParticipantData expectedDrug2;
  private ParticipantData expectedAllEvents1;
  private ParticipantData expectedAllEvents2;
  private ParticipantData expectedAllEvents3;
  private ParticipantData expectedAllEvents4;
  private ParticipantData expectedAllEvents5;
  private ParticipantData expectedAllEvents6;
  private ParticipantData expectedAllEvents7;
  private ParticipantData expectedAllEvents8;
  private CdrVersion cdrVersion;
  private Workspace workspace;

  @Autowired
  private CohortReviewController controller;

  @Autowired
  private TestWorkbenchConfig testWorkbenchConfig;

  @Autowired
  private CohortDao cohortDao;

  @Autowired
  private CohortReviewDao cohortReviewDao;

  @Autowired
  private WorkspaceDao workspaceDao;

  @Autowired
  private CdrVersionDao cdrVersionDao;

  @Autowired
  private ParticipantCohortStatusDao participantCohortStatusDao;

  @Autowired
  private FireCloudService mockFireCloudService;

  private Cohort cohort;

  @TestConfiguration
  @Import({
    WorkspaceServiceImpl.class,
    CohortReviewServiceImpl.class,
    CohortReviewController.class,
    BigQueryService.class,
    ReviewQueryBuilder.class,
    CohortService.class,
    ConceptSetService.class,
    ParticipantCounter.class,
    DomainLookupService.class,
    CohortQueryBuilder.class
  })
  @MockBean({
    FireCloudService.class,
    UserRecentResourceService.class
  })
  static class Configuration {
    @Bean
    public GenderRaceEthnicityConcept getGenderRaceEthnicityConcept() {
      Map<String, Map<Long, String>> concepts = new HashMap<>();
      concepts.put(ParticipantCohortStatusColumns.RACE.name(), new HashMap<>());
      concepts.put(ParticipantCohortStatusColumns.GENDER.name(), new HashMap<>());
      concepts.put(ParticipantCohortStatusColumns.ETHNICITY.name(), new HashMap<>());
      return new GenderRaceEthnicityConcept(concepts);
    }

    @Bean
    public Clock clock() {
      return CLOCK;
    }
  }

  @Override
  public List<String> getTableNames() {
    return Arrays.asList(
      "p_all_events",
      "p_condition",
      "p_procedure",
      "p_observation",
      "p_measurement",
      "p_drug",
      "p_physical_measure"
    );
  }

  @Override
  public String getTestDataDirectory() {
    return CB_DATA;
  }

  @Before
  public void setUp() {

    expectedAllEvents1 = new AllEvents()
      .dataId(12751440L)
      .domain("Condition")
      .standardVocabulary("SNOMED")
      .standardName("SNOMED")
      .standardCode("002")
      .sourceValue("0020")
      .sourceVocabulary("ICD9CM")
      .sourceName("Typhoid and paratyphoid fevers")
      .numMentions("2")
      .ageAtEvent(28)
      .firstMention("2008-07-22 05:00:00 UTC")
      .lastMention("2008-07-22 05:00:00 UTC")
      .visitType("visit")
      .itemDate("2008-07-22 05:00:00 UTC")
      .domainType(DomainType.ALL_EVENTS);
    expectedAllEvents2 = new AllEvents()
      .dataId(12751441L)
      .domain("Condition")
      .standardVocabulary("SNOMED")
      .standardName("SNOMED")
      .standardCode("002")
      .sourceValue("0021")
      .sourceVocabulary("ICD9CM")
      .sourceName("Typhoid and paratyphoid fevers")
      .ageAtEvent(28)
      .numMentions("2")
      .firstMention("2008-08-01 05:00:00 UTC")
      .lastMention("2008-08-01 05:00:00 UTC")
      .visitType("visit")
      .itemDate("2008-08-01 05:00:00 UTC")
      .domainType(DomainType.ALL_EVENTS);
    expectedAllEvents3 = new AllEvents()
      .dataId(12751446L)
      .domain("Observation")
      .standardVocabulary("ICD10CM")
      .standardName("name")
      .standardCode("002")
      .sourceValue("sourceValue")
      .sourceVocabulary("ICD10CM")
      .sourceName("name")
      .numMentions("2")
      .ageAtEvent(29)
      .firstMention("2008-08-01 05:00:00 UTC")
      .lastMention("2008-08-01 05:00:00 UTC")
      .visitType("visit")
      .itemDate("2009-12-03 05:00:00 UTC")
      .domainType(DomainType.ALL_EVENTS);
    expectedAllEvents4 = new AllEvents()
      .dataId(12751447L)
      .domain("Observation")
      .standardVocabulary("ICD10CM")
      .standardName("name")
      .standardCode("002")
      .sourceValue("sourceValue")
      .sourceVocabulary("ICD10CM")
      .sourceName("name")
      .numMentions("2")
      .ageAtEvent(29)
      .firstMention("2008-08-01 05:00:00 UTC")
      .lastMention("2008-08-01 05:00:00 UTC")
      .visitType("visit")
      .itemDate("2009-12-04 05:00:00 UTC")
      .domainType(DomainType.ALL_EVENTS);
    expectedAllEvents5 = new AllEvents()
      .dataId(12751444L)
      .domain("Procedure")
      .standardVocabulary("ICD10CM")
      .standardName("name")
      .standardCode("002")
      .sourceValue("val")
      .sourceVocabulary("ICD10CM")
      .sourceName("name")
      .numMentions("2")
      .ageAtEvent(29)
      .firstMention("2008-08-01 05:00:00 UTC")
      .lastMention("2008-08-01 05:00:00 UTC")
      .visitType("visit")
      .itemDate("2009-12-03 05:00:00 UTC")
      .domainType(DomainType.ALL_EVENTS);
    expectedAllEvents6 = new AllEvents()
      .dataId(12751445L)
      .domain("Procedure")
      .standardVocabulary("CPT4")
      .standardName("name")
      .standardCode("002")
      .sourceValue("val")
      .sourceVocabulary("CPT4")
      .sourceName("name")
      .numMentions("2")
      .ageAtEvent(29)
      .firstMention("2008-08-01 05:00:00 UTC")
      .lastMention("2008-08-01 05:00:00 UTC")
      .visitType("visit")
      .itemDate("2009-12-04 05:00:00 UTC")
      .domainType(DomainType.ALL_EVENTS);
    expectedAllEvents7 = new AllEvents()
      .dataId(12751442L)
      .domain("Condition")
      .standardVocabulary("CPT4")
      .standardName("name")
      .standardCode("002")
      .sourceValue("Varivax")
      .sourceVocabulary("CPT4")
      .sourceName("name")
      .numMentions("2")
      .ageAtEvent(21)
      .firstMention("2008-08-01 05:00:00 UTC")
      .lastMention("2008-08-01 05:00:00 UTC")
      .visitType("visit")
      .itemDate("2001-12-03 05:00:00 UTC")
      .domainType(DomainType.ALL_EVENTS);
    expectedAllEvents8 = new AllEvents()
      .dataId(12751443L)
      .domain("Drug")
      .standardVocabulary("CPT4")
      .standardName("name")
      .standardCode("002")
      .sourceValue("Varivax")
      .sourceVocabulary("CPT4")
      .sourceName("name")
      .numMentions("2")
      .ageAtEvent(21)
      .firstMention("2008-08-01 05:00:00 UTC")
      .lastMention("2008-08-01 05:00:00 UTC")
      .visitType("visit")
      .itemDate("2001-12-04 05:00:00 UTC")
      .domainType(DomainType.ALL_EVENTS);
    expectedCondition1 = new Condition()
      .visitId(1L)
      .firstMention("2008-07-22 05:00:00 UTC")
      .lastMention("2008-07-22 05:00:00 UTC")
      .numMentions("1")
      .ageAtEvent(28)
      .standardVocabulary("SNOMED")
      .standardName("SNOMED")
      .standardCode("002")
      .sourceCode("004")
      .sourceVocabulary("ICD9CM")
      .sourceName("Typhoid and paratyphoid fevers")
      .itemDate("2008-07-22 05:00:00 UTC")
      .domainType(DomainType.CONDITION);
    expectedCondition2 = new Condition()
      .visitId(1L)
      .firstMention("2008-07-22 05:00:00 UTC")
      .lastMention("2008-07-22 05:00:00 UTC")
      .numMentions("1")
      .ageAtEvent(28)
      .standardVocabulary("SNOMED")
      .standardName("SNOMED")
      .standardCode("002")
      .sourceCode("004")
      .sourceVocabulary("ICD9CM")
      .sourceName("Typhoid and paratyphoid fevers")
      .itemDate("2008-08-01 05:00:00 UTC")
      .domainType(DomainType.CONDITION);
    expectedPhysicalMeasure1 = new PhysicalMeasurement()
      .valueConcept("val")
      .valueSource("val")
      .valueNumber("1.0")
      .units("nits")
      .ageAtEvent(28)
      .standardVocabulary("SNOMED")
      .standardName("SNOMED")
      .standardCode("002")
      .itemDate("2008-07-22 05:00:00 UTC")
      .domainType(DomainType.PHYSICAL_MEASURE);
    expectedPhysicalMeasure2 = new PhysicalMeasurement()
      .valueConcept("val")
      .valueSource("val")
      .valueNumber("1.0")
      .units("nits")
      .ageAtEvent(28)
      .standardVocabulary("SNOMED")
      .standardName("SNOMED")
      .standardCode("002")
      .itemDate("2008-08-01 05:00:00 UTC")
      .domainType(DomainType.PHYSICAL_MEASURE);
    expectedMeasurement1 = new Measurement()
      .valueConcept("val")
      .valueSource("val")
      .valueNumber("1.0")
      .units("units")
      .labRefRange("range")
      .visitId(1L)
      .ageAtEvent(29)
      .standardVocabulary("ICD10CM")
      .standardName("name")
      .standardCode("002")
      .sourceCode("004")
      .sourceName("name")
      .itemDate("2009-12-03 05:00:00 UTC")
      .domainType(DomainType.MEASUREMENT);
    expectedMeasurement2 = new Measurement()
      .valueConcept("val")
      .valueSource("val")
      .valueNumber("1.0")
      .units("units")
      .labRefRange("range")
      .visitId(1L)
      .visitId(1L)
      .ageAtEvent(29)
      .standardVocabulary("CPT4")
      .standardName("name")
      .standardCode("002")
      .sourceCode("004")
      .sourceName("name")
      .itemDate("2009-12-04 05:00:00 UTC")
      .domainType(DomainType.MEASUREMENT);
    expectedProcedure1 = new Procedure()
      .visitId(1L)
      .firstMention("2009-12-03 05:00:00 UTC")
      .lastMention("2009-12-03 05:00:00 UTC")
      .numMentions("2")
      .ageAtEvent(29)
      .standardVocabulary("ICD10CM")
      .standardName("name")
      .standardCode("002")
      .sourceCode("004")
      .sourceVocabulary("ICD10CM")
      .sourceName("name")
      .itemDate("2009-12-03 05:00:00 UTC")
      .domainType(DomainType.PROCEDURE);
    expectedProcedure2 = new Procedure()
      .visitId(1L)
      .firstMention("2009-12-03 05:00:00 UTC")
      .lastMention("2009-12-03 05:00:00 UTC")
      .numMentions("2")
      .ageAtEvent(29)
      .standardVocabulary("CPT4")
      .standardName("name")
      .standardCode("002")
      .sourceCode("004")
      .sourceVocabulary("CPT4")
      .sourceName("name")
      .itemDate("2009-12-04 05:00:00 UTC")
      .domainType(DomainType.PROCEDURE);
    expectedObservation1 = new Observation()
      .visitId(1L)
      .ageAtEvent(29)
      .standardVocabulary("ICD10CM")
      .standardName("name")
      .standardCode("002")
      .sourceCode("004")
      .sourceVocabulary("ICD10CM")
      .sourceName("name")
      .itemDate("2009-12-03 05:00:00 UTC")
      .domainType(DomainType.OBSERVATION);
    expectedObservation2 = new Observation()
      .visitId(1L)
      .ageAtEvent(29)
      .standardVocabulary("ICD10CM")
      .standardName("name")
      .standardCode("002")
      .sourceCode("004")
      .sourceVocabulary("ICD10CM")
      .sourceName("name")
      .itemDate("2009-12-04 05:00:00 UTC")
      .domainType(DomainType.OBSERVATION);
    expectedDrug1 = new Drug()
      .visitId(1L)
      .route("route")
      .strength("str")
      .refills("1")
      .quantity("1.0")
      .numMentions("2")
      .firstMention("2001-12-03 05:00:00 UTC")
      .lastMention("2001-12-03 05:00:00 UTC")
      .ageAtEvent(21)
      .sourceVocabulary("CPT4")
      .standardName("name")
      .standardCode("002")
      .sourceCode("004")
      .sourceVocabulary("CPT4")
      .sourceName("name")
      .itemDate("2001-12-03 05:00:00 UTC")
      .domainType(DomainType.DRUG);
    expectedDrug2 = new Drug()
      .visitId(1L)
      .route("route")
      .strength("str")
      .refills("1")
      .quantity("1.0")
      .numMentions("2")
      .firstMention("2001-12-03 05:00:00 UTC")
      .lastMention("2001-12-03 05:00:00 UTC")
      .ageAtEvent(21)
      .sourceVocabulary("CPT4")
      .standardName("name")
      .standardCode("002")
      .sourceCode("004")
      .sourceVocabulary("CPT4")
      .sourceName("name")
      .itemDate("2001-12-04 05:00:00 UTC")
      .domainType(DomainType.DRUG);

    cdrVersion = new CdrVersion();
    cdrVersion.setBigqueryDataset(testWorkbenchConfig.bigquery.dataSetId);
    cdrVersion.setBigqueryProject(testWorkbenchConfig.bigquery.projectId);
    cdrVersionDao.save(cdrVersion);

    workspace = new Workspace();
    workspace.setCdrVersion(cdrVersion);
    workspace.setWorkspaceNamespace(NAMESPACE);
    workspace.setFirecloudName(NAME);
    workspaceDao.save(workspace);

    cohort = new Cohort();
    cohort.setWorkspaceId(workspace.getWorkspaceId());
    cohortDao.save(cohort);

    CohortReview review = new CohortReview()
      .cdrVersionId(cdrVersion.getCdrVersionId())
      .cohortId(cohort.getCohortId());
    cohortReviewDao.save(review);

    ParticipantCohortStatusKey key = new ParticipantCohortStatusKey()
      .participantId(PARTICIPANT_ID)
      .cohortReviewId(review.getCohortReviewId());
    ParticipantCohortStatus participantCohortStatus = new ParticipantCohortStatus()
      .participantKey(key);
    participantCohortStatusDao.save(participantCohortStatus);
  }

  @After
  public void tearDown() {
    workspaceDao.delete(workspace.getWorkspaceId());
    cdrVersionDao.delete(cdrVersion.getCdrVersionId());
  }

  @Test
  public void getParticipantConditionsSorting() throws Exception {
    PageRequest expectedPageRequest = new PageRequest()
      .page(0)
      .pageSize(25)
      .sortOrder(SortOrder.ASC)
      .sortColumn("startDate");

    stubMockFirecloudGetWorkspace();

    ReviewFilter testFilter = new ReviewFilter().domain(DomainType.CONDITION);
    testFilter.pageFilterType(PageFilterType.REVIEWFILTER);

    //no sort order or column
    ParticipantDataListResponse response = controller
      .getParticipantData(
        NAMESPACE,
        NAME,
        cohort.getCohortId(),
        cdrVersion.getCdrVersionId(),
        PARTICIPANT_ID,
        testFilter)
      .getBody();

    assertResponse(response, expectedPageRequest, Arrays.asList(expectedCondition1, expectedCondition2), 2);

    //added sort order
    testFilter.sortOrder(SortOrder.DESC);
    expectedPageRequest.sortOrder(SortOrder.DESC);
    response = controller
      .getParticipantData(
        NAMESPACE,
        NAME,
        cohort.getCohortId(),
        cdrVersion.getCdrVersionId(),
        PARTICIPANT_ID,
        testFilter)
      .getBody();

    assertResponse(response, expectedPageRequest, Arrays.asList(expectedCondition2, expectedCondition1), 2);
  }

  @Test
  public void getParticipantConditionsPagination() throws Exception {
    PageRequest expectedPageRequest = new PageRequest()
      .page(0)
      .pageSize(1)
      .sortOrder(SortOrder.ASC)
      .sortColumn("startDate");

    stubMockFirecloudGetWorkspace();

    ReviewFilter testFilter = new ReviewFilter().domain(DomainType.CONDITION);
    testFilter.pageFilterType(PageFilterType.REVIEWFILTER);
    testFilter.page(0);
    testFilter.pageSize(1);

    //page 1 should have 1 item
    ParticipantDataListResponse response = controller
      .getParticipantData(
        NAMESPACE,
        NAME,
        cohort.getCohortId(),
        cdrVersion.getCdrVersionId(),
        PARTICIPANT_ID,
        testFilter)
      .getBody();

    assertResponse(response, expectedPageRequest, Arrays.asList(expectedCondition1), 2);

    //page 2 should have 1 item
    testFilter.page(1);
    expectedPageRequest.page(1);
    response = controller
      .getParticipantData(
        NAMESPACE,
        NAME,
        cohort.getCohortId(),
        cdrVersion.getCdrVersionId(),
        PARTICIPANT_ID,
        testFilter)
      .getBody();
    assertResponse(response, expectedPageRequest, Arrays.asList(expectedCondition2), 2);
  }

  @Test
  public void getParticipantPhysicalMeasureSorting() throws Exception {
    PageRequest expectedPageRequest = new PageRequest()
      .page(0)
      .pageSize(25)
      .sortOrder(SortOrder.ASC)
      .sortColumn("startDate");

    stubMockFirecloudGetWorkspace();

    ReviewFilter testFilter = new ReviewFilter().domain(DomainType.PHYSICAL_MEASURE);
    testFilter.pageFilterType(PageFilterType.REVIEWFILTER);

    //no sort order or column
    ParticipantDataListResponse response = controller
      .getParticipantData(
        NAMESPACE,
        NAME,
        cohort.getCohortId(),
        cdrVersion.getCdrVersionId(),
        PARTICIPANT_ID,
        testFilter)
      .getBody();

    assertResponse(response, expectedPageRequest, Arrays.asList(expectedPhysicalMeasure1, expectedPhysicalMeasure2), 2);

    //added sort order
    testFilter.sortOrder(SortOrder.DESC);
    expectedPageRequest.sortOrder(SortOrder.DESC);
    response = controller
      .getParticipantData(
        NAMESPACE,
        NAME,
        cohort.getCohortId(),
        cdrVersion.getCdrVersionId(),
        PARTICIPANT_ID,
        testFilter)
      .getBody();

    assertResponse(response, expectedPageRequest, Arrays.asList(expectedPhysicalMeasure2, expectedPhysicalMeasure1), 2);
  }

  @Test
  public void getParticipantPhysicalMeasurePagination() throws Exception {
    PageRequest expectedPageRequest = new PageRequest()
      .page(0)
      .pageSize(1)
      .sortOrder(SortOrder.ASC)
      .sortColumn("startDate");

    stubMockFirecloudGetWorkspace();

    ReviewFilter testFilter = new ReviewFilter().domain(DomainType.PHYSICAL_MEASURE);
    testFilter.pageFilterType(PageFilterType.REVIEWFILTER);
    testFilter.page(0);
    testFilter.pageSize(1);

    //page 1 should have 1 item
    ParticipantDataListResponse response = controller
      .getParticipantData(
        NAMESPACE,
        NAME,
        cohort.getCohortId(),
        cdrVersion.getCdrVersionId(),
        PARTICIPANT_ID,
        testFilter)
      .getBody();

    assertResponse(response, expectedPageRequest, Arrays.asList(expectedPhysicalMeasure1), 2);

    //page 2 should have 1 item
    testFilter.page(1);
    expectedPageRequest.page(1);
    response = controller
      .getParticipantData(
        NAMESPACE,
        NAME,
        cohort.getCohortId(),
        cdrVersion.getCdrVersionId(),
        PARTICIPANT_ID,
        testFilter)
      .getBody();
    assertResponse(response, expectedPageRequest, Arrays.asList(expectedPhysicalMeasure2), 2);
  }

  @Test
  public void getParticipantMeasurementsSorting() throws Exception {
    PageRequest expectedPageRequest = new PageRequest()
      .page(0)
      .pageSize(25)
      .sortOrder(SortOrder.ASC)
      .sortColumn("startDate");

    stubMockFirecloudGetWorkspace();

    ReviewFilter testFilter = new ReviewFilter().domain(DomainType.MEASUREMENT);
    testFilter.pageFilterType(PageFilterType.REVIEWFILTER);

    //no sort order or column
    ParticipantDataListResponse response = controller
      .getParticipantData(
        NAMESPACE,
        NAME,
        cohort.getCohortId(),
        cdrVersion.getCdrVersionId(),
        PARTICIPANT_ID,
        testFilter)
      .getBody();

    assertResponse(response, expectedPageRequest, Arrays.asList(expectedMeasurement1, expectedMeasurement2), 2);

    //added sort order
    testFilter.sortOrder(SortOrder.DESC);
    expectedPageRequest.sortOrder(SortOrder.DESC);
    response = controller
      .getParticipantData(
        NAMESPACE,
        NAME,
        cohort.getCohortId(),
        cdrVersion.getCdrVersionId(),
        PARTICIPANT_ID,
        testFilter)
      .getBody();

    assertResponse(response, expectedPageRequest, Arrays.asList(expectedMeasurement2, expectedMeasurement1), 2);
  }

  @Test
  public void getParticipantMeasurementsPagination() throws Exception {
    PageRequest expectedPageRequest = new PageRequest()
      .page(0)
      .pageSize(1)
      .sortOrder(SortOrder.ASC)
      .sortColumn("startDate");

    stubMockFirecloudGetWorkspace();

    ReviewFilter testFilter = new ReviewFilter().domain(DomainType.MEASUREMENT);
    testFilter.pageFilterType(PageFilterType.REVIEWFILTER);
    testFilter.page(0);
    testFilter.pageSize(1);

    //page 1 should have 1 item
    ParticipantDataListResponse response = controller
      .getParticipantData(
        NAMESPACE,
        NAME,
        cohort.getCohortId(),
        cdrVersion.getCdrVersionId(),
        PARTICIPANT_ID,
        testFilter)
      .getBody();

    assertResponse(response, expectedPageRequest, Arrays.asList(expectedMeasurement1), 2);

    //page 2 should have 1 item
    testFilter.page(1);
    expectedPageRequest.page(1);
    response = controller
      .getParticipantData(
        NAMESPACE,
        NAME,
        cohort.getCohortId(),
        cdrVersion.getCdrVersionId(),
        PARTICIPANT_ID,
        testFilter)
      .getBody();
    assertResponse(response, expectedPageRequest, Arrays.asList(expectedMeasurement2), 2);
  }

  @Test
  public void getParticipantProceduresSorting() throws Exception {
    PageRequest expectedPageRequest = new PageRequest()
      .page(0)
      .pageSize(25)
      .sortOrder(SortOrder.ASC)
      .sortColumn("startDate");

    stubMockFirecloudGetWorkspace();

    ReviewFilter testFilter = new ReviewFilter().domain(DomainType.PROCEDURE);
    testFilter.pageFilterType(PageFilterType.REVIEWFILTER);

    //no sort order or column
    ParticipantDataListResponse response = controller
      .getParticipantData(
        NAMESPACE,
        NAME,
        cohort.getCohortId(),
        cdrVersion.getCdrVersionId(),
        PARTICIPANT_ID,
        testFilter)
      .getBody();

    assertResponse(response, expectedPageRequest, Arrays.asList(expectedProcedure1, expectedProcedure2), 2);

    //added sort order
    testFilter.sortOrder(SortOrder.DESC);
    expectedPageRequest.sortOrder(SortOrder.DESC);
    response = controller
      .getParticipantData(
        NAMESPACE,
        NAME,
        cohort.getCohortId(),
        cdrVersion.getCdrVersionId(),
        PARTICIPANT_ID,
        testFilter)
      .getBody();

    assertResponse(response, expectedPageRequest, Arrays.asList(expectedProcedure2, expectedProcedure1), 2);
  }

  @Test
  public void getParticipantProceduresPagination() throws Exception {
    PageRequest expectedPageRequest = new PageRequest()
      .page(0)
      .pageSize(1)
      .sortOrder(SortOrder.ASC)
      .sortColumn("startDate");

    stubMockFirecloudGetWorkspace();

    ReviewFilter testFilter = new ReviewFilter().domain(DomainType.PROCEDURE);
    testFilter.pageFilterType(PageFilterType.REVIEWFILTER);
    testFilter.page(0);
    testFilter.pageSize(1);

    //page 1 should have 1 item
    ParticipantDataListResponse response = controller
      .getParticipantData(
        NAMESPACE,
        NAME,
        cohort.getCohortId(),
        cdrVersion.getCdrVersionId(),
        PARTICIPANT_ID,
        testFilter)
      .getBody();

    assertResponse(response, expectedPageRequest, Arrays.asList(expectedProcedure1), 2);

    //page 2 should have 1 item
    testFilter.page(1);
    expectedPageRequest.page(1);
    response = controller
      .getParticipantData(
        NAMESPACE,
        NAME,
        cohort.getCohortId(),
        cdrVersion.getCdrVersionId(),
        PARTICIPANT_ID,
        testFilter)
      .getBody();

    assertResponse(response, expectedPageRequest, Arrays.asList(expectedProcedure2), 2);
  }

  @Test
  public void getParticipantObservationsSorting() throws Exception {
    PageRequest expectedPageRequest = new PageRequest()
      .page(0)
      .pageSize(25)
      .sortOrder(SortOrder.ASC)
      .sortColumn("startDate");

    stubMockFirecloudGetWorkspace();

    ReviewFilter testFilter = new ReviewFilter().domain(DomainType.OBSERVATION);
    testFilter.pageFilterType(PageFilterType.REVIEWFILTER);

    //no sort order or column
    ParticipantDataListResponse response = controller
      .getParticipantData(
        NAMESPACE,
        NAME,
        cohort.getCohortId(),
        cdrVersion.getCdrVersionId(),
        PARTICIPANT_ID,
        testFilter)
      .getBody();

    assertResponse(response, expectedPageRequest, Arrays.asList(expectedObservation1, expectedObservation2), 2);

    //added sort order
    testFilter.sortOrder(SortOrder.DESC);
    expectedPageRequest.sortOrder(SortOrder.DESC);
    response = controller
      .getParticipantData(
        NAMESPACE,
        NAME,
        cohort.getCohortId(),
        cdrVersion.getCdrVersionId(),
        PARTICIPANT_ID,
        testFilter)
      .getBody();

    assertResponse(response, expectedPageRequest, Arrays.asList(expectedObservation2, expectedObservation1), 2);
  }

  @Test
  public void getParticipantObservationsPagination() throws Exception {
    PageRequest expectedPageRequest = new PageRequest()
      .page(0)
      .pageSize(1)
      .sortOrder(SortOrder.ASC)
      .sortColumn("startDate");

    stubMockFirecloudGetWorkspace();

    ReviewFilter testFilter = new ReviewFilter().domain(DomainType.OBSERVATION);
    testFilter.pageFilterType(PageFilterType.REVIEWFILTER);
    testFilter.page(0);
    testFilter.pageSize(1);

    //page 1 should have 1 item
    ParticipantDataListResponse response = controller
      .getParticipantData(
        NAMESPACE,
        NAME,
        cohort.getCohortId(),
        cdrVersion.getCdrVersionId(),
        PARTICIPANT_ID,
        testFilter)
      .getBody();

    assertResponse(response, expectedPageRequest, Arrays.asList(expectedObservation1), 2);

    //page 2 should have 1 item
    testFilter.page(1);
    expectedPageRequest.page(1);
    response = controller
      .getParticipantData(
        NAMESPACE,
        NAME,
        cohort.getCohortId(),
        cdrVersion.getCdrVersionId(),
        PARTICIPANT_ID,
        testFilter)
      .getBody();

    assertResponse(response, expectedPageRequest, Arrays.asList(expectedObservation2), 2);
  }

  @Test
  public void getParticipantDrugsSorting() throws Exception {
    PageRequest expectedPageRequest = new PageRequest()
      .page(0)
      .pageSize(25)
      .sortOrder(SortOrder.ASC)
      .sortColumn("startDate");

    stubMockFirecloudGetWorkspace();

    ReviewFilter testFilter = new ReviewFilter().domain(DomainType.DRUG);
    testFilter.pageFilterType(PageFilterType.REVIEWFILTER);

    //no sort order or column
    ParticipantDataListResponse response = controller
      .getParticipantData(
        NAMESPACE,
        NAME,
        cohort.getCohortId(),
        cdrVersion.getCdrVersionId(),
        PARTICIPANT_ID,
        testFilter)
      .getBody();

    assertResponse(response, expectedPageRequest, Arrays.asList(expectedDrug1, expectedDrug2), 2);

    //added sort order
    testFilter.sortOrder(SortOrder.DESC);
    expectedPageRequest.sortOrder(SortOrder.DESC);
    response = controller
      .getParticipantData(
        NAMESPACE,
        NAME,
        cohort.getCohortId(),
        cdrVersion.getCdrVersionId(),
        PARTICIPANT_ID,
        testFilter)
      .getBody();

    assertResponse(response, expectedPageRequest, Arrays.asList(expectedDrug2, expectedDrug1), 2);
  }

  @Test
  public void getParticipantDrugsPagination() throws Exception {
    PageRequest expectedPageRequest = new PageRequest()
      .page(0)
      .pageSize(1)
      .sortOrder(SortOrder.ASC)
      .sortColumn("startDate");

    stubMockFirecloudGetWorkspace();

    ReviewFilter testFilter = new ReviewFilter().domain(DomainType.DRUG);
    testFilter.pageFilterType(PageFilterType.REVIEWFILTER);
    testFilter.page(0);
    testFilter.pageSize(1);

    //page 1 should have 1 item
    ParticipantDataListResponse response = controller
      .getParticipantData(
        NAMESPACE,
        NAME,
        cohort.getCohortId(),
        cdrVersion.getCdrVersionId(),
        PARTICIPANT_ID,
        testFilter)
      .getBody();

    assertResponse(response, expectedPageRequest, Arrays.asList(expectedDrug1), 2);

    //page 2 should have 1 item
    testFilter.page(1);
    expectedPageRequest.page(1);
    response = controller
      .getParticipantData(
        NAMESPACE,
        NAME,
        cohort.getCohortId(),
        cdrVersion.getCdrVersionId(),
        PARTICIPANT_ID,
        testFilter)
      .getBody();

    assertResponse(response, expectedPageRequest, Arrays.asList(expectedDrug2), 2);
  }

  @Test
  public void getParticipantAllEventsPagination() throws Exception {
    PageRequest expectedPageRequest = new PageRequest()
      .page(0)
      .pageSize(1)
      .sortOrder(SortOrder.ASC)
      .sortColumn("startDate");

    stubMockFirecloudGetWorkspace();

    ReviewFilter testFilter = new ReviewFilter().domain(DomainType.ALL_EVENTS);
    testFilter.pageFilterType(PageFilterType.REVIEWFILTER);
    testFilter.page(0);
    testFilter.pageSize(1);

    //page 1 should have 1 item
    ParticipantDataListResponse response = controller
      .getParticipantData(
        NAMESPACE,
        NAME,
        cohort.getCohortId(),
        cdrVersion.getCdrVersionId(),
        PARTICIPANT_ID,
        testFilter)
      .getBody();

    assertResponse(response, expectedPageRequest, Arrays.asList(expectedAllEvents7), 8);

    //page 2 should have 1 item
    testFilter.page(1);
    expectedPageRequest.page(1);
    response = controller
      .getParticipantData(
        NAMESPACE,
        NAME,
        cohort.getCohortId(),
        cdrVersion.getCdrVersionId(),
        PARTICIPANT_ID,
        testFilter)
      .getBody();

    assertResponse(response, expectedPageRequest, Arrays.asList(expectedAllEvents8), 8);
  }

  @Test
  public void getParticipantAllEventsSorting() throws Exception {
    PageRequest expectedPageRequest = new PageRequest()
      .page(0)
      .pageSize(25)
      .sortOrder(SortOrder.ASC)
      .sortColumn("startDate");

    stubMockFirecloudGetWorkspace();

    ReviewFilter testFilter = new ReviewFilter().domain(DomainType.ALL_EVENTS);
    testFilter.pageFilterType(PageFilterType.REVIEWFILTER);

    //no sort order or column
    ParticipantDataListResponse response = controller
      .getParticipantData(
        NAMESPACE,
        NAME,
        cohort.getCohortId(),
        cdrVersion.getCdrVersionId(),
        PARTICIPANT_ID,
        testFilter)
      .getBody();

    assertResponse(response, expectedPageRequest,
      Arrays.asList(expectedAllEvents7,
        expectedAllEvents8,
        expectedAllEvents1,
        expectedAllEvents2,
        expectedAllEvents5,
        expectedAllEvents3,
        expectedAllEvents6,
        expectedAllEvents4), 8);

    //added sort order
    testFilter.sortOrder(SortOrder.DESC);
    expectedPageRequest.sortOrder(SortOrder.DESC);
    response = controller
      .getParticipantData(
        NAMESPACE,
        NAME,
        cohort.getCohortId(),
        cdrVersion.getCdrVersionId(),
        PARTICIPANT_ID,
        testFilter)
      .getBody();

    assertResponse(response, expectedPageRequest,
      Arrays.asList(expectedAllEvents6,
        expectedAllEvents4,
        expectedAllEvents5,
        expectedAllEvents3,
        expectedAllEvents2,
        expectedAllEvents1,
        expectedAllEvents8,
        expectedAllEvents7), 8);
  }

  private void assertResponse(ParticipantDataListResponse response, PageRequest expectedPageRequest, List<ParticipantData> expectedData, int totalCount) {
    List<ParticipantData> data = response.getItems();
    assertThat(response.getCount()).isEqualTo(totalCount);
    assertThat(response.getPageRequest()).isEqualTo(expectedPageRequest);
    assertThat(data.size()).isEqualTo(expectedData.size());
    int i = 0;
    for (ParticipantData actualData : data) {
      ParticipantData expected = expectedData.get(i++);
      if (expected instanceof Drug) {
        assertThat((Drug) actualData).isEqualTo((Drug) expected);
      } else if (expected instanceof Observation) {
        assertThat((Observation) actualData).isEqualTo((Observation) expected);
      } else if (expected instanceof Condition) {
        assertThat((Condition) actualData).isEqualTo((Condition) expected);
      } else if (expected instanceof Procedure) {
        assertThat((Procedure) actualData).isEqualTo((Procedure) expected);
      } else if (expected instanceof AllEvents) {
        assertThat((AllEvents) actualData).isEqualTo((AllEvents) expected);
      } else if (expected instanceof Measurement) {
        assertThat((Measurement) actualData).isEqualTo((Measurement) expected);
      } else if (expected instanceof PhysicalMeasurement) {
        assertThat((PhysicalMeasurement) actualData).isEqualTo((PhysicalMeasurement) expected);
      }
      assertThat(actualData.getDomainType()).isEqualTo(expected.getDomainType());
      assertThat(actualData.getItemDate()).isEqualTo(expected.getItemDate());
    }
  }

  private void stubMockFirecloudGetWorkspace() throws ApiException {
    WorkspaceResponse workspaceResponse = new WorkspaceResponse();
    workspaceResponse.setAccessLevel(WorkspaceAccessLevel.READER.toString());
    when(mockFireCloudService.getWorkspace(NAMESPACE, NAME)).thenReturn(workspaceResponse);
  }
}
