package org.pmiops.workbench.api;

import org.bitbucket.radistao.test.runner.BeforeAfterSpringTestRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.cdr.cache.GenderRaceEthnicityConcept;
import org.pmiops.workbench.cdr.cache.GenderRaceEthnicityType;
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
public class CohortReviewControllerTest extends BigQueryBaseTest {

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
      concepts.put(GenderRaceEthnicityType.RACE.name(), new HashMap<>());
      concepts.put(GenderRaceEthnicityType.GENDER.name(), new HashMap<>());
      concepts.put(GenderRaceEthnicityType.ETHNICITY.name(), new HashMap<>());
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
      "participant_review"
    );
  }

  @Override
  public String getTestDataDirectory() {
    return CB_DATA;
  }

  @Before
  public void setUp() {
    LocalDate personBirthDate = LocalDate.of(1980, Month.FEBRUARY, 17);
    LocalDate conditionDate1 = LocalDate.of(2008, Month.JULY, 22);
    LocalDate conditionDate2 = LocalDate.of(2008, Month.AUGUST, 1);
    LocalDate procedureDate1 = LocalDate.of(2009, Month.DECEMBER, 2);
    LocalDate procedureDate2 = LocalDate.of(2009, Month.DECEMBER, 3);
    LocalDate observationDate1 = LocalDate.of(2009, Month.DECEMBER, 3);
    LocalDate observationDate2 = LocalDate.of(2009, Month.DECEMBER, 4);
    LocalDate drugDate1 = LocalDate.of(2001, Month.DECEMBER, 3);
    LocalDate drugDate2 = LocalDate.of(2001, Month.DECEMBER, 4);
    Period conditionAge1 = Period.between(personBirthDate, conditionDate1);
    Period conditionAge2 = Period.between(personBirthDate, conditionDate2);
    Period procedureAge1 = Period.between(personBirthDate, procedureDate1);
    Period procedureAge2 = Period.between(personBirthDate, procedureDate2);
    Period observationAge1 = Period.between(personBirthDate, observationDate1);
    Period observationAge2 = Period.between(personBirthDate, observationDate2);
    Period drugAge1 = Period.between(personBirthDate, drugDate1);
    Period drugAge2 = Period.between(personBirthDate, drugDate2);

    expectedMaster1 = new Master()
      .dataId(12751440L)
      .domain("Condition")
      .itemDate("2008-07-22 05:00:00 UTC")
      .standardVocabulary("SNOMED")
      .standardName("SNOMED")
      .sourceValue("0020")
      .sourceVocabulary("ICD9CM")
      .sourceName("Typhoid and paratyphoid fevers")
      .domainType(DomainType.MASTER);
    expectedMaster2 = new Master()
      .dataId(12751441L)
      .domain("Condition")
      .itemDate("2008-08-01 05:00:00 UTC")
      .standardVocabulary("SNOMED")
      .standardName("SNOMED")
      .sourceValue("0021")
      .sourceVocabulary("ICD9CM")
      .sourceName("Typhoid and paratyphoid fevers")
      .domainType(DomainType.MASTER);
    expectedMaster3 = new Master()
      .dataId(12751446L)
      .domain("Observation")
      .itemDate("2009-12-03 05:00:00 UTC")
      .standardVocabulary("ICD10CM")
      .standardName("name")
      .sourceValue("sourceValue")
      .sourceVocabulary("ICD10CM")
      .sourceName("name")
      .domainType(DomainType.MASTER);
    expectedMaster4 = new Master()
      .dataId(12751447L)
      .domain("Observation")
      .itemDate("2009-12-04 05:00:00 UTC")
      .standardVocabulary("ICD10CM")
      .standardName("name")
      .sourceValue("sourceValue")
      .sourceVocabulary("ICD10CM")
      .sourceName("name")
      .domainType(DomainType.MASTER);
    expectedMaster5 = new Master()
      .dataId(12751444L)
      .domain("Procedure")
      .itemDate("2009-12-03 05:00:00 UTC")
      .standardVocabulary("ICD10CM")
      .standardName("name")
      .sourceValue("val")
      .sourceVocabulary("ICD10CM")
      .sourceName("name")
      .domainType(DomainType.MASTER);
    expectedMaster6 = new Master()
      .dataId(12751445L)
      .domain("Procedure")
      .itemDate("2009-12-04 05:00:00 UTC")
      .standardVocabulary("CPT4")
      .standardName("name")
      .sourceValue("val")
      .sourceVocabulary("CPT4")
      .sourceName("name")
      .domainType(DomainType.MASTER);
    expectedMaster7 = new Master()
      .dataId(12751442L)
      .domain("Drug")
      .itemDate("2001-12-03 05:00:00 UTC")
      .standardVocabulary("CPT4")
      .standardName("name")
      .sourceValue("Varivax")
      .sourceVocabulary("CPT4")
      .sourceName("name")
      .domainType(DomainType.MASTER);
    expectedMaster8 = new Master()
      .dataId(12751443L)
      .domain("Drug")
      .itemDate("2001-12-04 05:00:00 UTC")
      .standardVocabulary("CPT4")
      .standardName("name")
      .sourceValue("Varivax")
      .sourceVocabulary("CPT4")
      .sourceName("name")
      .domainType(DomainType.MASTER);
    expectedCondition1 = new Condition()
      .age(conditionAge1.getYears())
      .itemDate("2008-07-22 05:00:00 UTC")
      .standardVocabulary("SNOMED")
      .standardName("SNOMED")
      .sourceValue("0020")
      .sourceVocabulary("ICD9CM")
      .sourceName("Typhoid and paratyphoid fevers")
      .domainType(DomainType.CONDITION);
    expectedCondition2 = new Condition()
      .age(conditionAge2.getYears())
      .itemDate("2008-08-01 05:00:00 UTC")
      .standardVocabulary("SNOMED")
      .standardName("SNOMED")
      .sourceValue("0021")
      .sourceVocabulary("ICD9CM")
      .sourceName("Typhoid and paratyphoid fevers")
      .domainType(DomainType.CONDITION);
    expectedProcedure1 = new Procedure()
      .age(procedureAge1.getYears())
      .itemDate("2009-12-03 05:00:00 UTC")
      .standardVocabulary("ICD10CM")
      .standardName("name")
      .sourceValue("val")
      .sourceVocabulary("ICD10CM")
      .sourceName("name")
      .domainType(DomainType.PROCEDURE);
    expectedProcedure2 = new Procedure()
      .age(procedureAge2.getYears())
      .itemDate("2009-12-04 05:00:00 UTC")
      .standardVocabulary("CPT4")
      .standardName("name")
      .sourceValue("val")
      .sourceVocabulary("CPT4")
      .sourceName("name")
      .domainType(DomainType.PROCEDURE);
    expectedObservation1 = new Observation()
      .age(observationAge1.getYears())
      .itemDate("2009-12-03 05:00:00 UTC")
      .standardVocabulary("ICD10CM")
      .standardName("name")
      .sourceValue("sourceValue")
      .sourceVocabulary("ICD10CM")
      .sourceName("name")
      .domainType(DomainType.OBSERVATION);
    expectedObservation2 = new Observation()
      .age(observationAge2.getYears())
      .itemDate("2009-12-04 05:00:00 UTC")
      .standardVocabulary("ICD10CM")
      .standardName("name")
      .sourceValue("sourceValue")
      .sourceVocabulary("ICD10CM")
      .sourceName("name")
      .domainType(DomainType.OBSERVATION);
    expectedDrug1 = new Drug()
      .age(drugAge1.getYears())
      .signature("signature")
      .itemDate("2001-12-03 05:00:00 UTC")
      .standardVocabulary("CPT4")
      .standardName("name")
      .sourceValue("Varivax")
      .sourceVocabulary("CPT4")
      .sourceName("name")
      .domainType(DomainType.DRUG);
    expectedDrug2 = new Drug()
      .age(drugAge2.getYears())
      .signature("signature")
      .itemDate("2001-12-04 05:00:00 UTC")
      .standardVocabulary("CPT4")
      .standardName("name")
      .sourceValue("Varivax")
      .sourceVocabulary("CPT4")
      .sourceName("name")
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
      .sortColumn("itemDate");

    stubMockFirecloudGetWorkspace();

    ReviewFilter testFilter = new ReviewFilter().domain("Condition");
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
      .sortColumn("itemDate");

    stubMockFirecloudGetWorkspace();

    ReviewFilter testFilter = new ReviewFilter().domain("Condition");
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
      .sortColumn("itemDate");

    stubMockFirecloudGetWorkspace();

    ReviewFilter testFilter = new ReviewFilter().domain("Procedure");
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
      .sortColumn("itemDate");

    stubMockFirecloudGetWorkspace();

    ReviewFilter testFilter = new ReviewFilter().domain("Procedure");
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
      .sortColumn("itemDate");

    stubMockFirecloudGetWorkspace();

    ReviewFilter testFilter = new ReviewFilter().domain("Observation");
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
      .sortColumn("itemDate");

    stubMockFirecloudGetWorkspace();

    ReviewFilter testFilter = new ReviewFilter().domain("Observation");
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
      .sortColumn("itemDate");

    stubMockFirecloudGetWorkspace();

    ReviewFilter testFilter = new ReviewFilter().domain("Drug");
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
      .sortColumn("itemDate");

    stubMockFirecloudGetWorkspace();

    ReviewFilter testFilter = new ReviewFilter().domain("Drug");
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
      .sortColumn("itemDate");

    stubMockFirecloudGetWorkspace();

    ReviewFilter testFilter = new ReviewFilter().domain("Master");
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
      .sortColumn("itemDate");

    stubMockFirecloudGetWorkspace();

    ReviewFilter testFilter = new ReviewFilter().domain("Master");
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
        assertThat(((Drug) actualData).getAge()).isEqualTo(((Drug) expected).getAge());
        assertThat(((Drug) actualData).getSignature()).isEqualTo(((Drug) expected).getSignature());
      } else if (expected instanceof Observation) {
        assertThat(((Observation) actualData).getAge()).isEqualTo(((Observation) expected).getAge());
      } else if (expected instanceof Condition) {
        assertThat(((Condition) actualData).getAge()).isEqualTo(((Condition) expected).getAge());
      } else if (expected instanceof Procedure) {
        assertThat(((Procedure) actualData).getAge()).isEqualTo(((Procedure) expected).getAge());
      } else if (expected instanceof Master) {
        assertThat(((Master) actualData).getDataId()).isEqualTo(((Master) expected).getDataId());
      }
      assertThat(actualData.getDomainType()).isEqualTo(expected.getDomainType());
      assertThat(actualData.getItemDate()).isEqualTo(expected.getItemDate());
      assertThat(actualData.getSourceName()).isEqualTo(expected.getSourceName());
      assertThat(actualData.getSourceValue()).isEqualTo(expected.getSourceValue());
      assertThat(actualData.getSourceVocabulary()).isEqualTo(expected.getSourceVocabulary());
      assertThat(actualData.getStandardName()).isEqualTo(expected.getStandardName());
      assertThat(actualData.getStandardVocabulary()).isEqualTo(expected.getStandardVocabulary());
    }
  }

  private void stubMockFirecloudGetWorkspace() throws ApiException {
    WorkspaceResponse workspaceResponse = new WorkspaceResponse();
    workspaceResponse.setAccessLevel(WorkspaceAccessLevel.READER.toString());
    when(mockFireCloudService.getWorkspace(NAMESPACE, NAME)).thenReturn(workspaceResponse);
  }
}
