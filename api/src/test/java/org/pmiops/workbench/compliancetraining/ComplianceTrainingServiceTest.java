package org.pmiops.workbench.compliancetraining;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
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

  // An arbitrary timestamp to use as the anchor time for access module test cases.
  private static final Instant START_INSTANT = FakeClockConfiguration.NOW.toInstant();
  private static final int CLOCK_INCREMENT_MILLIS = 1000;
  private static DbUser providedDbUser;
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
  @MockBean({
    FireCloudService.class,
    InstitutionService.class,
    UserServiceAuditor.class
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
      return accessModules;
    }
  }

  @BeforeEach
  public void setUp() {
    DbUser user = PresetData.createDbUser();
    user.setUsername(USERNAME);
    when(userService.isServiceAccount(user)).thenReturn(false);
    user = userDao.save(user);
    providedDbUser = user;

    providedWorkbenchConfig = WorkbenchConfig.createEmptyConfig();
    providedWorkbenchConfig.access.renewal.expiryDays = 365L;
    providedWorkbenchConfig.access.enableComplianceTraining = true;

    accessModules = TestMockFactory.createAccessModules(accessModuleDao);
  }

  @Test
  public void testSyncComplianceTrainingStatusV2() throws Exception {
    mockGetUserBadgesByBadgeName(ImmutableMap.of(BadgeName.REGISTERED_TIER_TRAINING, defaultBadgeDetails()));

    complianceTrainingService.syncComplianceTrainingStatusV2();

    // The user should be updated in the database with a non-empty completion.
    DbUser user = userDao.findUserByUsername(USERNAME);
    assertModuleCompletionEqual(
        DbAccessModuleName.RT_COMPLIANCE_TRAINING, user, Timestamp.from(START_INSTANT));

    // Completion timestamp should not change when the method is called again.
    tick();
    complianceTrainingService.syncComplianceTrainingStatusV2();

    assertModuleCompletionEqual(
        DbAccessModuleName.RT_COMPLIANCE_TRAINING, user, Timestamp.from(START_INSTANT));
  }

  @Test
  public void
      testSyncComplianceTrainingStatusV2_UpdatesComplianceTrainingVerificationToMoodleIfComplete()
          throws Exception {
    mockGetUserBadgesByBadgeName(ImmutableMap.of(BadgeName.REGISTERED_TIER_TRAINING, defaultBadgeDetails()));

    assertThat(complianceTrainingVerificationDao.findAll()).isEmpty();

    complianceTrainingService.syncComplianceTrainingStatusV2();

    assertThat(complianceTrainingVerificationDao.findAll()).hasSize(1);

    DbUser user = userDao.findUserByUsername(USERNAME);

    // RT is complete, so there should be a verification record.
    var rtAccessModule =
        accessModuleDao.findOneByName(DbAccessModuleName.RT_COMPLIANCE_TRAINING).get();
    var rtUserAccessModule =
        userAccessModuleDao.getByUserAndAccessModule(user, rtAccessModule).get();
    var rtVerification =
        complianceTrainingVerificationDao.getByUserAccessModule(rtUserAccessModule);
    assertThat(rtVerification.isPresent()).isTrue();
    assertThat(rtVerification.get().getComplianceTrainingVerificationSystem())
        .isEqualTo(DbComplianceTrainingVerification.DbComplianceTrainingVerificationSystem.MOODLE);

    // CT is incomplete, so there should not be a verification record.
    var ctAccessModule =
        accessModuleDao.findOneByName(DbAccessModuleName.CT_COMPLIANCE_TRAINING).get();
    var ctUserAccessModule =
        userAccessModuleDao.getByUserAndAccessModule(user, ctAccessModule).get();
    var ctVerification =
        complianceTrainingVerificationDao.getByUserAccessModule(ctUserAccessModule);
    assertThat(ctVerification.isPresent()).isFalse();
  }

  @Test
  public void
      testSyncComplianceTrainingStatusV2_UpdatesComplianceTrainingVerification_OneVerificationPerAccessModule()
          throws Exception {
    var userBadgesByNameRTOnly = ImmutableMap.of(
                BadgeName.REGISTERED_TIER_TRAINING,
                defaultBadgeDetails());
    var userBadgesByNameRTAndCT = ImmutableMap.of(
                BadgeName.REGISTERED_TIER_TRAINING,
                defaultBadgeDetails(),
                BadgeName.CONTROLLED_TIER_TRAINING,
                defaultBadgeDetails());

    assertThat(complianceTrainingVerificationDao.findAll()).isEmpty();

    // Complete RT training
    mockGetUserBadgesByBadgeName(userBadgesByNameRTOnly);
    complianceTrainingService.syncComplianceTrainingStatusV2();
    assertThat(complianceTrainingVerificationDao.findAll()).hasSize(1);

    // Complete CT training
    mockGetUserBadgesByBadgeName(userBadgesByNameRTAndCT);
    complianceTrainingService.syncComplianceTrainingStatusV2();
    assertThat(complianceTrainingVerificationDao.findAll()).hasSize(2);

    DbUser user = userDao.findUserByUsername(USERNAME);

    // RT is complete, so there should be a verification record.
    var rtAccessModule =
        accessModuleDao.findOneByName(DbAccessModuleName.RT_COMPLIANCE_TRAINING).get();
    var rtUserAccessModule =
        userAccessModuleDao.getByUserAndAccessModule(user, rtAccessModule).get();
    var rtVerification =
        complianceTrainingVerificationDao.getByUserAccessModule(rtUserAccessModule);
    assertThat(rtVerification.isPresent()).isTrue();
    assertThat(rtVerification.get().getComplianceTrainingVerificationSystem())
        .isEqualTo(DbComplianceTrainingVerification.DbComplianceTrainingVerificationSystem.MOODLE);

    // CT is complete, so there should be a verification record.
    var ctAccessModule =
        accessModuleDao.findOneByName(DbAccessModuleName.CT_COMPLIANCE_TRAINING).get();
    var ctUserAccessModule =
        userAccessModuleDao.getByUserAndAccessModule(user, ctAccessModule).get();
    var ctVerification =
        complianceTrainingVerificationDao.getByUserAccessModule(ctUserAccessModule);
    assertThat(ctVerification.isPresent()).isTrue();
  }

  @Test
  public void testUpdateComplianceTrainingStatusV2() throws Exception {
    long issued = fakeClock.instant().getEpochSecond() - 10;
    BadgeDetailsV2 retBadge = defaultBadgeDetails().valid(true).lastissued(issued);

    mockGetUserBadgesByBadgeName(ImmutableMap.of(BadgeName.REGISTERED_TIER_TRAINING, retBadge));

    complianceTrainingService.syncComplianceTrainingStatusV2();

    // The user should be updated in the database with a non-empty completion time.
    DbUser user = userDao.findUserByUsername(USERNAME);
    assertModuleCompletionEqual(
        DbAccessModuleName.RT_COMPLIANCE_TRAINING, user, Timestamp.from(START_INSTANT));

    // Deprecate the old training.
    retBadge.setValid(false);

    // Completion timestamp should be wiped out by the expiry timestamp passing.
    complianceTrainingService.syncComplianceTrainingStatusV2();
    assertModuleCompletionEqual(DbAccessModuleName.RT_COMPLIANCE_TRAINING, user, null);

    // The user does a new training.
    retBadge.lastissued(issued + 5).valid(true);

    // Completion and expiry timestamp should be updated.
    complianceTrainingService.syncComplianceTrainingStatusV2();
    assertModuleCompletionEqual(
        DbAccessModuleName.RT_COMPLIANCE_TRAINING, user, Timestamp.from(START_INSTANT));

    // Time passes, user renews training
    retBadge.lastissued(fakeClock.instant().getEpochSecond() + 1);
    fakeClock.increment(5000);

    // Completion should be updated to the current time.
    complianceTrainingService.syncComplianceTrainingStatusV2();
    assertModuleCompletionEqual(
        DbAccessModuleName.RT_COMPLIANCE_TRAINING, user, Timestamp.from(fakeClock.instant()));
  }

  @Test
  public void testUpdateComplianceTrainingStatusV2_controlled() throws Exception {
    long issued = fakeClock.instant().getEpochSecond() - 10;
    BadgeDetailsV2 ctBadge = defaultBadgeDetails().lastissued(issued);
    mockGetUserBadgesByBadgeName(ImmutableMap.of(BadgeName.REGISTERED_TIER_TRAINING, defaultBadgeDetails().lastissued(issued), BadgeName.CONTROLLED_TIER_TRAINING, ctBadge));

    complianceTrainingService.syncComplianceTrainingStatusV2();

    // The user should be updated in the database with a non-empty completion time.
    DbUser user = userDao.findUserByUsername(USERNAME);
    assertModuleCompletionEqual(
        DbAccessModuleName.RT_COMPLIANCE_TRAINING, user, Timestamp.from(START_INSTANT));
    assertModuleCompletionEqual(
        DbAccessModuleName.CT_COMPLIANCE_TRAINING, user, Timestamp.from(START_INSTANT));

    ctBadge.lastissued(fakeClock.instant().getEpochSecond() + 1);
    fakeClock.increment(5000);

    // Renewing training updates completion.
    complianceTrainingService.syncComplianceTrainingStatusV2();
    assertModuleCompletionEqual(
        DbAccessModuleName.RT_COMPLIANCE_TRAINING, user, Timestamp.from(START_INSTANT));
    assertModuleCompletionEqual(
        DbAccessModuleName.CT_COMPLIANCE_TRAINING, user, Timestamp.from(fakeClock.instant()));
  }

  @Test
  public void testSyncComplianceTrainingStatusNullBadgeV2() throws ApiException {
    // When Moodle returns an empty RET badge response, we should clear the completion time.

    DbUser user = userDao.findUserByUsername(USERNAME);
    accessModuleService.updateCompletionTime(
        user, DbAccessModuleName.RT_COMPLIANCE_TRAINING, new Timestamp(12345));

    // An empty map should be returned when we have no badge information.
    mockGetUserBadgesByBadgeName(ImmutableMap.of());

    complianceTrainingService.syncComplianceTrainingStatusV2();
    user = userDao.findUserByUsername(USERNAME);
    assertModuleCompletionEqual(DbAccessModuleName.RT_COMPLIANCE_TRAINING, user, null);
  }

  @Test
  public void testSyncComplianceTrainingStatusBadgeNotFoundV2() throws ApiException {
    // We should propagate a NOT_FOUND exception from the compliance service.
    when(mockMoodleService.getUserBadgesByBadgeName(USERNAME))
        .thenThrow(new ApiException(HttpStatus.NOT_FOUND.value(), "user not found"));
    assertThrows(
        NotFoundException.class, () -> complianceTrainingService.syncComplianceTrainingStatusV2());
  }

  @Test
  public void testSyncComplianceTraining_SkippedForServiceAccountV2() throws ApiException {
    DbUser user = userDao.findUserByUsername(USERNAME);
    when(userService.isServiceAccount(user)).thenReturn(true);
    providedWorkbenchConfig.auth.serviceAccountApiUsers.add(USERNAME);
    complianceTrainingService.syncComplianceTrainingStatusV2();
    verifyNoInteractions(mockMoodleService);
  }

  private void tick() {
    fakeClock.increment(CLOCK_INCREMENT_MILLIS);
  }

  private void assertModuleCompletionEqual(
      DbAccessModuleName moduleName, DbUser user, Timestamp timestamp) {
    assertThat(getModuleCompletionTime(moduleName, user)).isEqualTo(timestamp);
  }

  private Timestamp getModuleCompletionTime(DbAccessModuleName moduleName, DbUser user) {
    return userAccessModuleDao
        .getByUserAndAccessModule(user, accessModuleDao.findOneByName(moduleName).get())
        .get()
        .getCompletionTime();
  }

  private BadgeDetailsV2 defaultBadgeDetails() {
    return new BadgeDetailsV2().valid(true).lastissued(START_INSTANT.getEpochSecond() - 100);
  }

  private void mockGetUserBadgesByBadgeName(Map<BadgeName, BadgeDetailsV2> userBadgesByName) throws ApiException {
    when(mockMoodleService.getUserBadgesByBadgeName(USERNAME)).thenReturn(userBadgesByName);
  }
}
