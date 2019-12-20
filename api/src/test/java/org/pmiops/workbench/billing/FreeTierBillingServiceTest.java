package org.pmiops.workbench.billing;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import com.google.cloud.PageImpl;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.FieldValue.Attribute;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.LegacySQLTypeName;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.TableResult;
import com.google.common.collect.ImmutableMap;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.dao.WorkspaceFreeTierUsageDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.db.model.DbWorkspace.BillingMigrationStatus;
import org.pmiops.workbench.db.model.DbWorkspaceFreeTierUsage;
import org.pmiops.workbench.model.BillingAccountType;
import org.pmiops.workbench.model.BillingStatus;
import org.pmiops.workbench.model.WorkspaceActiveStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
public class FreeTierBillingServiceTest {

  private static final double DEFAULT_PERCENTAGE_TOLERANCE = 0.001;

  @Autowired BigQueryService bigQueryService;
  @Autowired FreeTierBillingService freeTierBillingService;
  @Autowired NotificationService notificationService;

  @Autowired UserDao userDao;
  @Autowired WorkspaceDao workspaceDao;
  @Autowired WorkspaceFreeTierUsageDao workspaceFreeTierUsageDao;

  private static WorkbenchConfig workbenchConfig;

  private static final String SINGLE_WORKSPACE_TEST_USER = "test@test.com";
  private static final String SINGLE_WORKSPACE_TEST_PROJECT = "aou-test-123";

  @TestConfiguration
  @Import({FreeTierBillingService.class})
  @MockBean({BigQueryService.class, NotificationService.class})
  static class Configuration {
    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public WorkbenchConfig workbenchConfig() {
      return workbenchConfig;
    }
  }

  @Before
  public void setUp() {
    workbenchConfig = WorkbenchConfig.createEmptyConfig();
  }

  @Test
  public void checkFreeTierBillingUsage_exceedsDollarThresholds() {
    final double limit = 100.0;
    final double costUnderThreshold = 49.5;

    double threshold = 0.5;
    double costOverThreshold = 50.5;
    double remaining = limit - costOverThreshold;

    workbenchConfig.billing.defaultFreeCreditsDollarLimit = limit;

    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT);

    // check that we have not alerted before the threshold

    doReturn(mockBQTableSingleResult(costUnderThreshold)).when(bigQueryService).executeQuery(any());
    freeTierBillingService.checkFreeTierBillingUsage();
    verifyZeroInteractions(notificationService);

    // check that we alert for the 50% threshold

    doReturn(mockBQTableSingleResult(costOverThreshold)).when(bigQueryService).executeQuery(any());
    freeTierBillingService.checkFreeTierBillingUsage();
    verify(notificationService)
        .alertUserFreeTierDollarThreshold(
            eq(user), eq(threshold), eq(costOverThreshold), eq(remaining));

    // check that we do not alert twice for the 50% threshold

    doReturn(mockBQTableSingleResult(costOverThreshold)).when(bigQueryService).executeQuery(any());
    freeTierBillingService.checkFreeTierBillingUsage();
    verifyZeroInteractions(notificationService);

    // check that we alert for the 75% threshold

    threshold = 0.75;
    costOverThreshold = 75.3;
    remaining = limit - costOverThreshold;

    doReturn(mockBQTableSingleResult(costOverThreshold)).when(bigQueryService).executeQuery(any());
    freeTierBillingService.checkFreeTierBillingUsage();
    verify(notificationService)
        .alertUserFreeTierDollarThreshold(
            eq(user), eq(threshold), eq(costOverThreshold), eq(remaining));

    // check that we do not alert twice for the 75% threshold

    doReturn(mockBQTableSingleResult(costOverThreshold)).when(bigQueryService).executeQuery(any());
    freeTierBillingService.checkFreeTierBillingUsage();
    verifyZeroInteractions(notificationService);

    // check that we alert for expiration when we hit 100%

    final double costToTriggerExpiration = 100.01;

    doReturn(mockBQTableSingleResult(costToTriggerExpiration))
        .when(bigQueryService)
        .executeQuery(any());
    freeTierBillingService.checkFreeTierBillingUsage();
    verify(notificationService).alertUserFreeTierExpiration(eq(user));

    // check that we do not alert twice for 100%

