package org.pmiops.workbench;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.api.AuditController;
import org.pmiops.workbench.api.AuthDomainController;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.api.BugReportController;
import org.pmiops.workbench.api.ClusterController;
import org.pmiops.workbench.api.CohortAnnotationDefinitionController;
import org.pmiops.workbench.api.CohortBuilderController;
import org.pmiops.workbench.api.CohortReviewController;
import org.pmiops.workbench.api.CohortsController;
import org.pmiops.workbench.api.ConfigController;
import org.pmiops.workbench.api.DomainLookupService;
import org.pmiops.workbench.api.ProfileController;
import org.pmiops.workbench.api.StatusController;
import org.pmiops.workbench.api.WorkspacesController;
import org.pmiops.workbench.auth.ProfileService;
import org.pmiops.workbench.auth.UserInfoService;
import org.pmiops.workbench.blockscore.BlockscoreService;
import org.pmiops.workbench.cdr.CdrDbConfig;
import org.pmiops.workbench.cdr.dao.CriteriaDao;
import org.pmiops.workbench.cohortbuilder.ParticipantCounter;
import org.pmiops.workbench.cohortbuilder.QueryBuilderFactory;
import org.pmiops.workbench.cohortbuilder.querybuilder.CodesQueryBuilder;
import org.pmiops.workbench.cohortbuilder.querybuilder.DemoQueryBuilder;
import org.pmiops.workbench.cohortreview.CohortReviewService;
import org.pmiops.workbench.cohorts.CohortMaterializationService;
import org.pmiops.workbench.config.BigQueryConfig;
import org.pmiops.workbench.config.CacheSpringConfiguration;
import org.pmiops.workbench.config.CdrConfig;
import org.pmiops.workbench.config.CommonConfig;
import org.pmiops.workbench.config.ConceptCacheConfiguration;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.config.WorkbenchEnvironment;
import org.pmiops.workbench.db.WorkbenchDbConfig;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.CohortAnnotationDefinitionDao;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.CohortReviewDao;
import org.pmiops.workbench.db.dao.CohortService;
import org.pmiops.workbench.db.dao.ConfigDao;
import org.pmiops.workbench.db.dao.ParticipantCohortAnnotationDao;
import org.pmiops.workbench.db.dao.ParticipantCohortStatusDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.dao.WorkspaceService;
import org.pmiops.workbench.firecloud.FireCloudConfig;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.google.DirectoryService;
import org.pmiops.workbench.interceptors.AuthInterceptor;
import org.pmiops.workbench.interceptors.ClearCdrVersionContextInterceptor;
import org.pmiops.workbench.interceptors.CronInterceptor;
import org.pmiops.workbench.mailchimp.MailChimpService;
import org.pmiops.workbench.notebooks.NotebooksConfig;
import org.pmiops.workbench.notebooks.NotebooksService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {TestWebMvcConfig.class})
public class ApplicationTest {

    @Autowired
    private AuditController auditController;
    @Autowired
    private AuthDomainController authDomainController;
    @Autowired
    private AuthInterceptor authInterceptor;
    @Autowired
    private BigQueryConfig bigQueryConfig;
    @Autowired
    private BigQueryService bigQueryService;
    @Autowired
    private BlockscoreService blockscoreService;
    @Autowired
    private BugReportController bugReportController;
    @Autowired
    private CacheSpringConfiguration cacheSpringConfiguration;
    @Autowired
    private CdrConfig cdrConfig;
    @Autowired
    private CdrDbConfig cdrDbConfig;
    @Autowired
    private CdrVersionDao cdrVersionDao;
    @Autowired
    private ClearCdrVersionContextInterceptor clearCdrVersionContextInterceptor;
    @Autowired
    private ClusterController clusterController;
    @Autowired
    private CloudStorageService cloudStorageService;
    @Autowired
    private CodesQueryBuilder codesQueryBuilder;
    @Autowired
    private CohortAnnotationDefinitionController cohortAnnotationDefinitionController;
    @Autowired
    private CohortAnnotationDefinitionDao cohortAnnotationDefinitionDao;
    @Autowired
    private CohortBuilderController cohortBuilderController;
    @Autowired
    private CohortDao cohortDao;
    @Autowired
    private CohortMaterializationService cohortMaterializationService;
    @Autowired
    private CohortReviewController cohortReviewController;
    @Autowired
    private CohortReviewDao cohortReviewDao;
    @Autowired
    private CohortReviewService cohortReviewService;
    @Autowired
    private CohortsController cohortsController;
    @Autowired
    private CohortService cohortService;
    @Autowired
    private CommonConfig commonConfig;
    @Autowired
    private ConceptCacheConfiguration conceptCacheConfiguration;
    @Autowired
    private ConfigController configController;
    @Autowired
    private ConfigDao configDao;
    @Autowired
    private CriteriaDao criteriaDao;
    @Autowired
    private CronInterceptor cronInterceptor;
    @Autowired
    private DomainLookupService domainLookupService;
    @Autowired
    private DemoQueryBuilder demoQueryBuilder;
    @Autowired
    private DirectoryService directoryService;
    @Autowired
    private FireCloudConfig fireCloudConfig;
    @Autowired
    private FireCloudService fireCloudService;
    @Autowired
    private MailChimpService mailChimpService;
    @Autowired
    private NotebooksConfig notebooksConfig;
    @Autowired
    private NotebooksService notebooksService;
    @Autowired
    private ParticipantCohortAnnotationDao participantCohortAnnotationDao;
    @Autowired
    private ParticipantCohortStatusDao participantCohortStatusDao;
    @Autowired
    private ParticipantCounter participantCounter;
    @Autowired
    private ProfileController profileController;
    @Autowired
    private ProfileService profileService;
    @Autowired
    private StatusController statusController;
    @Autowired
    private UserDao userDao;
    @Autowired
    private UserInfoService userInfoService;
    @Autowired
    private UserService userService;
    @Autowired
    private QueryBuilderFactory queryBuilderFactory;
    @Autowired
    private WorkbenchConfig workbenchConfig;
    @Autowired
    private WorkbenchDbConfig workbenchDbConfig;
    @Autowired
    private WorkbenchEnvironment workbenchEnvironment;
    @Autowired
    private WorkspaceDao workspaceDao;
    @Autowired
    private WorkspacesController workspacesController;
    @Autowired
    private WorkspaceService workspaceService;

