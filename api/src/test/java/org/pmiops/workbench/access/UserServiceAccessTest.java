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
@ExtendWith(SpringExtension.class)
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class UserServiceAccessTest {
  private static final String USERNAME = "abc@fake-research-aou.org";
  private static final Instant START_INSTANT = Instant.parse("2000-01-01T00:00:00.00Z");
  private static final FakeClock PROVIDED_CLOCK = new FakeClock(START_INSTANT);

  private static DbUser providedDbUser;
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
      return providedDbUser;
    }
  }

  @BeforeEach
  public void setUp() {
    providedWorkbenchConfig = WorkbenchConfig.createEmptyConfig();

    registeredTier = TestMockFactory.createRegisteredTierForTests(accessTierDao);

    DbUser user = new DbUser();
    user.setUsername(USERNAME);
    user = userDao.save(user);
    providedDbUser = user;
  }

  @Test
  public void test_updateUserWithRetries_register() {
    DbUser dbUser = userDao.save(new DbUser());
    assertThat(userAccessTierDao.findAll()).isEmpty();

    dbUser = userService.updateUserWithRetries(registerUserNow, dbUser, Agent.asUser(dbUser));

    assertThat(userAccessTierDao.findAll()).hasSize(1);
    Optional<DbUserAccessTier> userAccessMaybe =
        userAccessTierDao.getByUserAndAccessTier(dbUser, registeredTier);
    assertThat(userAccessMaybe).isPresent();
    assertThat(userAccessMaybe.get().getTierAccessStatusEnum()).isEqualTo(TierAccessStatus.ENABLED);
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

    DbUser dbUser = userDao.save(new DbUser());
    assertThat(userAccessTierDao.findAll()).isEmpty();

    dbUser = userService.updateUserWithRetries(registerUserNow, dbUser, Agent.asUser(dbUser));

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
  public void test_updateUserWithRetries_unregister() {
    DbUser dbUser = new DbUser();
    assertThat(userAccessTierDao.findAll()).isEmpty();

    dbUser = userService.updateUserWithRetries(this::unregisterUser, dbUser, Agent.asUser(dbUser));

    // the user has never been registered so they have no DbUserAccessTier entry

    assertThat(userAccessTierDao.findAll()).hasSize(0);
    Optional<DbUserAccessTier> userAccessMaybe =
        userAccessTierDao.getByUserAndAccessTier(dbUser, registeredTier);
    assertThat(userAccessMaybe).isEmpty();
  }

  @Test
  public void test_updateUserWithRetries_register_then_unregister() {
    DbUser dbUser = new DbUser();
    assertThat(userAccessTierDao.findAll()).isEmpty();

    dbUser = userService.updateUserWithRetries(registerUserNow, dbUser, Agent.asUser(dbUser));
    dbUser = userService.updateUserWithRetries(this::unregisterUser, dbUser, Agent.asUser(dbUser));

    // The user received a DbUserAccessTier when they were registered.
    // They still have it after unregistering but now it is DISABLED.

    assertThat(userAccessTierDao.findAll()).hasSize(1);
    Optional<DbUserAccessTier> userAccessMaybe =
        userAccessTierDao.getByUserAndAccessTier(dbUser, registeredTier);
    assertThat(userAccessMaybe).isPresent();
    assertThat(userAccessMaybe.get().getTierAccessStatusEnum())
        .isEqualTo(TierAccessStatus.DISABLED);
  }

  @Test
  public void testSimulateUserFlowThroughRenewal() {
    providedWorkbenchConfig.access.enableAccessRenewal = true;
    providedWorkbenchConfig.access.enableDataUseAgreement = true;
    providedWorkbenchConfig.accessRenewal.expiryDays = (long) 365;
    DbUser dbUser = new DbUser();
    dbUser.setDataUseAgreementSignedVersion(userService.getCurrentDuccVersion());

    // The user is still compliant and has bypass set
    dbUser.setDataUseAgreementCompletionTime(
        Timestamp.from(
            START_INSTANT.minus(
                (providedWorkbenchConfig.accessRenewal.expiryDays - 1), ChronoUnit.DAYS)));
    dbUser = userService.updateUserWithRetries(registerUserNow, dbUser, Agent.asUser(dbUser));

    assertThat(userAccessTierDao.findAll()).hasSize(1);
    assertThat(userAccessTierDao.getAllByUser(dbUser).get(0).getTierAccessStatusEnum())
        .isEqualTo(TierAccessStatus.ENABLED);

    // User is compliant and we remove dua bypass
    dbUser =
        userService.updateUserWithRetries(
            this::registerUserWithoutDuaBypass, dbUser, Agent.asUser(dbUser));

    assertThat(userAccessTierDao.findAll()).hasSize(1);
    assertThat(userAccessTierDao.getAllByUser(dbUser).get(0).getTierAccessStatusEnum())
        .isEqualTo(TierAccessStatus.ENABLED);

    // Simulate time passing, user is no longer compliant
    PROVIDED_CLOCK.setInstant(START_INSTANT.plus(2, ChronoUnit.DAYS));
    dbUser =
        userService.updateUserWithRetries(
            this::registerUserWithoutDuaBypass, dbUser, Agent.asUser(dbUser));
    assertThat(userAccessTierDao.getAllByUser(dbUser)).hasSize(1);
    assertThat(userAccessTierDao.getAllByUser(dbUser).get(0).getTierAccessStatusEnum())
        .isEqualTo(TierAccessStatus.DISABLED);

    // Simulate user filling out DUA, becoming compliant again
    dbUser.setDataUseAgreementCompletionTime(
        Timestamp.from(START_INSTANT.plus(2, ChronoUnit.DAYS)));
    dbUser =
        userService.updateUserWithRetries(
            this::registerUserWithoutDuaBypass, dbUser, Agent.asUser(dbUser));
    assertThat(userAccessTierDao.getAllByUser(dbUser)).hasSize(1);
    assertThat(userAccessTierDao.getAllByUser(dbUser).get(0).getTierAccessStatusEnum())
        .isEqualTo(TierAccessStatus.ENABLED);
  }

  @Test
  public void testRenewalFlag() {
    providedWorkbenchConfig.access.enableAccessRenewal = false;
    providedWorkbenchConfig.access.enableDataUseAgreement = true;
    providedWorkbenchConfig.accessRenewal.expiryDays = (long) 365;
    DbUser dbUser = new DbUser();
    dbUser.setDataUseAgreementSignedVersion(userService.getCurrentDuccVersion());

    // The user is still compliant and has bypass set
    dbUser.setDataUseAgreementCompletionTime(
        Timestamp.from(
            START_INSTANT.minus(
                (providedWorkbenchConfig.accessRenewal.expiryDays - 1), ChronoUnit.DAYS)));
    dbUser = userService.updateUserWithRetries(registerUserNow, dbUser, Agent.asUser(dbUser));

    assertThat(userAccessTierDao.findAll()).hasSize(1);
    assertThat(userAccessTierDao.getAllByUser(dbUser).get(0).getTierAccessStatusEnum())
        .isEqualTo(TierAccessStatus.ENABLED);

    // User is compliant and we remove dua bypass
    dbUser =
        userService.updateUserWithRetries(
            this::registerUserWithoutDuaBypass, dbUser, Agent.asUser(dbUser));

    assertThat(userAccessTierDao.findAll()).hasSize(1);
    assertThat(userAccessTierDao.getAllByUser(dbUser).get(0).getTierAccessStatusEnum())
        .isEqualTo(TierAccessStatus.ENABLED);

    // Simulate time passing, user is no longer compliant, but the flag is not set so they should be
    // enabled
    PROVIDED_CLOCK.setInstant(START_INSTANT.plus(1, ChronoUnit.DAYS));
    dbUser =
        userService.updateUserWithRetries(
            this::registerUserWithoutDuaBypass, dbUser, Agent.asUser(dbUser));
    assertThat(userAccessTierDao.getAllByUser(dbUser)).hasSize(1);
    assertThat(userAccessTierDao.getAllByUser(dbUser).get(0).getTierAccessStatusEnum())
        .isEqualTo(TierAccessStatus.ENABLED);
  }

  private DbUser registerUserWithoutDuaBypass(DbUser user) {
    user.setDataUseAgreementBypassTime(null);
    return userDao.save(user);
  }

  private DbUser registerUser(Timestamp timestamp, DbUser user) {
    // shouldUserBeRegistered logic:
    //    return !user.getDisabled()
    //        && complianceTrainingCompliant
    //        && eraCommonsCompliant
    //        && betaAccessGranted
    //        && twoFactorAuthComplete
    //        && dataUseAgreementCompliant
    //        && EmailVerificationStatus.SUBSCRIBED.equals(user.getEmailVerificationStatusEnum());

    user.setDisabled(false);
    user.setEmailVerificationStatusEnum(EmailVerificationStatus.SUBSCRIBED);
    user.setComplianceTrainingBypassTime(timestamp);
    user.setEraCommonsBypassTime(timestamp);
    user.setBetaAccessBypassTime(timestamp);
    user.setTwoFactorAuthBypassTime(timestamp);
    user.setDataUseAgreementBypassTime(timestamp);

    return userDao.save(user);
  }

  private DbUser unregisterUser(DbUser user) {
    user.setDisabled(true);
    return userDao.save(user);
  }
}
