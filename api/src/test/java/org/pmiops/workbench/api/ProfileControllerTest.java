package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static junit.framework.TestCase.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import javax.mail.MessagingException;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.pmiops.workbench.actionaudit.auditors.ProfileAuditor;
import org.pmiops.workbench.actionaudit.auditors.UserServiceAuditor;
import org.pmiops.workbench.auth.ProfileService;
import org.pmiops.workbench.auth.UserAuthentication;
import org.pmiops.workbench.auth.UserAuthentication.UserType;
import org.pmiops.workbench.billing.FreeTierBillingService;
import org.pmiops.workbench.compliance.ComplianceService;
import org.pmiops.workbench.db.dao.AdminActionHistoryDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserDataUseAgreementDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.dao.UserServiceImpl;
import org.pmiops.workbench.db.dao.UserTermsOfServiceDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserDataUseAgreement;
import org.pmiops.workbench.db.model.DbUserTermsOfService;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudNihStatus;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
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

@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
public class ProfileControllerTest extends BaseControllerTest {

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

  @MockBean private FireCloudService fireCloudService;
  @MockBean private LeonardoNotebooksClient leonardoNotebooksClient;
  @MockBean private DirectoryService directoryService;
  @MockBean private CloudStorageService cloudStorageService;
  @MockBean private FreeTierBillingService freeTierBillingService;
  @MockBean private ComplianceService complianceTrainingService;
  @MockBean private MailService mailService;
  @MockBean private ProfileAuditor mockProfileAuditor;
  @MockBean private UserServiceAuditor mockUserServiceAuditAdapter;

  @Autowired private UserDao userDao;
  @Autowired private AdminActionHistoryDao adminActionHistoryDao;
  @Autowired private UserDataUseAgreementDao userDataUseAgreementDao;
  @Autowired private UserTermsOfServiceDao userTermsOfServiceDao;
  @Autowired private UserService userService;
  @Autowired private ProfileService profileService;
  @Autowired private ProfileController profileController;

  private CreateAccountRequest createAccountRequest;
  private InvitationVerificationRequest invitationVerificationRequest;
  private com.google.api.services.directory.model.User googleUser;
  private static FakeClock fakeClock = new FakeClock(NOW);

  private static DbUser dbUser;

  @Rule public final ExpectedException exception = ExpectedException.none();

  @TestConfiguration
  @Import({UserServiceImpl.class, ProfileService.class, ProfileController.class})
  static class Configuration {
    @Bean
    Clock clock() {
      return fakeClock;
    }

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
  }

  @Before
  @Override
  public void setUp() throws IOException {
    super.setUp();

    Profile profile = new Profile();
    profile.setContactEmail(CONTACT_EMAIL);
    profile.setFamilyName(FAMILY_NAME);
    profile.setGivenName(GIVEN_NAME);
    profile.setUsername(USERNAME);
    profile.setCurrentPosition(CURRENT_POSITION);
    profile.setOrganization(ORGANIZATION);
    profile.setAreaOfResearch(RESEARCH_PURPOSE);
    createAccountRequest = new CreateAccountRequest();
    createAccountRequest.setProfile(profile);
    createAccountRequest.setInvitationKey(INVITATION_KEY);

    invitationVerificationRequest = new InvitationVerificationRequest();
    invitationVerificationRequest.setInvitationKey(INVITATION_KEY);

    googleUser = new com.google.api.services.directory.model.User();
    googleUser.setPrimaryEmail(PRIMARY_EMAIL);
    googleUser.setChangePasswordAtNextLogin(true);
    googleUser.setPassword("testPassword");
    googleUser.setIsEnrolledIn2Sv(true);
    when(directoryService.getUser(PRIMARY_EMAIL)).thenReturn(googleUser);

    try {
      doNothing().when(mailService).sendBetaAccessRequestEmail(Mockito.any());
    } catch (MessagingException e) {
      e.printStackTrace();
    }
  }

  @Test(expected = BadRequestException.class)
  public void testCreateAccount_invitationKeyMismatch() throws Exception {
    createUser();

    when(cloudStorageService.readInvitationKey()).thenReturn("BLAH");
    profileController.createAccount(createAccountRequest);
  }

