package org.pmiops.workbench.initialcredits;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.pmiops.workbench.utils.ArgumentMatchers.MapMatcher;
import static org.pmiops.workbench.utils.BillingUtils.fullBillingAccountName;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.primitives.Doubles;
import jakarta.mail.MessagingException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.actionaudit.auditors.UserServiceAuditor;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.cloudtasks.TaskQueueService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.VwbUserPodDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.dao.WorkspaceFreeTierUsageDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserInitialCreditsExpiration;
import org.pmiops.workbench.db.model.DbVwbUserPod;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.db.model.DbWorkspaceFreeTierUsage;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.institution.InstitutionService;
import org.pmiops.workbench.leonardo.LeonardoApiClient;
import org.pmiops.workbench.mail.MailService;
import org.pmiops.workbench.model.WorkspaceActiveStatus;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.user.VwbUserService;
import org.pmiops.workbench.utils.ArgumentMatchers.UserListMatcher;
import org.pmiops.workbench.utils.mappers.WorkspaceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.test.context.transaction.TestTransaction;

@DataJpaTest
public class InitialCreditsServiceTest {

  private static final Instant START_INSTANT = Instant.parse("2000-01-01T00:00:00.00Z");
  private static final FakeClock CLOCK = new FakeClock(START_INSTANT);

  private static final double DEFAULT_PERCENTAGE_TOLERANCE = 0.000001;

  @SpyBean private UserDao spyUserDao;

  @SpyBean private WorkspaceDao spyWorkspaceDao;

  @MockBean private UserServiceAuditor mockUserServiceAuditor;
  @MockBean private MailService mailService;
  @MockBean private LeonardoApiClient leonardoApiClient;
  @MockBean private InstitutionService institutionService;

  @Autowired InitialCreditsService initialCreditsService;
  @Autowired UserDao userDao;
  @Autowired WorkspaceDao workspaceDao;
  @Autowired WorkspaceFreeTierUsageDao workspaceFreeTierUsageDao;

  @Autowired private TaskQueueService taskQueueService;
  @Autowired private ApplicationContext applicationContext;
  @Autowired private VwbUserPodDao vwbUserPodDao;

  private static WorkbenchConfig workbenchConfig;

  private static final String SINGLE_WORKSPACE_TEST_USER = "test@test.com";
  private static final String SINGLE_WORKSPACE_TEST_PROJECT = "aou-test-123";

  private static final long validityPeriodDays = 17L; // arbitrary
  private static final long extensionPeriodDays = 78L; // arbitrary
  private static final long warningPeriodDays = 10L; // arbitrary

  private static final Timestamp NOW = Timestamp.from(FakeClockConfiguration.NOW.toInstant());
  private static final Timestamp PAST_EXPIRATION =
      Timestamp.from(FakeClockConfiguration.NOW.toInstant().minusSeconds(24 * 60 * 60));
  private static final Timestamp DURING_WARNING_PERIOD =
      Timestamp.from(
          FakeClockConfiguration.NOW
              .toInstant()
              .plusSeconds((warningPeriodDays - 1L) * 24 * 60 * 60));
  private static final Timestamp BEFORE_WARNING_PERIOD =
      Timestamp.from(
          FakeClockConfiguration.NOW
              .toInstant()
              .plusSeconds((warningPeriodDays + 1L) * 24 * 60 * 60));

  private DbWorkspace workspace;
  @Autowired private VwbUserService vwbUserService;

  @TestConfiguration
  @Import({InitialCreditsService.class, WorkspaceInitialCreditUsageService.class})
  @MockBean({
    BigQueryService.class,
    TaskQueueService.class,
    WorkspaceMapper.class,
    FireCloudService.class,
    VwbUserService.class,
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
  public void setUp() throws MessagingException {
    workbenchConfig = WorkbenchConfig.createEmptyConfig();
    workbenchConfig.billing.initialCreditsCostAlertThresholds = Doubles.asList(.5, .75);
    workbenchConfig.billing.accountId = "initial-credits";
    workbenchConfig.billing.defaultInitialCreditsDollarLimit = 1000.0;
    workbenchConfig.billing.initialCreditsValidityPeriodDays = validityPeriodDays;
    workbenchConfig.billing.initialCreditsExtensionPeriodDays = extensionPeriodDays;
    workbenchConfig.billing.initialCreditsExpirationWarningDays = warningPeriodDays;
    workbenchConfig.billing.initialCreditsExpirationWarningDaysList = List.of(warningPeriodDays);
    workbenchConfig.billing.minutesBeforeLastInitialCreditsJob = 0;
    workbenchConfig.billing.numberOfDaysToConsiderForInitialCreditsUsageUpdate = 2L;
    workbenchConfig.featureFlags.enableInitialCreditsExpiration = true;
    workbenchConfig.featureFlags.enableUnlinkBillingForInitialCredits = true;
    workbenchConfig.offlineBatch.usersPerCheckInitialCreditsUsageTask = 10;

    workspace =
        spyWorkspaceDao.save(
            new DbWorkspace()
                .setBillingAccountName(workbenchConfig.billing.initialCreditsBillingAccountName())
                .setWorkspaceId(1L)
                .setWorkspaceNamespace("initial-credits-namespace")
                .setGoogleProject("initial-credits-project")
                .setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.ACTIVE));
    doNothing().when(mailService).alertUserInitialCreditsExpiring(isA(DbUser.class));
    doNothing().when(mailService).alertUserInitialCreditsExpired(isA(DbUser.class));
  }

  @AfterEach
  public void tearDown() {
    workspaceFreeTierUsageDao.deleteAll();
    workspaceDao.deleteAll();
    userDao.deleteAll();
  }

  @Test
  public void checkInitialCreditsUsage_exceedsDollarThresholds() {
    final double limit = 100.0;
    final double costUnderThreshold = 49.5;

    workbenchConfig.billing.defaultInitialCreditsDollarLimit = limit;

    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT);

    commitTransaction();

    Map<String, Double> allBQCosts = Maps.newHashMap();
    allBQCosts.put(SINGLE_WORKSPACE_TEST_PROJECT, costUnderThreshold);

    initialCreditsService.checkInitialCreditsUsageForUsers(
        Sets.newHashSet(user), allBQCosts, Collections.emptyMap());
    verifyNoInteractions(taskQueueService);

    // check that we alert for the 50% threshold
    double costOverThreshold = 50.5;
    allBQCosts.put(SINGLE_WORKSPACE_TEST_PROJECT, costOverThreshold);
    initialCreditsService.checkInitialCreditsUsageForUsers(
        Sets.newHashSet(user), allBQCosts, Collections.emptyMap());
    verify(taskQueueService)
        .pushInitialCreditsExhaustionTask(
            argThat(new UserListMatcher(List.of(user.getUserId()))),
            argThat(new MapMatcher(Map.of(user.getUserId(), costUnderThreshold))),
            argThat(new MapMatcher(Map.of(user.getUserId(), costOverThreshold))));

    // check that we alert for the 75% threshold
    costOverThreshold = 75.3;

    allBQCosts.put(SINGLE_WORKSPACE_TEST_PROJECT, costOverThreshold);
    initialCreditsService.checkInitialCreditsUsageForUsers(
        Sets.newHashSet(user), allBQCosts, Collections.emptyMap());
    verify(taskQueueService)
        .pushInitialCreditsExhaustionTask(
            argThat(new UserListMatcher(List.of(user.getUserId()))),
            argThat(new MapMatcher(Map.of(user.getUserId(), 50.5d))),
            argThat(new MapMatcher(Map.of(user.getUserId(), costOverThreshold))));

    // check that we alert for expiration when we hit 100%

    final double costToTriggerExpiration = 100.01;

    allBQCosts.put(SINGLE_WORKSPACE_TEST_PROJECT, costToTriggerExpiration);

    initialCreditsService.checkInitialCreditsUsageForUsers(
        Sets.newHashSet(user), allBQCosts, Collections.emptyMap());
    verify(taskQueueService)
        .pushInitialCreditsExhaustionTask(
            argThat(new UserListMatcher(List.of(user.getUserId()))),
            argThat(new MapMatcher(Map.of(user.getUserId(), costOverThreshold))),
            argThat(new MapMatcher(Map.of(user.getUserId(), costToTriggerExpiration))));
  }

