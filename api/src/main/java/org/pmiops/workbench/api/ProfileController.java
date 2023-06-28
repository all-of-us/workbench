package org.pmiops.workbench.api;

import com.google.api.services.directory.model.User;
import com.google.common.base.Strings;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Provider;
import org.pmiops.workbench.access.AccessTierService;
import org.pmiops.workbench.actionaudit.Agent;
import org.pmiops.workbench.actionaudit.auditors.ProfileAuditor;
import org.pmiops.workbench.auth.UserAuthentication;
import org.pmiops.workbench.auth.UserAuthentication.UserType;
import org.pmiops.workbench.captcha.CaptchaVerificationService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.DbPageVisit;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.exceptions.UnauthorizedException;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.google.DirectoryService;
import org.pmiops.workbench.institution.InstitutionService;
import org.pmiops.workbench.institution.VerifiedInstitutionalAffiliationMapper;
import org.pmiops.workbench.mail.MailService;
import org.pmiops.workbench.model.CreateAccountRequest;
import org.pmiops.workbench.model.Institution;
import org.pmiops.workbench.model.NihToken;
import org.pmiops.workbench.model.PageVisit;
import org.pmiops.workbench.model.Profile;
import org.pmiops.workbench.model.RasLinkRequestBody;
import org.pmiops.workbench.model.ResendWelcomeEmailRequest;
import org.pmiops.workbench.model.SendBillingSetupEmailRequest;
import org.pmiops.workbench.model.UpdateContactEmailRequest;
import org.pmiops.workbench.model.UsernameTakenResponse;
import org.pmiops.workbench.model.VerifiedInstitutionalAffiliation;
import org.pmiops.workbench.moodle.ApiException;
import org.pmiops.workbench.profile.AddressMapper;
import org.pmiops.workbench.profile.DemographicSurveyMapper;
import org.pmiops.workbench.profile.PageVisitMapper;
import org.pmiops.workbench.profile.ProfileService;
import org.pmiops.workbench.ras.RasLinkService;
import org.pmiops.workbench.shibboleth.ShibbolethService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contains implementations for all Workbench API methods tagged with "profile".
 *
 * <p>The majority of handlers here are lightweight wrappers which delegate to UserService, where
 * many user-focused database and/or API calls are implemented.
 */
@RestController
public class ProfileController implements ProfileApiDelegate {
  private static final Logger log = Logger.getLogger(ProfileController.class.getName());

  private final AddressMapper addressMapper;
  private final CaptchaVerificationService captchaVerificationService;
  private final Clock clock;
  private final DemographicSurveyMapper demographicSurveyMapper;
  private final DirectoryService directoryService;
  private final FireCloudService fireCloudService;
  private final InstitutionService institutionService;
  private final PageVisitMapper pageVisitMapper;
  private final ProfileAuditor profileAuditor;
  private final ProfileService profileService;
  private final Provider<DbUser> userProvider;
  private final Provider<MailService> mailServiceProvider;
  private final Provider<UserAuthentication> userAuthenticationProvider;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final ShibbolethService shibbolethService;
  private final UserDao userDao;
  private final UserService userService;
  private final VerifiedInstitutionalAffiliationMapper verifiedInstitutionalAffiliationMapper;
  private final RasLinkService rasLinkService;

  @Autowired
  ProfileController(
      AddressMapper addressMapper,
      CaptchaVerificationService captchaVerificationService,
      Clock clock,
      DemographicSurveyMapper demographicSurveyMapper,
      DirectoryService directoryService,
      FireCloudService fireCloudService,
      InstitutionService institutionService,
      PageVisitMapper pageVisitMapper,
      ProfileAuditor profileAuditor,
      ProfileService profileService,
      Provider<DbUser> userProvider,
      Provider<MailService> mailServiceProvider,
      Provider<UserAuthentication> userAuthenticationProvider,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      ShibbolethService shibbolethService,
      UserDao userDao,
      UserService userService,
      VerifiedInstitutionalAffiliationMapper verifiedInstitutionalAffiliationMapper,
      RasLinkService rasLinkService) {
    this.addressMapper = addressMapper;
    this.captchaVerificationService = captchaVerificationService;
    this.clock = clock;
    this.demographicSurveyMapper = demographicSurveyMapper;
    this.directoryService = directoryService;
    this.fireCloudService = fireCloudService;
    this.institutionService = institutionService;
    this.mailServiceProvider = mailServiceProvider;
    this.pageVisitMapper = pageVisitMapper;
    this.profileAuditor = profileAuditor;
    this.profileService = profileService;
    this.shibbolethService = shibbolethService;
    this.userAuthenticationProvider = userAuthenticationProvider;
    this.userDao = userDao;
    this.userProvider = userProvider;
    this.userService = userService;
    this.verifiedInstitutionalAffiliationMapper = verifiedInstitutionalAffiliationMapper;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.rasLinkService = rasLinkService;
  }

