package org.pmiops.workbench.profile;

import com.google.common.base.Strings;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.inject.Provider;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import org.apache.commons.lang3.StringUtils;
import org.pmiops.workbench.actionaudit.ActionAuditQueryService;
import org.pmiops.workbench.actionaudit.auditors.ProfileAuditor;
import org.pmiops.workbench.auth.UserAuthentication;
import org.pmiops.workbench.auth.UserAuthentication.UserType;
import org.pmiops.workbench.billing.FreeTierBillingService;
import org.pmiops.workbench.captcha.CaptchaVerificationService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.InstitutionDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.dao.UserTermsOfServiceDao;
import org.pmiops.workbench.db.dao.VerifiedInstitutionalAffiliationDao;
import org.pmiops.workbench.db.model.DbDemographicSurvey;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbInstitutionalAffiliation;
import org.pmiops.workbench.db.model.DbPageVisit;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserTermsOfService;
import org.pmiops.workbench.db.model.DbVerifiedInstitutionalAffiliation;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.EmailException;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.exceptions.UnauthorizedException;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.google.DirectoryService;
import org.pmiops.workbench.institution.InstitutionService;
import org.pmiops.workbench.institution.VerifiedInstitutionalAffiliationMapper;
import org.pmiops.workbench.mail.MailService;
import org.pmiops.workbench.model.Address;
import org.pmiops.workbench.model.CreateAccountRequest;
import org.pmiops.workbench.model.DemographicSurvey;
import org.pmiops.workbench.model.EmailVerificationStatus;
import org.pmiops.workbench.model.Institution;
import org.pmiops.workbench.model.InstitutionalRole;
import org.pmiops.workbench.model.PageVisit;
import org.pmiops.workbench.model.Profile;
import org.pmiops.workbench.model.ResendWelcomeEmailRequest;
import org.pmiops.workbench.model.UpdateContactEmailRequest;
import org.pmiops.workbench.model.VerifiedInstitutionalAffiliation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

@Service
public class ProfileService {
  private static final Logger log = Logger.getLogger(ProfileService.class.getName());

  private final AddressMapper addressMapper;
  private final CaptchaVerificationService captchaVerificationService;
  private final Clock clock;
  private final CloudStorageService cloudStorageService;
  private final DemographicSurveyMapper demographicSurveyMapper;
  private final DirectoryService directoryService;
  private final FireCloudService fireCloudService;
  private final FreeTierBillingService freeTierBillingService;
  private final InstitutionDao institutionDao;
  private final InstitutionService institutionService;
  private final PageVisitMapper pageVisitMapper;
  private final ProfileAuditor profileAuditor;
  private final ProfileMapper profileMapper;
  private final Provider<DbUser> userProvider;
  private final Provider<MailService> mailServiceProvider;
  private final Provider<UserAuthentication> userAuthenticationProvider;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final UserDao userDao;
  private final UserService userService;
  private final UserTermsOfServiceDao userTermsOfServiceDao;
  private final VerifiedInstitutionalAffiliationDao verifiedInstitutionalAffiliationDao;
  private final VerifiedInstitutionalAffiliationMapper verifiedInstitutionalAffiliationMapper;

  @Autowired
  public ProfileService(
      ActionAuditQueryService actionAuditQueryService,
      AddressMapper addressMapper,
      CaptchaVerificationService captchaVerificationService,
      Clock clock,
      CloudStorageService cloudStorageService,
      DemographicSurveyMapper demographicSurveyMapper,
      DirectoryService directoryService,
      FireCloudService fireCloudService,
      FreeTierBillingService freeTierBillingService,
      InstitutionDao institutionDao,
      InstitutionService institutionService,
      PageVisitMapper pageVisitMapper,
      ProfileAuditor profileAuditor,
      ProfileMapper profileMapper,
      Provider<DbUser> userProvider,
      Provider<MailService> mailServiceProvider,
      Provider<UserAuthentication> userAuthenticationProvider,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      UserDao userDao,
      UserService userService,
      UserTermsOfServiceDao userTermsOfServiceDao,
      VerifiedInstitutionalAffiliationDao verifiedInstitutionalAffiliationDao,
      VerifiedInstitutionalAffiliationMapper verifiedInstitutionalAffiliationMapper) {
    this.addressMapper = addressMapper;
    this.captchaVerificationService = captchaVerificationService;
    this.clock = clock;
    this.cloudStorageService = cloudStorageService;
    this.demographicSurveyMapper = demographicSurveyMapper;
    this.directoryService = directoryService;
    this.fireCloudService = fireCloudService;
    this.freeTierBillingService = freeTierBillingService;
    this.institutionDao = institutionDao;
    this.institutionService = institutionService;
    this.mailServiceProvider = mailServiceProvider;
    this.pageVisitMapper = pageVisitMapper;
    this.profileAuditor = profileAuditor;
    this.profileMapper = profileMapper;
    this.userAuthenticationProvider = userAuthenticationProvider;
    this.userDao = userDao;
    this.userProvider = userProvider;
    this.userService = userService;
    this.userTermsOfServiceDao = userTermsOfServiceDao;
    this.verifiedInstitutionalAffiliationDao = verifiedInstitutionalAffiliationDao;
    this.verifiedInstitutionalAffiliationMapper = verifiedInstitutionalAffiliationMapper;
    this.workbenchConfigProvider = workbenchConfigProvider;
  }

