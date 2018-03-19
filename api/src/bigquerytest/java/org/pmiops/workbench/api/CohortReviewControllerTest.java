package org.pmiops.workbench.api;

import org.bitbucket.radistao.test.runner.BeforeAfterSpringTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.pmiops.workbench.cdr.CdrVersionContext;
import org.pmiops.workbench.cdr.cache.GenderRaceEthnicityConcept;
import org.pmiops.workbench.cohortbuilder.ParticipantCounter;
import org.pmiops.workbench.cohortreview.CohortReviewService;
import org.pmiops.workbench.cohortreview.CohortReviewServiceImpl;
import org.pmiops.workbench.cohortreview.ConditionQueryBuilder;
import org.pmiops.workbench.db.dao.CohortAnnotationDefinitionDao;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.CohortReviewDao;
import org.pmiops.workbench.db.dao.ParticipantCohortAnnotationDao;
import org.pmiops.workbench.db.dao.ParticipantCohortStatusDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.dao.WorkspaceService;
import org.pmiops.workbench.db.model.CdrVersion;
import org.pmiops.workbench.db.model.Cohort;
import org.pmiops.workbench.db.model.CohortReview;
import org.pmiops.workbench.db.model.ParticipantCohortStatus;
import org.pmiops.workbench.db.model.ParticipantCohortStatusKey;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.model.PaginationFilteringRequest;
import org.pmiops.workbench.model.ParticipantCondition;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.testconfig.TestJpaConfig;
import org.pmiops.workbench.testconfig.TestWorkbenchConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import javax.inject.Provider;
import java.util.Arrays;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

@RunWith(BeforeAfterSpringTestRunner.class)
@Import({ConditionQueryBuilder.class, BigQueryService.class, TestJpaConfig.class})
public class CohortReviewControllerTest extends BigQueryBaseTest {

    private static final Long WORKSPACE_ID = 1L;
    private static final String NAMESPACE = "aou-test";
    private static final String NAME = "test";
    private static final Long CDR_VERSION_ID = 1L;
    private static final Long PARTICIPANT_ID = 102246L;
    private static final Long COHORT_REVIEW_ID = 1L;

    private CohortReviewController controller;

    private CdrVersion cdrVersion;

    @Autowired
    private BigQueryService bigQueryService;

    @Autowired
    private TestWorkbenchConfig testWorkbenchConfig;

    @Autowired
    private CohortDao cohortDao;

    @Autowired
    private CohortReviewDao cohortReviewDao;

    @Autowired
    private WorkspaceDao workspaceDao;

    @Autowired
    private ParticipantCohortStatusDao participantCohortStatusDao;

    @Autowired
    private ParticipantCohortAnnotationDao participantCohortAnnotationDao;

    @Autowired
    private CohortAnnotationDefinitionDao cohortAnnotationDefinitionDao;

    @Autowired
    private ConditionQueryBuilder conditionQueryBuilder;

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private Provider<GenderRaceEthnicityConcept> genderRaceEthnicityConceptProvider;

    @Mock
    private ParticipantCounter participantCounter;

    private Cohort cohort;

    @Override
    public List<String> getTableNames() {
        return Arrays.asList(
                "condition_occurrence",
                "concept"
        );
    }

    @Before
    public void setUp() {
        cdrVersion = new CdrVersion();
        cdrVersion.setCdrVersionId(CDR_VERSION_ID);
        cdrVersion.setBigqueryDataset(testWorkbenchConfig.bigquery.dataSetId);
        cdrVersion.setBigqueryProject(testWorkbenchConfig.bigquery.projectId);
        CdrVersionContext.setCdrVersion(cdrVersion);

        CohortReviewService cohortReviewService = new CohortReviewServiceImpl(cohortReviewDao,
                cohortDao,
                participantCohortStatusDao,
                participantCohortAnnotationDao,
                cohortAnnotationDefinitionDao,
                workspaceService,
                genderRaceEthnicityConceptProvider);

        controller = new CohortReviewController(cohortReviewService,
                bigQueryService,
                participantCounter,
                conditionQueryBuilder,
                genderRaceEthnicityConceptProvider);

        Workspace workspace = new Workspace();
        workspace.setWorkspaceId(WORKSPACE_ID);
        workspaceDao.save(workspace);

        cohort = new Cohort();
        cohort.setWorkspaceId(WORKSPACE_ID);
        cohortDao.save(cohort);

        CohortReview review = new CohortReview()
                .cdrVersionId(CDR_VERSION_ID)
                .cohortId(cohort.getCohortId());
        cohortReviewDao.save(review);

        ParticipantCohortStatusKey key = new ParticipantCohortStatusKey()
                .participantId(PARTICIPANT_ID)
                .cohortReviewId(COHORT_REVIEW_ID);
        ParticipantCohortStatus participantCohortStatus = new ParticipantCohortStatus()
                .participantKey(key);
        participantCohortStatusDao.save(participantCohortStatus);
    }

    @Test
    public void getParticipantConditions() throws Exception {
        Workspace workspace = new Workspace();
        workspace.setWorkspaceId(WORKSPACE_ID);
        when(workspaceService.enforceWorkspaceAccessLevel(NAMESPACE, NAME, WorkspaceAccessLevel.READER))
                .thenReturn(WorkspaceAccessLevel.fromValue(WorkspaceAccessLevel.READER.name()));
        when(workspaceService.getRequired(NAMESPACE, NAME)).thenReturn(workspace);

        ParticipantCondition expected = new ParticipantCondition()
                .itemDate("2008-07-22")
                .standardVocabulary("SNOMED")
                .standardName("SNOMED")
                .sourceValue("0020")
                .sourceVocabulary("ICD9CM")
                .sourceName("Typhoid and paratyphoid fevers");

        List<ParticipantCondition> conditions = controller
                .getParticipantConditions(
                        NAMESPACE,
                        NAME,
                        cohort.getCohortId(),
                        CDR_VERSION_ID,
                        PARTICIPANT_ID,
                        new PaginationFilteringRequest())
                .getBody()
                .getItems();
        assertThat(conditions.size()).isEqualTo(1);
        assertThat(conditions.get(0)).isEqualTo(expected);
    }
}
