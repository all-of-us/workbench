package org.pmiops.workbench.access;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.google.api.services.directory.model.User;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.actionaudit.Agent;
import org.pmiops.workbench.actionaudit.auditors.UserServiceAuditor;
import org.pmiops.workbench.compliance.ComplianceService;
import org.pmiops.workbench.compliance.ComplianceService.BadgeName;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.AccessModuleDao;
import org.pmiops.workbench.db.dao.UserAccessModuleDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.DbAccessModule;
import org.pmiops.workbench.db.model.DbAccessModule.DbAccessModuleName;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserAccessModule;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.google.DirectoryService;
import org.pmiops.workbench.institution.InstitutionService;
import org.pmiops.workbench.moodle.ApiException;
import org.pmiops.workbench.moodle.model.BadgeDetailsV2;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.utils.TestMockFactory;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;

@DataJpaTest
public class AccessSyncServiceTest {
  private static final String USERNAME = "abc@fake-research-aou.org";

  // An arbitrary timestamp to use as the anchor time for access module test cases.
  private static final Instant START_INSTANT = FakeClockConfiguration.NOW.toInstant();
  private static final int CLOCK_INCREMENT_MILLIS = 1000;

  @Autowired private AccessSyncService accessSyncService;

  @Autowired private AccessModuleDao accessModuleDao;
  @Autowired private AccessModuleService accessModuleService;
  @Autowired private FakeClock fakeClock;
  @Autowired private UserAccessModuleDao userAccessModuleDao;
  @Autowired private UserDao userDao;

  @MockBean private ComplianceService mockComplianceService;
  @MockBean private DirectoryService mockDirectoryService;

  private static WorkbenchConfig providedWorkbenchConfig;
  private static List<DbAccessModule> providedAccessModules;
  private static DbUser providedDbUser;

