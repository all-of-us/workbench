package org.pmiops.workbench.auth;

import javax.inject.Provider;

import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.firecloud.ApiException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.Authority;
import org.pmiops.workbench.model.Profile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProfileService {

  private final FireCloudService fireCloudService;
  private final Provider<User> userProvider;

  @Autowired
  ProfileService(FireCloudService fireCloudService, Provider<User> userProvider) {
    this.fireCloudService = fireCloudService;
    this.userProvider = userProvider;
  }

  public Profile getProfile() throws ApiException {
    return getProfile(userProvider.get());
  }

  public Profile getProfile(User user) throws ApiException {
    boolean enabledInFireCloud = fireCloudService.isRequesterEnabledInFirecloud();
    Profile profile = new Profile();
    profile.setEmail(user.getEmail());
    profile.setFamilyName(user.getFamilyName());
    profile.setFullName(user.getFullName());
    profile.setGivenName(user.getGivenName());
    profile.setContactEmail(user.getContactEmail());
    profile.setPhoneNumber(user.getPhoneNumber());
    profile.setFreeTierBillingProjectName(user.getFreeTierBillingProjectName());
    profile.setEnabledInFireCloud(enabledInFireCloud);
    profile.setDataAccessLevel(Profile.DataAccessLevelEnum.fromValue(
        user.getDataAccessLevel().toString().toLowerCase()));

    return profile;
  }
}
