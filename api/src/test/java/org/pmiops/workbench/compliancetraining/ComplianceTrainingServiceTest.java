package org.pmiops.workbench.compliancetraining;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.access.AccessModuleNameMapperImpl;
import org.pmiops.workbench.access.AccessModuleService;
import org.pmiops.workbench.access.AccessModuleServiceImpl;
import org.pmiops.workbench.access.AccessSyncServiceImpl;
import org.pmiops.workbench.access.AccessTierServiceImpl;
import org.pmiops.workbench.access.UserAccessModuleMapperImpl;
import org.pmiops.workbench.actionaudit.auditors.UserServiceAuditor;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.AccessModuleDao;
import org.pmiops.workbench.db.dao.ComplianceTrainingVerificationDao;
import org.pmiops.workbench.db.dao.UserAccessModuleDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.DbAccessModule;
import org.pmiops.workbench.db.model.DbAccessModule.DbAccessModuleName;
import org.pmiops.workbench.db.model.DbComplianceTrainingVerification;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.institution.InstitutionService;
import org.pmiops.workbench.moodle.ApiException;
import org.pmiops.workbench.moodle.MoodleService;
import org.pmiops.workbench.moodle.MoodleService.BadgeName;
import org.pmiops.workbench.moodle.MoodleServiceImpl;
import org.pmiops.workbench.moodle.model.BadgeDetailsV2;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.utils.PresetData;
import org.pmiops.workbench.utils.TestMockFactory;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;

