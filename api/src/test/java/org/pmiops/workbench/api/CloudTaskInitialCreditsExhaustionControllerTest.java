package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.pmiops.workbench.utils.BillingUtils.fullBillingAccountName;
import static org.pmiops.workbench.utils.PresetData.createDbInstitution;
import static org.pmiops.workbench.utils.PresetData.createDbVerifiedInstitutionalAffiliation;

import com.google.common.collect.Maps;
import com.google.common.primitives.Doubles;
import jakarta.mail.MessagingException;
import jakarta.validation.constraints.NotNull;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.access.AccessTierService;
import org.pmiops.workbench.actionaudit.auditors.BillingProjectAuditor;
import org.pmiops.workbench.actionaudit.auditors.UserServiceAuditor;
import org.pmiops.workbench.cloudtasks.TaskQueueService;
import org.pmiops.workbench.cohorts.CohortCloningService;
import org.pmiops.workbench.conceptset.ConceptSetService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.dataset.DataSetService;
import org.pmiops.workbench.db.dao.InstitutionDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.dao.VerifiedInstitutionalAffiliationDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.dao.WorkspaceFreeTierUsageDao;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbVerifiedInstitutionalAffiliation;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.db.model.DbWorkspaceFreeTierUsage;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.google.CloudBillingClient;
import org.pmiops.workbench.impersonation.ImpersonatedWorkspaceService;
import org.pmiops.workbench.initialcredits.InitialCreditsService;
import org.pmiops.workbench.initialcredits.WorkspaceInitialCreditUsageService;
import org.pmiops.workbench.institution.InstitutionService;
import org.pmiops.workbench.leonardo.LeonardoApiClient;
import org.pmiops.workbench.mail.MailService;
import org.pmiops.workbench.model.ExhaustedInitialCreditsEventRequest;
import org.pmiops.workbench.model.WorkspaceActiveStatus;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.utils.mappers.FeaturedWorkspaceMapper;
import org.pmiops.workbench.utils.mappers.FirecloudMapper;
import org.pmiops.workbench.utils.mappers.UserMapper;
import org.pmiops.workbench.utils.mappers.WorkspaceMapper;
import org.pmiops.workbench.workspaces.WorkspaceAuthService;
import org.pmiops.workbench.workspaces.WorkspaceServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;

@DataJpaTest
class CloudTaskInitialCreditsExhaustionControllerTest {

  private static final Instant START_INSTANT = Instant.parse("2000-01-01T00:00:00.00Z");
  private static final FakeClock CLOCK = new FakeClock(START_INSTANT);

  @Autowired UserDao userDao;
  @Autowired WorkspaceDao workspaceDao;
  @Autowired WorkspaceFreeTierUsageDao workspaceFreeTierUsageDao;
  @Autowired VerifiedInstitutionalAffiliationDao verifiedInstitutionalAffiliationDao;

  @Autowired InitialCreditsService initialCreditsService;
  @Autowired MailService mailService;

  @Autowired CloudTaskInitialCreditsExhaustionController controller;

  private DbInstitution dbInstitution;

  private static WorkbenchConfig workbenchConfig;

  private static final String SINGLE_WORKSPACE_TEST_USER = "test@test.com";
  private static final String SINGLE_WORKSPACE_TEST_PROJECT = "aou-test-123";
  @Autowired private InstitutionDao institutionDao;

  @TestConfiguration
  @Import({
    CloudTaskInitialCreditsExhaustionController.class,
    InitialCreditsService.class,
    WorkspaceServiceImpl.class,
  })
  @MockBean({
    AccessTierService.class,
    BillingProjectAuditor.class,
    CloudBillingClient.class,
    CohortCloningService.class,
    ConceptSetService.class,
    DataSetService.class,
    FeaturedWorkspaceMapper.class,
    FireCloudService.class,
    FirecloudMapper.class,
    ImpersonatedWorkspaceService.class,
    InstitutionService.class,
    LeonardoApiClient.class,
    MailService.class,
    TaskQueueService.class,
    UserMapper.class,
    UserService.class,
    UserServiceAuditor.class,
    WorkspaceAuthService.class,
    WorkspaceInitialCreditUsageService.class,
    WorkspaceMapper.class,
  })
  static class Configuration {
    @Bean
    public Clock clock() {
      return CLOCK;
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public WorkbenchConfig workbenchConfig() {
      return workbenchConfig;
    }
  }

