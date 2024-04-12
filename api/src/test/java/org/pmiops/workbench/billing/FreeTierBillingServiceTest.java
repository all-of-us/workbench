package org.pmiops.workbench.billing;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.pmiops.workbench.billing.FreeCreditsExpiryTaskMatchers.*;

import com.google.cloud.PageImpl;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.FieldValue.Attribute;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.LegacySQLTypeName;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.TableResult;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.actionaudit.auditors.UserServiceAuditor;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.billing.FreeCreditsExpiryTaskMatchers.UserListMatcher;
import org.pmiops.workbench.cloudtasks.TaskQueueService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.dao.WorkspaceFreeTierUsageDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.db.model.DbWorkspaceFreeTierUsage;
import org.pmiops.workbench.model.BillingStatus;
import org.pmiops.workbench.model.WorkspaceActiveStatus;
import org.pmiops.workbench.test.FakeClock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.test.context.transaction.TestTransaction;

@DataJpaTest
public class FreeTierBillingServiceTest {

  private static final Instant START_INSTANT = Instant.parse("2000-01-01T00:00:00.00Z");
  private static final FakeClock CLOCK = new FakeClock(START_INSTANT);

  private static final double DEFAULT_PERCENTAGE_TOLERANCE = 0.000001;

  @MockBean private UserServiceAuditor mockUserServiceAuditor;

  @Autowired BigQueryService bigQueryService;
  @Autowired FreeTierBillingService freeTierBillingService;
  @Autowired UserDao userDao;
  @Autowired WorkspaceDao workspaceDao;
  @Autowired WorkspaceFreeTierUsageDao workspaceFreeTierUsageDao;
  @Autowired WorkspaceFreeTierUsageService workspaceFreeTierUsageService;

  @Autowired private TaskQueueService taskQueueService;

  private static WorkbenchConfig workbenchConfig;

  private static final String SINGLE_WORKSPACE_TEST_USER = "test@test.com";
  private static final String SINGLE_WORKSPACE_TEST_PROJECT = "aou-test-123";

  @TestConfiguration
  @Import({FreeTierBillingService.class, WorkspaceFreeTierUsageService.class})
  @MockBean({BigQueryService.class, TaskQueueService.class})
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
    workbenchConfig = WorkbenchConfig.createEmptyConfig();
    workbenchConfig.billing.freeTierCostAlertThresholds = new ArrayList<>(Doubles.asList(.5, .75));
    workbenchConfig.billing.accountId = "free-tier";
    workbenchConfig.billing.defaultFreeCreditsDollarLimit = 1000.0;
    workbenchConfig.billing.freeTierCronUserBatchSize = 10;
    workbenchConfig.billing.minutesBeforeLastFreeTierJob = 0;
    workbenchConfig.billing.numberOfDaysToConsiderForFreeTierUsageUpdate = 2l;

