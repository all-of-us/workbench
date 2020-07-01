package org.pmiops.workbench.profile;

import com.google.common.annotations.VisibleForTesting;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Provider;
import org.apache.commons.lang3.StringUtils;
import org.javers.core.Javers;
import org.javers.core.diff.Change;
import org.javers.core.diff.Diff;
import org.javers.core.diff.changetype.PropertyChange;
import org.pmiops.workbench.actionaudit.auditors.ProfileAuditor;
import org.pmiops.workbench.billing.FreeTierBillingService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.InstitutionDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.dao.UserTermsOfServiceDao;
import org.pmiops.workbench.db.dao.VerifiedInstitutionalAffiliationDao;
import org.pmiops.workbench.db.model.DbDemographicSurvey;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbInstitutionalAffiliation;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserTermsOfService;
import org.pmiops.workbench.db.model.DbVerifiedInstitutionalAffiliation;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.institution.InstitutionService;
import org.pmiops.workbench.institution.VerifiedInstitutionalAffiliationMapper;
import org.pmiops.workbench.model.Address;
import org.pmiops.workbench.model.DemographicSurvey;
import org.pmiops.workbench.model.InstitutionalRole;
import org.pmiops.workbench.model.Profile;
import org.pmiops.workbench.model.VerifiedInstitutionalAffiliation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProfileService {

  private final AddressMapper addressMapper;
  private final Clock clock;
  private final DemographicSurveyMapper demographicSurveyMapper;
  private final FreeTierBillingService freeTierBillingService;
  private final InstitutionDao institutionDao;
  private final InstitutionService institutionService;
  private final ProfileAuditor profileAuditor;
  private final ProfileMapper profileMapper;
  private final Provider<DbUser> userProvider;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final UserDao userDao;
  private final UserService userService;
  private final UserTermsOfServiceDao userTermsOfServiceDao;
  private final VerifiedInstitutionalAffiliationDao verifiedInstitutionalAffiliationDao;
  private final VerifiedInstitutionalAffiliationMapper verifiedInstitutionalAffiliationMapper;
  private final Javers javers;

  @Autowired
  public ProfileService(
      AddressMapper addressMapper,
      Clock clock,
      DemographicSurveyMapper demographicSurveyMapper,
      FreeTierBillingService freeTierBillingService,
      InstitutionDao institutionDao,
      InstitutionService institutionService,
      ProfileAuditor profileAuditor,
      ProfileMapper profileMapper,
      Provider<DbUser> userProvider,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      UserDao userDao,
      UserService userService,
      UserTermsOfServiceDao userTermsOfServiceDao,
      VerifiedInstitutionalAffiliationDao verifiedInstitutionalAffiliationDao,
      VerifiedInstitutionalAffiliationMapper verifiedInstitutionalAffiliationMapper,
      Javers javers) {
    this.addressMapper = addressMapper;
    this.clock = clock;
    this.demographicSurveyMapper = demographicSurveyMapper;
    this.freeTierBillingService = freeTierBillingService;
    this.institutionDao = institutionDao;
    this.institutionService = institutionService;
    this.profileAuditor = profileAuditor;
    this.profileMapper = profileMapper;
    this.userProvider = userProvider;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.userDao = userDao;
    this.userService = userService;
    this.userTermsOfServiceDao = userTermsOfServiceDao;
    this.verifiedInstitutionalAffiliationDao = verifiedInstitutionalAffiliationDao;
    this.verifiedInstitutionalAffiliationMapper = verifiedInstitutionalAffiliationMapper;
    this.javers = javers;
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
    if (!workbenchConfigProvider.get().featureFlags.requireInstitutionalVerification) {
      return;
    }

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

  /**
   * Updates a profile for a given user and persists all information to the database.
   *
   * @param user
   * @param updatedProfile
   * @param previousProfile
   */
  public void updateProfileForUser(DbUser user, Profile updatedProfile, Profile previousProfile) {
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

    user.setLastModifiedTime(now);

    updateInstitutionalAffiliations(updatedProfile, user);

    userService.updateUserWithConflictHandling(user);

    if (workbenchConfigProvider.get().featureFlags.requireInstitutionalVerification) {
      // Save the verified institutional affiliation in the DB. The affiliation has already been
      // verified as part of the `validateProfile` call.
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

  public void cleanProfile(Profile profile) {
    // Cleaning steps, which provide non-null fields or apply some cleanup / transformation.
    profile.setDemographicSurvey(
        Optional.ofNullable(profile.getDemographicSurvey()).orElse(new DemographicSurvey()));
    profile.setInstitutionalAffiliations(
        Optional.ofNullable(profile.getInstitutionalAffiliations()).orElse(new ArrayList<>()));
    if (profile.getUsername() != null) {
      // We always store the username as all lowercase.
      profile.setUsername(profile.getUsername().toLowerCase());
    }
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
   * @param prevProfile
   * @throws BadRequestException
   */
  @VisibleForTesting
  public void validateProfile(@Nonnull Profile updatedProfile, @Nullable Profile prevProfile)
      throws BadRequestException {
    boolean isNewObject = prevProfile == null;
    Diff diff = javers.compare(prevProfile, updatedProfile);

    if (!getChangesWithPrefix(diff, "username").isEmpty() || isNewObject) {
      validateUsername(updatedProfile);
    }
    if (!getChangesWithPrefix(diff, "contactEmail").isEmpty() || isNewObject) {
      validateContactEmail(updatedProfile);
    }
    if (!getChangesWithPrefix(diff, "givenName").isEmpty() || isNewObject) {
      validateGivenName(updatedProfile);
    }
    if (!getChangesWithPrefix(diff, "familyName").isEmpty() || isNewObject) {
      validateFamilyName(updatedProfile);
    }
    if (!getChangesWithPrefix(diff, "address").isEmpty() || isNewObject) {
      validateAddress(updatedProfile);
    }
    if (!getChangesWithPrefix(diff, "areaOfResearch").isEmpty() || isNewObject) {
      validateAreaOfResearch(updatedProfile);
    }
    if (!getChangesWithPrefix(diff, "verifiedInstitutionalAffiliation").isEmpty() || isNewObject) {
      validateInstitutionalAffiliation(updatedProfile);
    }

    if (!isNewObject) {
      // We disallow changes in certain fields.
      if (!getChangesWithPrefix(diff, "username").isEmpty()) {
        // See RW-1488.
        throw new BadRequestException("Changing username is not supported");
      }
      if (!getChangesWithPrefix(diff, "contactEmail").isEmpty()) {
        // See RW-1488.
        throw new BadRequestException("Changing contact email is not currently supported");
      }
    }
  }

  /**
   * Validates a new Profile being created, by running all validation checks.
   *
   * @param profile
   * @throws BadRequestException
   */
  public void validateNewProfile(Profile profile) throws BadRequestException {
    validateProfile(profile, null);
  }

  public List<Profile> listAllProfiles() {
    return userService.getAllUsers().stream().map(this::getProfile).collect(Collectors.toList());
  }
}
