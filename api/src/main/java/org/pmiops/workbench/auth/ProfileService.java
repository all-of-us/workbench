package org.pmiops.workbench.auth;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.pmiops.workbench.billing.FreeTierBillingService;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.DbAddress;
import org.pmiops.workbench.db.model.DbDemographicSurvey;
import org.pmiops.workbench.db.model.DbInstitutionalAffiliation;
import org.pmiops.workbench.db.model.DbPageVisit;
import org.pmiops.workbench.db.model.DbUser;
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
              if (result.getDisability() != null)
                result.setDisability(demographicSurvey.getDisabilityEnum().equals(Disability.TRUE));
              result.setEducation(demographicSurvey.getEducationEnum());
              result.setEthnicity(demographicSurvey.getEthnicityEnum());
              result.setGender(demographicSurvey.getGenderEnum());
              result.setRace(demographicSurvey.getRaceEnum());
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

  private final UserDao userDao;
  private final FreeTierBillingService freeTierBillingService;

  @Autowired
  public ProfileService(UserDao userDao, FreeTierBillingService freeTierBillingService) {
    this.userDao = userDao;
    this.freeTierBillingService = freeTierBillingService;
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

    Profile profile = new Profile();
    profile.setUserId(user.getUserId());
    profile.setUsername(user.getUsername());
    if (user.getCreationNonce() != null) {
      profile.setCreationNonce(user.getCreationNonce().toString());
    }
    profile.setFamilyName(user.getFamilyName());
    profile.setGivenName(user.getGivenName());
    profile.setOrganization(user.getOrganization());
    profile.setCurrentPosition(user.getCurrentPosition());
    profile.setContactEmail(user.getContactEmail());
    profile.setPhoneNumber(user.getPhoneNumber());
    profile.setAboutYou(user.getAboutYou());
    profile.setAreaOfResearch(user.getAreaOfResearch());
    profile.setDisabled(user.getDisabled());
    profile.setEraCommonsLinkedNihUsername(user.getEraCommonsLinkedNihUsername());

    if (user.getComplianceTrainingCompletionTime() != null) {
      profile.setComplianceTrainingCompletionTime(
          user.getComplianceTrainingCompletionTime().getTime());
    }
    if (user.getComplianceTrainingBypassTime() != null) {
      profile.setComplianceTrainingBypassTime(user.getComplianceTrainingBypassTime().getTime());
    }
    if (user.getEraCommonsLinkExpireTime() != null) {
      profile.setEraCommonsLinkExpireTime(user.getEraCommonsLinkExpireTime().getTime());
    }
    if (user.getEraCommonsCompletionTime() != null) {
      profile.setEraCommonsCompletionTime(user.getEraCommonsCompletionTime().getTime());
    }
    if (user.getEraCommonsBypassTime() != null) {
      profile.setEraCommonsBypassTime(user.getEraCommonsBypassTime().getTime());
    }
    if (user.getDemographicSurveyCompletionTime() != null) {
      profile.setDemographicSurveyCompletionTime(
          user.getDemographicSurveyCompletionTime().getTime());
    }
    if (user.getFirstSignInTime() != null) {
      profile.setFirstSignInTime(user.getFirstSignInTime().getTime());
    }
    if (user.getIdVerificationBypassTime() != null) {
      profile.setIdVerificationBypassTime(user.getIdVerificationBypassTime().getTime());
    }
    if (user.getIdVerificationCompletionTime() != null) {
      profile.setIdVerificationCompletionTime(user.getIdVerificationCompletionTime().getTime());
    }
    if (user.getDataAccessLevelEnum() != null) {
      profile.setDataAccessLevel(user.getDataAccessLevelEnum());
    }
    if (user.getBetaAccessBypassTime() != null) {
      profile.setBetaAccessBypassTime(user.getBetaAccessBypassTime().getTime());
    }
    if (user.getBetaAccessRequestTime() != null) {
      profile.setBetaAccessRequestTime(user.getBetaAccessRequestTime().getTime());
    }
    if (user.getEmailVerificationCompletionTime() != null) {
      profile.setEmailVerificationCompletionTime(
          user.getEmailVerificationCompletionTime().getTime());
    }
    if (user.getEmailVerificationBypassTime() != null) {
      profile.setEmailVerificationBypassTime(user.getEmailVerificationBypassTime().getTime());
    }
    if (user.getDataUseAgreementCompletionTime() != null) {
      profile.setDataUseAgreementCompletionTime(user.getDataUseAgreementCompletionTime().getTime());
    }
    if (user.getDataUseAgreementBypassTime() != null) {
      profile.setDataUseAgreementBypassTime(user.getDataUseAgreementBypassTime().getTime());
    }
    if (user.getDataUseAgreementSignedVersion() != null) {
      profile.setDataUseAgreementSignedVersion(user.getDataUseAgreementSignedVersion());
    }
    if (user.getTwoFactorAuthCompletionTime() != null) {
      profile.setTwoFactorAuthCompletionTime(user.getTwoFactorAuthCompletionTime().getTime());
    }
    if (user.getTwoFactorAuthBypassTime() != null) {
      profile.setTwoFactorAuthBypassTime(user.getTwoFactorAuthBypassTime().getTime());
    }
    if (user.getAuthoritiesEnum() != null) {
      profile.setAuthorities(new ArrayList<>(user.getAuthoritiesEnum()));
    }
    if (user.getPageVisits() != null && !user.getPageVisits().isEmpty()) {
      profile.setPageVisits(
          user.getPageVisits().stream().map(TO_CLIENT_PAGE_VISIT).collect(Collectors.toList()));
    }
    if (user.getDemographicSurvey() != null) {
      profile.setDemographicSurvey(TO_CLIENT_DEMOGRAPHIC_SURVEY.apply(user.getDemographicSurvey()));
    }
    if (user.getAddress() != null) {
      profile.setAddress(TO_CLIENT_ADDRESS_SURVEY.apply(user.getAddress()));
    }
    profile.setInstitutionalAffiliations(
        user.getInstitutionalAffiliations().stream()
            .map(TO_CLIENT_INSTITUTIONAL_AFFILIATION)
            .collect(Collectors.toList()));
    profile.setEmailVerificationStatus(user.getEmailVerificationStatusEnum());

    profile.setFreeTierUsage(freeTierBillingService.getUserCachedFreeTierUsage(user));
    profile.setFreeTierDollarQuota(freeTierBillingService.getUserFreeTierDollarLimit(user));

    return profile;
  }
}
