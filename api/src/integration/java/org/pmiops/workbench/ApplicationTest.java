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
import org.pmiops.workbench.cdr.CdrVersionContext;
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
import org.pmiops.workbench.db.model.CdrVersion;
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
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {TestWebMvcConfig.class, TestCdrDbConfig.class})
public class ApplicationTest {

    @Autowired
    private ApplicationContext context;

    @Test
    public void contextLoads() throws Exception {
        CdrVersion cdrVersion = new CdrVersion();
        cdrVersion.setCdrVersionId(1L);
        CdrVersionContext.setCdrVersion(cdrVersion);
        List<String> beansToNotCheck =
          Arrays.asList(
          "userAuthentication",
            "userInfo",
            "user",
            "endUserApiClient",
            "allOfUsApiClient",
            "profileApi",
            "workspacesApi",
            "billingApi",
            "groupsApi",
            "notebooksApiClient",
            "clusterApi",
            "notebooksApi",
            "jupyterApi");

        String[] allBeanNames = context.getBeanDefinitionNames();
        for (String beanName : allBeanNames) {
            if (!beansToNotCheck.contains(beanName)) {
                System.out.println(beanName);
                assertThat(context.getBean(beanName)).isNotNull();
            }
        }
    }
}