  @BeforeEach
  public void setUp() {
    dbInstitution = institutionDao.save(createDbInstitution());
    workbenchConfig = WorkbenchConfig.createEmptyConfig();
    workbenchConfig.billing.freeTierCostAlertThresholds = new ArrayList<>(Doubles.asList(.5, .75));
    workbenchConfig.billing.accountId = "free-tier";
    workbenchConfig.billing.defaultFreeCreditsDollarLimit = 1000.0;
    workbenchConfig.billing.freeTierCronUserBatchSize = 10;
    workbenchConfig.billing.minutesBeforeLastFreeTierJob = 0;
    workbenchConfig.billing.numberOfDaysToConsiderForFreeTierUsageUpdate = 2L;
  }

  @AfterEach
  public void tearDown() {
    workspaceFreeTierUsageDao.deleteAll();
    workspaceDao.deleteAll();
    userDao.deleteAll();
    institutionDao.deleteAll();
    verifiedInstitutionalAffiliationDao.deleteAll();
  }

  @Test
  public void
      handleInitialCreditsExhaustion_alertsAndDeletesResources_whenDollarThresholdIsExceeded()
          throws MessagingException {
    final double limit = 100.0;
    final double costUnderThreshold = 49.5;

    double threshold = 0.5;
    double costOverThreshold = 50.5;
    double remaining = limit - costOverThreshold;

    workbenchConfig.billing.defaultFreeCreditsDollarLimit = limit;

    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT);

    Map<String, Double> allDbCosts = Maps.newHashMap();
    allDbCosts.put(String.valueOf(user.getUserId()), 0.0d);

    Map<String, Double> allBQCosts = Maps.newHashMap();
    allBQCosts.put(String.valueOf(user.getUserId()), costUnderThreshold);

    // check that we have not alerted before the threshold
    ExhaustedInitialCreditsEventRequest request =
        buildExhaustedInitialCreditsEventRequest(List.of(user), allBQCosts, allDbCosts);
    controller.handleInitialCreditsExhaustionBatch(request);
    verifyNoInteractions(mailService);

    // check that we alert for the 50% threshold
    allBQCosts.put(String.valueOf(user.getUserId()), costOverThreshold);
    request.setLiveCostByCreator(allBQCosts);
    controller.handleInitialCreditsExhaustionBatch(request);
    verify(mailService)
        .alertUserInitialCreditsDollarThreshold(
            eq(user), eq(threshold), eq(costOverThreshold), eq(remaining));

    // check that we do not alert twice for the 50% threshold
    allDbCosts.put(String.valueOf(user.getUserId()), costOverThreshold);
    controller.handleInitialCreditsExhaustionBatch(request);
    verifyNoMoreInteractions(mailService);

    // check that we alert for the 75% threshold
    threshold = 0.75;
    costOverThreshold = 75.3;
    remaining = limit - costOverThreshold;

    allBQCosts.put(String.valueOf(user.getUserId()), costOverThreshold);
    request.setLiveCostByCreator(allBQCosts);
    controller.handleInitialCreditsExhaustionBatch(request);
    verify(mailService)
        .alertUserInitialCreditsDollarThreshold(
            eq(user), eq(threshold), eq(costOverThreshold), eq(remaining));

    // check that we do not alert twice for the 75% threshold
    allDbCosts.put(String.valueOf(user.getUserId()), costOverThreshold);
    controller.handleInitialCreditsExhaustionBatch(request);
    verifyNoMoreInteractions(mailService);

    // check that we alert for expiration when we hit 100%
    final double costToTriggerExpiration = 100.01;

