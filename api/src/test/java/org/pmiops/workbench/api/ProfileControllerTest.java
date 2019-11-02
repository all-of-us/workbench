package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static junit.framework.TestCase.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.inject.Provider;
import javax.mail.MessagingException;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.pmiops.workbench.audit.adapters.ProfileAuditAdapterService;
import org.pmiops.workbench.auth.ProfileService;
import org.pmiops.workbench.auth.UserAuthentication;
import org.pmiops.workbench.auth.UserAuthentication.UserType;
import org.pmiops.workbench.billing.FreeTierBillingService;
import org.pmiops.workbench.compliance.ComplianceService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.config.WorkbenchEnvironment;
import org.pmiops.workbench.db.dao.AdminActionHistoryDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserDataUseAgreementDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.db.model.UserDataUseAgreement;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.NihStatus;
import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.google.DirectoryService;
import org.pmiops.workbench.mail.MailService;
import org.pmiops.workbench.model.AccessBypassRequest;
import org.pmiops.workbench.model.AccessModule;
import org.pmiops.workbench.model.CreateAccountRequest;
import org.pmiops.workbench.model.DataAccessLevel;
import org.pmiops.workbench.model.EmailVerificationStatus;
import org.pmiops.workbench.model.InstitutionalAffiliation;
import org.pmiops.workbench.model.InvitationVerificationRequest;
import org.pmiops.workbench.model.NihToken;
import org.pmiops.workbench.model.Profile;
import org.pmiops.workbench.model.ResendWelcomeEmailRequest;
import org.pmiops.workbench.model.UpdateContactEmailRequest;
import org.pmiops.workbench.notebooks.LeonardoNotebooksClient;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.test.FakeLongRandom;
import org.pmiops.workbench.test.Providers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class ProfileControllerTest {

  private static final Instant NOW = Instant.now();
  private static final Timestamp TIMESTAMP = new Timestamp(NOW.toEpochMilli());
  private static final long NONCE_LONG = 12345;
  private static final String NONCE = Long.toString(NONCE_LONG);
  private static final String USERNAME = "bob";
  private static final String GIVEN_NAME = "Bob";
  private static final String FAMILY_NAME = "Bobberson";
  private static final String CONTACT_EMAIL = "bob@example.com";
  private static final String INVITATION_KEY = "secretpassword";
  private static final String PRIMARY_EMAIL = "bob@researchallofus.org";
  private static final String BILLING_PROJECT_PREFIX = "all-of-us-free-";
  private static final String ORGANIZATION = "Test";
  private static final String CURRENT_POSITION = "Tester";
  private static final String RESEARCH_PURPOSE = "To test things";
  private static final int DUA_VERSION = 2;

  @Mock private Provider<User> userProvider;
  @Mock private Provider<UserAuthentication> userAuthenticationProvider;
  @Autowired private UserDao userDao;
  @Autowired private AdminActionHistoryDao adminActionHistoryDao;
  @Autowired private UserDataUseAgreementDao userDataUseAgreementDao;
  @Mock private FireCloudService fireCloudService;
  @Mock private LeonardoNotebooksClient leonardoNotebooksClient;
  @Mock private DirectoryService directoryService;
  @Mock private CloudStorageService cloudStorageService;
  @Mock private FreeTierBillingService freeTierBillingService;
  @Mock private ComplianceService complianceTrainingService;
  @Mock private MailService mailService;
  @Mock private UserService userService;
  @Mock private ProfileAuditAdapterService mockProfileAuditAdapterService;

  private ProfileController profileController;
  private ProfileController cloudProfileController;
  private CreateAccountRequest createAccountRequest;
  private InvitationVerificationRequest invitationVerificationRequest;
  private com.google.api.services.directory.model.User googleUser;
  private FakeClock clock;
  private User user;

  @Rule public final ExpectedException exception = ExpectedException.none();

  @Before
  public void setUp() throws MessagingException {
    WorkbenchConfig config = generateConfig();

    WorkbenchEnvironment environment = new WorkbenchEnvironment(true, "appId");
    WorkbenchEnvironment cloudEnvironment = new WorkbenchEnvironment(false, "appId");
    createAccountRequest = new CreateAccountRequest();
    invitationVerificationRequest = new InvitationVerificationRequest();
    Profile profile = new Profile();
    profile.setContactEmail(CONTACT_EMAIL);
    profile.setFamilyName(FAMILY_NAME);
    profile.setGivenName(GIVEN_NAME);
    profile.setUsername(USERNAME);
    profile.setCurrentPosition(CURRENT_POSITION);
    profile.setOrganization(ORGANIZATION);
    profile.setAreaOfResearch(RESEARCH_PURPOSE);
    createAccountRequest.setProfile(profile);
    createAccountRequest.setInvitationKey(INVITATION_KEY);
    invitationVerificationRequest.setInvitationKey(INVITATION_KEY);
    googleUser = new com.google.api.services.directory.model.User();
    googleUser.setPrimaryEmail(PRIMARY_EMAIL);
    googleUser.setChangePasswordAtNextLogin(true);
    googleUser.setPassword("testPassword");
    googleUser.setIsEnrolledIn2Sv(true);

    clock = new FakeClock(NOW);

    doNothing().when(mailService).sendBetaAccessRequestEmail(Mockito.any());
    userService =
        new UserService(
            userProvider,
            userDao,
            adminActionHistoryDao,
            userDataUseAgreementDao,
            clock,
            new FakeLongRandom(NONCE_LONG),
            fireCloudService,
            Providers.of(config),
            complianceTrainingService,
            directoryService);
    ProfileService profileService = new ProfileService(userDao, freeTierBillingService);
    this.profileController =
        new ProfileController(
            profileService,
            userProvider,
            userAuthenticationProvider,
            userDao,
            clock,
            userService,
            fireCloudService,
            directoryService,
            cloudStorageService,
            leonardoNotebooksClient,
            Providers.of(config),
            environment,
            Providers.of(mailService),
            mockAct);
    this.cloudProfileController =
        new ProfileController(
            profileService,
            userProvider,
            userAuthenticationProvider,
            userDao,
            clock,
            userService,
            fireCloudService,
            directoryService,
            cloudStorageService,
            leonardoNotebooksClient,
            Providers.of(config),
            cloudEnvironment,
            Providers.of(mailService));
    when(directoryService.getUser(PRIMARY_EMAIL)).thenReturn(googleUser);
  }

  @Test(expected = BadRequestException.class)
  public void testCreateAccount_invitationKeyMismatch() throws Exception {
    when(cloudStorageService.readInvitationKey()).thenReturn("BLAH");
    profileController.createAccount(createAccountRequest);
  }

  @Test(expected = BadRequestException.class)
  public void testInvitationKeyVerification_invitationKeyMismatch() throws Exception {
    profileController.invitationKeyVerification(invitationVerificationRequest);
  }

  @Test
  public void testCreateAccount_success() throws Exception {
    createUser();
    User user = userDao.findUserByEmail(PRIMARY_EMAIL);
    assertThat(user).isNotNull();
    assertThat(user.getDataAccessLevelEnum()).isEqualTo(DataAccessLevel.UNREGISTERED);
  }

  @Test
  public void testCreateAccount_invalidUser() throws Exception {
    when(cloudStorageService.readInvitationKey()).thenReturn(INVITATION_KEY);
    CreateAccountRequest accountRequest = new CreateAccountRequest();
    accountRequest.setInvitationKey(INVITATION_KEY);
    createAccountRequest.getProfile().setUsername("12");
    accountRequest.setProfile(createAccountRequest.getProfile());
    exception.expect(BadRequestException.class);
    exception.expectMessage(
        "Username should be at least 3 characters and not more than 64 characters");
    profileController.createAccount(accountRequest);
  }

  @Test
  public void testSubmitDemographicSurvey_success() throws Exception {
    createUser();
    assertThat(profileController.submitDemographicsSurvey().getStatusCode())
        .isEqualTo(HttpStatus.NOT_IMPLEMENTED);
  }

  @Test
  public void testSubmitDataUseAgreement_success() throws Exception {
    createUser();
    assertThat(profileController.submitDataUseAgreement(DUA_VERSION, "NIH").getStatusCode())
        .isEqualTo(HttpStatus.OK);
  }

  @Test
  public void testMe_success() throws Exception {
    createUser();

    Profile profile = profileController.getMe().getBody();
    assertProfile(
        profile,
        PRIMARY_EMAIL,
        CONTACT_EMAIL,
        FAMILY_NAME,
        GIVEN_NAME,
        DataAccessLevel.UNREGISTERED,
        TIMESTAMP,
        null);
    verify(fireCloudService).registerUser(CONTACT_EMAIL, GIVEN_NAME, FAMILY_NAME);
  }

  @Test
  public void testMe_userBeforeNotLoggedInSuccess() throws Exception {
    createUser();
    Profile profile = profileController.getMe().getBody();
    assertProfile(
        profile,
        PRIMARY_EMAIL,
        CONTACT_EMAIL,
        FAMILY_NAME,
        GIVEN_NAME,
        DataAccessLevel.UNREGISTERED,
        TIMESTAMP,
        null);
    verify(fireCloudService).registerUser(CONTACT_EMAIL, GIVEN_NAME, FAMILY_NAME);

    // An additional call to getMe() should have no effect.
    clock.increment(1);
    profile = profileController.getMe().getBody();
    assertProfile(
        profile,
        PRIMARY_EMAIL,
        CONTACT_EMAIL,
        FAMILY_NAME,
        GIVEN_NAME,
        DataAccessLevel.UNREGISTERED,
        TIMESTAMP,
        null);
  }

  @Test
  public void testMe_institutionalAffiliationsAlphabetical() throws Exception {
    createUser();

    Profile profile = profileController.getMe().getBody();
    ArrayList<InstitutionalAffiliation> affiliations = new ArrayList<InstitutionalAffiliation>();
    InstitutionalAffiliation first = new InstitutionalAffiliation();
    first.setRole("test");
    first.setInstitution("Institution");
    InstitutionalAffiliation second = new InstitutionalAffiliation();
    second.setRole("zeta");
    second.setInstitution("Zeta");
    affiliations.add(first);
    affiliations.add(second);
    profile.setInstitutionalAffiliations(affiliations);
    profileController.updateProfile(profile);

    Profile result = profileController.getMe().getBody();
    assertThat(result.getInstitutionalAffiliations().size()).isEqualTo(2);
    assertThat(result.getInstitutionalAffiliations().get(0)).isEqualTo(first);
    assertThat(result.getInstitutionalAffiliations().get(1)).isEqualTo(second);
  }

  @Test
  public void testMe_institutionalAffiliationsNotAlphabetical() throws Exception {
    createUser();

    Profile profile = profileController.getMe().getBody();
    ArrayList<InstitutionalAffiliation> affiliations = new ArrayList<InstitutionalAffiliation>();
    InstitutionalAffiliation first = new InstitutionalAffiliation();
    first.setRole("zeta");
    first.setInstitution("Zeta");
    InstitutionalAffiliation second = new InstitutionalAffiliation();
    second.setRole("test");
    second.setInstitution("Institution");
    affiliations.add(first);
    affiliations.add(second);
    profile.setInstitutionalAffiliations(affiliations);
    profileController.updateProfile(profile);

    Profile result = profileController.getMe().getBody();
    assertThat(result.getInstitutionalAffiliations().size()).isEqualTo(2);
    assertThat(result.getInstitutionalAffiliations().get(0)).isEqualTo(first);
    assertThat(result.getInstitutionalAffiliations().get(1)).isEqualTo(second);
  }

  @Test
  public void testMe_removeSingleInstitutionalAffiliation() throws Exception {
    createUser();

    Profile profile = profileController.getMe().getBody();
    ArrayList<InstitutionalAffiliation> affiliations = new ArrayList<InstitutionalAffiliation>();
    InstitutionalAffiliation first = new InstitutionalAffiliation();
    first.setRole("test");
    first.setInstitution("Institution");
    InstitutionalAffiliation second = new InstitutionalAffiliation();
    second.setRole("zeta");
    second.setInstitution("Zeta");
    affiliations.add(first);
    affiliations.add(second);
    profile.setInstitutionalAffiliations(affiliations);
    profileController.updateProfile(profile);
    affiliations = new ArrayList<InstitutionalAffiliation>();
    affiliations.add(first);
    profile.setInstitutionalAffiliations(affiliations);
    profileController.updateProfile(profile);
    Profile result = profileController.getMe().getBody();
    assertThat(result.getInstitutionalAffiliations().size()).isEqualTo(1);
    assertThat(result.getInstitutionalAffiliations().get(0)).isEqualTo(first);
  }

  @Test
  public void testMe_removeAllInstitutionalAffiliations() throws Exception {
    createUser();

    Profile profile = profileController.getMe().getBody();
    ArrayList<InstitutionalAffiliation> affiliations = new ArrayList<InstitutionalAffiliation>();
    InstitutionalAffiliation first = new InstitutionalAffiliation();
    first.setRole("test");
    first.setInstitution("Institution");
    InstitutionalAffiliation second = new InstitutionalAffiliation();
    second.setRole("zeta");
    second.setInstitution("Zeta");
    affiliations.add(first);
    affiliations.add(second);
    profile.setInstitutionalAffiliations(affiliations);
    profileController.updateProfile(profile);
    affiliations.clear();
    profile.setInstitutionalAffiliations(affiliations);
    profileController.updateProfile(profile);
    Profile result = profileController.getMe().getBody();
    assertThat(result.getInstitutionalAffiliations().size()).isEqualTo(0);
  }

  @Test
  public void updateContactEmail_forbidden() throws Exception {
    createUser();
    user.setFirstSignInTime(new Timestamp(new Date().getTime()));
    String originalEmail = user.getContactEmail();

    ResponseEntity<Void> response =
        profileController.updateContactEmail(
            new UpdateContactEmailRequest()
                .contactEmail("newContactEmail@whatever.com")
                .username(user.getEmail())
                .creationNonce(NONCE));
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(user.getContactEmail()).isEqualTo(originalEmail);
  }

  @Test
  public void updateContactEmail_badRequest() throws Exception {
    createUser();
    when(directoryService.resetUserPassword(anyString())).thenReturn(googleUser);
    user.setFirstSignInTime(null);
    String originalEmail = user.getContactEmail();

    ResponseEntity<Void> response =
        profileController.updateContactEmail(
            new UpdateContactEmailRequest()
                .contactEmail("bad email address *(SD&(*D&F&*(DS ")
                .username(user.getEmail())
                .creationNonce(NONCE));
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(user.getContactEmail()).isEqualTo(originalEmail);
  }

  @Test
  public void updateContactEmail_OK() throws Exception {
    createUser();
    user.setFirstSignInTime(null);
    when(directoryService.resetUserPassword(anyString())).thenReturn(googleUser);

    ResponseEntity<Void> response =
        profileController.updateContactEmail(
            new UpdateContactEmailRequest()
                .contactEmail("newContactEmail@whatever.com")
                .username(user.getEmail())
                .creationNonce(NONCE));
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(user.getContactEmail()).isEqualTo("newContactEmail@whatever.com");
  }

  @Test
  public void updateName_alsoUpdatesDua() throws Exception {
    createUser();
    Profile profile = profileController.getMe().getBody();
    profile.setGivenName("OldGivenName");
    profile.setFamilyName("OldFamilyName");
    profileController.updateProfile(profile);
    profileController.submitDataUseAgreement(DUA_VERSION, "O.O.");
    profile.setGivenName("NewGivenName");
    profile.setFamilyName("NewFamilyName");
    profileController.updateProfile(profile);
    List<UserDataUseAgreement> duas =
        userDataUseAgreementDao.findByUserIdOrderByCompletionTimeDesc(profile.getUserId());
    assertThat(duas.get(0).isUserNameOutOfDate()).isTrue();
  }

  @Test(expected = BadRequestException.class)
  public void updateGivenName_badRequest() throws Exception {
    createUser();
    Profile profile = profileController.getMe().getBody();
    String newName =
        "obladidobladalifegoesonyalalalalalifegoesonobladioblada" + "lifegoesonrahlalalalifegoeson";
    profile.setGivenName(newName);
    profileController.updateProfile(profile);
  }

  @Test(expected = BadRequestException.class)
  public void updateFamilyName_badRequest() throws Exception {
    createUser();
    Profile profile = profileController.getMe().getBody();
    String newName =
        "obladidobladalifegoesonyalalalalalifegoesonobladioblada" + "lifegoesonrahlalalalifegoeson";
    profile.setFamilyName(newName);
    profileController.updateProfile(profile);
  }

  @Test(expected = BadRequestException.class)
  public void updateCurrentPosition_badRequest() throws Exception {
    createUser();
    Profile profile = profileController.getMe().getBody();
    profile.setCurrentPosition(RandomStringUtils.random(256));
    profileController.updateProfile(profile);
  }

  @Test(expected = BadRequestException.class)
  public void updateOrganization_badRequest() throws Exception {
    createUser();
    Profile profile = profileController.getMe().getBody();
    profile.setOrganization(RandomStringUtils.random(256));
    profileController.updateProfile(profile);
  }

  @Test
  public void resendWelcomeEmail_messagingException() throws Exception {
    createUser();
    user.setFirstSignInTime(null);
    when(directoryService.resetUserPassword(anyString())).thenReturn(googleUser);
    doThrow(new MessagingException("exception"))
        .when(mailService)
        .sendWelcomeEmail(any(), any(), any());

    ResponseEntity<Void> response =
        profileController.resendWelcomeEmail(
            new ResendWelcomeEmailRequest().username(user.getEmail()).creationNonce(NONCE));
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    // called twice, once during account creation, once on resend
    verify(mailService, times(2)).sendWelcomeEmail(any(), any(), any());
    verify(directoryService, times(1)).resetUserPassword(anyString());
  }

  @Test
  public void resendWelcomeEmail_OK() throws Exception {
    createUser();
    when(directoryService.resetUserPassword(anyString())).thenReturn(googleUser);
    doNothing().when(mailService).sendWelcomeEmail(any(), any(), any());

    ResponseEntity<Void> response =
        profileController.resendWelcomeEmail(
            new ResendWelcomeEmailRequest().username(user.getEmail()).creationNonce(NONCE));
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    // called twice, once during account creation, once on resend
    verify(mailService, times(2)).sendWelcomeEmail(any(), any(), any());
    verify(directoryService, times(1)).resetUserPassword(anyString());
  }

  @Test
  public void testUpdateNihToken() {
    when(fireCloudService.postNihCallback(any()))
        .thenReturn(new NihStatus().linkedNihUsername("test").linkExpireTime(500L));
    try {
      createUser();
      profileController.updateNihToken(new NihToken().jwt("test"));
    } catch (Exception e) {
      fail();
    }
  }

  @Test(expected = BadRequestException.class)
  public void testUpdateNihToken_badRequest_1() {
    profileController.updateNihToken(null);
  }

  @Test(expected = BadRequestException.class)
  public void testUpdateNihToken_badRequest_2() {
    profileController.updateNihToken(new NihToken());
  }

  @Test(expected = ServerErrorException.class)
  public void testUpdateNihToken_serverError() {
    doThrow(new ServerErrorException()).when(fireCloudService).postNihCallback(any());
    profileController.updateNihToken(new NihToken().jwt("test"));
  }

  @Test
  public void testSyncEraCommons() throws Exception {
    NihStatus nihStatus = new NihStatus();
    String linkedUsername = "linked";
    nihStatus.setLinkedNihUsername(linkedUsername);
    nihStatus.setLinkExpireTime(TIMESTAMP.getTime());
    when(fireCloudService.getNihStatus()).thenReturn(nihStatus);

    createUser();

    profileController.syncEraCommonsStatus();
    assertThat(userDao.findUserByEmail(PRIMARY_EMAIL).getEraCommonsLinkedNihUsername())
        .isEqualTo(linkedUsername);
    assertThat(userDao.findUserByEmail(PRIMARY_EMAIL).getEraCommonsLinkExpireTime()).isNotNull();
    assertThat(userDao.findUserByEmail(PRIMARY_EMAIL).getEraCommonsCompletionTime()).isNotNull();
  }

  @Test
  public void testBypassAccessModule() throws Exception {
    Profile profile = createUser();
    userService = spy(userService);
    WorkbenchEnvironment environment = new WorkbenchEnvironment(true, "appId");
    WorkbenchConfig config = generateConfig();
    ProfileService profileService = new ProfileService(userDao, freeTierBillingService);
    this.profileController =
        new ProfileController(
            profileService,
            userProvider,
            userAuthenticationProvider,
            userDao,
            clock,
            userService,
            fireCloudService,
            directoryService,
            cloudStorageService,
            leonardoNotebooksClient,
            Providers.of(config),
            environment,
            Providers.of(mailService),
            mockProfileAuditAdapterService);
    profileController.bypassAccessRequirement(
        profile.getUserId(),
        new AccessBypassRequest().isBypassed(true).moduleName(AccessModule.DATA_USE_AGREEMENT));
    verify(userService, times(1)).setDataUseAgreementBypassTime(any(), any());
  }

  private Profile createUser() throws Exception {
    when(cloudStorageService.readInvitationKey()).thenReturn(INVITATION_KEY);
    when(directoryService.createUser(GIVEN_NAME, FAMILY_NAME, USERNAME, CONTACT_EMAIL))
        .thenReturn(googleUser);
    Profile result = profileController.createAccount(createAccountRequest).getBody();
    user = userDao.findUserByEmail(PRIMARY_EMAIL);
    user.setEmailVerificationStatusEnum(EmailVerificationStatus.SUBSCRIBED);
    userDao.save(user);
    when(userProvider.get()).thenReturn(user);
    when(userAuthenticationProvider.get())
        .thenReturn(new UserAuthentication(user, null, null, UserType.RESEARCHER));
    return result;
  }

  private void assertProfile(
      Profile profile,
      String primaryEmail,
      String contactEmail,
      String familyName,
      String givenName,
      DataAccessLevel dataAccessLevel,
      Timestamp firstSignInTime,
      Boolean contactEmailFailure) {
    assertThat(profile).isNotNull();
    assertThat(profile.getContactEmail()).isEqualTo(contactEmail);
    assertThat(profile.getFamilyName()).isEqualTo(familyName);
    assertThat(profile.getGivenName()).isEqualTo(givenName);
    assertThat(profile.getDataAccessLevel()).isEqualTo(dataAccessLevel);
    assertThat(profile.getContactEmailFailure()).isEqualTo(contactEmailFailure);
    assertUser(primaryEmail, contactEmail, familyName, givenName, dataAccessLevel, firstSignInTime);
  }

  private void assertUser(
      String primaryEmail,
      String contactEmail,
      String familyName,
      String givenName,
      DataAccessLevel dataAccessLevel,
      Timestamp firstSignInTime) {
    User user = userDao.findUserByEmail(primaryEmail);
    assertThat(user).isNotNull();
    assertThat(user.getContactEmail()).isEqualTo(contactEmail);
    assertThat(user.getFamilyName()).isEqualTo(familyName);
    assertThat(user.getGivenName()).isEqualTo(givenName);
    assertThat(user.getDataAccessLevelEnum()).isEqualTo(dataAccessLevel);
    assertThat(user.getFirstSignInTime()).isEqualTo(firstSignInTime);
    assertThat(user.getDataAccessLevelEnum()).isEqualTo(dataAccessLevel);
  }

  private WorkbenchConfig generateConfig() {
    WorkbenchConfig config = WorkbenchConfig.createEmptyConfig();
    config.billing.projectNamePrefix = BILLING_PROJECT_PREFIX;
    config.billing.retryCount = 2;
    config.firecloud.registeredDomainName = "";
    config.access.enableComplianceTraining = false;
    config.admin.adminIdVerification = "adminIdVerify@dummyMockEmail.com";
    // All access modules are enabled for these tests. So completing any one module should maintain
    // UNREGISTERED status.
    config.access.enableComplianceTraining = true;
    config.access.enableBetaAccess = true;
    config.access.enableEraCommons = true;
    config.access.enableDataUseAgreement = true;
    return config;
  }
}
