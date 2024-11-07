package org.pmiops.workbench.compliancetraining;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.absorb.AbsorbService;
import org.pmiops.workbench.absorb.ApiException;
import org.pmiops.workbench.absorb.Credentials;
import org.pmiops.workbench.absorb.Enrollment;
import org.pmiops.workbench.access.AccessModuleNameMapperImpl;
import org.pmiops.workbench.access.AccessModuleService;
import org.pmiops.workbench.access.AccessModuleServiceImpl;
import org.pmiops.workbench.access.AccessSyncServiceImpl;
import org.pmiops.workbench.access.AccessTierServiceImpl;
import org.pmiops.workbench.access.UserAccessModuleMapperImpl;
import org.pmiops.workbench.actionaudit.auditors.UserServiceAuditor;
import org.pmiops.workbench.billing.FreeTierBillingService;
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
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.institution.InstitutionService;
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
import org.springframework.test.annotation.DirtiesContext;

@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class ComplianceTrainingServiceTest {

  private static final String USERNAME = "abc@fake-research-aou.org";

  private static final Credentials FAKE_CREDENTIALS = new Credentials("fake", "fake", "fake");

  private static DbUser user;
  private static WorkbenchConfig providedWorkbenchConfig;
  private static List<DbAccessModule> accessModules;

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
    ComplianceTrainingServiceImpl.class
  })
  @MockBean({
    FireCloudService.class,
    InstitutionService.class,
    UserServiceAuditor.class,
    FreeTierBillingService.class
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
      return user;
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public List<DbAccessModule> getDbAccessModules() {
      return accessModules;
    }
  }

  @BeforeEach
  public void setUp() throws ApiException {
    user = PresetData.createDbUser();
    user.setUsername(USERNAME);
    when(userService.isServiceAccount(user)).thenReturn(false);
    user = userDao.save(user);

    providedWorkbenchConfig = WorkbenchConfig.createEmptyConfig();
    providedWorkbenchConfig.access.renewal.expiryDays = 365L;
    providedWorkbenchConfig.access.enableComplianceTraining = true;
    providedWorkbenchConfig.billing.initialCreditsValidityPeriodDays = 57L;

    accessModules = TestMockFactory.createAccessModules(accessModuleDao);

    when(mockAbsorbService.fetchCredentials(USERNAME)).thenReturn(FAKE_CREDENTIALS);
  }

  @Test
  public void testSyncComplianceTrainingStatus() throws Exception {
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
  public void testSyncComplianceTrainingStatus_renew() throws Exception {
    // User has completed RT and CT training in Absorb
    var rtOriginalCompletionTime = currentInstant().minusSeconds(2000);
    var ctOriginalCompletionTime = currentInstant();
    stubAbsorbAllTrainingsComplete(rtOriginalCompletionTime, ctOriginalCompletionTime);

    // User syncs training
    user = complianceTrainingService.syncComplianceTrainingStatus();

    // Verify database updates for RT and CT training completion
    assertModuleCompletionEqual(
        DbAccessModuleName.RT_COMPLIANCE_TRAINING, Timestamp.from(rtOriginalCompletionTime));
    assertModuleCompletionEqual(
        DbAccessModuleName.CT_COMPLIANCE_TRAINING, Timestamp.from(ctOriginalCompletionTime));

    // Verify Absorb verification records for RT and CT
    assertAbsorbVerificationSystem(DbAccessModuleName.RT_COMPLIANCE_TRAINING);
    assertAbsorbVerificationSystem(DbAccessModuleName.CT_COMPLIANCE_TRAINING);

    // Time passes: 1 year, user Trainings are now expired
    tick_NextYear();

    // User is re-enrolled to RT training in Absorb:
    // This means the RT Course enrollment completion date is null, while CT remains as is
    stubAbsorbRTExpiredCTComplete(ctOriginalCompletionTime);

    // User syncs training
    user = complianceTrainingService.syncComplianceTrainingStatus();

    // Verify there is no change in DB if RT completion time is returned null from Absorb.
    assertModuleCompletionEqual(
        DbAccessModuleName.RT_COMPLIANCE_TRAINING, Timestamp.from(rtOriginalCompletionTime));
    assertModuleCompletionEqual(
        DbAccessModuleName.CT_COMPLIANCE_TRAINING, Timestamp.from(ctOriginalCompletionTime));

    // Verify Absorb verification records still exist
    assertAbsorbVerificationSystem(DbAccessModuleName.RT_COMPLIANCE_TRAINING);
    assertAbsorbVerificationSystem(DbAccessModuleName.CT_COMPLIANCE_TRAINING);

    Instant currentRt = currentInstant();
    // User now finishes RT training in Absorb
    stubAbsorbAllTrainingsComplete(currentRt, ctOriginalCompletionTime);

    // User syncs training
    user = complianceTrainingService.syncComplianceTrainingStatus();

    // Verify RT training completion time is updated
    assertModuleCompletionEqual(
        DbAccessModuleName.RT_COMPLIANCE_TRAINING, Timestamp.from(currentRt));
    assertModuleCompletionEqual(
        DbAccessModuleName.CT_COMPLIANCE_TRAINING, Timestamp.from(ctOriginalCompletionTime));

    // Verify Absorb verification records still exist
    assertAbsorbVerificationSystem(DbAccessModuleName.RT_COMPLIANCE_TRAINING);
    assertAbsorbVerificationSystem(DbAccessModuleName.CT_COMPLIANCE_TRAINING);
  }

  private void assertAbsorbVerificationSystem(DbAccessModuleName moduleName) {
    assertThat(getVerification(moduleName).get().getComplianceTrainingVerificationSystem())
        .isEqualTo(DbComplianceTrainingVerification.DbComplianceTrainingVerificationSystem.ABSORB);
  }

  @Test
  public void testSyncComplianceTrainingStatus_Initial_Moodle_ResyncUsingAbsorbCausesNoChanges()
      throws Exception {
    mockRTTrainingCompletedWithMoodle();

    // Set up: The user completes and syncs CT training
    mockCTTrainingCompletedWithMoodle(null);
    var completionTime = currentTimestamp();
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

    // Time passes and the user re-syncs
    tick();
    stubAbsorbAllTrainingsComplete(completionTime.toInstant(), completionTime.toInstant());

    // Compliance Training information will come from Absorb now
    user = complianceTrainingService.syncComplianceTrainingStatus();

    // Completion timestamp should not change when the method is called again.
    assertModuleCompletionEqual(DbAccessModuleName.CT_COMPLIANCE_TRAINING, completionTime);
    // Database will show the user is now using Absorb
    assertThat(
            getVerification(DbAccessModuleName.CT_COMPLIANCE_TRAINING)
                .get()
                .getComplianceTrainingVerificationSystem())
        .isEqualTo(DbComplianceTrainingVerification.DbComplianceTrainingVerificationSystem.ABSORB);
  }

  @Test
  public void testSyncComplianceTrainingStatus_ResyncCausesNoChanges() throws Exception {
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
  public void testSyncComplianceTrainingStatus_DoesNothingIfUserHasntSignedIntoAbsorb()
      throws Exception {
    when(mockAbsorbService.userHasLoggedIntoAbsorb(FAKE_CREDENTIALS)).thenReturn(false);

    user = complianceTrainingService.syncComplianceTrainingStatus();

    assertModuleNotCompleted(DbAccessModuleName.RT_COMPLIANCE_TRAINING);
    assertModuleNotCompleted(DbAccessModuleName.CT_COMPLIANCE_TRAINING);
  }

  @Test
  public void testSyncComplianceTrainingStatus_DoesNothingIfNoCoursesComplete() throws Exception {
    stubAbsorbNoCoursesComplete();

    user = complianceTrainingService.syncComplianceTrainingStatus();

    assertModuleNotCompleted(DbAccessModuleName.RT_COMPLIANCE_TRAINING);
    assertModuleNotCompleted(DbAccessModuleName.CT_COMPLIANCE_TRAINING);
  }

  @Test
  public void testSyncComplianceTrainingStatus_Moodle_RenewsExpiredTrainingUsingAbsorb()
      throws Exception {
    // User completes RT training
    mockRTTrainingCompletedWithMoodle();

    // Time passes
    tick();

    // Mock moodle with completion date as today - 1 year
    var aYearAgo = currentInstant().minusSeconds(31556952L);
    // User completes CT training
    mockCTTrainingCompletedWithMoodle(Timestamp.from(aYearAgo));

    // Time passes
    tick();

    assertThat(
            getVerification(DbAccessModuleName.CT_COMPLIANCE_TRAINING)
                .get()
                .getComplianceTrainingVerificationSystem())
        .isEqualTo(DbComplianceTrainingVerification.DbComplianceTrainingVerificationSystem.MOODLE);

    // The user should be updated in the database with a non-empty completion time.
    assertModuleCompletionEqual(
        DbAccessModuleName.CT_COMPLIANCE_TRAINING, Timestamp.from(aYearAgo));

    var absorbCtCompletionTime = currentInstant().plusSeconds(3000);
    var absorbRTCompletionTime = currentTimestamp().toInstant();
    // Stub absorb training
    stubAbsorbAllTrainingsComplete(absorbRTCompletionTime, absorbCtCompletionTime);

    // Time passes, user syncs training using Absorb
    tick();
    user = complianceTrainingService.syncComplianceTrainingStatus();

    // Completion and expiry timestamp should be updated.
    assertModuleCompletionEqual(
        DbAccessModuleName.RT_COMPLIANCE_TRAINING, Timestamp.from(absorbRTCompletionTime));
    assertModuleCompletionEqual(
        DbAccessModuleName.CT_COMPLIANCE_TRAINING, Timestamp.from(absorbCtCompletionTime));

    // Database should now show that source of training is Absorb
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
  public void testSyncComplianceTrainingStatus_SkippedForServiceAccount() throws Exception {
    when(userService.isServiceAccount(user)).thenReturn(true);
    providedWorkbenchConfig.auth.serviceAccountApiUsers.add(USERNAME);
    user = complianceTrainingService.syncComplianceTrainingStatus();
    verifyNoInteractions(mockAbsorbService);
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

  private void stubAbsorbNoCoursesComplete() throws ApiException {
    when(mockAbsorbService.userHasLoggedIntoAbsorb(FAKE_CREDENTIALS)).thenReturn(true);
    when(mockAbsorbService.getActiveEnrollmentsForUser(FAKE_CREDENTIALS))
        .thenReturn(
            List.of(new Enrollment(ComplianceTrainingServiceImpl.rtTrainingCourseId, null)));
  }

  private void stubAbsorbOnlyRTComplete(Instant rtCompletionTime) throws ApiException {
    when(mockAbsorbService.userHasLoggedIntoAbsorb(FAKE_CREDENTIALS)).thenReturn(true);
    when(mockAbsorbService.getActiveEnrollmentsForUser(FAKE_CREDENTIALS))
        .thenReturn(
            List.of(
                new Enrollment(ComplianceTrainingServiceImpl.rtTrainingCourseId, rtCompletionTime),
                new Enrollment(ComplianceTrainingServiceImpl.ctTrainingCourseId, null)));
  }

  private void stubAbsorbRTExpiredCTComplete(Instant ctCompletionTime) throws ApiException {
    when(mockAbsorbService.userHasLoggedIntoAbsorb(FAKE_CREDENTIALS)).thenReturn(true);
    when(mockAbsorbService.getActiveEnrollmentsForUser(FAKE_CREDENTIALS))
        .thenReturn(
            List.of(
                new Enrollment(ComplianceTrainingServiceImpl.rtTrainingCourseId, null),
                new Enrollment(
                    ComplianceTrainingServiceImpl.ctTrainingCourseId, ctCompletionTime)));
  }

  private void stubAbsorbAllTrainingsComplete(Instant rtCompletionTime, Instant ctCompletionTime)
      throws ApiException {
    when(mockAbsorbService.userHasLoggedIntoAbsorb(FAKE_CREDENTIALS)).thenReturn(true);
    when(mockAbsorbService.getActiveEnrollmentsForUser(FAKE_CREDENTIALS))
        .thenReturn(
            List.of(
                new Enrollment(ComplianceTrainingServiceImpl.rtTrainingCourseId, rtCompletionTime),
                new Enrollment(
                    ComplianceTrainingServiceImpl.ctTrainingCourseId, ctCompletionTime)));
  }

  private Optional<DbComplianceTrainingVerification> getVerification(
      DbAccessModuleName moduleName) {
    return accessModuleDao
        .findOneByName(moduleName)
        .flatMap(am -> userAccessModuleDao.getByUserAndAccessModule(user, am))
        .flatMap(uam -> complianceTrainingVerificationDao.getByUserAccessModule(uam));
  }

  private void mockRTTrainingCompletedWithMoodle() {
    // Update database to mark user's training source as MOODLE.
    var uam =
        accessModuleService.updateCompletionTime(
            user, DbAccessModuleName.RT_COMPLIANCE_TRAINING, currentTimestamp());
    complianceTrainingVerificationDao.save(
        new DbComplianceTrainingVerification()
            .setUserAccessModule(uam)
            .setComplianceTrainingVerificationSystem(
                DbComplianceTrainingVerification.DbComplianceTrainingVerificationSystem.MOODLE));
  }

  private void mockCTTrainingCompletedWithMoodle(Timestamp timestamp) {
    // Update database to mark user's training source as MOODLE.
    var uam =
        accessModuleService.updateCompletionTime(
            user,
            DbAccessModuleName.CT_COMPLIANCE_TRAINING,
            timestamp != null ? timestamp : currentTimestamp());
    complianceTrainingVerificationDao.save(
        new DbComplianceTrainingVerification()
            .setUserAccessModule(uam)
            .setComplianceTrainingVerificationSystem(
                DbComplianceTrainingVerification.DbComplianceTrainingVerificationSystem.MOODLE));
  }

  private Instant currentInstant() {
    return fakeClock.instant();
  }

  private Timestamp currentTimestamp() {
    return Timestamp.from(currentInstant());
  }

  private void tick() {
    fakeClock.increment(1000); // The time increment is arbitrary
  }

  private void tick_NextYear() {
    Instant nextYearInstant = fakeClock.instant().plus(365, ChronoUnit.DAYS);
    fakeClock.setInstant(nextYearInstant);
  }
}
