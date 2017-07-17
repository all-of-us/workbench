package org.pmiops.workbench.auth;

import com.google.api.services.oauth2.model.Userinfoplus;
import org.pmiops.workbench.firecloud.ApiException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.Profile;
import org.pmiops.workbench.model.Profile.DataAccessLevelEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class ProfileService {

  private final FireCloudService fireCloudService;

  @Autowired
  ProfileService(FireCloudService fireCloudService) {
    this.fireCloudService = fireCloudService;
  }

  public Profile getProfile() throws ApiException {
    boolean enabledInFireCloud = fireCloudService.isRequesterEnabledInFirecloud();
    Userinfoplus userInfo =
        (Userinfoplus) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
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
