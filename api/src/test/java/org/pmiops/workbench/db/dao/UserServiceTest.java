package org.pmiops.workbench.db.dao;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static com.google.common.truth.Truth8.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.pmiops.workbench.access.AccessTierService.REGISTERED_TIER_SHORT_NAME;
import static org.pmiops.workbench.db.dao.UserService.LATEST_AOU_TOS_VERSION;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.FakeJpaDateTimeConfiguration;
import org.pmiops.workbench.access.AccessModuleService;
import org.pmiops.workbench.access.AccessModuleServiceImpl;
import org.pmiops.workbench.access.AccessTierServiceImpl;
import org.pmiops.workbench.access.UserAccessModuleMapperImpl;
import org.pmiops.workbench.actionaudit.auditors.UserServiceAuditor;
import org.pmiops.workbench.compliance.ComplianceService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.model.DbAccessModule;
import org.pmiops.workbench.db.model.DbAccessModule.DbAccessModuleName;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserTermsOfService;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudNihStatus;
import org.pmiops.workbench.google.DirectoryService;
import org.pmiops.workbench.institution.InstitutionService;
import org.pmiops.workbench.mail.MailService;
import org.pmiops.workbench.model.Authority;
import org.pmiops.workbench.model.Institution;
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
  @MockBean private InstitutionService mockInstitutionService;
  @MockBean private UserServiceAuditor mockUserServiceAuditAdapter;

  @Autowired private AccessModuleDao accessModuleDao;
  @Autowired private AccessTierDao accessTierDao;
  @Autowired private FakeClock fakeClock;
  @Autowired private UserAccessModuleDao userAccessModuleDao;
  @Autowired private UserDao userDao;
  @Autowired private UserService userService;
  @Autowired private UserTermsOfServiceDao userTermsOfServiceDao;

  // use a SpyBean when we need the full service for some tests and mocks for others
  @SpyBean private AccessModuleService accessModuleService;

  @Import({
    UserServiceTestConfiguration.class,
    CommonMappers.class,
    FakeClockConfiguration.class,
    FakeJpaDateTimeConfiguration.class,
    AccessModuleServiceImpl.class,
    AccessTierServiceImpl.class,
    UserAccessModuleMapperImpl.class,
  })
  @MockBean({
    ComplianceService.class,
    DirectoryService.class,
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
    providedWorkbenchConfig.access.renewal.expiryDays = 365L;
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

  private void tick() {
    fakeClock.increment(CLOCK_INCREMENT_MILLIS);
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
    assertModuleCompletionEqual(
        DbAccessModuleName.ERA_COMMONS, user, Timestamp.from(START_INSTANT));

    assertThat(user.getEraCommonsLinkExpireTime()).isEqualTo(Timestamp.from(START_INSTANT));
    assertThat(user.getEraCommonsLinkedNihUsername()).isEqualTo("nih-user");

    // Completion timestamp should not change when the method is called again.
    tick();
    userService.syncEraCommonsStatus();

    assertModuleCompletionEqual(
        DbAccessModuleName.ERA_COMMONS, user, Timestamp.from(START_INSTANT));
  }

  @Test
  public void testClearsEraCommonsStatus() {
    // Put the test user in a state where eRA commons is completed.
    DbUser testUser = userDao.findUserByUsername(USERNAME);
    testUser.setEraCommonsLinkedNihUsername("nih-user");
    testUser = userDao.save(testUser);

    accessModuleService.updateCompletionTime(
        testUser, DbAccessModuleName.ERA_COMMONS, Timestamp.from(START_INSTANT));

    userService.syncEraCommonsStatus();

    DbUser retrievedUser = userDao.findUserByUsername(USERNAME);
    assertModuleCompletionEqual(DbAccessModuleName.ERA_COMMONS, retrievedUser, null);
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
        DbAccessModuleName.RAS_LOGIN_GOV, providedDbUser, Timestamp.from(START_INSTANT));
  }

  @Test
  public void testSubmitAouTermsOfService() {
    // confirm empty to start
    assertThat(StreamSupport.stream(userTermsOfServiceDao.findAll().spliterator(), false).count())
        .isEqualTo(0);

    DbUser user = userDao.findUserByUsername(USERNAME);
    userService.submitAouTermsOfService(user, LATEST_AOU_TOS_VERSION);
    verify(mockUserServiceAuditAdapter).fireAcknowledgeTermsOfService(any(DbUser.class), eq(1));

    Optional<DbUserTermsOfService> tosMaybe =
        userTermsOfServiceDao.findFirstByUserIdOrderByTosVersionDesc(user.getUserId());
    assertThat(tosMaybe).isPresent();
    assertThat(tosMaybe.get().getTosVersion()).isEqualTo(LATEST_AOU_TOS_VERSION);
    assertThat(tosMaybe.get().getAouAgreementTime()).isNotNull();
    assertThat(tosMaybe.get().getTerraAgreementTime()).isNull();
  }

  @Test
  public void testAcceptTerraTermsOfService() {
    // confirm empty to start
    assertThat(StreamSupport.stream(userTermsOfServiceDao.findAll().spliterator(), false).count())
        .isEqualTo(0);

    // need to do this first
    DbUser user = userDao.findUserByUsername(USERNAME);
    userService.submitAouTermsOfService(user, LATEST_AOU_TOS_VERSION);

    userService.acceptTerraTermsOfService(userDao.findUserByUsername(USERNAME));
    verify(mockFireCloudService).acceptTermsOfService();

    Optional<DbUserTermsOfService> tosMaybe =
        userTermsOfServiceDao.findFirstByUserIdOrderByTosVersionDesc(user.getUserId());
    assertThat(tosMaybe).isPresent();
    assertThat(tosMaybe.get().getTerraAgreementTime()).isNotNull();
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
    assertModuleIncomplete(DbAccessModuleName.PROFILE_CONFIRMATION, providedDbUser);

    // user confirms profile, so confirmation time is set to START_INSTANT

    userService.confirmProfile(providedDbUser);
    assertModuleCompletionEqual(
        DbAccessModuleName.PROFILE_CONFIRMATION, providedDbUser, Timestamp.from(START_INSTANT));

    // time passes, user confirms again, confirmation time is updated

    tick();

    userService.confirmProfile(providedDbUser);
    assertThat(getModuleCompletionTime(DbAccessModuleName.PROFILE_CONFIRMATION, providedDbUser))
        .isGreaterThan(Timestamp.from(START_INSTANT));
  }

  @Test
  public void test_confirmPublications() {
    assertModuleIncomplete(DbAccessModuleName.PUBLICATION_CONFIRMATION, providedDbUser);

    // user confirms profile, so confirmation time is set to START_INSTANT

    userService.confirmPublications();
    assertModuleCompletionEqual(
        DbAccessModuleName.PUBLICATION_CONFIRMATION, providedDbUser, Timestamp.from(START_INSTANT));

    // time passes, user confirms again, confirmation time is updated

    tick();

    userService.confirmPublications();
    assertThat(getModuleCompletionTime(DbAccessModuleName.PUBLICATION_CONFIRMATION, providedDbUser))
        .isGreaterThan(Timestamp.from(START_INSTANT));
  }

  @Test
  public void test_validateAllOfUsTermsOfService() {
    // does not throw
    userService.validateAllOfUsTermsOfService(LATEST_AOU_TOS_VERSION);
  }

  @Test
  public void test_validateAllOfUsTermsOfService_null_version() {
    final Integer badVersion = null;
    assertThrows(
        BadRequestException.class, () -> userService.validateAllOfUsTermsOfService(badVersion));
  }

  @Test
  public void test_validateAllOfUsTermsOfService_wrong_version() {
    assertThrows(
        BadRequestException.class,
        () -> userService.validateAllOfUsTermsOfService(LATEST_AOU_TOS_VERSION - 1));
  }

  @Test
  public void test_validateAllOfUsTermsOfServiceVersion() {
    DbUser user = createUserWithAoUTOSVersion(LATEST_AOU_TOS_VERSION);
    assertThat(userService.validateAllOfUsTermsOfServiceVersion(user)).isTrue();
  }

  @Test
  public void test_validateAllOfUsTermsOfServiceVersion_incorrectVersion() {
    DbUser user = createUserWithAoUTOSVersion(LATEST_AOU_TOS_VERSION - 1);
    assertThat(userService.validateAllOfUsTermsOfServiceVersion(user)).isFalse();
  }

  @Test
  public void test_validateAllOfUsTermsOfServiceVersion_userHasNotAcceptedTOS() {
    DbUser dbUser = userDao.findUserByUsername(USERNAME);
    assertThat(userService.validateAllOfUsTermsOfServiceVersion(dbUser)).isFalse();
  }

  @Test
  public void test_validateTermsOfService_dbUser_hasNotAcceptedTerraTOS()
      throws org.pmiops.workbench.firecloud.ApiException {
    when(mockFireCloudService.getUserTermsOfServiceStatus()).thenReturn(false);
    DbUser user = createUserWithAoUTOSVersion(LATEST_AOU_TOS_VERSION);

    assertThat(userService.validateTermsOfService(user)).isFalse();
  }

  @Test
  public void test_validateTermsOfService_dbUser()
      throws org.pmiops.workbench.firecloud.ApiException {
    when(mockFireCloudService.getUserTermsOfServiceStatus()).thenReturn(true);
    DbUser user = createUserWithAoUTOSVersion(LATEST_AOU_TOS_VERSION);
    assertThat(userService.validateTermsOfService(user)).isTrue();
  }

  @Test
  public void test_validateTermsOfService_dbUser_missing_version()
      throws org.pmiops.workbench.firecloud.ApiException {
    DbUser user = userDao.findUserByUsername(USERNAME);
    userTermsOfServiceDao.save(new DbUserTermsOfService().setUserId(user.getUserId()));
    assertThat(userService.validateTermsOfService(user)).isFalse();
  }

  @Test
  public void test_validateTermsOfService_dbUser_wrong_aou_version_acceptedTerra()
      throws org.pmiops.workbench.firecloud.ApiException {
    when(mockFireCloudService.getUserTermsOfServiceStatus()).thenReturn(true);
    DbUser user = createUserWithAoUTOSVersion(LATEST_AOU_TOS_VERSION - 1);
    assertThat(userService.validateTermsOfService(user)).isFalse();
  }

  @Test
  public void test_validateTermsOfService_dbUser_wrong_aou_version_hasNot_acceptedTerra()
      throws org.pmiops.workbench.firecloud.ApiException {
    when(mockFireCloudService.getUserTermsOfServiceStatus()).thenReturn(false);
    DbUser user = createUserWithAoUTOSVersion(LATEST_AOU_TOS_VERSION - 1);
    assertThat(userService.validateTermsOfService(user)).isFalse();
  }

  @Test
  public void test_validateAllOfUsTermsOfService_dbUser_no_tos_entry() {
    DbUser user = userDao.findUserByUsername(USERNAME);
    assertThat(userService.validateTermsOfService(user)).isFalse();
  }

  @Test
  public void testCreateServiceAccountUser() {
    String username = "test@@appspot.gserviceaccount.com";
    userService.createServiceAccountUser(username);
    assertThat(userDao.findUserByUsername(username)).isNotNull();
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

  private void assertModuleIncomplete(DbAccessModuleName moduleName, DbUser user) {
    // assert that the module is either not present or explicitly null
    userAccessModuleDao
        .getByUserAndAccessModule(user, accessModuleDao.findOneByName(moduleName).get())
        .ifPresent(module -> assertThat(module.getCompletionTime()).isNull());
  }

  private DbUser createUserWithAoUTOSVersion(int tosVersion) {
    DbUser dbUser = userDao.findUserByUsername(USERNAME);
    userTermsOfServiceDao.save(
        new DbUserTermsOfService().setUserId(dbUser.getUserId()).setTosVersion(tosVersion));
    return dbUser;
  }
}
