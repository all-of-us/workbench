package org.pmiops.workbench.profile;

import com.google.common.annotations.VisibleForTesting;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Provider;
import org.apache.commons.lang3.StringUtils;
import org.javers.core.Javers;
import org.javers.core.diff.Change;
import org.javers.core.diff.Diff;
import org.javers.core.diff.changetype.NewObject;
import org.javers.core.diff.changetype.PropertyChange;
import org.pmiops.workbench.access.AccessTierService;
import org.pmiops.workbench.actionaudit.auditors.ProfileAuditor;
import org.pmiops.workbench.billing.FreeTierBillingService;
import org.pmiops.workbench.db.dao.InstitutionDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.dao.UserTermsOfServiceDao;
import org.pmiops.workbench.db.dao.VerifiedInstitutionalAffiliationDao;
import org.pmiops.workbench.db.model.DbDemographicSurvey;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserTermsOfService;
import org.pmiops.workbench.db.model.DbVerifiedInstitutionalAffiliation;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.institution.InstitutionService;
import org.pmiops.workbench.institution.VerifiedInstitutionalAffiliationMapper;
import org.pmiops.workbench.model.AccountPropertyUpdate;
import org.pmiops.workbench.model.Address;
import org.pmiops.workbench.model.AdminTableUser;
import org.pmiops.workbench.model.Authority;
import org.pmiops.workbench.model.DemographicSurvey;
import org.pmiops.workbench.model.InstitutionalRole;
import org.pmiops.workbench.model.Profile;
import org.pmiops.workbench.model.ProfileRenewableAccessModules;
import org.pmiops.workbench.model.RenewableAccessModuleStatus;
import org.pmiops.workbench.model.VerifiedInstitutionalAffiliation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProfileService {

  private static final Logger log = Logger.getLogger(ProfileService.class.getName());

  private final AccessTierService accessTierService;
  private final AddressMapper addressMapper;
  private final Clock clock;
  private final DemographicSurveyMapper demographicSurveyMapper;
  private final FreeTierBillingService freeTierBillingService;
  private final InstitutionDao institutionDao;
  private final InstitutionService institutionService;
  private final Javers javers;
  private final ProfileAuditor profileAuditor;
  private final ProfileMapper profileMapper;
  private final Provider<DbUser> userProvider;
  private final UserDao userDao;
  private final UserService userService;
  private final UserTermsOfServiceDao userTermsOfServiceDao;
  private final VerifiedInstitutionalAffiliationDao verifiedInstitutionalAffiliationDao;
  private final VerifiedInstitutionalAffiliationMapper verifiedInstitutionalAffiliationMapper;

  @Autowired
  public ProfileService(
      AccessTierService accessTierService,
      AddressMapper addressMapper,
      Clock clock,
      DemographicSurveyMapper demographicSurveyMapper,
      FreeTierBillingService freeTierBillingService,
      InstitutionDao institutionDao,
      InstitutionService institutionService,
      Javers javers,
      ProfileAuditor profileAuditor,
      ProfileMapper profileMapper,
      Provider<DbUser> userProvider,
      UserDao userDao,
      UserService userService,
      UserTermsOfServiceDao userTermsOfServiceDao,
      VerifiedInstitutionalAffiliationDao verifiedInstitutionalAffiliationDao,
      VerifiedInstitutionalAffiliationMapper verifiedInstitutionalAffiliationMapper) {
    this.accessTierService = accessTierService;
    this.addressMapper = addressMapper;
    this.clock = clock;
    this.demographicSurveyMapper = demographicSurveyMapper;
    this.freeTierBillingService = freeTierBillingService;
    this.institutionDao = institutionDao;
    this.institutionService = institutionService;
    this.javers = javers;
    this.profileAuditor = profileAuditor;
    this.profileMapper = profileMapper;
    this.userProvider = userProvider;
    this.userDao = userDao;
    this.userService = userService;
    this.userTermsOfServiceDao = userTermsOfServiceDao;
    this.verifiedInstitutionalAffiliationDao = verifiedInstitutionalAffiliationDao;
    this.verifiedInstitutionalAffiliationMapper = verifiedInstitutionalAffiliationMapper;
  }

  // TODO: avoid all these separate queries by appropriate ORM mappings
  public Profile getProfile(DbUser userLite) {
    // Fetch the user's authorities, since they aren't loaded during normal request interception.
    final DbUser user =
        userService.findUserWithAuthoritiesAndPageVisits(userLite.getUserId()).orElse(userLite);

    final @Nullable Double freeTierUsage = freeTierBillingService.getCachedFreeTierUsage(user);
    final @Nullable Double freeTierDollarQuota =
        freeTierBillingService.getUserFreeTierDollarLimit(user);
    final @Nullable VerifiedInstitutionalAffiliation verifiedInstitutionalAffiliation =
        verifiedInstitutionalAffiliationDao
            .findFirstByUser(user)
            .map(verifiedInstitutionalAffiliationMapper::dbToModel)
            .orElse(null);

    final @Nullable DbUserTermsOfService latestTermsOfService =
        userTermsOfServiceDao.findFirstByUserIdOrderByTosVersionDesc(user.getUserId()).orElse(null);

    final List<String> accessTierShortNames =
        accessTierService.getAccessTierShortNamesForUser(user);

    final List<RenewableAccessModuleStatus> modulesStatus =
        userService.getRenewableAccessModuleStatus(userLite);

    final ProfileRenewableAccessModules renewableAccessModules =
        new ProfileRenewableAccessModules()
            .modules(modulesStatus)
            .anyModuleHasExpired(modulesStatus.stream().anyMatch(x -> x.getHasExpired()));

    final Timestamp profileLastConfirmedTime = user.getProfileLastConfirmedTime();

    final Timestamp publicationsLastConfirmedTime = user.getPublicationsLastConfirmedTime();

    return profileMapper.toModel(
        user,
        verifiedInstitutionalAffiliation,
        latestTermsOfService,
        freeTierUsage,
        freeTierDollarQuota,
        accessTierShortNames,
        renewableAccessModules);
  }

  public void validateAffiliation(Profile profile) {
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

    validateAffiliationEmail(profile);
  }

  private void validateAffiliationEmail(Profile profile) {
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

  /**
   * Updates a profile for a given user and persists all information to the database.
   *
   * @param user
   * @param updatedProfile
   * @param previousProfile
   */
  public void updateProfile(DbUser user, Profile updatedProfile, Profile previousProfile) {
    // Apply cleaning methods to both the previous and updated profile, to avoid false positive
    // field diffs due to null-to-empty-object changes.
    cleanProfile(updatedProfile);
    cleanProfile(previousProfile);
    validateProfile(updatedProfile, previousProfile);

    if (!user.getGivenName().equalsIgnoreCase(updatedProfile.getGivenName())
        || !user.getFamilyName().equalsIgnoreCase(updatedProfile.getFamilyName())) {
      userService.setDataUseAgreementNameOutOfDate(
          updatedProfile.getGivenName(), updatedProfile.getFamilyName());
    }

    Timestamp now = new Timestamp(clock.instant().toEpochMilli());

    user.setContactEmail(updatedProfile.getContactEmail());
    user.setGivenName(updatedProfile.getGivenName());
    user.setFamilyName(updatedProfile.getFamilyName());
    user.setAreaOfResearch(updatedProfile.getAreaOfResearch());
    user.setProfessionalUrl(updatedProfile.getProfessionalUrl());
    user.setAddress(addressMapper.addressToDbAddress(updatedProfile.getAddress()));
    // Address may be null for users who were created before address validation was in place. See
    // RW-5139.
    Optional.ofNullable(user.getAddress()).ifPresent(address -> address.setUser(user));

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
    userService.updateUserWithConflictHandling(user);

    // FIXME: why not do a getOrCreateAffiliation() here and then update that object & save?

    // Save the verified institutional affiliation in the DB. The affiliation has already been
    // verified as part of the `validateProfile` call.
    DbVerifiedInstitutionalAffiliation newAffiliation =
        verifiedInstitutionalAffiliationMapper.modelToDbWithoutUser(
            updatedProfile.getVerifiedInstitutionalAffiliation(), institutionService);
    newAffiliation.setUser(
        user); // Why are we calling a non-user mapping function and then adding a user?

    // This is 1:1 in terms of our current usage, but not modelled that way (aside from the unique
    // constraint on user_id).

    // If this user already has an affiliation, replace the ID of hte
    verifiedInstitutionalAffiliationDao
        .findFirstByUser(user)
        .map(DbVerifiedInstitutionalAffiliation::getVerifiedInstitutionalAffiliationId)
        .ifPresent(newAffiliation::setVerifiedInstitutionalAffiliationId);
    this.verifiedInstitutionalAffiliationDao.save(newAffiliation);

    final Profile appliedUpdatedProfile = getProfile(user);

    userService.confirmProfile();
    profileAuditor.fireUpdateAction(previousProfile, appliedUpdatedProfile);
  }

  public void cleanProfile(Profile profile) {
    // Cleaning steps, which provide non-null fields or apply some cleanup / transformation.
    profile.setDemographicSurvey(
        Optional.ofNullable(profile.getDemographicSurvey()).orElse(new DemographicSurvey()));
    if (profile.getUsername() != null) {
      // We always store the username as all lowercase.
      profile.setUsername(profile.getUsername().toLowerCase());
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

  private void validateUsername(Profile profile) throws BadRequestException {
    String username = profile.getUsername();
    if (username == null || username.length() < 3 || username.length() > 64) {
      throw new BadRequestException(
          "Username should be at least 3 characters and not more than 64 characters");
    }
  }

  private void validateContactEmail(Profile profile) throws BadRequestException {
    if (StringUtils.isEmpty(profile.getContactEmail())) {
      throw new BadRequestException("Contact email cannot be empty");
    }
  }

  private void validateGivenName(Profile profile) throws BadRequestException {
    validateStringLength(profile.getGivenName(), "Given Name", 80, 1);
  }

  private void validateFamilyName(Profile profile) throws BadRequestException {
    validateStringLength(profile.getFamilyName(), "Family Name", 80, 1);
  }

  private void validateAddress(Profile profile) throws BadRequestException {
    Optional.ofNullable(profile.getAddress())
        .orElseThrow(() -> new BadRequestException("Address must not be empty"));

    Address updatedProfileAddress = profile.getAddress();
    if (StringUtils.isEmpty(updatedProfileAddress.getStreetAddress1())
        || StringUtils.isEmpty(updatedProfileAddress.getCity())
        || StringUtils.isEmpty(updatedProfileAddress.getState())
        || StringUtils.isEmpty(updatedProfileAddress.getCountry())
        || StringUtils.isEmpty(updatedProfileAddress.getZipCode())) {
      throw new BadRequestException(
          "Address cannot have empty street address, city, state, country or zip code");
    }
  }

  private void validateAreaOfResearch(Profile profile) throws BadRequestException {
    if (StringUtils.isEmpty(profile.getAreaOfResearch())) {
      throw new BadRequestException("Research background cannot be empty");
    }
  }

  /**
   * Returns a list of PropertyChange entries from a diff that match either a field or any of its
   * subfields. For example, a pathPrefix of 'address' will match a field change for both 'address'
   * and for 'address.zipCode'.
   */
  private List<Change> getChangesWithPrefix(final Diff diff, final String pathPrefix) {
    return diff.getChanges(
        change ->
            change instanceof PropertyChange
                && ((PropertyChange) change).getPropertyNameWithPath().startsWith(pathPrefix));
  }

  /**
   * Has this Profile field changed?
   *
   * @param diff a Diff between two Profiles
   * @param field which field to check
   * @return true if there are difference between the Profiles
   */
  private boolean fieldChanged(Diff diff, String field) {
    return getChangesWithPrefix(diff, field).size() > 0;
  }

  /**
   * Is this a new Profile?
   *
   * @param diff a Diff between two profiles
   * @return true if the Profile is new
   */
  private boolean isNewProfile(Diff diff) {

    return diff.getChanges(
                change ->
                    change instanceof NewObject
                        && change
                            .getAffectedGlobalId()
                            .getTypeName()
                            .equals(Profile.class.getCanonicalName()))
            .size()
        > 0;
  }

  /**
   * Validates a set of Profile changes by comparing the updated profile to the previous version.
   * Only fields that have changed are subject to validation.
   *
   * <p>If the previous version is null, the updated profile is presumed to be a new profile and all
   * validation rules are run. If both versions are non-null, only changed fields are validated.
   *
   * <p>This method should only be called after calling `cleanProfile` on the updated Profile
   * object.
   *
   * @param updatedProfile
   * @param previousProfile
   * @throws BadRequestException
   */
  @VisibleForTesting
  public void validateProfile(@Nonnull Profile updatedProfile, @Nullable Profile previousProfile) {
    final Diff diff = javers.compare(previousProfile, updatedProfile);

    validateProfileForCorrectness(diff, updatedProfile);

    if (userService.hasAuthority(userProvider.get().getUserId(), Authority.ACCESS_CONTROL_ADMIN)) {
      validateChangesAllowedByAdmin(diff);
    } else {
      validateChangesAllowedByUser(diff);
    }
  }

  private void validateProfileForCorrectness(
      @Nullable Profile previousProfile, @Nonnull Profile profile) throws BadRequestException {

    final Diff diff = javers.compare(previousProfile, profile);
    validateProfileForCorrectness(diff, profile);
  }

  private void validateProfileForCorrectness(Diff diff, @Nonnull Profile profile)
      throws BadRequestException {

    if (isNewProfile(diff) || fieldChanged(diff, "username")) {
      validateUsername(profile);
    }
    if (isNewProfile(diff) || fieldChanged(diff, "contactEmail")) {
      validateContactEmail(profile);

      // only validate if the new profile has an affiliation - some older users do not
      if (profile.getVerifiedInstitutionalAffiliation() != null) {
        validateAffiliationEmail(profile);
      }
    }
    if (isNewProfile(diff) || fieldChanged(diff, "givenName")) {
      validateGivenName(profile);
    }
    if (isNewProfile(diff) || fieldChanged(diff, "familyName")) {
      validateFamilyName(profile);
    }
    if (isNewProfile(diff) || fieldChanged(diff, "address")) {
      validateAddress(profile);
    }
    if (isNewProfile(diff) || fieldChanged(diff, "areaOfResearch")) {
      validateAreaOfResearch(profile);
    }
    if (fieldChanged(diff, "verifiedInstitutionalAffiliation")) {
      validateAffiliation(profile);
    }
  }

  private void validateChangesAllowedByUser(Diff diff) {
    if (fieldChanged(diff, "username")) {
      // See RW-1488.
      throw new BadRequestException("Changing username is not supported");
    }
    if (fieldChanged(diff, "contactEmail")) {
      // See RW-1488.
      throw new BadRequestException("Changing contact email is not currently supported");
    }
    if (fieldChanged(diff, "verifiedInstitutionalAffiliation")) {
      throw new BadRequestException("Changing Verified Institutional Affiliation is not supported");
    }
  }

  private void validateChangesAllowedByAdmin(Diff diff) {
    if (fieldChanged(diff, "username")) {
      // See RW-1488.
      throw new BadRequestException("Changing username is not supported");
    }
  }

  /**
   * Validates a new Profile being created, by running all validation checks.
   *
   * @param profile
   * @throws BadRequestException
   */
  public void validateNewProfile(Profile profile) throws BadRequestException {
    final Profile dummyProfile = null;
    validateProfileForCorrectness(dummyProfile, profile);
  }

  public List<AdminTableUser> getAdminTableUsers() {
    return profileMapper.adminViewToModel(userDao.getAdminTableUsers());
  }

  /**
   * Updates the user metadata referenced by the fields of AccountPropertyUpdate.
   *
   * @param request the fields to update. Fields left null here will not be updated. Contact Email
   *     and Verified Institutional Affiliation updates will trigger a check for affiliation
   *     validation.
   * @return the Profile of the user, after updates
   */
  public Profile updateAccountProperties(AccountPropertyUpdate request) {
    final DbUser dbUser = userService.getByUsernameOrThrow(request.getUsername());
    final Profile originalProfile = getProfile(dbUser);

    Optional.ofNullable(request.getFreeCreditsLimit())
        .ifPresent(
            newLimit -> freeTierBillingService.maybeSetDollarLimitOverride(dbUser, newLimit));

    request
        .getAccessBypassRequests()
        .forEach(bypass -> userService.updateBypassTime(dbUser.getUserId(), bypass));

    // refetch from the DB
    Profile updatedProfile = getProfile(userService.getByUsernameOrThrow(request.getUsername()));
    Optional.ofNullable(request.getContactEmail()).ifPresent(updatedProfile::setContactEmail);
    Optional.ofNullable(request.getAffiliation())
        .ifPresent(updatedProfile::setVerifiedInstitutionalAffiliation);

    updateProfile(dbUser, updatedProfile, originalProfile);

    return getProfile(dbUser);
  }
}
