package org.pmiops.workbench.db.dao;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.pmiops.workbench.access.AccessTierService.REGISTERED_TIER_SHORT_NAME;

import com.google.api.services.directory.model.User;
import com.google.common.collect.ImmutableMap;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.FakeJpaDateTimeConfiguration;
import org.pmiops.workbench.access.AccessModuleService;
import org.pmiops.workbench.access.AccessModuleServiceImpl;
import org.pmiops.workbench.access.AccessTierServiceImpl;
import org.pmiops.workbench.access.UserAccessModuleMapperImpl;
import org.pmiops.workbench.actionaudit.Agent;
import org.pmiops.workbench.actionaudit.auditors.UserServiceAuditor;
import org.pmiops.workbench.compliance.ComplianceService;
import org.pmiops.workbench.compliance.ComplianceService.BadgeName;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.model.DbAccessModule;
import org.pmiops.workbench.db.model.DbAccessModule.AccessModuleName;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserCodeOfConductAgreement;
import org.pmiops.workbench.db.model.DbUserTermsOfService;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudNihStatus;
import org.pmiops.workbench.google.DirectoryService;
import org.pmiops.workbench.institution.InstitutionService;
import org.pmiops.workbench.mail.MailService;
import org.pmiops.workbench.model.Authority;
import org.pmiops.workbench.model.Institution;
import org.pmiops.workbench.moodle.ApiException;
import org.pmiops.workbench.moodle.model.BadgeDetailsV2;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.testconfig.UserServiceTestConfiguration;
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
public class UserServiceTest {

  private static final String USERNAME = "abc@fake-research-aou.org";

  // An arbitrary timestamp to use as the anchor time for access module test cases.
  private static final Instant START_INSTANT = FakeClockConfiguration.NOW.toInstant();
  private static final int CLOCK_INCREMENT_MILLIS = 1000;
  private static DbUser providedDbUser;
  private static WorkbenchConfig providedWorkbenchConfig;
  private static DbAccessTier registeredTier;
  private static List<DbAccessModule> accessModules;

  @MockBean private FireCloudService mockFireCloudService;
  @MockBean private ComplianceService mockComplianceService;
  @MockBean private DirectoryService mockDirectoryService;
  @MockBean private UserServiceAuditor mockUserServiceAuditAdapter;
  @MockBean private UserTermsOfServiceDao mockUserTermsOfServiceDao;
  @MockBean private InstitutionService mockInstitutionService;

  @Autowired private UserService userService;
  @Autowired private UserDao userDao;
  @Autowired private AccessTierDao accessTierDao;
  @Autowired private AccessModuleDao accessModuleDao;
  @Autowired private UserAccessModuleDao userAccessModuleDao;
  @Autowired private FakeClock fakeClock;

  // we need the full service for some tests and mocks for others
  @SpyBean private AccessModuleService accessModuleService;

  @Import({
    FakeClockConfiguration.class,
    FakeJpaDateTimeConfiguration.class,
    UserServiceTestConfiguration.class,
    AccessTierServiceImpl.class,
    AccessModuleServiceImpl.class,
    CommonMappers.class,
    UserAccessModuleMapperImpl.class,
  })
  @MockBean({
    MailService.class,
  })
  @TestConfiguration
  static class Configuration {
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

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public List<DbAccessModule> getDbAccessModules() {
      return accessModules;
    }
  }

  @BeforeEach
  public void setUp() {
    DbUser user = new DbUser();
    user.setUsername(USERNAME);
    user = userDao.save(user);
    providedDbUser = user;

    providedWorkbenchConfig = WorkbenchConfig.createEmptyConfig();
    providedWorkbenchConfig.accessRenewal.expiryDays = (long) 365;
    providedWorkbenchConfig.access.enableComplianceTraining = true;
    providedWorkbenchConfig.access.enableEraCommons = true;

    // key UserService logic depends on the existence of the Registered Tier
    registeredTier = TestMockFactory.createRegisteredTierForTests(accessTierDao);

    accessModules = TestMockFactory.createAccessModules(accessModuleDao);
    Institution institution = new Institution();
    when(mockInstitutionService.getByUser(user)).thenReturn(Optional.of(institution));
    when(mockInstitutionService.eRaRequiredForTier(institution, REGISTERED_TIER_SHORT_NAME))
        .thenReturn(false);
    when(mockInstitutionService.validateInstitutionalEmail(
            institution, user.getContactEmail(), REGISTERED_TIER_SHORT_NAME))
        .thenReturn(true);
  }

