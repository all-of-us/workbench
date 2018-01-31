package org.pmiops.workbench.auth;

import java.util.ArrayList;
import javax.inject.Provider;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.firecloud.ApiException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.BlockscoreIdVerificationStatus;
import org.pmiops.workbench.model.Profile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProfileService {

  private final FireCloudService fireCloudService;
  private final Provider<User> userProvider;
  private final UserDao userDao;

  @Autowired
  public ProfileService(FireCloudService fireCloudService, Provider<User> userProvider,
      UserDao userDao) {
    this.fireCloudService = fireCloudService;
    this.userProvider = userProvider;
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
    profile.setEnabledInFireCloud(enabledInFireCloud);

    if (user.getBlockscoreVerificationIsValid() == null) {
      profile.setBlockscoreIdVerificationStatus(BlockscoreIdVerificationStatus.UNVERIFIED);
    } else if (user.getBlockscoreVerificationIsValid() == false) {
      profile.setBlockscoreIdVerificationStatus(BlockscoreIdVerificationStatus.REJECTED);
    }
    else {
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
    if (user.getDataAccessLevel() != null) {
      profile.setDataAccessLevel(user.getDataAccessLevel());
    }
    if (user.getAuthorities() != null) {
      profile.setAuthorities(new ArrayList(user.getAuthorities()));
    }

    return profile;
  }
}
