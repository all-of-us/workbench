package org.pmiops.workbench.db.dao;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.actionaudit.auditors.UserServiceAuditor;
import org.pmiops.workbench.actionaudit.targetproperties.BypassTimeTargetProperty;
import org.pmiops.workbench.compliance.ComplianceService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudNihStatus;
import org.pmiops.workbench.google.DirectoryService;
import org.pmiops.workbench.moodle.ApiException;
import org.pmiops.workbench.moodle.model.BadgeDetails;
import org.pmiops.workbench.moodle.model.BadgeDetailsDeprecated;
import org.pmiops.workbench.test.FakeClock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class UserServiceTest {

  private static final String USERNAME = "abc@fake-research-aou.org";
  private static final int MOODLE_ID = 1001;

  // An arbitrary timestamp to use as the anchor time for access module test cases.
  private static final Instant START_INSTANT = Instant.parse("2000-01-01T00:00:00.00Z");
  private static final long TIMESTAMP_MSECS = START_INSTANT.toEpochMilli();
  private static final FakeClock PROVIDED_CLOCK = new FakeClock(START_INSTANT);
  private static final int CLOCK_INCREMENT_MILLIS = 1000;
  private static DbUser providedDbUser;
  private static WorkbenchConfig providedWorkbenchConfig;

  @Autowired private FireCloudService mockFireCloudService;
  @Autowired private ComplianceService mockComplianceService;
  @Autowired private DirectoryService mockDirectoryService;
  @Autowired private UserServiceAuditor mockUserServiceAuditAdapter;

  @Autowired private UserService userService;
  @Autowired private UserDao userDao;

  @TestConfiguration
  @Import({UserServiceImpl.class})
  @MockBean({
    AdminActionHistoryDao.class,
    FireCloudService.class,
    ComplianceService.class,
    DirectoryService.class,
    UserServiceAuditor.class
  })
  static class Configuration {
    @Bean
    Clock clock() {
      return PROVIDED_CLOCK;
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    WorkbenchConfig getWorkbenchConfig() {
      return providedWorkbenchConfig;
    }

    @Bean
    Random getRandom() {
      return new Random();
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    DbUser getDbUser() {
      return providedDbUser;
    }
  }

  @Before
  public void setUp() {
    DbUser user = new DbUser();
    user.setUsername(USERNAME);
    userDao.save(user);
    providedDbUser = user;

    providedWorkbenchConfig = WorkbenchConfig.createEmptyConfig();

    // Since we're injecting the same static instance of this FakeClock,
    // increments and other mutations will carry across tests if we don't reset it here.
    // I tried easier ways of ensuring this, like creating new instances for every test run,
    // but it was injecting null clocks giving NPEs long after construction, and so far this
    // is the only working approach I've seen.
    PROVIDED_CLOCK.setInstant(START_INSTANT);
  }

  @Test
  public void testSyncComplianceTrainingStatusDeprecated() throws Exception {
    providedWorkbenchConfig.featureFlags.enableMoodleV2Api = false;

    BadgeDetailsDeprecated badge = new BadgeDetailsDeprecated();
    badge.setName("All of us badge");
    long expiry = PROVIDED_CLOCK.instant().toEpochMilli() + 100000;
    badge.setDateexpire(Long.toString(expiry));

    when(mockComplianceService.getMoodleId(USERNAME)).thenReturn(MOODLE_ID);
    when(mockComplianceService.getUserBadge(MOODLE_ID))
        .thenReturn(Collections.singletonList(badge));

    userService.syncComplianceTrainingStatus();

    // The user should be updated in the database with a non-empty completion and expiration time.
    DbUser user = userDao.findUserByUsername(USERNAME);
    assertThat(user.getComplianceTrainingCompletionTime())
        .isEqualTo(new Timestamp(TIMESTAMP_MSECS));
    assertThat(user.getComplianceTrainingExpirationTime())
        .isEqualTo(Timestamp.from(Instant.ofEpochMilli(expiry)));

    // Completion timestamp should not change when the method is called again.
    tick();
    Timestamp completionTime = user.getComplianceTrainingCompletionTime();
    userService.syncComplianceTrainingStatus();
    assertThat(user.getComplianceTrainingCompletionTime()).isEqualTo(completionTime);
  }

  @Test
  public void testSyncComplianceTrainingStatus() throws Exception {
    providedWorkbenchConfig.featureFlags.enableMoodleV2Api = true;

    BadgeDetails retBadge = new BadgeDetails();
    long expiry = PROVIDED_CLOCK.instant().toEpochMilli() + 100000;
    retBadge.setDateexpire(Long.toString(expiry));

    Map<String, BadgeDetails> userBadgesByName = new HashMap<String, BadgeDetails>();
    userBadgesByName.put(mockComplianceService.getResearchEthicsTrainingField(), retBadge);

    when(mockComplianceService.getUserBadgesByName(USERNAME)).thenReturn(userBadgesByName);

    userService.syncComplianceTrainingStatus();

    // The user should be updated in the database with a non-empty completion and expiration time.
    DbUser user = userDao.findUserByUsername(USERNAME);
    assertThat(user.getComplianceTrainingCompletionTime())
        .isEqualTo(new Timestamp(TIMESTAMP_MSECS));
    assertThat(user.getComplianceTrainingExpirationTime())
        .isEqualTo(Timestamp.from(Instant.ofEpochMilli(expiry)));

    // Completion timestamp should not change when the method is called again.
    tick();
    Timestamp completionTime = user.getComplianceTrainingCompletionTime();
    userService.syncComplianceTrainingStatus();
    assertThat(user.getComplianceTrainingCompletionTime()).isEqualTo(completionTime);
  }

  @Test
  public void testUpdateComplianceTrainingStatus() throws Exception {
    providedWorkbenchConfig.featureFlags.enableMoodleV2Api = true;

    BadgeDetails retBadge = new BadgeDetails();
    long expiry = PROVIDED_CLOCK.instant().toEpochMilli();
    retBadge.setDateexpire(Long.toString(expiry));
    retBadge.setLastissued(Long.toString(expiry - 100000));
    retBadge.setValid(true);

    Map<String, BadgeDetails> userBadgesByName = new HashMap<String, BadgeDetails>();
    userBadgesByName.put(mockComplianceService.getResearchEthicsTrainingField(), retBadge);

    when(mockComplianceService.getUserBadgesByName(USERNAME)).thenReturn(userBadgesByName);

    userService.syncComplianceTrainingStatus();

    // The user should be updated in the database with a non-empty completion and expiration time.
    DbUser user = userDao.findUserByUsername(USERNAME);
    assertThat(user.getComplianceTrainingCompletionTime())
        .isEqualTo(new Timestamp(TIMESTAMP_MSECS));
    assertThat(user.getComplianceTrainingExpirationTime())
        .isEqualTo(Timestamp.from(Instant.ofEpochMilli(expiry)));

    // Deprecate the old training.
    long newExpiry = expiry - 1000;
    retBadge.setDateexpire(Long.toString(newExpiry));
    retBadge.setValid(false);

    // Completion timestamp should be wiped out by the expiry timestamp passing.
    userService.syncComplianceTrainingStatus();
    assertThat(user.getComplianceTrainingCompletionTime()).isNull();

    // The user does a new training.
    long newerExpiry = expiry + 1000;
    retBadge.setDateexpire(Long.toString(newerExpiry));
    retBadge.setValid(true);

    // Completion and expiry timestamp should be updated.
    userService.syncComplianceTrainingStatus();
    assertThat(user.getComplianceTrainingCompletionTime())
        .isEqualTo(new Timestamp(TIMESTAMP_MSECS));
    assertThat(user.getComplianceTrainingExpirationTime())
        .isEqualTo(Timestamp.from(Instant.ofEpochMilli(newerExpiry)));

    // A global expiration is set.
    long globalExpiry = expiry - 1000;
    retBadge.setGlobalexpiration(Long.toString(globalExpiry));
    retBadge.setValid(false);

    // Completion timestamp should be wiped out by the globalexpiry timestamp passing.
    userService.syncComplianceTrainingStatus();
    assertThat(user.getComplianceTrainingCompletionTime()).isNull();
  }

  private void tick() {
    PROVIDED_CLOCK.increment(CLOCK_INCREMENT_MILLIS);
  }

  @Test
  public void testSyncComplianceTrainingStatusNoMoodleId() throws Exception {
    when(mockComplianceService.getMoodleId(USERNAME)).thenReturn(null);
    userService.syncComplianceTrainingStatus();

    verify(mockComplianceService, never()).getUserBadge(anyInt());
    DbUser user = userDao.findUserByUsername(USERNAME);
    assertThat(user.getComplianceTrainingCompletionTime()).isNull();
  }

  @Test
  public void testSyncComplianceTrainingStatusNullBadge() throws ApiException {
    // When Moodle returns an empty badge response, we should clear the completion bit.
    DbUser user = userDao.findUserByUsername(USERNAME);
    user.setComplianceTrainingCompletionTime(new Timestamp(12345));
    userDao.save(user);

    when(mockComplianceService.getMoodleId(USERNAME)).thenReturn(1);
    when(mockComplianceService.getUserBadge(1)).thenReturn(null);
    userService.syncComplianceTrainingStatus();
    user = userDao.findUserByUsername(USERNAME);
    assertThat(user.getComplianceTrainingCompletionTime()).isNull();
  }

  @Test(expected = NotFoundException.class)
  public void testSyncComplianceTrainingStatusBadgeNotFound() throws ApiException {
    // We should propagate a NOT_FOUND exception from the compliance service.
    when(mockComplianceService.getMoodleId(USERNAME)).thenReturn(MOODLE_ID);
    when(mockComplianceService.getUserBadge(MOODLE_ID))
        .thenThrow(
            new org.pmiops.workbench.moodle.ApiException(
                HttpStatus.NOT_FOUND.value(), "user not found"));
    userService.syncComplianceTrainingStatus();
  }

  @Test
  public void testSyncComplianceTraining_SkippedForServiceAccount() throws ApiException {
    providedWorkbenchConfig.auth.serviceAccountApiUsers.add(USERNAME);
    userService.syncComplianceTrainingStatus();
    assertThat(providedDbUser.getMoodleId()).isNull();
  }

  @Test
  public void testSyncEraCommonsStatus() {
    FirecloudNihStatus nihStatus = new FirecloudNihStatus();
    nihStatus.setLinkedNihUsername("nih-user");
    // FireCloud stores the NIH status in seconds, not msecs.
    final long FC_LINK_EXPIRATION_SECONDS = START_INSTANT.toEpochMilli() / 1000;
    nihStatus.setLinkExpireTime(FC_LINK_EXPIRATION_SECONDS);

    when(mockFireCloudService.getNihStatus()).thenReturn(nihStatus);

    userService.syncEraCommonsStatus();

    DbUser user = userDao.findUserByUsername(USERNAME);
    assertThat(user.getEraCommonsCompletionTime()).isEqualTo(Timestamp.from(START_INSTANT));
    assertThat(user.getEraCommonsLinkExpireTime()).isEqualTo(Timestamp.from(START_INSTANT));
    assertThat(user.getEraCommonsLinkedNihUsername()).isEqualTo("nih-user");

    // Completion timestamp should not change when the method is called again.
    tick();
    Timestamp completionTime = user.getEraCommonsCompletionTime();
    userService.syncEraCommonsStatus();
    assertThat(user.getEraCommonsCompletionTime()).isEqualTo(completionTime);
  }

  @Test
  public void testClearsEraCommonsStatus() {
    DbUser testUser = userDao.findUserByUsername(USERNAME);
    // Put the test user in a state where eRA commons is completed.
    testUser.setEraCommonsCompletionTime(new Timestamp(TIMESTAMP_MSECS));
    testUser.setEraCommonsLinkedNihUsername("nih-user");

    //noinspection UnusedAssignment
    testUser = userDao.save(testUser);

    userService.syncEraCommonsStatus();

    DbUser retrievedUser = userDao.findUserByUsername(USERNAME);
    assertThat(retrievedUser.getEraCommonsCompletionTime()).isNull();
  }

  @Test
  public void testSyncTwoFactorAuthStatus() {
    com.google.api.services.directory.model.User googleUser =
        new com.google.api.services.directory.model.User();
    googleUser.setPrimaryEmail(USERNAME);
    googleUser.setIsEnrolledIn2Sv(true);

    when(mockDirectoryService.getUser(USERNAME)).thenReturn(googleUser);
    userService.syncTwoFactorAuthStatus();
    // twoFactorAuthCompletionTime should now be set
    DbUser user = userDao.findUserByUsername(USERNAME);
    assertThat(user.getTwoFactorAuthCompletionTime()).isNotNull();

    // twoFactorAuthCompletionTime should not change when already set
    tick();
    Timestamp twoFactorAuthCompletionTime = user.getTwoFactorAuthCompletionTime();
    userService.syncTwoFactorAuthStatus();
    user = userDao.findUserByUsername(USERNAME);
    assertThat(user.getTwoFactorAuthCompletionTime()).isEqualTo(twoFactorAuthCompletionTime);

    // unset 2FA in google and check that twoFactorAuthCompletionTime is set to null
    googleUser.setIsEnrolledIn2Sv(false);
    userService.syncTwoFactorAuthStatus();
    user = userDao.findUserByUsername(USERNAME);
    assertThat(user.getTwoFactorAuthCompletionTime()).isNull();
  }

  @Test
  public void testSetBypassTimes() {
    DbUser dbUser = userDao.findUserByUsername(USERNAME);

    // Make sure we're starting with a clean slate before doing the operations and assertions
    // below. This both a sanity check against future changes to the test user initialization
    // logic that could accidentally render one of the assertions moot as well as executable
    // documentation that these fields are expected to be null by default.
    assertThat(dbUser.getDataUseAgreementBypassTime()).isNull();
    assertThat(dbUser.getComplianceTrainingBypassTime()).isNull();
    assertThat(dbUser.getBetaAccessBypassTime()).isNull();
    assertThat(dbUser.getEraCommonsBypassTime()).isNull();
    assertThat(dbUser.getTwoFactorAuthBypassTime()).isNull();

    final Timestamp duaBypassTime = Timestamp.from(Instant.parse("2000-01-01T00:00:00.00Z"));
    userService.setDataUseAgreementBypassTime(dbUser.getUserId(), duaBypassTime);
    verify(mockUserServiceAuditAdapter)
        .fireAdministrativeBypassTime(
            dbUser.getUserId(),
            BypassTimeTargetProperty.DATA_USE_AGREEMENT_BYPASS_TIME,
            nullableTimestampToOptionalInstant(duaBypassTime));
    assertThat(dbUser.getDataUseAgreementBypassTime()).isEqualTo(duaBypassTime);

    userService.setDataUseAgreementBypassTime(dbUser.getUserId(), null);
    verify(mockUserServiceAuditAdapter)
        .fireAdministrativeBypassTime(
            dbUser.getUserId(),
            BypassTimeTargetProperty.DATA_USE_AGREEMENT_BYPASS_TIME,
            Optional.empty());
    assertThat(dbUser.getDataUseAgreementBypassTime()).isNull();

    final Timestamp complianceTrainingBypassTime =
        Timestamp.from(Instant.parse("2001-01-01T00:00:00.00Z"));
    userService.setComplianceTrainingBypassTime(dbUser.getUserId(), complianceTrainingBypassTime);
    verify(mockUserServiceAuditAdapter)
        .fireAdministrativeBypassTime(
            dbUser.getUserId(),
            BypassTimeTargetProperty.COMPLIANCE_TRAINING_BYPASS_TIME,
            nullableTimestampToOptionalInstant(complianceTrainingBypassTime));
    assertThat(dbUser.getComplianceTrainingBypassTime()).isEqualTo(complianceTrainingBypassTime);

    final Timestamp betaAccessBypassTime = Timestamp.from(Instant.parse("2002-01-01T00:00:00.00Z"));
    userService.setBetaAccessBypassTime(dbUser.getUserId(), betaAccessBypassTime);
    verify(mockUserServiceAuditAdapter)
        .fireAdministrativeBypassTime(
            dbUser.getUserId(),
            BypassTimeTargetProperty.BETA_ACCESS_BYPASS_TIME,
            nullableTimestampToOptionalInstant(betaAccessBypassTime));
    assertThat(dbUser.getBetaAccessBypassTime()).isEqualTo(betaAccessBypassTime);

    final Timestamp eraCommonsBypassTime = Timestamp.from(Instant.parse("2003-01-01T00:00:00.00Z"));
    userService.setEraCommonsBypassTime(dbUser.getUserId(), eraCommonsBypassTime);
    verify(mockUserServiceAuditAdapter)
        .fireAdministrativeBypassTime(
            dbUser.getUserId(),
            BypassTimeTargetProperty.ERA_COMMONS_BYPASS_TIME,
            nullableTimestampToOptionalInstant(eraCommonsBypassTime));
    assertThat(dbUser.getEraCommonsBypassTime()).isEqualTo(eraCommonsBypassTime);

    final Timestamp twoFactorBypassTime = Timestamp.from(Instant.parse("2004-01-01T00:00:00.00Z"));
    userService.setTwoFactorAuthBypassTime(dbUser.getUserId(), twoFactorBypassTime);
    verify(mockUserServiceAuditAdapter)
        .fireAdministrativeBypassTime(
            dbUser.getUserId(),
            BypassTimeTargetProperty.TWO_FACTOR_AUTH_BYPASS_TIME,
            nullableTimestampToOptionalInstant(twoFactorBypassTime));
    assertThat(dbUser.getTwoFactorAuthBypassTime()).isEqualTo(twoFactorBypassTime);
  }

  private Optional<Instant> nullableTimestampToOptionalInstant(
      @Nullable Timestamp complianceTrainingBypassTime) {
    return Optional.ofNullable(complianceTrainingBypassTime).map(Timestamp::toInstant);
  }
}