  @Test
  public void testSyncComplianceTrainingStatusV2() throws Exception {
    long issued = fakeClock.instant().getEpochSecond() - 100;

    Map<BadgeName, BadgeDetailsV2> userBadgesByName = new HashMap<>();
    userBadgesByName.put(
        BadgeName.REGISTERED_TIER_TRAINING, new BadgeDetailsV2().lastissued(issued).valid(true));

    when(mockComplianceService.getUserBadgesByBadgeName(USERNAME)).thenReturn(userBadgesByName);

    userService.syncComplianceTrainingStatusV2();

    // The user should be updated in the database with a non-empty completion.
    DbUser user = userDao.findUserByUsername(USERNAME);
    assertModuleCompletionEqual(
        AccessModuleName.RT_COMPLIANCE_TRAINING, user, Timestamp.from(START_INSTANT));

    // Completion timestamp should not change when the method is called again.
    tick();
    userService.syncComplianceTrainingStatusV2();

    assertModuleCompletionEqual(
        AccessModuleName.RT_COMPLIANCE_TRAINING, user, Timestamp.from(START_INSTANT));
  }

  @Test
  public void testUpdateComplianceTrainingStatusV2() throws Exception {
    long issued = fakeClock.instant().getEpochSecond() - 10;
    BadgeDetailsV2 retBadge = new BadgeDetailsV2().lastissued(issued).valid(true);

    Map<BadgeName, BadgeDetailsV2> userBadgesByName = new HashMap<>();
    userBadgesByName.put(BadgeName.REGISTERED_TIER_TRAINING, retBadge);

    when(mockComplianceService.getUserBadgesByBadgeName(USERNAME)).thenReturn(userBadgesByName);

    userService.syncComplianceTrainingStatusV2();

    // The user should be updated in the database with a non-empty completion time.
    DbUser user = userDao.findUserByUsername(USERNAME);
    assertModuleCompletionEqual(
        AccessModuleName.RT_COMPLIANCE_TRAINING, user, Timestamp.from(START_INSTANT));

    // Deprecate the old training.
    retBadge.setValid(false);

    // Completion timestamp should be wiped out by the expiry timestamp passing.
    userService.syncComplianceTrainingStatusV2();
    assertModuleCompletionEqual(AccessModuleName.RT_COMPLIANCE_TRAINING, user, null);

    // The user does a new training.
    retBadge.lastissued(issued + 5).valid(true);

    // Completion and expiry timestamp should be updated.
    userService.syncComplianceTrainingStatusV2();
    assertModuleCompletionEqual(
        AccessModuleName.RT_COMPLIANCE_TRAINING, user, Timestamp.from(START_INSTANT));

    // Time passes, user renews training
    retBadge.lastissued(fakeClock.instant().getEpochSecond() + 1);
    fakeClock.increment(5000);

    // Completion should be updated to the current time.
    userService.syncComplianceTrainingStatusV2();
    assertModuleCompletionEqual(
        AccessModuleName.RT_COMPLIANCE_TRAINING, user, Timestamp.from(fakeClock.instant()));
  }

  @Test
  public void testUpdateComplianceTrainingStatusV2_controlled() throws Exception {
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

    userService.syncComplianceTrainingStatusV2();

    // The user should be updated in the database with a non-empty completion time.
    DbUser user = userDao.findUserByUsername(USERNAME);
    assertModuleCompletionEqual(
        AccessModuleName.RT_COMPLIANCE_TRAINING, user, Timestamp.from(START_INSTANT));
    assertModuleCompletionEqual(
        AccessModuleName.CT_COMPLIANCE_TRAINING, user, Timestamp.from(START_INSTANT));

    ctBadge.lastissued(fakeClock.instant().getEpochSecond() + 1);
    fakeClock.increment(5000);

    // Renewing training updates completion.
    userService.syncComplianceTrainingStatusV2();
    assertModuleCompletionEqual(
        AccessModuleName.RT_COMPLIANCE_TRAINING, user, Timestamp.from(START_INSTANT));
    assertModuleCompletionEqual(
        AccessModuleName.CT_COMPLIANCE_TRAINING, user, Timestamp.from(fakeClock.instant()));
  }