  public Profile getProfile(DbUser user) {
    // Fetch the user's authorities, since they aren't loaded during normal request interception.
    DbUser userWithAuthoritiesAndPageVisits =
        userDao.findUserWithAuthoritiesAndPageVisits(user.getUserId());
    if (userWithAuthoritiesAndPageVisits != null) {
      // If the user is already written to the database, use it and whatever authorities and page
      // visits are there.
      user = userWithAuthoritiesAndPageVisits;
    }

    Profile profile = profileMapper.dbUserToProfile(user);

    profile.setFreeTierUsage(freeTierBillingService.getCachedFreeTierUsage(user));
    profile.setFreeTierDollarQuota(freeTierBillingService.getUserFreeTierDollarLimit(user));

    verifiedInstitutionalAffiliationDao
        .findFirstByUser(user)
        .ifPresent(
            verifiedInstitutionalAffiliation ->
                profile.setVerifiedInstitutionalAffiliation(
                    verifiedInstitutionalAffiliationMapper.dbToModel(
                        verifiedInstitutionalAffiliation)));

    Optional<DbUserTermsOfService> latestTermsOfServiceMaybe =
        userTermsOfServiceDao.findFirstByUserIdOrderByTosVersionDesc(user.getUserId());
    if (latestTermsOfServiceMaybe.isPresent()) {
      profile.setLatestTermsOfServiceVersion(latestTermsOfServiceMaybe.get().getTosVersion());
      profile.setLatestTermsOfServiceTime(
          latestTermsOfServiceMaybe.get().getAgreementTime().getTime());
    }

    return profile;
  }

  public void validateInstitutionalAffiliation(Profile profile) {
    VerifiedInstitutionalAffiliation verifiedInstitutionalAffiliation =
        profile.getVerifiedInstitutionalAffiliation();

    if (verifiedInstitutionalAffiliation == null) {
      throw new BadRequestException("Institutional affiliation cannot be empty");
    }

    Optional<DbInstitution> institution =
        institutionDao.findOneByShortName(
            verifiedInstitutionalAffiliation.getInstitutionShortName());
    if (!institution.isPresent()) {
      throw new NotFoundException(
          String.format(
              "Could not find institution %s in database",
              verifiedInstitutionalAffiliation.getInstitutionShortName()));
    }
    if (verifiedInstitutionalAffiliation.getInstitutionalRoleEnum() == null) {
      throw new BadRequestException("Institutional role cannot be empty");
    }
    if (verifiedInstitutionalAffiliation.getInstitutionalRoleEnum().equals(InstitutionalRole.OTHER)
        && (verifiedInstitutionalAffiliation.getInstitutionalRoleOtherText() == null
            || verifiedInstitutionalAffiliation.getInstitutionalRoleOtherText().equals(""))) {
      throw new BadRequestException(
          "Institutional role description cannot be empty when institutional role is set to Other");
    }

    String contactEmail = profile.getContactEmail();
    DbVerifiedInstitutionalAffiliation dbVerifiedAffiliation =
        verifiedInstitutionalAffiliationMapper.modelToDbWithoutUser(
            profile.getVerifiedInstitutionalAffiliation(), institutionService);
    if (!institutionService.validateAffiliation(dbVerifiedAffiliation, contactEmail)) {
      final String msg =
          Optional.ofNullable(dbVerifiedAffiliation)
              .map(
                  affiliation ->
                      String.format(
                          "Contact email %s is not a valid member of institution '%s'",
                          contactEmail, affiliation.getInstitution().getShortName()))
              .orElse(
                  String.format(
                      "Contact email %s does not have a valid institutional affiliation",
                      contactEmail));
      throw new BadRequestException(msg);
    }
  }

