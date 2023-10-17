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
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.absorb.AbsorbService;
import org.pmiops.workbench.absorb.Credentials;
import org.pmiops.workbench.absorb.Enrollment;
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
import org.pmiops.workbench.db.model.DbUserAccessModule;
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

  private static final String RT_ABSORB_COURSE_ID = "1234";
  private static final String CT_ABSORB_COURSE_ID = "5678";
  private static final Credentials FAKE_CREDENTIALS = new Credentials("fake", "fake", "fake");

  private static DbUser user;
  private static WorkbenchConfig providedWorkbenchConfig;
  private static List<DbAccessModule> accessModules;

  @MockBean private MoodleService mockMoodleService;
  @MockBean private AbsorbService mockAbsorbService;
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
  public void setUp() throws org.pmiops.workbench.absorb.ApiException {
    user = PresetData.createDbUser();
    user.setUsername(USERNAME);
    when(userService.isServiceAccount(user)).thenReturn(false);
    user = userDao.save(user);

    providedWorkbenchConfig = WorkbenchConfig.createEmptyConfig();
    providedWorkbenchConfig.access.renewal.expiryDays = 365L;
    providedWorkbenchConfig.access.enableComplianceTraining = true;

    providedWorkbenchConfig.absorb.rtTrainingCourseId = RT_ABSORB_COURSE_ID;
    providedWorkbenchConfig.absorb.ctTrainingCourseId = CT_ABSORB_COURSE_ID;

    accessModules = TestMockFactory.createAccessModules(accessModuleDao);

    when(mockAbsorbService.fetchCredentials(USERNAME)).thenReturn(FAKE_CREDENTIALS);
  }

  @Test
  public void testSyncComplianceTrainingStatus_Moodle() throws Exception {
    providedWorkbenchConfig.absorb.enabledForNewUsers = false;

    // User completes RT training in Moodle
    var rtBadge = defaultBadgeDetails().lastissued(currentSecond());
    mockGetUserBadgesByBadgeName(ImmutableMap.of(BadgeName.REGISTERED_TIER_TRAINING, rtBadge));

    // Time passes
    tick();

    // User syncs training
    user = complianceTrainingService.syncComplianceTrainingStatus();

    // The user should be updated in the database with a non-empty completion
    // for only RT training.
    var rtCompletionTime = currentTimestamp();
    assertModuleCompletionEqual(DbAccessModuleName.RT_COMPLIANCE_TRAINING, rtCompletionTime);
    assertModuleNotCompleted(DbAccessModuleName.CT_COMPLIANCE_TRAINING);

    // There should be a Moodle (not Absorb) verification record for RT but not CT.
    assertThat(
            getVerification(DbAccessModuleName.RT_COMPLIANCE_TRAINING)
                .get()
                .getComplianceTrainingVerificationSystem())
        .isEqualTo(DbComplianceTrainingVerification.DbComplianceTrainingVerificationSystem.MOODLE);
    assertThat(getVerification(DbAccessModuleName.CT_COMPLIANCE_TRAINING).isPresent()).isFalse();

    // Time passes
    tick();

    // User completes CT training in Moodle
    var ctBadge = defaultBadgeDetails().lastissued(currentSecond());
    mockGetUserBadgesByBadgeName(
        ImmutableMap.of(
            BadgeName.REGISTERED_TIER_TRAINING,
            rtBadge,
            BadgeName.CONTROLLED_TIER_TRAINING,
            ctBadge));

    // Time passes
    tick();

    // User syncs training
    user = complianceTrainingService.syncComplianceTrainingStatus();

    // The user should be updated in the database with a non-empty completion
    // for both RT and CT training.
    assertModuleCompletionEqual(DbAccessModuleName.RT_COMPLIANCE_TRAINING, rtCompletionTime);
    assertModuleCompletionEqual(DbAccessModuleName.CT_COMPLIANCE_TRAINING, currentTimestamp());

    // There should be Moodle (not Absorb) verification records.
    assertThat(
            getVerification(DbAccessModuleName.RT_COMPLIANCE_TRAINING)
                .get()
                .getComplianceTrainingVerificationSystem())
        .isEqualTo(DbComplianceTrainingVerification.DbComplianceTrainingVerificationSystem.MOODLE);
    assertThat(
            getVerification(DbAccessModuleName.CT_COMPLIANCE_TRAINING)
                .get()
                .getComplianceTrainingVerificationSystem())
        .isEqualTo(DbComplianceTrainingVerification.DbComplianceTrainingVerificationSystem.MOODLE);
  }

  @Test
  public void testSyncComplianceTrainingStatus_Absorb() throws Exception {
    providedWorkbenchConfig.absorb.enabledForNewUsers = true;

    // User completes RT training in Absorb
    var rtCompletionTime = currentInstant();
    stubAbsorbOnlyRTComplete(rtCompletionTime);

    // User syncs training
    user = complianceTrainingService.syncComplianceTrainingStatus();

    // The user should be updated in the database with a non-empty completion
    // for only RT training.
    assertModuleCompletionEqual(
        DbAccessModuleName.RT_COMPLIANCE_TRAINING, Timestamp.from(rtCompletionTime));
    assertModuleNotCompleted(DbAccessModuleName.CT_COMPLIANCE_TRAINING);

    // There should be an Absorb (not Moodle) verification record for RT but not CT.
    assertThat(
            getVerification(DbAccessModuleName.RT_COMPLIANCE_TRAINING)
                .get()
                .getComplianceTrainingVerificationSystem())
        .isEqualTo(DbComplianceTrainingVerification.DbComplianceTrainingVerificationSystem.ABSORB);
    assertThat(getVerification(DbAccessModuleName.CT_COMPLIANCE_TRAINING).isPresent()).isFalse();

    // Time passes
    tick();

    // User completes CT training in Absorb
    var ctCompletionTime = currentInstant();
    stubAbsorbAllTrainingsComplete(rtCompletionTime, ctCompletionTime);

    // User syncs training
    user = complianceTrainingService.syncComplianceTrainingStatus();

    // The user should be updated in the database with a non-empty completion
    // for both RT and CT training.
    assertModuleCompletionEqual(
        DbAccessModuleName.RT_COMPLIANCE_TRAINING, Timestamp.from(rtCompletionTime));
    assertModuleCompletionEqual(
        DbAccessModuleName.CT_COMPLIANCE_TRAINING, Timestamp.from(ctCompletionTime));

    // There should be Absorb (not Moodle) verification records.
    assertThat(
            getVerification(DbAccessModuleName.RT_COMPLIANCE_TRAINING)
                .get()
                .getComplianceTrainingVerificationSystem())
        .isEqualTo(DbComplianceTrainingVerification.DbComplianceTrainingVerificationSystem.ABSORB);
    assertThat(
            getVerification(DbAccessModuleName.CT_COMPLIANCE_TRAINING)
                .get()
                .getComplianceTrainingVerificationSystem())
        .isEqualTo(DbComplianceTrainingVerification.DbComplianceTrainingVerificationSystem.ABSORB);
  }

  @Test
  public void testSyncComplianceTrainingStatus_Moodle_ResyncCausesNoChanges() throws Exception {
    providedWorkbenchConfig.absorb.enabledForNewUsers = false;

    // Set up: The user completes ands syncs RT training
    mockGetUserBadgesByBadgeName(
        ImmutableMap.of(BadgeName.REGISTERED_TIER_TRAINING, defaultBadgeDetails()));
    user = complianceTrainingService.syncComplianceTrainingStatus();
    var completionTime = currentTimestamp();
    assertModuleCompletionEqual(DbAccessModuleName.RT_COMPLIANCE_TRAINING, completionTime);

    // Time passes and the user re-syncs
    tick();
    user = complianceTrainingService.syncComplianceTrainingStatus();

    // Completion timestamp should not change when the method is called again.
    assertModuleCompletionEqual(DbAccessModuleName.RT_COMPLIANCE_TRAINING, completionTime);
  }

  @Test
  public void testSyncComplianceTrainingStatus_Absorb_ResyncCausesNoChanges() throws Exception {
    providedWorkbenchConfig.absorb.enabledForNewUsers = true;

    // Set up: The user completes ands syncs RT training
    var completionTime = currentInstant();
    stubAbsorbOnlyRTComplete(completionTime);
    user = complianceTrainingService.syncComplianceTrainingStatus();
    assertModuleCompletionEqual(
        DbAccessModuleName.RT_COMPLIANCE_TRAINING, Timestamp.from(completionTime));

    // Time passes and the user re-syncs
    tick();
    user = complianceTrainingService.syncComplianceTrainingStatus();

    // Completion timestamp should not change when the method is called again.
    assertModuleCompletionEqual(
        DbAccessModuleName.RT_COMPLIANCE_TRAINING, Timestamp.from(completionTime));
  }

  @Test
  public void testSyncComplianceTrainingStatus_Moodle_UpdatesVerificationOncePerAccessModule()
      throws Exception {
    providedWorkbenchConfig.absorb.enabledForNewUsers = false;

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
    user = complianceTrainingService.syncComplianceTrainingStatus();
    assertThat(complianceTrainingVerificationDao.findAll()).hasSize(1);

    // Complete CT training
    mockGetUserBadgesByBadgeName(userBadgesByNameRTAndCT);
    user = complianceTrainingService.syncComplianceTrainingStatus();
    assertThat(complianceTrainingVerificationDao.findAll()).hasSize(2);

    // RT is complete, so there should be a verification record.
    var rtVerification = getVerification(DbAccessModuleName.RT_COMPLIANCE_TRAINING);
    assertThat(rtVerification.isPresent()).isTrue();

    // CT is complete, so there should be a verification record.
    var ctVerification = getVerification(DbAccessModuleName.CT_COMPLIANCE_TRAINING);
    assertThat(ctVerification.isPresent()).isTrue();
  }

  @Test
  public void testSyncComplianceTrainingStatus_Absorb_UpdatesVerificationOncePerAccessModule()
      throws Exception {
    providedWorkbenchConfig.absorb.enabledForNewUsers = true;

    assertThat(complianceTrainingVerificationDao.findAll()).isEmpty();

    // Complete RT training
    stubAbsorbOnlyRTComplete(currentInstant());
    user = complianceTrainingService.syncComplianceTrainingStatus();
    assertThat(complianceTrainingVerificationDao.findAll()).hasSize(1);

    // Complete CT training
    stubAbsorbAllTrainingsComplete(currentInstant(), currentInstant());
    user = complianceTrainingService.syncComplianceTrainingStatus();
    assertThat(complianceTrainingVerificationDao.findAll()).hasSize(2);

    // RT is complete, so there should be a verification record.
    var rtVerification = getVerification(DbAccessModuleName.RT_COMPLIANCE_TRAINING);
    assertThat(rtVerification.isPresent()).isTrue();

    // CT is complete, so there should be a verification record.
    var ctVerification = getVerification(DbAccessModuleName.CT_COMPLIANCE_TRAINING);
    assertThat(ctVerification.isPresent()).isTrue();
  }

  @Test
  public void testSyncComplianceTrainingStatus_Absorb_DoesNothingIfUserHasntSignedIntoAbsorb()
      throws Exception {
    providedWorkbenchConfig.absorb.enabledForNewUsers = true;
    when(mockAbsorbService.userHasLoggedIntoAbsorb(FAKE_CREDENTIALS)).thenReturn(false);

    user = complianceTrainingService.syncComplianceTrainingStatus();

    assertModuleNotCompleted(DbAccessModuleName.RT_COMPLIANCE_TRAINING);
    assertModuleNotCompleted(DbAccessModuleName.CT_COMPLIANCE_TRAINING);
  }

  @Test
  public void testSyncComplianceTrainingStatus_Moodle_DoesNothingIfNoCoursesComplete()
      throws Exception {
    providedWorkbenchConfig.absorb.enabledForNewUsers = false;
    mockGetUserBadgesByBadgeName(Map.of());

    user = complianceTrainingService.syncComplianceTrainingStatus();

    assertModuleNotCompleted(DbAccessModuleName.RT_COMPLIANCE_TRAINING);
    assertModuleNotCompleted(DbAccessModuleName.CT_COMPLIANCE_TRAINING);
  }

  @Test
  public void testSyncComplianceTrainingStatus_Absorb_DoesNothingIfNoCoursesComplete()
      throws Exception {
    providedWorkbenchConfig.absorb.enabledForNewUsers = true;
    stubAbsorbNoCoursesComplete();

    user = complianceTrainingService.syncComplianceTrainingStatus();

    assertModuleNotCompleted(DbAccessModuleName.RT_COMPLIANCE_TRAINING);
    assertModuleNotCompleted(DbAccessModuleName.CT_COMPLIANCE_TRAINING);
  }

  @Test
  public void testSyncComplianceTrainingStatus_Moodle_RenewsExpiredTraining() throws Exception {
    providedWorkbenchConfig.absorb.enabledForNewUsers = false;

    // User completes trainings
    BadgeDetailsV2 rtBadge = defaultBadgeDetails().valid(true).lastissued(currentSecond());
    BadgeDetailsV2 ctBadge = defaultBadgeDetails().valid(true).lastissued(currentSecond());
    mockGetUserBadgesByBadgeName(
        ImmutableMap.of(
            BadgeName.REGISTERED_TIER_TRAINING,
            rtBadge,
            BadgeName.CONTROLLED_TIER_TRAINING,
            ctBadge));

    // Time passes
    tick();

    // User syncs training
    user = complianceTrainingService.syncComplianceTrainingStatus();

    // The user should be updated in the database with a non-empty completion time.
    assertModuleCompletionEqual(DbAccessModuleName.RT_COMPLIANCE_TRAINING, currentTimestamp());
    assertModuleCompletionEqual(DbAccessModuleName.CT_COMPLIANCE_TRAINING, currentTimestamp());

    // Time passes
    tick();

    // Deprecate the old training.
    rtBadge.setValid(false);
    ctBadge.setValid(false);

    // Time passes
    tick();

    // Completion timestamp should be wiped out by the expiry timestamp passing.
    user = complianceTrainingService.syncComplianceTrainingStatus();
    assertModuleCompletionEqual(DbAccessModuleName.RT_COMPLIANCE_TRAINING, null);
    assertModuleCompletionEqual(DbAccessModuleName.CT_COMPLIANCE_TRAINING, null);

    // Time passes
    tick();

    // The user does a new training.
    rtBadge.lastissued(currentSecond()).valid(true);
    ctBadge.lastissued(currentSecond()).valid(true);

    // Time passes, user syncs training
    tick();
    user = complianceTrainingService.syncComplianceTrainingStatus();

    // Completion and expiry timestamp should be updated.
    assertModuleCompletionEqual(DbAccessModuleName.RT_COMPLIANCE_TRAINING, currentTimestamp());
    assertModuleCompletionEqual(DbAccessModuleName.CT_COMPLIANCE_TRAINING, currentTimestamp());
  }

  @Test
  public void testSyncComplianceTrainingStatus_Moodle_RenewsExpiringTraining() throws Exception {
    providedWorkbenchConfig.absorb.enabledForNewUsers = false;

    // User completes trainings
    BadgeDetailsV2 rtBadge = defaultBadgeDetails().valid(true).lastissued(currentSecond());
    BadgeDetailsV2 ctBadge = defaultBadgeDetails().valid(true).lastissued(currentSecond());
    mockGetUserBadgesByBadgeName(
        ImmutableMap.of(
            BadgeName.REGISTERED_TIER_TRAINING,
            rtBadge,
            BadgeName.CONTROLLED_TIER_TRAINING,
            ctBadge));

    // Time passes
    tick();

    // User syncs training
    user = complianceTrainingService.syncComplianceTrainingStatus();

    // The user should be updated in the database with a non-empty completion time.
    assertModuleCompletionEqual(DbAccessModuleName.RT_COMPLIANCE_TRAINING, currentTimestamp());
    assertModuleCompletionEqual(DbAccessModuleName.CT_COMPLIANCE_TRAINING, currentTimestamp());

    // Time passes
    tick();

    // Time passes, user renews training
    tick();
    rtBadge.lastissued(currentSecond());
    ctBadge.lastissued(currentSecond());

    // Time passes, user syncs training
    tick();
    user = complianceTrainingService.syncComplianceTrainingStatus();

    // Completion should be updated to the current time.
    assertModuleCompletionEqual(DbAccessModuleName.RT_COMPLIANCE_TRAINING, currentTimestamp());
    assertModuleCompletionEqual(DbAccessModuleName.CT_COMPLIANCE_TRAINING, currentTimestamp());
  }

  @Test
  public void testSyncComplianceTrainingStatus_Moodle_NullBadge() throws Exception {
    providedWorkbenchConfig.absorb.enabledForNewUsers = false;

    // When Moodle returns an empty RET badge response, we should clear the completion time.
    accessModuleService.updateCompletionTime(
        user, DbAccessModuleName.RT_COMPLIANCE_TRAINING, new Timestamp(12345));

    // An empty map should be returned when we have no badge information.
    mockGetUserBadgesByBadgeName(ImmutableMap.of());

    user = complianceTrainingService.syncComplianceTrainingStatus();
    assertModuleCompletionEqual(DbAccessModuleName.RT_COMPLIANCE_TRAINING, null);
  }

  @Test
  public void testSyncComplianceTrainingStatus_Moodle_BadgeNotFound() throws ApiException {
    providedWorkbenchConfig.absorb.enabledForNewUsers = false;

    // We should propagate a NOT_FOUND exception from the compliance service.
    when(mockMoodleService.getUserBadgesByBadgeName(USERNAME))
        .thenThrow(new ApiException(HttpStatus.NOT_FOUND.value(), "user not found"));
    assertThrows(
        NotFoundException.class, () -> complianceTrainingService.syncComplianceTrainingStatus());
  }

  @Test
  public void testSyncComplianceTrainingStatus_SkippedForServiceAccount() throws Exception {
    when(userService.isServiceAccount(user)).thenReturn(true);
    providedWorkbenchConfig.auth.serviceAccountApiUsers.add(USERNAME);
    user = complianceTrainingService.syncComplianceTrainingStatus();
    verifyNoInteractions(mockMoodleService);
  }

  @Test
  public void testUseAbsorb_TrueWhenFeatureFlagEnabledAndNoTrainingsCompleted() {
    providedWorkbenchConfig.absorb.enabledForNewUsers = true;
    assertThat(complianceTrainingService.useAbsorb()).isTrue();
  }

  @Test
  public void testUseAbsorb_TrueWhenFeatureFlagEnabledAndAbsorbPreviouslyUsed() throws Exception {
    providedWorkbenchConfig.absorb.enabledForNewUsers = true;
    stubAbsorbOnlyRTComplete(currentInstant());
    user = complianceTrainingService.syncComplianceTrainingStatus();

    assertThat(complianceTrainingService.useAbsorb()).isTrue();
  }

  @Test
  public void testUseAbsorb_FalseWhenFeatureFlagDisabled() {
    providedWorkbenchConfig.absorb.enabledForNewUsers = false;
    assertThat(complianceTrainingService.useAbsorb()).isFalse();
  }

  @Test
  public void testUseAbsorb_FalseWhenMoodlePreviouslyUsed() throws Exception {
    // Feature flag is off.
    providedWorkbenchConfig.absorb.enabledForNewUsers = false;

    // Use Moodle to complete RT training.
    mockGetUserBadgesByBadgeName(
        ImmutableMap.of(BadgeName.REGISTERED_TIER_TRAINING, defaultBadgeDetails()));
    user = complianceTrainingService.syncComplianceTrainingStatus();

    // Turn on feature flag.
    providedWorkbenchConfig.absorb.enabledForNewUsers = true;

    assertThat(complianceTrainingService.useAbsorb()).isFalse();
  }

  @Test
  public void testSyncComplianceTrainingStatus_AbsorbRollbacksLeadToUnexpectedComplianceLapse()
      throws Exception {
    // This test documents undesired behavior. We think it is unlikely to happen and can
    // be fixed as-needed if we roll back Absorb.

    providedWorkbenchConfig.absorb.enabledForNewUsers = true;

    // A user uses Absorb to complete RT training.
    var rtCompletionTime = currentInstant();
    stubAbsorbOnlyRTComplete(rtCompletionTime);
    user = complianceTrainingService.syncComplianceTrainingStatus();
    assertModuleCompletionEqual(
        DbAccessModuleName.RT_COMPLIANCE_TRAINING, Timestamp.from(rtCompletionTime));

    // Time passes.
    tick();

    // We roll back the Absorb feature flag, for some reason.
    providedWorkbenchConfig.absorb.enabledForNewUsers = false;

    // The user uses Moodle to complete CT training.
    var ctCompletionTime = currentTimestamp();
    mockGetUserBadgesByBadgeName(
        ImmutableMap.of(
            BadgeName.CONTROLLED_TIER_TRAINING, defaultBadgeDetails().lastissued(currentSecond())));
    user = complianceTrainingService.syncComplianceTrainingStatus();

    // Desired behavior: The user's RT training persists.
    // Actual behavior: The user's RT training is wiped out.
    assertModuleNotCompleted(DbAccessModuleName.RT_COMPLIANCE_TRAINING);
    assertModuleCompletionEqual(DbAccessModuleName.CT_COMPLIANCE_TRAINING, ctCompletionTime);
  }

  private void assertModuleCompletionEqual(DbAccessModuleName moduleName, Timestamp timestamp) {
    assertThat(getModuleCompletionTime(moduleName)).isEqualTo(timestamp);
  }

  private void assertModuleNotCompleted(DbAccessModuleName moduleName) {
    assertThat(getModuleCompletionTime(moduleName)).isNull();
  }

  private Timestamp getModuleCompletionTime(DbAccessModuleName moduleName) {
    return userAccessModuleDao
        .getByUserAndAccessModule(user, accessModuleDao.findOneByName(moduleName).get())
        .map(DbUserAccessModule::getCompletionTime)
        .orElse(null);
  }

  private BadgeDetailsV2 defaultBadgeDetails() {
    Timestamp start_timestamp = FakeClockConfiguration.NOW;
    return new BadgeDetailsV2()
        .valid(true)
        .lastissued(start_timestamp.toInstant().getEpochSecond() - 100);
  }

  private void mockGetUserBadgesByBadgeName(Map<BadgeName, BadgeDetailsV2> userBadgesByName)
      throws ApiException {
    when(mockMoodleService.getUserBadgesByBadgeName(USERNAME)).thenReturn(userBadgesByName);
  }

  private void stubAbsorbNoCoursesComplete() throws org.pmiops.workbench.absorb.ApiException {
    when(mockAbsorbService.userHasLoggedIntoAbsorb(FAKE_CREDENTIALS)).thenReturn(true);
    when(mockAbsorbService.getActiveEnrollmentsForUser(FAKE_CREDENTIALS))
        .thenReturn(List.of(new Enrollment(RT_ABSORB_COURSE_ID, null)));
  }

  private void stubAbsorbOnlyRTComplete(Instant rtCompletionTime)
      throws org.pmiops.workbench.absorb.ApiException {
    when(mockAbsorbService.userHasLoggedIntoAbsorb(FAKE_CREDENTIALS)).thenReturn(true);
    when(mockAbsorbService.getActiveEnrollmentsForUser(FAKE_CREDENTIALS))
        .thenReturn(
            List.of(
                new Enrollment(RT_ABSORB_COURSE_ID, rtCompletionTime),
                new Enrollment(CT_ABSORB_COURSE_ID, null)));
  }

  private void stubAbsorbAllTrainingsComplete(Instant rtCompletionTime, Instant ctCompletionTime)
      throws org.pmiops.workbench.absorb.ApiException {
    when(mockAbsorbService.userHasLoggedIntoAbsorb(FAKE_CREDENTIALS)).thenReturn(true);
    when(mockAbsorbService.getActiveEnrollmentsForUser(FAKE_CREDENTIALS))
        .thenReturn(
            List.of(
                new Enrollment(RT_ABSORB_COURSE_ID, rtCompletionTime),
                new Enrollment(CT_ABSORB_COURSE_ID, ctCompletionTime)));
  }

  private Optional<DbComplianceTrainingVerification> getVerification(
      DbAccessModuleName moduleName) {
    return accessModuleDao
        .findOneByName(moduleName)
        .flatMap(am -> userAccessModuleDao.getByUserAndAccessModule(user, am))
        .flatMap(uam -> complianceTrainingVerificationDao.getByUserAccessModule(uam));
  }

  private Instant currentInstant() {
    return fakeClock.instant();
  }

  private long currentSecond() {
    return currentInstant().getEpochSecond();
  }

  private Timestamp currentTimestamp() {
    return Timestamp.from(currentInstant());
  }

  private void tick() {
    fakeClock.increment(1000); // The time increment is arbitrary
  }
}
