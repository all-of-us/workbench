package org.pmiops.workbench.auth;

import com.google.api.services.oauth2.model.Userinfoplus;
import javax.inject.Provider;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.firecloud.ApiException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.Profile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProfileService {

  private final FireCloudService fireCloudService;
  private final Provider<User> userProvider;
  private final Provider<Userinfoplus> userInfoProvider;

  @Autowired
  ProfileService(FireCloudService fireCloudService, UserDao userDao,
      Provider<Userinfoplus> userInfoProvider, Provider<User> userProvider) {
    this.fireCloudService = fireCloudService;
    this.userProvider = userProvider;
    this.userInfoProvider = userInfoProvider;
  }

  public Profile getProfile() throws ApiException {
    boolean enabledInFireCloud = fireCloudService.isRequesterEnabledInFirecloud();
    Userinfoplus userInfo = userInfoProvider.get();
    Profile profile = new Profile();
    profile.setEmail(userInfo.getEmail());
    profile.setFamilyName(userInfo.getFamilyName());
    profile.setFullName(userInfo.getName());
    profile.setGivenName(userInfo.getGivenName());
    profile.setEnabledInFireCloud(enabledInFireCloud);

    User user = userProvider.get();
    profile.setDataAccessLevel(Profile.DataAccessLevelEnum.fromValue(
        user.getDataAccessLevel().toString().toLowerCase()));

    return profile;
  }
}