  public void updateProfileForUser(DbUser user, Profile updatedProfile, Profile previousProfile) {
    validateUpdatedProfile(updatedProfile, previousProfile);

    if (!userProvider.get().getGivenName().equalsIgnoreCase(updatedProfile.getGivenName())
        || !userProvider.get().getFamilyName().equalsIgnoreCase(updatedProfile.getFamilyName())) {
      userService.setDataUseAgreementNameOutOfDate(
          updatedProfile.getGivenName(), updatedProfile.getFamilyName());
    }

    Timestamp now = new Timestamp(clock.instant().toEpochMilli());

    user.setGivenName(updatedProfile.getGivenName());
    user.setFamilyName(updatedProfile.getFamilyName());
    user.setAreaOfResearch(updatedProfile.getAreaOfResearch());
    user.setProfessionalUrl(updatedProfile.getProfessionalUrl());
    user.setAddress(addressMapper.addressToDbAddress(updatedProfile.getAddress()));
    user.getAddress().setUser(user);
    DbDemographicSurvey dbDemographicSurvey =
        demographicSurveyMapper.demographicSurveyToDbDemographicSurvey(
            updatedProfile.getDemographicSurvey());

    if (user.getDemographicSurveyCompletionTime() == null && dbDemographicSurvey != null) {
      user.setDemographicSurveyCompletionTime(now);
    }

    if (dbDemographicSurvey != null && dbDemographicSurvey.getUser() == null) {
      dbDemographicSurvey.setUser(user);
    }

    user.setDemographicSurvey(dbDemographicSurvey);
    user.setLastModifiedTime(now);

    updateInstitutionalAffiliations(updatedProfile, user);
    boolean requireInstitutionalVerification =
        workbenchConfigProvider.get().featureFlags.requireInstitutionalVerification;
    if (requireInstitutionalVerification) {
      validateInstitutionalAffiliation(updatedProfile);
    }

    userService.updateUserWithConflictHandling(user);
    if (requireInstitutionalVerification) {
      DbVerifiedInstitutionalAffiliation updatedDbVerifiedAffiliation =
          verifiedInstitutionalAffiliationMapper.modelToDbWithoutUser(
              updatedProfile.getVerifiedInstitutionalAffiliation(), institutionService);
      updatedDbVerifiedAffiliation.setUser(user);
      Optional<DbVerifiedInstitutionalAffiliation> dbVerifiedAffiliation =
          verifiedInstitutionalAffiliationDao.findFirstByUser(user);
      dbVerifiedAffiliation.ifPresent(
          verifiedInstitutionalAffiliation ->
              updatedDbVerifiedAffiliation.setVerifiedInstitutionalAffiliationId(
                  verifiedInstitutionalAffiliation.getVerifiedInstitutionalAffiliationId()));
      this.verifiedInstitutionalAffiliationDao.save(updatedDbVerifiedAffiliation);
    }

    final Profile appliedUpdatedProfile = getProfile(user);
    profileAuditor.fireUpdateAction(previousProfile, appliedUpdatedProfile);
  }

  public void validateAndCleanProfile(Profile profile) throws BadRequestException {
    // Validation steps, which yield a BadRequestException if errors are found.
    String userName = profile.getUsername();
    if (userName == null || userName.length() < 3 || userName.length() > 64) {
      throw new BadRequestException(
          "Username should be at least 3 characters and not more than 64 characters");
    }
    validateStringLength(profile.getGivenName(), "Given Name", 80, 1);
    validateStringLength(profile.getFamilyName(), "Family Name", 80, 1);

    // Cleaning steps, which provide non-null fields or apply some cleanup / transformation.
    profile.setDemographicSurvey(
        Optional.ofNullable(profile.getDemographicSurvey()).orElse(new DemographicSurvey()));
    profile.setInstitutionalAffiliations(
        Optional.ofNullable(profile.getInstitutionalAffiliations()).orElse(new ArrayList<>()));
    // We always store the username as all lowercase.
    profile.setUsername(profile.getUsername().toLowerCase());
  }

