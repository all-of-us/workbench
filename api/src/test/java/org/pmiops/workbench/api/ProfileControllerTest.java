package org.pmiops.workbench.api;


import static com.google.common.truth.Truth.assertThat;
import static junit.framework.TestCase.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.blockscore.models.Address;
import com.blockscore.models.Person;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import javax.inject.Provider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.pmiops.workbench.auth.ProfileService;
import org.pmiops.workbench.auth.UserAuthentication;
import org.pmiops.workbench.auth.UserAuthentication.UserType;
import org.pmiops.workbench.blockscore.BlockscoreService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.config.WorkbenchConfig.FireCloudConfig;
import org.pmiops.workbench.config.WorkbenchEnvironment;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.firecloud.ApiException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.BillingProjectMembership.CreationStatusEnum;
import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.google.DirectoryService;
import org.pmiops.workbench.mailchimp.MailChimpService;
import org.pmiops.workbench.model.BillingProjectMembership;
import org.pmiops.workbench.model.BillingProjectStatus;
import org.pmiops.workbench.model.BlockscoreIdVerificationStatus;
import org.pmiops.workbench.model.CreateAccountRequest;
import org.pmiops.workbench.model.DataAccessLevel;
import org.pmiops.workbench.model.EmailVerificationStatus;
import org.pmiops.workbench.model.IdVerificationRequest;
import org.pmiops.workbench.model.InvitationVerificationRequest;
import org.pmiops.workbench.model.Profile;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.test.Providers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
public class ProfileControllerTest {

  private static final Instant NOW = Instant.now();
  private static final Timestamp TIMESTAMP = new Timestamp(NOW.toEpochMilli());
  private static final String USERNAME = "bob";
  private static final String GIVEN_NAME = "Bob";
  private static final String FAMILY_NAME = "Bobberson";
  private static final String CONTACT_EMAIL = "bob@example.com";
  private static final String INVITATION_KEY = "secretpassword";
  private static final String PASSWORD = "12345";
  private static final String PRIMARY_EMAIL = "bob@researchallofus.org";
  private static final String BILLING_PROJECT_PREFIX = "all-of-us-free-";
  private static final String BILLING_PROJECT_NAME =
      BILLING_PROJECT_PREFIX + PRIMARY_EMAIL.hashCode();

  @Mock
  private Provider<User> userProvider;
  @Mock
  private Provider<UserAuthentication> userAuthenticationProvider;
  @Autowired
  private UserDao userDao;
  @Mock
  private FireCloudService fireCloudService;
  @Mock
  private MailChimpService mailChimpService;
  @Mock
  private DirectoryService directoryService;
  @Mock
  private CloudStorageService cloudStorageService;
  @Mock
  private BlockscoreService blockscoreService;
  @Mock
  private Person person;
  @Mock
  private Provider<WorkbenchConfig> configProvider;

  private ProfileController profileController;
  private ProfileController cloudProfileController;
  private CreateAccountRequest createAccountRequest;
  private InvitationVerificationRequest invitationVerificationRequest;
  private com.google.api.services.admin.directory.model.User googleUser;
  private FakeClock clock;
  private IdVerificationRequest idVerificationRequest;
  private User user;

  @Before
  public void setUp() {
    WorkbenchConfig config = new WorkbenchConfig();
    config.firecloud = new FireCloudConfig();
    config.firecloud.billingProjectPrefix = BILLING_PROJECT_PREFIX;

    WorkbenchEnvironment environment = new WorkbenchEnvironment(true, "appId");
    WorkbenchEnvironment cloudEnvironment = new WorkbenchEnvironment(false, "appId");
    createAccountRequest = new CreateAccountRequest();
    invitationVerificationRequest = new InvitationVerificationRequest();
    Profile profile = new Profile();
    profile.setContactEmail(CONTACT_EMAIL);
    profile.setFamilyName(FAMILY_NAME);
    profile.setGivenName(GIVEN_NAME);
    profile.setUsername(USERNAME);
    createAccountRequest.setProfile(profile);
    createAccountRequest.setInvitationKey(INVITATION_KEY);
    createAccountRequest.setPassword(PASSWORD);
    invitationVerificationRequest.setInvitationKey(INVITATION_KEY);
    googleUser = new com.google.api.services.admin.directory.model.User();
    googleUser.setPrimaryEmail(PRIMARY_EMAIL);

    clock = new FakeClock(NOW);

    idVerificationRequest = new IdVerificationRequest();
    idVerificationRequest.setFirstName("Bob");
    UserService userService = new UserService(userProvider, userDao, clock, fireCloudService, configProvider);
    ProfileService profileService = new ProfileService(fireCloudService, mailChimpService, userDao);
    this.profileController = new ProfileController(profileService, userProvider, userAuthenticationProvider,
        userDao, clock, userService, fireCloudService, directoryService,
        cloudStorageService, blockscoreService, mailChimpService, Providers.of(config), environment);
    this.cloudProfileController = new ProfileController(profileService, userProvider, userAuthenticationProvider,
        userDao, clock, userService, fireCloudService, directoryService,
        cloudStorageService, blockscoreService, mailChimpService, Providers.of(config), cloudEnvironment);
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
    assertThat(user.getDataAccessLevel()).isEqualTo(DataAccessLevel.UNREGISTERED);
  }

