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
import org.pmiops.workbench.cohortreview.ReviewTabQueryBuilder;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.CohortReviewDao;
import org.pmiops.workbench.db.dao.CohortService;
import org.pmiops.workbench.db.dao.ParticipantCohortStatusDao;
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
  private ParticipantData expectedProcedure1;
  private ParticipantData expectedProcedure2;
  private ParticipantData expectedObservation1;
  private ParticipantData expectedObservation2;
  private ParticipantData expectedDrug1;
  private ParticipantData expectedDrug2;
  private ParticipantData expectedMaster1;
  private ParticipantData expectedMaster2;
  private ParticipantData expectedMaster3;
  private ParticipantData expectedMaster4;
  private ParticipantData expectedMaster5;
  private ParticipantData expectedMaster6;
  private ParticipantData expectedMaster7;
  private ParticipantData expectedMaster8;
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
    ReviewTabQueryBuilder.class,
    CohortService.class,
    ParticipantCounter.class,
    DomainLookupService.class,
    CohortQueryBuilder.class
  })
  @MockBean({
    FireCloudService.class
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
      "p_physical_measurement"
    );
  }

  @Override
  public String getTestDataDirectory() {
    return CB_DATA;
  }

  @Before
  public void setUp() {

    expectedMaster1 = new Master()
      .dataId(12751440L)
      .domain("Condition")
      .standardVocabulary("SNOMED")
      .standardName("SNOMED")
      .standardCode("code")
      .sourceValue("0020")
      .sourceVocabulary("ICD9CM")
      .sourceName("Typhoid and paratyphoid fevers")
      .numMentions(11)
      .firstMention("2008-07-22 05:00:00 UTC")
      .lastMention("2018-03-22 05:00:00 UTC")
      .visitType("visitType")
      .itemDate("2008-07-22 05:00:00 UTC")
      .domainType(DomainType.MASTER);
    expectedMaster2 = new Master()
      .dataId(12751441L)
      .domain("Condition")
      .standardVocabulary("SNOMED")
      .standardName("SNOMED")
      .standardCode("code")
      .sourceValue("0021")
      .sourceVocabulary("ICD9CM")
      .sourceName("Typhoid and paratyphoid fevers")
      .numMentions(11)
      .firstMention("2008-07-22 05:00:00 UTC")
      .lastMention("2018-03-22 05:00:00 UTC")
      .visitType("visitType")
      .itemDate("2008-08-01 05:00:00 UTC")
      .domainType(DomainType.MASTER);
    expectedMaster3 = new Master()
      .dataId(12751446L)
      .domain("Observation")
      .standardVocabulary("ICD10CM")
      .standardName("name")
      .standardCode("code")
      .sourceValue("sourceValue")
      .sourceVocabulary("ICD10CM")
      .sourceName("name")
      .numMentions(11)
      .firstMention("2008-07-22 05:00:00 UTC")
      .lastMention("2018-03-22 05:00:00 UTC")
      .visitType("visitType")
      .itemDate("2009-12-03 05:00:00 UTC")
      .domainType(DomainType.MASTER);
    expectedMaster4 = new Master()
      .dataId(12751447L)
      .domain("Observation")
      .standardVocabulary("ICD10CM")
      .standardName("name")
      .standardCode("code")
      .sourceValue("sourceValue")
      .sourceVocabulary("ICD10CM")
      .sourceName("name")
      .numMentions(11)
      .firstMention("2008-07-22 05:00:00 UTC")
      .lastMention("2018-03-22 05:00:00 UTC")
      .visitType("visitType")
      .itemDate("2009-12-04 05:00:00 UTC")
      .domainType(DomainType.MASTER);
    expectedMaster5 = new Master()
      .dataId(12751444L)
      .domain("Procedure")
      .standardVocabulary("ICD10CM")
      .standardName("name")
      .standardCode("code")
      .sourceValue("val")
      .sourceVocabulary("ICD10CM")
      .sourceName("name")
      .numMentions(11)
      .firstMention("2008-07-22 05:00:00 UTC")
      .lastMention("2018-03-22 05:00:00 UTC")
      .visitType("visitType")
      .itemDate("2009-12-03 05:00:00 UTC")
      .domainType(DomainType.MASTER);
    expectedMaster6 = new Master()
      .dataId(12751445L)
      .domain("Procedure")
      .standardVocabulary("CPT4")
      .standardName("name")
      .standardCode("code")
      .sourceValue("val")
      .sourceVocabulary("CPT4")
      .sourceName("name")
      .numMentions(11)
      .firstMention("2008-07-22 05:00:00 UTC")
      .lastMention("2018-03-22 05:00:00 UTC")
      .visitType("visitType")
      .itemDate("2009-12-04 05:00:00 UTC")
      .domainType(DomainType.MASTER);
    expectedMaster7 = new Master()
      .dataId(12751442L)
      .domain("Drug")
      .standardVocabulary("CPT4")
      .standardName("name")
      .standardCode("code")
      .sourceValue("Varivax")
      .sourceVocabulary("CPT4")
      .sourceName("name")
      .numMentions(11)
      .firstMention("2008-07-22 05:00:00 UTC")
      .lastMention("2018-03-22 05:00:00 UTC")
      .visitType("visitType")
      .itemDate("2001-12-03 05:00:00 UTC")
      .domainType(DomainType.MASTER);
    expectedMaster8 = new Master()
      .dataId(12751443L)
      .domain("Drug")
      .standardVocabulary("CPT4")
      .standardName("name")
      .standardCode("code")
      .sourceValue("Varivax")
      .sourceVocabulary("CPT4")
      .sourceName("name")
      .numMentions(11)
      .firstMention("2008-07-22 05:00:00 UTC")
      .lastMention("2018-03-22 05:00:00 UTC")
      .visitType("visitType")
      .itemDate("2001-12-04 05:00:00 UTC")
      .domainType(DomainType.MASTER);
    expectedCondition1 = new Condition()
      .ageAtEvent(28)
      .standardVocabulary("SNOMED")
      .standardName("SNOMED")
      .standardCode("code")
      .sourceCode("0020")
      .sourceVocabulary("ICD9CM")
      .sourceName("Typhoid and paratyphoid fevers")
      .itemDate("2008-07-22 05:00:00 UTC")
      .domainType(DomainType.CONDITION);
    expectedCondition2 = new Condition()
      .ageAtEvent(28)
      .standardVocabulary("SNOMED")
      .standardName("SNOMED")
      .standardCode("code")
      .sourceCode("0021")
      .sourceVocabulary("ICD9CM")
      .sourceName("Typhoid and paratyphoid fevers")
      .itemDate("2008-08-01 05:00:00 UTC")
      .domainType(DomainType.CONDITION);
    expectedProcedure1 = new Procedure()
      .ageAtEvent(29)
      .standardVocabulary("ICD10CM")
      .standardName("name")
      .standardCode("code")
      .sourceCode("val")
      .sourceVocabulary("ICD10CM")
      .sourceName("name")
      .itemDate("2009-12-03 05:00:00 UTC")
      .domainType(DomainType.PROCEDURE);
    expectedProcedure2 = new Procedure()
      .ageAtEvent(29)
      .standardVocabulary("CPT4")
      .standardName("name")
      .standardCode("code")
      .sourceCode("val")
      .sourceVocabulary("CPT4")
      .sourceName("name")
      .itemDate("2009-12-04 05:00:00 UTC")
      .domainType(DomainType.PROCEDURE);
    expectedObservation1 = new Observation()
      .ageAtEvent(29)
      .standardVocabulary("ICD10CM")
      .standardName("name")
      .standardCode("code")
      .sourceCode("sourceValue")
      .sourceVocabulary("ICD10CM")
      .sourceName("name")
      .itemDate("2009-12-03 05:00:00 UTC")
      .domainType(DomainType.OBSERVATION);
    expectedObservation2 = new Observation()
      .ageAtEvent(29)
      .standardVocabulary("ICD10CM")
      .standardName("name")
      .standardCode("code")
      .sourceCode("sourceValue")
      .sourceVocabulary("ICD10CM")
      .sourceName("name")
      .itemDate("2009-12-04 05:00:00 UTC")
      .domainType(DomainType.OBSERVATION);
    expectedDrug1 = new Drug()
      .ageAtEvent(21)
      .sourceVocabulary("CPT4")
      .standardName("name")
      .standardCode("code")
      .sourceCode("Varivax")
      .sourceVocabulary("CPT4")
      .sourceName("name")
      .itemDate("2001-12-03 05:00:00 UTC")
      .domainType(DomainType.DRUG);
    expectedDrug2 = new Drug()
      .ageAtEvent(21)
      .sourceVocabulary("CPT4")
      .standardName("name")
      .standardCode("code")
      .sourceCode("Varivax")
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
  public void getParticipantMasterPagination() throws Exception {
    PageRequest expectedPageRequest = new PageRequest()
      .page(0)
      .pageSize(1)
      .sortOrder(SortOrder.ASC)
      .sortColumn("startDate");

    stubMockFirecloudGetWorkspace();

    ReviewFilter testFilter = new ReviewFilter().domain(DomainType.MASTER);
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

    assertResponse(response, expectedPageRequest, Arrays.asList(expectedMaster7), 8);

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

    assertResponse(response, expectedPageRequest, Arrays.asList(expectedMaster8), 8);
  }

  @Test
  public void getParticipantMasterSorting() throws Exception {
    PageRequest expectedPageRequest = new PageRequest()
      .page(0)
      .pageSize(25)
      .sortOrder(SortOrder.ASC)
      .sortColumn("startDate");

    stubMockFirecloudGetWorkspace();

    ReviewFilter testFilter = new ReviewFilter().domain(DomainType.MASTER);
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
      Arrays.asList(expectedMaster7,
        expectedMaster8,
        expectedMaster1,
        expectedMaster2,
        expectedMaster5,
        expectedMaster3,
        expectedMaster6,
        expectedMaster4), 8);

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
      Arrays.asList(expectedMaster6,
        expectedMaster4,
        expectedMaster5,
        expectedMaster3,
        expectedMaster2,
        expectedMaster1,
        expectedMaster8,
        expectedMaster7), 8);
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
        assertThat(((Drug) actualData).getAgeAtEvent()).isEqualTo(((Drug) expected).getAgeAtEvent());
        assertThat(((Drug) actualData).getSourceName()).isEqualTo(((Drug) expected).getSourceName());
        assertThat(((Drug) actualData).getSourceVocabulary()).isEqualTo(((Drug) expected).getSourceVocabulary());
        assertThat(((Drug) actualData).getStandardName()).isEqualTo(((Drug) expected).getStandardName());
      } else if (expected instanceof Observation) {
        assertThat(((Observation) actualData).getAgeAtEvent()).isEqualTo(((Observation) expected).getAgeAtEvent());
        assertThat(((Observation) actualData).getSourceName()).isEqualTo(((Observation) expected).getSourceName());
        assertThat(((Observation) actualData).getSourceVocabulary()).isEqualTo(((Observation) expected).getSourceVocabulary());
        assertThat(((Observation) actualData).getStandardName()).isEqualTo(((Observation) expected).getStandardName());
        assertThat(((Observation) actualData).getStandardVocabulary()).isEqualTo(((Observation) expected).getStandardVocabulary());
      } else if (expected instanceof Condition) {
        assertThat(((Condition) actualData).getAgeAtEvent()).isEqualTo(((Condition) expected).getAgeAtEvent());
        assertThat(((Condition) actualData).getSourceName()).isEqualTo(((Condition) expected).getSourceName());
        assertThat(((Condition) actualData).getSourceVocabulary()).isEqualTo(((Condition) expected).getSourceVocabulary());
        assertThat(((Condition) actualData).getStandardName()).isEqualTo(((Condition) expected).getStandardName());
        assertThat(((Condition) actualData).getStandardVocabulary()).isEqualTo(((Condition) expected).getStandardVocabulary());
      } else if (expected instanceof Procedure) {
        assertThat(((Procedure) actualData).getAgeAtEvent()).isEqualTo(((Procedure) expected).getAgeAtEvent());
        assertThat(((Procedure) actualData).getSourceName()).isEqualTo(((Procedure) expected).getSourceName());
        assertThat(((Procedure) actualData).getSourceVocabulary()).isEqualTo(((Procedure) expected).getSourceVocabulary());
        assertThat(((Procedure) actualData).getStandardName()).isEqualTo(((Procedure) expected).getStandardName());
        assertThat(((Procedure) actualData).getStandardVocabulary()).isEqualTo(((Procedure) expected).getStandardVocabulary());
      } else if (expected instanceof Master) {
        assertThat(((Master) actualData).getDataId()).isEqualTo(((Master) expected).getDataId());
        assertThat(((Master) actualData).getSourceName()).isEqualTo(((Master) expected).getSourceName());
        assertThat(((Master) actualData).getSourceValue()).isEqualTo(((Master) expected).getSourceValue());
        assertThat(((Master) actualData).getSourceVocabulary()).isEqualTo(((Master) expected).getSourceVocabulary());
        assertThat(((Master) actualData).getStandardName()).isEqualTo(((Master) expected).getStandardName());
        assertThat(((Master) actualData).getStandardVocabulary()).isEqualTo(((Master) expected).getStandardVocabulary());
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