@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class ComplianceTrainingServiceTest {

  private static final String USERNAME = "abc@fake-research-aou.org";

  private static DbUser user;
  private static WorkbenchConfig providedWorkbenchConfig;
  private static List<DbAccessModule> accessModules;

  @MockBean private MoodleService mockMoodleService;
  @MockBean private UserService userService;

  // use a SpyBean when we need the full service for some tests and mocks for others
  @SpyBean private AccessModuleService accessModuleService;

  @Autowired private AccessModuleDao accessModuleDao;
  @Autowired private FakeClock fakeClock;
  @Autowired private UserAccessModuleDao userAccessModuleDao;
  @Autowired private UserDao userDao;
  @Autowired private ComplianceTrainingService complianceTrainingService;
  @Autowired private ComplianceTrainingVerificationDao complianceTrainingVerificationDao;

  @Import({
    FakeClockConfiguration.class,
    AccessModuleNameMapperImpl.class,
    AccessSyncServiceImpl.class,
    AccessTierServiceImpl.class,
    AccessModuleServiceImpl.class,
    CommonMappers.class,
    UserAccessModuleMapperImpl.class,
    MoodleServiceImpl.class,
    ComplianceTrainingServiceImpl.class
  })
  @MockBean({FireCloudService.class, InstitutionService.class, UserServiceAuditor.class})
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
      return user;
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public List<DbAccessModule> getDbAccessModules() {
      return accessModules;
    }
  }

  @BeforeEach
  public void setUp() {
    user = PresetData.createDbUser();
    user.setUsername(USERNAME);
    when(userService.isServiceAccount(user)).thenReturn(false);
    user = userDao.save(user);

    providedWorkbenchConfig = WorkbenchConfig.createEmptyConfig();
    providedWorkbenchConfig.access.renewal.expiryDays = 365L;
    providedWorkbenchConfig.access.enableComplianceTraining = true;

    accessModules = TestMockFactory.createAccessModules(accessModuleDao);
  }

  @Test
  public void testSyncComplianceTrainingStatus() throws Exception {
    // User completes RT training in Moodle
    var rtBadge = defaultBadgeDetails().lastissued(currentSecond());
    mockGetUserBadgesByBadgeName(
            ImmutableMap.of(BadgeName.REGISTERED_TIER_TRAINING, rtBadge));

    // Time passes
    tick();

    // User syncs training
    complianceTrainingService.syncComplianceTrainingStatus();

    // The user should be updated in the database with a non-empty completion
    // for only RT training.
    var rtCompletionTime = currentTimestamp();
    assertModuleCompletionEqual(
        DbAccessModuleName.RT_COMPLIANCE_TRAINING, rtCompletionTime);
    assertModuleNotCompleted(DbAccessModuleName.CT_COMPLIANCE_TRAINING);

    // Time passes
    tick();

    // User completes CT training in Moodle
    var ctBadge = defaultBadgeDetails().lastissued(currentSecond());
    mockGetUserBadgesByBadgeName(
            ImmutableMap.of(BadgeName.REGISTERED_TIER_TRAINING, rtBadge, BadgeName.CONTROLLED_TIER_TRAINING, ctBadge));

    // Time passes
    tick();

    // User syncs training
    complianceTrainingService.syncComplianceTrainingStatus();

    // The user should be updated in the database with a non-empty completion
    // for both RT and CT training.
    assertModuleCompletionEqual(
            DbAccessModuleName.RT_COMPLIANCE_TRAINING, rtCompletionTime);
    assertModuleCompletionEqual(
            DbAccessModuleName.CT_COMPLIANCE_TRAINING, currentTimestamp());
  }

  @Test
  public void testSyncComplianceTrainingStatus_ResyncCausesNoChanges() throws Exception {
    // Set up: The user completes ands syncs RT training
    mockGetUserBadgesByBadgeName(
            ImmutableMap.of(BadgeName.REGISTERED_TIER_TRAINING, defaultBadgeDetails()));
    complianceTrainingService.syncComplianceTrainingStatus();
    reloadUser();
    var completionTime = currentTimestamp();
    assertModuleCompletionEqual(
            DbAccessModuleName.RT_COMPLIANCE_TRAINING, completionTime);

    // Time passes and the user re-syncs
    tick();
    complianceTrainingService.syncComplianceTrainingStatus();
    reloadUser();

    // Completion timestamp should not change when the method is called again.
    assertModuleCompletionEqual(
            DbAccessModuleName.RT_COMPLIANCE_TRAINING, completionTime);
  }

  @Test
  public void
      testSyncComplianceTrainingStatus_UpdatesVerificationToMoodleIfComplete()
          throws Exception {
    mockGetUserBadgesByBadgeName(
        ImmutableMap.of(BadgeName.REGISTERED_TIER_TRAINING, defaultBadgeDetails()));

    assertThat(complianceTrainingVerificationDao.findAll()).isEmpty();

    complianceTrainingService.syncComplianceTrainingStatus();

    assertThat(complianceTrainingVerificationDao.findAll()).hasSize(1);

    reloadUser();

    // RT is complete, so there should be a verification record.
    var rtVerification = getVerification(DbAccessModuleName.RT_COMPLIANCE_TRAINING);
    assertThat(rtVerification.isPresent()).isTrue();
    assertThat(rtVerification.get().getComplianceTrainingVerificationSystem())
        .isEqualTo(DbComplianceTrainingVerification.DbComplianceTrainingVerificationSystem.MOODLE);

    // CT is incomplete, so there should not be a verification record.
    var ctVerification = getVerification(DbAccessModuleName.CT_COMPLIANCE_TRAINING);
    assertThat(ctVerification.isPresent()).isFalse();
  }

  @Test
  public void
      testSyncComplianceTrainingStatus_UpdatesVerification_OnePerAccessModule()
          throws Exception {
    var userBadgesByNameRTOnly =
        ImmutableMap.of(BadgeName.REGISTERED_TIER_TRAINING, defaultBadgeDetails());
    var userBadgesByNameRTAndCT =
        ImmutableMap.of(
            BadgeName.REGISTERED_TIER_TRAINING,
            defaultBadgeDetails(),
            BadgeName.CONTROLLED_TIER_TRAINING,
            defaultBadgeDetails());

    assertThat(complianceTrainingVerificationDao.findAll()).isEmpty();

    // Complete RT training
    mockGetUserBadgesByBadgeName(userBadgesByNameRTOnly);
    complianceTrainingService.syncComplianceTrainingStatus();
    assertThat(complianceTrainingVerificationDao.findAll()).hasSize(1);

    // Complete CT training
    mockGetUserBadgesByBadgeName(userBadgesByNameRTAndCT);
    complianceTrainingService.syncComplianceTrainingStatus();
    assertThat(complianceTrainingVerificationDao.findAll()).hasSize(2);

    reloadUser();

    // RT is complete, so there should be a verification record.
    var rtVerification = getVerification(DbAccessModuleName.RT_COMPLIANCE_TRAINING);
    assertThat(rtVerification.isPresent()).isTrue();

    // CT is complete, so there should be a verification record.
    var ctVerification = getVerification(DbAccessModuleName.CT_COMPLIANCE_TRAINING);
    assertThat(ctVerification.isPresent()).isTrue();
  }

  @Test
  public void testSyncComplianceTrainingStatus_RenewsExpiredTraining() throws Exception {
    // User completes trainings
    BadgeDetailsV2 rtBadge = defaultBadgeDetails().valid(true).lastissued(currentSecond());
    BadgeDetailsV2 ctBadge = defaultBadgeDetails().valid(true).lastissued(currentSecond());
    mockGetUserBadgesByBadgeName(ImmutableMap.of(BadgeName.REGISTERED_TIER_TRAINING, rtBadge, BadgeName.CONTROLLED_TIER_TRAINING, ctBadge));

    // Time passes
    tick();

    // User syncs training
    complianceTrainingService.syncComplianceTrainingStatus();

    // The user should be updated in the database with a non-empty completion time.
    assertModuleCompletionEqual(
        DbAccessModuleName.RT_COMPLIANCE_TRAINING, currentTimestamp());
    assertModuleCompletionEqual(
        DbAccessModuleName.CT_COMPLIANCE_TRAINING, currentTimestamp());

    // Time passes
    tick();

    // Deprecate the old training.
    rtBadge.setValid(false);
    ctBadge.setValid(false);

    // Time passes
    tick();

    // Completion timestamp should be wiped out by the expiry timestamp passing.
    complianceTrainingService.syncComplianceTrainingStatus();
    assertModuleCompletionEqual(DbAccessModuleName.RT_COMPLIANCE_TRAINING, null);
    assertModuleCompletionEqual(DbAccessModuleName.CT_COMPLIANCE_TRAINING, null);

    // Time passes
    tick();

    // The user does a new training.
    rtBadge.lastissued(currentSecond()).valid(true);
    ctBadge.lastissued(currentSecond()).valid(true);

    // Time passes, user syncs training
    tick();
    complianceTrainingService.syncComplianceTrainingStatus();

    // Completion and expiry timestamp should be updated.
    assertModuleCompletionEqual(
        DbAccessModuleName.RT_COMPLIANCE_TRAINING, currentTimestamp());
    assertModuleCompletionEqual(
            DbAccessModuleName.CT_COMPLIANCE_TRAINING, currentTimestamp());
  }

  @Test
  public void testSyncComplianceTrainingStatus_RenewsExpiringTraining() throws Exception {
    // User completes trainings
    BadgeDetailsV2 rtBadge = defaultBadgeDetails().valid(true).lastissued(currentSecond());
    BadgeDetailsV2 ctBadge = defaultBadgeDetails().valid(true).lastissued(currentSecond());
    mockGetUserBadgesByBadgeName(ImmutableMap.of(BadgeName.REGISTERED_TIER_TRAINING, rtBadge, BadgeName.CONTROLLED_TIER_TRAINING, ctBadge));

    // Time passes
    tick();

    // User syncs training
    complianceTrainingService.syncComplianceTrainingStatus();

    // The user should be updated in the database with a non-empty completion time.
    assertModuleCompletionEqual(
            DbAccessModuleName.RT_COMPLIANCE_TRAINING, currentTimestamp());
    assertModuleCompletionEqual(
            DbAccessModuleName.CT_COMPLIANCE_TRAINING, currentTimestamp());

    // Time passes
    tick();

    // Time passes, user renews training
    tick();
    rtBadge.lastissued(currentSecond());
    ctBadge.lastissued(currentSecond());

    // Time passes, user syncs training
    tick();
    complianceTrainingService.syncComplianceTrainingStatus();

    // Completion should be updated to the current time.
    assertModuleCompletionEqual(
            DbAccessModuleName.RT_COMPLIANCE_TRAINING, currentTimestamp());
    assertModuleCompletionEqual(
            DbAccessModuleName.CT_COMPLIANCE_TRAINING, currentTimestamp());
  }

  @Test
  public void testSyncComplianceTrainingStatus_NullBadge() throws ApiException {
    // When Moodle returns an empty RET badge response, we should clear the completion time.
    accessModuleService.updateCompletionTime(
        user, DbAccessModuleName.RT_COMPLIANCE_TRAINING, new Timestamp(12345));

    // An empty map should be returned when we have no badge information.
    mockGetUserBadgesByBadgeName(ImmutableMap.of());

    complianceTrainingService.syncComplianceTrainingStatus();
    reloadUser();
    assertModuleCompletionEqual(DbAccessModuleName.RT_COMPLIANCE_TRAINING, null);
  }

  @Test
  public void testSyncComplianceTrainingStatus_BadgeNotFound() throws ApiException {
    // We should propagate a NOT_FOUND exception from the compliance service.
    when(mockMoodleService.getUserBadgesByBadgeName(USERNAME))
        .thenThrow(new ApiException(HttpStatus.NOT_FOUND.value(), "user not found"));
    assertThrows(
        NotFoundException.class, () -> complianceTrainingService.syncComplianceTrainingStatus());
  }

  @Test
  public void testSyncComplianceTrainingStatus_SkippedForServiceAccount() throws ApiException {
    when(userService.isServiceAccount(user)).thenReturn(true);
    providedWorkbenchConfig.auth.serviceAccountApiUsers.add(USERNAME);
    complianceTrainingService.syncComplianceTrainingStatus();
    verifyNoInteractions(mockMoodleService);
  }

  private void assertModuleCompletionEqual(
      DbAccessModuleName moduleName, Timestamp timestamp) {
    assertThat(getModuleCompletionTime(moduleName)).isEqualTo(timestamp);
  }

  private void assertModuleNotCompleted(DbAccessModuleName moduleName) {
    assertThat(getModuleCompletionTime(moduleName)).isNull();
  }

  private Timestamp getModuleCompletionTime(DbAccessModuleName moduleName) {
    return userAccessModuleDao
        .getByUserAndAccessModule(user, accessModuleDao.findOneByName(moduleName).get())
        .get()
        .getCompletionTime();
  }

  private BadgeDetailsV2 defaultBadgeDetails() {
    Timestamp start_timestamp = FakeClockConfiguration.NOW;
    return new BadgeDetailsV2().valid(true).lastissued(start_timestamp.toInstant().getEpochSecond() - 100);
  }

  private void mockGetUserBadgesByBadgeName(Map<BadgeName, BadgeDetailsV2> userBadgesByName)
      throws ApiException {
    when(mockMoodleService.getUserBadgesByBadgeName(USERNAME)).thenReturn(userBadgesByName);
  }

  private void reloadUser() {
    user = userDao.findUserByUsername(USERNAME);
  }

  private Optional<DbComplianceTrainingVerification> getVerification(
      DbAccessModuleName moduleName) {
    return accessModuleDao
        .findOneByName(moduleName)
        .flatMap(am -> userAccessModuleDao.getByUserAndAccessModule(user, am))
        .flatMap(uam -> complianceTrainingVerificationDao.getByUserAccessModule(uam));
  }

  private long currentSecond() {
    return fakeClock.instant().getEpochSecond();
  }

  private Timestamp currentTimestamp() {
    return Timestamp.from(fakeClock.instant());
  }

  private void tick() {
    fakeClock.increment(1000); // The time increment is arbitrary
  }
}
