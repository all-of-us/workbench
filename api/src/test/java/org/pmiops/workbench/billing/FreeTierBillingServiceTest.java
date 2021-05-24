package org.pmiops.workbench.billing;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
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
import com.google.common.primitives.Doubles;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.Period;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.actionaudit.auditors.UserServiceAuditor;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.dao.WorkspaceFreeTierUsageDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.db.model.DbWorkspace.BillingMigrationStatus;
import org.pmiops.workbench.db.model.DbWorkspaceFreeTierUsage;
import org.pmiops.workbench.mail.MailService;
import org.pmiops.workbench.model.BillingAccountType;
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
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
public class FreeTierBillingServiceTest {

  private static final Instant START_INSTANT = Instant.parse("2000-01-01T00:00:00.00Z");
  private static final FakeClock CLOCK = new FakeClock(START_INSTANT);

  private static final double DEFAULT_PERCENTAGE_TOLERANCE = 0.000001;

  @MockBean private UserServiceAuditor mockUserServiceAuditor;

  @Autowired BigQueryService bigQueryService;
  @Autowired FreeTierBillingService freeTierBillingService;
  @Autowired MailService mailService;

  @Autowired UserDao userDao;
  @Autowired WorkspaceDao workspaceDao;
  @Autowired WorkspaceFreeTierUsageDao workspaceFreeTierUsageDao;

  private static WorkbenchConfig workbenchConfig;

  private static final String SINGLE_WORKSPACE_TEST_USER = "test@test.com";
  private static final String SINGLE_WORKSPACE_TEST_PROJECT = "aou-test-123";

  @TestConfiguration
  @Import({FreeTierBillingService.class})
  @MockBean({BigQueryService.class, MailService.class})
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