  private void tick() {
    fakeClock.increment(CLOCK_INCREMENT_MILLIS);
  }

  @Test
  public void testSyncComplianceTrainingStatusNullBadgeV2() throws ApiException {
    // When Moodle returns an empty RET badge response, we should clear the completion time.

    DbUser user = userDao.findUserByUsername(USERNAME);
    accessModuleService.updateCompletionTime(
        user, AccessModuleName.RT_COMPLIANCE_TRAINING, new Timestamp(12345));

    // An empty map should be returned when we have no badge information.
    Map<BadgeName, BadgeDetailsV2> userBadgesByName = new HashMap<>();

    when(mockComplianceService.getUserBadgesByBadgeName(USERNAME)).thenReturn(userBadgesByName);

    userService.syncComplianceTrainingStatusV2();
    user = userDao.findUserByUsername(USERNAME);
    assertModuleCompletionEqual(AccessModuleName.RT_COMPLIANCE_TRAINING, user, null);
  }

  @Test
  public void testSyncComplianceTrainingStatusBadgeNotFoundV2() throws ApiException {
    // We should propagate a NOT_FOUND exception from the compliance service.
    when(mockComplianceService.getUserBadgesByBadgeName(USERNAME))
        .thenThrow(
            new org.pmiops.workbench.moodle.ApiException(
                HttpStatus.NOT_FOUND.value(), "user not found"));
    assertThrows(NotFoundException.class, () -> userService.syncComplianceTrainingStatusV2());
  }

  @Test
  public void testSyncComplianceTraining_SkippedForServiceAccountV2() throws ApiException {
    providedWorkbenchConfig.auth.serviceAccountApiUsers.add(USERNAME);
    userService.syncComplianceTrainingStatusV2();
    verifyZeroInteractions(mockComplianceService);
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
    assertModuleCompletionEqual(AccessModuleName.ERA_COMMONS, user, Timestamp.from(START_INSTANT));

    assertThat(user.getEraCommonsLinkExpireTime()).isEqualTo(Timestamp.from(START_INSTANT));
    assertThat(user.getEraCommonsLinkedNihUsername()).isEqualTo("nih-user");

    // Completion timestamp should not change when the method is called again.
    tick();
    userService.syncEraCommonsStatus();

    assertModuleCompletionEqual(AccessModuleName.ERA_COMMONS, user, Timestamp.from(START_INSTANT));
  }

  @Test
  public void testClearsEraCommonsStatus() {
    // Put the test user in a state where eRA commons is completed.
    DbUser testUser = userDao.findUserByUsername(USERNAME);
    testUser.setEraCommonsLinkedNihUsername("nih-user");
    testUser = userDao.save(testUser);

    accessModuleService.updateCompletionTime(
        testUser, AccessModuleName.ERA_COMMONS, Timestamp.from(START_INSTANT));

    userService.syncEraCommonsStatus();

    DbUser retrievedUser = userDao.findUserByUsername(USERNAME);
    assertModuleCompletionEqual(AccessModuleName.ERA_COMMONS, retrievedUser, null);
  }