    @Test
    public void contextLoads() throws Exception {
        assertThat(auditController).isNotNull();
        assertThat(authDomainController).isNotNull();
        assertThat(authInterceptor).isNotNull();
        assertThat(bigQueryConfig).isNotNull();
        assertThat(bigQueryService).isNotNull();
        assertThat(blockscoreService).isNotNull();
        assertThat(bugReportController).isNotNull();
        assertThat(cacheSpringConfiguration).isNotNull();
        assertThat(cdrConfig).isNotNull();
        assertThat(cdrDbConfig).isNotNull();
        assertThat(cdrVersionDao).isNotNull();
        assertThat(clearCdrVersionContextInterceptor).isNotNull();
        assertThat(clusterController).isNotNull();
        assertThat(cloudStorageService).isNotNull();
        assertThat(codesQueryBuilder).isNotNull();
        assertThat(cohortAnnotationDefinitionController).isNotNull();
        assertThat(cohortAnnotationDefinitionDao).isNotNull();
        assertThat(cohortBuilderController).isNotNull();
        assertThat(cohortDao).isNotNull();
        assertThat(cohortMaterializationService).isNotNull();
        assertThat(cohortReviewController).isNotNull();
        assertThat(cohortReviewDao).isNotNull();
        assertThat(cohortReviewService).isNotNull();
        assertThat(cohortsController).isNotNull();
        assertThat(cohortService).isNotNull();
        assertThat(commonConfig).isNotNull();
        assertThat(conceptCacheConfiguration).isNotNull();
        assertThat(configController).isNotNull();
        assertThat(configDao).isNotNull();
        assertThat(criteriaDao).isNotNull();
        assertThat(cronInterceptor).isNotNull();
        assertThat(domainLookupService).isNotNull();
        assertThat(demoQueryBuilder).isNotNull();
        assertThat(directoryService).isNotNull();
        assertThat(fireCloudConfig).isNotNull();
        assertThat(fireCloudService).isNotNull();
        assertThat(mailChimpService).isNotNull();
        assertThat(notebooksConfig).isNotNull();
        assertThat(notebooksService).isNotNull();
        assertThat(participantCohortAnnotationDao).isNotNull();
        assertThat(participantCohortStatusDao).isNotNull();
        assertThat(participantCounter).isNotNull();
        assertThat(profileController).isNotNull();
        assertThat(profileService).isNotNull();
        assertThat(statusController).isNotNull();
        assertThat(userDao).isNotNull();
        assertThat(userInfoService).isNotNull();
        assertThat(userService).isNotNull();
        assertThat(queryBuilderFactory).isNotNull();
        assertThat(workbenchConfig).isNotNull();
        assertThat(workbenchDbConfig).isNotNull();
        assertThat(workbenchEnvironment).isNotNull();
        assertThat(workspaceDao).isNotNull();
        assertThat(workspacesController).isNotNull();
        assertThat(workspaceService).isNotNull();
    }
}
