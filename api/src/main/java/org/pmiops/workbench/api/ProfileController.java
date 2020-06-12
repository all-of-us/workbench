package org.pmiops.workbench.api;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
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
import org.pmiops.workbench.db.model.DbDemographicSurvey;
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
import org.pmiops.workbench.firecloud.model.FirecloudBillingProjectMembership;
import org.pmiops.workbench.firecloud.model.FirecloudBillingProjectMembership.CreationStatusEnum;
import org.pmiops.workbench.firecloud.model.FirecloudJWTWrapper;
import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.google.DirectoryService;
import org.pmiops.workbench.institution.InstitutionService;
import org.pmiops.workbench.institution.VerifiedInstitutionalAffiliationMapper;
import org.pmiops.workbench.mail.MailService;
import org.pmiops.workbench.model.AccessBypassRequest;
import org.pmiops.workbench.model.Address;
import org.pmiops.workbench.model.Authority;
import org.pmiops.workbench.model.BillingProjectMembership;
import org.pmiops.workbench.model.BillingProjectStatus;
import org.pmiops.workbench.model.CreateAccountRequest;
import org.pmiops.workbench.model.DemographicSurvey;
import org.pmiops.workbench.model.Disability;
import org.pmiops.workbench.model.EmailVerificationStatus;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.Institution;
import org.pmiops.workbench.model.InvitationVerificationRequest;
import org.pmiops.workbench.model.NihToken;
import org.pmiops.workbench.model.PageVisit;
import org.pmiops.workbench.model.Profile;
import org.pmiops.workbench.model.ResendWelcomeEmailRequest;
import org.pmiops.workbench.model.UpdateContactEmailRequest;
import org.pmiops.workbench.model.UsernameTakenResponse;
import org.pmiops.workbench.model.VerifiedInstitutionalAffiliation;
import org.pmiops.workbench.moodle.ApiException;
import org.pmiops.workbench.profile.AddressMapper;
import org.pmiops.workbench.profile.DemographicSurveyMapper;
import org.pmiops.workbench.profile.ProfileService;
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
  private static final Function<FirecloudBillingProjectMembership, BillingProjectMembership>
      TO_CLIENT_BILLING_PROJECT_MEMBERSHIP =
          new Function<FirecloudBillingProjectMembership, BillingProjectMembership>() {
            @Override
            public BillingProjectMembership apply(
                FirecloudBillingProjectMembership billingProjectMembership) {
              BillingProjectMembership result = new BillingProjectMembership();
              result.setProjectName(billingProjectMembership.getProjectName());
              result.setRole(billingProjectMembership.getRole());
              result.setStatus(
                  fcToWorkbenchBillingMap.get(billingProjectMembership.getCreationStatus()));
              return result;
            }
          };

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

  private static final Function<DemographicSurvey, DbDemographicSurvey>
      FROM_CLIENT_DEMOGRAPHIC_SURVEY =
          new Function<DemographicSurvey, DbDemographicSurvey>() {
            @Override
            public DbDemographicSurvey apply(DemographicSurvey demographicSurvey) {
              DbDemographicSurvey result = new DbDemographicSurvey();
              if (demographicSurvey.getRace() != null) {
                result.setRaceEnum(demographicSurvey.getRace());
              }
              if (demographicSurvey.getEthnicity() != null) {
                result.setEthnicityEnum(demographicSurvey.getEthnicity());
              }
              if (demographicSurvey.getDisability() != null) {
                result.setDisabilityEnum(
                    demographicSurvey.getDisability() ? Disability.TRUE : Disability.FALSE);
              }
              if (demographicSurvey.getEducation() != null) {
                result.setEducationEnum(demographicSurvey.getEducation());
              }
              result.setIdentifiesAsLgbtq(demographicSurvey.getIdentifiesAsLgbtq());
              result.setLgbtqIdentity(demographicSurvey.getLgbtqIdentity());
              if (demographicSurvey.getDisability() != null) {
                result.setDisabilityEnum(
                    demographicSurvey.getDisability() ? Disability.TRUE : Disability.FALSE);
              }
              if (demographicSurvey.getGenderIdentityList() != null) {
                result.setGenderIdentityEnumList(demographicSurvey.getGenderIdentityList());
              }
              if (demographicSurvey.getSexAtBirth() != null) {
                result.setSexAtBirthEnum(demographicSurvey.getSexAtBirth());
              }
              if (demographicSurvey.getYearOfBirth() != null) {
                result.setYear_of_birth(demographicSurvey.getYearOfBirth().intValue());
              }
              return result;
            }
          };

  private static final Logger log = Logger.getLogger(ProfileController.class.getName());

  private static final long MAX_BILLING_PROJECT_CREATION_ATTEMPTS = 5;

  private final ActionAuditQueryService actionAuditQueryService;
  private final AddressMapper addressMapper;
  private final CaptchaVerificationService captchaVerificationService;
  private final Clock clock;
  private final CloudStorageService cloudStorageService;
  private final DemographicSurveyMapper demographicSurveyMapper;
  private final DirectoryService directoryService;
  private final FireCloudService fireCloudService;
  private final InstitutionService institutionService;
  private final ProfileAuditor profileAuditor;
  private final ProfileService profileService;
  private final Provider<DbUser> userProvider;
  private final Provider<MailService> mailServiceProvider;
  private final Provider<UserAuthentication> userAuthenticationProvider;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final UserDao userDao;
  private final UserService userService;
  private final VerifiedInstitutionalAffiliationMapper verifiedInstitutionalAffiliationMapper;

  @Autowired
  ProfileController(
      ActionAuditQueryService actionAuditQueryService,
      AddressMapper addressMapper,
      CaptchaVerificationService captchaVerificationService,
      Clock clock,
      CloudStorageService cloudStorageService,
      DemographicSurveyMapper demographicSurveyMapper,
      DirectoryService directoryService,
      FireCloudService fireCloudService,
      InstitutionService institutionService,
      ProfileAuditor profileAuditor,
      ProfileService profileService,
      Provider<DbUser> userProvider,
      Provider<MailService> mailServiceProvider,
      Provider<UserAuthentication> userAuthenticationProvider,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      UserDao userDao,
      UserService userService,
      VerifiedInstitutionalAffiliationMapper verifiedInstitutionalAffiliationMapper) {
    this.actionAuditQueryService = actionAuditQueryService;
    this.addressMapper = addressMapper;
    this.captchaVerificationService = captchaVerificationService;
    this.clock = clock;
    this.cloudStorageService = cloudStorageService;
    this.demographicSurveyMapper = demographicSurveyMapper;
    this.directoryService = directoryService;
    this.fireCloudService = fireCloudService;
    this.institutionService = institutionService;
    this.mailServiceProvider = mailServiceProvider;
    this.profileAuditor = profileAuditor;
    this.profileService = profileService;
    this.userAuthenticationProvider = userAuthenticationProvider;
    this.userDao = userDao;
    this.userProvider = userProvider;
    this.userService = userService;
    this.verifiedInstitutionalAffiliationMapper = verifiedInstitutionalAffiliationMapper;
    this.workbenchConfigProvider = workbenchConfigProvider;
  }

  @Override
  public ResponseEntity<List<BillingProjectMembership>> getBillingProjects() {
    List<FirecloudBillingProjectMembership> memberships =
        fireCloudService.getBillingProjectMemberships();
    return ResponseEntity.ok(
        memberships.stream()
            .map(TO_CLIENT_BILLING_PROJECT_MEMBERSHIP)
            .collect(Collectors.toList()));
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

    if (workbenchConfigProvider.get().access.requireInvitationKey) {
      verifyInvitationKey(request.getInvitationKey());
    }

    if (workbenchConfigProvider.get().featureFlags.requireInstitutionalVerification) {
      profileService.validateInstitutionalAffiliation(request.getProfile());
    }

    final Profile profile = request.getProfile();

    // We don't include this check in validateAndCleanProfile since some existing user profiles
    // may have empty addresses. So we only check this on user creation, not update.
    Optional.ofNullable(profile.getAddress())
        .orElseThrow(() -> new BadRequestException("Address must not be empty"));

    profileService.validateAndCleanProfile(profile);

    com.google.api.services.directory.model.User googleUser =
        directoryService.createUser(
            profile.getGivenName(),
            profile.getFamilyName(),
            profile.getUsername(),
            profile.getContactEmail());

    // Create a user that has no data access or FC user associated.
    // We create this account before they sign in so we can keep track of which users we have
    // created Google accounts for. This can be used subsequently to delete orphaned accounts.

    // We store this information in our own database so that:
    // 1) we can support bring-your-own account in future (when we won't be using directory service)
    // 2) we can easily generate lists of researchers for the storefront, without joining to Google

    // It's possible for the profile information to become out of sync with the user's Google
    // profile, since it can be edited in our UI as well as the Google UI,  and we're fine with
    // that; the expectation is their profile in AofU will be managed in AofU, not in Google.

    DbUser user =
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
            profile.getInstitutionalAffiliations().stream()
                .map(institutionService::legacyInstitutionToDbInstitution)
                .collect(Collectors.toList()),
            verifiedInstitutionalAffiliationMapper.modelToDbWithoutUser(
                profile.getVerifiedInstitutionalAffiliation(), institutionService));

    if (request.getTermsOfServiceVersion() != null) {
      userService.submitTermsOfService(user, request.getTermsOfServiceVersion());
    }

    final MailService mail = mailServiceProvider.get();

    try {
      mail.sendWelcomeEmail(profile.getContactEmail(), googleUser.getPassword(), googleUser);
    } catch (MessagingException e) {
      throw new WorkbenchException(e);
    }

    if (workbenchConfigProvider.get().featureFlags.requireInstitutionalVerification) {
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
    }

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
      if (workbenchConfigProvider.get().featureFlags.enableMoodleV2Api) {
        userService.syncComplianceTrainingStatusV2();
      } else {
        userService.syncComplianceTrainingStatusV1();
      }
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

  @Override
  public ResponseEntity<Void> invitationKeyVerification(
      InvitationVerificationRequest invitationVerificationRequest) {
    verifyInvitationKey(invitationVerificationRequest.getInvitationKey());
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  private void verifyInvitationKey(String invitationKey) {
    if (invitationKey == null
        || invitationKey.equals("")
        || !invitationKey.equals(cloudStorageService.readInvitationKey())) {
      throw new BadRequestException(
          "Missing or incorrect invitationKey (this API is not yet publicly launched)");
    }
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
    DbUser user = userDao.findUserByUsername(username);
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
    DbUser user = userDao.findUserByUsername(username);
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
      final DbPageVisit firstPageVisit = new DbPageVisit();
      firstPageVisit.setPageId(newPageVisit.getPage());
      firstPageVisit.setUser(dbUser);
      firstPageVisit.setFirstVisit(timestamp);
      dbUser.getPageVisits().add(firstPageVisit);
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

    profileService.updateProfileForUser(user, updatedProfile, previousProfile);

    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  @AuthorityRequired(Authority.ACCESS_CONTROL_ADMIN)
  @Override
  public ResponseEntity<EmptyResponse> updateVerifiedInstitutionalAffiliation(
      Long userId, VerifiedInstitutionalAffiliation verifiedAffiliation) {
    DbUser dbUser = userDao.findUserByUserId(userId);
    Profile updatedProfile = profileService.getProfile(dbUser);

    if (verifiedAffiliation == null) {
      throw new BadRequestException("Cannot delete Verified Institutional Affiliation.");
    }

    final Optional<Institution> institution =
        institutionService.getInstitution(verifiedAffiliation.getInstitutionShortName());
    institution.ifPresent(i -> verifiedAffiliation.setInstitutionDisplayName(i.getDisplayName()));

    updatedProfile.setVerifiedInstitutionalAffiliation(verifiedAffiliation);

    Profile oldProfile = profileService.getProfile(dbUser);

    profileService.updateProfileForUser(dbUser, updatedProfile, oldProfile);

    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  public ResponseEntity<EmptyResponse> unsafeSelfBypassAccessRequirement(
      AccessBypassRequest accessBypassRequest) {
    if (!workbenchConfigProvider.get().access.unsafeAllowSelfBypass) {
      throw new ForbiddenException("Self bypass is disallowed in this environment.");
    }
    long userId = userProvider.get().getUserId();
    userService.updateBypassTime(userId, accessBypassRequest);
    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  public ResponseEntity<Profile> updateNihToken(NihToken token) {
    if (token == null || token.getJwt() == null) {
      throw new BadRequestException("Token is required.");
    }
    FirecloudJWTWrapper wrapper = new FirecloudJWTWrapper().jwt(token.getJwt());
    try {
      fireCloudService.postNihCallback(wrapper);
      userService.syncEraCommonsStatus();
      return getProfileResponse(userProvider.get());
    } catch (WorkbenchException e) {
      throw e;
    }
  }

  @Override
  public ResponseEntity<Void> deleteProfile() {
    if (!workbenchConfigProvider.get().featureFlags.unsafeAllowDeleteUser) {
      throw new ForbiddenException("Self account deletion is disallowed in this environment.");
    }
    DbUser user = userProvider.get();
    log.log(Level.WARNING, "Deleting profile: user email: " + user.getUsername());
    directoryService.deleteUser(user.getUsername().split("@")[0]);
    userDao.delete(user.getUserId());
    profileAuditor.fireDeleteAction(user.getUserId(), user.getUsername());

    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }
}