  @Test
  public void testSubmitIdVerification_success() throws Exception {
    createUser();
    when(blockscoreService.createPerson(eq("Bob"), eq(null), any(Address.class),
        eq(null), eq(null), eq(null))).thenReturn(person);
    when(person.getId()).thenReturn("id");
    when(person.isValid()).thenReturn(true);
    Profile profile = profileController.submitIdVerification(idVerificationRequest).getBody();
    assertThat(profile.getDataAccessLevel()).isEqualTo(DataAccessLevel.UNREGISTERED);
    assertThat(profile.getBlockscoreIdVerificationStatus()).isEqualTo(BlockscoreIdVerificationStatus.VERIFIED);
    assertThat(profile.getDemographicSurveyCompletionTime()).isNull();
    assertThat(profile.getTermsOfServiceCompletionTime()).isNull();
    assertThat(profile.getEthicsTrainingCompletionTime()).isNull();
  }

  @Test
  public void testSubmitDemographicSurvey_success() throws Exception {
    createUser();
    Profile profile = profileController.submitDemographicsSurvey().getBody();
    assertThat(profile.getDataAccessLevel()).isEqualTo(DataAccessLevel.UNREGISTERED);
    assertThat(profile.getBlockscoreIdVerificationStatus()).isEqualTo(BlockscoreIdVerificationStatus.UNVERIFIED);
    assertThat(profile.getDemographicSurveyCompletionTime()).isEqualTo(NOW.toEpochMilli());
    assertThat(profile.getTermsOfServiceCompletionTime()).isNull();
    assertThat(profile.getEthicsTrainingCompletionTime()).isNull();
  }

  @Test
  public void testSubmitTermsOfService_success() throws Exception {
    createUser();
    Profile profile = profileController.submitTermsOfService().getBody();
    assertThat(profile.getDataAccessLevel()).isEqualTo(DataAccessLevel.UNREGISTERED);
    assertThat(profile.getBlockscoreIdVerificationStatus()).isEqualTo(BlockscoreIdVerificationStatus.UNVERIFIED);
    assertThat(profile.getDemographicSurveyCompletionTime()).isNull();
    assertThat(profile.getTermsOfServiceCompletionTime()).isEqualTo(NOW.toEpochMilli());
    assertThat(profile.getEthicsTrainingCompletionTime()).isNull();
  }

  @Test
  public void testSubmitEthicsTraining_success() throws Exception {
    createUser();
    Profile profile = profileController.completeEthicsTraining().getBody();
    assertThat(profile.getDataAccessLevel()).isEqualTo(DataAccessLevel.UNREGISTERED);
    assertThat(profile.getBlockscoreIdVerificationStatus()).isEqualTo(BlockscoreIdVerificationStatus.UNVERIFIED);
    assertThat(profile.getDemographicSurveyCompletionTime()).isNull();
    assertThat(profile.getTermsOfServiceCompletionTime()).isNull();
    assertThat(profile.getEthicsTrainingCompletionTime()).isEqualTo(NOW.toEpochMilli());
  }