  @Import({
    CommonMappers.class,
    FakeClockConfiguration.class,
    AccessModuleServiceImpl.class,
    AccessModuleNameMapperImpl.class,
    AccessSyncServiceImpl.class,
    AccessTierServiceImpl.class,
    UserAccessModuleMapperImpl.class,
  })
  @MockBean({
    FireCloudService.class,
    InstitutionService.class,
    UserServiceAuditor.class,
  })
  @TestConfiguration
  static class Configuration {
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    WorkbenchConfig getWorkbenchConfig() {
      return providedWorkbenchConfig;
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    DbUser getDbUser() {
      return providedDbUser;
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public List<DbAccessModule> getDbAccessModules() {
      return providedAccessModules;
    }
  }

  @BeforeEach
  public void setUp() {
    providedWorkbenchConfig = WorkbenchConfig.createEmptyConfig();
    providedWorkbenchConfig.access.renewal.expiryDays = 365L;
    providedWorkbenchConfig.access.enableComplianceTraining = true;
    providedWorkbenchConfig.access.enableEraCommons = true;

    providedAccessModules = TestMockFactory.createAccessModules(accessModuleDao);

    providedDbUser = userDao.save(new DbUser().setUsername(USERNAME));
  }

  @Test
  public void testSyncComplianceTrainingStatusV2() throws Exception {
    long issued = fakeClock.instant().getEpochSecond() - 100;

    Map<BadgeName, BadgeDetailsV2> userBadgesByName = new HashMap<>();
    userBadgesByName.put(
        BadgeName.REGISTERED_TIER_TRAINING, new BadgeDetailsV2().lastissued(issued).valid(true));

    when(mockComplianceService.getUserBadgesByBadgeName(USERNAME)).thenReturn(userBadgesByName);

    accessSyncService.syncComplianceTrainingStatusV2();

    // The user should be updated in the database with a non-empty completion.
    DbUser user = userDao.findUserByUsername(USERNAME);
    assertModuleCompletionEqual(
        DbAccessModuleName.RT_COMPLIANCE_TRAINING, user, Timestamp.from(START_INSTANT));

    // Completion timestamp should not change when the method is called again.
    tick();
    accessSyncService.syncComplianceTrainingStatusV2();

    assertModuleCompletionEqual(
        DbAccessModuleName.RT_COMPLIANCE_TRAINING, user, Timestamp.from(START_INSTANT));
  }

  @Test
  public void testUpdateComplianceTrainingStatusV2() throws Exception {
    Instant originalCompletion = fakeClock.instant();

    long issued = fakeClock.instant().getEpochSecond() - 10;
    BadgeDetailsV2 retBadge = new BadgeDetailsV2().lastissued(issued).valid(true);
    when(mockComplianceService.getUserBadgesByBadgeName(USERNAME))
        .thenReturn(ImmutableMap.of(BadgeName.REGISTERED_TIER_TRAINING, retBadge));

    accessSyncService.syncComplianceTrainingStatusV2();

    // The user should be updated in the database with a non-empty completion time.
    DbUser user = userDao.findUserByUsername(USERNAME);
    assertModuleCompletionEqual(
        DbAccessModuleName.RT_COMPLIANCE_TRAINING, user, Timestamp.from(originalCompletion));

    // Deprecate the old training.
    retBadge.setValid(false);

    // Completion timestamp should be wiped out by the expiry timestamp passing.
    accessSyncService.syncComplianceTrainingStatusV2();
    assertModuleCompletionMissing(DbAccessModuleName.RT_COMPLIANCE_TRAINING, user);

    // The user does a new training.
    retBadge.lastissued(issued + 5).valid(true);

    // Completion and expiry timestamp should be updated.
    accessSyncService.syncComplianceTrainingStatusV2();
    assertModuleCompletionEqual(
        DbAccessModuleName.RT_COMPLIANCE_TRAINING, user, Timestamp.from(originalCompletion));

    // Time passes, user renews training
    tick();
    retBadge.lastissued(fakeClock.instant().getEpochSecond() + 1);
    accessSyncService.syncComplianceTrainingStatusV2();

    // Completion should be updated to the current time.
    Instant newCompletion = fakeClock.instant();
    assertThat(newCompletion.isAfter(originalCompletion)).isTrue();
    assertModuleCompletionEqual(
        DbAccessModuleName.RT_COMPLIANCE_TRAINING, user, Timestamp.from(newCompletion));
  }

  @Test
  public void testUpdateComplianceTrainingStatusV2_controlled() throws Exception {
    Instant originalCompletion = fakeClock.instant();

    long issued = fakeClock.instant().getEpochSecond() - 10;
    BadgeDetailsV2 ctBadge = new BadgeDetailsV2().lastissued(issued).valid(true);
    Map<BadgeName, BadgeDetailsV2> userBadgesByName =
        ImmutableMap.<BadgeName, BadgeDetailsV2>builder()
            .put(
                BadgeName.REGISTERED_TIER_TRAINING,
                new BadgeDetailsV2().lastissued(issued).valid(true))
            .put(BadgeName.CONTROLLED_TIER_TRAINING, ctBadge)
            .build();

    when(mockComplianceService.getUserBadgesByBadgeName(USERNAME)).thenReturn(userBadgesByName);

    accessSyncService.syncComplianceTrainingStatusV2();

    // The user should be updated in the database with a non-empty completion time.
    DbUser user = userDao.findUserByUsername(USERNAME);
    assertModuleCompletionEqual(
        DbAccessModuleName.RT_COMPLIANCE_TRAINING, user, Timestamp.from(originalCompletion));
    assertModuleCompletionEqual(
        DbAccessModuleName.CT_COMPLIANCE_TRAINING, user, Timestamp.from(originalCompletion));

    tick();
    ctBadge.lastissued(fakeClock.instant().getEpochSecond() + 1);

    // Renewing training updates completion.
    accessSyncService.syncComplianceTrainingStatusV2();
    Instant newCompletion = fakeClock.instant();
    assertThat(newCompletion.isAfter(originalCompletion)).isTrue();
    assertModuleCompletionEqual(
        DbAccessModuleName.RT_COMPLIANCE_TRAINING, user, Timestamp.from(originalCompletion));
    assertModuleCompletionEqual(
        DbAccessModuleName.CT_COMPLIANCE_TRAINING, user, Timestamp.from(newCompletion));
  }

  @Test
  public void testSyncComplianceTrainingStatusNullBadgeV2() throws ApiException {
    // When Moodle returns an empty RET badge response, we should clear the completion time.

    DbUser user = userDao.findUserByUsername(USERNAME);
    accessModuleService.updateCompletionTime(
        user, DbAccessModuleName.RT_COMPLIANCE_TRAINING, new Timestamp(12345));

    // An empty map should be returned when we have no badge information.
    Map<BadgeName, BadgeDetailsV2> userBadgesByName = new HashMap<>();

    when(mockComplianceService.getUserBadgesByBadgeName(USERNAME)).thenReturn(userBadgesByName);

    accessSyncService.syncComplianceTrainingStatusV2();
    user = userDao.findUserByUsername(USERNAME);
    assertModuleCompletionMissing(DbAccessModuleName.RT_COMPLIANCE_TRAINING, user);
  }

  @Test
  public void testSyncComplianceTrainingStatusBadgeNotFoundV2() throws ApiException {
    // We should propagate a NOT_FOUND exception from the compliance service.
    when(mockComplianceService.getUserBadgesByBadgeName(USERNAME))
        .thenThrow(new ApiException(HttpStatus.NOT_FOUND.value(), "user not found"));
    assertThrows(NotFoundException.class, () -> accessSyncService.syncComplianceTrainingStatusV2());
  }

  @Test
  public void testSyncComplianceTraining_SkippedForServiceAccountV2() throws ApiException {
    providedWorkbenchConfig.auth.serviceAccountApiUsers.add(USERNAME);
    accessSyncService.syncComplianceTrainingStatusV2();
    verifyZeroInteractions(mockComplianceService);
  }

  @Test
  public void testSyncTwoFactorAuthStatus() {
    User googleUser = new User();
    googleUser.setPrimaryEmail(USERNAME);
    googleUser.setIsEnrolledIn2Sv(true);

    when(mockDirectoryService.getUserOrThrow(USERNAME)).thenReturn(googleUser);
    accessSyncService.syncTwoFactorAuthStatus();
    // twoFactorAuthCompletionTime should now be set
    DbUser user = userDao.findUserByUsername(USERNAME);
    Timestamp twoFactorAuthCompletionTime =
        getModuleCompletionTime(DbAccessModuleName.TWO_FACTOR_AUTH, user).get();
    assertThat(twoFactorAuthCompletionTime).isNotNull();

    // twoFactorAuthCompletionTime should not change when already set
    tick();
    accessSyncService.syncTwoFactorAuthStatus();
    assertModuleCompletionEqual(
        DbAccessModuleName.TWO_FACTOR_AUTH, providedDbUser, twoFactorAuthCompletionTime);

    // unset 2FA in google and check that twoFactorAuthCompletionTime is set to null
    googleUser.setIsEnrolledIn2Sv(false);
    accessSyncService.syncTwoFactorAuthStatus();
    user = userDao.findUserByUsername(USERNAME);
    assertModuleCompletionMissing(DbAccessModuleName.TWO_FACTOR_AUTH, providedDbUser);
  }

  @Test
  public void testSyncDuccVersionStatus_correctVersions() {
    providedWorkbenchConfig.access.currentDuccVersions = ImmutableList.of(3, 4, 5);

    final DbUser user = userDao.findUserByUsername(USERNAME);
    accessModuleService.updateCompletionTime(
        user, DbAccessModuleName.DATA_USER_CODE_OF_CONDUCT, Timestamp.from(START_INSTANT));

    providedWorkbenchConfig.access.currentDuccVersions.forEach(
        version -> {
          DbUser updatedUser =
              userDao.save(
                  user.setDuccAgreement(
                      TestMockFactory.createDuccAgreement(
                          user, version, FakeClockConfiguration.NOW)));
          accessSyncService.syncDuccVersionStatus(updatedUser, Agent.asSystem());
          assertModuleCompletionEqual(
              DbAccessModuleName.DATA_USER_CODE_OF_CONDUCT,
              updatedUser,
              Timestamp.from(START_INSTANT));
        });
  }

  @Test
  public void testSyncDuccVersionStatus_incorrectVersion() {
    providedWorkbenchConfig.access.currentDuccVersions = ImmutableList.of(3, 4, 5);
    int olderVersion = 2;

    DbUser user = userDao.findUserByUsername(USERNAME);
    accessModuleService.updateCompletionTime(
        user, DbAccessModuleName.DATA_USER_CODE_OF_CONDUCT, Timestamp.from(START_INSTANT));

    user =
        userDao.save(
            user.setDuccAgreement(
                TestMockFactory.createDuccAgreement(
                    user, olderVersion, FakeClockConfiguration.NOW)));

    accessSyncService.syncDuccVersionStatus(user, Agent.asSystem());

    // the completion time we recoded here was removed because the DUCC version is non-compliant
    assertModuleCompletionMissing(DbAccessModuleName.DATA_USER_CODE_OF_CONDUCT, user);
  }

  @Test
  public void testSyncDuccVersionStatus_missing() {
    final DbUser user = userDao.findUserByUsername(USERNAME);

    accessSyncService.syncDuccVersionStatus(user, Agent.asSystem());

    assertModuleCompletionMissing(DbAccessModuleName.DATA_USER_CODE_OF_CONDUCT, user);
  }

  private Optional<Timestamp> getModuleCompletionTime(DbAccessModuleName moduleName, DbUser user) {
    return userAccessModuleDao
        .getByUserAndAccessModule(user, accessModuleDao.findOneByName(moduleName).get())
        .map(DbUserAccessModule::getCompletionTime);
  }

  private void assertModuleCompletionEqual(
      DbAccessModuleName moduleName, DbUser user, Timestamp timestamp) {
    assertThat(getModuleCompletionTime(moduleName, user)).hasValue(timestamp);
  }

  private void assertModuleCompletionMissing(DbAccessModuleName moduleName, DbUser user) {
    assertThat(getModuleCompletionTime(moduleName, user).orElse(null)).isNull();
  }

  private void tick() {
    fakeClock.increment(CLOCK_INCREMENT_MILLIS);
  }
}