  @Test
  public void testCreateAccount_noRequireInvitationKey() throws Exception {
    createUser();

    // When invitation key verification is turned off, even a bad invitation key should
    // allow a user to be created.
    config.access.requireInvitationKey = false;
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
    verify(mockProfileAuditor).fireCreateAction(any(Profile.class));
    final DbUser dbUser = userDao.findUserByUsername(PRIMARY_EMAIL);
    assertThat(dbUser).isNotNull();
    assertThat(dbUser.getDataAccessLevelEnum()).isEqualTo(DataAccessLevel.UNREGISTERED);
  }

  @Test
  public void testCreateAccount_withTosVersion() throws Exception {
    createAccountRequest.setTermsOfServiceVersion(1);
    createUser();

    final DbUser dbUser = userDao.findUserByUsername(PRIMARY_EMAIL);
    final List<DbUserTermsOfService> tosRows = Lists.newArrayList(userTermsOfServiceDao.findAll());
    assertThat(tosRows.size()).isEqualTo(1);
    assertThat(tosRows.get(0).getTosVersion()).isEqualTo(1);
    assertThat(tosRows.get(0).getUserId()).isEqualTo(dbUser.getUserId());
    assertThat(tosRows.get(0).getAgreementTime()).isNotNull();
    Profile profile = profileService.getProfile(dbUser);
    assertThat(profile.getLatestTermsOfServiceVersion()).isEqualTo(1);
  }

