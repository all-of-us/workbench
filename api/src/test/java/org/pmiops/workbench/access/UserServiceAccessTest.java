package org.pmiops.workbench.access;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;

import com.google.common.collect.ImmutableList;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
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
import org.pmiops.workbench.model.EmailVerificationStatus;
import org.pmiops.workbench.model.TierAccessStatus;
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
import org.springframework.test.context.junit4.SpringRunner;

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
  private static final Instant START_INSTANT = Instant.parse("2000-01-01T00:00:00.00Z");
  private static final FakeClock PROVIDED_CLOCK = new FakeClock(START_INSTANT);

  private static DbUser dbUser;
  private static WorkbenchConfig providedWorkbenchConfig;

  private static DbAccessTier registeredTier;

  private Function<Timestamp, Function<DbUser, DbUser>> registerUserWithTime =
      t -> dbu -> registerUser(t, dbu);
  private Function<DbUser, DbUser> registerUserNow =
      registerUserWithTime.apply(Timestamp.from(Instant.now()));

  @Autowired private AccessTierDao accessTierDao;
  @Autowired private UserAccessTierDao userAccessTierDao;
  @Autowired private UserDao userDao;
  @Autowired private UserService userService;

  @Import({
    UserServiceTestConfiguration.class,
    AccessTierServiceImpl.class,
  })
  @MockBean({
    ComplianceService.class,
    DirectoryService.class,
    FireCloudService.class,
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

    registeredTier = TestMockFactory.createRegisteredTierForTests(accessTierDao);

    DbUser user = new DbUser();
    user.setUsername(USERNAME);
    user = userDao.save(user);
    dbUser = user;
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
    providedWorkbenchConfig.access.enableAccessRenewal = true;
    providedWorkbenchConfig.access.enableDataUseAgreement = true;
    providedWorkbenchConfig.accessRenewal.expiryDays = (long) 365;

    // initialize user as registered with generic values including bypassed DUA

    dbUser = updateUserWithRetries(registerUserNow);
    assertRegisteredTierEnabled(dbUser);

    // add a proper DUA completion which will expire soon, but remove DUA bypass

    dbUser.setDataUseAgreementSignedVersion(userService.getCurrentDuccVersion());
    dbUser.setDataUseAgreementCompletionTime(
        Timestamp.from(
            START_INSTANT.minus(
                (providedWorkbenchConfig.accessRenewal.expiryDays - 1), ChronoUnit.DAYS)));
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
    providedWorkbenchConfig.access.enableDataUseAgreement = true;

    final long expirationWindow = 365L;
    providedWorkbenchConfig.accessRenewal.expiryDays = expirationWindow;
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

    // Time passing beyond the expirationWindow would cause the user to become
    // noncompliant when enableAccessRenewal = true
    advanceClockDays(expirationWindow + 1);

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

  // Beta Access is entirely controlled by bypass, if enabled.
  // It is not subject to annual renewal.

  @Test
  public void test_updateUserWithRetries_beta_unbypassed_noncompliant() {
    providedWorkbenchConfig.access.enableBetaAccess = true;
    testUnregistration(
        user -> {
          user.setBetaAccessBypassTime(null);
          return userDao.save(user);
        });
  }

  // email verification is not subject to bypass or annual renewal.
  // It must be SUBSCRIBED for access.

  @Test
  public void test_updateUserWithRetries_email_pending_noncompliant() {
    testUnregistration(
        user -> {
          user.setEmailVerificationStatusEnum(EmailVerificationStatus.PENDING);
          return userDao.save(user);
        });
  }

  @Test
  public void test_updateUserWithRetries_email_unverified_noncompliant() {
    testUnregistration(
        user -> {
          user.setEmailVerificationStatusEnum(EmailVerificationStatus.UNVERIFIED);
          return userDao.save(user);
        });
  }

  // ERA Commons can be bypassed and is not subject to annual renewal.

  @Test
  public void test_updateUserWithRetries_era_unbypassed_noncompliant() {
    providedWorkbenchConfig.access.enableEraCommons = true;
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
    providedWorkbenchConfig.access.enableComplianceTraining = true;
    providedWorkbenchConfig.access.enableAccessRenewal = false;
    testUnregistration(
        user -> {
          user.setComplianceTrainingBypassTime(null);
          return userDao.save(user);
        });
  }

  @Test
  public void test_updateUserWithRetries_training_unbypassed_aar_noncompliant() {
    providedWorkbenchConfig.access.enableComplianceTraining = true;
    providedWorkbenchConfig.access.enableAccessRenewal = true;
    providedWorkbenchConfig.accessRenewal.expiryDays = 365L;
    testUnregistration(
        user -> {
          user.setComplianceTrainingBypassTime(null);
          return userDao.save(user);
        });
  }

  @Test
  public void test_updateUserWithRetries_training_unbypassed_aar_expired_noncompliant() {
    providedWorkbenchConfig.access.enableComplianceTraining = true;
    providedWorkbenchConfig.access.enableAccessRenewal = true;

    final long expirationWindow = 365L;
    providedWorkbenchConfig.accessRenewal.expiryDays = expirationWindow;

    testUnregistration(
        user -> {
          user.setComplianceTrainingBypassTime(null);
          final Timestamp willExpire = Timestamp.from(START_INSTANT);
          user.setComplianceTrainingCompletionTime(willExpire);

          advanceClockDays(expirationWindow + 1);

          return userDao.save(user);
        });
  }

  // DUA can be bypassed, and is subject to annual renewal.
  // A missing DUA version or a version other than the latest is also noncompliant.

  @Test
  public void test_updateUserWithRetries_dua_unbypassed_no_aar_noncompliant() {
    providedWorkbenchConfig.access.enableDataUseAgreement = true;
    providedWorkbenchConfig.access.enableAccessRenewal = false;
    testUnregistration(
        user -> {
          user.setDataUseAgreementBypassTime(null);
          return userDao.save(user);
        });
  }

  @Test
  public void test_updateUserWithRetries_dua_unbypassed_no_aar_missing_version_noncompliant() {
    providedWorkbenchConfig.access.enableDataUseAgreement = true;
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
    providedWorkbenchConfig.access.enableDataUseAgreement = true;
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
    providedWorkbenchConfig.access.enableDataUseAgreement = true;
    providedWorkbenchConfig.access.enableAccessRenewal = true;
    providedWorkbenchConfig.accessRenewal.expiryDays = 365L;
    testUnregistration(
        user -> {
          user.setDataUseAgreementBypassTime(null);
          return userDao.save(user);
        });
  }

  @Test
  public void test_updateUserWithRetries_dua_unbypassed_aar_missing_version_noncompliant() {
    providedWorkbenchConfig.access.enableDataUseAgreement = true;
    providedWorkbenchConfig.access.enableAccessRenewal = true;
    providedWorkbenchConfig.accessRenewal.expiryDays = 365L;
    testUnregistration(
        user -> {
          user.setDataUseAgreementBypassTime(null);
          user.setDataUseAgreementCompletionTime(Timestamp.from(START_INSTANT));
          return userDao.save(user);
        });
  }

  @Test
  public void test_updateUserWithRetries_dua_unbypassed_aar_wrong_version_noncompliant() {
    providedWorkbenchConfig.access.enableDataUseAgreement = true;
    providedWorkbenchConfig.access.enableAccessRenewal = true;
    providedWorkbenchConfig.accessRenewal.expiryDays = 365L;
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
    providedWorkbenchConfig.access.enableDataUseAgreement = true;
    providedWorkbenchConfig.access.enableAccessRenewal = true;

    final long expirationWindow = 365L;
    providedWorkbenchConfig.accessRenewal.expiryDays = expirationWindow;

    testUnregistration(
        user -> {
          user.setDataUseAgreementBypassTime(null);
          final Timestamp willExpire = Timestamp.from(START_INSTANT);
          user.setDataUseAgreementCompletionTime(willExpire);
          user.setDataUseAgreementSignedVersion(userService.getCurrentDuccVersion());

          advanceClockDays(expirationWindow + 1);

          return userDao.save(user);
        });
  }

  // Publications confirmation is subject to annual renewal and cannot be bypassed.

  @Test
  public void test_updateUserWithRetries_publications_not_confirmed() {
    providedWorkbenchConfig.access.enableAccessRenewal = true;
    providedWorkbenchConfig.accessRenewal.expiryDays = 365L;
    testUnregistration(
        user -> {
          user.setPublicationsLastConfirmedTime(null);
          return userDao.save(user);
        });
  }

  @Test
  public void test_updateUserWithRetries_publications_expired() {
    providedWorkbenchConfig.access.enableAccessRenewal = true;

    final long expirationWindow = 365L;
    providedWorkbenchConfig.accessRenewal.expiryDays = expirationWindow;

    testUnregistration(
        user -> {
          final Timestamp willExpire = Timestamp.from(START_INSTANT);
          user.setPublicationsLastConfirmedTime(willExpire);

          advanceClockDays(expirationWindow + 1);

          return userDao.save(user);
        });
  }

  // Profile confirmation is subject to annual renewal and cannot be bypassed.

  @Test
  public void test_updateUserWithRetries_profile_not_confirmed() {
    providedWorkbenchConfig.access.enableAccessRenewal = true;
    providedWorkbenchConfig.accessRenewal.expiryDays = 365L;
    testUnregistration(
        user -> {
          user.setProfileLastConfirmedTime(null);
          return userDao.save(user);
        });
  }

  @Test
  public void test_updateUserWithRetries_profile_expired() {
    providedWorkbenchConfig.access.enableAccessRenewal = true;

    final long expirationWindow = 365L;
    providedWorkbenchConfig.accessRenewal.expiryDays = expirationWindow;

    testUnregistration(
        user -> {
          final Timestamp willExpire = Timestamp.from(START_INSTANT);
          user.setProfileLastConfirmedTime(willExpire);

          advanceClockDays(expirationWindow + 1);

          return userDao.save(user);
        });
  }

  private void advanceClockDays(long days) {
    PROVIDED_CLOCK.setInstant(START_INSTANT.plus(days, ChronoUnit.DAYS));
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
    //        && betaAccessGranted
    //        && twoFactorAuthComplete
    //        && dataUseAgreementCompliant
    //        && isPublicationsCompliant
    //        && isProfileCompliant
    //        && EmailVerificationStatus.SUBSCRIBED.equals(user.getEmailVerificationStatusEnum());

    user.setDisabled(false);
    user.setEmailVerificationStatusEnum(EmailVerificationStatus.SUBSCRIBED);
    user.setComplianceTrainingBypassTime(timestamp);
    user.setEraCommonsBypassTime(timestamp);
    user.setBetaAccessBypassTime(timestamp);
    user.setTwoFactorAuthBypassTime(timestamp);
    user.setDataUseAgreementBypassTime(timestamp);
    user.setPublicationsLastConfirmedTime(timestamp);
    user.setProfileLastConfirmedTime(timestamp);

    return userDao.save(user);
  }
}
