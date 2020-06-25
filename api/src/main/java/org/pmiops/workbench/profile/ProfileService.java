package org.pmiops.workbench.profile;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.apache.commons.lang3.StringUtils;
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
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final UserDao userDao;
  private final UserService userService;
  private final UserTermsOfServiceDao userTermsOfServiceDao;
  private final VerifiedInstitutionalAffiliationDao verifiedInstitutionalAffiliationDao;
  private final VerifiedInstitutionalAffiliationMapper verifiedInstitutionalAffiliationMapper;

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
      Provider<WorkbenchConfig> workbenchConfigProvider,
      UserDao userDao,
      UserService userService,
      UserTermsOfServiceDao userTermsOfServiceDao,
      VerifiedInstitutionalAffiliationDao verifiedInstitutionalAffiliationDao,
      VerifiedInstitutionalAffiliationMapper verifiedInstitutionalAffiliationMapper) {
    this.addressMapper = addressMapper;
    this.clock = clock;
    this.demographicSurveyMapper = demographicSurveyMapper;
    this.freeTierBillingService = freeTierBillingService;
    this.institutionDao = institutionDao;
    this.institutionService = institutionService;
    this.profileAuditor = profileAuditor;
    this.profileMapper = profileMapper;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.userDao = userDao;
    this.userService = userService;
    this.userTermsOfServiceDao = userTermsOfServiceDao;
    this.verifiedInstitutionalAffiliationDao = verifiedInstitutionalAffiliationDao;
    this.verifiedInstitutionalAffiliationMapper = verifiedInstitutionalAffiliationMapper;
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

    boolean requireInstitutionalVerification =
        workbenchConfigProvider.get().featureFlags.requireInstitutionalVerification;
    if (requireInstitutionalVerification) {
      validateInstitutionalAffiliation(updatedProfile);
    } else {
      updateInstitutionalAffiliations(updatedProfile, user);
    }

    user = userService.updateUserWithConflictHandling(user);

    if (requireInstitutionalVerification) {
      saveVerifiedInstitutionalAffiliation(user, updatedProfile);
    }

    final Profile appliedUpdatedProfile = getProfile(user);
    profileAuditor.fireUpdateAction(previousProfile, appliedUpdatedProfile);
  }

  public void saveVerifiedInstitutionalAffiliation(DbUser user, Profile updatedProfile) {
    DbVerifiedInstitutionalAffiliation updatedDbVerifiedAffiliation =
        verifiedInstitutionalAffiliationMapper.modelToDbWithoutUser(
            updatedProfile.getVerifiedInstitutionalAffiliation(), institutionService);

    updatedDbVerifiedAffiliation.setUser(user);

    // set DB ID if present, to cause an update
    verifiedInstitutionalAffiliationDao
        .findFirstByUser(user)
        .map(DbVerifiedInstitutionalAffiliation::getVerifiedInstitutionalAffiliationId)
        .ifPresent(updatedDbVerifiedAffiliation::setVerifiedInstitutionalAffiliationId);

    this.verifiedInstitutionalAffiliationDao.save(updatedDbVerifiedAffiliation);
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

  public void validateUpdatedProfile(Profile updatedProfile, Profile prevProfile)
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

  public void adminUpdateProfile(Profile updatedProfile) {
    final int optimisticLockingVersion =
        userDao.findUserByUserId(updatedProfile.getUserId()).getVersion();

    final DbUser user = profileMapper.profileToDbUser(updatedProfile);
    user.setVersion(optimisticLockingVersion);
    user.setLastModifiedTime(new Timestamp(clock.instant().toEpochMilli()));
    userDao.save(user);
  }
}