  @Test(expected = BadRequestException.class)
  public void testCreateAccount_withBadTosVersion() throws Exception {
    createAccountRequest.setTermsOfServiceVersion(999);
    createUser();
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
    verify(mockProfileAuditor).fireCreateAction(any(Profile.class));
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
    String duaInitials = "NIH";
    assertThat(profileController.submitDataUseAgreement(DUA_VERSION, duaInitials).getStatusCode())
        .isEqualTo(HttpStatus.OK);
    List<DbUserDataUseAgreement> dbUserDataUseAgreementList =
        userDataUseAgreementDao.findByUserIdOrderByCompletionTimeDesc(dbUser.getUserId());
    assertThat(dbUserDataUseAgreementList.size()).isEqualTo(1);
    DbUserDataUseAgreement dbUserDataUseAgreement = dbUserDataUseAgreementList.get(0);
    assertThat(dbUserDataUseAgreement.getUserFamilyName()).isEqualTo(dbUser.getFamilyName());
    assertThat(dbUserDataUseAgreement.getUserGivenName()).isEqualTo(dbUser.getGivenName());
    assertThat(dbUserDataUseAgreement.getUserInitials()).isEqualTo(duaInitials);
    assertThat(dbUserDataUseAgreement.getDataUseAgreementSignedVersion()).isEqualTo(DUA_VERSION);
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
        false);
    verify(fireCloudService).registerUser(CONTACT_EMAIL, GIVEN_NAME, FAMILY_NAME);
    verify(mockProfileAuditor).fireLoginAction(dbUser);
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
        false);
    verify(fireCloudService).registerUser(CONTACT_EMAIL, GIVEN_NAME, FAMILY_NAME);

    // An additional call to getMe() should have no effect.
    fakeClock.increment(1);
    profile = profileController.getMe().getBody();
    assertProfile(
        profile,
        PRIMARY_EMAIL,
        CONTACT_EMAIL,
        FAMILY_NAME,
        GIVEN_NAME,
        DataAccessLevel.UNREGISTERED,
        TIMESTAMP,
        false);
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
    dbUser.setFirstSignInTime(new Timestamp(new Date().getTime()));
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
  public void updateContactEmail_badRequest() throws Exception {
    createUser();
    when(directoryService.resetUserPassword(anyString())).thenReturn(googleUser);
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
  public void updateContactEmail_OK() throws Exception {
    createUser();
    dbUser.setFirstSignInTime(null);
    when(directoryService.resetUserPassword(anyString())).thenReturn(googleUser);

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
    List<DbUserDataUseAgreement> duas =
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
    config.featureFlags.enableNewAccountCreation = false;
    createUser();
    Profile profile = profileController.getMe().getBody();
    profile.setCurrentPosition(RandomStringUtils.random(256));
    profileController.updateProfile(profile);
  }

  @Test(expected = BadRequestException.class)
  public void updateOrganization_badRequest() throws Exception {
    config.featureFlags.enableNewAccountCreation = false;
    createUser();
    Profile profile = profileController.getMe().getBody();
    profile.setOrganization(RandomStringUtils.random(256));
    profileController.updateProfile(profile);
  }

  @Test
  public void resendWelcomeEmail_messagingException() throws Exception {
    createUser();
    dbUser.setFirstSignInTime(null);
    when(directoryService.resetUserPassword(anyString())).thenReturn(googleUser);
    doThrow(new MessagingException("exception"))
        .when(mailService)
        .sendWelcomeEmail(any(), any(), any());

    ResponseEntity<Void> response =
        profileController.resendWelcomeEmail(
            new ResendWelcomeEmailRequest().username(dbUser.getUsername()).creationNonce(NONCE));
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
            new ResendWelcomeEmailRequest().username(dbUser.getUsername()).creationNonce(NONCE));
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    // called twice, once during account creation, once on resend
    verify(mailService, times(2)).sendWelcomeEmail(any(), any(), any());
    verify(directoryService, times(1)).resetUserPassword(anyString());
  }

  @Test
  public void testUpdateNihToken() {
    when(fireCloudService.postNihCallback(any()))
        .thenReturn(new FirecloudNihStatus().linkedNihUsername("test").linkExpireTime(500L));
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
    FirecloudNihStatus nihStatus = new FirecloudNihStatus();
    String linkedUsername = "linked";
    nihStatus.setLinkedNihUsername(linkedUsername);
    nihStatus.setLinkExpireTime(TIMESTAMP.getTime());
    when(fireCloudService.getNihStatus()).thenReturn(nihStatus);

    createUser();

    profileController.syncEraCommonsStatus();
    assertThat(userDao.findUserByUsername(PRIMARY_EMAIL).getEraCommonsLinkedNihUsername())
        .isEqualTo(linkedUsername);
    assertThat(userDao.findUserByUsername(PRIMARY_EMAIL).getEraCommonsLinkExpireTime()).isNotNull();
    assertThat(userDao.findUserByUsername(PRIMARY_EMAIL).getEraCommonsCompletionTime()).isNotNull();
  }

  @Test
  public void testBypassAccessModule() throws Exception {
    Profile profile = createUser();
    profileController.bypassAccessRequirement(
        profile.getUserId(),
        new AccessBypassRequest().isBypassed(true).moduleName(AccessModule.DATA_USE_AGREEMENT));

    DbUser dbUser = userDao.findUserByUsername(PRIMARY_EMAIL);
    assertThat(dbUser.getDataUseAgreementBypassTime()).isNotNull();
  }

  @Test
  public void testDeleteProfile() throws Exception {
    createUser();

    profileController.deleteProfile();
    verify(mockProfileAuditor).fireDeleteAction(dbUser.getUserId(), dbUser.getUsername());
  }

  private Profile createUser() throws Exception {
    when(cloudStorageService.readInvitationKey()).thenReturn(INVITATION_KEY);
    when(directoryService.createUser(GIVEN_NAME, FAMILY_NAME, USERNAME, CONTACT_EMAIL))
        .thenReturn(googleUser);
    Profile result = profileController.createAccount(createAccountRequest).getBody();
    dbUser = userDao.findUserByUsername(PRIMARY_EMAIL);
    dbUser.setEmailVerificationStatusEnum(EmailVerificationStatus.SUBSCRIBED);
    userDao.save(dbUser);
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
    DbUser user = userDao.findUserByUsername(primaryEmail);
    assertThat(user).isNotNull();
    assertThat(user.getContactEmail()).isEqualTo(contactEmail);
    assertThat(user.getFamilyName()).isEqualTo(familyName);
    assertThat(user.getGivenName()).isEqualTo(givenName);
    assertThat(user.getDataAccessLevelEnum()).isEqualTo(dataAccessLevel);
    assertThat(user.getFirstSignInTime()).isEqualTo(firstSignInTime);
    assertThat(user.getDataAccessLevelEnum()).isEqualTo(dataAccessLevel);
  }
}
