package org.pmiops.workbench.access;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.pmiops.workbench.access.AccessTierService.CONTROLLED_TIER_SHORT_NAME;
import static org.pmiops.workbench.access.AccessTierService.REGISTERED_TIER_SHORT_NAME;
import static org.pmiops.workbench.utils.TestMockFactory.createControlledTier;
import static org.pmiops.workbench.utils.TestMockFactory.createRegisteredTier;

import com.google.common.collect.ImmutableList;
import jakarta.mail.MessagingException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.actionaudit.Agent;
import org.pmiops.workbench.actionaudit.auditors.UserServiceAuditor;
import org.pmiops.workbench.compliance.ComplianceService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.AccessModuleDao;
import org.pmiops.workbench.db.dao.AccessTierDao;
import org.pmiops.workbench.db.dao.UserAccessTierDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.dao.VerifiedInstitutionalAffiliationDao;
import org.pmiops.workbench.db.model.DbAccessModule;
import org.pmiops.workbench.db.model.DbAccessModule.DbAccessModuleName;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserAccessTier;
import org.pmiops.workbench.db.model.DbUserCodeOfConductAgreement;
import org.pmiops.workbench.db.model.DbVerifiedInstitutionalAffiliation;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.google.DirectoryService;
import org.pmiops.workbench.institution.InstitutionService;
import org.pmiops.workbench.institution.InstitutionServiceImpl;
import org.pmiops.workbench.mail.MailService;
import org.pmiops.workbench.model.Institution;
import org.pmiops.workbench.model.InstitutionMembershipRequirement;
import org.pmiops.workbench.model.InstitutionTierConfig;
import org.pmiops.workbench.model.InstitutionalRole;
import org.pmiops.workbench.model.OrganizationType;
import org.pmiops.workbench.model.TierAccessStatus;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.testconfig.UserServiceTestConfiguration;
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
import org.springframework.test.annotation.DirtiesContext;