  // Deprecated because it refers to old-style Institutional Affiliations, to be deleted in RW-4362
  // The new-style equivalent is VerifiedInstitutionalAffiliationMapper.modelToDbWithoutUser()
  @Deprecated
  private void updateInstitutionalAffiliations(Profile updatedProfile, DbUser user) {
    List<DbInstitutionalAffiliation> newAffiliations =
        updatedProfile.getInstitutionalAffiliations().stream()
            .map(institutionService::legacyInstitutionToDbInstitution)
            .collect(Collectors.toList());
    int i = 0;
    ListIterator<DbInstitutionalAffiliation> oldAffilations =
        user.getInstitutionalAffiliations().listIterator();
    boolean shouldAdd = false;
    if (newAffiliations.size() == 0) {
      shouldAdd = true;
    }
    for (DbInstitutionalAffiliation affiliation : newAffiliations) {
      affiliation.setOrderIndex(i);
      affiliation.setUser(user);
      if (oldAffilations.hasNext()) {
        DbInstitutionalAffiliation oldAffilation = oldAffilations.next();
        if (!oldAffilation.getRole().equals(affiliation.getRole())
            || !oldAffilation.getInstitution().equals(affiliation.getInstitution())) {
          shouldAdd = true;
        }
      } else {
        shouldAdd = true;
      }
      i++;
    }
    if (oldAffilations.hasNext()) {
      shouldAdd = true;
    }
    if (shouldAdd) {
      user.clearInstitutionalAffiliations();
      for (DbInstitutionalAffiliation affiliation : newAffiliations) {
        user.addInstitutionalAffiliation(affiliation);
      }
    }
  }

  private void validateUpdatedProfile(Profile updatedProfile, Profile prevProfile)
      throws BadRequestException {
    validateAndCleanProfile(updatedProfile);
    if (StringUtils.isEmpty(updatedProfile.getAreaOfResearch())) {
      throw new BadRequestException("Research background cannot be empty");
    }
    Optional.ofNullable(updatedProfile.getAddress())
        .orElseThrow(() -> new BadRequestException("Address must not be empty"));

    Address updatedProfileAddress = updatedProfile.getAddress();
    if (StringUtils.isEmpty(updatedProfileAddress.getStreetAddress1())
        || StringUtils.isEmpty(updatedProfileAddress.getCity())
        || StringUtils.isEmpty(updatedProfileAddress.getState())
        || StringUtils.isEmpty(updatedProfileAddress.getCountry())
        || StringUtils.isEmpty(updatedProfileAddress.getZipCode())) {
      throw new BadRequestException(
          "Address cannot have empty street Address 1/city/state/country or Zip Code");
    }
    if (updatedProfile.getContactEmail() != null
        && !updatedProfile.getContactEmail().equals(prevProfile.getContactEmail())) {
      // See RW-1488.
      throw new BadRequestException("Changing email is not currently supported");
    }
    if (updatedProfile.getUsername() != null
        && !updatedProfile.getUsername().equals(prevProfile.getUsername())) {
      // See RW-1488.
      throw new BadRequestException("Changing username is not supported");
    }
  }

  private void validateStringLength(String field, String fieldName, int max, int min) {
    if (field == null) {
      throw new BadRequestException(String.format("%s cannot be left blank!", fieldName));
    }
    if (field.length() > max) {
      throw new BadRequestException(
          String.format("%s length exceeds character limit. (%d)", fieldName, max));
    }
    if (field.length() < min) {
      if (min == 1) {
        throw new BadRequestException(String.format("%s cannot be left blank.", fieldName));
      } else {
        throw new BadRequestException(
            String.format("%s is under character minimum. (%d)", fieldName, min));
      }
    }
  }

