package org.pmiops.workbench.api;

import java.util.Optional;
import javax.inject.Provider;
import org.joda.time.DateTime;
import org.pmiops.workbench.actionaudit.ActionAuditQueryService;
import org.pmiops.workbench.actionaudit.auditors.ProfileAuditor;
import org.pmiops.workbench.annotations.AuthorityRequired;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudJWTWrapper;
import org.pmiops.workbench.google.DirectoryService;
import org.pmiops.workbench.model.AccessBypassRequest;
import org.pmiops.workbench.model.Authority;
import org.pmiops.workbench.model.CreateAccountRequest;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.InvitationVerificationRequest;
import org.pmiops.workbench.model.NihToken;
import org.pmiops.workbench.model.PageVisit;
import org.pmiops.workbench.model.Profile;
import org.pmiops.workbench.model.ResendWelcomeEmailRequest;
import org.pmiops.workbench.model.UpdateContactEmailRequest;
import org.pmiops.workbench.model.UserAuditLogQueryResponse;
import org.pmiops.workbench.model.UserListResponse;
import org.pmiops.workbench.model.UsernameTakenResponse;
import org.pmiops.workbench.model.VerifiedInstitutionalAffiliation;
import org.pmiops.workbench.moodle.ApiException;
import org.pmiops.workbench.profile.ProfileService;
import org.pmiops.workbench.shibboleth.ShibbolethService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contains implementations for all Workbench API methods tagged with "profile".
 *
 * <p>The majority of handlers here are lightweight wrappers which delegate to UserService, where
 * many user-focused database and/or API calls are implemented.
 */
@RestController
public class ProfileController implements ProfileApiDelegate {
  private final ActionAuditQueryService actionAuditQueryService;
  private final DirectoryService directoryService;
  private final FireCloudService fireCloudService;
  private final ProfileAuditor profileAuditor;
  private final ProfileService profileService;
  private final Provider<DbUser> userProvider;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final ShibbolethService shibbolethService;
  private final UserDao userDao;
  private final UserService userService;

  @Autowired
  ProfileController(
      ActionAuditQueryService actionAuditQueryService,
      DirectoryService directoryService,
      FireCloudService fireCloudService,
      ProfileAuditor profileAuditor,
      ProfileService profileService,
      Provider<DbUser> userProvider,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      ShibbolethService shibbolethService,
      UserDao userDao,
      UserService userService) {
    this.actionAuditQueryService = actionAuditQueryService;
    this.directoryService = directoryService;
    this.fireCloudService = fireCloudService;
    this.profileAuditor = profileAuditor;
    this.profileService = profileService;
    this.shibbolethService = shibbolethService;
    this.userDao = userDao;
    this.userProvider = userProvider;
    this.userService = userService;
    this.workbenchConfigProvider = workbenchConfigProvider;
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

    DbUser dbUser = profileService.initializeUserIfNeeded();
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
    final Profile createdProfile = profileService.createAccount(request);

    profileAuditor.fireCreateAction(createdProfile);
    // Note: Avoid getProfileResponse() here as this is not an authenticated request.
    return ResponseEntity.ok(createdProfile);
  }

  @Override
  public ResponseEntity<Profile> requestBetaAccess() {
    DbUser user = profileService.requestBetaAccess();
    return getProfileResponse(user);
  }

  @Override
  public ResponseEntity<Profile> submitDataUseAgreement(
      Integer dataUseAgreementSignedVersion, String initials) {
    DbUser user = profileService.submitDataUseAgreement(dataUseAgreementSignedVersion, initials);
    return getProfileResponse(user);
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
    profileService.verifyInvitationKey(invitationVerificationRequest.getInvitationKey());
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  /*
   * This un-authed API method is limited such that we only allow contact email updates before the user has signed in
   * with the newly created gsuite account. Once the user has logged in, they can change their contact email through
   * the normal profile update process.
   */
  @Override
  public ResponseEntity<Void> updateContactEmail(
      UpdateContactEmailRequest updateContactEmailRequest) {
    return profileService.updateContactEmail(updateContactEmailRequest);
  }

  @Override
  public ResponseEntity<Void> resendWelcomeEmail(ResendWelcomeEmailRequest resendRequest) {
    return profileService.resendWelcomeEmail(resendRequest);
  }

  @Override
  public ResponseEntity<Profile> updatePageVisits(PageVisit newPageVisit) {
    DbUser dbUser = profileService.updatePageVisits(newPageVisit);
    return getProfileResponse(dbUser);
  }

  @Override
  public ResponseEntity<Void> updateProfile(Profile updatedProfile) {
    profileService.updateProfile(updatedProfile);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  @AuthorityRequired(Authority.ACCESS_CONTROL_ADMIN)
  @Override
  public ResponseEntity<EmptyResponse> updateVerifiedInstitutionalAffiliation(
      Long userId, VerifiedInstitutionalAffiliation verifiedAffiliation) {
    profileService.updateVerifiedInstitutionalAffiliation(userId, verifiedAffiliation);
    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  @AuthorityRequired({Authority.ACCESS_CONTROL_ADMIN})
  public ResponseEntity<UserListResponse> getAllUsers() {
    UserListResponse response = new UserListResponse();
    response.setProfileList(profileService.listAllProfiles());
    return ResponseEntity.ok(response);
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
  public ResponseEntity<Profile> updateNihToken(NihToken token) {
    if (token == null || token.getJwt() == null) {
      throw new BadRequestException("Token is required.");
    }

    if (workbenchConfigProvider.get().featureFlags.useNewShibbolethService) {
      shibbolethService.updateShibbolethToken(token.getJwt());
    } else {
      fireCloudService.postNihCallback(new FirecloudJWTWrapper().jwt(token.getJwt()));
    }

    userService.syncEraCommonsStatus();
    return getProfileResponse(userProvider.get());
  }

  @Override
  public ResponseEntity<Void> deleteProfile() {
    profileService.deleteProfile();
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
    final DateTime after = new DateTime(afterMillis);
    final DateTime before =
        Optional.ofNullable(beforeMillisNullable).map(DateTime::new).orElse(DateTime.now());
    return ResponseEntity.ok(
        actionAuditQueryService.queryEventsForUser(userDatabaseId, limit, after, before));
  }
}