  @Test
  public void testSubmitEverything_success() throws Exception {
    createUser();
    when(blockscoreService.createPerson(eq("Bob"), eq(null), any(Address.class),
        eq(null), eq(null), eq(null))).thenReturn(person);
    when(mailChimpService.getMember(CONTACT_EMAIL)).thenReturn("subscribed");
    when(person.getId()).thenReturn("id");
    when(person.isValid()).thenReturn(true);
    WorkbenchConfig testConfig = new WorkbenchConfig();
    testConfig.firecloud = new FireCloudConfig();
    testConfig.firecloud.registeredDomainName = "";

    when(configProvider.get()).thenReturn(testConfig);
    Profile profile = profileController.completeEthicsTraining().getBody();
    assertThat(profile.getDataAccessLevel()).isEqualTo(DataAccessLevel.UNREGISTERED);
    profile = profileController.submitDemographicsSurvey().getBody();
    assertThat(profile.getDataAccessLevel()).isEqualTo(DataAccessLevel.UNREGISTERED);
    profile = profileController.submitTermsOfService().getBody();
    assertThat(profile.getDataAccessLevel()).isEqualTo(DataAccessLevel.UNREGISTERED);
    profile = profileController.submitIdVerification(idVerificationRequest).getBody();
    assertThat(profile.getDataAccessLevel()).isEqualTo(DataAccessLevel.REGISTERED);
    verify(fireCloudService).addUserToGroup("bob@researchallofus.org", "");

    assertThat(profile.getBlockscoreIdVerificationStatus()).isEqualTo(BlockscoreIdVerificationStatus.VERIFIED);
    assertThat(profile.getDemographicSurveyCompletionTime()).isEqualTo(NOW.toEpochMilli());
    assertThat(profile.getTermsOfServiceCompletionTime()).isEqualTo(NOW.toEpochMilli());
    assertThat(profile.getEthicsTrainingCompletionTime()).isEqualTo(NOW.toEpochMilli());
  }


  @Test(expected = ServerErrorException.class)
  public void testCreateAccount_directoryServiceFail() throws Exception {
    when(cloudStorageService.readInvitationKey()).thenReturn(INVITATION_KEY);

    when(directoryService.createUser(GIVEN_NAME, FAMILY_NAME, USERNAME, PASSWORD))
        .thenThrow(new IOException());
    profileController.createAccount(createAccountRequest);
  }

  @Test
  public void testMe_success() throws Exception {
    createUser();
    when(fireCloudService.isRequesterEnabledInFirecloud()).thenReturn(true);

    Profile profile = profileController.getMe().getBody();
    assertProfile(profile, PRIMARY_EMAIL, CONTACT_EMAIL, FAMILY_NAME, GIVEN_NAME,
        DataAccessLevel.UNREGISTERED, TIMESTAMP, BILLING_PROJECT_NAME, true);
    verify(fireCloudService).registerUser(CONTACT_EMAIL, GIVEN_NAME, FAMILY_NAME);

    verify(fireCloudService).createAllOfUsBillingProject(BILLING_PROJECT_NAME);
    verify(fireCloudService).addUserToBillingProject(PRIMARY_EMAIL, BILLING_PROJECT_NAME);
  }

  @Test
  public void testMe_secondCallInitializesProject() throws Exception {
    createUser();
    when(fireCloudService.isRequesterEnabledInFirecloud()).thenReturn(true);

    Profile profile = profileController.getMe().getBody();
    assertThat(profile.getFreeTierBillingProjectStatus()).isEqualTo(BillingProjectStatus.PENDING);

    // Simulate FC "Ready".
    org.pmiops.workbench.firecloud.model.BillingProjectMembership membership =
        new org.pmiops.workbench.firecloud.model.BillingProjectMembership();
    membership.setCreationStatus(CreationStatusEnum.READY);
    membership.setProjectName(BILLING_PROJECT_NAME);
    when(fireCloudService.getBillingProjectMemberships()).thenReturn(ImmutableList.of(membership));
    profile = profileController.getMe().getBody();
    assertThat(profile.getFreeTierBillingProjectStatus()).isEqualTo(BillingProjectStatus.READY);

    verify(fireCloudService).grantGoogleRoleToUser(
        BILLING_PROJECT_NAME, FireCloudService.BIGQUERY_JOB_USER_GOOGLE_ROLE, PRIMARY_EMAIL);
  }

