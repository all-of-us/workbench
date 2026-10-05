package org.pmiops.workbench.api;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.pmiops.workbench.exfiltration.ExfiltrationUtils.EGRESS_OBJECT_LENGTHS_SERVICE_QUALIFIER;
import static org.pmiops.workbench.utils.TestMockFactory.createDefaultCdrVersion;
import static org.pmiops.workbench.utils.TestMockFactory.createRegisteredTier;

import com.google.api.services.cloudbilling.model.ProjectBillingInfo;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.access.AccessTierService;
import org.pmiops.workbench.actionaudit.ActionAuditQueryService;
import org.pmiops.workbench.actionaudit.auditors.AdminAuditor;
import org.pmiops.workbench.actionaudit.auditors.BillingProjectAuditor;
import org.pmiops.workbench.actionaudit.auditors.LeonardoRuntimeAuditor;
import org.pmiops.workbench.actionaudit.auditors.WorkspaceAuditor;
import org.pmiops.workbench.actionaudit.bucket.BucketAuditQueryService;
import org.pmiops.workbench.actionaudit.bucket.BucketAuditQueryServiceImpl;
import org.pmiops.workbench.cdr.CdrVersionService;
import org.pmiops.workbench.cdr.ConceptBigQueryService;
import org.pmiops.workbench.cloudtasks.TaskQueueService;
import org.pmiops.workbench.config.CdrBigQuerySchemaConfigService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.AccessTierDao;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.CohortReviewDao;
import org.pmiops.workbench.db.dao.ConceptSetDao;
import org.pmiops.workbench.db.dao.DataSetDao;
import org.pmiops.workbench.db.dao.FeaturedWorkspaceDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserRecentWorkspaceDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.dao.WorkspaceFreeTierUsageDao;
import org.pmiops.workbench.db.dao.WorkspaceOperationDao;
import org.pmiops.workbench.db.jdbc.ReportingQueryService;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exfiltration.EgressRemediationService;
import org.pmiops.workbench.google.CloudBillingClient;
import org.pmiops.workbench.google.CloudMonitoringService;
import org.pmiops.workbench.google.CloudStorageClient;
import org.pmiops.workbench.iam.IamService;
import org.pmiops.workbench.initialcredits.InitialCreditsService;
import org.pmiops.workbench.mail.MailService;
import org.pmiops.workbench.model.*;
import org.pmiops.workbench.tanagra.api.TanagraApi;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.user.VwbUserService;
import org.pmiops.workbench.utils.mappers.AnalysisLanguageMapperImpl;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.utils.mappers.FeaturedWorkspaceMapper;
import org.pmiops.workbench.utils.mappers.FirecloudMapper;
import org.pmiops.workbench.utils.mappers.FirecloudMapperImpl;
import org.pmiops.workbench.utils.mappers.LeonardoMapperImpl;
import org.pmiops.workbench.utils.mappers.UserMapperImpl;
import org.pmiops.workbench.utils.mappers.WorkspaceMapperImpl;
import org.pmiops.workbench.vwb.admin.VwbAdminQueryService;
import org.pmiops.workbench.vwb.wsm.WsmClient;
import org.pmiops.workbench.workspaceadmin.WorkspaceAdminService;
import org.pmiops.workbench.workspaceadmin.WorkspaceAdminServiceImpl;
import org.pmiops.workbench.workspaces.*;
import org.pmiops.workbench.workspaces.migration.WorkspaceMigrationService;
import org.pmiops.workbench.workspaces.resources.UserRecentResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class WorkspacesControllerTest {

  @MockitoBean private ActionAuditQueryService actionAuditQueryService;
  @MockitoBean private AdminAuditor adminAuditor;
  @MockitoBean private AnalysisLanguageMapperImpl analysisLanguageMapperImpl;
  @MockitoBean private BillingProjectAuditor billingProjectAuditor;
  @MockitoBean private CdrBigQuerySchemaConfigService cdrBigQuerySchemaConfigService;
  @MockitoBean private CdrVersionService cdrVersionService;
  @MockitoBean private CloudMonitoringService cloudMonitoringService;

  @MockitoBean private LeonardoRuntimeAuditor leonardoRuntimeAuditor;
  @MockitoBean private MailService mailService;
  @MockitoBean private ReportingQueryService reportingQueryService;
  @MockitoBean private TanagraApi tanagraApi;
  @MockitoBean private TaskQueueService taskQueueService;
  @MockitoBean private UserService userService;
  @MockitoBean private WorkspaceAuditor workspaceAuditor;
  @MockitoBean private WorkspaceMigrationService workspaceMigrationService;
  @MockitoBean private WsmClient wsmClient;
  @MockitoBean private VwbUserService vwbUserService;
  @MockitoBean private VwbAdminQueryService vwbAdminQueryService;
  private static final String LOGGED_IN_USER_EMAIL = "bob@gmail.com";

  @MockitoBean private BigQueryService bigQueryService;
  @MockitoBean private CloudStorageClient cloudStorageClient;
  @MockitoBean private ConceptBigQueryService conceptBigQueryService;
  @MockitoBean private UserRecentResourceService userRecentResourceService;
  @MockitoBean private WorkspaceServiceFactory workspaceServiceFactory;
  @MockitoBean AccessTierService accessTierService;
  @MockitoBean BucketAuditQueryService bucketAuditQueryService;
  @MockitoBean CloudBillingClient mockCloudBillingClient;
  @MockitoBean FeaturedWorkspaceMapper featuredWorkspaceMapper;
  @MockitoBean InitialCreditsService mockInitialCreditsService;
  @MockitoBean IamService mockIamService;

  @MockitoBean
  @Qualifier(EGRESS_OBJECT_LENGTHS_SERVICE_QUALIFIER)
  EgressRemediationService egressRemediationService;

  @MockitoSpyBean @Autowired WorkspaceDao workspaceDao;
  @MockitoSpyBean @Autowired WorkspaceService workspaceService;

  @Autowired AccessTierDao accessTierDao;
  @Autowired CdrVersionDao cdrVersionDao;
  @Autowired CohortDao cohortDao;
  @Autowired CohortReviewDao cohortReviewDao;
  @Autowired ConceptSetDao conceptSetDao;
  @Autowired DataSetDao dataSetDao;
  @Autowired FakeClock fakeClock;
  @Autowired FirecloudMapper firecloudMapper;
  @Autowired UserDao userDao;
  @Autowired UserRecentWorkspaceDao userRecentWorkspaceDao;
  @Autowired WorkspaceAdminService workspaceAdminService;
  @Autowired WorkspaceAuditor mockWorkspaceAuditor;
  @Autowired WorkspaceFreeTierUsageDao workspaceFreeTierUsageDao;
  @Autowired WorkspaceOperationDao workspaceOperationDao;
  @Autowired WorkspacesController workspacesController;
  @Autowired FeaturedWorkspaceDao featuredWorkspaceDao;

  private static DbUser currentUser;
  private static WorkbenchConfig workbenchConfig;

  private DbAccessTier registeredTier;
  private DbCdrVersion cdrVersion;
  private String cdrVersionId;
  private String archivedCdrVersionId;

  @TestConfiguration
  @Import({
    BucketAuditQueryServiceImpl.class,
    CdrVersionService.class,
    CommonMappers.class,
    FakeClockConfiguration.class,
    FirecloudMapperImpl.class,
    LeonardoMapperImpl.class,
    UserMapperImpl.class,
    WorkspaceAdminServiceImpl.class,
    WorkspaceAuthService.class,
    WorkspaceMapperImpl.class,
    WorkspaceOperationMapperImpl.class,
    WorkspaceServiceImpl.class,
    WorkspacesController.class,
  })
  static class Configuration {
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    DbUser user() {
      return currentUser;
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    WorkbenchConfig workbenchConfig() {
      return workbenchConfig;
    }
  }

  @BeforeEach
  public void setUp() throws Exception {
    workbenchConfig = WorkbenchConfig.createEmptyConfig();
    workbenchConfig.billing.accountId = "initial-credits";
    workbenchConfig.billing.projectNamePrefix = "aou-local";

    currentUser = createUser(LOGGED_IN_USER_EMAIL);
    registeredTier = accessTierDao.save(createRegisteredTier());

    when(accessTierService.getAccessTierShortNamesForUser(currentUser))
        .thenReturn(List.of(AccessTierService.REGISTERED_TIER_SHORT_NAME));
    when(accessTierService.getRegisteredTierOrThrow()).thenReturn(registeredTier);

    cdrVersion = createDefaultCdrVersion(1);
    accessTierDao.save(cdrVersion.getAccessTier());
    cdrVersion.setName("1");
    // set the db name to be empty since test cases currently
    // run in the workbench schema only.
    cdrVersion.setCdrDbName("");
    cdrVersion.setAccessTier(registeredTier);
    cdrVersion = cdrVersionDao.save(cdrVersion);
    cdrVersionId = Long.toString(cdrVersion.getCdrVersionId());

    DbCdrVersion archivedCdrVersion = createDefaultCdrVersion(2);
    accessTierDao.save(archivedCdrVersion.getAccessTier());
    archivedCdrVersion.setName("archived");
    archivedCdrVersion.setCdrDbName("");
    archivedCdrVersion.setArchivalStatusEnum(ArchivalStatus.ARCHIVED);
    archivedCdrVersion = cdrVersionDao.save(archivedCdrVersion);
    archivedCdrVersionId = Long.toString(archivedCdrVersion.getCdrVersionId());

    ProjectBillingInfo billingInfo = new ProjectBillingInfo().setBillingEnabled(true);
    when(mockCloudBillingClient.pollUntilBillingAccountLinked(any(), any()))
        .thenReturn(billingInfo);
    when(mockCloudBillingClient.pollUntilBillingAccountLinked(any(), any(), anyBoolean()))
        .thenReturn(billingInfo);

    when(workspaceServiceFactory.getWorkspaceService(anyBoolean())).thenReturn(workspaceService);
  }

  private DbUser createUser(String email) {
    DbUser user = new DbUser();
    user.setUsername(email);
    user.setDisabled(false);
    return userDao.save(user);
  }
}