    // by default we have 0 spend
    doReturn(mockBQTableSingleResult(0.0)).when(bigQueryService).executeQuery(any());
  }

  @Test
  public void checkFreeTierBillingUsage_exceedsDollarThresholds() throws MessagingException {
    final double limit = 100.0;
    final double costUnderThreshold = 49.5;

    double threshold = 0.5;
    double costOverThreshold = 50.5;
    double remaining = limit - costOverThreshold;

    workbenchConfig.billing.defaultFreeCreditsDollarLimit = limit;

    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT);

    // set an arbitrary registration time

    final Instant registrationTime = START_INSTANT.minus(Period.ofDays(100));
    user.setFirstRegistrationCompletionTime(Timestamp.from(registrationTime));
    userDao.save(user);

    // check that we have not alerted before the threshold

    doReturn(mockBQTableSingleResult(costUnderThreshold)).when(bigQueryService).executeQuery(any());
    freeTierBillingService.checkFreeTierBillingUsage();
    verifyZeroInteractions(mailService);

    // check that we alert for the 50% threshold

    doReturn(mockBQTableSingleResult(costOverThreshold)).when(bigQueryService).executeQuery(any());
    freeTierBillingService.checkFreeTierBillingUsage();
    verify(mailService)
        .alertUserFreeTierDollarThreshold(
            eq(user), eq(threshold), eq(costOverThreshold), eq(remaining));

    // check that we do not alert twice for the 50% threshold

    doReturn(mockBQTableSingleResult(costOverThreshold)).when(bigQueryService).executeQuery(any());
    freeTierBillingService.checkFreeTierBillingUsage();
    verifyZeroInteractions(mailService);

    // check that we alert for the 75% threshold

    threshold = 0.75;
    costOverThreshold = 75.3;
    remaining = limit - costOverThreshold;

    doReturn(mockBQTableSingleResult(costOverThreshold)).when(bigQueryService).executeQuery(any());
    freeTierBillingService.checkFreeTierBillingUsage();
    verify(mailService)
        .alertUserFreeTierDollarThreshold(
            eq(user), eq(threshold), eq(costOverThreshold), eq(remaining));

    // check that we do not alert twice for the 75% threshold

    doReturn(mockBQTableSingleResult(costOverThreshold)).when(bigQueryService).executeQuery(any());
    freeTierBillingService.checkFreeTierBillingUsage();
    verifyZeroInteractions(mailService);

    // check that we alert for expiration when we hit 100%

    final double costToTriggerExpiration = 100.01;

    doReturn(mockBQTableSingleResult(costToTriggerExpiration))
        .when(bigQueryService)
        .executeQuery(any());
    freeTierBillingService.checkFreeTierBillingUsage();
    verify(mailService).alertUserFreeTierExpiration(eq(user));

    // check that we do not alert twice for 100%

    doReturn(mockBQTableSingleResult(costToTriggerExpiration))
        .when(bigQueryService)
        .executeQuery(any());
    freeTierBillingService.checkFreeTierBillingUsage();
    verifyZeroInteractions(mailService);
  }

  @Test
  public void checkFreeTierBillingUsage_altDollarThresholds() throws MessagingException {

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

    // check that we have not alerted before the threshold

    doReturn(mockBQTableSingleResult(costUnderThreshold)).when(bigQueryService).executeQuery(any());
    freeTierBillingService.checkFreeTierBillingUsage();
    verifyZeroInteractions(mailService);

    // check that we alert for the 30% threshold

    doReturn(mockBQTableSingleResult(costOverThreshold)).when(bigQueryService).executeQuery(any());
    freeTierBillingService.checkFreeTierBillingUsage();
    verify(mailService)
        .alertUserFreeTierDollarThreshold(
            eq(user), eq(threshold), eq(costOverThreshold), eq(remaining));

    // check that we do not alert twice for the 30% threshold

    doReturn(mockBQTableSingleResult(costOverThreshold)).when(bigQueryService).executeQuery(any());
    freeTierBillingService.checkFreeTierBillingUsage();
    verifyZeroInteractions(mailService);

    // check that we alert for the 65% threshold

    threshold = 0.65;
    costOverThreshold = 65.01;
    remaining = limit - costOverThreshold;

    doReturn(mockBQTableSingleResult(costOverThreshold)).when(bigQueryService).executeQuery(any());
    freeTierBillingService.checkFreeTierBillingUsage();
    verify(mailService)
        .alertUserFreeTierDollarThreshold(
            eq(user), eq(threshold), eq(costOverThreshold), eq(remaining));

    // check that we do not alert twice for the 65% threshold

    doReturn(mockBQTableSingleResult(costOverThreshold)).when(bigQueryService).executeQuery(any());
    freeTierBillingService.checkFreeTierBillingUsage();
    verifyZeroInteractions(mailService);

    // check that we alert for expiration when we hit 100%

    final double costToTriggerExpiration = 100.01;

    doReturn(mockBQTableSingleResult(costToTriggerExpiration))
        .when(bigQueryService)
        .executeQuery(any());
    freeTierBillingService.checkFreeTierBillingUsage();
    verify(mailService).alertUserFreeTierExpiration(eq(user));

    // check that we do not alert twice for 100%

    doReturn(mockBQTableSingleResult(costToTriggerExpiration))
        .when(bigQueryService)
        .executeQuery(any());
    freeTierBillingService.checkFreeTierBillingUsage();
    verifyZeroInteractions(mailService);
  }

  @Test
  public void checkFreeTierBillingUsage_disabledUserNotIgnored() throws MessagingException {
    workbenchConfig.billing.defaultFreeCreditsDollarLimit = 100.0;
    doReturn(mockBQTableSingleResult(100.01)).when(bigQueryService).executeQuery(any());

    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    user.setDisabled(true);
    userDao.save(user);
    final DbWorkspace workspace = createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT);

    freeTierBillingService.checkFreeTierBillingUsage();
    verify(mailService).alertUserFreeTierExpiration(eq(user));

    assertSingleWorkspaceTestDbState(user, workspace, BillingStatus.INACTIVE, 100.01);
  }

  @Test
  public void checkFreeTierBillingUsage_deletedWorkspaceNotIgnored() throws MessagingException {
    workbenchConfig.billing.defaultFreeCreditsDollarLimit = 100.0;
    doReturn(mockBQTableSingleResult(100.01)).when(bigQueryService).executeQuery(any());

    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    final DbWorkspace workspace = createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT);
    workspace.setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.DELETED);
    workspaceDao.save(workspace);

    freeTierBillingService.checkFreeTierBillingUsage();
    verify(mailService).alertUserFreeTierExpiration(eq(user));

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
    verifyZeroInteractions(mailService);

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
    verifyZeroInteractions(mailService);

    assertSingleWorkspaceTestDbState(user, workspace, BillingStatus.ACTIVE, 49.99);
  }

  @Test
  public void maybeSetDollarLimitOverride_true() {
    workbenchConfig.billing.defaultFreeCreditsDollarLimit = 100.0;
    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);

    assertThat(freeTierBillingService.maybeSetDollarLimitOverride(user, 200.0)).isTrue();
    verify(mockUserServiceAuditor)
        .fireSetFreeTierDollarLimitOverride(user.getUserId(), null, 200.0);
    assertWithinBillingTolerance(freeTierBillingService.getUserFreeTierDollarLimit(user), 200.0);

    assertThat(freeTierBillingService.maybeSetDollarLimitOverride(user, 100.0)).isTrue();
    verify(mockUserServiceAuditor)
        .fireSetFreeTierDollarLimitOverride(user.getUserId(), 200.0, 100.0);
    assertWithinBillingTolerance(freeTierBillingService.getUserFreeTierDollarLimit(user), 100.0);
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
  public void maybeSetDollarLimitOverride_above_usage() throws MessagingException {
    workbenchConfig.billing.defaultFreeCreditsDollarLimit = 100.0;
    doReturn(mockBQTableSingleResult(150.0)).when(bigQueryService).executeQuery(any());

    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    final DbWorkspace workspace = createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT);

    assertThat(freeTierBillingService.getCachedFreeTierUsage(user)).isNull();
    assertWithinBillingTolerance(freeTierBillingService.getUserFreeTierDollarLimit(user), 100.0);

    freeTierBillingService.checkFreeTierBillingUsage();
    verify(mailService).alertUserFreeTierExpiration(eq(user));
    assertSingleWorkspaceTestDbState(user, workspace, BillingStatus.INACTIVE, 150.0);
    assertWithinBillingTolerance(freeTierBillingService.getCachedFreeTierUsage(user), 150.0);

    freeTierBillingService.maybeSetDollarLimitOverride(user, 200.0);
    verify(mockUserServiceAuditor)
        .fireSetFreeTierDollarLimitOverride(user.getUserId(), null, 200.0);
    assertWithinBillingTolerance(freeTierBillingService.getUserFreeTierDollarLimit(user), 200.0);
    assertSingleWorkspaceTestDbState(user, workspace, BillingStatus.ACTIVE, 150.0);

    freeTierBillingService.checkFreeTierBillingUsage();
    verifyZeroInteractions(mailService);
    assertSingleWorkspaceTestDbState(user, workspace, BillingStatus.ACTIVE, 150.0);
  }

  // do not reactivate workspaces if the new dollar limit is still below the usage

  @Test
  public void setFreeTierDollarOverride_under_usage() throws MessagingException {
    workbenchConfig.billing.defaultFreeCreditsDollarLimit = 100.0;
    doReturn(mockBQTableSingleResult(300.0)).when(bigQueryService).executeQuery(any());

    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    final DbWorkspace workspace = createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT);

    assertThat(freeTierBillingService.getCachedFreeTierUsage(user)).isNull();
    assertWithinBillingTolerance(freeTierBillingService.getUserFreeTierDollarLimit(user), 100.0);

    freeTierBillingService.checkFreeTierBillingUsage();
    verify(mailService).alertUserFreeTierExpiration(eq(user));
    assertSingleWorkspaceTestDbState(user, workspace, BillingStatus.INACTIVE, 300.0);
    assertWithinBillingTolerance(freeTierBillingService.getCachedFreeTierUsage(user), 300.0);

    freeTierBillingService.maybeSetDollarLimitOverride(user, 200.0);
    verify(mockUserServiceAuditor)
        .fireSetFreeTierDollarLimitOverride(user.getUserId(), null, 200.0);
    assertWithinBillingTolerance(freeTierBillingService.getUserFreeTierDollarLimit(user), 200.0);
    assertSingleWorkspaceTestDbState(user, workspace, BillingStatus.INACTIVE, 300.0);

    freeTierBillingService.checkFreeTierBillingUsage();
    verifyZeroInteractions(mailService);
    assertSingleWorkspaceTestDbState(user, workspace, BillingStatus.INACTIVE, 300.0);

    verify(mockUserServiceAuditor)
        .fireSetFreeTierDollarLimitOverride(user.getUserId(), null, 200.0);
  }

  @Test
  public void checkFreeTierBillingUsage_combinedProjectsExceedsLimit() throws MessagingException {
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
    verify(mailService).alertUserFreeTierExpiration(eq(user));
    assertThat(workspaceFreeTierUsageDao.count()).isEqualTo(2);

    // confirm DB updates after checkFreeTierBillingUsage()

    for (DbWorkspace ws : Arrays.asList(ws1, ws2)) {
      // retrieve from DB again to reflect update after cron
      DbWorkspace dbWorkspace = workspaceDao.findById(ws.getWorkspaceId()).get();
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
  public void checkFreeTierBillingUsage_twoUsers() throws MessagingException {
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
    verify(mailService).alertUserFreeTierExpiration(eq(user1));
    verify(mailService).alertUserFreeTierExpiration(eq(user2));

    assertThat(workspaceFreeTierUsageDao.count()).isEqualTo(2);

    // confirm DB updates after checkFreeTierBillingUsage()

    final DbWorkspace dbWorkspace1 = workspaceDao.findById(ws1.getWorkspaceId()).get();
    assertThat(dbWorkspace1.getBillingStatus()).isEqualTo(BillingStatus.INACTIVE);
    assertThat(dbWorkspace1.getBillingAccountType()).isEqualTo(BillingAccountType.FREE_TIER);

    final DbWorkspaceFreeTierUsage usage1 = workspaceFreeTierUsageDao.findOneByWorkspace(ws1);
    assertThat(usage1.getUser()).isEqualTo(user1);
    assertWithinBillingTolerance(usage1.getCost(), cost1);

    final DbWorkspace dbWorkspace2 = workspaceDao.findById(ws2.getWorkspaceId()).get();
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

    verifyZeroInteractions(mailService);
    assertThat(workspaceFreeTierUsageDao.count()).isEqualTo(0);
  }

  @Test
  public void checkFreeTierBillingUsage_ignoreMIGRATEDMigrationStatus() {
    workbenchConfig.billing.defaultFreeCreditsDollarLimit = 100.0;
    doReturn(mockBQTableSingleResult(100.01)).when(bigQueryService).executeQuery(any());

    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT, BillingMigrationStatus.MIGRATED);
    freeTierBillingService.checkFreeTierBillingUsage();

    verifyZeroInteractions(mailService);
    assertThat(workspaceFreeTierUsageDao.count()).isEqualTo(0);
  }

  @Test
  public void checkFreeTierBillingUsage_dbUpdate() throws MessagingException {
    workbenchConfig.billing.defaultFreeCreditsDollarLimit = 100.0;
    doReturn(mockBQTableSingleResult(100.01)).when(bigQueryService).executeQuery(any());

    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    final DbWorkspace workspace = createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT);

    freeTierBillingService.checkFreeTierBillingUsage();
    verify(mailService).alertUserFreeTierExpiration(eq(user));
    assertSingleWorkspaceTestDbState(user, workspace, BillingStatus.INACTIVE, 100.01);

    Timestamp t0 = workspaceFreeTierUsageDao.findAll().iterator().next().getLastUpdateTime();

    // time elapses, and this project incurs additional cost

    final double newTotalCost = 123.45;
    doReturn(mockBQTableSingleResult(newTotalCost)).when(bigQueryService).executeQuery(any());

    // we do not alert again, but the cost field is updated in the DB

    freeTierBillingService.checkFreeTierBillingUsage();
    verify(mailService, times(1)).alertUserFreeTierExpiration(eq(user));

    // retrieve from DB again to reflect update after cron
    DbWorkspace dbWorkspace = workspaceDao.findById(workspace.getWorkspaceId()).get();
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

    final double limit1 = 100.0;
    freeTierBillingService.maybeSetDollarLimitOverride(user, limit1);
    verify(mockUserServiceAuditor)
        .fireSetFreeTierDollarLimitOverride(user.getUserId(), null, limit1);
    assertWithinBillingTolerance(freeTierBillingService.getUserFreeTierDollarLimit(user), limit1);

    final double limit2 = 200.0;
    freeTierBillingService.maybeSetDollarLimitOverride(user, limit2);
    verify(mockUserServiceAuditor)
        .fireSetFreeTierDollarLimitOverride(user.getUserId(), limit1, limit2);
    assertWithinBillingTolerance(freeTierBillingService.getUserFreeTierDollarLimit(user), limit2);
  }

  @Test
  public void getUserCachedFreeTierUsage() {
    workbenchConfig.billing.defaultFreeCreditsDollarLimit = 100.0;
    doReturn(mockBQTableSingleResult(100.01)).when(bigQueryService).executeQuery(any());

    final DbUser user1 = createUser(SINGLE_WORKSPACE_TEST_USER);
    createWorkspace(user1, SINGLE_WORKSPACE_TEST_PROJECT);

    // we have not yet had a chance to cache this usage
    assertThat(freeTierBillingService.getCachedFreeTierUsage(user1)).isNull();
    assertThat(freeTierBillingService.userHasRemainingFreeTierCredits(user1)).isTrue();

    freeTierBillingService.checkFreeTierBillingUsage();

    assertWithinBillingTolerance(freeTierBillingService.getCachedFreeTierUsage(user1), 100.01);
    assertThat(freeTierBillingService.userHasRemainingFreeTierCredits(user1)).isFalse();

    createWorkspace(user1, "another project");

    final Map<String, Double> costs =
        ImmutableMap.of(SINGLE_WORKSPACE_TEST_PROJECT, 1000.0, "another project", 200.0);
    doReturn(mockBQTableResult(costs)).when(bigQueryService).executeQuery(any());

    // we have not yet cached the new workspace costs
    assertWithinBillingTolerance(freeTierBillingService.getCachedFreeTierUsage(user1), 100.01);

    freeTierBillingService.checkFreeTierBillingUsage();
    final double expectedTotalCachedFreeTierUsage = 1000.0 + 200.0;
    assertWithinBillingTolerance(
        freeTierBillingService.getCachedFreeTierUsage(user1), expectedTotalCachedFreeTierUsage);

    final double user2Costs = 999.0;
    final DbUser user2 = createUser("another user");
    createWorkspace(user2, "project 3");

    doReturn(mockBQTableResult(ImmutableMap.of("project 3", user2Costs)))
        .when(bigQueryService)
        .executeQuery(any());

    freeTierBillingService.checkFreeTierBillingUsage();

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

    // 99.99 < 100.0
    doReturn(mockBQTableSingleResult(99.99)).when(bigQueryService).executeQuery(any());
    freeTierBillingService.checkFreeTierBillingUsage();
    assertThat(freeTierBillingService.userHasRemainingFreeTierCredits(user1)).isTrue();

    // 100.01 > 100.0
    doReturn(mockBQTableSingleResult(100.01)).when(bigQueryService).executeQuery(any());
    freeTierBillingService.checkFreeTierBillingUsage();
    assertThat(freeTierBillingService.userHasRemainingFreeTierCredits(user1)).isFalse();

    // 100.01 < 200.0
    workbenchConfig.billing.defaultFreeCreditsDollarLimit = 200.0;
    assertThat(freeTierBillingService.userHasRemainingFreeTierCredits(user1)).isTrue();
  }

  @Test
  public void test_disableOnlyFreeTierWorkspaces() throws MessagingException {
    workbenchConfig.billing.defaultFreeCreditsDollarLimit = 100.0;
    doReturn(mockBQTableSingleResult(100.01)).when(bigQueryService).executeQuery(any());

    final DbUser user = createUser(SINGLE_WORKSPACE_TEST_USER);
    final DbWorkspace freeTierWorkspace = createWorkspace(user, SINGLE_WORKSPACE_TEST_PROJECT);
    final DbWorkspace userAccountWorkspace = new DbWorkspace();
    userAccountWorkspace.setCreator(user);
    userAccountWorkspace.setWorkspaceNamespace("some other namespace");
    userAccountWorkspace.setBillingMigrationStatusEnum(BillingMigrationStatus.NEW);
    userAccountWorkspace.setBillingAccountName("some other account");
    userAccountWorkspace.setBillingStatus(BillingStatus.ACTIVE);
    workspaceDao.save(userAccountWorkspace);

    freeTierBillingService.checkFreeTierBillingUsage();
    verify(mailService).alertUserFreeTierExpiration(eq(user));

    assertSingleWorkspaceTestDbState(user, freeTierWorkspace, BillingStatus.INACTIVE, 100.01);

    final DbWorkspace retrievedWorkspace =
        workspaceDao.findById(userAccountWorkspace.getWorkspaceId()).get();
    assertThat(retrievedWorkspace.getBillingStatus()).isEqualTo(BillingStatus.ACTIVE);
    // TODO RW-5107
    // assertThat(retrievedWorkspace.getBillingAccountType()).isEqualTo(BillingAccountType.USER_PROVIDED);
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

    final DbWorkspace workspace =
        workspaceDao.findById(workspaceForQuerying.getWorkspaceId()).get();
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
    user.setFirstRegistrationCompletionTime(Timestamp.from(START_INSTANT));
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
    workspace.setBillingAccountName(workbenchConfig.billing.freeTierBillingAccountName());
    workspace.setBillingAccountType(BillingAccountType.FREE_TIER);
    return workspaceDao.save(workspace);
  }

  private void assertWithinBillingTolerance(double actualValue, double expectedValue) {
    final double tolerance = DEFAULT_PERCENTAGE_TOLERANCE * expectedValue * 0.01;
    assertThat(actualValue).isWithin(tolerance).of(expectedValue);
  }
}