  @Test
  public void testMe_secondCallErrorsProject() throws Exception {
    createUser();
    when(fireCloudService.isRequesterEnabledInFirecloud()).thenReturn(true);

    Profile profile = profileController.getMe().getBody();
    assertThat(profile.getFreeTierBillingProjectStatus()).isEqualTo(BillingProjectStatus.PENDING);

    // Simulate FC "Error".
    org.pmiops.workbench.firecloud.model.BillingProjectMembership membership =
        new org.pmiops.workbench.firecloud.model.BillingProjectMembership();
    membership.setCreationStatus(CreationStatusEnum.ERROR);
    membership.setProjectName(BILLING_PROJECT_NAME);
    when(fireCloudService.getBillingProjectMemberships()).thenReturn(ImmutableList.of(membership));
    profile = profileController.getMe().getBody();
    assertThat(profile.getFreeTierBillingProjectStatus()).isEqualTo(BillingProjectStatus.ERROR);

    verify(fireCloudService, never()).grantGoogleRoleToUser(any(), any(), any());
  }

  @Test
  public void testMe_secondCallStillPendingProject() throws Exception {
    createUser();
    when(fireCloudService.isRequesterEnabledInFirecloud()).thenReturn(true);

    Profile profile = profileController.getMe().getBody();
    assertThat(profile.getFreeTierBillingProjectStatus()).isEqualTo(BillingProjectStatus.PENDING);

    // Simulate FC "Creating".
    org.pmiops.workbench.firecloud.model.BillingProjectMembership membership =
        new org.pmiops.workbench.firecloud.model.BillingProjectMembership();
    membership.setCreationStatus(CreationStatusEnum.CREATING);
    membership.setProjectName(BILLING_PROJECT_NAME);
    when(fireCloudService.getBillingProjectMemberships()).thenReturn(ImmutableList.of(membership));
    profile = profileController.getMe().getBody();
    assertThat(profile.getFreeTierBillingProjectStatus()).isEqualTo(BillingProjectStatus.PENDING);

    verify(fireCloudService, never()).grantGoogleRoleToUser(any(), any(), any());
  }

  @Test
  public void testMe_secondCallProjectNotReturned() throws Exception {
    createUser();
    when(fireCloudService.isRequesterEnabledInFirecloud()).thenReturn(true);

    Profile profile = profileController.getMe().getBody();
    assertThat(profile.getFreeTierBillingProjectStatus()).isEqualTo(BillingProjectStatus.PENDING);

    org.pmiops.workbench.firecloud.model.BillingProjectMembership membership =
        new org.pmiops.workbench.firecloud.model.BillingProjectMembership();
    membership.setCreationStatus(CreationStatusEnum.READY);
    membership.setProjectName("unrelated-project");
    when(fireCloudService.getBillingProjectMemberships()).thenReturn(ImmutableList.of(membership));
    profile = profileController.getMe().getBody();
    assertThat(profile.getFreeTierBillingProjectStatus()).isEqualTo(BillingProjectStatus.PENDING);

    verify(fireCloudService, never()).grantGoogleRoleToUser(any(), any(), any());
  }

  @Test
  public void testMe_successDevProjectConflict() throws Exception {
    createUser();
    when(fireCloudService.isRequesterEnabledInFirecloud()).thenReturn(true);

    Profile profile = profileController.getMe().getBody();

    String projectName = BILLING_PROJECT_PREFIX + PRIMARY_EMAIL.hashCode();
    doThrow(new ApiException(HttpStatus.CONFLICT.value(), "conflict"))
        .when(fireCloudService).createAllOfUsBillingProject(projectName);

    // When a conflict occurs in dev, log the exception but continue.
    assertProfile(profile, PRIMARY_EMAIL, CONTACT_EMAIL, FAMILY_NAME, GIVEN_NAME,
        DataAccessLevel.UNREGISTERED, TIMESTAMP, projectName, true);
    verify(fireCloudService).registerUser(CONTACT_EMAIL, GIVEN_NAME, FAMILY_NAME);
    verify(fireCloudService).createAllOfUsBillingProject(projectName);
    verify(fireCloudService).addUserToBillingProject(PRIMARY_EMAIL, projectName);
  }