  @Test
  public void testSyncEraCommonsStatus_lastModified() {
    // User starts without eRA commons.
    Supplier<Timestamp> getLastModified =
        () -> userDao.findUserByUsername(USERNAME).getLastModifiedTime();
    Timestamp modifiedTime0 = getLastModified.get();

    when(mockFireCloudService.getNihStatus())
        .thenReturn(
            new FirecloudNihStatus()
                .linkedNihUsername("nih-user")
                // FireCloud stores the NIH status in seconds, not msecs.
                .linkExpireTime(START_INSTANT.toEpochMilli() / 1000));

    tick();
    userService.syncEraCommonsStatus();
    Timestamp modifiedTime1 = getLastModified.get();
    assertWithMessage(
            "modified time should change when eRA commons status changes, want %s < %s",
            modifiedTime0, modifiedTime1)
        .that(modifiedTime0.before(modifiedTime1))
        .isTrue();

    userService.syncEraCommonsStatus();
    assertWithMessage(
            "modified time should not change on sync, if eRA commons status doesn't change")
        .that(modifiedTime1)
        .isEqualTo(getLastModified.get());
  }

  @Test
  public void testUpdateRasLink() {
    String loginGovName = "loginGov@email.com";
    userService.updateRasLinkLoginGovStatus(loginGovName);
    assertThat(providedDbUser.getRasLinkLoginGovUsername()).isEqualTo(loginGovName);
    assertModuleCompletionEqual(
        AccessModuleName.RAS_LOGIN_GOV, providedDbUser, Timestamp.from(START_INSTANT));
  }

  @Test
  public void testSyncTwoFactorAuthStatus() {
    User googleUser = new User();
    googleUser.setPrimaryEmail(USERNAME);
    googleUser.setIsEnrolledIn2Sv(true);

    when(mockDirectoryService.getUserOrThrow(USERNAME)).thenReturn(googleUser);
    userService.syncTwoFactorAuthStatus();
    // twoFactorAuthCompletionTime should now be set
    DbUser user = userDao.findUserByUsername(USERNAME);
    Timestamp twoFactorAuthCompletionTime =
        getModuleCompletionTime(AccessModuleName.TWO_FACTOR_AUTH, user);
    assertThat(twoFactorAuthCompletionTime).isNotNull();

    // twoFactorAuthCompletionTime should not change when already set
    tick();
    userService.syncTwoFactorAuthStatus();
    assertModuleCompletionEqual(
        AccessModuleName.TWO_FACTOR_AUTH, providedDbUser, twoFactorAuthCompletionTime);

    // unset 2FA in google and check that twoFactorAuthCompletionTime is set to null
    googleUser.setIsEnrolledIn2Sv(false);
    userService.syncTwoFactorAuthStatus();
    user = userDao.findUserByUsername(USERNAME);
    assertModuleCompletionEqual(AccessModuleName.TWO_FACTOR_AUTH, providedDbUser, null);
  }

  private Optional<Instant> nullableTimestampToOptionalInstant(
      @Nullable Timestamp complianceTrainingBypassTime) {
    return Optional.ofNullable(complianceTrainingBypassTime).map(Timestamp::toInstant);
  }

  @Test
  public void testSubmitTermsOfService_illegalTosVersion() {
    // Testing NULL input version
    assertThrows(
        BadRequestException.class,
        () -> {
          userService.submitTermsOfService(
              userDao.findUserByUsername(USERNAME), /* tosVersion */ null);
        });

    // Testing not current term input version
    assertThrows(
        BadRequestException.class,
        () -> {
          userService.submitTermsOfService(
              userDao.findUserByUsername(USERNAME), /* tosVersion */ -1);
        });
  }

  @Test
  public void testSubmitTermsOfService() {
    userService.submitTermsOfService(userDao.findUserByUsername(USERNAME), /* tosVersion */ 1);
    verify(mockUserTermsOfServiceDao).save(any(DbUserTermsOfService.class));
    verify(mockUserServiceAuditAdapter).fireAcknowledgeTermsOfService(any(DbUser.class), eq(1));
  }

  @Test
  public void testSyncDuccVersionStatus_correctVersion() {
    final DbUser user = userDao.findUserByUsername(USERNAME);
    user.setDuccAgreement(signDucc(user, accessModuleService.getCurrentDuccVersion()));
    userDao.save(user);

    userService.syncDuccVersionStatus(user, Agent.asSystem());

    verify(accessModuleService, never()).updateCompletionTime(any(), any(), any());
  }