  @Test
  public void checkInitialCreditsUsage_altDollarThresholds() {

    // set alert thresholds at 30% and 65% instead
    workbenchConfig.billing.initialCreditsCostAlertThresholds = Doubles.asList(.3, .65);

    final double limit = 100.0;
    final double costUnderThreshold = 29.9;

    double costOverThreshold = 30.1;

    workbenchConfig.billing.defaultInitialCreditsDollarLimit = limit;

    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT);

    commitTransaction();
    // check that we have not alerted before the threshold
    Map<String, Double> allBQCosts = Maps.newHashMap();
    allBQCosts.put(SINGLE_WORKSPACE_TEST_PROJECT, costUnderThreshold);

    initialCreditsService.checkInitialCreditsUsageForUsers(
        Sets.newHashSet(user), allBQCosts, Collections.emptyMap());
    verifyNoInteractions(taskQueueService);

    // check that we detect the 30% threshold
    allBQCosts.put(SINGLE_WORKSPACE_TEST_PROJECT, costOverThreshold);
    initialCreditsService.checkInitialCreditsUsageForUsers(
        Sets.newHashSet(user), allBQCosts, Collections.emptyMap());
    verify(taskQueueService)
        .pushInitialCreditsExhaustionTask(
            argThat(new UserListMatcher(List.of(user.getUserId()))),
            argThat(new MapMatcher(Map.of(user.getUserId(), costUnderThreshold))),
            argThat(new MapMatcher(Map.of(user.getUserId(), costOverThreshold))));

    costOverThreshold = 65.01;

    allBQCosts.put(SINGLE_WORKSPACE_TEST_PROJECT, costOverThreshold);
    initialCreditsService.checkInitialCreditsUsageForUsers(
        Sets.newHashSet(user), allBQCosts, Collections.emptyMap());
    verify(taskQueueService)
        .pushInitialCreditsExhaustionTask(
            argThat(new UserListMatcher(List.of(user.getUserId()))),
            argThat(
                new MapMatcher(Map.of(user.getUserId(), 30.1))), // The previous costOverThreshold
            argThat(new MapMatcher(Map.of(user.getUserId(), costOverThreshold))));

    // check that we detect expiration when we hit 100%
    final double costToTriggerExpiration = 100.01;
    allBQCosts.put(SINGLE_WORKSPACE_TEST_PROJECT, costToTriggerExpiration);

