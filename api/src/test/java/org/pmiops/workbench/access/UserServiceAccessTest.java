package org.pmiops.workbench.access;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import com.google.common.collect.ImmutableList;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Function;
import javax.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.actionaudit.Agent;
import org.pmiops.workbench.actionaudit.auditors.UserServiceAuditor;
import org.pmiops.workbench.compliance.ComplianceService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.AccessTierDao;
import org.pmiops.workbench.db.dao.UserAccessTierDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.dao.UserServiceImpl;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserAccessTier;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.google.DirectoryService;
import org.pmiops.workbench.mail.MailService;
import org.pmiops.workbench.model.TierAccessStatus;
import org.pmiops.workbench.model.UserAccessExpiration;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.testconfig.UserServiceTestConfiguration;
import org.pmiops.workbench.utils.TestMockFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Tests to cover access change determinations by executing {@link
 * UserService#updateUserWithRetries(java.util.function.Function,
 * org.pmiops.workbench.db.model.DbUser, org.pmiops.workbench.actionaudit.Agent)} with different
 * configurations, which ultimately executes the private method {@link
 * UserServiceImpl#shouldUserBeRegistered(org.pmiops.workbench.db.model.DbUser)} to make this
 * determination.
 */
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class UserServiceAccessTest {
  private static final String USERNAME = "abc@fake-research-aou.org";
  private static final Instant START_INSTANT = Instant.parse("2030-01-01T00:00:00.00Z");
  private static final FakeClock PROVIDED_CLOCK = new FakeClock(START_INSTANT);
  private static final long EXPIRATION_DAYS = 365L;

  private static DbUser dbUser;
  private static WorkbenchConfig providedWorkbenchConfig;

  private static DbAccessTier registeredTier;

  private Function<Timestamp, Function<DbUser, DbUser>> registerUserWithTime =
      t -> dbu -> registerUser(t, dbu);
  private Function<DbUser, DbUser> registerUserNow;

  @Autowired private AccessTierDao accessTierDao;
  @Autowired private UserAccessTierDao userAccessTierDao;
  @Autowired private UserDao userDao;
  @Autowired private UserService userService;

  @MockBean private MailService mailService;

  @Import({
    UserServiceTestConfiguration.class,
    AccessTierServiceImpl.class,
  })
  @MockBean({
    ComplianceService.class,
    DirectoryService.class,
    FireCloudService.class,
    MailService.class,
    UserServiceAuditor.class,
  })
  @TestConfiguration
  static class Configuration {
    @Bean
    Clock clock() {
      return PROVIDED_CLOCK;
    }

    @Bean
    Random getRandom() {
      return new Random();
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    WorkbenchConfig getWorkbenchConfig() {
      return providedWorkbenchConfig;
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    DbUser getDbUser() {
      return dbUser;
    }
  }

  @BeforeEach
  public void setUp() {
    providedWorkbenchConfig = WorkbenchConfig.createEmptyConfig();
    providedWorkbenchConfig.access.enableAccessRenewal = true;
    providedWorkbenchConfig.access.enableComplianceTraining = true;
    providedWorkbenchConfig.access.enableEraCommons = true;
    providedWorkbenchConfig.accessRenewal.expiryDays = EXPIRATION_DAYS;
    providedWorkbenchConfig.accessRenewal.expiryDaysWarningThresholds =
        ImmutableList.of(1L, 3L, 7L, 15L, 30L);

    registeredTier = TestMockFactory.createRegisteredTierForTests(accessTierDao);

    dbUser = new DbUser();
    dbUser.setUsername(USERNAME);
    dbUser = userDao.save(dbUser);

    // reset the clock so tests changing this don't affect each other
    PROVIDED_CLOCK.setInstant(START_INSTANT);
    registerUserNow = registerUserWithTime.apply(new Timestamp(PROVIDED_CLOCK.millis()));
  }

  @Test
  public void test_updateUserWithRetries_never_registered() {
    assertThat(userAccessTierDao.findAll()).isEmpty();

    dbUser = updateUserWithRetries(Function.identity());

    // the user has never been registered so they have no DbUserAccessTier entry

    assertThat(userAccessTierDao.findAll()).hasSize(0);
    Optional<DbUserAccessTier> userAccessMaybe =
        userAccessTierDao.getByUserAndAccessTier(dbUser, registeredTier);
    assertThat(userAccessMaybe).isEmpty();
  }

  @Test
  public void test_updateUserWithRetries_register() {
    assertThat(userAccessTierDao.findAll()).isEmpty();

    dbUser = updateUserWithRetries(registerUserNow);
    assertRegisteredTierEnabled(dbUser);
  }

  @Test
  public void test_updateUserWithRetries_register_includes_others() {
    providedWorkbenchConfig.featureFlags.unsafeAllowAccessToAllTiersForRegisteredUsers = true;

    DbAccessTier controlledTier = TestMockFactory.createControlledTierForTests(accessTierDao);
    DbAccessTier aThirdTierWhyNot =
        accessTierDao.save(
            new DbAccessTier()
                .setAccessTierId(3)
                .setShortName("three")
                .setDisplayName("Third Tier")
                .setAuthDomainName("Third Tier Auth Domain")
                .setAuthDomainGroupEmail("t3-users@fake-research-aou.org")
                .setServicePerimeter("tier/3/perimeter"));

    assertThat(userAccessTierDao.findAll()).isEmpty();

    dbUser = updateUserWithRetries(registerUserNow);

    List<DbAccessTier> expectedTiers =
        ImmutableList.of(registeredTier, controlledTier, aThirdTierWhyNot);

    assertThat(userAccessTierDao.findAll()).hasSize(expectedTiers.size());
    for (DbAccessTier tier : expectedTiers) {
      Optional<DbUserAccessTier> userAccessMaybe =
          userAccessTierDao.getByUserAndAccessTier(dbUser, tier);
      assertThat(userAccessMaybe).isPresent();
      assertThat(userAccessMaybe.get().getTierAccessStatusEnum())
          .isEqualTo(TierAccessStatus.ENABLED);
    }
  }

  @Test
  public void testSimulateUserFlowThroughRenewal() {
    // initialize user as registered with generic values including bypassed DUA

    dbUser = updateUserWithRetries(registerUserNow);
    assertRegisteredTierEnabled(dbUser);

    // add a proper DUA completion which will expire soon, but remove DUA bypass

    dbUser.setDataUseAgreementSignedVersion(userService.getCurrentDuccVersion());
    dbUser.setDataUseAgreementCompletionTime(willExpireAfter(Duration.ofDays(1)));
    dbUser = updateUserWithRetries(this::removeDuaBypass);

    // User is compliant
    assertRegisteredTierEnabled(dbUser);

    // Simulate time passing, user is no longer compliant
    advanceClockDays(2);
    dbUser = updateUserWithRetries(Function.identity());
    assertRegisteredTierDisabled(dbUser);

    // Simulate user filling out DUA, becoming compliant again
    dbUser =
        updateUserWithRetries(
            user -> {
              user.setDataUseAgreementCompletionTime(new Timestamp(PROVIDED_CLOCK.millis()));
              return user;
            });
    assertRegisteredTierEnabled(dbUser);
  }

  // Ensure that we don't enforce access renewal in environments where the flag is not set:
  // make the user expire in all of the ways possible by access renewal, and test that none
  // of these cause noncompliance.

  @Test
  public void testRenewalFlag() {
    providedWorkbenchConfig.access.enableAccessRenewal = false;

    final Timestamp willExpire = Timestamp.from(START_INSTANT);

    // initialize user as registered, including:
    // bypassed DUA
    // bypassed Compliance training
    // recent confirmedPublications
    // recent confirmedProfile

    dbUser = updateUserWithRetries(registerUserNow);
    assertRegisteredTierEnabled(dbUser);

    // add proper DUA completion and compliance training and remove bypasses

    dbUser =
        updateUserWithRetries(
            user -> {
              user.setDataUseAgreementSignedVersion(userService.getCurrentDuccVersion());
              user.setDataUseAgreementCompletionTime(willExpire);
              user.setDataUseAgreementBypassTime(null);

              user.setComplianceTrainingCompletionTime(willExpire);
              user.setComplianceTrainingBypassTime(null);
              return user;
            });

    // This is just a switch from bypassed -> user-performed action so we remain compliant
    // (and still would be so, with enableAccessRenewal = true)
    assertRegisteredTierEnabled(dbUser);

    // Time passing beyond the expiration window would cause the user to become
    // noncompliant when enableAccessRenewal = true
    advanceClockDays(EXPIRATION_DAYS + 1);

    dbUser =
        updateUserWithRetries(
            user -> {
              // removing publicationsConfirmed and profileConfirmed would also cause
              // noncompliance when enableAccessRenewal = true
              user.setPublicationsLastConfirmedTime(null);
              user.setProfileLastConfirmedTime(null);
              return user;
            });

    // the user is still compliant because we are not checking for expiration
    assertRegisteredTierEnabled(dbUser);
  }

  private DbUser removeDuaBypass(DbUser user) {
    user.setDataUseAgreementBypassTime(null);
    return userDao.save(user);
  }

  // test that every way for a user to be non-compliant or ineligible for Registered Tier access
  // does in fact remove their access

  // TODO: test all the ways to retain/restore access

  // enabled/disabled is more of a master switch than a module but let's verify it anyway

  @Test
  public void test_updateUserWithRetries_disable_noncompliant() {
    testUnregistration(
        user -> {
          user.setDisabled(true);
          return userDao.save(user);
        });
  }

  // ERA Commons can be bypassed and is not subject to annual renewal.

  @Test
  public void test_updateUserWithRetries_era_unbypassed_noncompliant() {
    testUnregistration(
        user -> {
          user.setEraCommonsBypassTime(null);
          return userDao.save(user);
        });
  }

  // Two Factor Auth (2FA) can be bypassed and is not subject to annual renewal.

  @Test
  public void test_updateUserWithRetries_2fa_unbypassed_noncompliant() {
    testUnregistration(
        user -> {
          user.setTwoFactorAuthBypassTime(null);
          return userDao.save(user);
        });
  }

  // Compliance training can be bypassed, and is subject to annual renewal.

  @Test
  public void test_updateUserWithRetries_training_unbypassed_no_aar_noncompliant() {
    providedWorkbenchConfig.access.enableAccessRenewal = false;

    testUnregistration(
        user -> {
          user.setComplianceTrainingBypassTime(null);
          return userDao.save(user);
        });
  }

  @Test
  public void test_updateUserWithRetries_training_unbypassed_aar_noncompliant() {
    testUnregistration(
        user -> {
          user.setComplianceTrainingBypassTime(null);
          return userDao.save(user);
        });
  }

  @Test
  public void test_updateUserWithRetries_training_unbypassed_aar_expired_noncompliant() {
    testUnregistration(
        user -> {
          user.setComplianceTrainingBypassTime(null);
          final Timestamp willExpire = Timestamp.from(START_INSTANT);
          user.setComplianceTrainingCompletionTime(willExpire);

          advanceClockDays(EXPIRATION_DAYS + 1);

          return userDao.save(user);
        });
  }

  // DUA can be bypassed, and is subject to annual renewal.
  // A missing DUA version or a version other than the latest is also noncompliant.

  @Test
  public void test_updateUserWithRetries_dua_unbypassed_no_aar_noncompliant() {
    providedWorkbenchConfig.access.enableAccessRenewal = false;

    testUnregistration(
        user -> {
          user.setDataUseAgreementBypassTime(null);
          return userDao.save(user);
        });
  }

  @Test
  public void test_updateUserWithRetries_dua_unbypassed_no_aar_missing_version_noncompliant() {
    providedWorkbenchConfig.access.enableAccessRenewal = false;

    testUnregistration(
        user -> {
          user.setDataUseAgreementBypassTime(null);
          user.setDataUseAgreementCompletionTime(Timestamp.from(START_INSTANT));
          return userDao.save(user);
        });
  }

  @Test
  public void test_updateUserWithRetries_dua_unbypassed_no_aar_wrong_version_noncompliant() {
    providedWorkbenchConfig.access.enableAccessRenewal = false;

    testUnregistration(
        user -> {
          user.setDataUseAgreementBypassTime(null);
          user.setDataUseAgreementCompletionTime(Timestamp.from(START_INSTANT));
          user.setDataUseAgreementSignedVersion(userService.getCurrentDuccVersion() - 1);
          return userDao.save(user);
        });
  }

  @Test
  public void test_updateUserWithRetries_dua_unbypassed_aar_noncompliant() {
    testUnregistration(
        user -> {
          user.setDataUseAgreementBypassTime(null);
          return userDao.save(user);
        });
  }

  @Test
  public void test_updateUserWithRetries_dua_unbypassed_aar_missing_version_noncompliant() {
    testUnregistration(
        user -> {
          user.setDataUseAgreementBypassTime(null);
          user.setDataUseAgreementCompletionTime(Timestamp.from(START_INSTANT));
          return userDao.save(user);
        });
  }

  @Test
  public void test_updateUserWithRetries_dua_unbypassed_aar_wrong_version_noncompliant() {
    testUnregistration(
        user -> {
          user.setDataUseAgreementBypassTime(null);
          user.setDataUseAgreementCompletionTime(Timestamp.from(START_INSTANT));
          user.setDataUseAgreementSignedVersion(userService.getCurrentDuccVersion() - 1);
          return userDao.save(user);
        });
  }

  @Test
  public void test_updateUserWithRetries_dua_unbypassed_aar_expired_noncompliant() {
    testUnregistration(
        user -> {
          user.setDataUseAgreementBypassTime(null);
          final Timestamp willExpire = Timestamp.from(START_INSTANT);
          user.setDataUseAgreementCompletionTime(willExpire);
          user.setDataUseAgreementSignedVersion(userService.getCurrentDuccVersion());

          advanceClockDays(EXPIRATION_DAYS + 1);

          return userDao.save(user);
        });
  }

  // Publications confirmation is subject to annual renewal and cannot be bypassed.

  @Test
  public void test_updateUserWithRetries_publications_not_confirmed() {
    testUnregistration(
        user -> {
          user.setPublicationsLastConfirmedTime(null);
          return userDao.save(user);
        });
  }

  @Test
  public void test_updateUserWithRetries_publications_expired() {
    testUnregistration(
        user -> {
          final Timestamp willExpire = Timestamp.from(START_INSTANT);
          user.setPublicationsLastConfirmedTime(willExpire);

          advanceClockDays(EXPIRATION_DAYS + 1);

          return userDao.save(user);
        });
  }

  // Profile confirmation is subject to annual renewal and cannot be bypassed.

  @Test
  public void test_updateUserWithRetries_profile_not_confirmed() {
    testUnregistration(
        user -> {
          user.setProfileLastConfirmedTime(null);
          return userDao.save(user);
        });
  }

  @Test
  public void test_updateUserWithRetries_profile_expired() {
    testUnregistration(
        user -> {
          final Timestamp willExpire = Timestamp.from(START_INSTANT);
          user.setProfileLastConfirmedTime(willExpire);

          advanceClockDays(EXPIRATION_DAYS + 1);

          return userDao.save(user);
        });
  }

  @Test
  public void test_maybeSendAccessExpirationEmail_up_to_date() {
    final Timestamp now = new Timestamp(PROVIDED_CLOCK.millis());
    dbUser.setProfileLastConfirmedTime(now);
    dbUser.setPublicationsLastConfirmedTime(now);
    dbUser.setDataUseAgreementCompletionTime(now);
    dbUser.setComplianceTrainingCompletionTime(now);
    // a completion requirement for DUCC (formerly "DUA" - TODO rename)
    dbUser.setDataUseAgreementSignedVersion(userService.getCurrentDuccVersion());

    userService.maybeSendAccessExpirationEmail(dbUser);

    verifyZeroInteractions(mailService);
  }

  // bypassed modules do not expire: so no email

  @Test
  public void test_maybeSendAccessExpirationEmail_bypassed_is_up_to_date() {
    final Timestamp now = new Timestamp(PROVIDED_CLOCK.millis());

    dbUser.setDataUseAgreementBypassTime(now);
    dbUser.setComplianceTrainingBypassTime(now);

    // these 2 are not bypassable

    dbUser.setProfileLastConfirmedTime(now);
    dbUser.setPublicationsLastConfirmedTime(now);

    userService.maybeSendAccessExpirationEmail(dbUser);

    verifyZeroInteractions(mailService);
  }

  @Test
  public void test_maybeSendAccessExpirationEmail_expiring_1() throws MessagingException {
    // these are up to date
    final Timestamp now = new Timestamp(PROVIDED_CLOCK.millis());
    dbUser.setProfileLastConfirmedTime(now);
    dbUser.setPublicationsLastConfirmedTime(now);
    dbUser.setDataUseAgreementCompletionTime(now);
    // a completion requirement for DUCC (formerly "DUA" - TODO rename)
    dbUser.setDataUseAgreementSignedVersion(userService.getCurrentDuccVersion());

    // expiring in 1 day (plus some) will trigger the 1-day warning

    final Duration oneDayPlusSome = daysPlusSome(1);
    final Instant expirationTime = PROVIDED_CLOCK.instant().plus(oneDayPlusSome);
    dbUser.setComplianceTrainingCompletionTime(willExpireAfter(oneDayPlusSome));

    userService.maybeSendAccessExpirationEmail(dbUser);

    verify(mailService).alertUserRegisteredTierWarningThreshold(dbUser, 1, expirationTime);
  }

  @Test
  public void test_maybeSendAccessExpirationEmail_expiring_1_FF_false() {
    providedWorkbenchConfig.access.enableAccessRenewal = false;
    // these are up to date
    final Timestamp now = new Timestamp(PROVIDED_CLOCK.millis());
    dbUser.setProfileLastConfirmedTime(now);
    dbUser.setPublicationsLastConfirmedTime(now);
    dbUser.setDataUseAgreementCompletionTime(now);
    // a completion requirement for DUCC (formerly "DUA" - TODO rename)
    dbUser.setDataUseAgreementSignedVersion(userService.getCurrentDuccVersion());

    // expiring in 1 day (plus some) would trigger the 1-day warning...

    final Duration oneDayPlusSome = daysPlusSome(1);
    dbUser.setComplianceTrainingCompletionTime(willExpireAfter(oneDayPlusSome));

    // but the feature flag is off
    providedWorkbenchConfig.access.enableAccessRenewal = false;

    userService.maybeSendAccessExpirationEmail(dbUser);

    verifyZeroInteractions(mailService);
  }

  // if any module is incomplete, we don't send an email
  // because the user is not expiring soon - they never had access at all

  @Test
  public void test_maybeSendAccessExpirationEmail_expired_but_missing() {
    // these are up to date
    final Timestamp now = new Timestamp(PROVIDED_CLOCK.millis());
    dbUser.setProfileLastConfirmedTime(now);
    dbUser.setPublicationsLastConfirmedTime(now);

    // expiring in 1 day (plus some) would trigger the 1-day warning...

    final Duration oneDayPlusSome = daysPlusSome(1);
    dbUser.setComplianceTrainingCompletionTime(willExpireAfter(oneDayPlusSome));

    // but this module is incomplete (and also not bypassed)
    dbUser.setDataUseAgreementCompletionTime(null);
    dbUser.setDataUseAgreementBypassTime(null);

    userService.maybeSendAccessExpirationEmail(dbUser);

    verifyZeroInteractions(mailService);
  }

  // one or more bypassed modules will not affect whether emails are sent.
  // we consider only the unbypassed

  @Test
  public void test_maybeSendAccessExpirationEmail_expiring_1_with_bypass()
      throws MessagingException {
    // these are up to date
    final Timestamp now = new Timestamp(PROVIDED_CLOCK.millis());
    dbUser.setProfileLastConfirmedTime(now);
    dbUser.setPublicationsLastConfirmedTime(now);

    // this is bypassed
    dbUser.setDataUseAgreementBypassTime(now);

    // expiring in 1 day (plus some) will trigger the 1-day warning

    final Duration oneDayPlusSome = daysPlusSome(1);
    final Instant expirationTime = PROVIDED_CLOCK.instant().plus(oneDayPlusSome);
    dbUser.setComplianceTrainingCompletionTime(willExpireAfter(oneDayPlusSome));

    userService.maybeSendAccessExpirationEmail(dbUser);

    verify(mailService).alertUserRegisteredTierWarningThreshold(dbUser, 1, expirationTime);
  }

  // bypass times are not relevant to expiration emails

  @Test
  public void test_maybeSendAccessExpirationEmail_expiring_1_with_older_bypass()
      throws MessagingException {
    // these are up to date
    final Timestamp now = new Timestamp(PROVIDED_CLOCK.millis());
    dbUser.setProfileLastConfirmedTime(now);
    dbUser.setPublicationsLastConfirmedTime(now);

    // expiring in 1 day (plus some) will trigger the 1-day warning

    final Duration oneDayPlusSome = daysPlusSome(1);
    final Instant expirationTime = PROVIDED_CLOCK.instant().plus(oneDayPlusSome);
    dbUser.setComplianceTrainingCompletionTime(willExpireAfter(oneDayPlusSome));

    // a bypass which would "expire" in 30 days does NOT trigger a 30-day warning
    dbUser.setDataUseAgreementBypassTime(willExpireAfter(daysPlusSome(30)));

    userService.maybeSendAccessExpirationEmail(dbUser);

    verify(mailService).alertUserRegisteredTierWarningThreshold(dbUser, 1, expirationTime);
  }

  // we do not send an email if the expiration time is within the day.
  // we sent one yesterday for 1 day already, and we will send another once it actually expires.

  @Test
  public void test_maybeSendAccessExpirationEmail_expiring_today() {
    // these are up to date
    final Timestamp now = new Timestamp(PROVIDED_CLOCK.millis());
    dbUser.setProfileLastConfirmedTime(now);
    dbUser.setPublicationsLastConfirmedTime(now);
    dbUser.setDataUseAgreementCompletionTime(now);
    // a completion requirement for DUCC (formerly "DUA" - TODO rename)
    dbUser.setDataUseAgreementSignedVersion(userService.getCurrentDuccVersion());

    // expiring in .5 days will not trigger an email

    final Duration halfDay = Duration.ofHours(12);
    dbUser.setComplianceTrainingCompletionTime(willExpireAfter(halfDay));

    userService.maybeSendAccessExpirationEmail(dbUser);

    verifyZeroInteractions(mailService);
  }

  @Test
  public void test_maybeSendAccessExpirationEmail_expiring_30() throws MessagingException {
    // these are up to date
    final Timestamp now = new Timestamp(PROVIDED_CLOCK.millis());
    dbUser.setProfileLastConfirmedTime(now);
    dbUser.setPublicationsLastConfirmedTime(now);
    dbUser.setDataUseAgreementCompletionTime(now);
    // a completion requirement for DUCC (formerly "DUA" - TODO rename)
    dbUser.setDataUseAgreementSignedVersion(userService.getCurrentDuccVersion());

    // expiring in 30 days (plus) will trigger the 30-day warning

    final Duration thirtyPlus = daysPlusSome(30);
    final Instant expirationTime = PROVIDED_CLOCK.instant().plus(thirtyPlus);
    dbUser.setComplianceTrainingCompletionTime(willExpireAfter(thirtyPlus));

    userService.maybeSendAccessExpirationEmail(dbUser);

    verify(mailService).alertUserRegisteredTierWarningThreshold(dbUser, 30, expirationTime);
  }

  @Test
  public void test_maybeSendAccessExpirationEmail_expiring_31() {
    // these are up to date
    final Timestamp now = new Timestamp(PROVIDED_CLOCK.millis());
    dbUser.setProfileLastConfirmedTime(now);
    dbUser.setPublicationsLastConfirmedTime(now);
    dbUser.setDataUseAgreementCompletionTime(now);
    // a completion requirement for DUCC (formerly "DUA" - TODO rename)
    dbUser.setDataUseAgreementSignedVersion(userService.getCurrentDuccVersion());

    // expiring in 31 days (plus) will not trigger a warning
    dbUser.setComplianceTrainingCompletionTime(willExpireAfter(daysPlusSome(31)));

    userService.maybeSendAccessExpirationEmail(dbUser);

    verifyZeroInteractions(mailService);
  }

  // 15 days is sooner, so that's the email we send rather than 30

  @Test
  public void test_maybeSendAccessExpirationEmail_expiring_15_and_30() throws MessagingException {
    // these are up to date
    final Timestamp now = new Timestamp(PROVIDED_CLOCK.millis());
    dbUser.setProfileLastConfirmedTime(now);
    dbUser.setPublicationsLastConfirmedTime(now);

    // a completion requirement for DUCC (formerly "DUA" - TODO rename)
    dbUser.setDataUseAgreementSignedVersion(userService.getCurrentDuccVersion());

    // expiring in 30 days (plus) would trigger the 30-day warning...
    final Duration thirtyPlus = daysPlusSome(30);
    final Instant expirationTime30 = PROVIDED_CLOCK.instant().plus(thirtyPlus);
    dbUser.setComplianceTrainingCompletionTime(willExpireAfter(thirtyPlus));

    // but 15 days (plus) is sooner, so trigger 15 instead
    final Duration fifteenPlus = daysPlusSome(15);
    final Instant expirationTime15 = PROVIDED_CLOCK.instant().plus(fifteenPlus);
    dbUser.setDataUseAgreementCompletionTime(willExpireAfter(fifteenPlus));

    userService.maybeSendAccessExpirationEmail(dbUser);

    verify(mailService).alertUserRegisteredTierWarningThreshold(dbUser, 15, expirationTime15);
    verify(mailService, never())
        .alertUserRegisteredTierWarningThreshold(dbUser, 30, expirationTime30);
  }

  // 14 days is sooner than 15, but 14 days is not one of our email warning thresholds
  // so we send no email

  @Test
  public void test_maybeSendAccessExpirationEmail_expiring_14_and_15() {
    // these are up to date
    final Timestamp now = new Timestamp(PROVIDED_CLOCK.millis());
    dbUser.setProfileLastConfirmedTime(now);
    dbUser.setPublicationsLastConfirmedTime(now);

    // a completion requirement for DUCC (formerly "DUA" - TODO rename)
    dbUser.setDataUseAgreementSignedVersion(userService.getCurrentDuccVersion());

    // expiring in 15 days (plus) would trigger the 15-day warning...
    dbUser.setComplianceTrainingCompletionTime(willExpireAfter(daysPlusSome(15)));

    // but 14 days (plus) is sooner, so no email is sent
    dbUser.setDataUseAgreementCompletionTime(willExpireAfter(daysPlusSome(14)));

    userService.maybeSendAccessExpirationEmail(dbUser);

    verifyZeroInteractions(mailService);
  }

  @Test
  public void test_maybeSendAccessExpirationEmail_expired() throws MessagingException {
    // these are up to date
    final Timestamp now = new Timestamp(PROVIDED_CLOCK.millis());
    dbUser.setProfileLastConfirmedTime(now);
    dbUser.setPublicationsLastConfirmedTime(now);
    dbUser.setDataUseAgreementCompletionTime(now);
    // a completion requirement for DUCC (formerly "DUA" - TODO rename)
    dbUser.setDataUseAgreementSignedVersion(userService.getCurrentDuccVersion());

    // but this is expired
    final Duration oneHour = Duration.ofHours(1);
    final Instant expirationTime = PROVIDED_CLOCK.instant().minus(oneHour);
    dbUser.setComplianceTrainingCompletionTime(expiredBy(oneHour));

    userService.maybeSendAccessExpirationEmail(dbUser);

    verify(mailService).alertUserRegisteredTierExpiration(dbUser, expirationTime);
  }

  @Test
  public void test_maybeSendAccessExpirationEmail_expired_FF_false() {
    // these are up to date
    final Timestamp now = new Timestamp(PROVIDED_CLOCK.millis());
    dbUser.setProfileLastConfirmedTime(now);
    dbUser.setPublicationsLastConfirmedTime(now);
    dbUser.setDataUseAgreementCompletionTime(now);
    // a completion requirement for DUCC (formerly "DUA" - TODO rename)
    dbUser.setDataUseAgreementSignedVersion(userService.getCurrentDuccVersion());

    // this would be expired...
    dbUser.setComplianceTrainingCompletionTime(expiredBy(Duration.ofHours(1)));

    // but the feature flag is off
    providedWorkbenchConfig.access.enableAccessRenewal = false;

    userService.maybeSendAccessExpirationEmail(dbUser);

    verifyZeroInteractions(mailService);
  }

  // don't send an email if we have been expired for more than a day
  // because we sent the expiration email yesterday

  @Test
  public void test_maybeSendAccessExpirationEmail_extra_expired() {
    // these are up to date
    final Timestamp now = new Timestamp(PROVIDED_CLOCK.millis());
    dbUser.setProfileLastConfirmedTime(now);
    dbUser.setPublicationsLastConfirmedTime(now);
    dbUser.setDataUseAgreementCompletionTime(now);
    // a completion requirement for DUCC (formerly "DUA" - TODO rename)
    dbUser.setDataUseAgreementSignedVersion(userService.getCurrentDuccVersion());

    // but this expired yesterday

    final Instant aYearAgo = PROVIDED_CLOCK.instant().minus(EXPIRATION_DAYS, ChronoUnit.DAYS);
    final Timestamp extraExpired =
        Timestamp.from(aYearAgo.minus(Duration.ofDays(1)).minus(Duration.ofHours(1)));

    dbUser.setComplianceTrainingCompletionTime(extraExpired);

    userService.maybeSendAccessExpirationEmail(dbUser);

    verifyZeroInteractions(mailService);
  }

  @Test
  public void test_getRegisteredTierExpirations_empty() {
    assertThat(userService.getRegisteredTierExpirations()).isEmpty();
  }

  @Test
  public void test_getRegisteredTierExpirations_one_year() {
    // register user by setting 2 bypassable modules' bypass to now
    // and the 2 unbypassable modules' completions to now

    dbUser = updateUserWithRetries(registerUserNow);
    assertRegisteredTierEnabled(dbUser);

    // the 2 unbypassable modules will expire in a year
    final String aYearFromNow =
        PROVIDED_CLOCK.instant().plus(EXPIRATION_DAYS, ChronoUnit.DAYS).toString();

    final List<UserAccessExpiration> expirations = userService.getRegisteredTierExpirations();
    assertThat(expirations.size()).isEqualTo(1);
    assertThat(expirations.get(0).getUserName()).isEqualTo(dbUser.getUsername());
    assertThat(expirations.get(0).getContactEmail()).isEqualTo(dbUser.getContactEmail());
    assertThat(expirations.get(0).getGivenName()).isEqualTo(dbUser.getGivenName());
    assertThat(expirations.get(0).getFamilyName()).isEqualTo(dbUser.getFamilyName());

    assertThat(expirations.get(0).getExpirationDate()).isEqualTo(aYearFromNow);
  }

  // regression test: confirm that expirations are returned when the compliance module is disabled
  // and the user has not completed or bypassed compliance

  @Test
  public void test_getRegisteredTierExpirations_one_year_compliance_disabled() {
    providedWorkbenchConfig.access.enableComplianceTraining = false;

    // register user by setting the DUCC bypass to now
    // and the 2 unbypassable modules' completions to now

    final Timestamp now = Timestamp.from(PROVIDED_CLOCK.instant());
    dbUser =
        updateUserWithRetries(
            user -> {

              // this is sufficient to fully register the user when the compliance module is
              // disabled

              user.setDisabled(false);
              user.setEraCommonsBypassTime(now);
              user.setTwoFactorAuthBypassTime(now);
              user.setDataUseAgreementBypassTime(now);
              user.setPublicationsLastConfirmedTime(now);
              user.setProfileLastConfirmedTime(now);

              // ensure there is nothing set for compliance

              user.setComplianceTrainingCompletionTime(null);
              user.setComplianceTrainingBypassTime(null);

              return user;
            });
    assertRegisteredTierEnabled(dbUser);

    // the 2 unbypassable modules will expire in a year
    final String aYearFromNow =
        PROVIDED_CLOCK.instant().plus(EXPIRATION_DAYS, ChronoUnit.DAYS).toString();

    final List<UserAccessExpiration> expirations = userService.getRegisteredTierExpirations();
    assertThat(expirations.size()).isEqualTo(1);
    assertThat(expirations.get(0).getUserName()).isEqualTo(dbUser.getUsername());
    assertThat(expirations.get(0).getContactEmail()).isEqualTo(dbUser.getContactEmail());
    assertThat(expirations.get(0).getGivenName()).isEqualTo(dbUser.getGivenName());
    assertThat(expirations.get(0).getFamilyName()).isEqualTo(dbUser.getFamilyName());

    assertThat(expirations.get(0).getExpirationDate()).isEqualTo(aYearFromNow);
  }

  // adds `days` days plus most of another day (to demonstrate we are truncating, not rounding)
  private Duration daysPlusSome(long days) {
    return Duration.ofDays(days).plus(Duration.ofHours(18));
  }

  private Instant expirationBoundary() {
    return PROVIDED_CLOCK.instant().minus(EXPIRATION_DAYS, ChronoUnit.DAYS);
  }

  // set a completion timestamp which will expire after `duration`
  // by choosing a timestamp of (expirationBoundary() + duration)
  private Timestamp willExpireAfter(Duration duration) {
    return Timestamp.from(expirationBoundary().plus(duration));
  }

  // set a completion timestamp which is expired
  // by choosing a timestamp of (expirationBoundary() - a small duration)
  private Timestamp expiredBy(Duration duration) {
    return Timestamp.from(expirationBoundary().minus(duration));
  }

  private void advanceClockDays(long days) {
    PROVIDED_CLOCK.increment(daysPlusSome(days).toMillis());
  }

  // checks which power most of these tests - confirm that the unregisteringFunction does that
  private void testUnregistration(Function<DbUser, DbUser> unregisteringFunction) {
    // initial state: user is unregistered (has no tier memberships)
    assertThat(userAccessTierDao.findAll()).isEmpty();

    // we register the user
    dbUser = updateUserWithRetries(registerUserNow);
    assertRegisteredTierEnabled(dbUser);

    // we unregister the user by applying the function under test
    dbUser = updateUserWithRetries(unregisteringFunction);

    // The user received a DbUserAccessTier when they were registered.
    // They still have it after unregistering but now it is DISABLED.
    assertRegisteredTierDisabled(dbUser);
  }

  // we can trim the signature since we always call this in the same way
  private DbUser updateUserWithRetries(Function<DbUser, DbUser> userModifier) {
    return userService.updateUserWithRetries(userModifier, dbUser, Agent.asUser(dbUser));
  }

  private void assertRegisteredTierEnabled(DbUser dbUser) {
    assertRegisteredTierMembershipWithStatus(dbUser, TierAccessStatus.ENABLED);
  }

  private void assertRegisteredTierDisabled(DbUser dbUser) {
    assertRegisteredTierMembershipWithStatus(dbUser, TierAccessStatus.DISABLED);
  }

  private void assertRegisteredTierMembershipWithStatus(DbUser dbUser, TierAccessStatus status) {
    assertThat(userAccessTierDao.findAll()).hasSize(1);
    Optional<DbUserAccessTier> userAccessMaybe =
        userAccessTierDao.getByUserAndAccessTier(dbUser, registeredTier);
    assertThat(userAccessMaybe).isPresent();
    assertThat(userAccessMaybe.get().getTierAccessStatusEnum()).isEqualTo(status);
  }

  private DbUser registerUser(Timestamp timestamp, DbUser user) {
    // shouldUserBeRegistered logic:
    //    return !user.getDisabled()
    //        && complianceTrainingCompliant
    //        && eraCommonsCompliant
    //        && twoFactorAuthComplete
    //        && dataUseAgreementCompliant
    //        && isPublicationsCompliant
    //        && isProfileCompliant

    user.setDisabled(false);
    user.setComplianceTrainingBypassTime(timestamp);
    user.setEraCommonsBypassTime(timestamp);
    user.setTwoFactorAuthBypassTime(timestamp);
    user.setDataUseAgreementBypassTime(timestamp);
    user.setPublicationsLastConfirmedTime(timestamp);
    user.setProfileLastConfirmedTime(timestamp);

    return userDao.save(user);
  }
}