    // by default we have 0 spend
    // doReturn(mockBQTableSingleResult(0.0)).when(bigQueryService).executeQuery(any());
  }

  @AfterEach
  public void tearDown() {
    workspaceFreeTierUsageDao.deleteAll();
    workspaceDao.deleteAll();
    userDao.deleteAll();
  }

  @Test
  public void checkFreeTierBillingUsage_exceedsDollarThresholds() throws MessagingException {
    final double limit = 100.0;
    final double costUnderThreshold = 49.5;

    workbenchConfig.billing.defaultFreeCreditsDollarLimit = limit;

    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT);

    commitTransaction();

    Map<String, Double> allBQCosts = Maps.newHashMap();
    allBQCosts.put(SINGLE_WORKSPACE_TEST_PROJECT, costUnderThreshold);

    freeTierBillingService.checkFreeTierBillingUsageForUsers(Sets.newHashSet(user), allBQCosts);
    verifyNoInteractions(taskQueueService);

    // check that we alert for the 50% threshold
    double costOverThreshold = 50.5;
    allBQCosts.put(SINGLE_WORKSPACE_TEST_PROJECT, costOverThreshold);
    freeTierBillingService.checkFreeTierBillingUsageForUsers(Sets.newHashSet(user), allBQCosts);
    verify(taskQueueService)
        .pushExpiredFreeCreditsTask(
            argThat(new UserListMatcher(List.of(user.getUserId()))),
            argThat(new MapMatcher(Map.of(user.getUserId(), costUnderThreshold))),
            argThat(new MapMatcher(Map.of(user.getUserId(), costOverThreshold))));

    // check that we alert for the 75% threshold
    costOverThreshold = 75.3;

    allBQCosts.put(SINGLE_WORKSPACE_TEST_PROJECT, costOverThreshold);
    freeTierBillingService.checkFreeTierBillingUsageForUsers(Sets.newHashSet(user), allBQCosts);
    verify(taskQueueService)
        .pushExpiredFreeCreditsTask(
            argThat(new UserListMatcher(List.of(user.getUserId()))),
            argThat(new MapMatcher(Map.of(user.getUserId(), 50.5d))),
            argThat(new MapMatcher(Map.of(user.getUserId(), costOverThreshold))));

    // check that we alert for expiration when we hit 100%

    final double costToTriggerExpiration = 100.01;

    allBQCosts.put(SINGLE_WORKSPACE_TEST_PROJECT, costToTriggerExpiration);

    freeTierBillingService.checkFreeTierBillingUsageForUsers(Sets.newHashSet(user), allBQCosts);
    verify(taskQueueService)
        .pushExpiredFreeCreditsTask(
            argThat(new UserListMatcher(List.of(user.getUserId()))),
            argThat(new MapMatcher(Map.of(user.getUserId(), costOverThreshold))),
            argThat(new MapMatcher(Map.of(user.getUserId(), costToTriggerExpiration))));
  }

  @Test
  public void checkFreeTierBillingUsage_altDollarThresholds() throws MessagingException {

    // set alert thresholds at 30% and 65% instead
    workbenchConfig.billing.freeTierCostAlertThresholds = new ArrayList<>(Doubles.asList(.3, .65));

    final double limit = 100.0;
    final double costUnderThreshold = 29.9;

    double costOverThreshold = 30.1;

    workbenchConfig.billing.defaultFreeCreditsDollarLimit = limit;

    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT);

    commitTransaction();
    // check that we have not alerted before the threshold
    Map<String, Double> allBQCosts = Maps.newHashMap();
    allBQCosts.put(SINGLE_WORKSPACE_TEST_PROJECT, costUnderThreshold);

    freeTierBillingService.checkFreeTierBillingUsageForUsers(Sets.newHashSet(user), allBQCosts);
    verifyNoInteractions(taskQueueService);

    // check that we detect the 30% threshold
    allBQCosts.put(SINGLE_WORKSPACE_TEST_PROJECT, costOverThreshold);
    freeTierBillingService.checkFreeTierBillingUsageForUsers(Sets.newHashSet(user), allBQCosts);
    verify(taskQueueService)
        .pushExpiredFreeCreditsTask(
            argThat(new UserListMatcher(List.of(user.getUserId()))),
            argThat(new MapMatcher(Map.of(user.getUserId(), costUnderThreshold))),
            argThat(new MapMatcher(Map.of(user.getUserId(), costOverThreshold))));

    costOverThreshold = 65.01;

    allBQCosts.put(SINGLE_WORKSPACE_TEST_PROJECT, costOverThreshold);
    freeTierBillingService.checkFreeTierBillingUsageForUsers(Sets.newHashSet(user), allBQCosts);
    verify(taskQueueService)
        .pushExpiredFreeCreditsTask(
            argThat(new UserListMatcher(List.of(user.getUserId()))),
            argThat(
                new MapMatcher(Map.of(user.getUserId(), 30.1))), // The previous costOverThreshold
            argThat(new MapMatcher(Map.of(user.getUserId(), costOverThreshold))));

    // check that we detect expiration when we hit 100%
    final double costToTriggerExpiration = 100.01;
    allBQCosts.put(SINGLE_WORKSPACE_TEST_PROJECT, costToTriggerExpiration);

    freeTierBillingService.checkFreeTierBillingUsageForUsers(Sets.newHashSet(user), allBQCosts);
    verify(taskQueueService)
        .pushExpiredFreeCreditsTask(
            argThat(new UserListMatcher(List.of(user.getUserId()))),
            argThat(new MapMatcher(Map.of(user.getUserId(), costOverThreshold))),
            argThat(new MapMatcher(Map.of(user.getUserId(), costToTriggerExpiration))));
  }

  @Test
  public void checkFreeTierBillingUsage_disabledUserNotIgnored() {
    workbenchConfig.billing.defaultFreeCreditsDollarLimit = 100.0;
    Map<String, Double> allBQCosts = Maps.newHashMap();
    allBQCosts.put(SINGLE_WORKSPACE_TEST_PROJECT, 100.01);

    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    user.setDisabled(true);
    userDao.save(user);
    final DbWorkspace workspace = createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT);

    commitTransaction();

    freeTierBillingService.checkFreeTierBillingUsageForUsers(Sets.newHashSet(user), allBQCosts);
    verify(taskQueueService)
        .pushExpiredFreeCreditsTask(
            argThat(new UserListMatcher(List.of(user.getUserId()))),
            argThat(new MapMatcher(Map.of(user.getUserId(), 0.0d))),
            argThat(new MapMatcher(Map.of(user.getUserId(), 100.01))));

    assertSingleWorkspaceTestDbState(user, workspace, 100.01);
  }

  @Test
  public void checkFreeTierBillingUsage_deletedWorkspaceNotIgnored() throws MessagingException {
    workbenchConfig.billing.defaultFreeCreditsDollarLimit = 100.0;

    Map<String, Double> allBQCosts = Maps.newHashMap();
    allBQCosts.put(SINGLE_WORKSPACE_TEST_PROJECT, 100.01);

    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    final DbWorkspace workspace = createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT);
    workspace.setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.DELETED);
    workspaceDao.save(workspace);

    commitTransaction();

    freeTierBillingService.checkFreeTierBillingUsageForUsers(Sets.newHashSet(user), allBQCosts);
    verify(taskQueueService)
        .pushExpiredFreeCreditsTask(
            argThat(new UserListMatcher(List.of(user.getUserId()))),
            argThat(new MapMatcher(Map.of(user.getUserId(), 0.0d))),
            argThat(new MapMatcher(Map.of(user.getUserId(), 100.01))));

    assertSingleWorkspaceTestDbState(user, workspace, 100.01);
  }

  @Test
  public void checkFreeTierBillingUsage_noAlert() {
    // set limit so usage is just under the 50% threshold
    workbenchConfig.billing.defaultFreeCreditsDollarLimit = 100.0;

    Map<String, Double> allBQCosts = Maps.newHashMap();
    allBQCosts.put(SINGLE_WORKSPACE_TEST_PROJECT, 49.99);

    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    final DbWorkspace workspace = createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT);

    commitTransaction();

    freeTierBillingService.checkFreeTierBillingUsageForUsers(Sets.newHashSet(user), allBQCosts);
    verifyNoInteractions(taskQueueService); // No tasks have been added to the queue

    assertSingleWorkspaceTestDbState(user, workspace, 49.99);
  }

  @Test
  public void checkFreeTierBillingUsage_workspaceMissingCreatorNoNPE() {
    // set limit so usage is just under the 50% threshold
    workbenchConfig.billing.defaultFreeCreditsDollarLimit = 100.0;

    Map<String, Double> allBQCosts = Maps.newHashMap();
    allBQCosts.put(SINGLE_WORKSPACE_TEST_PROJECT, 49.99);

    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    final DbWorkspace workspace = createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT);
    createWorkspace(null, "rumney");

    commitTransaction();

    freeTierBillingService.checkFreeTierBillingUsageForUsers(Sets.newHashSet(user), allBQCosts);
    verifyNoInteractions(taskQueueService);

    assertSingleWorkspaceTestDbState(user, workspace, 49.99);
  }

  @Test
  public void maybeSetDollarLimitOverride_true() {
    workbenchConfig.billing.defaultFreeCreditsDollarLimit = 100.0;
    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);

    commitTransaction();

    assertThat(freeTierBillingService.maybeSetDollarLimitOverride(user, 200.0)).isTrue();
    verify(mockUserServiceAuditor)
        .fireSetFreeTierDollarLimitOverride(user.getUserId(), null, 200.0);
    assertWithinBillingTolerance(freeTierBillingService.getUserFreeTierDollarLimit(user), 200.0);

    DbUser currentUser = userDao.findUserByUserId(user.getUserId());
    assertThat(freeTierBillingService.maybeSetDollarLimitOverride(currentUser, 100.0)).isTrue();
    verify(mockUserServiceAuditor)
        .fireSetFreeTierDollarLimitOverride(user.getUserId(), 200.0, 100.0);
    assertWithinBillingTolerance(
        freeTierBillingService.getUserFreeTierDollarLimit(currentUser), 100.0);
  }

  @Test
  public void maybeSetDollarLimitOverride_false() {
    workbenchConfig.billing.defaultFreeCreditsDollarLimit = 100.0;
    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);

    assertThat(freeTierBillingService.maybeSetDollarLimitOverride(user, 100.0)).isFalse();
    verify(mockUserServiceAuditor, never())
        .fireSetFreeTierDollarLimitOverride(anyLong(), anyDouble(), anyDouble());
    assertWithinBillingTolerance(freeTierBillingService.getUserFreeTierDollarLimit(user), 100.0);

    workbenchConfig.billing.defaultFreeCreditsDollarLimit = 200.0;
    assertWithinBillingTolerance(freeTierBillingService.getUserFreeTierDollarLimit(user), 200.0);

    assertThat(freeTierBillingService.maybeSetDollarLimitOverride(user, 200.0)).isFalse();
    verify(mockUserServiceAuditor, never())
        .fireSetFreeTierDollarLimitOverride(anyLong(), anyDouble(), anyDouble());
    assertWithinBillingTolerance(freeTierBillingService.getUserFreeTierDollarLimit(user), 200.0);
  }

  @Test
  public void maybeSetDollarLimitOverride_above_usage() {
    workbenchConfig.billing.defaultFreeCreditsDollarLimit = 100.0;

    Map<String, Double> allBQCosts = Maps.newHashMap();
    allBQCosts.put(SINGLE_WORKSPACE_TEST_PROJECT, 150.0);

    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    final DbWorkspace workspace = createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT);

    assertThat(freeTierBillingService.getCachedFreeTierUsage(user)).isNull();
    assertWithinBillingTolerance(freeTierBillingService.getUserFreeTierDollarLimit(user), 100.0);

    commitTransaction();

    freeTierBillingService.checkFreeTierBillingUsageForUsers(Sets.newHashSet(user), allBQCosts);
    verify(taskQueueService)
        .pushExpiredFreeCreditsTask(
            argThat(new UserListMatcher(List.of(user.getUserId()))),
            argThat(new MapMatcher(Map.of(user.getUserId(), 0.0d))),
            argThat(new MapMatcher(Map.of(user.getUserId(), 150.0))));

    assertSingleWorkspaceTestDbState(user, workspace, 150.0);
    assertWithinBillingTolerance(freeTierBillingService.getCachedFreeTierUsage(user), 150.0);

    freeTierBillingService.maybeSetDollarLimitOverride(user, 200.0);
    verify(mockUserServiceAuditor)
        .fireSetFreeTierDollarLimitOverride(user.getUserId(), null, 200.0);
    assertWithinBillingTolerance(freeTierBillingService.getUserFreeTierDollarLimit(user), 200.0);
    assertSingleWorkspaceTestDbState(user, workspace, 150.0);

    freeTierBillingService.checkFreeTierBillingUsageForUsers(Sets.newHashSet(user), allBQCosts);
    assertSingleWorkspaceTestDbState(user, workspace, 150.0);
  }

  // do not reactivate workspaces if the new dollar limit is still below the usage

  @Test
  public void setFreeTierDollarOverride_under_usage() {
    workbenchConfig.billing.defaultFreeCreditsDollarLimit = 100.0;

    Map<String, Double> allBQCosts = Maps.newHashMap();
    allBQCosts.put(SINGLE_WORKSPACE_TEST_PROJECT, 300.0);

    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    final DbWorkspace workspace = createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT);

    commitTransaction();

    assertThat(freeTierBillingService.getCachedFreeTierUsage(user)).isNull();
    assertWithinBillingTolerance(freeTierBillingService.getUserFreeTierDollarLimit(user), 100.0);

    freeTierBillingService.checkFreeTierBillingUsageForUsers(Sets.newHashSet(user), allBQCosts);
    verify(taskQueueService)
        .pushExpiredFreeCreditsTask(
            argThat(new UserListMatcher(List.of(user.getUserId()))),
            argThat(new MapMatcher(Map.of(user.getUserId(), 0.0d))),
            argThat(new MapMatcher(Map.of(user.getUserId(), 300.0))));
    assertSingleWorkspaceTestDbState(user, workspace, 300.0);
    assertWithinBillingTolerance(freeTierBillingService.getCachedFreeTierUsage(user), 300.0);

    freeTierBillingService.maybeSetDollarLimitOverride(user, 200.0);
    verify(mockUserServiceAuditor)
        .fireSetFreeTierDollarLimitOverride(user.getUserId(), null, 200.0);
    assertWithinBillingTolerance(freeTierBillingService.getUserFreeTierDollarLimit(user), 200.0);
    assertSingleWorkspaceTestDbState(user, workspace, 300.0);

    freeTierBillingService.checkFreeTierBillingUsageForUsers(Sets.newHashSet(user), allBQCosts);
    assertSingleWorkspaceTestDbState(user, workspace, 300.0);

    verify(mockUserServiceAuditor)
        .fireSetFreeTierDollarLimitOverride(user.getUserId(), null, 200.0);
  }

  @Test
  public void checkFreeTierBillingUsage_combinedProjectsExceedsLimit() {
    final String proj1 = "proj-1";
    final String proj2 = "proj-2";
    final double cost1 = 123.45;
    final double cost2 = 234.56;
    final double sum = cost1 + cost2;

    workbenchConfig.billing.defaultFreeCreditsDollarLimit = sum - 0.01;

    Map<String, Double> allBQCosts = Maps.newHashMap();
    allBQCosts.put(proj1, cost1);
    allBQCosts.put(proj2, cost2);

    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    final DbWorkspace ws1 = createWorkspace(user, proj1);
    final DbWorkspace ws2 = createWorkspace(user, proj2);

    commitTransaction();

    freeTierBillingService.checkFreeTierBillingUsageForUsers(Sets.newHashSet(user), allBQCosts);
    verify(taskQueueService)
        .pushExpiredFreeCreditsTask(
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
  public void checkFreeTierBillingUsage_twoUsers() {
    final String proj1 = "proj-1";
    final String proj2 = "proj-2";
    final double cost1 = 123.45;
    final double cost2 = 234.56;

    workbenchConfig.billing.defaultFreeCreditsDollarLimit = Math.min(cost1, cost2) - 0.01;

    Map<String, Double> allBQCosts = Maps.newHashMap();
    allBQCosts.put(proj1, cost1);
    allBQCosts.put(proj2, cost2);

    DbUser user1 = createUser(SINGLE_WORKSPACE_TEST_USER);
    DbWorkspace ws1 = createWorkspace(user1, proj1);
    DbUser user2 = createUser("more@test.com");
    DbWorkspace ws2 = createWorkspace(user2, proj2);

    commitTransaction();

    freeTierBillingService.checkFreeTierBillingUsageForUsers(
        Sets.newHashSet(user1, user2), allBQCosts);
    verify(taskQueueService)
        .pushExpiredFreeCreditsTask(
            argThat(new UserListMatcher(List.of(user1.getUserId(), user2.getUserId()))),
            argThat(new MapMatcher(Map.of(user1.getUserId(), 0.0d, user2.getUserId(), 0.0d))),
            argThat(new MapMatcher(Map.of(user1.getUserId(), cost1, user2.getUserId(), cost2))));

    assertThat(workspaceFreeTierUsageDao.count()).isEqualTo(2);

    // confirm DB updates after checkFreeTierBillingUsage()
    final DbWorkspaceFreeTierUsage usage1 = workspaceFreeTierUsageDao.findOneByWorkspace(ws1);
    assertThat(usage1.getUser()).isEqualTo(user1);
    assertWithinBillingTolerance(usage1.getCost(), cost1);

    final DbWorkspaceFreeTierUsage usage2 = workspaceFreeTierUsageDao.findOneByWorkspace(ws2);
    assertThat(usage2.getUser()).isEqualTo(user2);
    assertWithinBillingTolerance(usage2.getCost(), cost2);
  }

  @Test
  public void checkFreeTierBillingUsage_dbUpdate() {
    workbenchConfig.billing.defaultFreeCreditsDollarLimit = 100.0;
    Map<String, Double> allBQCosts = Maps.newHashMap();
    allBQCosts.put(SINGLE_WORKSPACE_TEST_PROJECT, 100.01);

    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    final DbWorkspace workspace = createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT);

    commitTransaction();

    freeTierBillingService.checkFreeTierBillingUsageForUsers(Sets.newHashSet(user), allBQCosts);
    verify(taskQueueService)
        .pushExpiredFreeCreditsTask(
            argThat(new UserListMatcher(List.of(user.getUserId()))),
            argThat(new MapMatcher(Map.of(user.getUserId(), 0.0d))),
            argThat(new MapMatcher(Map.of(user.getUserId(), 100.01))));
    assertSingleWorkspaceTestDbState(user, workspace, 100.01);

    Timestamp t0 = workspaceFreeTierUsageDao.findAll().iterator().next().getLastUpdateTime();

    // time elapses, and this project incurs additional cost

    final double newTotalCost = 123.45;
    allBQCosts.put(SINGLE_WORKSPACE_TEST_PROJECT, newTotalCost);

    // we do not alert again, but the cost field is updated in the DB

    freeTierBillingService.checkFreeTierBillingUsageForUsers(Sets.newHashSet(user), allBQCosts);
    verify(taskQueueService, times(1))
        .pushExpiredFreeCreditsTask(
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
  public void checkFreeTierBillingUsage_singleAlertForExhaustedAndByoBilling() {
    workbenchConfig.billing.defaultFreeCreditsDollarLimit = 100.0;
    Map<String, Double> allBQCosts = Maps.newHashMap();
    allBQCosts.put(SINGLE_WORKSPACE_TEST_PROJECT, 100.01);

    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    DbWorkspace workspace = createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT);

    commitTransaction();

    freeTierBillingService.checkFreeTierBillingUsageForUsers(Sets.newHashSet(user), allBQCosts);
    verify(taskQueueService)
        .pushExpiredFreeCreditsTask(
            argThat(new UserListMatcher(List.of(user.getUserId()))),
            argThat(new MapMatcher(Map.of(user.getUserId(), 0.0d))),
            argThat(new MapMatcher(Map.of(user.getUserId(), 100.01))));

    // Simulate the user attaching their own billing account to the previously free tier workspace.
    TestTransaction.start();
    workspace = workspaceDao.findDbWorkspaceByWorkspaceId(workspace.getWorkspaceId());
    workspaceDao.save(
        workspace
            .setBillingAccountName("billingAccounts/byo-account")
            .setBillingStatus(BillingStatus.ACTIVE));

    commitTransaction();
  }

  @Test
  public void getUserFreeTierDollarLimit_default() {
    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);

    commitTransaction();

    final double initialFreeCreditsDollarLimit = 1.0;
    workbenchConfig.billing.defaultFreeCreditsDollarLimit = initialFreeCreditsDollarLimit;
    assertWithinBillingTolerance(
        freeTierBillingService.getUserFreeTierDollarLimit(user), initialFreeCreditsDollarLimit);

    final double fractionalFreeCreditsDollarLimit = 123.456;
    workbenchConfig.billing.defaultFreeCreditsDollarLimit = fractionalFreeCreditsDollarLimit;
    assertWithinBillingTolerance(
        freeTierBillingService.getUserFreeTierDollarLimit(user), fractionalFreeCreditsDollarLimit);
  }

  @Test
  public void getUserFreeTierDollarLimit_override() {
    workbenchConfig.billing.defaultFreeCreditsDollarLimit = 123.456;

    DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);

    commitTransaction();

    final double limit1 = 100.0;
    freeTierBillingService.maybeSetDollarLimitOverride(user, limit1);
    verify(mockUserServiceAuditor)
        .fireSetFreeTierDollarLimitOverride(user.getUserId(), null, limit1);
    assertWithinBillingTolerance(freeTierBillingService.getUserFreeTierDollarLimit(user), limit1);

    final double limit2 = 200.0;

    user = userDao.findUserByUserId(user.getUserId());

    freeTierBillingService.maybeSetDollarLimitOverride(user, limit2);
    verify(mockUserServiceAuditor)
        .fireSetFreeTierDollarLimitOverride(user.getUserId(), limit1, limit2);
    assertWithinBillingTolerance(freeTierBillingService.getUserFreeTierDollarLimit(user), limit2);
  }

  @Test
  public void getUserCachedFreeTierUsage() {
    workbenchConfig.billing.defaultFreeCreditsDollarLimit = 100.0;

    Map<String, Double> allBQCosts = Maps.newHashMap();
    allBQCosts.put(SINGLE_WORKSPACE_TEST_PROJECT, 100.01);

    final DbUser user1 = createUser(SINGLE_WORKSPACE_TEST_USER);
    createWorkspace(user1, SINGLE_WORKSPACE_TEST_PROJECT);
    final double user2Costs = 999.0;

    final DbUser user2 = createUser("another user");
    createWorkspace(user2, "project 3");

    commitTransaction();

    // we have not yet had a chance to cache this usage
    assertThat(freeTierBillingService.getCachedFreeTierUsage(user1)).isNull();
    assertThat(freeTierBillingService.userHasRemainingFreeTierCredits(user1)).isTrue();

    freeTierBillingService.checkFreeTierBillingUsageForUsers(Sets.newHashSet(user1), allBQCosts);

    assertWithinBillingTolerance(freeTierBillingService.getCachedFreeTierUsage(user1), 100.01);
    assertThat(freeTierBillingService.userHasRemainingFreeTierCredits(user1)).isFalse();

    TestTransaction.start();
    createWorkspace(user1, "another project");
    commitTransaction();

    final Map<String, Double> costs =
        ImmutableMap.of(SINGLE_WORKSPACE_TEST_PROJECT, 1000.0, "another project", 200.0);

    // we have not yet cached the new workspace costs
    assertWithinBillingTolerance(freeTierBillingService.getCachedFreeTierUsage(user1), 100.01);

    freeTierBillingService.checkFreeTierBillingUsageForUsers(Sets.newHashSet(user1), costs);
    final double expectedTotalCachedFreeTierUsage = 1000.0 + 200.0;
    assertWithinBillingTolerance(
        freeTierBillingService.getCachedFreeTierUsage(user1), expectedTotalCachedFreeTierUsage);

    freeTierBillingService.checkFreeTierBillingUsageForUsers(
        Sets.newHashSet(user1, user2), ImmutableMap.of("project 3", user2Costs));

    assertWithinBillingTolerance(
        freeTierBillingService.getCachedFreeTierUsage(user1), expectedTotalCachedFreeTierUsage);
    assertWithinBillingTolerance(freeTierBillingService.getCachedFreeTierUsage(user2), user2Costs);
    assertThat(freeTierBillingService.userHasRemainingFreeTierCredits(user2)).isFalse();
  }

  @Test
  public void userHasRemainingFreeTierCredits_newUser() {
    workbenchConfig.billing.defaultFreeCreditsDollarLimit = 100.0;
    final DbUser user1 = createUser(SINGLE_WORKSPACE_TEST_USER);
    assertThat(freeTierBillingService.userHasRemainingFreeTierCredits(user1)).isTrue();
  }

  @Test
  public void userHasRemainingFreeTierCredits() {
    workbenchConfig.billing.defaultFreeCreditsDollarLimit = 100.0;

    final DbUser user1 = createUser(SINGLE_WORKSPACE_TEST_USER);
    createWorkspace(user1, SINGLE_WORKSPACE_TEST_PROJECT);

    commitTransaction();

    // 99.99 < 100.0
    Map<String, Double> allBQCosts = ImmutableMap.of(SINGLE_WORKSPACE_TEST_PROJECT, 99.99);

    freeTierBillingService.checkFreeTierBillingUsageForUsers(Sets.newHashSet(user1), allBQCosts);
    assertThat(freeTierBillingService.userHasRemainingFreeTierCredits(user1)).isTrue();

    // 100.01 > 100.0
    allBQCosts = ImmutableMap.of(SINGLE_WORKSPACE_TEST_PROJECT, 100.01);
    freeTierBillingService.checkFreeTierBillingUsageForUsers(Sets.newHashSet(user1), allBQCosts);
    assertThat(freeTierBillingService.userHasRemainingFreeTierCredits(user1)).isFalse();

    // 100.01 < 200.0
    workbenchConfig.billing.defaultFreeCreditsDollarLimit = 200.0;
    assertThat(freeTierBillingService.userHasRemainingFreeTierCredits(user1)).isTrue();
  }

  @Test
  public void test_disableOnlyFreeTierWorkspaces() {
    workbenchConfig.billing.defaultFreeCreditsDollarLimit = 100.0;

    Map<String, Double> allBQCosts = ImmutableMap.of(SINGLE_WORKSPACE_TEST_PROJECT, 100.01);

    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    final DbWorkspace freeTierWorkspace = createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT);
    final DbWorkspace userAccountWorkspace =
        new DbWorkspace()
            .setCreator(user)
            .setWorkspaceNamespace("some other namespace")
            .setGoogleProject("other project")
            .setBillingAccountName("some other account")
            .setBillingStatus(BillingStatus.ACTIVE);
    workspaceDao.save(userAccountWorkspace);

    commitTransaction();

    freeTierBillingService.checkFreeTierBillingUsageForUsers(Sets.newHashSet(user), allBQCosts);
    verify(taskQueueService)
        .pushExpiredFreeCreditsTask(
            argThat(new UserListMatcher(List.of(user.getUserId()))),
            argThat(new MapMatcher(Map.of(user.getUserId(), 0.0d))),
            argThat(new MapMatcher(Map.of(user.getUserId(), 100.01d))));

    assertSingleWorkspaceTestDbState(user, freeTierWorkspace, 100.01);

    final DbWorkspace retrievedWorkspace =
        workspaceDao.findById(userAccountWorkspace.getWorkspaceId()).get();
    assertThat(retrievedWorkspace.getBillingStatus()).isEqualTo(BillingStatus.ACTIVE);
  }

  @Test
  public void test_deletedWorkspaceUsageIsConsidered_whenChargeIsPostedAfterCron() {
    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    DbWorkspace workspace = createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT);

    commitTransaction();

    workbenchConfig.billing.defaultFreeCreditsDollarLimit = 100.0;
    Map<String, Double> allBQCosts = ImmutableMap.of(SINGLE_WORKSPACE_TEST_PROJECT, 50.0);

    TestTransaction.start();
    workspace.setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.DELETED);
    workspace.setLastModifiedTime(Timestamp.valueOf(LocalDateTime.now()));
    workspaceDao.save(workspace);
    commitTransaction();

    freeTierBillingService.checkFreeTierBillingUsageForUsers(Sets.newHashSet(user), allBQCosts);

    allBQCosts = ImmutableMap.of(SINGLE_WORKSPACE_TEST_PROJECT, 100.1);

    freeTierBillingService.checkFreeTierBillingUsageForUsers(Sets.newHashSet(user), allBQCosts);
    verify(taskQueueService)
        .pushExpiredFreeCreditsTask(
            argThat(new UserListMatcher(List.of(user.getUserId()))),
            argThat(new MapMatcher(Map.of(user.getUserId(), 50.0d))),
            argThat(new MapMatcher(Map.of(user.getUserId(), 100.1))));
  }

  @Test
  public void test_deletedWorkspaceUsageIsConsidered_whenAnotherWorkspaceExceedsLimitAfterCron() {
    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    DbWorkspace workspace = createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT);

    commitTransaction();

    workbenchConfig.billing.defaultFreeCreditsDollarLimit = 100.0;
    Map<String, Double> allBQCosts = new HashMap<>();
    allBQCosts.put(SINGLE_WORKSPACE_TEST_PROJECT, 50.0);

    TestTransaction.start();
    workspace.setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.DELETED);
    workspace.setLastModifiedTime(Timestamp.valueOf(LocalDateTime.now()));
    workspaceDao.save(workspace);
    commitTransaction();

    freeTierBillingService.checkFreeTierBillingUsageForUsers(Sets.newHashSet(user), allBQCosts);

    TestTransaction.start();
    DbWorkspace anotherWorkspace = createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT + "4");
    commitTransaction();

    assertSingleWorkspaceTestDbState(user, workspace, 50);

    allBQCosts.put(SINGLE_WORKSPACE_TEST_PROJECT + "4", 100.1);

    freeTierBillingService.checkFreeTierBillingUsageForUsers(Sets.newHashSet(user), allBQCosts);
    verify(taskQueueService)
        .pushExpiredFreeCreditsTask(
            argThat(new UserListMatcher(List.of(user.getUserId()))),
            argThat(new MapMatcher(Map.of(user.getUserId(), 50.0d))),
            argThat(new MapMatcher(Map.of(user.getUserId(), 150.1d))));

    assertSingleWorkspaceTestDbState(user, workspace, 50);
    assertSingleWorkspaceTestDbState(user, anotherWorkspace, 100.1);
  }

  private TableResult mockBQTableResult(final Map<String, Double> costMap) {
    Field idField = Field.of("id", LegacySQLTypeName.STRING);
    Field costField = Field.of("cost", LegacySQLTypeName.FLOAT);
    Schema s = Schema.of(idField, costField);

    List<FieldValueList> tableRows =
        costMap.entrySet().stream()
            .map(
                e -> {
                  FieldValue id = FieldValue.of(Attribute.PRIMITIVE, e.getKey());
                  FieldValue cost = FieldValue.of(Attribute.PRIMITIVE, e.getValue().toString());
                  return FieldValueList.of(Arrays.asList(id, cost));
                })
            .collect(Collectors.toList());

    return new TableResult(s, tableRows.size(), new PageImpl<>(() -> null, null, tableRows));
  }

  private TableResult mockBQTableSingleResult(final String project, final Double cost) {
    return mockBQTableResult(ImmutableMap.of(project, cost));
  }

  private TableResult mockBQTableSingleResult(final Double cost) {
    return mockBQTableSingleResult(SINGLE_WORKSPACE_TEST_PROJECT, cost);
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
            .setBillingAccountName(workbenchConfig.billing.freeTierBillingAccountName()));
  }

  private void assertWithinBillingTolerance(double actualValue, double expectedValue) {
    final double tolerance = DEFAULT_PERCENTAGE_TOLERANCE * expectedValue * 0.01;
    assertThat(actualValue).isWithin(tolerance).of(expectedValue);
  }

  private void commitTransaction() {
    TestTransaction.flagForCommit();
    TestTransaction.end();
  }
}