    allBQCosts.put(String.valueOf(user.getUserId()), costToTriggerExpiration);
    request.setLiveCostByCreator(allBQCosts);

    controller.handleInitialCreditsExhaustionBatch(request);
    verify(mailService).alertUserInitialCreditsExhausted(eq(user));

    // check that we do not alert twice for 100%
    allDbCosts.put(String.valueOf(user.getUserId()), costToTriggerExpiration);
    controller.handleInitialCreditsExhaustionBatch(request);
    verifyNoMoreInteractions(mailService);
  }

  @Test
  public void
      handleInitialCreditsExhaustion_alertsAndDeletesResources_whenAltDollarThresholdIsExceeded()
          throws MessagingException {

    // set alert thresholds at 30% and 65% instead
    workbenchConfig.billing.freeTierCostAlertThresholds = new ArrayList<>(Doubles.asList(.3, .65));

    final double limit = 100.0;
    final double costUnderThreshold = 29.9;

    double threshold = 0.3;
    double costOverThreshold = 30.1;
    double remaining = limit - costOverThreshold;

    workbenchConfig.billing.defaultFreeCreditsDollarLimit = limit;

    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT);

    Map<String, Double> allDbCosts = Maps.newHashMap();
    allDbCosts.put(String.valueOf(user.getUserId()), 0.0d);

    // check that we have not alerted before the threshold
    Map<String, Double> allBQCosts = Maps.newHashMap();
    allBQCosts.put(String.valueOf(user.getUserId()), costUnderThreshold);

    ExhaustedInitialCreditsEventRequest request =
        buildExhaustedInitialCreditsEventRequest(List.of(user), allBQCosts, allDbCosts);

    controller.handleInitialCreditsExhaustionBatch(request);
    verifyNoInteractions(mailService);

    // check that we alert for the 30% threshold
    allBQCosts.put(String.valueOf(user.getUserId()), costOverThreshold);
    allDbCosts.put(String.valueOf(user.getUserId()), costUnderThreshold);
    controller.handleInitialCreditsExhaustionBatch(request);
    verify(mailService)
        .alertUserInitialCreditsDollarThreshold(
            eq(user), eq(threshold), eq(costOverThreshold), eq(remaining));

    // check that we do not alert twice for the 30% threshold
    allBQCosts.put(String.valueOf(user.getUserId()), costOverThreshold);
    allDbCosts.put(String.valueOf(user.getUserId()), costOverThreshold);
    controller.handleInitialCreditsExhaustionBatch(request);
    verifyNoMoreInteractions(mailService);

    // check that we alert for the 65% threshold

    threshold = 0.65;
    costOverThreshold = 65.01;
    remaining = limit - costOverThreshold;

    allBQCosts.put(String.valueOf(user.getUserId()), costOverThreshold);
    controller.handleInitialCreditsExhaustionBatch(request);
    verify(mailService)
        .alertUserInitialCreditsDollarThreshold(
            eq(user), eq(threshold), eq(costOverThreshold), eq(remaining));

    // check that we do not alert twice for the 65% threshold

    allBQCosts.put(String.valueOf(user.getUserId()), costOverThreshold);
    allDbCosts.put(String.valueOf(user.getUserId()), costOverThreshold);
    controller.handleInitialCreditsExhaustionBatch(request);
    verifyNoMoreInteractions(mailService);

    // check that we alert for expiration when we hit 100%

    final double costToTriggerExpiration = 100.01;
    allBQCosts.put(String.valueOf(user.getUserId()), costToTriggerExpiration);

    controller.handleInitialCreditsExhaustionBatch(request);
    verify(mailService).alertUserInitialCreditsExhausted(eq(user));

    // check that we do not alert twice for 100%

