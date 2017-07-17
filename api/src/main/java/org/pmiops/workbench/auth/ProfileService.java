package org.pmiops.workbench.auth;

import com.google.api.services.oauth2.model.Userinfoplus;
import javax.inject.Provider;
import org.pmiops.workbench.firecloud.ApiException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.Profile;
import org.pmiops.workbench.model.Profile.DataAccessLevelEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProfileService {

  private final FireCloudService fireCloudService;
  private final Provider<Userinfoplus> userInfoProvider;

  @Autowired
  ProfileService(FireCloudService fireCloudService, Provider<Userinfoplus> userInfoProvider) {
    this.fireCloudService = fireCloudService;
    this.userInfoProvider = userInfoProvider;
  }

  public Profile getProfile() throws ApiException {
    boolean enabledInFireCloud = fireCloudService.isRequesterEnabledInFirecloud();
    Userinfoplus userInfo = userInfoProvider.get();
    Profile profile = new Profile();
    // TODO: do group membership check to determine the appropriate data access level here
    profile.setDataAccessLevel(DataAccessLevelEnum.REGISTERED);
    profile.setEmail(userInfo.getEmail());
    profile.setFamilyName(userInfo.getFamilyName());
    profile.setFullName(userInfo.getName());
    profile.setGivenName(userInfo.getGivenName());
    profile.setEnabledInFireCloud(enabledInFireCloud);
    return profile;
  }
}