    initialCreditsService.checkInitialCreditsUsageForUsers(
        Sets.newHashSet(user), allBQCosts, Collections.emptyMap());
    verify(taskQueueService)
        .pushInitialCreditsExhaustionTask(
            argThat(new UserListMatcher(List.of(user.getUserId()))),
            argThat(new MapMatcher(Map.of(user.getUserId(), costOverThreshold))),
            argThat(new MapMatcher(Map.of(user.getUserId(), costToTriggerExpiration))));
  }

  @Test
  public void checkInitialCreditsUsage_disabledUserNotIgnored() {
    workbenchConfig.billing.defaultInitialCreditsDollarLimit = 100.0;
    Map<String, Double> allBQCosts = Maps.newHashMap();
    allBQCosts.put(SINGLE_WORKSPACE_TEST_PROJECT, 100.01);

    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    user.setDisabled(true);
    userDao.save(user);
    final DbWorkspace workspace = createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT);

    commitTransaction();

    initialCreditsService.checkInitialCreditsUsageForUsers(
        Sets.newHashSet(user), allBQCosts, Collections.emptyMap());
    verify(taskQueueService)
        .pushInitialCreditsExhaustionTask(
            argThat(new UserListMatcher(List.of(user.getUserId()))),
            argThat(new MapMatcher(Map.of(user.getUserId(), 0.0d))),
            argThat(new MapMatcher(Map.of(user.getUserId(), 100.01))));

    assertSingleWorkspaceTestDbState(user, workspace, 100.01);
  }

  @Test
  public void checkInitialCreditsUsage_deletedWorkspaceNotIgnored() {
    workbenchConfig.billing.defaultInitialCreditsDollarLimit = 100.0;

    Map<String, Double> allBQCosts = Maps.newHashMap();
    allBQCosts.put(SINGLE_WORKSPACE_TEST_PROJECT, 100.01);

    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    final DbWorkspace workspace = createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT);
    workspace.setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.DELETED);
    workspaceDao.save(workspace);

    commitTransaction();

    initialCreditsService.checkInitialCreditsUsageForUsers(
        Sets.newHashSet(user), allBQCosts, Collections.emptyMap());
    verify(taskQueueService)
        .pushInitialCreditsExhaustionTask(
            argThat(new UserListMatcher(List.of(user.getUserId()))),
            argThat(new MapMatcher(Map.of(user.getUserId(), 0.0d))),
            argThat(new MapMatcher(Map.of(user.getUserId(), 100.01))));

    assertSingleWorkspaceTestDbState(user, workspace, 100.01);
  }

  @Test
  public void checkInitialCreditsUsage_noAlert() {
    // set limit so usage is just under the 50% threshold
    workbenchConfig.billing.defaultInitialCreditsDollarLimit = 100.0;

    Map<String, Double> allBQCosts = Maps.newHashMap();
    allBQCosts.put(SINGLE_WORKSPACE_TEST_PROJECT, 49.99);

    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    final DbWorkspace workspace = createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT);

    commitTransaction();

    initialCreditsService.checkInitialCreditsUsageForUsers(
        Sets.newHashSet(user), allBQCosts, Collections.emptyMap());
    verifyNoInteractions(taskQueueService); // No tasks have been added to the queue

    assertSingleWorkspaceTestDbState(user, workspace, 49.99);
  }

  @Test
  public void checkInitialCreditsUsage_workspaceMissingCreatorNoNPE() {
    // set limit so usage is just under the 50% threshold
    workbenchConfig.billing.defaultInitialCreditsDollarLimit = 100.0;

    Map<String, Double> allBQCosts = Maps.newHashMap();
    allBQCosts.put(SINGLE_WORKSPACE_TEST_PROJECT, 49.99);

    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    final DbWorkspace workspace = createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT);
    createWorkspace(null, "rumney");

    commitTransaction();

    initialCreditsService.checkInitialCreditsUsageForUsers(
        Sets.newHashSet(user), allBQCosts, Collections.emptyMap());
    verifyNoInteractions(taskQueueService);

    assertSingleWorkspaceTestDbState(user, workspace, 49.99);
  }

  @Test
  public void maybeSetDollarLimitOverride_true() {
    workbenchConfig.billing.defaultInitialCreditsDollarLimit = 100.0;
    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);

    commitTransaction();

    assertThat(initialCreditsService.maybeSetDollarLimitOverride(user, 200.0)).isTrue();
    verify(mockUserServiceAuditor).fireSetInitialCreditsOverride(user.getUserId(), null, 200.0);
    assertWithinBillingTolerance(initialCreditsService.getUserInitialCreditsLimit(user), 200.0);

    DbUser currentUser = userDao.findUserByUserId(user.getUserId());
    assertThat(initialCreditsService.maybeSetDollarLimitOverride(currentUser, 100.0)).isTrue();
    verify(mockUserServiceAuditor).fireSetInitialCreditsOverride(user.getUserId(), 200.0, 100.0);
    assertWithinBillingTolerance(
        initialCreditsService.getUserInitialCreditsLimit(currentUser), 100.0);
  }

  @Test
  public void maybeSetDollarLimitOverride_false() {
    workbenchConfig.billing.defaultInitialCreditsDollarLimit = 100.0;
    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);

    assertThat(initialCreditsService.maybeSetDollarLimitOverride(user, 100.0)).isFalse();
    verify(mockUserServiceAuditor, never())
        .fireSetInitialCreditsOverride(anyLong(), anyDouble(), anyDouble());
    assertWithinBillingTolerance(initialCreditsService.getUserInitialCreditsLimit(user), 100.0);

    workbenchConfig.billing.defaultInitialCreditsDollarLimit = 200.0;
    assertWithinBillingTolerance(initialCreditsService.getUserInitialCreditsLimit(user), 200.0);

    assertThat(initialCreditsService.maybeSetDollarLimitOverride(user, 200.0)).isFalse();
    verify(mockUserServiceAuditor, never())
        .fireSetInitialCreditsOverride(anyLong(), anyDouble(), anyDouble());
    assertWithinBillingTolerance(initialCreditsService.getUserInitialCreditsLimit(user), 200.0);
  }

  @Test
  public void maybeSetDollarLimitOverride_above_usage() {
    workbenchConfig.billing.defaultInitialCreditsDollarLimit = 100.0;

    Map<String, Double> allBQCosts = Maps.newHashMap();
    allBQCosts.put(SINGLE_WORKSPACE_TEST_PROJECT, 150.0);

    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    final DbWorkspace workspace = createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT);

    assertThat(initialCreditsService.getCachedInitialCreditsUsage(user)).isNull();
    assertWithinBillingTolerance(initialCreditsService.getUserInitialCreditsLimit(user), 100.0);

    commitTransaction();

    initialCreditsService.checkInitialCreditsUsageForUsers(
        Sets.newHashSet(user), allBQCosts, Collections.emptyMap());
    verify(taskQueueService)
        .pushInitialCreditsExhaustionTask(
            argThat(new UserListMatcher(List.of(user.getUserId()))),
            argThat(new MapMatcher(Map.of(user.getUserId(), 0.0d))),
            argThat(new MapMatcher(Map.of(user.getUserId(), 150.0))));

    assertSingleWorkspaceTestDbState(user, workspace, 150.0);
    assertWithinBillingTolerance(initialCreditsService.getCachedInitialCreditsUsage(user), 150.0);

    initialCreditsService.maybeSetDollarLimitOverride(user, 200.0);
    verify(mockUserServiceAuditor).fireSetInitialCreditsOverride(user.getUserId(), null, 200.0);
    assertWithinBillingTolerance(initialCreditsService.getUserInitialCreditsLimit(user), 200.0);
    assertSingleWorkspaceTestDbState(user, workspace, 150.0);

    initialCreditsService.checkInitialCreditsUsageForUsers(
        Sets.newHashSet(user), allBQCosts, Collections.emptyMap());
    assertSingleWorkspaceTestDbState(user, workspace, 150.0);
  }

  // do not reactivate workspaces if the new dollar limit is still below the usage

  @Test
  public void setFreeTierDollarOverride_under_usage() {
    workbenchConfig.billing.defaultInitialCreditsDollarLimit = 100.0;

    Map<String, Double> allBQCosts = Maps.newHashMap();
    allBQCosts.put(SINGLE_WORKSPACE_TEST_PROJECT, 300.0);

    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    final DbWorkspace workspace = createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT);

    commitTransaction();

    assertThat(initialCreditsService.getCachedInitialCreditsUsage(user)).isNull();
    assertWithinBillingTolerance(initialCreditsService.getUserInitialCreditsLimit(user), 100.0);

    initialCreditsService.checkInitialCreditsUsageForUsers(
        Sets.newHashSet(user), allBQCosts, Collections.emptyMap());
    verify(taskQueueService)
        .pushInitialCreditsExhaustionTask(
            argThat(new UserListMatcher(List.of(user.getUserId()))),
            argThat(new MapMatcher(Map.of(user.getUserId(), 0.0d))),
            argThat(new MapMatcher(Map.of(user.getUserId(), 300.0))));
    assertSingleWorkspaceTestDbState(user, workspace, 300.0);
    assertWithinBillingTolerance(initialCreditsService.getCachedInitialCreditsUsage(user), 300.0);

    initialCreditsService.maybeSetDollarLimitOverride(user, 200.0);
    verify(mockUserServiceAuditor).fireSetInitialCreditsOverride(user.getUserId(), null, 200.0);
    assertWithinBillingTolerance(initialCreditsService.getUserInitialCreditsLimit(user), 200.0);
    assertSingleWorkspaceTestDbState(user, workspace, 300.0);

    initialCreditsService.checkInitialCreditsUsageForUsers(
        Sets.newHashSet(user), allBQCosts, Collections.emptyMap());
    assertSingleWorkspaceTestDbState(user, workspace, 300.0);

    verify(mockUserServiceAuditor).fireSetInitialCreditsOverride(user.getUserId(), null, 200.0);
  }

  @Test
  public void checkInitialCreditsUsage_combinedProjectsExceedsLimit() {
    final String proj1 = "proj-1";
    final String proj2 = "proj-2";
    final double cost1 = 123.45;
    final double cost2 = 234.56;
    final double sum = cost1 + cost2;

    workbenchConfig.billing.defaultInitialCreditsDollarLimit = sum - 0.01;

    Map<String, Double> allBQCosts = Maps.newHashMap();
    allBQCosts.put(proj1, cost1);
    allBQCosts.put(proj2, cost2);

    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    final DbWorkspace ws1 = createWorkspace(user, proj1);
    final DbWorkspace ws2 = createWorkspace(user, proj2);

    commitTransaction();

    initialCreditsService.checkInitialCreditsUsageForUsers(
        Sets.newHashSet(user), allBQCosts, Collections.emptyMap());
    verify(taskQueueService)
        .pushInitialCreditsExhaustionTask(
            argThat(new UserListMatcher(List.of(user.getUserId()))),
            argThat(new MapMatcher(Map.of(user.getUserId(), 0.0d))),
            argThat(new MapMatcher(Map.of(user.getUserId(), cost1 + cost2))));
    assertThat(workspaceFreeTierUsageDao.count()).isEqualTo(2);

    final DbWorkspaceFreeTierUsage usage1 = workspaceFreeTierUsageDao.findOneByWorkspace(ws1);
    assertThat(usage1.getUser()).isEqualTo(user);
    assertWithinBillingTolerance(usage1.getCost(), cost1);

    final DbWorkspaceFreeTierUsage usage2 = workspaceFreeTierUsageDao.findOneByWorkspace(ws2);
    assertThat(usage2.getUser()).isEqualTo(user);
    assertWithinBillingTolerance(usage2.getCost(), cost2);
  }

  @Test
  public void checkInitialCreditsUsage_twoUsers() {
    final String proj1 = "proj-1";
    final String proj2 = "proj-2";
    final double cost1 = 123.45;
    final double cost2 = 234.56;

    workbenchConfig.billing.defaultInitialCreditsDollarLimit = Math.min(cost1, cost2) - 0.01;

    Map<String, Double> allBQCosts = Maps.newHashMap();
    allBQCosts.put(proj1, cost1);
    allBQCosts.put(proj2, cost2);

    DbUser user1 = createUser(SINGLE_WORKSPACE_TEST_USER);
    DbWorkspace ws1 = createWorkspace(user1, proj1);
    DbUser user2 = createUser("more@test.com");
    DbWorkspace ws2 = createWorkspace(user2, proj2);

    commitTransaction();

    initialCreditsService.checkInitialCreditsUsageForUsers(
        Sets.newHashSet(user1, user2), allBQCosts, Collections.emptyMap());
    verify(taskQueueService)
        .pushInitialCreditsExhaustionTask(
            argThat(new UserListMatcher(List.of(user1.getUserId(), user2.getUserId()))),
            argThat(new MapMatcher(Map.of(user1.getUserId(), 0.0d, user2.getUserId(), 0.0d))),
            argThat(new MapMatcher(Map.of(user1.getUserId(), cost1, user2.getUserId(), cost2))));

    assertThat(workspaceFreeTierUsageDao.count()).isEqualTo(2);

    // confirm DB updates after checkInitialCreditsUsage()
    final DbWorkspaceFreeTierUsage usage1 = workspaceFreeTierUsageDao.findOneByWorkspace(ws1);
    assertThat(usage1.getUser()).isEqualTo(user1);
    assertWithinBillingTolerance(usage1.getCost(), cost1);

    final DbWorkspaceFreeTierUsage usage2 = workspaceFreeTierUsageDao.findOneByWorkspace(ws2);
    assertThat(usage2.getUser()).isEqualTo(user2);
    assertWithinBillingTolerance(usage2.getCost(), cost2);
  }

  @Test
  public void checkInitialCreditsUsage_dbUpdate() {
    workbenchConfig.billing.defaultInitialCreditsDollarLimit = 100.0;
    Map<String, Double> allBQCosts = Maps.newHashMap();
    allBQCosts.put(SINGLE_WORKSPACE_TEST_PROJECT, 100.01);

    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    final DbWorkspace workspace = createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT);

    commitTransaction();

    initialCreditsService.checkInitialCreditsUsageForUsers(
        Sets.newHashSet(user), allBQCosts, Collections.emptyMap());
    verify(taskQueueService)
        .pushInitialCreditsExhaustionTask(
            argThat(new UserListMatcher(List.of(user.getUserId()))),
            argThat(new MapMatcher(Map.of(user.getUserId(), 0.0d))),
            argThat(new MapMatcher(Map.of(user.getUserId(), 100.01))));
    assertSingleWorkspaceTestDbState(user, workspace, 100.01);

    Timestamp t0 = workspaceFreeTierUsageDao.findAll().iterator().next().getLastUpdateTime();

    // time elapses, and this project incurs additional cost

    final double newTotalCost = 123.45;
    allBQCosts.put(SINGLE_WORKSPACE_TEST_PROJECT, newTotalCost);

    // we do not alert again, but the cost field is updated in the DB

    initialCreditsService.checkInitialCreditsUsageForUsers(
        Sets.newHashSet(user), allBQCosts, Collections.emptyMap());
    verify(taskQueueService, times(1))
        .pushInitialCreditsExhaustionTask(
            argThat(new UserListMatcher(List.of(user.getUserId()))),
            argThat(new MapMatcher(Map.of(user.getUserId(), 100.01d))),
            argThat(new MapMatcher(Map.of(user.getUserId(), 123.45))));

    assertThat(workspaceFreeTierUsageDao.count()).isEqualTo(1);
    DbWorkspaceFreeTierUsage dbEntry = workspaceFreeTierUsageDao.findAll().iterator().next();
    assertThat(dbEntry.getUser()).isEqualTo(user);
    assertThat(dbEntry.getWorkspace()).isEqualTo(workspace);
    assertWithinBillingTolerance(dbEntry.getCost(), newTotalCost);
    Timestamp t1 = dbEntry.getLastUpdateTime();
    assertThat(t1.after(t0)).isTrue();
  }

  // Regression test coverage for RW-8328.
  @Test
  public void checkInitialCreditsUsage_singleAlertForExhaustedAndByoBilling() {
    workbenchConfig.billing.defaultInitialCreditsDollarLimit = 100.0;
    Map<String, Double> allBQCosts = Maps.newHashMap();
    allBQCosts.put(SINGLE_WORKSPACE_TEST_PROJECT, 100.01);

    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    DbWorkspace workspace = createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT);

    commitTransaction();

    initialCreditsService.checkInitialCreditsUsageForUsers(
        Sets.newHashSet(user), allBQCosts, Collections.emptyMap());
    verify(taskQueueService)
        .pushInitialCreditsExhaustionTask(
            argThat(new UserListMatcher(List.of(user.getUserId()))),
            argThat(new MapMatcher(Map.of(user.getUserId(), 0.0d))),
            argThat(new MapMatcher(Map.of(user.getUserId(), 100.01))));

    // Simulate the user attaching their own billing account to the previously free tier workspace.
    TestTransaction.start();
    workspaceDao.save(workspace.setBillingAccountName(fullBillingAccountName("byo-account")));

    commitTransaction();
  }

  @Test
  public void getUserInitialCreditsLimit_default() {
    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);

    commitTransaction();

    final double initialFreeCreditsDollarLimit = 1.0;
    workbenchConfig.billing.defaultInitialCreditsDollarLimit = initialFreeCreditsDollarLimit;
    assertWithinBillingTolerance(
        initialCreditsService.getUserInitialCreditsLimit(user), initialFreeCreditsDollarLimit);

    final double fractionalFreeCreditsDollarLimit = 123.456;
    workbenchConfig.billing.defaultInitialCreditsDollarLimit = fractionalFreeCreditsDollarLimit;
    assertWithinBillingTolerance(
        initialCreditsService.getUserInitialCreditsLimit(user), fractionalFreeCreditsDollarLimit);
  }

  @Test
  public void getUserInitialCreditsLimit_override() {
    workbenchConfig.billing.defaultInitialCreditsDollarLimit = 123.456;

    DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);

    commitTransaction();

    final double limit1 = 100.0;
    initialCreditsService.maybeSetDollarLimitOverride(user, limit1);
    verify(mockUserServiceAuditor).fireSetInitialCreditsOverride(user.getUserId(), null, limit1);
    assertWithinBillingTolerance(initialCreditsService.getUserInitialCreditsLimit(user), limit1);

    final double limit2 = 200.0;

    user = userDao.findUserByUserId(user.getUserId());

    initialCreditsService.maybeSetDollarLimitOverride(user, limit2);
    verify(mockUserServiceAuditor).fireSetInitialCreditsOverride(user.getUserId(), limit1, limit2);
    assertWithinBillingTolerance(initialCreditsService.getUserInitialCreditsLimit(user), limit2);
  }

  @Test
  public void getUserCachedFreeTierUsage() {
    workbenchConfig.billing.defaultInitialCreditsDollarLimit = 100.0;

    Map<String, Double> allBQCosts = Maps.newHashMap();
    allBQCosts.put(SINGLE_WORKSPACE_TEST_PROJECT, 100.01);

    final DbUser user1 = createUser(SINGLE_WORKSPACE_TEST_USER);
    createWorkspace(user1, SINGLE_WORKSPACE_TEST_PROJECT);
    final double user2Costs = 999.0;

    final DbUser user2 = createUser("another user");
    createWorkspace(user2, "project 3");

    commitTransaction();

    // we have not yet had a chance to cache this usage
    assertThat(initialCreditsService.getCachedInitialCreditsUsage(user1)).isNull();
    assertThat(initialCreditsService.userHasRemainingInitialCredits(user1)).isTrue();

    initialCreditsService.checkInitialCreditsUsageForUsers(
        Sets.newHashSet(user1), allBQCosts, Collections.emptyMap());

    assertWithinBillingTolerance(initialCreditsService.getCachedInitialCreditsUsage(user1), 100.01);
    assertThat(initialCreditsService.userHasRemainingInitialCredits(user1)).isFalse();

    TestTransaction.start();
    createWorkspace(user1, "another project");
    commitTransaction();

    final Map<String, Double> costs =
        ImmutableMap.of(SINGLE_WORKSPACE_TEST_PROJECT, 1000.0, "another project", 200.0);

    // we have not yet cached the new workspace costs
    assertWithinBillingTolerance(initialCreditsService.getCachedInitialCreditsUsage(user1), 100.01);

    initialCreditsService.checkInitialCreditsUsageForUsers(
        Sets.newHashSet(user1), costs, Collections.emptyMap());
    final double expectedTotalCachedFreeTierUsage = 1000.0 + 200.0;
    assertWithinBillingTolerance(
        initialCreditsService.getCachedInitialCreditsUsage(user1),
        expectedTotalCachedFreeTierUsage);

    initialCreditsService.checkInitialCreditsUsageForUsers(
        Sets.newHashSet(user1, user2),
        ImmutableMap.of("project 3", user2Costs),
        Collections.emptyMap());

    assertWithinBillingTolerance(
        initialCreditsService.getCachedInitialCreditsUsage(user1),
        expectedTotalCachedFreeTierUsage);
    assertWithinBillingTolerance(
        initialCreditsService.getCachedInitialCreditsUsage(user2), user2Costs);
    assertThat(initialCreditsService.userHasRemainingInitialCredits(user2)).isFalse();
  }

  @Test
  public void userHasRemainingFreeTierCredits_newUser() {
    workbenchConfig.billing.defaultInitialCreditsDollarLimit = 100.0;
    final DbUser user1 = createUser(SINGLE_WORKSPACE_TEST_USER);
    assertThat(initialCreditsService.userHasRemainingInitialCredits(user1)).isTrue();
  }

  @Test
  public void userHasRemainingInitialCredits() {
    workbenchConfig.billing.defaultInitialCreditsDollarLimit = 100.0;

    final DbUser user1 = createUser(SINGLE_WORKSPACE_TEST_USER);
    createWorkspace(user1, SINGLE_WORKSPACE_TEST_PROJECT);

    commitTransaction();

    // 99.99 < 100.0
    Map<String, Double> allBQCosts = ImmutableMap.of(SINGLE_WORKSPACE_TEST_PROJECT, 99.99);

    initialCreditsService.checkInitialCreditsUsageForUsers(
        Sets.newHashSet(user1), allBQCosts, Collections.emptyMap());
    assertThat(initialCreditsService.userHasRemainingInitialCredits(user1)).isTrue();

    // 100.01 > 100.0
    allBQCosts = ImmutableMap.of(SINGLE_WORKSPACE_TEST_PROJECT, 100.01);
    initialCreditsService.checkInitialCreditsUsageForUsers(
        Sets.newHashSet(user1), allBQCosts, Collections.emptyMap());
    assertThat(initialCreditsService.userHasRemainingInitialCredits(user1)).isFalse();

    // 100.01 < 200.0
    workbenchConfig.billing.defaultInitialCreditsDollarLimit = 200.0;
    assertThat(initialCreditsService.userHasRemainingInitialCredits(user1)).isTrue();
  }

  @Test
  public void test_disableOnlyFreeTierWorkspaces() {
    workbenchConfig.billing.defaultInitialCreditsDollarLimit = 100.0;

    Map<String, Double> allBQCosts = ImmutableMap.of(SINGLE_WORKSPACE_TEST_PROJECT, 100.01);

    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    final DbWorkspace freeTierWorkspace = createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT);
    final DbWorkspace userAccountWorkspace =
        new DbWorkspace()
            .setCreator(user)
            .setWorkspaceNamespace("some other namespace")
            .setGoogleProject("other project")
            .setBillingAccountName("some other account");
    workspaceDao.save(userAccountWorkspace);

    commitTransaction();

    initialCreditsService.checkInitialCreditsUsageForUsers(
        Sets.newHashSet(user), allBQCosts, Collections.emptyMap());
    verify(taskQueueService)
        .pushInitialCreditsExhaustionTask(
            argThat(new UserListMatcher(List.of(user.getUserId()))),
            argThat(new MapMatcher(Map.of(user.getUserId(), 0.0d))),
            argThat(new MapMatcher(Map.of(user.getUserId(), 100.01d))));

    assertSingleWorkspaceTestDbState(user, freeTierWorkspace, 100.01);

    final DbWorkspace retrievedWorkspace =
        workspaceDao.findById(userAccountWorkspace.getWorkspaceId()).get();
    assertThat(retrievedWorkspace.isInitialCreditsExhausted()).isEqualTo(false);
    assertThat(initialCreditsService.areUserCreditsExpired(user)).isEqualTo(false);
  }

  @Test
  public void test_deletedWorkspaceUsageIsConsidered_whenChargeIsPostedAfterCron() {
    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    DbWorkspace workspace = createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT);

    commitTransaction();

    workbenchConfig.billing.defaultInitialCreditsDollarLimit = 100.0;
    Map<String, Double> allBQCosts = ImmutableMap.of(SINGLE_WORKSPACE_TEST_PROJECT, 50.0);

    TestTransaction.start();
    workspace.setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.DELETED);
    workspace.setLastModifiedTime(Timestamp.valueOf(LocalDateTime.now()));
    workspaceDao.save(workspace);
    commitTransaction();

    initialCreditsService.checkInitialCreditsUsageForUsers(
        Sets.newHashSet(user), allBQCosts, Collections.emptyMap());

    allBQCosts = ImmutableMap.of(SINGLE_WORKSPACE_TEST_PROJECT, 100.1);

    initialCreditsService.checkInitialCreditsUsageForUsers(
        Sets.newHashSet(user), allBQCosts, Collections.emptyMap());
    verify(taskQueueService)
        .pushInitialCreditsExhaustionTask(
            argThat(new UserListMatcher(List.of(user.getUserId()))),
            argThat(new MapMatcher(Map.of(user.getUserId(), 50.0d))),
            argThat(new MapMatcher(Map.of(user.getUserId(), 100.1))));
  }

  @Test
  public void test_deletedWorkspaceUsageIsConsidered_whenAnotherWorkspaceExceedsLimitAfterCron() {
    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    DbWorkspace workspace = createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT);

    commitTransaction();

    workbenchConfig.billing.defaultInitialCreditsDollarLimit = 100.0;
    Map<String, Double> allBQCosts = new HashMap<>();
    allBQCosts.put(SINGLE_WORKSPACE_TEST_PROJECT, 50.0);

    TestTransaction.start();
    workspace.setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.DELETED);
    workspace.setLastModifiedTime(Timestamp.valueOf(LocalDateTime.now()));
    workspaceDao.save(workspace);
    commitTransaction();

    initialCreditsService.checkInitialCreditsUsageForUsers(
        Sets.newHashSet(user), allBQCosts, Collections.emptyMap());

    TestTransaction.start();
    DbWorkspace anotherWorkspace = createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT + "4");
    commitTransaction();

    assertSingleWorkspaceTestDbState(user, workspace, 50);

    allBQCosts.put(SINGLE_WORKSPACE_TEST_PROJECT + "4", 100.1);

    initialCreditsService.checkInitialCreditsUsageForUsers(
        Sets.newHashSet(user), allBQCosts, Collections.emptyMap());
    verify(taskQueueService)
        .pushInitialCreditsExhaustionTask(
            argThat(new UserListMatcher(List.of(user.getUserId()))),
            argThat(new MapMatcher(Map.of(user.getUserId(), 50.0d))),
            argThat(new MapMatcher(Map.of(user.getUserId(), 150.1d))));

    assertSingleWorkspaceTestDbState(user, workspace, 50);
    assertSingleWorkspaceTestDbState(user, anotherWorkspace, 100.1);
  }

  @Test
  public void test_initial_credit_expiration_disabled() {
    workbenchConfig.featureFlags.enableInitialCreditsExpiration = false;
    DbUser user = new DbUser();
    assertThat(initialCreditsService.getCreditsExpiration(user)).isEmpty();
  }

  @Test
  public void test_none() {
    DbUser user = new DbUser();
    assertThat(initialCreditsService.getCreditsExpiration(user)).isEmpty();
  }

  @Test
  public void test_userBypassed() {
    DbUser user =
        new DbUser()
            .setUserInitialCreditsExpiration(
                new DbUserInitialCreditsExpiration().setBypassed(true).setExpirationTime(NOW));
    assertThat(initialCreditsService.getCreditsExpiration(user)).isEmpty();
  }

  @Test
  public void test_institutionBypassed() {
    DbUser user =
        spyUserDao.save(
            new DbUser()
                .setUserInitialCreditsExpiration(
                    new DbUserInitialCreditsExpiration()
                        .setBypassed(false)
                        .setExpirationTime(NOW)));
    when(institutionService.shouldBypassForCreditsExpiration(user)).thenReturn(true);
    assertThat(initialCreditsService.getCreditsExpiration(user)).isEmpty();
  }

  @Test
  public void test_nullExpirationTimestamp() {
    DbUser user =
        new DbUser()
            .setUserInitialCreditsExpiration(
                new DbUserInitialCreditsExpiration().setBypassed(false).setExpirationTime(null));
    assertThat(initialCreditsService.getCreditsExpiration(user)).isEmpty();
  }

  @Test
  public void test_nullExtensionTimestamp() {
    DbUser user =
        new DbUser()
            .setUserInitialCreditsExpiration(
                new DbUserInitialCreditsExpiration().setBypassed(false).setExtensionTime(null));
    assertThat(initialCreditsService.getCreditsExtension(user)).isEmpty();
  }

  @Test
  public void test_validExpirationTimestamp() {
    DbUser user =
        new DbUser()
            .setUserInitialCreditsExpiration(
                new DbUserInitialCreditsExpiration().setBypassed(false).setExpirationTime(NOW));
    assertThat(initialCreditsService.getCreditsExpiration(user)).hasValue(NOW);
  }

  @Test
  public void test_validExtensionTimestamp() {
    DbUser user =
        new DbUser()
            .setUserInitialCreditsExpiration(
                new DbUserInitialCreditsExpiration().setBypassed(false).setExtensionTime(NOW));
    assertThat(initialCreditsService.getCreditsExtension(user)).hasValue(NOW);
  }

  @Test
  public void test_checkCreditsExpirationForUserIDs_null() {
    initialCreditsService.checkCreditsExpirationForUserIDs(null);
    verify(spyUserDao, never()).findAllById(any());
  }

  @Test
  public void test_checkCreditsExpirationForUserIDs_emptyList() {
    initialCreditsService.checkCreditsExpirationForUserIDs(new ArrayList<>());
    verify(spyUserDao, never()).findAllById(any());
  }

  @Test
  public void test_checkCreditsExpirationForUserIDs_noExpirationRecord() {
    DbUser user = spyUserDao.save(new DbUser());
    when(spyUserDao.findAllById(List.of(user.getUserId()))).thenReturn(List.of(user));

    initialCreditsService.checkCreditsExpirationForUserIDs(List.of(user.getUserId()));

    verify(leonardoApiClient, never()).deleteAllResources(workspace.getGoogleProject(), false);
    assertNull(user.getUserInitialCreditsExpiration());
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("dataForUsersWithAnExpirationRecord")
  public void test_checkCreditsExpirationForUserIDs_withExpirationRecord(
      String testName,
      Timestamp expirationTime,
      boolean bypassed,
      Timestamp expectedWarningNotificationTime,
      Timestamp expectedCleanupTime) {
    DbUser user =
        spyUserDao.save(
            new DbUser()
                .setUserInitialCreditsExpiration(
                    new DbUserInitialCreditsExpiration()
                        .setExpirationTime(expirationTime)
                        .setBypassed(bypassed)));
    when(spyUserDao.findAllById(List.of(user.getUserId()))).thenReturn(List.of(user));

    initialCreditsService.checkCreditsExpirationForUserIDs(List.of(user.getUserId()));

    assertEquals(
        user.getUserInitialCreditsExpiration().getApproachingExpirationNotificationTime(),
        expectedWarningNotificationTime);
    assertEquals(
        user.getUserInitialCreditsExpiration().getExpirationCleanupTime(), expectedCleanupTime);
  }

  static Stream<Arguments> dataForUsersWithAnExpirationRecord() {
    return Stream.of(
        Arguments.of(
            "If a bypassed user is before their warning period, they will not receive a warning email or have their resources cleaned up.",
            BEFORE_WARNING_PERIOD,
            true,
            null,
            null),
        Arguments.of(
            "If a non-bypassed user is before their warning period, they will not receive a warning email or have their resources cleaned up.",
            BEFORE_WARNING_PERIOD,
            false,
            null,
            null),
        Arguments.of(
            "If a bypassed user is in their warning period, they will not receive a warning email or have their resources cleaned up.",
            DURING_WARNING_PERIOD,
            true,
            null,
            null),
        Arguments.of(
            "If a non-bypassed user is in their warning period, they will receive a warning email but not have their resources cleaned up.",
            DURING_WARNING_PERIOD,
            false,
            null,
            null),
        Arguments.of(
            "If a bypassed user has passed their expiration date, they will not receive a warning email or have their resources cleaned up.",
            PAST_EXPIRATION,
            true,
            null,
            null),
        Arguments.of(
            "If a non-bypassed user has passed their expiration date, they will not receive a warning email but will have their resources cleaned up.",
            PAST_EXPIRATION,
            false,
            null,
            NOW));
  }

  @Test
  public void test_extendInitialCreditsExpiration_extensionDisabled() {
    workbenchConfig.featureFlags.enableInitialCreditsExpiration = false;

    DbUser user = spyUserDao.save(new DbUser());

    BadRequestException exception =
        assertThrows(
            BadRequestException.class,
            () -> initialCreditsService.extendInitialCreditsExpiration(user));
    assertEquals("Initial credits extension is disabled.", exception.getMessage());
  }

  @Test
  public void test_checkInitialCreditsExtensionEligibility_noRemainingCredits() {
    workbenchConfig.billing.defaultInitialCreditsDollarLimit = 100.0;

    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    user.setUserInitialCreditsExpiration(
        new DbUserInitialCreditsExpiration()
            .setCreditStartTime(NOW)
            .setExpirationTime(NOW)
            .setExtensionTime(null));

    workspaceFreeTierUsageDao.save(
        new DbWorkspaceFreeTierUsage(workspace).setUser(user).setCost(300.0));
    boolean eligibility = initialCreditsService.checkInitialCreditsExtensionEligibility(user);
    assertThat(eligibility).isFalse();
  }

  @Test
  public void test_checkInitialCreditsExtensionEligibility_hasRemainingCredits() {
    workbenchConfig.billing.defaultInitialCreditsDollarLimit = 100.0;

    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    user.setUserInitialCreditsExpiration(
        new DbUserInitialCreditsExpiration()
            .setCreditStartTime(NOW)
            .setExpirationTime(NOW)
            .setExtensionTime(null));

    workspaceFreeTierUsageDao.save(
        new DbWorkspaceFreeTierUsage(workspace).setUser(user).setCost(30.0));
    boolean eligibility = initialCreditsService.checkInitialCreditsExtensionEligibility(user);
    assertThat(eligibility).isFalse();
  }

  @Test
  public void checkInitialCreditsUsageForUsers_withVwbCosts_calculatesExhaustionCorrectly() {
    final String proj1 = "proj-1";
    final String proj2 = "proj-2";
    final double cost1 = 123.45;
    final double cost2 = 234.56;
    final double vwbCost1 = 0.55;
    final double vwbCost2 = 0.44;

    workbenchConfig.billing.defaultInitialCreditsDollarLimit = Math.min(cost1, cost2) - 0.01;
    workbenchConfig.featureFlags.enableVWBInitialCreditsExhaustion = true;

    Map<String, Double> allBQCosts = Maps.newHashMap();
    allBQCosts.put(proj1, cost1);
    allBQCosts.put(proj2, cost2);

    DbUser user1 = createUser(SINGLE_WORKSPACE_TEST_USER);
    DbWorkspace ws1 = createWorkspace(user1, proj1);
    DbUser user2 = createUser("more@test.com");
    DbWorkspace ws2 = createWorkspace(user2, proj2);
    DbVwbUserPod vwbPod1 = createVwbPodForUser(user1, true);
    DbVwbUserPod vwbPod2 = createVwbPodForUser(user2, true);
    user1.setVwbUserPod(vwbPod1);
    user2.setVwbUserPod(vwbPod2);
    userDao.save(user1);
    userDao.save(user2);

    commitTransaction();

    Map<Long, Double> vwbCosts = new HashMap<>();
    vwbCosts.put(user1.getUserId(), vwbCost1);
    vwbCosts.put(user2.getUserId(), vwbCost2);

    initialCreditsService.checkInitialCreditsUsageForUsers(
        Sets.newHashSet(user1, user2), allBQCosts, vwbCosts);
    verify(taskQueueService)
        .pushInitialCreditsExhaustionTask(
            argThat(new UserListMatcher(List.of(user1.getUserId(), user2.getUserId()))),
            argThat(new MapMatcher(Map.of(user1.getUserId(), 0.0d, user2.getUserId(), 0.0d))),
            argThat(
                new MapMatcher(
                    Map.of(
                        user1.getUserId(),
                        cost1 + vwbCost1,
                        user2.getUserId(),
                        cost2 + vwbCost2))));

    assertThat(workspaceFreeTierUsageDao.count()).isEqualTo(2);

    // confirm DB updates after checkInitialCreditsUsage()
    final DbWorkspaceFreeTierUsage usage1 = workspaceFreeTierUsageDao.findOneByWorkspace(ws1);
    assertThat(usage1.getUser()).isEqualTo(user1);
    assertWithinBillingTolerance(usage1.getCost(), cost1);

    final DbWorkspaceFreeTierUsage usage2 = workspaceFreeTierUsageDao.findOneByWorkspace(ws2);
    assertThat(usage2.getUser()).isEqualTo(user2);
    assertWithinBillingTolerance(usage2.getCost(), cost2);

    // Confirm VwbPod costs are correctly applied
    Optional<DbVwbUserPod> pod1 = vwbUserPodDao.findById(vwbPod1.getVwbUserPodId());
    assertThat(pod1).isPresent();
    assertWithinBillingTolerance(vwbCost1, pod1.get().getCost());
    Optional<DbVwbUserPod> pod2 = vwbUserPodDao.findById(vwbPod2.getVwbUserPodId());
    assertThat(pod2).isPresent();
    assertWithinBillingTolerance(vwbCost2, pod2.get().getCost());
  }

  @Test
  public void
      maybeSetDollarLimitOverride_callsLinkInitialCreditsAccountAndSetVwbInitialCreditsActive() {
    // Arrange
    workbenchConfig.featureFlags.enableVWBInitialCreditsExhaustion = true;
    DbUser user = createUser("vwbuser@test.com");
    DbVwbUserPod pod = createVwbPodForUser(user, false);
    user.setVwbUserPod(pod);
    userDao.save(user);

    initialCreditsService.maybeSetDollarLimitOverride(user, 200);

    DbVwbUserPod updatedPod = vwbUserPodDao.findById(pod.getVwbUserPodId()).get();
    verify(vwbUserService, times(1)).linkInitialCreditsBillingAccountToPod(pod);
    assertThat(updatedPod.isInitialCreditsActive()).isTrue();
  }

  @Test
  public void checkInitialCreditsUsage_includesAllVwbPodsForAccurateDbCostCalculation() {
    // This test verifies that VWB pod costs are included in DB cost calculations
    // regardless of when they were recently updated. Before the fix, only "not recently updated"
    // pods were included, which caused inaccurate DB cost calculations and incorrect
    // downstream comparisons with live costs.

    final String proj1 = "proj-1";
    final double terraWorkspaceCost = 100.0;
    final double vwbPodCostInDb = 50.0;
    final double vwbPodLiveCost = 50.0; // Same as DB cost (recently updated)

    workbenchConfig.billing.defaultInitialCreditsDollarLimit = 200.0;
    workbenchConfig.featureFlags.enableVWBInitialCreditsExhaustion = true;
    workbenchConfig.billing.minutesBeforeLastInitialCreditsJob = 60;

    // Create user with Terra workspace and VWB pod
    DbUser user = createUser("user@test.com");
    DbWorkspace ws1 = createWorkspace(user, proj1);
    DbVwbUserPod vwbPod = createVwbPodForUser(user, true);

    // Set the pod cost to 50.0 and mark it as recently updated
    vwbPod.setCost(vwbPodCostInDb);
    vwbUserPodDao.save(vwbPod);

    user.setVwbUserPod(vwbPod);
    userDao.save(user);

    commitTransaction();

    // Live costs from BigQuery
    Map<String, Double> terraBQCosts = Maps.newHashMap();
    terraBQCosts.put(proj1, terraWorkspaceCost);

    Map<Long, Double> vwbLiveCosts = new HashMap<>();
    vwbLiveCosts.put(user.getUserId(), vwbPodLiveCost);

    // Act: Check initial credits usage
    initialCreditsService.checkInitialCreditsUsageForUsers(
        Sets.newHashSet(user), terraBQCosts, vwbLiveCosts);

    // Assert: The DB cost should include the VWB pod cost even though it was recently updated
    // The total DB cost should be: terraWorkspaceCost (0 initially, will be set to 100) + vwbPodCostInDb (50)
    // The total live cost should be: terraWorkspaceCost (100) + vwbPodLiveCost (50) = 150

    // Before the fix, the VWB pod cost would NOT be included in DB cost calculation
    // because it was recently updated, leading to incorrect comparison:
    // - dbCostByCreator would be 0.0 (missing the 50.0 from VWB pod)
    // - liveCostByCreator would be 150.0
    // This would cause incorrect threshold calculations

    // After the fix, all VWB pods are included:
    // - dbCostByCreator includes the VWB pod cost: 50.0
    // - liveCostByCreator is 150.0
    // This ensures accurate cost comparison

    verify(taskQueueService)
        .pushInitialCreditsExhaustionTask(
            argThat(new UserListMatcher(List.of(user.getUserId()))),
            // DB cost should include VWB pod cost (50.0) + initial workspace cost (0.0) = 50.0
            argThat(new MapMatcher(Map.of(user.getUserId(), vwbPodCostInDb))),
            // Live cost is terra (100) + vwb (50) = 150.0
            argThat(new MapMatcher(Map.of(user.getUserId(), terraWorkspaceCost + vwbPodLiveCost))));

    // Verify the workspace was updated
    final DbWorkspaceFreeTierUsage usage1 = workspaceFreeTierUsageDao.findOneByWorkspace(ws1);
    assertThat(usage1.getUser()).isEqualTo(user);
    assertWithinBillingTolerance(usage1.getCost(), terraWorkspaceCost);
  }

  private void assertSingleWorkspaceTestDbState(
      DbUser user, DbWorkspace workspaceForQuerying, double cost) {

    final DbWorkspace workspace =
        workspaceDao.findById(workspaceForQuerying.getWorkspaceId()).get();

    final DbWorkspaceFreeTierUsage dbEntry =
        workspaceFreeTierUsageDao.findOneByWorkspace(workspaceForQuerying);

    assertThat(dbEntry.getUser()).isEqualTo(user);
    assertThat(dbEntry.getWorkspace()).isEqualTo(workspace);
    assertWithinBillingTolerance(dbEntry.getCost(), cost);
  }

  private DbUser createUser(String email) {
    return userDao.save(new DbUser().setUsername(email));
  }

  private DbWorkspace createWorkspace(DbUser creator, String project) {
    return workspaceDao.save(
        new DbWorkspace()
            .setCreator(creator)
            .setWorkspaceNamespace(project + "-ns")
            .setGoogleProject(project)
            .setBillingAccountName(workbenchConfig.billing.initialCreditsBillingAccountName()));
  }

  private DbVwbUserPod createVwbPodForUser(DbUser user, boolean initialCreditsActive) {
    return vwbUserPodDao.save(
        new DbVwbUserPod()
            .setUser(user)
            .setVwbPodId("pod" + user.getUserId())
            .setInitialCreditsActive(initialCreditsActive));
  }

  private void assertWithinBillingTolerance(double actualValue, double expectedValue) {
    final double tolerance = DEFAULT_PERCENTAGE_TOLERANCE * expectedValue * 0.01;
    assertThat(actualValue).isWithin(tolerance).of(expectedValue);
  }

  private void commitTransaction() {
    TestTransaction.flagForCommit();
    TestTransaction.end();
  }

  @ParameterizedTest(name = "Test with enableUnlinkBillingForInitialCredits={0}")
  @MethodSource("unlinkBillingFlagProvider")
  public void test_checkCreditsExpirationForUserIDs_onlyStopsSpendForInitialCreditsWorkspaces(
      boolean unlinkBillingEnabled) {
    // ARRANGE
    // Set the feature flag
    workbenchConfig.featureFlags.enableUnlinkBillingForInitialCredits = unlinkBillingEnabled;

    // Create a user with an expiration that has passed
    DbUser user = new DbUser().setUsername(SINGLE_WORKSPACE_TEST_USER);
    DbUserInitialCreditsExpiration expiration =
        new DbUserInitialCreditsExpiration()
            .setExpirationTime(PAST_EXPIRATION)
            .setBypassed(false)
            .setUser(user);
    user.setUserInitialCreditsExpiration(expiration);
    user = spyUserDao.save(user);

    // Create DbWorkspace entities directly in the database
    DbWorkspace initialCreditsWorkspace =
        workspaceDao.save(
            new DbWorkspace()
                .setCreator(user)
                .setWorkspaceNamespace("initial-credits-namespace")
                .setGoogleProject("initial-credits-project")
                .setBillingAccountName(workbenchConfig.billing.initialCreditsBillingAccountName())
                .setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.ACTIVE));

    DbWorkspace nonInitialCreditsWorkspace =
        workspaceDao.save(
            new DbWorkspace()
                .setCreator(user)
                .setWorkspaceNamespace("non-initial-credits-namespace")
                .setGoogleProject("non-initial-credits-project")
                .setBillingAccountName("some-other-billing-account")
                .setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.ACTIVE));

    commitTransaction();

    // Re-fetch the user to ensure we're working with the persisted version
    user = userDao.findUserByUserId(user.getUserId());

    FireCloudService mockFireCloudService = applicationContext.getBean(FireCloudService.class);

    // ACT
    // Call the method being tested
    initialCreditsService.checkCreditsExpirationForUserIDs(List.of(user.getUserId()));

    // ASSERT
    verify(leonardoApiClient).deleteAllResources(initialCreditsWorkspace.getGoogleProject(), false);
    // Verify the appropriate behavior based on the flag
    if (unlinkBillingEnabled) {
      // When flag is enabled, the billing account should be unlinked.
      verify(mockFireCloudService)
          .removeBillingAccountFromBillingProjectAsService(
              initialCreditsWorkspace.getWorkspaceNamespace());
    } else {
      // When flag is disabled, billing unlinking should not happen.
      verify(mockFireCloudService, never())
          .removeBillingAccountFromBillingProjectAsService(
              initialCreditsWorkspace.getWorkspaceNamespace());
    }

    verify(mockFireCloudService, never())
        .removeBillingAccountFromBillingProjectAsService(
            nonInitialCreditsWorkspace.getWorkspaceNamespace());
    // Verify that the user's expiration cleanup time was set
    assertEquals(
        NOW,
        userDao
            .findUserByUserId(user.getUserId())
            .getUserInitialCreditsExpiration()
            .getExpirationCleanupTime());
  }

  private static Stream<Arguments> unlinkBillingFlagProvider() {
    return Stream.of(
        Arguments.of(true), // Flag enabled
        Arguments.of(false) // Flag disabled
        );
  }

  @Test
  public void test_updateInitialCreditsExhaustion_markAsExhausted() {
    // ARRANGE
    // Create a user and initial credits workspaces
    DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    DbWorkspace initialCreditsWorkspace1 = createWorkspace(user, "initial-credits-project-1");
    DbWorkspace initialCreditsWorkspace2 = createWorkspace(user, "initial-credits-project-2");

    // Create a workspace with a different billing account (not initial credits)
    DbWorkspace nonInitialCreditsWorkspace =
        workspaceDao.save(
            new DbWorkspace()
                .setCreator(user)
                .setWorkspaceNamespace("non-initial-credits-namespace")
                .setGoogleProject("non-initial-credits-project")
                .setBillingAccountName("some-other-billing-account")
                .setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.ACTIVE));

    commitTransaction();

    FireCloudService mockFireCloudService = applicationContext.getBean(FireCloudService.class);

    // ACT
    initialCreditsService.updateInitialCreditsExhaustion(user, true);

    // ASSERT
    // Verify that initial credits workspaces were marked as exhausted
    DbWorkspace updatedWorkspace1 =
        workspaceDao.findById(initialCreditsWorkspace1.getWorkspaceId()).get();
    assertThat(updatedWorkspace1.isInitialCreditsExhausted()).isTrue();

    DbWorkspace updatedWorkspace2 =
        workspaceDao.findById(initialCreditsWorkspace2.getWorkspaceId()).get();
    assertThat(updatedWorkspace2.isInitialCreditsExhausted()).isTrue();

    // Verify that non-initial credits workspace was not marked as exhausted
    DbWorkspace updatedNonInitialWorkspace =
        workspaceDao.findById(nonInitialCreditsWorkspace.getWorkspaceId()).get();
    assertThat(updatedNonInitialWorkspace.isInitialCreditsExhausted()).isFalse();

    // Verify that resources were stopped for initial credits workspaces
    verify(leonardoApiClient)
        .deleteAllResources(initialCreditsWorkspace1.getGoogleProject(), false);
    verify(leonardoApiClient)
        .deleteAllResources(initialCreditsWorkspace2.getGoogleProject(), false);

    // Verify that appropriate billing account behavior occurs based on flag
    if (workbenchConfig.featureFlags.enableUnlinkBillingForInitialCredits) {
      verify(mockFireCloudService)
          .removeBillingAccountFromBillingProjectAsService(
              initialCreditsWorkspace1.getWorkspaceNamespace());
      verify(mockFireCloudService)
          .removeBillingAccountFromBillingProjectAsService(
              initialCreditsWorkspace2.getWorkspaceNamespace());
    } else {
      verify(mockFireCloudService, never())
          .removeBillingAccountFromBillingProjectAsService(
              initialCreditsWorkspace1.getWorkspaceNamespace());
      verify(mockFireCloudService, never())
          .removeBillingAccountFromBillingProjectAsService(
              initialCreditsWorkspace2.getWorkspaceNamespace());
    }

    // Verify that resources were not stopped for non-initial credits workspace
    verify(leonardoApiClient, never())
        .deleteAllResources(nonInitialCreditsWorkspace.getGoogleProject(), false);
    verify(mockFireCloudService, never())
        .removeBillingAccountFromBillingProjectAsService(
            nonInitialCreditsWorkspace.getWorkspaceNamespace());
  }

  @Test
  public void test_updateInitialCreditsExhaustion_markAsNotExhausted() {
    // ARRANGE
    // Create a user and exhausted initial credits workspaces
    DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    DbWorkspace initialCreditsWorkspace1 = createWorkspace(user, "initial-credits-project-1");
    initialCreditsWorkspace1.setInitialCreditsExhausted(true);
    workspaceDao.save(initialCreditsWorkspace1);

    DbWorkspace initialCreditsWorkspace2 = createWorkspace(user, "initial-credits-project-2");
    initialCreditsWorkspace2.setInitialCreditsExhausted(true);
    workspaceDao.save(initialCreditsWorkspace2);

    // Create a workspace with a different billing account
    DbWorkspace nonInitialCreditsWorkspace =
        workspaceDao.save(
            new DbWorkspace()
                .setCreator(user)
                .setWorkspaceNamespace("non-initial-credits-namespace")
                .setGoogleProject("non-initial-credits-project")
                .setBillingAccountName("some-other-billing-account")
                .setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.ACTIVE)
                .setInitialCreditsExhausted(true));

    commitTransaction();

    FireCloudService mockFireCloudService = applicationContext.getBean(FireCloudService.class);

    // ACT
    initialCreditsService.updateInitialCreditsExhaustion(user, false);

    // ASSERT
    // Verify that initial credits workspaces were marked as not exhausted
    DbWorkspace updatedWorkspace1 =
        workspaceDao.findById(initialCreditsWorkspace1.getWorkspaceId()).get();
    assertThat(updatedWorkspace1.isInitialCreditsExhausted()).isFalse();

    DbWorkspace updatedWorkspace2 =
        workspaceDao.findById(initialCreditsWorkspace2.getWorkspaceId()).get();
    assertThat(updatedWorkspace2.isInitialCreditsExhausted()).isFalse();

    // Verify that non-initial credits workspace was not affected
    DbWorkspace updatedNonInitialWorkspace =
        workspaceDao.findById(nonInitialCreditsWorkspace.getWorkspaceId()).get();
    assertThat(updatedNonInitialWorkspace.isInitialCreditsExhausted()).isTrue();

    // Verify that resources were NOT stopped (as exhausted is false)
    verify(leonardoApiClient, never()).deleteAllResources(any(String.class), any(Boolean.class));
    verify(mockFireCloudService, never())
        .removeBillingAccountFromBillingProjectAsService(any(String.class));
  }

  @Test
  public void test_updateInitialCreditsExhaustion_noWorkspaces() {
    // ARRANGE
    DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);

    // No workspaces for this user
    when(spyWorkspaceDao.findAllByCreator(user)).thenReturn(Collections.emptySet());

    // ACT
    initialCreditsService.updateInitialCreditsExhaustion(user, true);

    // ASSERT
    // Verify no interactions with APIs as there are no workspaces
    verifyNoInteractions(leonardoApiClient);
    FireCloudService mockFireCloudService = applicationContext.getBean(FireCloudService.class);
    verify(mockFireCloudService, never()).removeBillingAccountFromBillingProjectAsService(any());

    // Verify workspaceDao.saveAll was called (even though the iterable was empty)
    verify(spyWorkspaceDao).saveAll(any(Iterable.class));
  }

  @ParameterizedTest(name = "Test with enableUnlinkBillingForInitialCredits={0}")
  @MethodSource("unlinkBillingFlagProvider")
  public void test_updateInitialCreditsExhaustion_respectsUnlinkBillingFlag(
      boolean unlinkBillingEnabled) {
    // ARRANGE
    // Set the feature flag
    workbenchConfig.featureFlags.enableUnlinkBillingForInitialCredits = unlinkBillingEnabled;

    // Create a user and initial credits workspace
    DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    DbWorkspace initialCreditsWorkspace = createWorkspace(user, "initial-credits-project-test");

    commitTransaction();

    FireCloudService mockFireCloudService = applicationContext.getBean(FireCloudService.class);

    // ACT
    initialCreditsService.updateInitialCreditsExhaustion(user, true);

    // ASSERT
    // Verify that initial credits workspace was marked as exhausted
    DbWorkspace updatedWorkspace =
        workspaceDao.findById(initialCreditsWorkspace.getWorkspaceId()).get();
    assertThat(updatedWorkspace.isInitialCreditsExhausted()).isTrue();

    // Verify that resources were stopped
    verify(leonardoApiClient).deleteAllResources(initialCreditsWorkspace.getGoogleProject(), false);

    // Verify the appropriate behavior based on the flag
    if (unlinkBillingEnabled) {
      // When flag is enabled, the billing account should be unlinked.
      verify(mockFireCloudService)
          .removeBillingAccountFromBillingProjectAsService(
              initialCreditsWorkspace.getWorkspaceNamespace());
    } else {
      // When flag is disabled, billing unlinking should not happen.
      verify(mockFireCloudService, never())
          .removeBillingAccountFromBillingProjectAsService(
              initialCreditsWorkspace.getWorkspaceNamespace());
    }
  }
}