    allBQCosts.put(String.valueOf(user.getUserId()), costToTriggerExpiration);
    allDbCosts.put(String.valueOf(user.getUserId()), costToTriggerExpiration);
    controller.handleInitialCreditsExhaustionBatch(request);
    verifyNoMoreInteractions(mailService);
  }

  @Test
  public void handleInitialCreditsExhaustion_alertsAndDeletesResources_evenWhenUserIsDisabled()
      throws MessagingException {
    workbenchConfig.billing.defaultFreeCreditsDollarLimit = 100.0;

    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    user.setDisabled(true);
    userDao.save(user);

    Map<String, Double> allBQCosts = Maps.newHashMap();
    allBQCosts.put(String.valueOf(user.getUserId()), 100.01);

    final DbWorkspace workspace = createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT);

    ExhaustedInitialCreditsEventRequest request =
        buildExhaustedInitialCreditsEventRequest(
            List.of(user), allBQCosts, Map.of(String.valueOf(user.getUserId()), 0d));

    controller.handleInitialCreditsExhaustionBatch(request);

    verify(mailService).alertUserInitialCreditsExhausted(eq(user));
    assertThat(workspace.isInitialCreditsExhausted()).isEqualTo(true);
  }

  @Test
  public void handleInitialCreditsExhaustion_alertsAndDeletesResources_evenWhenWorkspaceIsDeleted()
      throws MessagingException {
    workbenchConfig.billing.defaultFreeCreditsDollarLimit = 100.0;

    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    final DbWorkspace workspace = createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT);
    workspace.setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.DELETED);
    workspaceDao.save(workspace);

    Map<String, Double> allBQCosts = Maps.newHashMap();
    allBQCosts.put(String.valueOf(user.getUserId()), 100.01);

    ExhaustedInitialCreditsEventRequest request =
        buildExhaustedInitialCreditsEventRequest(
            List.of(user), allBQCosts, Map.of(String.valueOf(user.getUserId()), 0d));

    controller.handleInitialCreditsExhaustionBatch(request);

    verify(mailService).alertUserInitialCreditsExhausted(eq(user));

    assertThat(workspace.isInitialCreditsExhausted()).isEqualTo(true);
  }

  @Test
  public void handleInitialCreditsExhaustion_noAlert_ifCostIsBelowLowestThreshold() {
    // set limit so usage is just under the 50% threshold
    workbenchConfig.billing.defaultFreeCreditsDollarLimit = 100.0;

    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    final DbWorkspace workspace = createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT);
    Map<String, Double> allBQCosts = Maps.newHashMap();
    allBQCosts.put(String.valueOf(user.getUserId()), 49.99);

    ExhaustedInitialCreditsEventRequest request =
        buildExhaustedInitialCreditsEventRequest(
            List.of(user), allBQCosts, Map.of(String.valueOf(user.getUserId()), 0d));

    controller.handleInitialCreditsExhaustionBatch(request);
    verifyNoInteractions(mailService);
    assertThat(workspace.isInitialCreditsExhausted()).isEqualTo(false);
  }

  @Test
  public void handleInitialCreditsExhaustion_doesntThrowNPE_whenWorkspaceIsMissingCreator() {
    workbenchConfig.billing.defaultFreeCreditsDollarLimit = 100.0;

    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    final DbWorkspace workspace = createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT);
    createWorkspace(null, "rumney");

    // set limit so usage is just under the 50% threshold
    Map<String, Double> allBQCosts = Map.of(String.valueOf(user.getUserId()), 49.99);

    ExhaustedInitialCreditsEventRequest request =
        buildExhaustedInitialCreditsEventRequest(
            List.of(user), allBQCosts, Map.of(String.valueOf(user.getUserId()), 0d));

    controller.handleInitialCreditsExhaustionBatch(request);
    verifyNoInteractions(mailService);
    assertThat(workspace.isInitialCreditsExhausted()).isEqualTo(false);
  }

  @Test
  public void handleInitialCreditsExhaustion_doesntaAlert_ifDollarLimitWasOverriddenToOverUse()
      throws MessagingException {
    workbenchConfig.billing.defaultFreeCreditsDollarLimit = 100.0;

    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    final DbWorkspace workspace = createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT);

    Map<String, Double> allBQCosts = Maps.newHashMap();
    allBQCosts.put(String.valueOf(user.getUserId()), 150.0);

    controller.handleInitialCreditsExhaustionBatch(
        buildExhaustedInitialCreditsEventRequest(
            List.of(user), allBQCosts, Map.of(String.valueOf(user.getUserId()), 0d)));
    verify(mailService).alertUserInitialCreditsExhausted(eq(user));

    assertThat(workspace.isInitialCreditsExhausted()).isEqualTo(true);

    initialCreditsService.maybeSetDollarLimitOverride(user, 200.0);

    assertThat(workspace.isInitialCreditsExhausted()).isEqualTo(false);

    controller.handleInitialCreditsExhaustionBatch(
        buildExhaustedInitialCreditsEventRequest(
            List.of(user), allBQCosts, Map.of(String.valueOf(user.getUserId()), 150.0)));

    verifyNoMoreInteractions(mailService);
    assertThat(workspace.isInitialCreditsExhausted()).isEqualTo(false);
  }

  // do not reactivate workspaces if the new dollar limit is still below the usage

  @Test
  public void handleInitialCreditsExhaustion_doesntaAlert_ifDollarLimitWasOverriddenToUnderUse()
      throws MessagingException {
    workbenchConfig.billing.defaultFreeCreditsDollarLimit = 100.0;

    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    final DbWorkspace workspace = createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT);
    workspaceFreeTierUsageDao.save(
        new DbWorkspaceFreeTierUsage(workspace).setUser(user).setCost(300.0));

    Map<String, Double> allBQCosts = Maps.newHashMap();
    allBQCosts.put(String.valueOf(user.getUserId()), 300.0);

    controller.handleInitialCreditsExhaustionBatch(
        buildExhaustedInitialCreditsEventRequest(
            List.of(user), allBQCosts, Map.of(String.valueOf(user.getUserId()), 0d)));
    verify(mailService).alertUserInitialCreditsExhausted(eq(user));
    assertThat(workspace.isInitialCreditsExhausted()).isEqualTo(true);

    initialCreditsService.maybeSetDollarLimitOverride(user, 200.0);
    assertThat(workspace.isInitialCreditsExhausted()).isEqualTo(true);

    controller.handleInitialCreditsExhaustionBatch(
        buildExhaustedInitialCreditsEventRequest(
            List.of(user), allBQCosts, Map.of(String.valueOf(user.getUserId()), 300.0)));
    verifyNoMoreInteractions(mailService);
    assertThat(workspace.isInitialCreditsExhausted()).isEqualTo(true);
  }

  @Test
  public void handleInitialCreditsExhaustion_disabledAllWorkspaces_ifTheCombinedCostExceedsLimit()
      throws MessagingException {
    final String proj1 = "proj-1";
    final String proj2 = "proj-2";
    final double cost1 = 123.45;
    final double cost2 = 234.56;
    final double sum = cost1 + cost2;

    workbenchConfig.billing.defaultFreeCreditsDollarLimit = sum - 0.01;

    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    final DbWorkspace ws1 = createWorkspace(user, proj1);
    final DbWorkspace ws2 = createWorkspace(user, proj2);

    Map<String, Double> allBQCosts = Maps.newHashMap();
    allBQCosts.put(String.valueOf(user.getUserId()), sum);

    controller.handleInitialCreditsExhaustionBatch(
        buildExhaustedInitialCreditsEventRequest(
            List.of(user), allBQCosts, Map.of(String.valueOf(user.getUserId()), 0d)));
    verify(mailService).alertUserInitialCreditsExhausted(eq(user));

    // confirm DB updates after handleInitialCreditsExhaustionBatch()

    for (DbWorkspace ws : Arrays.asList(ws1, ws2)) {
      // retrieve from DB again to reflect update after cron
      DbWorkspace dbWorkspace = workspaceDao.findById(ws.getWorkspaceId()).get();
      assertThat(dbWorkspace.isInitialCreditsExhausted()).isEqualTo(true);
    }
  }

  @Test
  public void handleInitialCreditsExhaustion_alertsAllUsers_ifTheyExceedFreeTierLimit()
      throws MessagingException {
    final String proj1 = "proj-1";
    final String proj2 = "proj-2";
    final double cost1 = 123.45;
    final double cost2 = 234.56;

    workbenchConfig.billing.defaultFreeCreditsDollarLimit = Math.min(cost1, cost2) - 0.01;

    DbUser user1 = createUser(SINGLE_WORKSPACE_TEST_USER);
    DbWorkspace ws1 = createWorkspace(user1, proj1);
    DbUser user2 = createUser("more@test.com");
    DbWorkspace ws2 = createWorkspace(user2, proj2);

    Map<String, Double> allBQCosts = Maps.newHashMap();
    allBQCosts.put(String.valueOf(user1.getUserId()), cost1);
    allBQCosts.put(String.valueOf(user2.getUserId()), cost2);

    controller.handleInitialCreditsExhaustionBatch(
        buildExhaustedInitialCreditsEventRequest(
            List.of(user1, user2),
            allBQCosts,
            Map.of(String.valueOf(user1.getUserId()), 0d, String.valueOf(user2.getUserId()), 0d)));
    verify(mailService).alertUserInitialCreditsExhausted(eq(user1));
    verify(mailService).alertUserInitialCreditsExhausted(eq(user2));

    final DbWorkspace dbWorkspace1 = workspaceDao.findById(ws1.getWorkspaceId()).get();
    assertThat(dbWorkspace1.isInitialCreditsExhausted()).isEqualTo(true);

    final DbWorkspace dbWorkspace2 = workspaceDao.findById(ws2.getWorkspaceId()).get();
    assertThat(dbWorkspace2.isInitialCreditsExhausted()).isEqualTo(true);
  }

  @Test
  public void handleInitialCreditsExhaustion_alertsOnlyOnce_ifCostKeepIncreasingAboveThreshold()
      throws MessagingException {
    workbenchConfig.billing.defaultFreeCreditsDollarLimit = 100.0;

    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    final DbWorkspace workspace = createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT);

    Map<String, Double> allBQCosts = Maps.newHashMap();
    allBQCosts.put(String.valueOf(user.getUserId()), 100.01);

    controller.handleInitialCreditsExhaustionBatch(
        buildExhaustedInitialCreditsEventRequest(
            List.of(user), allBQCosts, Map.of(String.valueOf(user.getUserId()), 0d)));
    verify(mailService).alertUserInitialCreditsExhausted(eq(user));
    assertThat(workspace.isInitialCreditsExhausted()).isEqualTo(true);

    // time elapses, and this project incurs additional cost

    final double newTotalCost = 123.45;
    allBQCosts.put(String.valueOf(user.getUserId()), newTotalCost);

    // we do not alert again
    controller.handleInitialCreditsExhaustionBatch(
        buildExhaustedInitialCreditsEventRequest(
            List.of(user), allBQCosts, Map.of(String.valueOf(user.getUserId()), 100.01)));
    verify(mailService, times(1)).alertUserInitialCreditsExhausted(eq(user));

    // retrieve from DB again to reflect update after cron
    DbWorkspace dbWorkspace = workspaceDao.findById(workspace.getWorkspaceId()).get();
    assertThat(dbWorkspace.isInitialCreditsExhausted()).isEqualTo(true);
  }

  // Regression test coverage for RW-8328.
  @Test
  public void handleInitialCreditsExhaustion_singleAlert_forExhaustedAndByoBilling()
      throws Exception {
    workbenchConfig.billing.defaultFreeCreditsDollarLimit = 100.0;

    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    DbWorkspace workspace = createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT);

    Map<String, Double> allBQCosts = Maps.newHashMap();
    allBQCosts.put(String.valueOf(user.getUserId()), 100.01);

    controller.handleInitialCreditsExhaustionBatch(
        buildExhaustedInitialCreditsEventRequest(
            List.of(user), allBQCosts, Map.of(String.valueOf(user.getUserId()), 0d)));
    verify(mailService).alertUserInitialCreditsExhausted(eq(user));

    // Simulate the user attaching their own billing account to the previously free tier workspace.
    workspaceDao.save(workspace.setBillingAccountName(fullBillingAccountName("byo-account")));

    controller.handleInitialCreditsExhaustionBatch(
        buildExhaustedInitialCreditsEventRequest(
            List.of(user), allBQCosts, Map.of(String.valueOf(user.getUserId()), 100.01)));
    verifyNoMoreInteractions(mailService);
  }

  @Test
  public void
      handleInitialCreditsExhaustion_disableFreeTierWorkspacesOnly_whenUserHasMultipleWorkspaces()
          throws MessagingException {
    workbenchConfig.billing.defaultFreeCreditsDollarLimit = 100.0;

    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    final DbWorkspace freeTierWorkspace = createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT);
    final DbWorkspace userAccountWorkspace =
        new DbWorkspace()
            .setCreator(user)
            .setWorkspaceNamespace("some other namespace")
            .setGoogleProject("other project")
            .setBillingAccountName("some other account")
            .setInitialCreditsExhausted(false);
    workspaceDao.save(userAccountWorkspace);

    Map<String, Double> allBQCosts = Map.of(String.valueOf(user.getUserId()), 100.01);
    controller.handleInitialCreditsExhaustionBatch(
        buildExhaustedInitialCreditsEventRequest(
            List.of(user), allBQCosts, Map.of(String.valueOf(user.getUserId()), 0d)));
    verify(mailService).alertUserInitialCreditsExhausted(eq(user));

    assertThat(freeTierWorkspace.isInitialCreditsExhausted()).isEqualTo(true);

    final DbWorkspace retrievedWorkspace =
        workspaceDao.findById(userAccountWorkspace.getWorkspaceId()).get();
    assertThat(retrievedWorkspace.isInitialCreditsExhausted()).isEqualTo(false);
  }

  @Test
  public void
      handleInitialCreditsExhaustion_deletedWorkspaceUsageIsConsidered_whenChargeIsPostedAfterCron()
          throws MessagingException {
    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    DbWorkspace workspace = createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT);

    workbenchConfig.billing.defaultFreeCreditsDollarLimit = 100.0;
    Map<String, Double> allBQCosts = Map.of(String.valueOf(user.getUserId()), 50.0);

    workspace.setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.DELETED);
    workspace.setLastModifiedTime(Timestamp.valueOf(LocalDateTime.now()));
    workspaceDao.save(workspace);

    controller.handleInitialCreditsExhaustionBatch(
        buildExhaustedInitialCreditsEventRequest(
            List.of(user), allBQCosts, Map.of(String.valueOf(user.getUserId()), 0d)));
    verifyNoInteractions(mailService);

    allBQCosts = Map.of(String.valueOf(user.getUserId()), 100.1);

    controller.handleInitialCreditsExhaustionBatch(
        buildExhaustedInitialCreditsEventRequest(
            List.of(user), allBQCosts, Map.of(String.valueOf(user.getUserId()), 50.0)));
    verify(mailService).alertUserInitialCreditsExhausted(eq(user));
  }

  @Test
  public void
      handleInitialCreditsExhaustion_deletedWorkspaceUsageIsConsidered_whenAnotherWorkspaceExceedsLimitAfterCron()
          throws MessagingException {
    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    DbWorkspace workspace = createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT);

    workbenchConfig.billing.defaultFreeCreditsDollarLimit = 100.0;
    Map<String, Double> allBQCosts = new HashMap<>();
    allBQCosts.put(String.valueOf(user.getUserId()), 50.0);

    workspace.setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.DELETED);
    workspace.setLastModifiedTime(Timestamp.valueOf(LocalDateTime.now()));
    workspaceDao.save(workspace);

    controller.handleInitialCreditsExhaustionBatch(
        buildExhaustedInitialCreditsEventRequest(
            List.of(user), allBQCosts, Map.of(String.valueOf(user.getUserId()), 0d)));
    verifyNoInteractions(mailService);

    DbWorkspace anotherWorkspace = createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT + "4");
    assertThat(workspace.isInitialCreditsExhausted()).isEqualTo(false);

    allBQCosts.put(String.valueOf(user.getUserId()), 100.1);

    controller.handleInitialCreditsExhaustionBatch(
        buildExhaustedInitialCreditsEventRequest(
            List.of(user), allBQCosts, Map.of(String.valueOf(user.getUserId()), 50.0)));
    verify(mailService).alertUserInitialCreditsExhausted(eq(user));

    assertThat(workspace.isInitialCreditsExhausted()).isEqualTo(true);
    assertThat(anotherWorkspace.isInitialCreditsExhausted()).isEqualTo(true);
  }

  @Test
  public void handleInitialCreditsExhaustion_withMissingUsersInRequest_NoNPE() throws Exception {

    workbenchConfig.billing.defaultFreeCreditsDollarLimit = 300.0;

    DbUser user1 = createUser("user1@test.com");
    DbUser user2 = createUser("user2@test.com");
    DbUser user3 = createUser("user3@test.com");

    createWorkspace(user1, SINGLE_WORKSPACE_TEST_PROJECT);
    createWorkspace(user2, SINGLE_WORKSPACE_TEST_PROJECT);
    createWorkspace(user3, SINGLE_WORKSPACE_TEST_PROJECT);

    ExhaustedInitialCreditsEventRequest request = new ExhaustedInitialCreditsEventRequest();
    request.setUsers(Arrays.asList(user1.getUserId(), user2.getUserId()));

    Map<String, Double> liveCostByCreator = new HashMap<>();
    liveCostByCreator.put(String.valueOf(user1.getUserId()), 310.0);
    liveCostByCreator.put(String.valueOf(user2.getUserId()), 250.0);
    liveCostByCreator.put(String.valueOf(user3.getUserId()), 151.0);
    request.setLiveCostByCreator(liveCostByCreator);

    request.setDbCostByCreator(
        Map.of(
            String.valueOf(user1.getUserId()), 0d,
            String.valueOf(user2.getUserId()), 0d,
            String.valueOf(user3.getUserId()), 0d));

    controller.handleInitialCreditsExhaustionBatch(request);

    verify(mailService)
        .alertUserInitialCreditsDollarThreshold(eq(user3), eq(0.5d), eq(151.0d), eq(149.0d));
    verify(mailService)
        .alertUserInitialCreditsDollarThreshold(eq(user2), eq(0.75d), eq(250.0d), eq(50.0d));
    verify(mailService).alertUserInitialCreditsExhausted(eq(user1));
    verifyNoMoreInteractions(mailService);
  }

  private DbUser createUser(String email) {
    DbUser user = userDao.save(new DbUser().setUsername(email));

    DbVerifiedInstitutionalAffiliation verifiedInstitutionalAffiliation =
        createDbVerifiedInstitutionalAffiliation(dbInstitution, user);
    verifiedInstitutionalAffiliationDao.save(verifiedInstitutionalAffiliation);
    return user;
  }

  private DbWorkspace createWorkspace(DbUser creator, String project) {
    return workspaceDao.save(
        new DbWorkspace()
            .setCreator(creator)
            .setWorkspaceNamespace(project + "-ns")
            .setGoogleProject(project)
            .setBillingAccountName(workbenchConfig.billing.initialCreditsBillingAccountName()));
  }

  @NotNull
  private static ExhaustedInitialCreditsEventRequest buildExhaustedInitialCreditsEventRequest(
      List<DbUser> users, Map<String, Double> allBQCosts, Map<String, Double> dbCostByCreator) {
    return new ExhaustedInitialCreditsEventRequest()
        .users(users.stream().map(DbUser::getUserId).toList())
        .liveCostByCreator(allBQCosts)
        .dbCostByCreator(dbCostByCreator);
  }
}