  @Test
  public void testMe_userBeforeSuccessCloudProjectConflict() throws Exception {
    createUser();
    when(fireCloudService.isRequesterEnabledInFirecloud()).thenReturn(true);

    String projectName = BILLING_PROJECT_PREFIX + user.getUserId();
    doThrow(new ApiException(HttpStatus.CONFLICT.value(), "conflict"))
        .when(fireCloudService).createAllOfUsBillingProject(projectName);
    doThrow(new ApiException(HttpStatus.CONFLICT.value(), "conflict"))
        .when(fireCloudService).createAllOfUsBillingProject(projectName + "-1");

    Profile profile = cloudProfileController.getMe().getBody();

    // When a conflict occurs in dev, log the exception but continue.
    assertProfile(profile, PRIMARY_EMAIL, CONTACT_EMAIL, FAMILY_NAME, GIVEN_NAME,
        DataAccessLevel.UNREGISTERED, TIMESTAMP, projectName + "-2",
        true);
    verify(fireCloudService).registerUser(CONTACT_EMAIL, GIVEN_NAME, FAMILY_NAME);
    verify(fireCloudService).createAllOfUsBillingProject(projectName);
    verify(fireCloudService).createAllOfUsBillingProject(projectName + "-1");
    verify(fireCloudService).createAllOfUsBillingProject(projectName + "-2");
    verify(fireCloudService).addUserToBillingProject(PRIMARY_EMAIL, projectName + "-2");
  }

  @Test
  public void testMe_userBeforeSuccessCloudProjectTooManyConflicts() throws Exception {
    createUser();
    when(fireCloudService.isRequesterEnabledInFirecloud()).thenReturn(true);

    String projectName = BILLING_PROJECT_PREFIX + user.getUserId();
    doThrow(new ApiException(HttpStatus.CONFLICT.value(), "conflict"))
        .when(fireCloudService).createAllOfUsBillingProject(projectName);
    doThrow(new ApiException(HttpStatus.CONFLICT.value(), "conflict"))
        .when(fireCloudService).createAllOfUsBillingProject(projectName + "-1");
    doThrow(new ApiException(HttpStatus.CONFLICT.value(), "conflict"))
        .when(fireCloudService).createAllOfUsBillingProject(projectName + "-2");
    doThrow(new ApiException(HttpStatus.CONFLICT.value(), "conflict"))
        .when(fireCloudService).createAllOfUsBillingProject(projectName + "-3");
    doThrow(new ApiException(HttpStatus.CONFLICT.value(), "conflict"))
        .when(fireCloudService).createAllOfUsBillingProject(projectName + "-4");

    try {
      cloudProfileController.getMe();
      fail("ServerErrorException expected");
    } catch (ServerErrorException e) {
      // expected
    }

    // When too many conflicts occur, the user doesn't have their project name set or first
    // sign in time.
    assertUser(PRIMARY_EMAIL, CONTACT_EMAIL, FAMILY_NAME, GIVEN_NAME,
        DataAccessLevel.UNREGISTERED, null, null);
    verify(fireCloudService).registerUser(CONTACT_EMAIL, GIVEN_NAME, FAMILY_NAME);
    verify(fireCloudService).createAllOfUsBillingProject(projectName);
    verify(fireCloudService).createAllOfUsBillingProject(projectName + "-1");
    verify(fireCloudService).createAllOfUsBillingProject(projectName + "-2");
    verify(fireCloudService).createAllOfUsBillingProject(projectName + "-3");
    verify(fireCloudService).createAllOfUsBillingProject(projectName + "-4");
  }

  @Test
  public void testMe_userBeforeNotLoggedInSuccess() throws Exception {
    createUser();
    when(fireCloudService.isRequesterEnabledInFirecloud()).thenReturn(true);
    Profile profile = profileController.getMe().getBody();
    String projectName = BILLING_PROJECT_PREFIX + PRIMARY_EMAIL.hashCode();
    assertProfile(profile, PRIMARY_EMAIL, CONTACT_EMAIL, FAMILY_NAME, GIVEN_NAME,
        DataAccessLevel.UNREGISTERED, TIMESTAMP, projectName, true);
    verify(fireCloudService).registerUser(CONTACT_EMAIL, GIVEN_NAME, FAMILY_NAME);

    verify(fireCloudService).createAllOfUsBillingProject(projectName);
    verify(fireCloudService).addUserToBillingProject(PRIMARY_EMAIL, projectName);

    // An additional call to getMe() should have no effect.
    clock.increment(1);
    profile = profileController.getMe().getBody();
    assertProfile(profile, PRIMARY_EMAIL, CONTACT_EMAIL, FAMILY_NAME, GIVEN_NAME,
        DataAccessLevel.UNREGISTERED, TIMESTAMP, projectName, true);
  }