  private DbUser saveUserWithConflictHandling(DbUser dbUser) {
    try {
      return userDao.save(dbUser);
    } catch (ObjectOptimisticLockingFailureException e) {
      log.log(Level.WARNING, "version conflict for user update", e);
      throw new ConflictException("Failed due to concurrent modification");
    }
  }

  private DbUser maybeInitializeUserWithTerra() {
    UserAuthentication userAuthentication = userAuthenticationProvider.get();
    DbUser dbUser = userAuthentication.getUser();
    if (userAuthentication.getUserType() == UserType.SERVICE_ACCOUNT) {
      // Service accounts don't need further initialization.
      return dbUser;
    }

    // On first sign-in register with Terra, submit the Terra ToS (if user accepted latest), and set
    // the first sign in time. All actions taken here must be idempotent to avoid improper user
    // initialization.
    if (dbUser.getFirstSignInTime() == null) {
      // This call should be idempotent. If the user is already registered, their profile will get
      // updated.
      fireCloudService.registerUser(dbUser.getGivenName(), dbUser.getFamilyName());

      // By approving the latest AOU Terms of Service, the user also approves the latest Terra TOS
      // In case user has not accepted latest AoU version, getUserTermsOfServiceStatus and acceptTos
      // will take care of scenario by re-directing them to AoU terms of service page. This call
      // should be idempotent.
      if (userService.validateAllOfUsTermsOfServiceVersion(dbUser)) {
        userService.acceptTerraTermsOfService(dbUser);
      }
      dbUser.setFirstSignInTime(new Timestamp(clock.instant().toEpochMilli()));
      return saveUserWithConflictHandling(dbUser);
    }

    return dbUser;
  }

  private ResponseEntity<Profile> getProfileResponse(DbUser user) {
    Profile profile = profileService.getProfile(user);
    // Note: The following requires that the current request is authenticated.
    return ResponseEntity.ok(profile);
  }

  @Override
  public ResponseEntity<Profile> getMe() {
    // Record that the user signed in and run Terra initialization as needed.
    DbUser dbUser = maybeInitializeUserWithTerra();
    profileAuditor.fireLoginAction(dbUser);
    return getProfileResponse(dbUser);
  }

  @Override
  public ResponseEntity<Boolean> getUserTermsOfServiceStatus() {
    DbUser loggedInUser = userAuthenticationProvider.get().getUser();
    return ResponseEntity.ok(userService.validateTermsOfService(loggedInUser));
  }

