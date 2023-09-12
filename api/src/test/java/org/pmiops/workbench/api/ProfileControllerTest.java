package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.pmiops.workbench.utils.TestMockFactory.createRegisteredTier;

import com.google.api.services.directory.model.User;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import jakarta.mail.MessagingException;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.FakeJpaDateTimeConfiguration;
import org.pmiops.workbench.access.AccessModuleService;
import org.pmiops.workbench.access.AccessModuleServiceImpl;
import org.pmiops.workbench.access.AccessTierService;
import org.pmiops.workbench.access.AccessTierServiceImpl;
import org.pmiops.workbench.access.UserAccessModuleMapperImpl;
import org.pmiops.workbench.actionaudit.ActionAuditQueryServiceImpl;
import org.pmiops.workbench.actionaudit.Agent;
import org.pmiops.workbench.actionaudit.auditors.ProfileAuditor;
import org.pmiops.workbench.actionaudit.auditors.UserServiceAuditor;
import org.pmiops.workbench.actionaudit.targetproperties.BypassTimeTargetProperty;
import org.pmiops.workbench.auth.UserAuthentication;
import org.pmiops.workbench.auth.UserAuthentication.UserType;
import org.pmiops.workbench.billing.FreeTierBillingService;
import org.pmiops.workbench.billing.WorkspaceFreeTierUsageService;
import org.pmiops.workbench.captcha.ApiException;
import org.pmiops.workbench.captcha.CaptchaVerificationService;
import org.pmiops.workbench.compliancetraining.ComplianceTrainingServiceImpl;
import org.pmiops.workbench.config.CommonConfig;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.AccessModuleDao;
import org.pmiops.workbench.db.dao.AccessTierDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserServiceImpl;
import org.pmiops.workbench.db.dao.UserTermsOfServiceDao;
import org.pmiops.workbench.db.model.DbAccessModule;
import org.pmiops.workbench.db.model.DbAccessModule.DbAccessModuleName;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserCodeOfConductAgreement;
import org.pmiops.workbench.db.model.DbUserTermsOfService;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudNihStatus;
import org.pmiops.workbench.google.CloudStorageClient;
import org.pmiops.workbench.google.DirectoryService;
import org.pmiops.workbench.institution.InstitutionMapperImpl;
import org.pmiops.workbench.institution.InstitutionService;
import org.pmiops.workbench.institution.InstitutionServiceImpl;
import org.pmiops.workbench.institution.VerifiedInstitutionalAffiliationMapperImpl;
import org.pmiops.workbench.mail.MailService;
import org.pmiops.workbench.model.AccessBypassRequest;
import org.pmiops.workbench.model.AccessModule;
import org.pmiops.workbench.model.AccessModuleStatus;
import org.pmiops.workbench.model.AccountPropertyUpdate;
import org.pmiops.workbench.model.Address;
import org.pmiops.workbench.model.Authority;
import org.pmiops.workbench.model.CreateAccountRequest;
import org.pmiops.workbench.model.DemographicSurvey;
import org.pmiops.workbench.model.Disability;
import org.pmiops.workbench.model.Education;
import org.pmiops.workbench.model.Ethnicity;
import org.pmiops.workbench.model.GenderIdentity;
import org.pmiops.workbench.model.Institution;
import org.pmiops.workbench.model.InstitutionMembershipRequirement;
import org.pmiops.workbench.model.InstitutionTierConfig;
import org.pmiops.workbench.model.InstitutionUserInstructions;
import org.pmiops.workbench.model.InstitutionalRole;
import org.pmiops.workbench.model.NihToken;
import org.pmiops.workbench.model.OrganizationType;
import org.pmiops.workbench.model.Profile;
import org.pmiops.workbench.model.Race;
import org.pmiops.workbench.model.RasLinkRequestBody;
import org.pmiops.workbench.model.ResendWelcomeEmailRequest;
import org.pmiops.workbench.model.SexAtBirth;
import org.pmiops.workbench.model.UpdateContactEmailRequest;
import org.pmiops.workbench.model.VerifiedInstitutionalAffiliation;
import org.pmiops.workbench.moodle.MoodleServiceImpl;
import org.pmiops.workbench.profile.AddressMapperImpl;
import org.pmiops.workbench.profile.DemographicSurveyMapperImpl;
import org.pmiops.workbench.profile.PageVisitMapperImpl;
import org.pmiops.workbench.profile.ProfileMapperImpl;
import org.pmiops.workbench.profile.ProfileService;
import org.pmiops.workbench.ras.RasLinkService;
import org.pmiops.workbench.shibboleth.ShibbolethService;
import org.pmiops.workbench.survey.NewUserSatisfactionSurveyService;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.test.FakeLongRandom;
import org.pmiops.workbench.testconfig.UserServiceTestConfiguration;
import org.pmiops.workbench.utils.TestMockFactory;
import org.pmiops.workbench.utils.mappers.AuditLogEntryMapperImpl;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;

@DataJpaTest
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
public class ProfileControllerTest extends BaseControllerTest {
  @MockBean private CaptchaVerificationService mockCaptchaVerificationService;
  @MockBean private CloudStorageClient mockCloudStorageClient;
  @MockBean private DirectoryService mockDirectoryService;
  @MockBean private FireCloudService mockFireCloudService;
  @MockBean private MailService mockMailService;
  @MockBean private ProfileAuditor mockProfileAuditor;
  @MockBean private ShibbolethService mockShibbolethService;
  @MockBean private UserServiceAuditor mockUserServiceAuditor;
  @MockBean private RasLinkService mockRasLinkService;

  @Autowired private AccessModuleDao accessModuleDao;
  @Autowired private AccessModuleService accessModuleService;
  @Autowired private AccessTierDao accessTierDao;
  @Autowired private AccessTierService accessTierService;
  @Autowired private InstitutionService institutionService;
  @Autowired private ProfileController profileController;
  @Autowired private ProfileService profileService;
  @Autowired private UserDao userDao;
  @Autowired private UserTermsOfServiceDao userTermsOfServiceDao;
  @Autowired private FakeClock fakeClock;

  private static final long NONCE_LONG = 12345;
  private static final String CAPTCHA_TOKEN = "captchaToken";
  private static final String CITY = "Exampletown";
  private static final String CONTACT_EMAIL = "bob@example.com";
  private static final String COUNTRY = "Example";
  private static final String FAMILY_NAME = "Bobberson";
  private static final String GIVEN_NAME = "Bob";
  private static final String GSUITE_DOMAIN = "researchallofus.org";
  private static final String NONCE = Long.toString(NONCE_LONG);
  private static final String RESEARCH_PURPOSE = "To test things";
  private static final String STATE = "EX";
  private static final String STREET_ADDRESS = "1 Example Lane";
  private static final String USER_PREFIX = "bob";
  private static final String WRONG_CAPTCHA_TOKEN = "WrongCaptchaToken";
  private static final String ZIP_CODE = "12345";
  private static final String FULL_USER_NAME = USER_PREFIX + "@" + GSUITE_DOMAIN;
  private static final Timestamp TIMESTAMP = FakeClockConfiguration.NOW;
  private static final double TIME_TOLERANCE_MILLIS = 100.0;
  private static final int CURRENT_DUCC_VERSION = 27; // arbitrary for test
  private static final String AOU_TOS_NOT_ACCEPTED_ERROR_MESSAGE =
      "No Terms of Service acceptance recorded for user ID";
  private CreateAccountRequest createAccountRequest;
  private User googleUser;
  private static DbUser dbUser;
  private static List<DbAccessModule> accessModules;

  private DbAccessTier registeredTier;
  private InstitutionTierConfig rtAddressesConfig;
  private InstitutionTierConfig rtDomainsConfig;

