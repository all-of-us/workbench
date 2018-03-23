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
import org.pmiops.workbench.cohortreview.ConditionQueryBuilder;
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
import org.pmiops.workbench.model.PageFilterType;
import org.pmiops.workbench.model.ParticipantCondition;
import org.pmiops.workbench.model.ParticipantConditionsPageFilter;
import org.pmiops.workbench.model.SortOrder;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.testconfig.TestJpaConfig;
import org.pmiops.workbench.testconfig.TestWorkbenchConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
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
    private ParticipantCondition expected1;
    private ParticipantCondition expected2;
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
            ConditionQueryBuilder.class,
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
                "concept"
        );
    }

    @Before
    public void setUp() {
        expected1 = new ParticipantCondition()
                .itemDate("2008-07-22")
                .standardVocabulary("SNOMED")
                .standardName("SNOMED")
                .sourceValue("0020")
                .sourceVocabulary("ICD9CM")
                .sourceName("Typhoid and paratyphoid fevers");
        expected2 = new ParticipantCondition()
                .itemDate("2008-08-01")
                .standardVocabulary("SNOMED")
                .standardName("SNOMED")
                .sourceValue("0021")
                .sourceVocabulary("ICD9CM")
                .sourceName("Typhoid and paratyphoid fevers");

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
        stubMockFirecloudGetWorkspace();

        ParticipantConditionsPageFilter testFilter = new ParticipantConditionsPageFilter();
        testFilter.pageFilterType(PageFilterType.PARTICIPANTCONDITIONSPAGEFILTER);

        //no sort order or column
        List<ParticipantCondition> conditions = controller
                .getParticipantConditions(
                        NAMESPACE,
                        NAME,
                        cohort.getCohortId(),
                        cdrVersion.getCdrVersionId(),
                        PARTICIPANT_ID,
                        testFilter)
                .getBody()
                .getItems();
        assertThat(conditions.size()).isEqualTo(2);
        assertThat(conditions.get(0)).isEqualTo(expected1);
        assertThat(conditions.get(1)).isEqualTo(expected2);

        //added sort order
        testFilter.sortOrder(SortOrder.DESC);
        conditions = controller
                .getParticipantConditions(
                        NAMESPACE,
                        NAME,
                        cohort.getCohortId(),
                        cdrVersion.getCdrVersionId(),
                        PARTICIPANT_ID,
                        testFilter)
                .getBody()
                .getItems();
        assertThat(conditions.size()).isEqualTo(2);
        assertThat(conditions.get(0)).isEqualTo(expected2);
        assertThat(conditions.get(1)).isEqualTo(expected1);
    }

    @Test
    public void getParticipantConditionsPagination() throws Exception {
        stubMockFirecloudGetWorkspace();

        ParticipantConditionsPageFilter testFilter = new ParticipantConditionsPageFilter();
        testFilter.pageFilterType(PageFilterType.PARTICIPANTCONDITIONSPAGEFILTER);
        testFilter.page(0);
        testFilter.pageSize(1);

        //page 1 should have 1 item
        List<ParticipantCondition> conditions = controller
                .getParticipantConditions(
                        NAMESPACE,
                        NAME,
                        cohort.getCohortId(),
                        cdrVersion.getCdrVersionId(),
                        PARTICIPANT_ID,
                        testFilter)
                .getBody()
                .getItems();
        assertThat(conditions.size()).isEqualTo(1);
        assertThat(conditions.get(0)).isEqualTo(expected1);

        //page 2 should have 1 item
        testFilter.page(1);
        conditions = controller
                .getParticipantConditions(
                        NAMESPACE,
                        NAME,
                        cohort.getCohortId(),
                        cdrVersion.getCdrVersionId(),
                        PARTICIPANT_ID,
                        testFilter)
                .getBody()
                .getItems();
        assertThat(conditions.size()).isEqualTo(1);
        assertThat(conditions.get(0)).isEqualTo(expected2);
    }

    private void stubMockFirecloudGetWorkspace() throws ApiException {
        WorkspaceResponse workspaceResponse = new WorkspaceResponse();
        workspaceResponse.setAccessLevel(WorkspaceAccessLevel.READER.toString());
        when(mockFireCloudService.getWorkspace(NAMESPACE, NAME)).thenReturn(workspaceResponse);
    }
}