  @Test
  public void testGetBillingProjects_empty() throws Exception {
    when(fireCloudService.getBillingProjectMemberships()).thenReturn(
        ImmutableList.<org.pmiops.workbench.firecloud.model.BillingProjectMembership>of());
    assertThat(profileController.getBillingProjects().getBody()).isEmpty();
  }

  @Test
  public void testGetBillingProjects_notEmpty() throws Exception {
    org.pmiops.workbench.firecloud.model.BillingProjectMembership membership =
        new org.pmiops.workbench.firecloud.model.BillingProjectMembership();
    membership.setProjectName("a");
    membership.setRole("c");
    membership.setCreationStatus(CreationStatusEnum.CREATING);
    when(fireCloudService.getBillingProjectMemberships()).thenReturn(
        ImmutableList.of(membership));
    List<BillingProjectMembership> memberships =
        profileController.getBillingProjects().getBody();
    assertThat(memberships.size()).isEqualTo(1);
    BillingProjectMembership result = memberships.get(0);
    assertThat(result.getProjectName()).isEqualTo("a");
    assertThat(result.getRole()).isEqualTo("c");
    assertThat(result.getStatus()).isEqualTo(BillingProjectStatus.PENDING);
  }

  private Profile createUser() throws Exception {
    when(cloudStorageService.readInvitationKey()).thenReturn(INVITATION_KEY);
    when(directoryService.createUser(GIVEN_NAME, FAMILY_NAME, USERNAME, PASSWORD))
        .thenReturn(googleUser);
    when(fireCloudService.isRequesterEnabledInFirecloud()).thenReturn(false);
    Profile result = profileController.createAccount(createAccountRequest).getBody();
    user = userDao.findUserByEmail(PRIMARY_EMAIL);
    user.setEmailVerificationStatus(EmailVerificationStatus.SUBSCRIBED);
    userDao.save(user);
    when(userProvider.get()).thenReturn(user);
    when(userAuthenticationProvider.get()).thenReturn(
        new UserAuthentication(user, null, null, UserType.RESEARCHER));
    return result;
  }

  private void assertProfile(Profile profile, String primaryEmail, String contactEmail,
      String familyName, String givenName, DataAccessLevel dataAccessLevel,
      Timestamp firstSignInTime, String freeTierBillingProject, boolean enabledInFirecloud) {
    assertThat(profile).isNotNull();
    assertThat(profile.getContactEmail()).isEqualTo(contactEmail);
    assertThat(profile.getFamilyName()).isEqualTo(familyName);
    assertThat(profile.getGivenName()).isEqualTo(givenName);
    assertThat(profile.getDataAccessLevel()).isEqualTo(dataAccessLevel);
    assertThat(profile.getFreeTierBillingProjectName()).isEqualTo(freeTierBillingProject);
    assertThat(profile.getEnabledInFireCloud()).isEqualTo(enabledInFirecloud);
    assertUser(primaryEmail, contactEmail, familyName, givenName, dataAccessLevel, firstSignInTime,
        freeTierBillingProject);
  }

  private void assertUser(String primaryEmail, String contactEmail,
      String familyName, String givenName, DataAccessLevel dataAccessLevel,
      Timestamp firstSignInTime, String freeTierBillingProject) {
    User user = userDao.findUserByEmail(primaryEmail);
    assertThat(user).isNotNull();
    assertThat(user.getContactEmail()).isEqualTo(contactEmail);
    assertThat(user.getFamilyName()).isEqualTo(familyName);
    assertThat(user.getGivenName()).isEqualTo(givenName);
    assertThat(user.getDataAccessLevel()).isEqualTo(dataAccessLevel);
    assertThat(user.getFirstSignInTime()).isEqualTo(firstSignInTime);
    assertThat(user.getFreeTierBillingProjectName()).isEqualTo(freeTierBillingProject);
    assertThat(user.getDataAccessLevel()).isEqualTo(dataAccessLevel);
  }

}