  public List<Profile> listAllProfiles() {
    return userService.getAllUsers().stream().map(this::getProfile).collect(Collectors.toList());
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

  public void verifyInvitationKey(String invitationKey) {
    if (invitationKey == null
        || invitationKey.equals("")
        || !invitationKey.equals(cloudStorageService.readInvitationKey())) {
      throw new BadRequestException(
          "Missing or incorrect invitationKey (this API is not yet publicly launched)");
    }
  }

  public Profile createAccount(CreateAccountRequest request) {
    if (workbenchConfigProvider.get().captcha.enableCaptcha) {
      verifyCaptcha(request.getCaptchaVerificationToken());
    }

    if (workbenchConfigProvider.get().access.requireInvitationKey) {
      verifyInvitationKey(request.getInvitationKey());
    }

    if (workbenchConfigProvider.get().featureFlags.requireInstitutionalVerification) {
      validateInstitutionalAffiliation(request.getProfile());
    }

    final Profile profile = request.getProfile();

    // We don't include this check in validateAndCleanProfile since some existing user profiles
    // may have empty addresses. So we only check this on user creation, not update.
    Optional.ofNullable(profile.getAddress())
        .orElseThrow(() -> new BadRequestException("Address must not be empty"));

    validateAndCleanProfile(profile);

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
            addressMapper.addressToDbAddress(profile.getAddress()),
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

    return getProfile(user);
  }

  public DbUser initializeUserIfNeeded() {
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

  private DbUser saveUserWithConflictHandling(DbUser dbUser) {
    try {
      return userDao.save(dbUser);
    } catch (ObjectOptimisticLockingFailureException e) {
      log.log(Level.WARNING, "version conflict for user update", e);
      throw new ConflictException("Failed due to concurrent modification");
    }
  }

  public DbUser requestBetaAccess() {
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
    return user;
  }

  public DbUser submitDataUseAgreement(Integer dataUseAgreementSignedVersion, String initials) {
    DbUser user =
        userService.submitDataUseAgreement(
            userProvider.get(), dataUseAgreementSignedVersion, initials);
    return saveUserWithConflictHandling(user);
  }

  private void checkUserCreationNonce(DbUser user, String nonce) {
    if (Strings.isNullOrEmpty(nonce)) {
      throw new BadRequestException("missing required creationNonce");
    }
    if (user.getCreationNonce() == null || !nonce.equals(user.getCreationNonce().toString())) {
      throw new UnauthorizedException("invalid creationNonce provided");
    }
  }

  private boolean userHasEverLoggedIn(
      com.google.api.services.directory.model.User googleUser, DbUser user) {
    return user.getFirstSignInTime() != null || !googleUser.getChangePasswordAtNextLogin();
  }

  // TODO: should not be aware of ResponseEntity at the Service level
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

  // TODO: should not be aware of ResponseEntity at the Service level
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

  // TODO: should not be aware of ResponseEntity at the Service level
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

  public DbUser updatePageVisits(PageVisit newPageVisit) {
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
    return saveUserWithConflictHandling(dbUser);
  }

  public void updateProfile(Profile updatedProfile) {
    DbUser user = userProvider.get();

    // Save current profile for audit trail. Continue to use the userProvider (instead
    // of info on previousProfile) to ensure addition of audit system doesn't change behavior.
    // That is, in the (rare, hopefully) condition that the old profile gives incorrect information,
    // the update will still work as well as it would have.
    final Profile previousProfile = getProfile(user);
    final VerifiedInstitutionalAffiliation updatedAffil =
        updatedProfile.getVerifiedInstitutionalAffiliation();
    final VerifiedInstitutionalAffiliation prevAffil =
        previousProfile.getVerifiedInstitutionalAffiliation();
    if (!Objects.equals(updatedAffil, prevAffil)) {
      throw new BadRequestException("Cannot update Verified Institutional Affiliation");
    }

    updateProfileForUser(user, updatedProfile, previousProfile);
  }

  public void updateVerifiedInstitutionalAffiliation(
      Long userId, VerifiedInstitutionalAffiliation verifiedAffiliation) {

    DbUser dbUser = userDao.findUserByUserId(userId);
    Profile updatedProfile = getProfile(dbUser);

    if (verifiedAffiliation == null) {
      throw new BadRequestException("Cannot delete Verified Institutional Affiliation.");
    }

    Optional<Institution> institution =
        institutionService.getInstitution(verifiedAffiliation.getInstitutionShortName());
    institution.ifPresent(i -> verifiedAffiliation.setInstitutionDisplayName(i.getDisplayName()));

    updatedProfile.setVerifiedInstitutionalAffiliation(verifiedAffiliation);

    Profile oldProfile = getProfile(dbUser);
    updateProfileForUser(dbUser, updatedProfile, oldProfile);
  }

  public void deleteProfile() {
    if (!workbenchConfigProvider.get().featureFlags.unsafeAllowDeleteUser) {
      throw new ForbiddenException("Self account deletion is disallowed in this environment.");
    }

    DbUser user = userProvider.get();
    log.log(Level.WARNING, "Deleting profile: user email: " + user.getUsername());
    directoryService.deleteUser(user.getUsername());
    userDao.delete(user.getUserId());
    profileAuditor.fireDeleteAction(user.getUserId(), user.getUsername());
  }
}
