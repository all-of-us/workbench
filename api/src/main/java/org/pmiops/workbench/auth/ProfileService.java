package org.pmiops.workbench.auth;

import java.util.ArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.firecloud.ApiException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.mailchimp.MailChimpService;
import org.pmiops.workbench.model.BlockscoreIdVerificationStatus;
import org.pmiops.workbench.model.EmailVerificationStatus;
import org.pmiops.workbench.model.InstitutionalAffiliation;
import org.pmiops.workbench.model.Profile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProfileService {
  private static final Function<org.pmiops.workbench.db.model.InstitutionalAffiliation,
      InstitutionalAffiliation> TO_CLIENT_INSTITUTIONAL_AFFILIATION =
      new Function<org.pmiops.workbench.db.model.InstitutionalAffiliation, InstitutionalAffiliation>() {
        @Override
        public InstitutionalAffiliation apply(
            org.pmiops.workbench.db.model.InstitutionalAffiliation institutionalAffiliation) {
          InstitutionalAffiliation result = new InstitutionalAffiliation();
          result.setRole(institutionalAffiliation.getRole());
          result.setInstitution(institutionalAffiliation.getInstitution());

          return result;
        }
      };

  private final FireCloudService fireCloudService;
  private final MailChimpService mailChimpService;
  private final UserDao userDao;

  @Autowired
  public ProfileService(FireCloudService fireCloudService, MailChimpService mailChimpService,
      UserDao userDao) {
    this.fireCloudService = fireCloudService;
    this.mailChimpService = mailChimpService;
    this.userDao = userDao;
  }

  public Profile getProfile(User user) throws ApiException {
    // Fetch the user's authorities, since they aren't loaded during normal request interception.
    User userWithAuthorities = userDao.findUserWithAuthorities(user.getUserId());
    if (userWithAuthorities != null) {
      // If the user is already written to the database, use it and whatever authorities are there.
      user = userWithAuthorities;
    }

    boolean enabledInFireCloud = fireCloudService.isRequesterEnabledInFirecloud();
    Profile profile = new Profile();
    profile.setUserId(user.getUserId());
    profile.setUsername(user.getEmail());
    profile.setFamilyName(user.getFamilyName());
    profile.setGivenName(user.getGivenName());
    profile.setContactEmail(user.getContactEmail());
    profile.setPhoneNumber(user.getPhoneNumber());
    profile.setFreeTierBillingProjectName(user.getFreeTierBillingProjectName());
    profile.setFreeTierBillingProjectStatus(user.getFreeTierBillingProjectStatus());
    profile.setEnabledInFireCloud(enabledInFireCloud);
    profile.setAboutYou(user.getAboutYou());
    profile.setAreaOfResearch(user.getAreaOfResearch());

    if (user.getBlockscoreVerificationIsValid() == null) {
      profile.setBlockscoreIdVerificationStatus(BlockscoreIdVerificationStatus.UNVERIFIED);
    } else if (user.getBlockscoreVerificationIsValid() == false) {
      profile.setBlockscoreIdVerificationStatus(BlockscoreIdVerificationStatus.REJECTED);
    } else {
      profile.setBlockscoreIdVerificationStatus(BlockscoreIdVerificationStatus.VERIFIED);
    }

    if (user.getTermsOfServiceCompletionTime() != null) {
      profile.setTermsOfServiceCompletionTime(user.getTermsOfServiceCompletionTime().getTime());
    }
    if (user.getEthicsTrainingCompletionTime() != null) {
      profile.setEthicsTrainingCompletionTime(user.getEthicsTrainingCompletionTime().getTime());
    }
    if (user.getDemographicSurveyCompletionTime() != null) {
      profile.setDemographicSurveyCompletionTime(user.getDemographicSurveyCompletionTime()
          .getTime());
    }
    if (user.getFirstSignInTime() != null) {
      profile.setFirstSignInTime(user.getFirstSignInTime().getTime());
    }
    if (user.getDataAccessLevel() != null) {
      profile.setDataAccessLevel(user.getDataAccessLevel());
    }
    if (user.getAuthorities() != null) {
      profile.setAuthorities(new ArrayList<>(user.getAuthorities()));
    }
    profile.setInstitutionalAffiliations(user.getInstitutionalAffiliations()
        .stream().map(TO_CLIENT_INSTITUTIONAL_AFFILIATION)
        .collect(Collectors.toList()));
    EmailVerificationStatus userEmailVerificationStatus = user.getEmailVerificationStatus();
    // if verification is pending or unverified, need to query MailChimp and update DB accordingly
    if (!userEmailVerificationStatus.equals(EmailVerificationStatus.SUBSCRIBED)) {
      if (userEmailVerificationStatus.equals(EmailVerificationStatus.UNVERIFIED) && user.getContactEmail() != null) {
        mailChimpService.addUserContactEmail(user.getContactEmail());
        userEmailVerificationStatus = EmailVerificationStatus.PENDING;
      } else if (userEmailVerificationStatus.equals(EmailVerificationStatus.PENDING)) {
        userEmailVerificationStatus = EmailVerificationStatus.fromValue(mailChimpService.getMember(user.getContactEmail()));
      }
      user.setEmailVerificationStatus(userEmailVerificationStatus);
      userDao.save(user);
    }
    profile.setEmailVerificationStatus(user.getEmailVerificationStatus());
    return profile;
  }
}