  @TestConfiguration
  @Import({
    AccessModuleServiceImpl.class,
    ActionAuditQueryServiceImpl.class,
    AddressMapperImpl.class,
    AuditLogEntryMapperImpl.class,
    CaptchaVerificationService.class,
    CommonConfig.class,
    CommonMappers.class,
    MoodleServiceImpl.class,
    ComplianceTrainingServiceImpl.class,
    DemographicSurveyMapperImpl.class,
    FreeTierBillingService.class,
    InstitutionMapperImpl.class,
    InstitutionServiceImpl.class,
    PageVisitMapperImpl.class,
    ProfileController.class,
    ProfileMapperImpl.class,
    UserAccessModuleMapperImpl.class,
    ProfileService.class,
    UserServiceImpl.class,
    UserServiceTestConfiguration.class,
    VerifiedInstitutionalAffiliationMapperImpl.class,
    AccessTierServiceImpl.class,
    FakeClockConfiguration.class,
    FakeJpaDateTimeConfiguration.class,
    WorkspaceFreeTierUsageService.class,
  })
  @MockBean({
    BigQueryService.class,
    NewUserSatisfactionSurveyService.class,
  })
  static class Configuration {
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    DbUser dbUser() {
      return dbUser;
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    UserAuthentication userAuthentication() {
      return new UserAuthentication(dbUser, null, null, UserType.RESEARCHER);
    }

    @Bean
    @Primary
    Random getRandom() {
      return new FakeLongRandom(NONCE_LONG);
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public WorkbenchConfig workbenchConfig() {
      return config;
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public List<DbAccessModule> getDbAccessModules() {
      return accessModules;
    }
  }

  @BeforeEach
  @Override
  public void setUp() throws IOException {
    super.setUp();

    config.googleDirectoryService.gSuiteDomain = GSUITE_DOMAIN;

    // key UserService logic depends on the existence of the Registered Tier
    registeredTier = accessTierDao.save(createRegisteredTier());

    rtAddressesConfig =
        new InstitutionTierConfig()
            .membershipRequirement(InstitutionMembershipRequirement.ADDRESSES)
            .eraRequired(false)
            .accessTierShortName(registeredTier.getShortName());
    rtDomainsConfig =
        new InstitutionTierConfig()
            .membershipRequirement(InstitutionMembershipRequirement.DOMAINS)
            .eraRequired(false)
            .accessTierShortName(registeredTier.getShortName());

    Profile profile = new Profile();
    profile.setContactEmail(CONTACT_EMAIL);
    profile.setFamilyName(FAMILY_NAME);
    profile.setGivenName(GIVEN_NAME);
    profile.setUsername(USER_PREFIX);
    profile.setAreaOfResearch(RESEARCH_PURPOSE);
    profile.setGeneralDiscoverySources(List.of());
    profile.setGeneralDiscoverySourceOtherText(null);
    profile.setPartnerDiscoverySources(List.of());
    profile.setPartnerDiscoverySourceOtherText(null);
    profile.setAddress(
        new Address()
            .streetAddress1(STREET_ADDRESS)
            .city(CITY)
            .state(STATE)
            .country(COUNTRY)
            .zipCode(ZIP_CODE));

    createAccountRequest = new CreateAccountRequest();
    createAccountRequest.setTermsOfServiceVersion(config.termsOfService.latestAouVersion);
    createAccountRequest.setProfile(profile);
    createAccountRequest.setCaptchaVerificationToken(CAPTCHA_TOKEN);

    googleUser = new User();
    googleUser.setPrimaryEmail(FULL_USER_NAME);
    googleUser.setChangePasswordAtNextLogin(true);
    googleUser.setPassword("testPassword");
    googleUser.setIsEnrolledIn2Sv(true);

    config.access.currentDuccVersions = ImmutableList.of(CURRENT_DUCC_VERSION);

    when(mockDirectoryService.getUserOrThrow(FULL_USER_NAME)).thenReturn(googleUser);
    when(mockDirectoryService.createUser(GIVEN_NAME, FAMILY_NAME, FULL_USER_NAME, CONTACT_EMAIL))
        .thenReturn(googleUser);
    when(mockCloudStorageClient.getCaptchaServerKey()).thenReturn("Server_Key");

    try {
      when(mockCaptchaVerificationService.verifyCaptcha(CAPTCHA_TOKEN)).thenReturn(true);
      when(mockCaptchaVerificationService.verifyCaptcha(WRONG_CAPTCHA_TOKEN)).thenReturn(false);
      when(mockFireCloudService.getUserTermsOfServiceStatus()).thenReturn(true);
    } catch (ApiException | org.pmiops.workbench.firecloud.ApiException e) {
      e.printStackTrace();
    }

    accessModules = TestMockFactory.createAccessModules(accessModuleDao);
  }

  @Test
  public void testCreateAccount_invalidCaptchaToken() {
    config.captcha.enableCaptcha = true;
    createAccountAndDbUserWithAffiliation();
    createAccountRequest.setCaptchaVerificationToken(WRONG_CAPTCHA_TOKEN);

    assertThrows(
        BadRequestException.class, () -> profileController.createAccount(createAccountRequest));
  }

  @Test
  public void testCreateAccount_invalidCaptchaToken_okIfEnableCaptchaFalse() {
    config.captcha.enableCaptcha = false;

    createAccountAndDbUserWithAffiliation();
    createAccountRequest.setCaptchaVerificationToken(WRONG_CAPTCHA_TOKEN);

    // no exception
    profileController.createAccount(createAccountRequest);
  }

  @Test
  public void testCreateAccount_MismatchEmailAddress() {
    assertThrows(
        BadRequestException.class,
        () -> {
          final Institution broad =
              new Institution()
                  .shortName("Broad")
                  .displayName("The Broad Institute")
                  .organizationTypeEnum(OrganizationType.ACADEMIC_RESEARCH_INSTITUTION)
                  .addTierConfigsItem(
                      rtAddressesConfig.emailAddresses(ImmutableList.of("email@domain.org")));
          institutionService.createInstitution(broad);
          final VerifiedInstitutionalAffiliation verifiedInstitutionalAffiliation =
              new VerifiedInstitutionalAffiliation()
                  .institutionShortName("Broad")
                  .institutionalRoleEnum(InstitutionalRole.STUDENT);
          createAccountRequest.getProfile().contactEmail("bob@broad.com");
          createAccountAndDbUserWithAffiliation(verifiedInstitutionalAffiliation);
        });
  }

  @Test
  public void testCreateAccount_MismatchEmailDomain() {
    assertThrows(
        BadRequestException.class,
        () -> {
          final Institution broad =
              new Institution()
                  .shortName("Broad")
                  .displayName("The Broad Institute")
                  .organizationTypeEnum(OrganizationType.ACADEMIC_RESEARCH_INSTITUTION)
                  .addTierConfigsItem(
                      rtDomainsConfig
                          .emailAddresses(ImmutableList.of(CONTACT_EMAIL))
                          .emailDomains(ImmutableList.of("example.com")));
          institutionService.createInstitution(broad);
          final VerifiedInstitutionalAffiliation verifiedInstitutionalAffiliation =
              new VerifiedInstitutionalAffiliation()
                  .institutionShortName("Broad")
                  .institutionalRoleEnum(InstitutionalRole.STUDENT);
          createAccountRequest.getProfile().contactEmail("bob@broad.com");
          createAccountAndDbUserWithAffiliation(verifiedInstitutionalAffiliation);
        });
  }

  @Test
  public void testCreateAccount_success_addressesRtRequirement() {
    final Institution broad =
        new Institution()
            .shortName("Broad")
            .displayName("The Broad Institute")
            .addTierConfigsItem(rtAddressesConfig.emailAddresses(ImmutableList.of(CONTACT_EMAIL)))
            .organizationTypeEnum(OrganizationType.ACADEMIC_RESEARCH_INSTITUTION);
    institutionService.createInstitution(broad);

    final VerifiedInstitutionalAffiliation verifiedInstitutionalAffiliation =
        new VerifiedInstitutionalAffiliation()
            .institutionShortName("Broad")
            .institutionalRoleEnum(InstitutionalRole.STUDENT);
    createAccountRequest.getProfile().contactEmail(CONTACT_EMAIL);
    createAccountAndDbUserWithAffiliation(verifiedInstitutionalAffiliation);
  }

  @Test
  public void testCreateAccount_success_domainsRtRequirement() {
    final Institution broad =
        new Institution()
            .shortName("Broad")
            .displayName("The Broad Institute")
            .addTierConfigsItem(rtDomainsConfig.emailDomains(ImmutableList.of("example.com")))
            .organizationTypeEnum(OrganizationType.ACADEMIC_RESEARCH_INSTITUTION);
    institutionService.createInstitution(broad);

    final VerifiedInstitutionalAffiliation verifiedInstitutionalAffiliation =
        new VerifiedInstitutionalAffiliation()
            .institutionShortName("Broad")
            .institutionalRoleEnum(InstitutionalRole.STUDENT);
    createAccountRequest.getProfile().contactEmail("bob@example.com");
    createAccountAndDbUserWithAffiliation(verifiedInstitutionalAffiliation);
  }

  @Test
  public void testCreateAccount_success() {
    createAccountAndDbUserWithAffiliation();
    verify(mockProfileAuditor).fireCreateAction(any(Profile.class));
    final DbUser dbUser = userDao.findUserByUsername(FULL_USER_NAME);
    assertThat(dbUser).isNotNull();
    assertThat(accessTierService.getAccessTierShortNamesForUser(dbUser)).isEmpty();
  }

  @Test
  public void testCreateAccount_withTosVersion() {
    createAccountRequest.setTermsOfServiceVersion(config.termsOfService.latestAouVersion);
    createAccountAndDbUserWithAffiliation();

    final DbUser dbUser = userDao.findUserByUsername(FULL_USER_NAME);
    final List<DbUserTermsOfService> tosRows = Lists.newArrayList(userTermsOfServiceDao.findAll());
    assertThat(tosRows.size()).isEqualTo(1);
    assertThat(tosRows.get(0).getTosVersion()).isEqualTo(config.termsOfService.latestAouVersion);
    assertThat(tosRows.get(0).getUserId()).isEqualTo(dbUser.getUserId());
    assertThat(tosRows.get(0).getAouAgreementTime()).isNotNull();
    assertThat(tosRows.get(0).getTerraAgreementTime()).isNull();
    Profile profile = profileService.getProfile(dbUser);
    assertThat(profile.getLatestTermsOfServiceVersion())
        .isEqualTo(config.termsOfService.latestAouVersion);
  }

  @Test
  public void testCreateAccount_withBadTosVersion() {
    assertThrows(
        BadRequestException.class,
        () -> {
          createAccountRequest.setTermsOfServiceVersion(config.termsOfService.latestAouVersion - 1);
          createAccountAndDbUserWithAffiliation();
        });
  }

  @Test
  public void testCreateAccount_withBadTosVersion_Null() {
    assertThrows(
        BadRequestException.class,
        () -> {
          createAccountRequest.setTermsOfServiceVersion(null);
          createAccountAndDbUserWithAffiliation();
        });
  }

  @Test
  public void testCreateAccount_invalidUser() {
    CreateAccountRequest accountRequest = new CreateAccountRequest();
    accountRequest.setCaptchaVerificationToken(CAPTCHA_TOKEN);
    createAccountRequest.getProfile().setUsername("12");
    accountRequest.setProfile(createAccountRequest.getProfile());
    assertThrows(
        BadRequestException.class,
        this::createAccountAndDbUserWithAffiliation,
        "Username should be at least 3 characters and not more than 64 characters");
  }

  @Test
  public void testCreateAccount_dbUserFailure() {
    assertThrows(
        Exception.class,
        () -> {
          // Exercises a scenario where the userService throws an unexpected exception (e.g. a SQL
          // error),
          // ensuring we attempt to clean up the orphaned G Suite user after catching the exception.
          createAccountRequest.getProfile().getAddress().setZipCode("12345678901234567890");
          createAccountAndDbUserWithAffiliation();
          // The G Suite user should be deleted after the DbUser creation fails.
          verify(mockDirectoryService).deleteUser(anyString());
        });
  }

  @Test
  public void testSubmitDUCC_success() {
    createAccountAndDbUserWithAffiliation();
    String initials = "NIH";
    assertThat(profileController.submitDUCC(CURRENT_DUCC_VERSION, initials).getStatusCode())
        .isEqualTo(HttpStatus.OK);
    DbUserCodeOfConductAgreement duccAgreement = dbUser.getDuccAgreement();
    assertThat(duccAgreement.getUserFamilyName()).isEqualTo(dbUser.getFamilyName());
    assertThat(duccAgreement.getUserGivenName()).isEqualTo(dbUser.getGivenName());
    assertThat(duccAgreement.getUserInitials()).isEqualTo(initials);
    assertThat(duccAgreement.getSignedVersion()).isEqualTo(CURRENT_DUCC_VERSION);
  }

  @Test
  public void testSubmitDUCC_success_multiple_current() {
    config.access.currentDuccVersions = ImmutableList.of(7, 8, 9);

    createAccountAndDbUserWithAffiliation();
    String initials = "NIH";

    config.access.currentDuccVersions.forEach(
        version -> {
          assertThat(profileController.submitDUCC(version, initials).getStatusCode())
              .isEqualTo(HttpStatus.OK);
          DbUserCodeOfConductAgreement duccAgreement = dbUser.getDuccAgreement();
          assertThat(duccAgreement.getSignedVersion()).isEqualTo(version);
          assertThat(duccAgreement.getUserFamilyName()).isEqualTo(dbUser.getFamilyName());
          assertThat(duccAgreement.getUserGivenName()).isEqualTo(dbUser.getGivenName());
          assertThat(duccAgreement.getUserInitials()).isEqualTo(initials);
        });
  }

  @Test
  public void testSubmitDUCC_wrongVersion_older() {
    assertThrows(
        BadRequestException.class,
        () -> {
          createAccountAndDbUserWithAffiliation();
          String initials = "NIH";
          profileController.submitDUCC(CURRENT_DUCC_VERSION - 1, initials);
        });
  }

  // not really a use case for this, but shows we need an exact match

  @Test
  public void testSubmitDUCC_wrongVersion_newer() {
    assertThrows(
        BadRequestException.class,
        () -> {
          createAccountAndDbUserWithAffiliation();
          String initials = "NIH";
          profileController.submitDUCC(CURRENT_DUCC_VERSION + 1, initials);
        });
  }

  // the user signs Version A of the DUCC, but the system later requires Version B instead
  // and therefore the user is no longer compliant

  @Test
  public void test_DuccBecomesOutdated() {
    final long userId = createAccountAndDbUserWithAffiliation().getUserId();

    // bypass the other access requirements
    final DbUser dbUser = userDao.findUserByUserId(userId);
    accessModuleService.updateCompletionTime(dbUser, DbAccessModuleName.ERA_COMMONS, TIMESTAMP);
    accessModuleService.updateCompletionTime(dbUser, DbAccessModuleName.IDENTITY, TIMESTAMP);
    accessModuleService.updateCompletionTime(dbUser, DbAccessModuleName.RAS_LOGIN_GOV, TIMESTAMP);
    accessModuleService.updateCompletionTime(
        dbUser, DbAccessModuleName.RT_COMPLIANCE_TRAINING, TIMESTAMP);
    accessModuleService.updateCompletionTime(dbUser, DbAccessModuleName.TWO_FACTOR_AUTH, TIMESTAMP);
    accessModuleService.updateCompletionTime(
        dbUser, DbAccessModuleName.PUBLICATION_CONFIRMATION, TIMESTAMP);
    accessModuleService.updateCompletionTime(
        dbUser, DbAccessModuleName.PROFILE_CONFIRMATION, TIMESTAMP);

    // arbitrary; at coding time the current version is 3
    final int versionA = 5;
    final int versionB = 8;

    // set the current DUCC version to version A
    config.access.currentDuccVersions = ImmutableList.of(versionA);

    // sign the current version (A)

    final String initials = "NIH";
    assertThat(profileController.submitDUCC(versionA, initials).getStatusCode())
        .isEqualTo(HttpStatus.OK);
    assertThat(accessTierService.getAccessTiersForUser(dbUser)).contains(registeredTier);

    // time passes and the system now requires a newer version (B)
    config.access.currentDuccVersions = ImmutableList.of(versionB);

    // a bit of a hack here: use this to sync the registration status
    // see also https://precisionmedicineinitiative.atlassian.net/browse/RW-2352
    profileController.syncTwoFactorAuthStatus();
    assertThat(accessTierService.getAccessTiersForUser(dbUser)).doesNotContain(registeredTier);
  }

  @Test
  public void testMe_success() {
    createAccountAndDbUserWithAffiliation();

    Profile profile = profileController.getMe().getBody();
    assertProfile(profile);
    verify(mockFireCloudService).registerUser();
    verify(mockProfileAuditor).fireLoginAction(dbUser);
  }

  @Test
  public void testGetUserTermsOfServiceStatus_UserHasNotAcceptedTerraTOS()
      throws org.pmiops.workbench.firecloud.ApiException {
    when(mockFireCloudService.getUserTermsOfServiceStatus()).thenReturn(false);
    createAccountAndDbUserWithAffiliation();
    assertThat(profileController.getUserTermsOfServiceStatus().getBody()).isFalse();
  }

  @Test
  public void testGetUserTermsOfServiceStatus_UserHasNotAcceptedLatestAoUTOSVersion() {
    createAccountAndDbUserWithAffiliation();
    DbUser user = userDao.findUserByUsername(FULL_USER_NAME);
    userTermsOfServiceDao.save(
        userTermsOfServiceDao.findByUserIdOrThrow(user.getUserId()).setTosVersion(-1));
    assertThat(profileController.getUserTermsOfServiceStatus().getBody()).isFalse();
  }

  @Test
  public void testGetUserTermsOfServiceStatus_UserHasNotAcceptedAoUTOS() {
    createAccountAndDbUserWithAffiliation();
    DbUser user = userDao.findUserByUsername(FULL_USER_NAME);
    DbUserTermsOfService userTos = userTermsOfServiceDao.findByUserIdOrThrow(user.getUserId());
    userTermsOfServiceDao.delete(userTos);
    assertThat(profileController.getUserTermsOfServiceStatus().getBody()).isEqualTo(false);
  }

  @Test
  public void testGetUserTermsOfServiceStatus() {
    createAccountAndDbUserWithAffiliation();
    assertThat(profileController.getUserTermsOfServiceStatus().getBody()).isTrue();
  }

  @Test
  public void testMe_userBeforeNotLoggedInSuccess() {
    createAccountAndDbUserWithAffiliation();
    Profile profile = profileController.getMe().getBody();
    assertProfile(profile);
    verify(mockFireCloudService).registerUser();

    // An additional call to getMe() should have no effect.
    fakeClock.increment(1);
    profile = profileController.getMe().getBody();
    assertProfile(profile);
  }

  @Test
  public void testMe_verifiedInstitutionalAffiliation_missing() {
    assertThrows(
        BadRequestException.class,
        () -> {
          final VerifiedInstitutionalAffiliation missing = null;
          createAccountAndDbUserWithAffiliation(missing);
        });
  }

  @Test
  public void testMe_verifiedInstitutionalAffiliation_invalidInstitution() {
    assertThrows(
        NotFoundException.class,
        () -> {
          final Institution broad =
              new Institution()
                  .shortName("Broad")
                  .displayName("The Broad Institute")
                  .addTierConfigsItem(
                      rtAddressesConfig.emailAddresses(ImmutableList.of(CONTACT_EMAIL)))
                  .organizationTypeEnum(OrganizationType.ACADEMIC_RESEARCH_INSTITUTION);
          institutionService.createInstitution(broad);
          // "Broad" is the only institution
          final String invalidInst = "Not the Broad";
          final VerifiedInstitutionalAffiliation verifiedInstitutionalAffiliation =
              new VerifiedInstitutionalAffiliation()
                  .institutionShortName(invalidInst)
                  .institutionalRoleEnum(InstitutionalRole.STUDENT);
          createAccountAndDbUserWithAffiliation(verifiedInstitutionalAffiliation);
        });
  }

  @Test
  public void testMe_verifiedInstitutionalAffiliation_invalidEmail() {
    assertThrows(
        BadRequestException.class,
        () -> {
          final Institution broad =
              new Institution()
                  .shortName("Broad")
                  .displayName("The Broad Institute")
                  .addTierConfigsItem(rtAddressesConfig.emailAddresses(ImmutableList.of()))
                  .organizationTypeEnum(OrganizationType.ACADEMIC_RESEARCH_INSTITUTION);
          institutionService.createInstitution(broad);
          final VerifiedInstitutionalAffiliation verifiedInstitutionalAffiliation =
              new VerifiedInstitutionalAffiliation()
                  .institutionShortName(broad.getShortName())
                  .institutionalRoleEnum(InstitutionalRole.ADMIN);
          createAccountAndDbUserWithAffiliation(verifiedInstitutionalAffiliation);
        });
  }

  @Test
  public void test_AcceptTermsOfService() {
    createAccountAndDbUserWithAffiliation();
    profileController.acceptTermsOfService(1);
    verify(mockFireCloudService).acceptTermsOfService();
  }

  @Test
  public void create_verifiedInstitutionalAffiliation_invalidDomain() {
    assertThrows(
        BadRequestException.class,
        () -> {
          ArrayList<String> emailDomains = new ArrayList<>();
          emailDomains.add("@broadinstitute.org");
          emailDomains.add("@broad.org");
          final Institution broad =
              new Institution()
                  .shortName("Broad")
                  .displayName("The Broad Institute")
                  .organizationTypeEnum(OrganizationType.ACADEMIC_RESEARCH_INSTITUTION)
                  .addTierConfigsItem(rtAddressesConfig.emailDomains(emailDomains));
          institutionService.createInstitution(broad);
          final VerifiedInstitutionalAffiliation verifiedInstitutionalAffiliation =
              new VerifiedInstitutionalAffiliation()
                  .institutionShortName(broad.getShortName())
                  .institutionalRoleEnum(InstitutionalRole.ADMIN);
          // CONTACT_EMAIL has the domain @example.com
          createAccountAndDbUserWithAffiliation(verifiedInstitutionalAffiliation);
        });
  }

  @Test
  public void updateProfile_removeVerifiedInstitutionalAffiliationForbidden() {
    final VerifiedInstitutionalAffiliation original = createVerifiedInstitutionalAffiliation();
    createAccountAndDbUserWithAffiliation(original);
    final Profile profile = profileController.getMe().getBody();
    profile.setVerifiedInstitutionalAffiliation(null);
    assertThrows(BadRequestException.class, () -> profileController.updateProfile(profile));
  }

  @Test
  public void updateContactEmail_forbidden() {
    createAccountAndDbUserWithAffiliation();
    dbUser.setFirstSignInTime(TIMESTAMP);
    String originalEmail = dbUser.getContactEmail();

    ResponseEntity<Void> response =
        profileController.updateContactEmail(
            new UpdateContactEmailRequest()
                .contactEmail("newContactEmail@whatever.com")
                .username(dbUser.getUsername())
                .creationNonce(NONCE));
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(dbUser.getContactEmail()).isEqualTo(originalEmail);
  }

  @Test
  public void updateContactEmail_badRequest() {
    createAccountAndDbUserWithAffiliation();
    when(mockDirectoryService.resetUserPassword(anyString())).thenReturn(googleUser);
    dbUser.setFirstSignInTime(null);
    String originalEmail = dbUser.getContactEmail();

    ResponseEntity<Void> response =
        profileController.updateContactEmail(
            new UpdateContactEmailRequest()
                .contactEmail("bad email address *(SD&(*D&F&*(DS ")
                .username(dbUser.getUsername())
                .creationNonce(NONCE));
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(dbUser.getContactEmail()).isEqualTo(originalEmail);
  }

  @Test
  public void updateContactEmail_OK() {
    createAccountAndDbUserWithAffiliation();
    dbUser.setFirstSignInTime(null);
    when(mockDirectoryService.resetUserPassword(anyString())).thenReturn(googleUser);

    ResponseEntity<Void> response =
        profileController.updateContactEmail(
            new UpdateContactEmailRequest()
                .contactEmail("newContactEmail@whatever.com")
                .username(dbUser.getUsername())
                .creationNonce(NONCE));
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(dbUser.getContactEmail()).isEqualTo("newContactEmail@whatever.com");
  }

  @Test
  public void updateName_alsoUpdatesDucc() {
    createAccountAndDbUserWithAffiliation();
    Profile profile = profileController.getMe().getBody();
    profile.setGivenName("OldGivenName");
    profile.setFamilyName("OldFamilyName");
    profileController.updateProfile(profile);
    profileController.submitDUCC(CURRENT_DUCC_VERSION, "O.O.");
    profile.setGivenName("NewGivenName");
    profile.setFamilyName("NewFamilyName");
    profileController.updateProfile(profile);
    assertThat(dbUser.getDuccAgreement().isUserNameOutOfDate()).isTrue();
  }

  @Test
  public void testSubmitDUCC_name_no_longer_out_of_date() {
    createAccountAndDbUserWithAffiliation();
    Profile profile = profileController.getMe().getBody();
    String givenName1 = profile.getGivenName();
    String familyName1 = profile.getFamilyName();
    String initials1 = "AAA";

    profileController.submitDUCC(CURRENT_DUCC_VERSION, initials1);
    DbUserCodeOfConductAgreement duccAgreement = dbUser.getDuccAgreement();
    assertThat(duccAgreement.isUserNameOutOfDate()).isFalse();
    assertThat(duccAgreement.getUserGivenName()).isEqualTo(givenName1);
    assertThat(duccAgreement.getUserFamilyName()).isEqualTo(familyName1);
    assertThat(duccAgreement.getUserInitials()).isEqualTo(initials1);
    assertThat(duccAgreement.getSignedVersion()).isEqualTo(CURRENT_DUCC_VERSION);

    String givenName2 = profile.getGivenName() + " Jr.";
    String familyName2 = profile.getFamilyName() + " Jr.";
    String initials2 = "BBB";

    profile.setGivenName(givenName2);
    profile.setFamilyName(familyName2);
    profileController.updateProfile(profile);
    assertThat(dbUser.getDuccAgreement().isUserNameOutOfDate()).isTrue();

    // signing again updates the name and also the out-of-date flag
    profileController.submitDUCC(CURRENT_DUCC_VERSION, initials2);
    duccAgreement = dbUser.getDuccAgreement();
    assertThat(duccAgreement.isUserNameOutOfDate()).isFalse();
    assertThat(duccAgreement.getUserGivenName()).isEqualTo(givenName2);
    assertThat(duccAgreement.getUserFamilyName()).isEqualTo(familyName2);
    assertThat(duccAgreement.getUserInitials()).isEqualTo(initials2);
    assertThat(duccAgreement.getSignedVersion()).isEqualTo(CURRENT_DUCC_VERSION);
  }

  @Test
  public void updateGivenName_badRequest() {
    assertThrows(
        BadRequestException.class,
        () -> {
          createAccountAndDbUserWithAffiliation();
          Profile profile = profileController.getMe().getBody();
          String newName =
              "obladidobladalifegoesonyalalalalalifegoesonobladioblada"
                  + "lifegoesonrahlalalalifegoeson";
          profile.setGivenName(newName);
          profileController.updateProfile(profile);
        });
  }

  @Test
  public void updateProfile_badRequest_nullAddress() {
    assertThrows(
        BadRequestException.class,
        () -> {
          createAccountAndDbUserWithAffiliation();
          Profile profile = profileController.getMe().getBody();
          profile.setAddress(null);
          profileController.updateProfile(profile);
        });
  }

  @Test
  public void updateProfile_badRequest_nullCountry() {
    assertThrows(
        BadRequestException.class,
        () -> {
          createAccountAndDbUserWithAffiliation();
          Profile profile = profileController.getMe().getBody();
          profile.getAddress().country(null);
          profileController.updateProfile(profile);
        });
  }

  @Test
  public void updateProfile_badRequest_nullState() {
    assertThrows(
        BadRequestException.class,
        () -> {
          createAccountAndDbUserWithAffiliation();
          Profile profile = profileController.getMe().getBody();
          profile.getAddress().state(null);
          profileController.updateProfile(profile);
        });
  }

  @Test
  public void updateProfile_badRequest_nullZipCode() {
    assertThrows(
        BadRequestException.class,
        () -> {
          createAccountAndDbUserWithAffiliation();
          Profile profile = profileController.getMe().getBody();
          profile.getAddress().zipCode(null);
          profileController.updateProfile(profile);
        });
  }

  @Test
  public void updateProfile_badRequest_emptyReasonForResearch() {
    assertThrows(
        BadRequestException.class,
        () -> {
          createAccountAndDbUserWithAffiliation();
          Profile profile = profileController.getMe().getBody();
          profile.setAreaOfResearch("");
          profileController.updateProfile(profile);
        });
  }

  @Test
  public void updateProfile_badRequest_UpdateUserName() {
    assertThrows(
        BadRequestException.class,
        () -> {
          createAccountAndDbUserWithAffiliation();
          Profile profile = profileController.getMe().getBody();
          profile.setUsername("newUserName@fakeDomain.com");
          profileController.updateProfile(profile);
        });
  }

  @Test
  public void updateProfile_badRequest_UpdateContactEmail() {
    assertThrows(
        BadRequestException.class,
        () -> {
          createAccountAndDbUserWithAffiliation();
          Profile profile = profileController.getMe().getBody();
          profile.setContactEmail("newContact@fakeDomain.com");
          profileController.updateProfile(profile);
        });
  }

  @Test
  public void updateFamilyName_badRequest() {
    assertThrows(
        BadRequestException.class,
        () -> {
          createAccountAndDbUserWithAffiliation();
          Profile profile = profileController.getMe().getBody();
          String newName =
              "obladidobladalifegoesonyalalalalalifegoesonobladioblada"
                  + "lifegoesonrahlalalalifegoeson";
          profile.setFamilyName(newName);
          profileController.updateProfile(profile);
        });
  }

  @Test
  public void resendWelcomeEmail_messagingException() throws MessagingException {
    createAccountAndDbUserWithAffiliation();
    dbUser.setFirstSignInTime(null);
    when(mockDirectoryService.resetUserPassword(anyString())).thenReturn(googleUser);
    doThrow(new MessagingException("exception"))
        .when(mockMailService)
        .sendWelcomeEmail(any(), any(), any(), any(), anyBoolean(), anyBoolean());

    ResponseEntity<Void> response =
        profileController.resendWelcomeEmail(
            new ResendWelcomeEmailRequest().username(dbUser.getUsername()).creationNonce(NONCE));
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    // called twice, once during account creation, once on resend
    verify(mockMailService, times(2))
        .sendWelcomeEmail(any(), any(), any(), any(), anyBoolean(), anyBoolean());
    verify(mockDirectoryService, times(1)).resetUserPassword(anyString());
  }

  @Test
  public void resendWelcomeEmail_OK() throws MessagingException {
    createAccountAndDbUserWithAffiliation();
    when(mockDirectoryService.resetUserPassword(anyString())).thenReturn(googleUser);
    doNothing()
        .when(mockMailService)
        .sendWelcomeEmail(any(), any(), any(), any(), anyBoolean(), anyBoolean());

    ResponseEntity<Void> response =
        profileController.resendWelcomeEmail(
            new ResendWelcomeEmailRequest().username(dbUser.getUsername()).creationNonce(NONCE));
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    // called twice, once during account creation, once on resend
    verify(mockMailService, times(2))
        .sendWelcomeEmail(any(), any(), any(), any(), anyBoolean(), anyBoolean());
    verify(mockDirectoryService, times(1)).resetUserPassword(anyString());
  }

  @Test
  public void sendUserInstructions_none() throws MessagingException {
    // default Institution in this test class has no instructions
    createAccountAndDbUserWithAffiliation();
    verify(mockMailService).sendWelcomeEmail(any(), any(), any(), any(), any(), any());

    // don't send the user instructions email if there are no instructions
    verifyNoMoreInteractions(mockMailService);
  }

  @Test
  public void sendUserInstructions_withInstructions() throws MessagingException {
    final VerifiedInstitutionalAffiliation verifiedInstitutionalAffiliation =
        createVerifiedInstitutionalAffiliation();

    final InstitutionUserInstructions instructions =
        new InstitutionUserInstructions()
            .institutionShortName(verifiedInstitutionalAffiliation.getInstitutionShortName())
            .instructions(
                "Wash your hands for 20 seconds <img src=\"https://this.is.escaped.later.com\" />");
    institutionService.setInstitutionUserInstructions(instructions);

    createAccountAndDbUserWithAffiliation(verifiedInstitutionalAffiliation);
    verify(mockMailService).sendWelcomeEmail(any(), any(), any(), any(), any(), any());
    verify(mockMailService)
        .sendInstitutionUserInstructions(
            CONTACT_EMAIL, instructions.getInstructions(), FULL_USER_NAME);
  }

  @Test
  public void sendUserInstructions_deleted() throws MessagingException {
    final VerifiedInstitutionalAffiliation verifiedInstitutionalAffiliation =
        createVerifiedInstitutionalAffiliation();

    final InstitutionUserInstructions instructions =
        new InstitutionUserInstructions()
            .institutionShortName(verifiedInstitutionalAffiliation.getInstitutionShortName())
            .instructions("whatever");
    institutionService.setInstitutionUserInstructions(instructions);

    institutionService.deleteInstitutionUserInstructions(
        verifiedInstitutionalAffiliation.getInstitutionShortName());

    createAccountAndDbUserWithAffiliation(verifiedInstitutionalAffiliation);
    verify(mockMailService).sendWelcomeEmail(any(), any(), any(), any(), any(), any());

    // don't send the user instructions email if the instructions have been deleted
    verifyNoMoreInteractions(mockMailService);
  }

  @Test
  public void testUpdateNihToken() {
    NihToken nihToken = new NihToken().jwt("test");
    createAccountAndDbUserWithAffiliation();
    profileController.updateNihToken(nihToken);
    verify(mockShibbolethService).updateShibbolethToken(eq(nihToken.getJwt()));
  }

  @Test
  public void testUpdateNihToken_serverError() {
    assertThrows(
        ServerErrorException.class,
        () -> {
          doThrow(new ServerErrorException())
              .when(mockShibbolethService)
              .updateShibbolethToken(any());
          profileController.updateNihToken(new NihToken().jwt("test"));
        });
  }

  @Test
  public void testUpdateNihToken_badRequest_1() {
    assertThrows(
        BadRequestException.class,
        () -> {
          profileController.updateNihToken(null);
        });
  }

  @Test
  public void testUpdateNihToken_badRequest_noJwt() {
    assertThrows(
        BadRequestException.class,
        () -> {
          profileController.updateNihToken(new NihToken());
        });
  }

  @Test
  public void testSyncEraCommons() {
    FirecloudNihStatus nihStatus = new FirecloudNihStatus();
    String linkedUsername = "linked";
    nihStatus.setLinkedNihUsername(linkedUsername);
    nihStatus.setLinkExpireTime(TIMESTAMP.getTime());
    when(mockFireCloudService.getNihStatus()).thenReturn(nihStatus);

    createAccountAndDbUserWithAffiliation();

    profileController.syncEraCommonsStatus();
    final DbUser user = userDao.findUserByUsername(FULL_USER_NAME);
    assertThat(user.getEraCommonsLinkedNihUsername()).isEqualTo(linkedUsername);
    assertThat(user.getEraCommonsLinkExpireTime()).isNotNull();
    assertThat(
            getCompletionEpochMillis(profileController.getMe().getBody(), AccessModule.ERA_COMMONS))
        .isNotNull();
  }

  @Test
  public void testDeleteProfile() {
    createAccountAndDbUserWithAffiliation();

    profileController.deleteProfile();
    verify(mockProfileAuditor).fireDeleteAction(dbUser.getUserId(), dbUser.getUsername());
  }

  @Test
  public void testUpdateProfile_updateDemographicSurvey() {
    createAccountAndDbUserWithAffiliation();
    Profile profile = profileController.getMe().getBody();

    DemographicSurvey demographicSurvey = profile.getDemographicSurvey();
    demographicSurvey.addRaceItem(Race.AA);
    demographicSurvey.setEthnicity(Ethnicity.HISPANIC);
    demographicSurvey.setIdentifiesAsLgbtq(true);
    demographicSurvey.setLgbtqIdentity("very");
    demographicSurvey.addGenderIdentityListItem(GenderIdentity.NONE_DESCRIBE_ME);
    demographicSurvey.addSexAtBirthItem(SexAtBirth.FEMALE);
    demographicSurvey.setYearOfBirth(new BigDecimal(2000));
    demographicSurvey.setEducation(Education.NO_EDUCATION);
    demographicSurvey.setDisability(Disability.FALSE);

    profile.setDemographicSurvey(demographicSurvey);

    profileController.updateProfile(profile);

    Profile updatedProfile = profileController.getMe().getBody();
    assertProfile(updatedProfile);
  }

  @Test
  public void testUpdateProfile_confirmsProfile() {
    final long initialTestTime = fakeClock.millis();

    createAccountAndDbUserWithAffiliation();
    Profile profile = profileController.getMe().getBody();
    assertThat(getCompletionEpochMillis(profile, AccessModule.PROFILE_CONFIRMATION))
        .isEqualTo(initialTestTime);

    // time passes

    fakeClock.increment(Duration.ofDays(1).toMillis());
    final long laterTime = fakeClock.millis();
    assertThat(laterTime).isGreaterThan(initialTestTime);

    // we make an arbitrary change

    profile.setProfessionalUrl("http://google.com/");
    profileController.updateProfile(profile);

    Profile updatedProfile = profileController.getMe().getBody();
    assertThat(getCompletionEpochMillis(updatedProfile, AccessModule.PROFILE_CONFIRMATION))
        .isEqualTo(laterTime);
  }

  @Test
  public void test_updateAccountProperties_null_user() {
    assertThrows(
        NotFoundException.class,
        () -> {
          profileService.updateAccountProperties(new AccountPropertyUpdate());
        });
  }

  @Test
  public void test_updateAccountProperties_user_not_found() {
    assertThrows(
        NotFoundException.class,
        () -> {
          final AccountPropertyUpdate request = new AccountPropertyUpdate().username("not found");
          profileService.updateAccountProperties(request);
        });
  }

  @Test
  public void test_updateAccountProperties_no_change() {
    final Profile original = createAccountAndDbUserWithAffiliation();

    // valid user but no fields updated
    final AccountPropertyUpdate request = new AccountPropertyUpdate().username(FULL_USER_NAME);
    final Profile retrieved = profileService.updateAccountProperties(request);

    // RW-5257 Demo Survey completion time is incorrectly updated
    retrieved.setDemographicSurveyCompletionTime(null);
    assertThat(retrieved).isEqualTo(original);
  }

  @Test
  public void test_updateAccountProperties_contactEmail() {
    // ProfileController.updateAccountProperties() is gated on ACCESS_CONTROL_ADMIN Authority
    // which is also checked in ProfileService.validateProfile()
    boolean grantAdminAuthority = true;

    // pre-affiliate with an Institution which will validate the user's existing
    // CONTACT_EMAIL and also a new one
    final String newContactEmail = "eric.lander@broadinstitute.org";

    final Institution broadPlus =
        new Institution()
            .shortName("Broad")
            .displayName("The Broad Institute")
            .addTierConfigsItem(
                rtAddressesConfig.emailAddresses(ImmutableList.of(CONTACT_EMAIL, newContactEmail)))
            .organizationTypeEnum(OrganizationType.ACADEMIC_RESEARCH_INSTITUTION);
    institutionService.createInstitution(broadPlus);

    final VerifiedInstitutionalAffiliation affiliation =
        new VerifiedInstitutionalAffiliation()
            .institutionShortName(broadPlus.getShortName())
            .institutionDisplayName(broadPlus.getDisplayName())
            .institutionalRoleEnum(InstitutionalRole.PROJECT_PERSONNEL);

    final Profile original =
        createAccountAndDbUserWithAffiliation(affiliation, grantAdminAuthority);
    assertThat(original.getContactEmail()).isEqualTo(CONTACT_EMAIL);

    final AccountPropertyUpdate request =
        new AccountPropertyUpdate().username(FULL_USER_NAME).contactEmail(newContactEmail);

    final Profile retrieved = profileService.updateAccountProperties(request);
    assertThat(retrieved.getContactEmail()).isEqualTo(newContactEmail);

    final Agent adminAgent = Agent.asAdmin(userDao.findUserByUserId(original.getUserId()));
    verify(mockProfileAuditor).fireUpdateAction(original, retrieved, adminAgent);
  }

  @Test
  public void test_updateAccountProperties_contactEmail_user() {
    assertThrows(
        BadRequestException.class,
        () -> {
          // ProfileController.updateAccountProperties() is gated on ACCESS_CONTROL_ADMIN Authority
          // which is also checked in ProfileService.validateProfile()
          boolean grantAdminAuthority = false;
          // pre-affiliate with an Institution which will validate the user's existing
          // CONTACT_EMAIL and also a new one
          final String newContactEmail = "eric.lander@broadinstitute.org";
          final Institution broadPlus =
              new Institution()
                  .shortName("Broad")
                  .displayName("The Broad Institute")
                  .addTierConfigsItem(
                      rtAddressesConfig.emailAddresses(
                          ImmutableList.of(CONTACT_EMAIL, newContactEmail)))
                  .organizationTypeEnum(OrganizationType.ACADEMIC_RESEARCH_INSTITUTION);
          institutionService.createInstitution(broadPlus);
          final VerifiedInstitutionalAffiliation affiliation =
              new VerifiedInstitutionalAffiliation()
                  .institutionShortName(broadPlus.getShortName())
                  .institutionDisplayName(broadPlus.getDisplayName())
                  .institutionalRoleEnum(InstitutionalRole.PROJECT_PERSONNEL);
          final Profile original =
              createAccountAndDbUserWithAffiliation(affiliation, grantAdminAuthority);
          assertThat(original.getContactEmail()).isEqualTo(CONTACT_EMAIL);
          final AccountPropertyUpdate request =
              new AccountPropertyUpdate().username(FULL_USER_NAME).contactEmail(newContactEmail);
          profileService.updateAccountProperties(request);
        });
  }

  @Test
  public void test_updateAccountProperties_contactEmail_no_match() {
    assertThrows(
        BadRequestException.class,
        () -> {
          // ProfileController.updateAccountProperties() is gated on ACCESS_CONTROL_ADMIN Authority
          // which is also checked in ProfileService.validateProfile()
          boolean grantAdminAuthority = true;
          // the existing Institution for this user only matches the single CONTACT_EMAIL
          createAccountAndDbUserWithAffiliation(grantAdminAuthority);
          final String newContactEmail = "eric.lander@broadinstitute.org";
          final AccountPropertyUpdate request =
              new AccountPropertyUpdate().username(FULL_USER_NAME).contactEmail(newContactEmail);
          profileService.updateAccountProperties(request);
        });
  }

  @Test
  public void test_updateAccountProperties_newAffiliation() {
    // ProfileController.updateAccountProperties() is gated on ACCESS_CONTROL_ADMIN Authority
    // which is also checked in ProfileService.validateProfile()
    boolean grantAdminAuthority = true;

    final VerifiedInstitutionalAffiliation expectedOriginalAffiliation =
        new VerifiedInstitutionalAffiliation()
            .institutionShortName("Broad")
            .institutionDisplayName("The Broad Institute")
            .institutionalRoleEnum(InstitutionalRole.PROJECT_PERSONNEL);
    final Profile original = createAccountAndDbUserWithAffiliation(grantAdminAuthority);

    assertThat(original.getVerifiedInstitutionalAffiliation())
        .isEqualTo(expectedOriginalAffiliation);

    // define a new affiliation which will match the user's existing CONTACT_EMAIL

    final Institution massGeneral =
        new Institution()
            .shortName("MGH123")
            .displayName("Massachusetts General Hospital")
            .addTierConfigsItem(rtAddressesConfig.emailAddresses(ImmutableList.of(CONTACT_EMAIL)))
            .organizationTypeEnum(OrganizationType.HEALTH_CENTER_NON_PROFIT);
    institutionService.createInstitution(massGeneral);

    final VerifiedInstitutionalAffiliation newAffiliation =
        new VerifiedInstitutionalAffiliation()
            .institutionShortName(massGeneral.getShortName())
            .institutionDisplayName(massGeneral.getDisplayName())
            .institutionalRoleEnum(InstitutionalRole.POST_DOCTORAL);

    final AccountPropertyUpdate request =
        new AccountPropertyUpdate().username(FULL_USER_NAME).affiliation(newAffiliation);
    final Profile retrieved = profileService.updateAccountProperties(request);
    assertThat(retrieved.getVerifiedInstitutionalAffiliation()).isEqualTo(newAffiliation);

    final Agent adminAgent = Agent.asAdmin(userDao.findUserByUserId(original.getUserId()));
    verify(mockProfileAuditor).fireUpdateAction(original, retrieved, adminAgent);
  }

  @Test
  public void test_updateAccountProperties_newAffiliation_no_match() {
    // ProfileController.updateAccountProperties() is gated on ACCESS_CONTROL_ADMIN Authority
    // which is also checked in ProfileService.validateProfile()
    boolean grantAdminAuthority = true;
    createAccountAndDbUserWithAffiliation(grantAdminAuthority);
    // define a new affiliation which will not match the user's CONTACT_EMAIL
    final Institution massGeneral =
        new Institution()
            .shortName("MGH123")
            .displayName("Massachusetts General Hospital")
            .addTierConfigsItem(
                rtDomainsConfig.emailDomains(ImmutableList.of("mgh.org", "massgeneral.hospital")))
            .organizationTypeEnum(OrganizationType.HEALTH_CENTER_NON_PROFIT);
    institutionService.createInstitution(massGeneral);
    final VerifiedInstitutionalAffiliation newAffiliation =
        new VerifiedInstitutionalAffiliation()
            .institutionShortName(massGeneral.getShortName())
            .institutionDisplayName(massGeneral.getDisplayName())
            .institutionalRoleEnum(InstitutionalRole.POST_DOCTORAL);
    final AccountPropertyUpdate request =
        new AccountPropertyUpdate().username(FULL_USER_NAME).affiliation(newAffiliation);
    assertThrows(BadRequestException.class, () -> profileService.updateAccountProperties(request));
  }

  @Test
  public void test_updateAccountProperties_newAffiliation_inst_not_found() {
    // ProfileController.updateAccountProperties() is gated on ACCESS_CONTROL_ADMIN Authority
    // which is also checked in ProfileService.validateProfile()
    boolean grantAdminAuthority = true;
    createAccountAndDbUserWithAffiliation(grantAdminAuthority);
    // define a new affiliation with an inst that doesn't exist
    final VerifiedInstitutionalAffiliation newAffiliation =
        new VerifiedInstitutionalAffiliation()
            .institutionShortName("not found")
            .institutionDisplayName("Not a real institution")
            .institutionalRoleEnum(InstitutionalRole.POST_DOCTORAL);
    final AccountPropertyUpdate request =
        new AccountPropertyUpdate().username(FULL_USER_NAME).affiliation(newAffiliation);
    assertThrows(NotFoundException.class, () -> profileService.updateAccountProperties(request));
  }

  @Test
  public void test_updateAccountProperties_remove_affiliation() {
    // ProfileController.updateAccountProperties() is gated on ACCESS_CONTROL_ADMIN Authority
    // which is also checked in ProfileService.validateProfile()
    boolean grantAdminAuthority = true;
    createAccountAndDbUserWithAffiliation(grantAdminAuthority);

    // define an affiliation without an institution
    final VerifiedInstitutionalAffiliation newAffiliation =
        new VerifiedInstitutionalAffiliation()
            .institutionalRoleEnum(InstitutionalRole.POST_DOCTORAL);
    final AccountPropertyUpdate request =
        new AccountPropertyUpdate().username(FULL_USER_NAME).affiliation(newAffiliation);
    assertThrows(NotFoundException.class, () -> profileService.updateAccountProperties(request));
  }

  @Test
  public void test_updateAccountProperties_contactEmail_newAffiliation_self_match() {
    // ProfileController.updateAccountProperties() is gated on ACCESS_CONTROL_ADMIN Authority
    // which is also checked in ProfileService.validateProfile()
    boolean grantAdminAuthority = true;

    final VerifiedInstitutionalAffiliation expectedOriginalAffiliation =
        new VerifiedInstitutionalAffiliation()
            .institutionShortName("Broad")
            .institutionDisplayName("The Broad Institute")
            .institutionalRoleEnum(InstitutionalRole.PROJECT_PERSONNEL);

    final Profile original = createAccountAndDbUserWithAffiliation(grantAdminAuthority);
    assertThat(original.getContactEmail()).isEqualTo(CONTACT_EMAIL);
    assertThat(original.getVerifiedInstitutionalAffiliation())
        .isEqualTo(expectedOriginalAffiliation);

    // update both the contact email and the affiliation, and validate against each other

    final String newContactEmail = "doctor@mgh.org";

    final Institution massGeneral =
        new Institution()
            .shortName("MGH123")
            .displayName("Massachusetts General Hospital")
            .addTierConfigsItem(
                rtDomainsConfig.emailDomains(ImmutableList.of("mgh.org", "massgeneral.hospital")))
            .organizationTypeEnum(OrganizationType.HEALTH_CENTER_NON_PROFIT);
    institutionService.createInstitution(massGeneral);

    final VerifiedInstitutionalAffiliation newAffiliation =
        new VerifiedInstitutionalAffiliation()
            .institutionShortName(massGeneral.getShortName())
            .institutionDisplayName(massGeneral.getDisplayName())
            .institutionalRoleEnum(InstitutionalRole.POST_DOCTORAL);

    final AccountPropertyUpdate request =
        new AccountPropertyUpdate()
            .username(FULL_USER_NAME)
            .contactEmail(newContactEmail)
            .affiliation(newAffiliation);
    final Profile retrieved = profileService.updateAccountProperties(request);
    assertThat(retrieved.getContactEmail()).isEqualTo(newContactEmail);
    assertThat(retrieved.getVerifiedInstitutionalAffiliation()).isEqualTo(newAffiliation);
    final Agent adminAgent = Agent.asAdmin(userDao.findUserByUserId(original.getUserId()));
    verify(mockProfileAuditor).fireUpdateAction(original, retrieved, adminAgent);
  }

  @Test
  public void test_updateAccountProperties_contactEmail_newAffiliation_no_match() {
    assertThrows(
        BadRequestException.class,
        () -> {
          // ProfileController.updateAccountProperties() is gated on ACCESS_CONTROL_ADMIN Authority
          // which is also checked in ProfileService.validateProfile()
          boolean grantAdminAuthority = true;
          final VerifiedInstitutionalAffiliation expectedOriginalAffiliation =
              new VerifiedInstitutionalAffiliation()
                  .institutionShortName("Broad")
                  .institutionDisplayName("The Broad Institute")
                  .institutionalRoleEnum(InstitutionalRole.PROJECT_PERSONNEL);
          final Profile original = createAccountAndDbUserWithAffiliation(grantAdminAuthority);
          assertThat(original.getContactEmail()).isEqualTo(CONTACT_EMAIL);
          assertThat(original.getVerifiedInstitutionalAffiliation())
              .isEqualTo(expectedOriginalAffiliation);
          // update both the contact email and the affiliation, and fail to validate against each
          // other
          final String newContactEmail = "notadoctor@hotmail.com";
          final Institution massGeneral =
              new Institution()
                  .shortName("MGH123")
                  .displayName("Massachusetts General Hospital")
                  .addTierConfigsItem(
                      rtDomainsConfig.emailDomains(
                          ImmutableList.of("mgh.org", "massgeneral.hospital")))
                  .organizationTypeEnum(OrganizationType.HEALTH_CENTER_NON_PROFIT);
          institutionService.createInstitution(massGeneral);
          final VerifiedInstitutionalAffiliation newAffiliation =
              new VerifiedInstitutionalAffiliation()
                  .institutionShortName(massGeneral.getShortName())
                  .institutionDisplayName(massGeneral.getDisplayName())
                  .institutionalRoleEnum(InstitutionalRole.POST_DOCTORAL);
          final AccountPropertyUpdate request =
              new AccountPropertyUpdate()
                  .username(FULL_USER_NAME)
                  .contactEmail(newContactEmail)
                  .affiliation(newAffiliation);
          profileService.updateAccountProperties(request);
        });
  }

  @Test
  public void test_updateAccountProperties_no_bypass_requests() {
    final Profile original = createAccountAndDbUserWithAffiliation();

    final AccountPropertyUpdate request =
        new AccountPropertyUpdate()
            .username(FULL_USER_NAME)
            .accessBypassRequests(Collections.emptyList());
    final Profile retrieved = profileService.updateAccountProperties(request);

    // RW-5257 Demo Survey completion time is incorrectly updated
    retrieved.setDemographicSurveyCompletionTime(null);
    assertThat(retrieved).isEqualTo(original);
  }

  @Test
  public void test_updateAccountProperties_bypass_requests() {
    final Profile original = createAccountAndDbUserWithAffiliation();

    // user has no bypasses at test start
    assertThat(getBypassEpochMillis(original, AccessModule.DATA_USER_CODE_OF_CONDUCT)).isNull();
    assertThat(getBypassEpochMillis(original, AccessModule.COMPLIANCE_TRAINING)).isNull();
    assertThat(getBypassEpochMillis(original, AccessModule.ERA_COMMONS)).isNull();
    assertThat(getBypassEpochMillis(original, AccessModule.TWO_FACTOR_AUTH)).isNull();

    final List<AccessBypassRequest> bypasses1 =
        ImmutableList.of(
            new AccessBypassRequest()
                .moduleName(AccessModule.DATA_USER_CODE_OF_CONDUCT)
                .isBypassed(true),
            // would un-bypass if a bypass had existed
            new AccessBypassRequest()
                .moduleName(AccessModule.COMPLIANCE_TRAINING)
                .isBypassed(false));

    final AccountPropertyUpdate request1 =
        new AccountPropertyUpdate().username(FULL_USER_NAME).accessBypassRequests(bypasses1);
    final Profile retrieved1 = profileService.updateAccountProperties(request1);

    // this is now bypassed
    assertThat(getBypassEpochMillis(retrieved1, AccessModule.DATA_USER_CODE_OF_CONDUCT))
        .isNotNull();
    // remains unbypassed because the flag was set to false
    assertThat(getBypassEpochMillis(retrieved1, AccessModule.COMPLIANCE_TRAINING)).isNull();
    // unchanged: unbypassed
    assertThat(getBypassEpochMillis(retrieved1, AccessModule.ERA_COMMONS)).isNull();
    assertThat(getBypassEpochMillis(retrieved1, AccessModule.TWO_FACTOR_AUTH)).isNull();

    final List<AccessBypassRequest> bypasses2 =
        ImmutableList.of(
            // un-bypass the previously bypassed
            new AccessBypassRequest()
                .moduleName(AccessModule.DATA_USER_CODE_OF_CONDUCT)
                .isBypassed(false),
            // bypass
            new AccessBypassRequest().moduleName(AccessModule.COMPLIANCE_TRAINING).isBypassed(true),
            new AccessBypassRequest().moduleName(AccessModule.ERA_COMMONS).isBypassed(true),
            new AccessBypassRequest().moduleName(AccessModule.TWO_FACTOR_AUTH).isBypassed(true));

    final AccountPropertyUpdate request2 = request1.accessBypassRequests(bypasses2);
    final Profile retrieved2 = profileService.updateAccountProperties(request2);

    // this is now unbypassed
    assertThat(getBypassEpochMillis(retrieved2, AccessModule.DATA_USER_CODE_OF_CONDUCT)).isNull();
    // these 3 are now bypassed
    assertThat(getBypassEpochMillis(retrieved2, AccessModule.COMPLIANCE_TRAINING)).isNotNull();
    assertThat(getBypassEpochMillis(retrieved2, AccessModule.ERA_COMMONS)).isNotNull();
    assertThat(getBypassEpochMillis(retrieved2, AccessModule.TWO_FACTOR_AUTH)).isNotNull();

    // TODO(RW-6930): Make Profile contain the new AccessModule block, then read from there.
    final Agent adminAgent = Agent.asAdmin(userDao.findUserByUserId(original.getUserId()));
    verify(mockProfileAuditor).fireUpdateAction(original, retrieved1, adminAgent);
    verify(mockProfileAuditor).fireUpdateAction(retrieved1, retrieved2, adminAgent);

    // DUCC and COMPLIANCE x2, one for each request
    verify(mockUserServiceAuditor, times(2))
        .fireAdministrativeBypassTime(
            eq(dbUser.getUserId()),
            eq(BypassTimeTargetProperty.DATA_USER_CODE_OF_CONDUCT),
            any(),
            any());
    verify(mockUserServiceAuditor, times(2))
        .fireAdministrativeBypassTime(
            eq(dbUser.getUserId()),
            eq(BypassTimeTargetProperty.RT_COMPLIANCE_TRAINING),
            any(),
            any());

    // ERA and 2FA once in request 2
    verify(mockUserServiceAuditor)
        .fireAdministrativeBypassTime(
            eq(dbUser.getUserId()), eq(BypassTimeTargetProperty.ERA_COMMONS), any(), any());
    verify(mockUserServiceAuditor)
        .fireAdministrativeBypassTime(
            eq(dbUser.getUserId()), eq(BypassTimeTargetProperty.TWO_FACTOR_AUTH), any(), any());
  }

  @Test
  public void test_updateAccountProperties_free_tier_quota() {
    createAccountAndDbUserWithAffiliation();

    final Double originalQuota = dbUser.getFreeTierCreditsLimitDollarsOverride();
    final Double newQuota = 123.4;

    final AccountPropertyUpdate request =
        new AccountPropertyUpdate().username(FULL_USER_NAME).freeCreditsLimit(newQuota);

    final Profile retrieved = profileService.updateAccountProperties(request);
    assertThat(retrieved.getFreeTierDollarQuota()).isWithin(0.01).of(newQuota);

    verify(mockUserServiceAuditor)
        .fireSetFreeTierDollarLimitOverride(dbUser.getUserId(), originalQuota, newQuota);
  }

  @Test
  public void test_updateAccountProperties_free_tier_quota_no_change() {
    final Profile original = createAccountAndDbUserWithAffiliation();

    final AccountPropertyUpdate request =
        new AccountPropertyUpdate()
            .username(FULL_USER_NAME)
            .freeCreditsLimit(original.getFreeTierDollarQuota());
    profileService.updateAccountProperties(request);

    verify(mockUserServiceAuditor, never())
        .fireSetFreeTierDollarLimitOverride(anyLong(), anyDouble(), anyDouble());
  }

  // don't set an override if the value to set is equal to the system default
  // and observe that the user's limit tracks with the default

  @Test
  public void test_updateAccountProperties_free_tier_quota_no_override() {
    config.billing.defaultFreeCreditsDollarLimit = 123.45;

    final Profile original = createAccountAndDbUserWithAffiliation();
    assertThat(original.getFreeTierDollarQuota()).isWithin(0.01).of(123.45);

    // update the default - the user's profile also updates

    config.billing.defaultFreeCreditsDollarLimit = 234.56;
    assertThat(profileService.getProfile(dbUser).getFreeTierDollarQuota())
        .isWithin(0.01)
        .of(234.56);

    // setting a Free Credits Limit equal to the default will not override

    final AccountPropertyUpdate request =
        new AccountPropertyUpdate()
            .username(FULL_USER_NAME)
            .freeCreditsLimit(config.billing.defaultFreeCreditsDollarLimit);
    profileService.updateAccountProperties(request);
    verify(mockUserServiceAuditor, never())
        .fireSetFreeTierDollarLimitOverride(anyLong(), anyDouble(), anyDouble());

    // the user's profile continues to track default changes

    config.billing.defaultFreeCreditsDollarLimit = 345.67;
    assertThat(profileService.getProfile(dbUser).getFreeTierDollarQuota())
        .isWithin(0.01)
        .of(345.67);
  }

  @Test
  public void linkRasAccount() {
    createAccountAndDbUserWithAffiliation();
    String loginGovUsername = "username@food.com";
    RasLinkRequestBody body = new RasLinkRequestBody();
    body.setAuthCode("code");
    body.setRedirectUrl("url");

    dbUser.setRasLinkUsername(loginGovUsername);
    dbUser = userDao.save(dbUser);
    accessModuleService.updateCompletionTime(dbUser, DbAccessModuleName.IDENTITY, TIMESTAMP);
    accessModuleService.updateCompletionTime(dbUser, DbAccessModuleName.RAS_LOGIN_GOV, TIMESTAMP);

    when(mockRasLinkService.linkRasAccount(body.getAuthCode(), body.getRedirectUrl()))
        .thenReturn(dbUser);

    final Profile profile = profileController.linkRasAccount(body).getBody();
    assertThat(profile.getRasLinkUsername()).isEqualTo(loginGovUsername);
    assertThat(getCompletionEpochMillis(profile, AccessModule.IDENTITY))
        .isEqualTo(TIMESTAMP.toInstant().toEpochMilli());
    assertThat(getCompletionEpochMillis(profile, AccessModule.RAS_LINK_LOGIN_GOV))
        .isEqualTo(TIMESTAMP.toInstant().toEpochMilli());
  }

  private Profile createAccountAndDbUserWithAffiliation(
      VerifiedInstitutionalAffiliation verifiedAffiliation, boolean grantAdminAuthority) {

    createAccountRequest.getProfile().setVerifiedInstitutionalAffiliation(verifiedAffiliation);

    Profile result = profileController.createAccount(createAccountRequest).getBody();

    // initialize the global test dbUser
    dbUser = userDao.findUserByUsername(FULL_USER_NAME);

    if (grantAdminAuthority) {
      dbUser.setAuthoritiesEnum(Collections.singleton(Authority.ACCESS_CONTROL_ADMIN));
    }

    dbUser = userDao.save(dbUser);

    // match dbUser updates

    result.setAuthorities(Lists.newArrayList(dbUser.getAuthoritiesEnum()));

    return result;
  }

  private Profile createAccountAndDbUserWithAffiliation(
      VerifiedInstitutionalAffiliation verifiedAffiliation) {
    boolean grantAdminAuthority = false;
    return createAccountAndDbUserWithAffiliation(verifiedAffiliation, grantAdminAuthority);
  }

  private Profile createAccountAndDbUserWithAffiliation() {
    return createAccountAndDbUserWithAffiliation(createVerifiedInstitutionalAffiliation());
  }

  private Profile createAccountAndDbUserWithAffiliation(boolean grantAdminAuthority) {
    return createAccountAndDbUserWithAffiliation(
        createVerifiedInstitutionalAffiliation(), grantAdminAuthority);
  }

  private void assertProfile(Profile profile) {
    assertThat(profile).isNotNull();
    assertThat(profile.getUsername()).isEqualTo(FULL_USER_NAME);
    assertThat(profile.getContactEmail()).isEqualTo(CONTACT_EMAIL);
    assertThat(profile.getFamilyName()).isEqualTo(FAMILY_NAME);
    assertThat(profile.getGivenName()).isEqualTo(GIVEN_NAME);
    assertThat(profile.getAccessTierShortNames()).isEmpty();
    assertThat(getCompletionEpochMillis(profile, AccessModule.PROFILE_CONFIRMATION))
        .isEqualTo(TIMESTAMP.getTime());
    assertThat(getCompletionEpochMillis(profile, AccessModule.PUBLICATION_CONFIRMATION))
        .isEqualTo(TIMESTAMP.getTime());

    DbUser user = userDao.findUserByUsername(FULL_USER_NAME);
    assertThat(user).isNotNull();
    assertThat(user.getContactEmail()).isEqualTo(CONTACT_EMAIL);
    assertThat(user.getFamilyName()).isEqualTo(FAMILY_NAME);
    assertThat(user.getGivenName()).isEqualTo(GIVEN_NAME);
    assertThat((double) user.getFirstSignInTime().getTime())
        .isWithin(TIME_TOLERANCE_MILLIS)
        .of(ProfileControllerTest.TIMESTAMP.getTime());
    assertThat(accessTierService.getAccessTierShortNamesForUser(user)).isEmpty();
  }

  private Long getCompletionEpochMillis(Profile profile, AccessModule accessModuleName) {
    return profile.getAccessModules().getModules().stream()
        .filter(m -> m.getModuleName() == accessModuleName)
        .findFirst()
        .map(AccessModuleStatus::getCompletionEpochMillis)
        .orElse(null);
  }

  private Long getBypassEpochMillis(Profile profile, AccessModule accessModuleName) {
    return profile.getAccessModules().getModules().stream()
        .filter(m -> m.getModuleName() == accessModuleName)
        .findFirst()
        .map(AccessModuleStatus::getBypassEpochMillis)
        .orElse(null);
  }

  private VerifiedInstitutionalAffiliation createVerifiedInstitutionalAffiliation() {
    final Institution broad =
        new Institution()
            .shortName("Broad")
            .displayName("The Broad Institute")
            .addTierConfigsItem(rtAddressesConfig.emailAddresses(ImmutableList.of(CONTACT_EMAIL)))
            .organizationTypeEnum(OrganizationType.ACADEMIC_RESEARCH_INSTITUTION);
    institutionService.createInstitution(broad);

    return new VerifiedInstitutionalAffiliation()
        .institutionShortName(broad.getShortName())
        .institutionDisplayName(broad.getDisplayName())
        .institutionalRoleEnum(InstitutionalRole.PROJECT_PERSONNEL);
  }
}
