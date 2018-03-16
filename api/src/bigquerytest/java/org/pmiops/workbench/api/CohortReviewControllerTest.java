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
import org.pmiops.workbench.db.dao.WorkspaceService;
import org.pmiops.workbench.db.model.CdrVersion;
import org.pmiops.workbench.db.model.Cohort;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.model.ParticipantCondition;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.testconfig.TestJpaConfig;
import org.pmiops.workbench.testconfig.TestWorkbenchConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import javax.inject.Provider;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.when;

@RunWith(BeforeAfterSpringTestRunner.class)
@Import({TestJpaConfig.class})
public class CohortReviewControllerTest extends BigQueryBaseTest {

    private static final Long WORKSPACE_ID = 1L;
    private static final String NAMESPACE = "aou-test";
    private static final String NAME = "test";
    private static final Long CDR_VERSION_ID = 1L;

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
        return Arrays.asList("condition_occurrence");
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

        cohort = new Cohort();
        cohort.setWorkspaceId(WORKSPACE_ID);
        cohortDao.save(cohort);

//        icd9ConditionParent = criteriaDao.save(createCriteriaParent(TYPE_ICD9, SUBTYPE_NONE,"001"));
//        icd9ConditionChild = criteriaDao.save(createCriteriaChild(TYPE_ICD9, SUBTYPE_NONE, icd9ConditionParent.getId(), "001", DOMAIN_CONDITION));
//        icd9ProcedureParent = criteriaDao.save(createCriteriaParent(TYPE_ICD9, SUBTYPE_NONE,"002"));
//        icd9ProcedureChild = criteriaDao.save(createCriteriaChild(TYPE_ICD9, SUBTYPE_NONE, icd9ProcedureParent.getId(), "002", DOMAIN_PROCEDURE));
//        icd9MeasurementParent = criteriaDao.save(createCriteriaParent(TYPE_ICD9, SUBTYPE_NONE,"003"));
//        icd9MeasurementChild = criteriaDao.save(createCriteriaChild(TYPE_ICD9, SUBTYPE_NONE, icd9MeasurementParent.getId(), "003", DOMAIN_MEASUREMENT));
//        icd10ConditionParent = criteriaDao.save(createCriteriaParent(TYPE_ICD10, SUBTYPE_ICD10CM,"A"));
//        icd10ConditionChild = criteriaDao.save(createCriteriaChild(TYPE_ICD10, SUBTYPE_ICD10CM, icd10ConditionParent.getId(), "A09", DOMAIN_CONDITION));
//        icd10ProcedureParent = criteriaDao.save(createCriteriaParent(TYPE_ICD10, SUBTYPE_ICD10PCS,"16"));
//        icd10ProcedureChild = criteriaDao.save(createCriteriaChild(TYPE_ICD10, SUBTYPE_ICD10PCS, icd10ProcedureParent.getId(), "16070", DOMAIN_PROCEDURE));
//        icd10MeasurementParent = criteriaDao.save(createCriteriaParent(TYPE_ICD10, SUBTYPE_ICD10CM,"R92"));
//        icd10MeasurementChild = criteriaDao.save(createCriteriaChild(TYPE_ICD10, SUBTYPE_ICD10CM, icd10MeasurementParent.getId(), "R92.2", DOMAIN_MEASUREMENT));
//        cptProcedure = criteriaDao.save(createCriteriaChild(TYPE_CPT, SUBTYPE_CPT4, 1L, "0001T", DOMAIN_PROCEDURE));
//        cptObservation = criteriaDao.save(createCriteriaChild(TYPE_CPT, SUBTYPE_CPT4, 1L, "0001Z", DOMAIN_OBSERVATION));
//        cptMeasurement = criteriaDao.save(createCriteriaChild(TYPE_CPT, SUBTYPE_CPT4, 1L, "0001Q", DOMAIN_MEASUREMENT));
//        cptDrug = criteriaDao.save(createCriteriaChild(TYPE_CPT, SUBTYPE_CPT4, 1L, "90703", DOMAIN_DRUG));
    }

    @Test
    public void getParticipantConditions() throws Exception {
        Workspace workspace = new Workspace();
        workspace.setWorkspaceId(WORKSPACE_ID);
        when(workspaceService.enforceWorkspaceAccessLevel(NAMESPACE, NAME, WorkspaceAccessLevel.READER))
                .thenReturn(WorkspaceAccessLevel.fromValue(WorkspaceAccessLevel.READER.name()));
        when(workspaceService.getRequired(NAMESPACE, NAME)).thenReturn(workspace);

        List<ParticipantCondition> conditions = controller
                .getParticipantConditions(NAMESPACE, NAME, cohort.getCohortId(), CDR_VERSION_ID,102246L)
                .getBody()
                .getItems();
    }
}