/**
 * Tests to cover access change determinations by executing {@link
 * UserService#updateUserWithRetries(java.util.function.Function,
 * org.pmiops.workbench.db.model.DbUser, org.pmiops.workbench.actionaudit.Agent)} or {@link
 * AccessSyncService#updateUserAccessTiers(org.pmiops.workbench.db.model.DbUser,
 * org.pmiops.workbench.actionaudit.Agent)} with different configurations, which ultimately executes
 * the private method {@link
 * AccessSyncService#shouldGrantUserTierAccess(org.pmiops.workbench.db.model.DbUser, List, String)}
 * to make this determination.
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
  private static DbAccessTier controlledTier;

  private Function<Timestamp, Function<DbUser, DbUser>> registerUserWithTime =
      t -> dbu -> registerUser(t, dbu);
  private Function<DbUser, DbUser> registerUserNow;

  private static List<DbAccessModule> accessModules;

  private InstitutionTierConfig rtTierConfig;
  private InstitutionTierConfig ctTierConfig;
  private Institution institution;

  @Autowired private AccessModuleDao accessModuleDao;
  @Autowired private AccessModuleService accessModuleService;
  @Autowired private AccessTierDao accessTierDao;
  @Autowired private AccessSyncService accessSyncService;
  @Autowired private InstitutionService institutionService;
  @Autowired private UserAccessTierDao userAccessTierDao;
  @Autowired private UserDao userDao;
  @Autowired private UserService userService;
  @Autowired private VerifiedInstitutionalAffiliationDao verifiedInstitutionalAffiliationDao;

  @MockBean private MailService mailService;

  @Import({
    UserServiceTestConfiguration.class,
    CommonMappers.class,
    AccessModuleServiceImpl.class,
    AccessTierServiceImpl.class,
    AccessSyncServiceImpl.class,
    InstitutionServiceImpl.class,
    UserAccessModuleMapperImpl.class,
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

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public List<DbAccessModule> getDbAccessModules() {
      return accessModules;
    }
  }

  @BeforeEach
  public void setUp() {
    providedWorkbenchConfig = WorkbenchConfig.createEmptyConfig();
    providedWorkbenchConfig.access.enableComplianceTraining = true;
    providedWorkbenchConfig.access.enableEraCommons = true;
    providedWorkbenchConfig.access.enableRasLoginGovLinking = true;
    providedWorkbenchConfig.access.currentDuccVersions = ImmutableList.of(1, 2); // arbitrary
    providedWorkbenchConfig.access.renewal.expiryDays = EXPIRATION_DAYS;
    providedWorkbenchConfig.access.renewal.expiryDaysWarningThresholds =
        ImmutableList.of(1L, 3L, 7L, 15L, 30L);
    registeredTier = accessTierDao.save(createRegisteredTier());
    controlledTier = accessTierDao.save(createControlledTier());
    accessModules = TestMockFactory.createAccessModules(accessModuleDao);

    dbUser = new DbUser();
    dbUser.setUsername(USERNAME);
    dbUser.setContactEmail("user@domain.com");
    dbUser = userDao.save(dbUser);

    rtTierConfig = new InstitutionTierConfig().accessTierShortName(registeredTier.getShortName());
    ctTierConfig = new InstitutionTierConfig().accessTierShortName(controlledTier.getShortName());
    institution =
        new Institution()
            .displayName("institution")
            .shortName("shortname")
            .tierConfigs(
                ImmutableList.of(
                    rtTierConfig
                        .membershipRequirement(InstitutionMembershipRequirement.DOMAINS)
                        .addEmailDomainsItem("domain.com")
                        .eraRequired(true)
                        .accessTierShortName(registeredTier.getShortName())))
            .organizationTypeEnum(OrganizationType.INDUSTRY)
            .userInstructions("Some user instructions");
    institution = institutionService.createInstitution(institution);
    createAffiliation(dbUser);

    // reset the clock so tests changing this don't affect each other
    PROVIDED_CLOCK.setInstant(START_INSTANT);
    registerUserNow = registerUserWithTime.apply(new Timestamp(PROVIDED_CLOCK.millis()));
  }

  @Test
  public void test_updateUserWithRetries_never_registered() {
    assertThat(userAccessTierDao.findAll()).isEmpty();

    dbUser = updateUserAccessTiers();

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
    assertUserNotInAccessTier(dbUser, controlledTier);
  }

  @Test
  public void testSimulateUserFlowThroughRenewal() {
    // initialize user as registered with generic values including bypassed DUCC

    dbUser = updateUserWithRetries(registerUserNow);
    assertRegisteredTierEnabled(dbUser);

    // add a proper DUCC completion which will expire soon, but remove DUCC bypass

    dbUser.setDuccAgreement(signCurrentDucc(dbUser));
    accessModuleService.updateCompletionTime(
        dbUser, DbAccessModuleName.DATA_USER_CODE_OF_CONDUCT, willExpireAfter(Duration.ofDays(1)));
    dbUser = updateUserWithRetries(this::removeDuccBypass);

    // User is compliant
    assertRegisteredTierEnabled(dbUser);

    // Simulate time passing, user is no longer compliant
    advanceClockDays(2);
    dbUser = updateUserAccessTiers();
    assertRegisteredTierDisabled(dbUser);

    // Simulate user filling out DUCC, becoming compliant again
    accessModuleService.updateCompletionTime(
        dbUser,
        DbAccessModuleName.DATA_USER_CODE_OF_CONDUCT,
        new Timestamp(PROVIDED_CLOCK.millis()));
    dbUser = updateUserAccessTiers();
    assertRegisteredTierEnabled(dbUser);
  }

  private DbUser removeDuccBypass(DbUser user) {
    accessModuleService.updateBypassTime(
        user.getUserId(), DbAccessModuleName.DATA_USER_CODE_OF_CONDUCT, false);
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

  // ERA Commons is not subject to annual renewal.

  @Test
  public void test_updateUserWithRetries_era_unbypassed_noncompliant() {
    testUnregistration(
        user -> {
          accessModuleService.updateBypassTime(
              dbUser.getUserId(), DbAccessModuleName.ERA_COMMONS, false);
          return userDao.save(user);
        });
  }

  // Two Factor Auth (2FA) is not subject to annual renewal.

  @Test
  public void test_updateUserWithRetries_2fa_unbypassed_noncompliant() {
    testUnregistration(
        user -> {
          accessModuleService.updateBypassTime(
              dbUser.getUserId(), DbAccessModuleName.TWO_FACTOR_AUTH, false);
          return userDao.save(user);
        });
  }

  // Compliance training is subject to annual renewal.

  @Test
  public void test_updateUserWithRetries_training_unbypassed_aar_noncompliant() {
    testUnregistration(
        user -> {
          accessModuleService.updateBypassTime(
              dbUser.getUserId(), DbAccessModuleName.RT_COMPLIANCE_TRAINING, false);
          return userDao.save(user);
        });
  }

  @Test
  public void test_updateUserWithRetries_training_unbypassed_aar_expired_noncompliant() {
    testUnregistration(
        user -> {
          final Timestamp willExpire = Timestamp.from(START_INSTANT);
          accessModuleService.updateBypassTime(
              dbUser.getUserId(), DbAccessModuleName.RT_COMPLIANCE_TRAINING, false);
          accessModuleService.updateCompletionTime(
              dbUser, DbAccessModuleName.RT_COMPLIANCE_TRAINING, willExpire);

          advanceClockDays(EXPIRATION_DAYS + 1);

          return userDao.save(user);
        });
  }

  // DUCC is subject to annual renewal.
  // A missing DUCC version or a version other than the latest is also noncompliant.

  @Test
  public void test_updateUserWithRetries_ducc_unbypassed_aar_noncompliant() {
    testUnregistration(
        user -> {
          accessModuleService.updateBypassTime(
              dbUser.getUserId(), DbAccessModuleName.DATA_USER_CODE_OF_CONDUCT, false);
          return userDao.save(user);
        });
  }

  @Test
  public void test_updateUserWithRetries_ducc_unbypassed_aar_missing_version_noncompliant() {
    testUnregistration(
        user -> {
          accessModuleService.updateBypassTime(
              dbUser.getUserId(), DbAccessModuleName.DATA_USER_CODE_OF_CONDUCT, false);
          accessModuleService.updateCompletionTime(
              dbUser, DbAccessModuleName.DATA_USER_CODE_OF_CONDUCT, Timestamp.from(START_INSTANT));
          return userDao.save(user);
        });
  }

  @Test
  public void test_updateUserWithRetries_ducc_unbypassed_aar_wrong_version_noncompliant() {
    providedWorkbenchConfig.access.currentDuccVersions = ImmutableList.of(4, 5);

    testUnregistration(
        user -> {
          accessModuleService.updateBypassTime(
              dbUser.getUserId(), DbAccessModuleName.DATA_USER_CODE_OF_CONDUCT, false);
          accessModuleService.updateCompletionTime(
              dbUser, DbAccessModuleName.DATA_USER_CODE_OF_CONDUCT, Timestamp.from(START_INSTANT));
          user.setDuccAgreement(signDucc(user, 3));
          return userDao.save(user);
        });
  }

  @Test
  public void test_updateUserWithRetries_ducc_unbypassed_aar_expired_noncompliant() {
    testUnregistration(
        user -> {
          final Timestamp willExpire = Timestamp.from(START_INSTANT);
          accessModuleService.updateBypassTime(
              dbUser.getUserId(), DbAccessModuleName.DATA_USER_CODE_OF_CONDUCT, false);
          accessModuleService.updateCompletionTime(
              dbUser, DbAccessModuleName.DATA_USER_CODE_OF_CONDUCT, willExpire);

          user.setDuccAgreement(signCurrentDucc(user));

          advanceClockDays(EXPIRATION_DAYS + 1);

          return userDao.save(user);
        });
  }

  // Publications confirmation is subject to annual renewal.

  @Test
  public void test_updateUserWithRetries_publications_unbypassed_publications_not_confirmed() {
    testUnregistration(
        user -> {
          accessModuleService.updateBypassTime(
              dbUser.getUserId(), DbAccessModuleName.PUBLICATION_CONFIRMATION, false);
          accessModuleService.updateCompletionTime(
              dbUser, DbAccessModuleName.PUBLICATION_CONFIRMATION, null);
          return userDao.save(user);
        });
  }

  @Test
  public void test_updateUserWithRetries_publications_unbypassed_publications_expired() {
    testUnregistration(
        user -> {
          accessModuleService.updateBypassTime(
              dbUser.getUserId(), DbAccessModuleName.PUBLICATION_CONFIRMATION, false);
          final Timestamp willExpire = Timestamp.from(START_INSTANT);
          accessModuleService.updateCompletionTime(
              dbUser, DbAccessModuleName.PUBLICATION_CONFIRMATION, willExpire);
          advanceClockDays(EXPIRATION_DAYS + 1);

          return userDao.save(user);
        });
  }

  // Profile confirmation is subject to annual renewal.

  @Test
  public void test_updateUserWithRetries_profile_unbypassed_profile_not_confirmed() {
    testUnregistration(
        user -> {
          accessModuleService.updateBypassTime(
              dbUser.getUserId(), DbAccessModuleName.PROFILE_CONFIRMATION, false);
          accessModuleService.updateCompletionTime(
              dbUser, DbAccessModuleName.PROFILE_CONFIRMATION, null);
          return userDao.save(user);
        });
  }

  @Test
  public void test_updateUserWithRetries_profile_unbypassed_profile_expired() {
    testUnregistration(
        user -> {
          accessModuleService.updateBypassTime(
              dbUser.getUserId(), DbAccessModuleName.PROFILE_CONFIRMATION, false);
          final Timestamp willExpire = Timestamp.from(START_INSTANT);
          accessModuleService.updateCompletionTime(
              dbUser, DbAccessModuleName.PROFILE_CONFIRMATION, willExpire);
          advanceClockDays(EXPIRATION_DAYS + 1);

          return userDao.save(user);
        });
  }

  @Test
  public void test_maybeSendAccessTierExpirationEmails_up_to_date() {
    final Timestamp now = new Timestamp(PROVIDED_CLOCK.millis());
    accessModuleService.updateCompletionTime(dbUser, DbAccessModuleName.PROFILE_CONFIRMATION, now);
    accessModuleService.updateCompletionTime(
        dbUser, DbAccessModuleName.PUBLICATION_CONFIRMATION, now);
    accessModuleService.updateCompletionTime(
        dbUser, DbAccessModuleName.DATA_USER_CODE_OF_CONDUCT, now);
    accessModuleService.updateCompletionTime(
        dbUser, DbAccessModuleName.RT_COMPLIANCE_TRAINING, now);

    // a completion requirement for DUCC
    dbUser.setDuccAgreement(signCurrentDucc(dbUser));

    userService.maybeSendAccessTierExpirationEmails(dbUser);

    verifyNoInteractions(mailService);
  }

  // bypassed modules do not expire: so no email

  @Test
  public void test_maybeSendAccessTierExpirationEmails_bypassed_is_up_to_date() {
    final Timestamp now = new Timestamp(PROVIDED_CLOCK.millis());

    accessModuleService.updateBypassTime(
        dbUser.getUserId(), DbAccessModuleName.DATA_USER_CODE_OF_CONDUCT, true);
    accessModuleService.updateBypassTime(
        dbUser.getUserId(), DbAccessModuleName.RT_COMPLIANCE_TRAINING, true);
    accessModuleService.updateBypassTime(
        dbUser.getUserId(), DbAccessModuleName.PROFILE_CONFIRMATION, true);
    accessModuleService.updateBypassTime(
        dbUser.getUserId(), DbAccessModuleName.PUBLICATION_CONFIRMATION, true);

    userService.maybeSendAccessTierExpirationEmails(dbUser);

    verifyNoInteractions(mailService);
  }

  @Test
  public void test_maybeSendAccessTierExpirationEmails_expiring_1_rt() throws MessagingException {
    // these are up to date
    final Timestamp now = new Timestamp(PROVIDED_CLOCK.millis());
    accessModuleService.updateCompletionTime(
        dbUser, DbAccessModuleName.PUBLICATION_CONFIRMATION, now);
    accessModuleService.updateCompletionTime(dbUser, DbAccessModuleName.PROFILE_CONFIRMATION, now);
    accessModuleService.updateCompletionTime(
        dbUser, DbAccessModuleName.DATA_USER_CODE_OF_CONDUCT, now);
    accessModuleService.updateCompletionTime(
        dbUser, DbAccessModuleName.CT_COMPLIANCE_TRAINING, now);

    // a completion requirement for DUCC
    dbUser.setDuccAgreement(signCurrentDucc(dbUser));

    // expiring in 1 day (plus some) will trigger the 1-day warning

    final Duration oneDayPlusSome = daysPlusSome(1);
    final Instant expirationTime = PROVIDED_CLOCK.instant().plus(oneDayPlusSome);
    accessModuleService.updateCompletionTime(
        dbUser, DbAccessModuleName.RT_COMPLIANCE_TRAINING, willExpireAfter(oneDayPlusSome));

    userService.maybeSendAccessTierExpirationEmails(dbUser);

    verify(mailService)
        .alertUserAccessTierWarningThreshold(dbUser, 1, expirationTime, REGISTERED_TIER_SHORT_NAME);

    verify(mailService, never())
        .alertUserAccessTierWarningThreshold(
            any(), anyLong(), any(), eq(CONTROLLED_TIER_SHORT_NAME));

    // No expiration email is sent.
    verify(mailService, never()).alertUserAccessTierExpiration(any(), any(), any());
  }

  @Test
  public void test_maybeSendAccessTierExpirationEmails_expiring_1_ct() throws MessagingException {
    // expiring in 1 day (plus some) will trigger the 1-day warning

    final Duration oneDayPlusSome = daysPlusSome(1);
    final Instant expirationTime = PROVIDED_CLOCK.instant().plus(oneDayPlusSome);
    accessModuleService.updateCompletionTime(
        dbUser, DbAccessModuleName.CT_COMPLIANCE_TRAINING, willExpireAfter(oneDayPlusSome));

    userService.maybeSendAccessTierExpirationEmails(dbUser);

    // Controlled Tier Threshold email is sent.
    verify(mailService)
        .alertUserAccessTierWarningThreshold(dbUser, 1, expirationTime, CONTROLLED_TIER_SHORT_NAME);

    // Registered Tier Threshold Email is not sent
    verify(mailService, never())
        .alertUserAccessTierWarningThreshold(
            any(), anyLong(), any(), eq(REGISTERED_TIER_SHORT_NAME));

    // No expiration email is sent.
    verify(mailService, never()).alertUserAccessTierExpiration(any(), any(), any());
  }

  // if any module is incomplete, we don't send an email
  // because the user is not expiring soon - they never had access at all

  @Test
  public void test_maybeSendAccessTierExpirationEmails_expired_but_missing() {
    // these are up to date
    final Timestamp now = new Timestamp(PROVIDED_CLOCK.millis());
    accessModuleService.updateCompletionTime(
        dbUser, DbAccessModuleName.PUBLICATION_CONFIRMATION, now);
    accessModuleService.updateCompletionTime(dbUser, DbAccessModuleName.PROFILE_CONFIRMATION, now);

    // expiring in 1 day (plus some) would trigger the 1-day warning...

    final Duration oneDayPlusSome = daysPlusSome(1);
    accessModuleService.updateCompletionTime(
        dbUser, DbAccessModuleName.RT_COMPLIANCE_TRAINING, willExpireAfter(oneDayPlusSome));

    // but this module is incomplete (and also not bypassed)
    accessModuleService.updateCompletionTime(
        dbUser, DbAccessModuleName.DATA_USER_CODE_OF_CONDUCT, null);
    accessModuleService.updateBypassTime(
        dbUser.getUserId(), DbAccessModuleName.DATA_USER_CODE_OF_CONDUCT, false);

    userService.maybeSendAccessTierExpirationEmails(dbUser);

    verifyNoInteractions(mailService);
  }

  // one or more bypassed modules will not affect whether emails are sent.
  // we consider only the unbypassed

  @Test
  public void test_maybeSendAccessTierExpirationEmails_expiring_1_with_bypass()
      throws MessagingException {
    // these are up to date
    final Timestamp now = new Timestamp(PROVIDED_CLOCK.millis());
    accessModuleService.updateCompletionTime(dbUser, DbAccessModuleName.PROFILE_CONFIRMATION, now);
    accessModuleService.updateCompletionTime(
        dbUser, DbAccessModuleName.PUBLICATION_CONFIRMATION, now);

    // this is bypassed
    accessModuleService.updateBypassTime(
        dbUser.getUserId(), DbAccessModuleName.DATA_USER_CODE_OF_CONDUCT, true);

    // expiring in 1 day (plus some) will trigger the 1-day warning

    final Duration oneDayPlusSome = daysPlusSome(1);
    final Instant expirationTime = PROVIDED_CLOCK.instant().plus(oneDayPlusSome);
    accessModuleService.updateCompletionTime(
        dbUser, DbAccessModuleName.RT_COMPLIANCE_TRAINING, willExpireAfter(oneDayPlusSome));

    userService.maybeSendAccessTierExpirationEmails(dbUser);

    verify(mailService)
        .alertUserAccessTierWarningThreshold(dbUser, 1, expirationTime, REGISTERED_TIER_SHORT_NAME);
  }

  // bypass times are not relevant to expiration emails

  @Test
  public void test_maybeSendAccessTierExpirationEmails_expiring_1_with_older_bypass()
      throws MessagingException {
    // these are up to date
    final Timestamp now = new Timestamp(PROVIDED_CLOCK.millis());
    accessModuleService.updateCompletionTime(dbUser, DbAccessModuleName.PROFILE_CONFIRMATION, now);
    accessModuleService.updateCompletionTime(
        dbUser, DbAccessModuleName.PUBLICATION_CONFIRMATION, now);

    // expiring in 1 day (plus some) will trigger the 1-day warning

    final Duration oneDayPlusSome = daysPlusSome(1);
    final Instant expirationTime = PROVIDED_CLOCK.instant().plus(oneDayPlusSome);
    accessModuleService.updateCompletionTime(
        dbUser, DbAccessModuleName.RT_COMPLIANCE_TRAINING, willExpireAfter(oneDayPlusSome));

    // a bypass which would "expire" in 30 days does NOT trigger a 30-day warning
    accessModuleService.updateBypassTime(
        dbUser.getUserId(), DbAccessModuleName.DATA_USER_CODE_OF_CONDUCT, true);

    userService.maybeSendAccessTierExpirationEmails(dbUser);

    verify(mailService)
        .alertUserAccessTierWarningThreshold(dbUser, 1, expirationTime, REGISTERED_TIER_SHORT_NAME);
  }

  // we do not send an email if the expiration time is within the day.
  // we sent one yesterday for 1 day already, and we will send another once it actually expires.

  @Test
  public void test_maybeSendAccessTierExpirationEmails_expiring_today() {
    // these are up to date
    final Timestamp now = new Timestamp(PROVIDED_CLOCK.millis());
    accessModuleService.updateCompletionTime(dbUser, DbAccessModuleName.PROFILE_CONFIRMATION, now);
    accessModuleService.updateCompletionTime(
        dbUser, DbAccessModuleName.PUBLICATION_CONFIRMATION, now);
    accessModuleService.updateCompletionTime(
        dbUser, DbAccessModuleName.DATA_USER_CODE_OF_CONDUCT, now);

    // a completion requirement for DUCC
    dbUser.setDuccAgreement(signCurrentDucc(dbUser));

    // expiring in .5 days will not trigger an email

    final Duration halfDay = Duration.ofHours(12);
    accessModuleService.updateCompletionTime(
        dbUser, DbAccessModuleName.RT_COMPLIANCE_TRAINING, willExpireAfter(halfDay));

    userService.maybeSendAccessTierExpirationEmails(dbUser);

    verifyNoInteractions(mailService);
  }

  @Test
  public void test_maybeSendAccessTierExpirationEmail_expiring_30() throws MessagingException {
    // these are up to date
    final Timestamp now = new Timestamp(PROVIDED_CLOCK.millis());
    accessModuleService.updateCompletionTime(dbUser, DbAccessModuleName.PROFILE_CONFIRMATION, now);
    accessModuleService.updateCompletionTime(
        dbUser, DbAccessModuleName.PUBLICATION_CONFIRMATION, now);
    accessModuleService.updateCompletionTime(
        dbUser, DbAccessModuleName.DATA_USER_CODE_OF_CONDUCT, now);

    // a completion requirement for DUCC
    dbUser.setDuccAgreement(signCurrentDucc(dbUser));

    // expiring in 30 days (plus) will trigger the 30-day warning

    final Duration thirtyPlus = daysPlusSome(30);
    final Instant expirationTime = PROVIDED_CLOCK.instant().plus(thirtyPlus);
    accessModuleService.updateCompletionTime(
        dbUser, DbAccessModuleName.RT_COMPLIANCE_TRAINING, willExpireAfter(thirtyPlus));

    userService.maybeSendAccessTierExpirationEmails(dbUser);

    verify(mailService)
        .alertUserAccessTierWarningThreshold(
            dbUser, 30, expirationTime, REGISTERED_TIER_SHORT_NAME);
  }

  @Test
  public void test_maybeSendAccessTierExpirationEmails_expiring_31() {
    // these are up to date
    final Timestamp now = new Timestamp(PROVIDED_CLOCK.millis());
    accessModuleService.updateCompletionTime(dbUser, DbAccessModuleName.PROFILE_CONFIRMATION, now);
    accessModuleService.updateCompletionTime(
        dbUser, DbAccessModuleName.PUBLICATION_CONFIRMATION, now);
    accessModuleService.updateCompletionTime(
        dbUser, DbAccessModuleName.DATA_USER_CODE_OF_CONDUCT, now);

    // a completion requirement for DUCC
    dbUser.setDuccAgreement(signCurrentDucc(dbUser));

    // expiring in 31 days (plus) will not trigger a warning
    accessModuleService.updateCompletionTime(
        dbUser, DbAccessModuleName.RT_COMPLIANCE_TRAINING, willExpireAfter(daysPlusSome(31)));

    userService.maybeSendAccessTierExpirationEmails(dbUser);

    verifyNoInteractions(mailService);
  }

  // 15 days is sooner, so that's the email we send rather than 30

  @Test
  public void test_maybeSendAccessTierExpirationEmails_expiring_15_and_30()
      throws MessagingException {
    // these are up to date
    final Timestamp now = new Timestamp(PROVIDED_CLOCK.millis());
    accessModuleService.updateCompletionTime(dbUser, DbAccessModuleName.PROFILE_CONFIRMATION, now);
    accessModuleService.updateCompletionTime(
        dbUser, DbAccessModuleName.PUBLICATION_CONFIRMATION, now);

    // a completion requirement for DUCC
    dbUser.setDuccAgreement(signCurrentDucc(dbUser));

    // expiring in 30 days (plus) would trigger the 30-day warning...
    final Duration thirtyPlus = daysPlusSome(30);
    final Instant expirationTime30 = PROVIDED_CLOCK.instant().plus(thirtyPlus);
    accessModuleService.updateCompletionTime(
        dbUser, DbAccessModuleName.RT_COMPLIANCE_TRAINING, willExpireAfter(thirtyPlus));

    // but 15 days (plus) is sooner, so trigger 15 instead
    final Duration fifteenPlus = daysPlusSome(15);
    final Instant expirationTime15 = PROVIDED_CLOCK.instant().plus(fifteenPlus);
    accessModuleService.updateCompletionTime(
        dbUser, DbAccessModuleName.DATA_USER_CODE_OF_CONDUCT, willExpireAfter(fifteenPlus));

    userService.maybeSendAccessTierExpirationEmails(dbUser);

    verify(mailService)
        .alertUserAccessTierWarningThreshold(
            dbUser, 15, expirationTime15, REGISTERED_TIER_SHORT_NAME);
    verify(mailService, never())
        .alertUserAccessTierWarningThreshold(
            dbUser, 30, expirationTime30, REGISTERED_TIER_SHORT_NAME);
  }

  // 14 days is sooner than 15, but 14 days is not one of our email warning thresholds
  // so we send no email

  @Test
  public void test_maybeSendAccessTierExpirationEmails_expiring_14_and_15() {
    // these are up to date
    final Timestamp now = new Timestamp(PROVIDED_CLOCK.millis());
    accessModuleService.updateCompletionTime(dbUser, DbAccessModuleName.PROFILE_CONFIRMATION, now);
    accessModuleService.updateCompletionTime(
        dbUser, DbAccessModuleName.PUBLICATION_CONFIRMATION, now);

    // a completion requirement for DUCC
    dbUser.setDuccAgreement(signCurrentDucc(dbUser));

    // expiring in 15 days (plus) would trigger the 15-day warning...
    accessModuleService.updateCompletionTime(
        dbUser, DbAccessModuleName.RT_COMPLIANCE_TRAINING, willExpireAfter(daysPlusSome(15)));

    // but 14 days (plus) is sooner, so no email is sent
    accessModuleService.updateCompletionTime(
        dbUser, DbAccessModuleName.DATA_USER_CODE_OF_CONDUCT, willExpireAfter(daysPlusSome(14)));

    userService.maybeSendAccessTierExpirationEmails(dbUser);

    verifyNoInteractions(mailService);
  }

  @Test
  public void test_maybeSendAccessTierExpirationEmails_expired_rt() throws MessagingException {
    // these are up to date
    final Timestamp now = new Timestamp(PROVIDED_CLOCK.millis());
    accessModuleService.updateCompletionTime(dbUser, DbAccessModuleName.PROFILE_CONFIRMATION, now);
    accessModuleService.updateCompletionTime(
        dbUser, DbAccessModuleName.PUBLICATION_CONFIRMATION, now);
    accessModuleService.updateCompletionTime(
        dbUser, DbAccessModuleName.DATA_USER_CODE_OF_CONDUCT, now);

    // a completion requirement for DUCC
    dbUser.setDuccAgreement(signCurrentDucc(dbUser));

    // but this is expired
    final Duration oneHour = Duration.ofHours(1);
    final Instant expirationTime = PROVIDED_CLOCK.instant().minus(oneHour);
    accessModuleService.updateCompletionTime(
        dbUser, DbAccessModuleName.RT_COMPLIANCE_TRAINING, expiredBy(oneHour));

    userService.maybeSendAccessTierExpirationEmails(dbUser);

    // Registered Tier Expiration Email is sent
    verify(mailService)
        .alertUserAccessTierExpiration(dbUser, expirationTime, REGISTERED_TIER_SHORT_NAME);

    verify(mailService, never())
        .alertUserAccessTierExpiration(any(), any(), eq(CONTROLLED_TIER_SHORT_NAME));

    // No expiring soon emails are sent
    verify(mailService, never())
        .alertUserAccessTierWarningThreshold(any(), anyLong(), any(), any());
  }

  @Test
  public void test_maybeSendAccessTierExpirationEmails_expired_ct() throws MessagingException {
    // but this is expired
    final Duration oneHour = Duration.ofHours(1);
    final Instant expirationTime = PROVIDED_CLOCK.instant().minus(oneHour);
    accessModuleService.updateCompletionTime(
        dbUser, DbAccessModuleName.CT_COMPLIANCE_TRAINING, expiredBy(oneHour));

    userService.maybeSendAccessTierExpirationEmails(dbUser);

    // Controlled Tier Expiration Email is sent
    verify(mailService)
        .alertUserAccessTierExpiration(dbUser, expirationTime, CONTROLLED_TIER_SHORT_NAME);

    // Registered Tier Expiration Email is not sent
    verify(mailService, never())
        .alertUserAccessTierExpiration(any(), any(), eq(REGISTERED_TIER_SHORT_NAME));

    // No expiring soon emails are sent
    verify(mailService, never())
        .alertUserAccessTierWarningThreshold(any(), anyLong(), any(), any());
  }

  // don't send an email if we have been expired for more than a day
  // because we sent the expiration email yesterday

  @Test
  public void test_maybeSendAccessTierExpirationEmails_extra_expired() {
    // these are up to date
    final Timestamp now = new Timestamp(PROVIDED_CLOCK.millis());
    accessModuleService.updateCompletionTime(dbUser, DbAccessModuleName.PROFILE_CONFIRMATION, now);
    accessModuleService.updateCompletionTime(
        dbUser, DbAccessModuleName.PUBLICATION_CONFIRMATION, now);
    accessModuleService.updateCompletionTime(
        dbUser, DbAccessModuleName.DATA_USER_CODE_OF_CONDUCT, now);

    // a completion requirement for DUCC
    dbUser.setDuccAgreement(signCurrentDucc(dbUser));

    // but this expired yesterday

    final Instant aYearAgo = PROVIDED_CLOCK.instant().minus(EXPIRATION_DAYS, ChronoUnit.DAYS);
    final Timestamp extraExpired =
        Timestamp.from(aYearAgo.minus(Duration.ofDays(1)).minus(Duration.ofHours(1)));

    accessModuleService.updateCompletionTime(
        dbUser, DbAccessModuleName.RT_COMPLIANCE_TRAINING, extraExpired);

    userService.maybeSendAccessTierExpirationEmails(dbUser);

    verifyNoInteractions(mailService);
  }

  @Test
  public void testInstitutionAddressInvalid_emailDomainsNotMatch() {
    testUnregistration(
        user -> {
          institutionService.updateInstitution(
              institution.getShortName(),
              institution.tierConfigs(
                  ImmutableList.of(rtTierConfig.emailDomains(ImmutableList.of("test.com")))));
          return userDao.save(user);
        });
  }

  @Test
  public void testInstitutionAddressInvalid_emailDomainsNull() {
    testUnregistration(
        user -> {
          institutionService.updateInstitution(
              institution.getShortName(),
              institution.tierConfigs(ImmutableList.of(rtTierConfig.emailDomains(null))));
          return userDao.save(user);
        });
  }

  @Test
  public void testInstitutionAddressInvalid_emailDomainsTierRequirementEmpty() {
    testUnregistration(
        user -> {
          institutionService.updateInstitution(
              institution.getShortName(), institution.tierConfigs(ImmutableList.of()));
          return userDao.save(user);
        });
  }

  @Test
  public void testInstitutionAddressInvalid_emailAddressMatch() {
    institutionService.updateInstitution(
        institution.getShortName(),
        institution.tierConfigs(
            ImmutableList.of(
                rtTierConfig
                    .membershipRequirement(InstitutionMembershipRequirement.ADDRESSES)
                    .addEmailAddressesItem(dbUser.getContactEmail())
                    .emailDomains(null))));
    dbUser = updateUserWithRetries(registerUserNow);
    assertRegisteredTierEnabled(dbUser);
  }

  @Test
  public void testInstitutionAddressInvalid_emailAddressNotMatch() {
    testUnregistration(
        user -> {
          institutionService.updateInstitution(
              institution.getShortName(),
              institution.tierConfigs(
                  ImmutableList.of(
                      rtTierConfig
                          .membershipRequirement(InstitutionMembershipRequirement.ADDRESSES)
                          .addEmailAddressesItem("randmom@email.com")
                          .emailDomains(null))));
          return userDao.save(user);
        });
  }

  @Test
  public void testInstitutionAddressInvalid_emailAddressNull() {
    testUnregistration(
        user -> {
          institutionService.updateInstitution(
              institution.getShortName(),
              institution.tierConfigs(
                  ImmutableList.of(rtTierConfig.emailDomains(null).emailAddresses(null))));
          return userDao.save(user);
        });
  }

  @Test
  public void testInstitutionAddressInvalid_emailAddressTierConfigNotExist() {
    testUnregistration(
        user -> {
          institutionService.updateInstitution(
              institution.getShortName(), institution.tierConfigs(ImmutableList.of()));
          return userDao.save(user);
        });
  }

  @Test
  public void testInstitutionRequirement_optionalEra() {
    assertThat(userAccessTierDao.findAll()).isEmpty();
    providedWorkbenchConfig.access.enableEraCommons = true;
    providedWorkbenchConfig.access.enableRasLoginGovLinking = true;

    institutionService.updateInstitution(
        institution.getShortName(),
        institution.tierConfigs(ImmutableList.of(rtTierConfig.eraRequired(true))));
    dbUser = updateUserWithRetries(registerUserNow);
    assertRegisteredTierEnabled(dbUser);

    // Now make user eRA not complete, expect user removed from Registered tier;
    accessModuleService.updateBypassTime(dbUser.getUserId(), DbAccessModuleName.ERA_COMMONS, false);
    accessModuleService.updateCompletionTime(dbUser, DbAccessModuleName.ERA_COMMONS, null);
    dbUser = updateUserAccessTiers();
    assertRegisteredTierDisabled(dbUser);

    // Make eRA is optional for that institution, verify user become registered
    institutionService.updateInstitution(
        institution.getShortName(),
        institution.tierConfigs(ImmutableList.of(rtTierConfig.eraRequired(false))));
    dbUser = updateUserAccessTiers();
    assertRegisteredTierEnabled(dbUser);
  }

  @Test
  public void testInstitutionRequirement_optionalEra_loginGovFlagDisabled() {
    // User not complete eRA, but it is optional for that institution
    assertThat(userAccessTierDao.findAll()).isEmpty();
    providedWorkbenchConfig.access.enableEraCommons = true;
    providedWorkbenchConfig.access.enableRasLoginGovLinking = true;
    updateUserWithRetries(registerUserNow);
    institutionService.updateInstitution(
        institution.getShortName(),
        institution.tierConfigs(ImmutableList.of(rtTierConfig.eraRequired(false))));
    accessModuleService.updateBypassTime(dbUser.getUserId(), DbAccessModuleName.ERA_COMMONS, false);
    accessModuleService.updateCompletionTime(dbUser, DbAccessModuleName.ERA_COMMONS, null);
    dbUser = updateUserAccessTiers();
    assertRegisteredTierEnabled(dbUser);

    // Now login.gov flag disabled, eRA is always required.
    providedWorkbenchConfig.access.enableRasLoginGovLinking = false;
    dbUser = updateUserAccessTiers();
    assertRegisteredTierDisabled(dbUser);
  }

  @Test
  public void testInstitutionRequirement_optionalEra_loginGovFlagEnabled_eRAFlagDisabled() {
    // When eRA flag is disabled, that means user completed eRA Commons
    assertThat(userAccessTierDao.findAll()).isEmpty();
    updateUserWithRetries(registerUserNow);
    providedWorkbenchConfig.access.enableEraCommons = false;
    providedWorkbenchConfig.access.enableRasLoginGovLinking = false;
    institutionService.updateInstitution(
        institution.getShortName(),
        institution.tierConfigs(ImmutableList.of(rtTierConfig.eraRequired(true))));
    accessModuleService.updateBypassTime(dbUser.getUserId(), DbAccessModuleName.ERA_COMMONS, false);
    accessModuleService.updateCompletionTime(dbUser, DbAccessModuleName.ERA_COMMONS, null);
    dbUser = updateUserAccessTiers();
    assertRegisteredTierEnabled(dbUser);
  }

  @Test
  public void testRasLinkNotComplete() {
    assertThat(userAccessTierDao.findAll()).isEmpty();
    providedWorkbenchConfig.access.enableRasLoginGovLinking = true;
    dbUser = updateUserWithRetries(registerUserNow);
    assertRegisteredTierEnabled(dbUser);

    // Incomplete RAS module, expect user removed from Registered tier;
    accessModuleService.updateBypassTime(
        dbUser.getUserId(), DbAccessModuleName.IDENTITY, false);
    dbUser = updateUserAccessTiers();
    assertRegisteredTierDisabled(dbUser);

    // Complete RAS Linking, verify user become registered
    accessModuleService.updateCompletionTime(
        dbUser, DbAccessModuleName.IDENTITY, new Timestamp(PROVIDED_CLOCK.millis()));
    dbUser = updateUserAccessTiers();
    assertRegisteredTierEnabled(dbUser);
  }

  @Test
  public void test_updateUserWithRetries_addToControlledTier() {
    assertThat(userAccessTierDao.findAll()).isEmpty();

    dbUser = updateUserWithRetries(this::completeRTAndCTRequirements);
    assertRegisteredTierEnabled(dbUser);
    assertControlledTierEnabled(dbUser);
  }

  @Test
  public void test_updateUserWithRetries_completeCTRequirementsOnly() {
    assertThat(userAccessTierDao.findAll()).isEmpty();

    dbUser = updateUserWithRetries(this::completeCTRequirements);
    assertUserNotInAccessTier(dbUser, registeredTier);
    assertUserNotInAccessTier(dbUser, controlledTier);
  }

  @Test
  public void test_updateUserWithRetries_inCompleteCTRequirements_CTCompliance() {
    assertThat(userAccessTierDao.findAll()).isEmpty();

    dbUser = completeRTAndCTRequirements(dbUser);

    accessModuleService.updateBypassTime(
        dbUser.getUserId(), DbAccessModuleName.CT_COMPLIANCE_TRAINING, false);
    dbUser = updateUserAccessTiers();

    assertRegisteredTierEnabled(dbUser);
    assertUserNotInAccessTier(dbUser, controlledTier);
  }

  @Test
  public void test_updateUserWithRetries_inCompleteCTRequirements_eraRequired() {
    assertThat(userAccessTierDao.findAll()).isEmpty();

    dbUser = completeRTAndCTRequirements(dbUser);

    // Setting eraRequired to false for RT just so user can still have access to RT even after NOT
    // bypassing era
    rtTierConfig.setEraRequired(false);
    updateInstitutionTier(rtTierConfig);

    ctTierConfig.setEraRequired(true);
    updateInstitutionTier(ctTierConfig);

    accessModuleService.updateBypassTime(dbUser.getUserId(), DbAccessModuleName.ERA_COMMONS, false);
    dbUser = updateUserAccessTiers();

    assertRegisteredTierEnabled(dbUser);
    assertUserNotInAccessTier(dbUser, controlledTier);
  }

  @Test
  public void test_updateUserWithRetries_eraNotRequiredForTiers() {
    assertThat(userAccessTierDao.findAll()).isEmpty();

    dbUser = completeRTAndCTRequirements(dbUser);
    rtTierConfig.setEraRequired(false);
    updateInstitutionTier(rtTierConfig);
    ctTierConfig.setEraRequired(false);
    updateInstitutionTier(ctTierConfig);

    accessModuleService.updateBypassTime(dbUser.getUserId(), DbAccessModuleName.ERA_COMMONS, false);
    dbUser = updateUserAccessTiers();

    assertRegisteredTierEnabled(dbUser);
    assertControlledTierEnabled(dbUser);
  }

  @Test
  public void testInstitutionRequirement_rtEraDoesNotAffectCTEra() {
    assertThat(userAccessTierDao.findAll()).isEmpty();
    providedWorkbenchConfig.access.enableEraCommons = true;
    providedWorkbenchConfig.access.enableRasLoginGovLinking = true;

    dbUser = completeRTAndCTRequirements(dbUser);
    ctTierConfig.setEraRequired(true);
    updateInstitutionTier(ctTierConfig);
    dbUser = updateUserAccessTiers();

    assertRegisteredTierEnabled(dbUser);
    assertControlledTierEnabled(dbUser);

    ctTierConfig.setEraRequired(false);
    updateInstitutionTier(ctTierConfig);
    dbUser = updateUserAccessTiers();

    assertRegisteredTierEnabled(dbUser);
    assertControlledTierEnabled(dbUser);
  }

  @Test
  public void test_updateUserWithRetries_emailValidForRTButNotValidForCT() {
    assertThat(userAccessTierDao.findAll()).isEmpty();
    dbUser = completeRTAndCTRequirements(dbUser);

    ctTierConfig.setEmailDomains(Arrays.asList("fakeDomain.com"));
    updateInstitutionTier(ctTierConfig);

    dbUser = updateUserAccessTiers();

    assertRegisteredTierEnabled(dbUser);
    assertUserNotInAccessTier(dbUser, controlledTier);
  }

  @Test
  public void test_updateUserWithRetries_updateInvalidEmailForCT() {
    test_updateUserWithRetries_emailValidForRTButNotValidForCT();

    ctTierConfig.setEmailDomains(Arrays.asList("domain.com"));
    updateInstitutionTier(ctTierConfig);

    dbUser = updateUserAccessTiers();

    assertRegisteredTierEnabled(dbUser);
    assertControlledTierEnabled(dbUser);
  }

  @Test
  public void test_updateUserWithRetries_didNotSignCTAgreement() {
    assertThat(userAccessTierDao.findAll()).isEmpty();
    dbUser = completeRTAndCTRequirements(dbUser);
    Institution institution = institutionService.getByUser(dbUser).get();
    institution
        .getTierConfigs()
        .removeIf(
            tier ->
                tier.getAccessTierShortName().equals(AccessTierService.CONTROLLED_TIER_SHORT_NAME));
    institutionService.updateInstitution(institution.getShortName(), institution);

    dbUser = updateUserAccessTiers();

    assertRegisteredTierEnabled(dbUser);
    assertUserNotInAccessTier(dbUser, controlledTier);
  }

  @Test
  public void test_updateUserWithRetries_eraFFisOff_CT() {
    providedWorkbenchConfig.access.enableEraCommons = false;
    assertThat(userAccessTierDao.findAll()).isEmpty();

    dbUser = completeRTAndCTRequirements(dbUser);

    ctTierConfig.setEraRequired(true);
    updateInstitutionTier(ctTierConfig);

    dbUser = updateUserAccessTiers();

    assertRegisteredTierEnabled(dbUser);
    assertControlledTierEnabled(dbUser);

    accessModuleService.updateBypassTime(dbUser.getUserId(), DbAccessModuleName.ERA_COMMONS, false);
    dbUser = updateUserAccessTiers();

    assertRegisteredTierEnabled(dbUser);
    assertControlledTierEnabled(dbUser);
  }

  @Test
  public void test_updateUserWithRetries_ct_complianceTrainingFFisOff_CT() {
    providedWorkbenchConfig.access.enableComplianceTraining = false;
    assertThat(userAccessTierDao.findAll()).isEmpty();

    dbUser = completeRTAndCTRequirements(dbUser);
    dbUser = updateUserAccessTiers();

    assertRegisteredTierEnabled(dbUser);
    assertControlledTierEnabled(dbUser);

    accessModuleService.updateBypassTime(
        dbUser.getUserId(), DbAccessModuleName.CT_COMPLIANCE_TRAINING, false);
    dbUser = updateUserAccessTiers();

    assertRegisteredTierEnabled(dbUser);
    assertControlledTierEnabled(dbUser);
  }

  @Test
  public void test_updateUserWithRetries_noCT() {
    // CT does not exist anywhere
    assertThat(userAccessTierDao.findAll()).isEmpty();

    dbUser = registerUser(new Timestamp(PROVIDED_CLOCK.millis()), dbUser);
    TestMockFactory.removeControlledTierForTests(accessTierDao);
    removeCTConfigFromInstitution();

    dbUser = updateUserAccessTiers();

    assertRegisteredTierEnabled(dbUser);
    assertUserNotInAccessTier(dbUser, controlledTier);
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

  private DbUserCodeOfConductAgreement signDucc(DbUser dbUser, int version) {
    return TestMockFactory.createDuccAgreement(
        dbUser, version, new Timestamp(PROVIDED_CLOCK.millis()));
  }

  private DbUserCodeOfConductAgreement signCurrentDucc(DbUser dbUser) {
    int aValidVersion = providedWorkbenchConfig.access.currentDuccVersions.get(0);
    return signDucc(dbUser, aValidVersion);
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

  // we can trim the signatures since we always call these in the same way

  private DbUser updateUserWithRetries(Function<DbUser, DbUser> userModifier) {
    return userService.updateUserWithRetries(userModifier, dbUser, Agent.asUser(dbUser));
  }

  private DbUser updateUserAccessTiers() {
    return accessSyncService.updateUserAccessTiers(dbUser, Agent.asUser(dbUser));
  }

  private void updateInstitutionTier(InstitutionTierConfig updatedTierConfig) {
    Institution institution = institutionService.getByUser(dbUser).get();
    institution
        .getTierConfigs()
        .removeIf(
            tierConfig ->
                tierConfig
                    .getAccessTierShortName()
                    .equals(updatedTierConfig.getAccessTierShortName()));
    institution.addTierConfigsItem(updatedTierConfig);
    institutionService.updateInstitution(institution.getShortName(), institution);
  }

  private void assertRegisteredTierEnabled(DbUser dbUser) {
    assertRegisteredTierMembershipWithStatus(dbUser, TierAccessStatus.ENABLED);
  }

  private void assertRegisteredTierDisabled(DbUser dbUser) {
    assertRegisteredTierMembershipWithStatus(dbUser, TierAccessStatus.DISABLED);
  }

  private void assertControlledTierEnabled(DbUser dbUser) {
    assertControlledTierMembershipWithStatus(dbUser, TierAccessStatus.ENABLED);
  }

  private void assertUserNotInAccessTier(DbUser dbUser, DbAccessTier accessTier) {
    // if not present, we're done
    // if present: assert that the row is disabled
    Optional<DbUserAccessTier> userAccessMaybe =
        userAccessTierDao.getByUserAndAccessTier(dbUser, accessTier);
    userAccessMaybe.ifPresent(
        userAccess ->
            assertThat(userAccess.getTierAccessStatusEnum()).isEqualTo(TierAccessStatus.DISABLED));
  }

  private void assertTierMembershipWithStatus(
      DbAccessTier dbAccessTier, DbUser dbUser, TierAccessStatus status) {
    Optional<DbUserAccessTier> userAccessMaybe =
        userAccessTierDao.getByUserAndAccessTier(dbUser, dbAccessTier);
    assertThat(userAccessMaybe).isPresent();
    assertThat(userAccessMaybe.get().getTierAccessStatusEnum()).isEqualTo(status);
  }

  private void assertRegisteredTierMembershipWithStatus(DbUser dbUser, TierAccessStatus status) {
    assertTierMembershipWithStatus(registeredTier, dbUser, status);
  }

  private void assertControlledTierMembershipWithStatus(DbUser dbUser, TierAccessStatus status) {
    assertTierMembershipWithStatus(controlledTier, dbUser, status);
  }

  private DbUser registerUser(Timestamp timestamp, DbUser user) {
    // shouldUserBeRegistered logic:
    //    return !user.getDisabled()
    //        && complianceTrainingCompliant
    //        && eraCommonsCompliant
    //        && twoFactorAuthComplete
    //        && duccCompliant
    //        && isPublicationsCompliant
    //        && isProfileCompliant
    //        && institutionEmailValid
    accessModuleService.updateBypassTime(
        user.getUserId(), DbAccessModuleName.RT_COMPLIANCE_TRAINING, true);
    accessModuleService.updateBypassTime(user.getUserId(), DbAccessModuleName.ERA_COMMONS, true);
    accessModuleService.updateBypassTime(
        user.getUserId(), DbAccessModuleName.TWO_FACTOR_AUTH, true);
    accessModuleService.updateBypassTime(
        user.getUserId(), DbAccessModuleName.DATA_USER_CODE_OF_CONDUCT, true);
    accessModuleService.updateBypassTime(user.getUserId(), DbAccessModuleName.IDENTITY, true);
    accessModuleService.updateBypassTime(
        user.getUserId(), DbAccessModuleName.PUBLICATION_CONFIRMATION, true);
    accessModuleService.updateBypassTime(
        user.getUserId(), DbAccessModuleName.PROFILE_CONFIRMATION, true);

    createAffiliation(user);
    return user;
  }

  private void addCTConfigToInstitution(Institution institution) {
    institution.addTierConfigsItem(
        ctTierConfig
            .eraRequired(true)
            .membershipRequirement(InstitutionMembershipRequirement.DOMAINS)
            .addEmailDomainsItem("domain.com"));
    institutionService.updateInstitution(institution.getShortName(), institution);
  }

  private void removeCTConfigFromInstitution() {
    Institution institution = institutionService.getByUser(dbUser).get();
    institution
        .getTierConfigs()
        .removeIf(
            tier ->
                tier.getAccessTierShortName().equals(AccessTierService.CONTROLLED_TIER_SHORT_NAME));
    institutionService.updateInstitution(institution.getShortName(), institution);
  }

  private DbUser completeCTRequirements(DbUser user) {
    addCTConfigToInstitution(institutionService.getByUser(user).get());
    accessModuleService.updateBypassTime(user.getUserId(), DbAccessModuleName.ERA_COMMONS, true);
    accessModuleService.updateBypassTime(
        user.getUserId(), DbAccessModuleName.CT_COMPLIANCE_TRAINING, true);
    return user;
  }

  private DbUser completeRTAndCTRequirements(DbUser user) {
    return completeCTRequirements(registerUser(new Timestamp(PROVIDED_CLOCK.millis()), user));
  }

  private void createAffiliation(final DbUser user) {
    final DbInstitution inst =
        institutionService.getDbInstitutionOrThrow(institution.getShortName());
    if (institutionService.getByUser(user).isPresent()) {
      return;
    }
    final DbVerifiedInstitutionalAffiliation affiliation =
        new DbVerifiedInstitutionalAffiliation()
            .setUser(user)
            .setInstitution(inst)
            .setInstitutionalRoleEnum(InstitutionalRole.FELLOW);
    verifiedInstitutionalAffiliationDao.save(affiliation);
  }
}