  @Override
  public ResponseEntity<Void> acceptTermsOfService(Integer termsOfServiceVersion) {
    DbUser loggedInUser = userAuthenticationProvider.get().getUser();
    userService.submitAouTermsOfService(loggedInUser, termsOfServiceVersion);
    userService.acceptTerraTermsOfService(loggedInUser);
    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<UsernameTakenResponse> isUsernameTaken(String username) {
    return ResponseEntity.ok(
        new UsernameTakenResponse().isTaken(directoryService.isUsernameTaken(username)));
  }

  @Override
  public ResponseEntity<Profile> createAccount(CreateAccountRequest request) {
    if (workbenchConfigProvider.get().captcha.enableCaptcha) {
      verifyCaptcha(request.getCaptchaVerificationToken());
    }

    userService.validateAllOfUsTermsOfService(request.getTermsOfServiceVersion());

    profileService.validateAffiliation(request.getProfile());

    final Profile profile = request.getProfile();

    profileService.cleanProfile(profile);
    profileService.validateNewProfile(profile);

    String gSuiteUsername =
        profile.getUsername()
            + "@"
            + workbenchConfigProvider.get().googleDirectoryService.gSuiteDomain;

    User googleUser =
        directoryService.createUser(
            profile.getGivenName(),
            profile.getFamilyName(),
            gSuiteUsername,
            profile.getContactEmail());

    DbUser user;
    try {
      user =
          userService.createUser(
              profile.getGivenName(),
              profile.getFamilyName(),
              gSuiteUsername,
              profile.getContactEmail(),
              profile.getAreaOfResearch(),
              profile.getProfessionalUrl(),
              profile.getDegrees(),
              addressMapper.addressToDbAddress(profile.getAddress()),
              demographicSurveyMapper.demographicSurveyToDbDemographicSurvey(
                  profile.getDemographicSurvey()),
              demographicSurveyMapper.toDbDemographicSurveyV2(profile.getDemographicSurveyV2()),
              verifiedInstitutionalAffiliationMapper.modelToDbWithoutUser(
                  profile.getVerifiedInstitutionalAffiliation(), institutionService));
    } catch (Exception e) {
      // If the creation of a User row in the RW database fails, we want to attempt to remove the
      // G Suite account to avoid having an orphaned account with no record in our database.
      log.severe(
          String.format(
              "An error occurred when creating DbUser for %s. Attempting to delete "
                  + "orphaned G Suite account",
              gSuiteUsername));
      try {
        directoryService.deleteUser(gSuiteUsername);
        log.severe("Orphaned G Suite account has been deleted.");
      } catch (Exception e2) {
        log.severe(
            String.format(
                "Orphaned G Suite account %s could not be deleted. "
                    + "Manual intervention may be required",
                gSuiteUsername));
        log.log(Level.SEVERE, e2.getMessage(), e2);
        // Throw the original error rather than the G Suite error.
        throw e;
      }
      throw e;
    }

    // we can't call submitTerraTermsOfService() yet because the Terra account doesn't exist yet.
    // see maybeInitializeUserWithTerra()
    userService.submitAouTermsOfService(user, request.getTermsOfServiceVersion());
    String institutionShortName =
        profile.getVerifiedInstitutionalAffiliation().getInstitutionShortName();
    try {
      Institution userInstitution =
          institutionService
              .getInstitution(institutionShortName)
              .orElseThrow(() -> new BadRequestException("User Institution cannot be found"));
      sendWelcomeEmail(user, googleUser, userInstitution);
    } catch (BadRequestException ex) {
      log.log(
          Level.SEVERE,
          "Exception while resending sending welcome email: " + ex.getLocalizedMessage());
      throw ex;
    }
    final MailService mail = mailServiceProvider.get();
    institutionService
        .getInstitutionUserInstructions(institutionShortName)
        .ifPresent(
            instructions -> {
              try {
                mail.sendInstitutionUserInstructions(
                    profile.getContactEmail(), instructions, gSuiteUsername);
              } catch (MessagingException e) {
                throw new WorkbenchException(e);
              }
            });

    // Note: Avoid getProfileResponse() here as this is not an authenticated request.
    final Profile createdProfile = profileService.getProfile(user);
    profileAuditor.fireCreateAction(createdProfile);
    return ResponseEntity.ok(createdProfile);
  }

  private void sendWelcomeEmail(DbUser user, User googleUser, Institution userInstitution) {

    final MailService mail = mailServiceProvider.get();

    try {
      boolean showEraStepInRT =
          eraRequiredForTier(userInstitution, AccessTierService.REGISTERED_TIER_SHORT_NAME);

      boolean showEraStepInCT =
          !showEraStepInRT
              && eraRequiredForTier(userInstitution, AccessTierService.CONTROLLED_TIER_SHORT_NAME);

      mail.sendWelcomeEmail(
          user.getContactEmail(),
          googleUser.getPassword(),
          user.getUsername(),
          userInstitution.getDisplayName(),
          showEraStepInRT,
          showEraStepInCT);

    } catch (MessagingException e) {
      throw new WorkbenchException(e);
    }
  }

  private boolean eraRequiredForTier(Institution institution, String tierShortName) {
    return institutionService.eRaRequiredForTier(institution, tierShortName);
  }

  @Override
  public ResponseEntity<Profile> submitDUCC(Integer duccSignedVersion, String initials) {
    DbUser user = userService.submitDUCC(userProvider.get(), duccSignedVersion, initials);
    return getProfileResponse(saveUserWithConflictHandling(user));
  }

  /**
   * This methods updates logged in user's training status from Moodle.
   *
   * @return Profile updated with training completion time
   */
  @Override
  public ResponseEntity<Profile> syncComplianceTrainingStatus() {
    try {
      userService.syncComplianceTrainingStatusV2();
    } catch (NotFoundException ex) {
      throw ex;
    } catch (ApiException e) {
      throw new ServerErrorException(e);
    }
    return getProfileResponse(userProvider.get());
  }

  @Override
  public ResponseEntity<Profile> syncEraCommonsStatus() {
    userService.syncEraCommonsStatus();
    return getProfileResponse(userProvider.get());
  }

  @Override
  public ResponseEntity<Profile> syncTwoFactorAuthStatus() {
    userService.syncTwoFactorAuthStatus();
    return getProfileResponse(userProvider.get());
  }

  private void verifyCaptcha(String captchaToken) {
    boolean isValidCaptcha = false;
    try {
      isValidCaptcha = captchaVerificationService.verifyCaptcha(captchaToken);
      if (!isValidCaptcha) {
        throw new BadRequestException("Missing or incorrect Captcha Token");
      }
    } catch (org.pmiops.workbench.captcha.ApiException e) {
      throw new ServerErrorException("Exception while verifying Captcha");
    }
  }

  private void checkUserCreationNonce(DbUser user, String nonce) {
    if (Strings.isNullOrEmpty(nonce)) {
      throw new BadRequestException("missing required creationNonce");
    }
    if (user.getCreationNonce() == null || !nonce.equals(user.getCreationNonce().toString())) {
      throw new UnauthorizedException("invalid creationNonce provided");
    }
  }

  /*
   * This un-authed API method is limited such that we only allow contact email updates before the user has signed in
   * with the newly created gsuite account. Once the user has logged in, they can change their contact email through
   * the normal profile update process.
   */
  @Override
  public ResponseEntity<Void> updateContactEmail(
      UpdateContactEmailRequest updateContactEmailRequest) {
    String username = updateContactEmailRequest.getUsername().toLowerCase();
    User googleUser = directoryService.getUserOrThrow(username);
    DbUser user = userService.getByUsernameOrThrow(username);
    checkUserCreationNonce(user, updateContactEmailRequest.getCreationNonce());
    if (userHasEverLoggedIn(googleUser, user)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
    String newEmail = updateContactEmailRequest.getContactEmail();
    try {
      new InternetAddress(newEmail).validate();
    } catch (AddressException e) {
      log.log(Level.INFO, "Invalid email entered.");
      return ResponseEntity.badRequest().build();
    }
    user.setContactEmail(newEmail);
    return resetPasswordAndSendWelcomeEmail(username, user);
  }

  @Override
  public ResponseEntity<Void> resendWelcomeEmail(ResendWelcomeEmailRequest resendRequest) {
    String username = resendRequest.getUsername().toLowerCase();
    User googleUser = directoryService.getUserOrThrow(username);
    DbUser user = userService.getByUsernameOrThrow(username);
    checkUserCreationNonce(user, resendRequest.getCreationNonce());
    if (userHasEverLoggedIn(googleUser, user)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
    return resetPasswordAndSendWelcomeEmail(username, user);
  }

  @Override
  public ResponseEntity<Void> sendBillingSetupEmail(SendBillingSetupEmailRequest emailRequest) {
    try {
      mailServiceProvider.get().sendBillingSetupEmail(userProvider.get(), emailRequest);
    } catch (MessagingException e) {
      throw new ServerErrorException("Failed to send billing setup email", e);
    }
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  private boolean userHasEverLoggedIn(User googleUser, DbUser user) {
    return user.getFirstSignInTime() != null || !googleUser.getChangePasswordAtNextLogin();
  }

  private ResponseEntity<Void> resetPasswordAndSendWelcomeEmail(String username, DbUser user) {
    User googleUser = directoryService.resetUserPassword(username);
    try {
      Institution userInstitution =
          institutionService
              .getByUser(user)
              .orElseThrow(() -> new BadRequestException("User Institution cannot be found"));
      sendWelcomeEmail(user, googleUser, userInstitution);
    } catch (BadRequestException ex) {
      log.log(
          Level.SEVERE,
          "Exception while resending sending welcome email: " + ex.getLocalizedMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    } catch (WorkbenchException e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  @Override
  public ResponseEntity<Profile> updatePageVisits(PageVisit newPageVisit) {
    DbUser dbUser = userProvider.get();
    dbUser = userDao.findUserWithAuthoritiesAndPageVisits(dbUser.getUserId());
    Timestamp timestamp = Timestamp.from(clock.instant());
    final boolean shouldAdd =
        dbUser.getPageVisits().stream()
            .noneMatch(v -> v.getPageId().equals(newPageVisit.getPage()));
    if (shouldAdd) {
      final DbPageVisit dbPageVisit = pageVisitMapper.pageVisitToDbPageVisit(newPageVisit);
      dbPageVisit.setUser(dbUser);
      dbPageVisit.setFirstVisit(timestamp);
      dbUser.getPageVisits().add(dbPageVisit);
      dbUser = userDao.save(dbUser);
    }
    return getProfileResponse(saveUserWithConflictHandling(dbUser));
  }

  @Override
  public ResponseEntity<Void> updateProfile(Profile updatedProfile) {
    DbUser user = userProvider.get();

    // Save current profile for audit trail. Continue to use the userProvider (instead
    // of info on previousProfile) to ensure addition of audit system doesn't change behavior.
    // That is, in the (rare, hopefully) condition that the old profile gives incorrect information,
    // the update will still work as well as it would have.
    final Profile previousProfile = profileService.getProfile(user);
    final VerifiedInstitutionalAffiliation updatedAffil =
        updatedProfile.getVerifiedInstitutionalAffiliation();
    final VerifiedInstitutionalAffiliation prevAffil =
        previousProfile.getVerifiedInstitutionalAffiliation();
    if (!Objects.equals(updatedAffil, prevAffil)) {
      throw new BadRequestException("Cannot update Verified Institutional Affiliation");
    }

    final DbUser updatedUser =
        profileService.updateProfile(user, Agent.asUser(user), updatedProfile, previousProfile);
    userService.confirmProfile(updatedUser);

    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  @Override
  public ResponseEntity<Profile> updateNihToken(NihToken token) {
    if (token == null || token.getJwt() == null) {
      throw new BadRequestException("Token is required.");
    }

    shibbolethService.updateShibbolethToken(token.getJwt());

    userService.syncEraCommonsStatus();
    return getProfileResponse(userProvider.get());
  }

  @Override
  public ResponseEntity<Void> deleteProfile() {
    if (!workbenchConfigProvider.get().featureFlags.unsafeAllowDeleteUser) {
      throw new ForbiddenException("Self account deletion is disallowed in this environment.");
    }
    DbUser user = userProvider.get();
    log.log(Level.WARNING, "Deleting profile: user email: " + user.getUsername());
    directoryService.deleteUser(user.getUsername());
    userDao.deleteById(user.getUserId());
    profileAuditor.fireDeleteAction(user.getUserId(), user.getUsername());

    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  @Override
  public ResponseEntity<Profile> linkRasAccount(RasLinkRequestBody body) {
    DbUser dbUser = rasLinkService.linkRasAccount(body.getAuthCode(), body.getRedirectUrl());
    return ResponseEntity.ok(profileService.getProfile(dbUser));
  }

  @Override
  public ResponseEntity<Void> confirmProfile() {
    userService.confirmProfile(userProvider.get());
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  @Override
  public ResponseEntity<Void> confirmPublications() {
    userService.confirmPublications();
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }
}
