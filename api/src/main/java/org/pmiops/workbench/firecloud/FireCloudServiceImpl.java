package org.pmiops.workbench.firecloud;

import javax.inject.Provider;
import org.pmiops.workbench.firecloud.api.ProfileApi;
import org.pmiops.workbench.firecloud.model.Me;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FireCloudServiceImpl implements FireCloudService {

  private final Provider<ProfileApi> profileApiProvider;

  @Autowired
  public FireCloudServiceImpl(Provider<ProfileApi> profileApiProvider) {
    this.profileApiProvider = profileApiProvider;
  }

  @Override
  public boolean isRequesterEnabledInFirecloud() throws ApiException {
    ProfileApi profileApi = profileApiProvider.get();
    Me me = profileApi.me();
    // Users can only use FireCloud if the Google and LDAP flags are enabled.
    return me.getEnabled() != null
        && isTrue(me.getEnabled().getGoogle()) && isTrue(me.getEnabled().getLdap());
  }

  private boolean isTrue(Boolean b) {
    return b != null && b == true;
  }
}
