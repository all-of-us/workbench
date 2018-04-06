package org.pmiops.workbench.api;

import org.bitbucket.radistao.test.runner.BeforeAfterSpringTestRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.cdr.cache.GenderRaceEthnicityConcept;
import org.pmiops.workbench.cdr.cache.GenderRaceEthnicityType;
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
public class CohortReviewControllerTest extends BigQueryBaseTest {

    private static final String NAMESPACE = "aou-test";
    private static final String NAME = "test";
    private static final Long PARTICIPANT_ID = 102246L;
    private static final FakeClock CLOCK = new FakeClock(Instant.now(), ZoneId.systemDefault());
    private ParticipantCondition expectedCondition1;
    private ParticipantCondition expectedCondition2;
    private ParticipantProcedure expectedProcedure1;
    private ParticipantProcedure expectedProcedure2;
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
            DomainLookupService.class
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
                "condition_occurrence",
                "procedure_occurrence",
                "person",
                "concept"
        );
    }

    @Before
    public void setUp() {
        LocalDate personBirthDate = LocalDate.of(1980, Month.FEBRUARY, 17);
        LocalDate procedureDate1 = LocalDate.of(2009, Month.DECEMBER, 2);
        LocalDate procedureDate2 = LocalDate.of(2009, Month.DECEMBER, 3);
        Period age1 = Period.between(personBirthDate, procedureDate1);
        Period age2 = Period.between(personBirthDate, procedureDate2);

        expectedCondition1 = new ParticipantCondition()
                .itemDate("2008-07-22")
                .standardVocabulary("SNOMED")
                .standardName("SNOMED")
                .sourceValue("0020")
                .sourceVocabulary("ICD9CM")
                .sourceName("Typhoid and paratyphoid fevers");
        expectedCondition2 = new ParticipantCondition()
                .itemDate("2008-08-01")
                .standardVocabulary("SNOMED")
                .standardName("SNOMED")
                .sourceValue("0021")
                .sourceVocabulary("ICD9CM")
                .sourceName("Typhoid and paratyphoid fevers");
        expectedProcedure1 = new ParticipantProcedure()
                .itemDate("2009-12-02")
                .standardVocabulary("ICD10CM")
                .standardName("name")
                .sourceValue("val")
                .sourceVocabulary("ICD10CM")
                .sourceName("name")
                .age(age1.getYears());
        expectedProcedure2 = new ParticipantProcedure()
                .itemDate("2009-12-03")
                .standardVocabulary("CPT4")
                .standardName("name")
                .sourceValue("val")
                .sourceVocabulary("CPT4")
                .sourceName("name")
                .age(age2.getYears());

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

        ParticipantConditions testFilter = new ParticipantConditions();
        testFilter.pageFilterType(PageFilterType.PARTICIPANTCONDITIONS);

        //no sort order or column
        ParticipantConditionsListResponse response = controller
                .getParticipantConditions(
                        NAMESPACE,
                        NAME,
                        cohort.getCohortId(),
                        cdrVersion.getCdrVersionId(),
                        PARTICIPANT_ID,
                        testFilter)
                .getBody();
        assertThat(response.getCount()).isEqualTo(2);
        assertThat(response.getPageRequest()).isEqualTo(expectedPageRequest);
        List<ParticipantCondition> conditions = response.getItems();
        assertThat(conditions.size()).isEqualTo(2);
        assertThat(conditions.get(0)).isEqualTo(expectedCondition1);
        assertThat(conditions.get(1)).isEqualTo(expectedCondition2);

        //added sort order
        testFilter.sortOrder(SortOrder.DESC);
        expectedPageRequest.sortOrder(SortOrder.DESC);
        response = controller
                .getParticipantConditions(
                        NAMESPACE,
                        NAME,
                        cohort.getCohortId(),
                        cdrVersion.getCdrVersionId(),
                        PARTICIPANT_ID,
                        testFilter)
                .getBody();
        assertThat(response.getCount()).isEqualTo(2);
        assertThat(response.getPageRequest()).isEqualTo(expectedPageRequest);
        conditions = response.getItems();
        assertThat(conditions.size()).isEqualTo(2);
        assertThat(conditions.get(0)).isEqualTo(expectedCondition2);
        assertThat(conditions.get(1)).isEqualTo(expectedCondition1);
    }

    @Test
    public void getParticipantConditionsPagination() throws Exception {
        PageRequest expectedPageRequest = new PageRequest()
                .page(0)
                .pageSize(1)
                .sortOrder(SortOrder.ASC)
                .sortColumn("itemDate");

        stubMockFirecloudGetWorkspace();

        ParticipantConditions testFilter = new ParticipantConditions();
        testFilter.pageFilterType(PageFilterType.PARTICIPANTCONDITIONS);
        testFilter.page(0);
        testFilter.pageSize(1);

        //page 1 should have 1 item
        ParticipantConditionsListResponse response =  controller
                .getParticipantConditions(
                        NAMESPACE,
                        NAME,
                        cohort.getCohortId(),
                        cdrVersion.getCdrVersionId(),
                        PARTICIPANT_ID,
                        testFilter)
                .getBody();
        assertThat(response.getCount()).isEqualTo(2);
        assertThat(response.getPageRequest()).isEqualTo(expectedPageRequest);
        List<ParticipantCondition> conditions = response.getItems();
        assertThat(conditions.size()).isEqualTo(1);
        assertThat(conditions.get(0)).isEqualTo(expectedCondition1);

        //page 2 should have 1 item
        testFilter.page(1);
        expectedPageRequest.page(1);
        response = controller
                .getParticipantConditions(
                        NAMESPACE,
                        NAME,
                        cohort.getCohortId(),
                        cdrVersion.getCdrVersionId(),
                        PARTICIPANT_ID,
                        testFilter)
                .getBody();
        assertThat(response.getCount()).isEqualTo(2);
        assertThat(response.getPageRequest()).isEqualTo(expectedPageRequest);
        conditions = response.getItems();
        assertThat(conditions.size()).isEqualTo(1);
        assertThat(conditions.get(0)).isEqualTo(expectedCondition2);
    }

    @Test
    public void getParticipantProceduresSorting() throws Exception {
        PageRequest expectedPageRequest = new PageRequest()
                .page(0)
                .pageSize(25)
                .sortOrder(SortOrder.ASC)
                .sortColumn("itemDate");

        stubMockFirecloudGetWorkspace();

        ParticipantProcedures testFilter = new ParticipantProcedures();
        testFilter.pageFilterType(PageFilterType.PARTICIPANTPROCEDURES);

        //no sort order or column
        ParticipantProceduresListResponse response = controller
                .getParticipantProcedures(
                        NAMESPACE,
                        NAME,
                        cohort.getCohortId(),
                        cdrVersion.getCdrVersionId(),
                        PARTICIPANT_ID,
                        testFilter)
                .getBody();
        assertThat(response.getCount()).isEqualTo(2);
        assertThat(response.getPageRequest()).isEqualTo(expectedPageRequest);
        List<ParticipantProcedure> procedures = response.getItems();
        assertThat(procedures.size()).isEqualTo(2);
        assertThat(procedures.get(0)).isEqualTo(expectedProcedure1);
        assertThat(procedures.get(1)).isEqualTo(expectedProcedure2);

        //added sort order
        testFilter.sortOrder(SortOrder.DESC);
        expectedPageRequest.sortOrder(SortOrder.DESC);
        response = controller
                .getParticipantProcedures(
                        NAMESPACE,
                        NAME,
                        cohort.getCohortId(),
                        cdrVersion.getCdrVersionId(),
                        PARTICIPANT_ID,
                        testFilter)
                .getBody();
        assertThat(response.getCount()).isEqualTo(2);
        assertThat(response.getPageRequest()).isEqualTo(expectedPageRequest);
        procedures = response.getItems();
        assertThat(procedures.size()).isEqualTo(2);
        assertThat(procedures.get(0)).isEqualTo(expectedProcedure2);
        assertThat(procedures.get(1)).isEqualTo(expectedProcedure1);
    }

    @Test
    public void getParticipantProceduresPagination() throws Exception {
        PageRequest expectedPageRequest = new PageRequest()
                .page(0)
                .pageSize(1)
                .sortOrder(SortOrder.ASC)
                .sortColumn("itemDate");

        stubMockFirecloudGetWorkspace();

        ParticipantProcedures testFilter = new ParticipantProcedures();
        testFilter.pageFilterType(PageFilterType.PARTICIPANTPROCEDURES);
        testFilter.page(0);
        testFilter.pageSize(1);

        //page 1 should have 1 item
        ParticipantProceduresListResponse response =  controller
                .getParticipantProcedures(
                        NAMESPACE,
                        NAME,
                        cohort.getCohortId(),
                        cdrVersion.getCdrVersionId(),
                        PARTICIPANT_ID,
                        testFilter)
                .getBody();
        assertThat(response.getCount()).isEqualTo(2);
        assertThat(response.getPageRequest()).isEqualTo(expectedPageRequest);
        List<ParticipantProcedure> procedures = response.getItems();
        assertThat(procedures.size()).isEqualTo(1);
        assertThat(procedures.get(0)).isEqualTo(expectedProcedure1);

        //page 2 should have 1 item
        testFilter.page(1);
        expectedPageRequest.page(1);
        response = controller
                .getParticipantProcedures(
                        NAMESPACE,
                        NAME,
                        cohort.getCohortId(),
                        cdrVersion.getCdrVersionId(),
                        PARTICIPANT_ID,
                        testFilter)
                .getBody();
        assertThat(response.getCount()).isEqualTo(2);
        assertThat(response.getPageRequest()).isEqualTo(expectedPageRequest);
        procedures = response.getItems();
        assertThat(procedures.size()).isEqualTo(1);
        assertThat(procedures.get(0)).isEqualTo(expectedProcedure2);
    }

    private void stubMockFirecloudGetWorkspace() throws ApiException {
        WorkspaceResponse workspaceResponse = new WorkspaceResponse();
        workspaceResponse.setAccessLevel(WorkspaceAccessLevel.READER.toString());
        when(mockFireCloudService.getWorkspace(NAMESPACE, NAME)).thenReturn(workspaceResponse);
    }
}
