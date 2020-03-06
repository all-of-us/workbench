package org.pmiops.workbench.profile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.pmiops.workbench.billing.FreeTierBillingService;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserTermsOfServiceDao;
import org.pmiops.workbench.db.dao.VerifiedInstitutionalAffiliationDao;
import org.pmiops.workbench.db.model.DbAddress;
import org.pmiops.workbench.db.model.DbDemographicSurvey;
import org.pmiops.workbench.db.model.DbInstitutionalAffiliation;
import org.pmiops.workbench.db.model.DbPageVisit;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserTermsOfService;
import org.pmiops.workbench.institution.VerifiedInstitutionalAffiliationMapper;
import org.pmiops.workbench.model.Address;
import org.pmiops.workbench.model.DemographicSurvey;
import org.pmiops.workbench.model.Disability;
import org.pmiops.workbench.model.InstitutionalAffiliation;
import org.pmiops.workbench.model.PageVisit;
import org.pmiops.workbench.model.Profile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProfileService {
  private static final Function<DbInstitutionalAffiliation, InstitutionalAffiliation>
      TO_CLIENT_INSTITUTIONAL_AFFILIATION =
          new Function<DbInstitutionalAffiliation, InstitutionalAffiliation>() {
            @Override
            public InstitutionalAffiliation apply(
                DbInstitutionalAffiliation institutionalAffiliation) {
              InstitutionalAffiliation result = new InstitutionalAffiliation();
              result.setRole(institutionalAffiliation.getRole());
              result.setInstitution(institutionalAffiliation.getInstitution());

              return result;
            }
          };

  private static final Function<DbPageVisit, PageVisit> TO_CLIENT_PAGE_VISIT =
      new Function<DbPageVisit, PageVisit>() {
        @Override
        public PageVisit apply(DbPageVisit pageVisit) {
          PageVisit result = new PageVisit();
          result.setPage(pageVisit.getPageId());
          result.setFirstVisit(pageVisit.getFirstVisit().getTime());
          return result;
        }
      };

  private static final Function<DbDemographicSurvey, DemographicSurvey>
      TO_CLIENT_DEMOGRAPHIC_SURVEY =
          new Function<DbDemographicSurvey, DemographicSurvey>() {
            @Override
            public DemographicSurvey apply(DbDemographicSurvey demographicSurvey) {
              DemographicSurvey result = new DemographicSurvey();
              if (demographicSurvey.getDisability() != null) {
                result.setDisability(Disability.TRUE.equals(demographicSurvey.getDisabilityEnum()));
              }
              result.setEducation(demographicSurvey.getEducationEnum());
              result.setEthnicity(demographicSurvey.getEthnicityEnum());
              result.setIdentifiesAsLgbtq(demographicSurvey.getIdentifiesAsLgbtq());
              result.setLgbtqIdentity(demographicSurvey.getLgbtqIdentity());
              result.setRace(demographicSurvey.getRaceEnum());
              result.setGenderIdentityList(demographicSurvey.getGenderIdentityEnumList());
              result.setSexAtBirth(demographicSurvey.getSexAtBirthEnum());
              result.setYearOfBirth(BigDecimal.valueOf(demographicSurvey.getYear_of_birth()));

              return result;
            }
          };

  private static final Function<DbAddress, Address> TO_CLIENT_ADDRESS_SURVEY =
      new Function<DbAddress, Address>() {
        @Override
        public Address apply(DbAddress address) {
          Address result = new Address();
          if (address != null) {
            result.setStreetAddress1(address.getStreetAddress1());
            result.setStreetAddress2(address.getStreetAddress2());
            result.setCity(address.getCity());
            result.setState(address.getState());
            result.setCountry(address.getCountry());
            result.setZipCode(address.getZipCode());
            return result;
          }
          return result;
        }
      };

  private final FreeTierBillingService freeTierBillingService;
  private final ProfileMapper profileMapper;
  private final UserDao userDao;
  private final UserTermsOfServiceDao userTermsOfServiceDao;
  private final VerifiedInstitutionalAffiliationDao verifiedInstitutionalAffiliationDao;
  private final VerifiedInstitutionalAffiliationMapper verifiedInstitutionalAffiliationMapper;

  @Autowired
  public ProfileService(
      FreeTierBillingService freeTierBillingService,
      ProfileMapper profileMapper,
      UserDao userDao,
      UserTermsOfServiceDao userTermsOfServiceDao,
      VerifiedInstitutionalAffiliationDao verifiedInstitutionalAffiliationDao,
      VerifiedInstitutionalAffiliationMapper verifiedInstitutionalAffiliationMapper) {
    this.freeTierBillingService = freeTierBillingService;
    this.profileMapper = profileMapper;
    this.userDao = userDao;
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
    profile.setInstitutionalAffiliations(
        user.getInstitutionalAffiliations().stream()
            .map(TO_CLIENT_INSTITUTIONAL_AFFILIATION)
            .collect(Collectors.toList()));

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
}