    doReturn(mockBQTableSingleResult(costToTriggerExpiration))
        .when(bigQueryService)
        .executeQuery(any());
    freeTierBillingService.checkFreeTierBillingUsage();
    verifyZeroInteractions(notificationService);
  }

  @Test
  public void checkFreeTierBillingUsage_doesNotExceedDayThresholds() {

    // set cost values to ensure we don't alert from cost

    final double dollarLimit = 100.0;
    final double spent = 0.0;
    workbenchConfig.billing.defaultFreeCreditsDollarLimit = dollarLimit;
    doReturn(mockBQTableSingleResult(spent)).when(bigQueryService).executeQuery(any());

    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT);

    final short limit = 1000;
    workbenchConfig.billing.defaultFreeCreditsDaysLimit = limit;

    final Instant testStartTime = Instant.now();

    // still have over half my free credit time remaining
    final Instant registrationTime =
        testStartTime.minus(Period.ofDays(limit / 2)).plus(Period.ofDays(1));
    user.setFirstRegistrationCompletionTime(Timestamp.from(registrationTime));
    userDao.save(user);

    // check that we do not alert

    freeTierBillingService.checkFreeTierBillingUsage();
    verifyZeroInteractions(notificationService);
  }

  // TODO: can we test multiple time thresholds in the same test?

  @Test
  public void checkFreeTierBillingUsage_exceeds50PctDayThreshold() {

    // set cost values to ensure we don't alert from cost

    final double dollarLimit = 100.0;
    final double spent = 0.0;
    final double dollarBalance = dollarLimit - spent;
    workbenchConfig.billing.defaultFreeCreditsDollarLimit = dollarLimit;
    doReturn(mockBQTableSingleResult(spent)).when(bigQueryService).executeQuery(any());

    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT);

    final short limit = 1000;
    workbenchConfig.billing.defaultFreeCreditsDaysLimit = limit;

    final Instant testStartTime = Instant.now();

    // check that we alert for the 50% threshold

    // set my registration time to "more than half of my free credits ago"
    final Instant registrationTime =
        testStartTime
            .minus(Period.ofDays(limit / 2))
            .minus(Period.ofDays(1))
            // avoid silly test errors due to comparing too close to day boundaries
            .minus(Duration.ofMinutes(1));
    user.setFirstRegistrationCompletionTime(Timestamp.from(registrationTime));
    userDao.save(user);

    final Instant expirationTime = registrationTime.plus(Period.ofDays(limit));
    final long daysRemaining = Duration.between(testStartTime, expirationTime).toDays();
    final LocalDate expirationDate = expirationTime.atZone(ZoneId.systemDefault()).toLocalDate();

    freeTierBillingService.checkFreeTierBillingUsage();
    verify(notificationService)
        .alertUserFreeTierTimeThreshold(
            eq(user), eq(daysRemaining), eq(expirationDate), eq(dollarBalance));

    // check that we do not alert twice for the 50% threshold

    freeTierBillingService.checkFreeTierBillingUsage();
    verifyZeroInteractions(notificationService);
  }

  @Test
  public void checkFreeTierBillingUsage_exceeds75PctDayThreshold() {

    // set cost values to ensure we don't alert from cost

    final double dollarLimit = 100.0;
    final double spent = 0.0;
    final double dollarBalance = dollarLimit - spent;
    workbenchConfig.billing.defaultFreeCreditsDollarLimit = dollarLimit;
    doReturn(mockBQTableSingleResult(spent)).when(bigQueryService).executeQuery(any());

    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT);

    final short limit = 1000;
    workbenchConfig.billing.defaultFreeCreditsDaysLimit = limit;

    final Instant testStartTime = Instant.now();

    // check that we alert for the 75% threshold

    // set my registration time to "more than 3/4 of my free credits ago"
    final Instant registrationTime =
        testStartTime
            .minus(Period.ofDays(limit * 3 / 4))
            .minus(Period.ofDays(1))
            // avoid silly test errors due to comparing too close to day boundaries
            .minus(Duration.ofMinutes(1));
    user.setFirstRegistrationCompletionTime(Timestamp.from(registrationTime));
    userDao.save(user);

    final Instant expirationTime = registrationTime.plus(Period.ofDays(limit));
    long daysRemaining = Duration.between(testStartTime, expirationTime).toDays();
    final LocalDate expirationDate = expirationTime.atZone(ZoneId.systemDefault()).toLocalDate();

    freeTierBillingService.checkFreeTierBillingUsage();
    verify(notificationService)
        .alertUserFreeTierTimeThreshold(
            eq(user), eq(daysRemaining), eq(expirationDate), eq(dollarBalance));

    // check that we do not alert twice for the 75% threshold

    freeTierBillingService.checkFreeTierBillingUsage();
    verifyZeroInteractions(notificationService);
  }

  @Test
  public void checkFreeTierBillingUsage_exceeds100PctDayThreshold() {

    // set cost values to ensure we don't alert from cost

    final double dollarLimit = 100.0;
    final double spent = 0.0;
    workbenchConfig.billing.defaultFreeCreditsDollarLimit = dollarLimit;
    doReturn(mockBQTableSingleResult(spent)).when(bigQueryService).executeQuery(any());

    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    final DbWorkspace workspace = createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT);

    workbenchConfig.billing.defaultFreeCreditsDaysLimit = 30;
    final Instant tooLongAgo = Instant.now().minus(Period.ofDays(40));
    user.setFirstRegistrationCompletionTime(Timestamp.from(tooLongAgo));
    userDao.save(user);

    freeTierBillingService.checkFreeTierBillingUsage();
    verify(notificationService).alertUserFreeTierExpiration(eq(user));

    assertSingleWorkspaceTestDbState(user, workspace, BillingStatus.INACTIVE, spent);

    // check that we do not alert twice

    freeTierBillingService.checkFreeTierBillingUsage();
    verifyZeroInteractions(notificationService);
  }

  @Test
  public void checkFreeTierBillingUsage_exceedsBothLimitsConcurrently() {
    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    final DbWorkspace workspace = createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT);

    // expire due to cost

    workbenchConfig.billing.defaultFreeCreditsDollarLimit = 100.0;
    doReturn(mockBQTableSingleResult(100.01)).when(bigQueryService).executeQuery(any());

    // expire due to time

    workbenchConfig.billing.defaultFreeCreditsDaysLimit = 10;
    final Instant tooLongAgo = Instant.now().minus(Period.ofDays(11));
    user.setFirstRegistrationCompletionTime(Timestamp.from(tooLongAgo));
    userDao.save(user);

    // check that we do not alert twice

    freeTierBillingService.checkFreeTierBillingUsage();
    verify(notificationService, times(1)).alertUserFreeTierExpiration(eq(user));
    assertSingleWorkspaceTestDbState(user, workspace, BillingStatus.INACTIVE, 100.01);
  }

  @Test
  public void checkFreeTierBillingUsage_exceedsBoth50PctThresholds() {
    final double dollarLimit = 100.0;
    final double spent = 50.1;
    final double dollarBalance = dollarLimit - spent;
    workbenchConfig.billing.defaultFreeCreditsDollarLimit = dollarLimit;
    doReturn(mockBQTableSingleResult(spent)).when(bigQueryService).executeQuery(any());

    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT);

    final short daysLimit = 1000;
    workbenchConfig.billing.defaultFreeCreditsDaysLimit = daysLimit;

    final Instant testStartTime = Instant.now();

    // check that we alert for both 50% thresholds

    // set my registration time to "more than half of my free credits ago"
    final Instant registrationTime =
        testStartTime
            .minus(Period.ofDays(daysLimit / 2))
            .minus(Period.ofDays(1))
            // avoid silly test errors due to comparing too close to day boundaries
            .minus(Duration.ofMinutes(1));
    user.setFirstRegistrationCompletionTime(Timestamp.from(registrationTime));
    userDao.save(user);

    final Instant expirationTime = registrationTime.plus(Period.ofDays(daysLimit));
    final long daysRemaining = Duration.between(testStartTime, expirationTime).toDays();
    final LocalDate expirationDate = expirationTime.atZone(ZoneId.systemDefault()).toLocalDate();

    freeTierBillingService.checkFreeTierBillingUsage();
    verify(notificationService)
        .alertUserFreeTierDollarThreshold(
            eq(user), eq(0.5), eq(spent), eq(dollarBalance));
    verify(notificationService)
        .alertUserFreeTierTimeThreshold(
            eq(user), eq(daysRemaining), eq(expirationDate), eq(dollarBalance));

    // check that we do not alert twice for the 50% threshold

    freeTierBillingService.checkFreeTierBillingUsage();
    verifyZeroInteractions(notificationService);
  }

  @Test
  public void checkFreeTierBillingUsage_exceedsBothLimitsCostFirst() {
    DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    DbWorkspace workspace = createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT);

    // expire due to cost

    workbenchConfig.billing.defaultFreeCreditsDollarLimit = 100.0;
    doReturn(mockBQTableSingleResult(100.01)).when(bigQueryService).executeQuery(any());

    freeTierBillingService.checkFreeTierBillingUsage();
    verify(notificationService).alertUserFreeTierExpiration(eq(user));
    assertSingleWorkspaceTestDbState(user, workspace, BillingStatus.INACTIVE, 100.01);

    // expire due to time

    workbenchConfig.billing.defaultFreeCreditsDaysLimit = 10;
    final Instant tooLongAgo = Instant.now().minus(Period.ofDays(11));
    user.setFirstRegistrationCompletionTime(Timestamp.from(tooLongAgo));
    userDao.save(user);

    // check that we do not alert twice

    freeTierBillingService.checkFreeTierBillingUsage();
    verifyZeroInteractions(notificationService);
  }

  @Test
  public void checkFreeTierBillingUsage_exceedsBothLimitsTimeFirst() {
    DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    DbWorkspace workspace = createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT);
    workbenchConfig.billing.defaultFreeCreditsDollarLimit = 100.0;
    doReturn(mockBQTableSingleResult(0.0)).when(bigQueryService).executeQuery(any());

    // expire due to time

    workbenchConfig.billing.defaultFreeCreditsDaysLimit = 10;
    final Instant tooLongAgo = Instant.now().minus(Period.ofDays(11));
    user.setFirstRegistrationCompletionTime(Timestamp.from(tooLongAgo));
    userDao.save(user);

    freeTierBillingService.checkFreeTierBillingUsage();
    verify(notificationService).alertUserFreeTierExpiration(eq(user));
    assertSingleWorkspaceTestDbState(user, workspace, BillingStatus.INACTIVE, 0.0);

    // expire due to cost

    doReturn(mockBQTableSingleResult(100.01)).when(bigQueryService).executeQuery(any());

    // check that we do not alert twice

    freeTierBillingService.checkFreeTierBillingUsage();
    verifyZeroInteractions(notificationService);
  }

  @Test
  public void checkFreeTierBillingUsage_timeExpirationAndCostThreshold() {

    // test the behavior when the user exceeds the free credit time limit
    // and also crosses the 50% cost threshold

    DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    DbWorkspace workspace = createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT);
    workbenchConfig.billing.defaultFreeCreditsDollarLimit = 100.0;
    doReturn(mockBQTableSingleResult(50.1)).when(bigQueryService).executeQuery(any());

    workbenchConfig.billing.defaultFreeCreditsDaysLimit = 10;
    final Instant tooLongAgo = Instant.now().minus(Period.ofDays(11));
    user.setFirstRegistrationCompletionTime(Timestamp.from(tooLongAgo));
    userDao.save(user);

    // we expect to see ONE alert due to time expiration
    // and NO alert for crossing the 50% cost threshold

    freeTierBillingService.checkFreeTierBillingUsage();
    verify(notificationService, times(1)).alertUserFreeTierExpiration(eq(user));
    verify(notificationService, times(0)).alertUserFreeTierDollarThreshold(
        eq(user), eq(0.5), eq(50.1), eq(49.9));

    assertSingleWorkspaceTestDbState(user, workspace, BillingStatus.INACTIVE, 50.1);
  }


  @Test
  public void checkFreeTierBillingUsage_costExpirationAndTimeThreshold() {

    // test the behavior when the user exceeds the free credit cost
    // and also crosses the 50% time threshold

    DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    DbWorkspace workspace = createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT);
    workbenchConfig.billing.defaultFreeCreditsDollarLimit = 100.0;
    doReturn(mockBQTableSingleResult(100.1)).when(bigQueryService).executeQuery(any());

    final Instant testStartTime = Instant.now();

    workbenchConfig.billing.defaultFreeCreditsDaysLimit = 10;
    final Instant registrationTime = testStartTime.minus(Period.ofDays(6));
    user.setFirstRegistrationCompletionTime(Timestamp.from(registrationTime));
    userDao.save(user);

    // we expect to see ONE alert due to cost expiration
    // and NO alert for crossing the 50% time threshold

    final Instant expirationTime = registrationTime.plus(Period.ofDays(10));
    final long daysRemaining = Duration.between(testStartTime, expirationTime).toDays();
    final LocalDate expirationDate = expirationTime.atZone(ZoneId.systemDefault()).toLocalDate();

    freeTierBillingService.checkFreeTierBillingUsage();
    verify(notificationService, times(1)).alertUserFreeTierExpiration(eq(user));
    verify(notificationService, times(0)).alertUserFreeTierTimeThreshold(
        eq(user), eq(daysRemaining), eq(expirationDate), eq(-0.01));

    assertSingleWorkspaceTestDbState(user, workspace, BillingStatus.INACTIVE, 100.1);
  }

  @Test
  public void checkFreeTierBillingUsage_disabledUserNotIgnored() {
    workbenchConfig.billing.defaultFreeCreditsDollarLimit = 100.0;
    doReturn(mockBQTableSingleResult(100.01)).when(bigQueryService).executeQuery(any());

    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    user.setDisabled(true);
    userDao.save(user);
    final DbWorkspace workspace = createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT);

    freeTierBillingService.checkFreeTierBillingUsage();
    verify(notificationService).alertUserFreeTierExpiration(eq(user));

    assertSingleWorkspaceTestDbState(user, workspace, BillingStatus.INACTIVE, 100.01);
  }

  @Test
  public void checkFreeTierBillingUsage_deletedWorkspaceNotIgnored() {
    workbenchConfig.billing.defaultFreeCreditsDollarLimit = 100.0;
    doReturn(mockBQTableSingleResult(100.01)).when(bigQueryService).executeQuery(any());

    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    final DbWorkspace workspace = createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT);
    workspace.setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.DELETED);
    workspaceDao.save(workspace);

    freeTierBillingService.checkFreeTierBillingUsage();
    verify(notificationService).alertUserFreeTierExpiration(eq(user));

    assertSingleWorkspaceTestDbState(user, workspace, BillingStatus.INACTIVE, 100.01);
  }

  @Test
  public void checkFreeTierBillingUsage_noAlert() {
    // set limit so usage is just under the 50% threshold
    workbenchConfig.billing.defaultFreeCreditsDollarLimit = 100.0;
    doReturn(mockBQTableSingleResult(49.99)).when(bigQueryService).executeQuery(any());

    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    final DbWorkspace workspace = createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT);

    freeTierBillingService.checkFreeTierBillingUsage();
    verifyZeroInteractions(notificationService);

    assertSingleWorkspaceTestDbState(user, workspace, BillingStatus.ACTIVE, 49.99);
  }

  @Test
  public void checkFreeTierBillingUsage_workspaceMissingCreatorNoNPE() {
    // set limit so usage is just under the 50% threshold
    workbenchConfig.billing.defaultFreeCreditsDollarLimit = 100.0;
    doReturn(mockBQTableSingleResult(49.99)).when(bigQueryService).executeQuery(any());

    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    final DbWorkspace workspace = createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT);
    createWorkspace(null, "rumney");

    freeTierBillingService.checkFreeTierBillingUsage();
    verifyZeroInteractions(notificationService);

    assertSingleWorkspaceTestDbState(user, workspace, BillingStatus.ACTIVE, 49.99);
  }

  @Test
  public void checkFreeTierBillingUsage_override() {
    workbenchConfig.billing.defaultFreeCreditsDollarLimit = 100.0;
    doReturn(mockBQTableSingleResult(100.01)).when(bigQueryService).executeQuery(any());

    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    final DbWorkspace workspace = createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT);

    freeTierBillingService.checkFreeTierBillingUsage();
    verify(notificationService).alertUserFreeTierExpiration(eq(user));

    assertSingleWorkspaceTestDbState(user, workspace, BillingStatus.INACTIVE, 100.01);

    user.setFreeTierCreditsLimitDollarsOverride(200.0);
    userDao.save(user);

    freeTierBillingService.checkFreeTierBillingUsage();
    verifyZeroInteractions(notificationService);

    // we do not reset the workspace's state to ACTIVE
    // that will be done by the override endpoint (TODO)
    assertSingleWorkspaceTestDbState(user, workspace, BillingStatus.INACTIVE, 100.01);
  }

  @Test
  public void checkFreeTierBillingUsage_combinedProjectsExceedsLimit() {
    final String ns1 = "namespace-1";
    final String ns2 = "namespace-2";
    final double cost1 = 123.45;
    final double cost2 = 234.56;
    final double sum = cost1 + cost2;

    workbenchConfig.billing.defaultFreeCreditsDollarLimit = sum - 0.01;

    doReturn(mockBQTableResult(ImmutableMap.of(ns1, cost1, ns2, cost2)))
        .when(bigQueryService)
        .executeQuery(any());

    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    final DbWorkspace ws1 = createWorkspace(user, ns1);
    final DbWorkspace ws2 = createWorkspace(user, ns2);

    freeTierBillingService.checkFreeTierBillingUsage();
    verify(notificationService).alertUserFreeTierExpiration(eq(user));
    assertThat(workspaceFreeTierUsageDao.count()).isEqualTo(2);

    // confirm DB updates after checkFreeTierBillingUsage()

    for (DbWorkspace ws : Arrays.asList(ws1, ws2)) {
      // retrieve from DB again to reflect update after cron
      DbWorkspace dbWorkspace = workspaceDao.findOne(ws.getWorkspaceId());
      assertThat(dbWorkspace.getBillingStatus()).isEqualTo(BillingStatus.INACTIVE);
      assertThat(dbWorkspace.getBillingAccountType()).isEqualTo(BillingAccountType.FREE_TIER);
    }

    final DbWorkspaceFreeTierUsage usage1 = workspaceFreeTierUsageDao.findOneByWorkspace(ws1);
    assertThat(usage1.getUser()).isEqualTo(user);
    assertWithinBillingTolerance(usage1.getCost(), cost1);

    final DbWorkspaceFreeTierUsage usage2 = workspaceFreeTierUsageDao.findOneByWorkspace(ws2);
    assertThat(usage2.getUser()).isEqualTo(user);
    assertWithinBillingTolerance(usage2.getCost(), cost2);
  }

  @Test
  public void checkFreeTierBillingUsage_twoUsers() {
    final String ns1 = "namespace-1";
    final String ns2 = "namespace-2";
    final double cost1 = 123.45;
    final double cost2 = 234.56;

    workbenchConfig.billing.defaultFreeCreditsDollarLimit = Math.min(cost1, cost2) - 0.01;

    doReturn(mockBQTableResult(ImmutableMap.of(ns1, cost1, ns2, cost2)))
        .when(bigQueryService)
        .executeQuery(any());

    DbUser user1 = createUser(SINGLE_WORKSPACE_TEST_USER);
    DbWorkspace ws1 = createWorkspace(user1, ns1);
    DbUser user2 = createUser("more@test.com");
    DbWorkspace ws2 = createWorkspace(user2, ns2);
    freeTierBillingService.checkFreeTierBillingUsage();
    verify(notificationService).alertUserFreeTierExpiration(eq(user1));
    verify(notificationService).alertUserFreeTierExpiration(eq(user2));

    assertThat(workspaceFreeTierUsageDao.count()).isEqualTo(2);

    // confirm DB updates after checkFreeTierBillingUsage()

    final DbWorkspace dbWorkspace1 = workspaceDao.findOne(ws1.getWorkspaceId());
    assertThat(dbWorkspace1.getBillingStatus()).isEqualTo(BillingStatus.INACTIVE);
    assertThat(dbWorkspace1.getBillingAccountType()).isEqualTo(BillingAccountType.FREE_TIER);

    final DbWorkspaceFreeTierUsage usage1 = workspaceFreeTierUsageDao.findOneByWorkspace(ws1);
    assertThat(usage1.getUser()).isEqualTo(user1);
    assertWithinBillingTolerance(usage1.getCost(), cost1);

    final DbWorkspace dbWorkspace2 = workspaceDao.findOne(ws2.getWorkspaceId());
    assertThat(dbWorkspace2.getBillingStatus()).isEqualTo(BillingStatus.INACTIVE);
    assertThat(dbWorkspace2.getBillingAccountType()).isEqualTo(BillingAccountType.FREE_TIER);

    final DbWorkspaceFreeTierUsage usage2 = workspaceFreeTierUsageDao.findOneByWorkspace(ws2);
    assertThat(usage2.getUser()).isEqualTo(user2);
    assertWithinBillingTolerance(usage2.getCost(), cost2);
  }

  @Test
  public void checkFreeTierBillingUsage_ignoreOLDMigrationStatus() {
    workbenchConfig.billing.defaultFreeCreditsDollarLimit = 100.0;
    doReturn(mockBQTableSingleResult(100.01)).when(bigQueryService).executeQuery(any());

    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT, BillingMigrationStatus.OLD);
    freeTierBillingService.checkFreeTierBillingUsage();

    verifyZeroInteractions(notificationService);
    assertThat(workspaceFreeTierUsageDao.count()).isEqualTo(0);
  }

  @Test
  public void checkFreeTierBillingUsage_ignoreMIGRATEDMigrationStatus() {
    workbenchConfig.billing.defaultFreeCreditsDollarLimit = 100.0;
    doReturn(mockBQTableSingleResult(100.01)).when(bigQueryService).executeQuery(any());

    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT, BillingMigrationStatus.MIGRATED);
    freeTierBillingService.checkFreeTierBillingUsage();

    verifyZeroInteractions(notificationService);
    assertThat(workspaceFreeTierUsageDao.count()).isEqualTo(0);
  }

  @Test
  public void checkFreeTierBillingUsage_dbUpdate() {
    workbenchConfig.billing.defaultFreeCreditsDollarLimit = 100.0;
    doReturn(mockBQTableSingleResult(100.01)).when(bigQueryService).executeQuery(any());

    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    final DbWorkspace workspace = createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT);

    freeTierBillingService.checkFreeTierBillingUsage();
    verify(notificationService).alertUserFreeTierExpiration(eq(user));
    assertSingleWorkspaceTestDbState(user, workspace, BillingStatus.INACTIVE, 100.01);

    Timestamp t0 = workspaceFreeTierUsageDao.findAll().iterator().next().getLastUpdateTime();

    // time elapses, and this project incurs additional cost

    final double newTotalCost = 123.45;
    doReturn(mockBQTableSingleResult(newTotalCost)).when(bigQueryService).executeQuery(any());

    // we do not alert again, but the cost field is updated in the DB

    freeTierBillingService.checkFreeTierBillingUsage();
    verify(notificationService, times(1)).alertUserFreeTierExpiration(eq(user));

    // retrieve from DB again to reflect update after cron
    DbWorkspace dbWorkspace = workspaceDao.findOne(workspace.getWorkspaceId());
    assertThat(dbWorkspace.getBillingStatus()).isEqualTo(BillingStatus.INACTIVE);
    assertThat(dbWorkspace.getBillingAccountType()).isEqualTo(BillingAccountType.FREE_TIER);

    assertThat(workspaceFreeTierUsageDao.count()).isEqualTo(1);
    DbWorkspaceFreeTierUsage dbEntry = workspaceFreeTierUsageDao.findAll().iterator().next();
    assertThat(dbEntry.getUser()).isEqualTo(user);
    assertThat(dbEntry.getWorkspace()).isEqualTo(workspace);
    assertWithinBillingTolerance(dbEntry.getCost(), newTotalCost);
    Timestamp t1 = dbEntry.getLastUpdateTime();
    assertThat(t1.after(t0)).isTrue();
  }

  @Test
  public void getUserFreeTierDollarLimit_default() {
    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
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

    final double freeTierCreditsDollarLimitOverride = 100.0;
    user.setFreeTierCreditsLimitDollarsOverride(freeTierCreditsDollarLimitOverride);
    user = userDao.save(user);
    assertWithinBillingTolerance(
        freeTierBillingService.getUserFreeTierDollarLimit(user),
        freeTierCreditsDollarLimitOverride);

    final double doubleFreeTierCreditsDollarLimitOverride = 200.0;
    user.setFreeTierCreditsLimitDollarsOverride(doubleFreeTierCreditsDollarLimitOverride);
    user = userDao.save(user);

    assertWithinBillingTolerance(
        freeTierBillingService.getUserFreeTierDollarLimit(user),
        doubleFreeTierCreditsDollarLimitOverride);
  }

  @Test
  public void getUserFreeTierDaysLimit_default() {
    DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    final short initialFreeCreditsDaysLimit = 1;
    workbenchConfig.billing.defaultFreeCreditsDaysLimit = initialFreeCreditsDaysLimit;
    assertThat(freeTierBillingService.getUserFreeTierDaysLimit(user))
        .isEqualTo(initialFreeCreditsDaysLimit);

    final short freeCreditsDaysLimitNew = 100;
    workbenchConfig.billing.defaultFreeCreditsDaysLimit = freeCreditsDaysLimitNew;
    assertThat(freeTierBillingService.getUserFreeTierDaysLimit(user))
        .isEqualTo(freeCreditsDaysLimitNew);
  }

  @Test
  public void getUserFreeTierDaysLimit_override() {
    workbenchConfig.billing.defaultFreeCreditsDaysLimit = 123;

    DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);

    final short freeTierCreditsDaysLimitOverride = 100;
    user.setFreeTierCreditsLimitDaysOverride(freeTierCreditsDaysLimitOverride);
    user = userDao.save(user);
    assertThat(freeTierBillingService.getUserFreeTierDaysLimit(user))
        .isEqualTo(freeTierCreditsDaysLimitOverride);

    final short freeTierCreditsDaysLimitNew = 200;
    user.setFreeTierCreditsLimitDaysOverride(freeTierCreditsDaysLimitNew);
    user = userDao.save(user);

    assertThat(freeTierBillingService.getUserFreeTierDaysLimit(user))
        .isEqualTo(freeTierCreditsDaysLimitNew);
  }

  @Test
  public void getUserCachedFreeTierUsage() {
    workbenchConfig.billing.defaultFreeCreditsDollarLimit = 100.0;
    doReturn(mockBQTableSingleResult(100.01)).when(bigQueryService).executeQuery(any());

    final DbUser user1 = createUser(SINGLE_WORKSPACE_TEST_USER);
    createWorkspace(user1, SINGLE_WORKSPACE_TEST_PROJECT);

    // we have not yet had a chance to cache this usage
    assertThat(freeTierBillingService.getUserCachedFreeTierUsage(user1)).isNull();

    freeTierBillingService.checkFreeTierBillingUsage();

    assertWithinBillingTolerance(freeTierBillingService.getUserCachedFreeTierUsage(user1), 100.01);

    createWorkspace(user1, "another project");

    final Map<String, Double> costs =
        ImmutableMap.of(SINGLE_WORKSPACE_TEST_PROJECT, 1000.0, "another project", 100.0);
    doReturn(mockBQTableResult(costs)).when(bigQueryService).executeQuery(any());

    // we have not yet cached the new workspace costs
    assertWithinBillingTolerance(freeTierBillingService.getUserCachedFreeTierUsage(user1), 100.01);

    freeTierBillingService.checkFreeTierBillingUsage();
    final double expectedTotalCachedFreeTierUsage = 1000.0 + 100.01;
    assertWithinBillingTolerance(
        freeTierBillingService.getUserCachedFreeTierUsage(user1), expectedTotalCachedFreeTierUsage);

    final double user2Costs = 999.0;
    final DbUser user2 = createUser("another user");
    createWorkspace(user2, "project 3");

    doReturn(mockBQTableResult(ImmutableMap.of("project 3", user2Costs)))
        .when(bigQueryService)
        .executeQuery(any());

    freeTierBillingService.checkFreeTierBillingUsage();

    assertWithinBillingTolerance(
        freeTierBillingService.getUserCachedFreeTierUsage(user1), expectedTotalCachedFreeTierUsage);
    assertWithinBillingTolerance(
        freeTierBillingService.getUserCachedFreeTierUsage(user2), user2Costs);
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

  private TableResult mockBQTableSingleResult(final Double cost) {
    return mockBQTableResult(ImmutableMap.of(SINGLE_WORKSPACE_TEST_PROJECT, cost));
  }

  private void assertSingleWorkspaceTestDbState(
      DbUser user, DbWorkspace workspaceForQuerying, BillingStatus billingStatus, double cost) {

    final DbWorkspace workspace = workspaceDao.findOne(workspaceForQuerying.getWorkspaceId());
    assertThat(workspace.getBillingStatus()).isEqualTo(billingStatus);
    assertThat(workspace.getBillingAccountType()).isEqualTo(BillingAccountType.FREE_TIER);

    assertThat(workspaceFreeTierUsageDao.count()).isEqualTo(1);
    final DbWorkspaceFreeTierUsage dbEntry = workspaceFreeTierUsageDao.findAll().iterator().next();
    assertThat(dbEntry.getUser()).isEqualTo(user);
    assertThat(dbEntry.getWorkspace()).isEqualTo(workspace);
    assertWithinBillingTolerance(dbEntry.getCost(), cost);
  }

  private DbUser createUser(String email) {
    DbUser user = new DbUser();
    user.setUsername(email);
    return userDao.save(user);
  }

  // we only alert/record for BillingMigrationStatus.NEW workspaces
  private DbWorkspace createWorkspace(DbUser creator, String namespace) {
    return createWorkspace(creator, namespace, BillingMigrationStatus.NEW);
  }

  private DbWorkspace createWorkspace(
      DbUser creator, String namespace, BillingMigrationStatus billingMigrationStatus) {
    DbWorkspace workspace = new DbWorkspace();
    workspace.setCreator(creator);
    workspace.setWorkspaceNamespace(namespace);
    workspace.setBillingMigrationStatusEnum(billingMigrationStatus);
    return workspaceDao.save(workspace);
  }

  private void assertWithinBillingTolerance(double actualValue, double expectedValue) {
    final double tolerance = DEFAULT_PERCENTAGE_TOLERANCE * expectedValue * 0.01;
    assertThat(actualValue).isWithin(tolerance).of(expectedValue);
  }
}
