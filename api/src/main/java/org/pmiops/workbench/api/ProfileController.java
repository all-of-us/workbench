package org.pmiops.workbench.api;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Provider;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import org.pmiops.workbench.actionaudit.ActionAuditQueryService;
import org.pmiops.workbench.actionaudit.auditors.ProfileAuditor;
import org.pmiops.workbench.annotations.AuthorityRequired;
import org.pmiops.workbench.auth.UserAuthentication;
import org.pmiops.workbench.auth.UserAuthentication.UserType;
import org.pmiops.workbench.captcha.CaptchaVerificationService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.DbAddress;
import org.pmiops.workbench.db.model.DbPageVisit;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.EmailException;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.exceptions.UnauthorizedException;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudBillingProjectMembership.CreationStatusEnum;
import org.pmiops.workbench.google.CloudStorageClient;
import org.pmiops.workbench.google.DirectoryService;
import org.pmiops.workbench.institution.InstitutionService;
import org.pmiops.workbench.institution.VerifiedInstitutionalAffiliationMapper;
import org.pmiops.workbench.mail.MailService;
import org.pmiops.workbench.model.AccessBypassRequest;
import org.pmiops.workbench.model.AccountPropertyUpdate;
import org.pmiops.workbench.model.Address;
import org.pmiops.workbench.model.AdminUserListResponse;
import org.pmiops.workbench.model.Authority;
import org.pmiops.workbench.model.BillingProjectStatus;
import org.pmiops.workbench.model.CreateAccountRequest;
import org.pmiops.workbench.model.EmailVerificationStatus;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.Institution;
import org.pmiops.workbench.model.NihToken;
import org.pmiops.workbench.model.PageVisit;
import org.pmiops.workbench.model.Profile;
import org.pmiops.workbench.model.RasLinkRequestBody;
import org.pmiops.workbench.model.ResendWelcomeEmailRequest;
import org.pmiops.workbench.model.UpdateContactEmailRequest;
import org.pmiops.workbench.model.UserAccessExpiration;
import org.pmiops.workbench.model.UserAuditLogQueryResponse;
import org.pmiops.workbench.model.UsernameTakenResponse;
import org.pmiops.workbench.model.VerifiedInstitutionalAffiliation;
import org.pmiops.workbench.moodle.ApiException;
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
  private static final Map<CreationStatusEnum, BillingProjectStatus> fcToWorkbenchBillingMap =
      new ImmutableMap.Builder<CreationStatusEnum, BillingProjectStatus>()
          .put(CreationStatusEnum.CREATING, BillingProjectStatus.PENDING)
          .put(CreationStatusEnum.READY, BillingProjectStatus.READY)
          .put(CreationStatusEnum.ERROR, BillingProjectStatus.ERROR)
          .build();

  private static final Function<Address, DbAddress> FROM_CLIENT_ADDRESS =
      new Function<Address, DbAddress>() {
        @Override
        public DbAddress apply(Address address) {
          DbAddress result = new DbAddress();
          result.setStreetAddress1(address.getStreetAddress1());
          result.setStreetAddress2(address.getStreetAddress2());
          result.setCity(address.getCity());
          result.setState(address.getState());
          result.setZipCode(address.getZipCode());
          result.setCountry(address.getCountry());
          return result;
        }
      };

  private static final Logger log = Logger.getLogger(ProfileController.class.getName());

  private final ActionAuditQueryService actionAuditQueryService;
  private final CaptchaVerificationService captchaVerificationService;
  private final Clock clock;
  private final CloudStorageClient cloudStorageClient;
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
      ActionAuditQueryService actionAuditQueryService,
      CaptchaVerificationService captchaVerificationService,
      Clock clock,
      CloudStorageClient cloudStorageClient,
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
    this.actionAuditQueryService = actionAuditQueryService;
    this.captchaVerificationService = captchaVerificationService;
    this.clock = clock;
    this.cloudStorageClient = cloudStorageClient;
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

  private DbUser initializeUserIfNeeded() {
    UserAuthentication userAuthentication = userAuthenticationProvider.get();
    DbUser dbUser = userAuthentication.getUser();
    if (userAuthentication.getUserType() == UserType.SERVICE_ACCOUNT) {
      // Service accounts don't need further initialization.
      return dbUser;
    }

    // On first sign-in, create a FC user, billing project, and set the first sign in time.
    if (dbUser.getFirstSignInTime() == null) {
      // If the user is already registered, their profile will get updated.
      fireCloudService.registerUser(
          dbUser.getContactEmail(), dbUser.getGivenName(), dbUser.getFamilyName());

      dbUser.setFirstSignInTime(new Timestamp(clock.instant().toEpochMilli()));
      // If the user is logged in, then we know that they have followed the account creation
      // instructions sent to
      // their initial contact email address.
      dbUser.setEmailVerificationStatusEnum(EmailVerificationStatus.SUBSCRIBED);
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
    // Record that the user signed in, and create the user's FireCloud user and free tier billing
    // project if they haven't been created already.
    // This means they can start using the NIH billing account in FireCloud (without access to
    // the CDR); we will probably need a job that deactivates accounts after some period of
    // not accepting the terms of use.

    DbUser dbUser = initializeUserIfNeeded();
    profileAuditor.fireLoginAction(dbUser);
    return getProfileResponse(dbUser);
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
    profileService.validateAffiliation(request.getProfile());

    final Profile profile = request.getProfile();

    profileService.cleanProfile(profile);
    profileService.validateNewProfile(profile);

    String gSuiteUsername =
        profile.getUsername()
            + "@"
            + workbenchConfigProvider.get().googleDirectoryService.gSuiteDomain;

    com.google.api.services.directory.model.User googleUser =
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
              googleUser.getPrimaryEmail(),
              profile.getContactEmail(),
              profile.getCurrentPosition(),
              profile.getOrganization(),
              profile.getAreaOfResearch(),
              profile.getProfessionalUrl(),
              profile.getDegrees(),
              FROM_CLIENT_ADDRESS.apply(profile.getAddress()),
              demographicSurveyMapper.demographicSurveyToDbDemographicSurvey(
                  profile.getDemographicSurvey()),
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

    if (request.getTermsOfServiceVersion() != null) {
      userService.submitTermsOfService(user, request.getTermsOfServiceVersion());
    }

    final MailService mail = mailServiceProvider.get();

    try {
      mail.sendWelcomeEmail(profile.getContactEmail(), googleUser.getPassword(), googleUser);
    } catch (MessagingException e) {
      throw new WorkbenchException(e);
    }

    institutionService
        .getInstitutionUserInstructions(
            profile.getVerifiedInstitutionalAffiliation().getInstitutionShortName())
        .ifPresent(
            instructions -> {
              try {
                mail.sendInstitutionUserInstructions(profile.getContactEmail(), instructions);
              } catch (MessagingException e) {
                throw new WorkbenchException(e);
              }
            });

    // Note: Avoid getProfileResponse() here as this is not an authenticated request.
    final Profile createdProfile = profileService.getProfile(user);
    profileAuditor.fireCreateAction(createdProfile);
    return ResponseEntity.ok(createdProfile);
  }

  @Override
  public ResponseEntity<Profile> requestBetaAccess() {
    Timestamp now = new Timestamp(clock.instant().toEpochMilli());
    DbUser user = userProvider.get();
    if (user.getBetaAccessRequestTime() == null) {
      log.log(
          Level.INFO,
          String.format("Sending beta access request email to %s.", user.getContactEmail()));
      try {
        mailServiceProvider.get().sendBetaAccessRequestEmail(user.getUsername());
      } catch (MessagingException e) {
        throw new EmailException("Error submitting beta access request", e);
      }
      user.setBetaAccessRequestTime(now);
      user = saveUserWithConflictHandling(user);
    }
    return getProfileResponse(user);
  }

  @Override
  public ResponseEntity<Profile> submitDataUseAgreement(
      Integer dataUseAgreementSignedVersion, String initials) {
    DbUser user =
        userService.submitDataUseAgreement(
            userProvider.get(), dataUseAgreementSignedVersion, initials);
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
    com.google.api.services.directory.model.User googleUser = directoryService.getUser(username);
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
    com.google.api.services.directory.model.User googleUser = directoryService.getUser(username);
    DbUser user = userService.getByUsernameOrThrow(username);
    checkUserCreationNonce(user, resendRequest.getCreationNonce());
    if (userHasEverLoggedIn(googleUser, user)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
    return resetPasswordAndSendWelcomeEmail(username, user);
  }

  private boolean userHasEverLoggedIn(
      com.google.api.services.directory.model.User googleUser, DbUser user) {
    return user.getFirstSignInTime() != null || !googleUser.getChangePasswordAtNextLogin();
  }

  private ResponseEntity<Void> resetPasswordAndSendWelcomeEmail(String username, DbUser user) {
    com.google.api.services.directory.model.User googleUser =
        directoryService.resetUserPassword(username);
    try {
      mailServiceProvider
          .get()
          .sendWelcomeEmail(user.getContactEmail(), googleUser.getPassword(), googleUser);
    } catch (MessagingException e) {
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

    profileService.updateProfile(user, updatedProfile, previousProfile);
    confirmProfile();

    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  @AuthorityRequired(Authority.ACCESS_CONTROL_ADMIN)
  @Override
  @Deprecated // use updateAccountProperties()
  public ResponseEntity<EmptyResponse> updateVerifiedInstitutionalAffiliation(
      Long userId, VerifiedInstitutionalAffiliation verifiedAffiliation) {
    DbUser dbUser = userDao.findUserByUserId(userId);
    Profile updatedProfile = profileService.getProfile(dbUser);

    if (verifiedAffiliation == null) {
      throw new BadRequestException("Cannot delete Verified Institutional Affiliation.");
    }

    Optional<Institution> institution =
        institutionService.getInstitution(verifiedAffiliation.getInstitutionShortName());
    institution.ifPresent(i -> verifiedAffiliation.setInstitutionDisplayName(i.getDisplayName()));

    updatedProfile.setVerifiedInstitutionalAffiliation(verifiedAffiliation);

    Profile oldProfile = profileService.getProfile(dbUser);

    profileService.updateProfile(dbUser, updatedProfile, oldProfile);

    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  @AuthorityRequired({Authority.ACCESS_CONTROL_ADMIN})
  public ResponseEntity<AdminUserListResponse> getAllUsers() {
    return ResponseEntity.ok(
        new AdminUserListResponse().users(profileService.getAdminTableUsers()));
  }

  @Override
  @AuthorityRequired({Authority.ACCESS_CONTROL_ADMIN})
  public ResponseEntity<Profile> getUser(Long userId) {
    DbUser user = userDao.findUserByUserId(userId);
    return ResponseEntity.ok(profileService.getProfile(user));
  }

  @Override
  @AuthorityRequired({Authority.ACCESS_CONTROL_ADMIN})
  public ResponseEntity<Profile> getUserByUsername(String username) {
    DbUser user = userService.getByUsernameOrThrow(username);
    return ResponseEntity.ok(profileService.getProfile(user));
  }

  @Override
  @AuthorityRequired({Authority.ACCESS_CONTROL_ADMIN})
  public ResponseEntity<EmptyResponse> bypassAccessRequirement(
      Long userId, AccessBypassRequest request) {
    userService.updateBypassTime(userId, request);
    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  public ResponseEntity<EmptyResponse> unsafeSelfBypassAccessRequirement(
      AccessBypassRequest request) {
    if (!workbenchConfigProvider.get().access.unsafeAllowSelfBypass) {
      throw new ForbiddenException("Self bypass is disallowed in this environment.");
    }
    long userId = userProvider.get().getUserId();
    userService.updateBypassTime(userId, request);
    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  @AuthorityRequired({Authority.ACCESS_CONTROL_ADMIN})
  public ResponseEntity<Profile> updateAccountProperties(AccountPropertyUpdate request) {
    return ResponseEntity.ok(profileService.updateAccountProperties(request));
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
  public ResponseEntity<UserAuditLogQueryResponse> getAuditLogEntries(
      String usernameWithoutGsuiteDomain,
      Integer limit,
      Long afterMillis,
      Long beforeMillisNullable) {
    final String username =
        String.format(
            "%s@%s",
            usernameWithoutGsuiteDomain,
            workbenchConfigProvider.get().googleDirectoryService.gSuiteDomain);
    final long userDatabaseId = userService.getByUsernameOrThrow(username).getUserId();
    final Instant after = Instant.ofEpochMilli(afterMillis);
    final Instant before =
        Optional.ofNullable(beforeMillisNullable).map(Instant::ofEpochMilli).orElse(Instant.now());
    return ResponseEntity.ok(
        actionAuditQueryService.queryEventsForUser(userDatabaseId, limit, after, before));
  }

  @Override
  public ResponseEntity<Profile> linkRasAccount(RasLinkRequestBody body) {
    DbUser dbUser =
        rasLinkService.linkRasLoginGovAccount(body.getAuthCode(), body.getRedirectUrl());
    return ResponseEntity.ok(profileService.getProfile(dbUser));
  }

  @Override
  public ResponseEntity<Void> confirmProfile() {
    userService.confirmProfile();
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  @Override
  public ResponseEntity<Void> confirmPublications() {
    userService.confirmPublications();
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  /**
   * Gets a JSON list of users and their registered tier access expiration dates.
   *
   * <p>This endpoint is intended as a temporary manual measure to assist with user communication
   * during the rollout of Annual Access Renewal (AAR).
   *
   * <p>Once fully rolled out, we will have an automated expiration email process and this can
   * likely be removed. See RW-6689 and RW-6703.
   */
  @Override
  @AuthorityRequired({Authority.ACCESS_CONTROL_ADMIN})
  public ResponseEntity<List<UserAccessExpiration>> getRegisteredTierAccessExpirations() {
    return ResponseEntity.ok(userService.getRegisteredTierExpirations());
  }
}