  @Test
  public void testSyncDuccVersionStatus_incorrectVersion() {
    final DbUser user = userDao.findUserByUsername(USERNAME);
    user.setDuccAgreement(signDucc(user, accessModuleService.getCurrentDuccVersion() - 1));
    userDao.save(user);

    userService.syncDuccVersionStatus(user, Agent.asSystem());

    verify(accessModuleService)
        .updateCompletionTime(user, AccessModuleName.DATA_USER_CODE_OF_CONDUCT, null);
  }

  @Test
  public void testSyncDuccVersionStatus_missing() {
    final DbUser user = userDao.findUserByUsername(USERNAME);

    userService.syncDuccVersionStatus(user, Agent.asSystem());

    verify(accessModuleService)
        .updateCompletionTime(user, AccessModuleName.DATA_USER_CODE_OF_CONDUCT, null);
  }

  @Test
  public void test_hasAuthority() {
    DbUser user = new DbUser();
    user.setAuthoritiesEnum(Collections.singleton(Authority.ACCESS_CONTROL_ADMIN));
    user = userDao.save(user);

    assertThat(userService.hasAuthority(user.getUserId(), Authority.ACCESS_CONTROL_ADMIN)).isTrue();
    assertThat(userService.hasAuthority(user.getUserId(), Authority.INSTITUTION_ADMIN)).isFalse();
  }

  // DEVELOPER Authority includes all of the others

  @Test
  public void test_hasAuthority_DEVELOPER() {
    DbUser user = new DbUser();
    user.setAuthoritiesEnum(Collections.singleton(Authority.DEVELOPER));
    user = userDao.save(user);

    for (Authority auth : Authority.values()) {
      assertThat(userService.hasAuthority(user.getUserId(), auth)).isTrue();
    }
  }

  @Test
  public void test_confirmProfile() {
    assertModuleIncomplete(AccessModuleName.PROFILE_CONFIRMATION, providedDbUser);

    // user confirms profile, so confirmation time is set to START_INSTANT

    userService.confirmProfile(providedDbUser);
    assertModuleCompletionEqual(
        AccessModuleName.PROFILE_CONFIRMATION, providedDbUser, Timestamp.from(START_INSTANT));

    // time passes, user confirms again, confirmation time is updated

    tick();

    userService.confirmProfile(providedDbUser);
    assertThat(getModuleCompletionTime(AccessModuleName.PROFILE_CONFIRMATION, providedDbUser))
        .isGreaterThan(Timestamp.from(START_INSTANT));
  }

  @Test
  public void test_confirmPublications() {
    assertModuleIncomplete(AccessModuleName.PUBLICATION_CONFIRMATION, providedDbUser);

    // user confirms profile, so confirmation time is set to START_INSTANT

    userService.confirmPublications();
    assertModuleCompletionEqual(
        AccessModuleName.PUBLICATION_CONFIRMATION, providedDbUser, Timestamp.from(START_INSTANT));

    // time passes, user confirms again, confirmation time is updated

    tick();

    userService.confirmPublications();
    assertThat(getModuleCompletionTime(AccessModuleName.PUBLICATION_CONFIRMATION, providedDbUser))
        .isGreaterThan(Timestamp.from(START_INSTANT));
  }

  private void assertModuleCompletionEqual(
      AccessModuleName moduleName, DbUser user, Timestamp timestamp) {
    assertThat(getModuleCompletionTime(moduleName, user)).isEqualTo(timestamp);
  }

  private Timestamp getModuleCompletionTime(AccessModuleName moduleName, DbUser user) {
    return userAccessModuleDao
        .getByUserAndAccessModule(user, accessModuleDao.findOneByName(moduleName).get())
        .get()
        .getCompletionTime();
  }

  private void assertModuleIncomplete(AccessModuleName moduleName, DbUser user) {
    // assert that the module is either not present or explicitly null
    userAccessModuleDao
        .getByUserAndAccessModule(user, accessModuleDao.findOneByName(moduleName).get())
        .ifPresent(module -> assertThat(module.getCompletionTime()).isNull());
  }

  private DbUserCodeOfConductAgreement signDucc(DbUser dbUser, int version) {
    return TestMockFactory.createDuccAgreement(dbUser, version, FakeClockConfiguration.NOW);
  }
}
