package org.pmiops.workbench.firecloud;

import javax.servlet.http.HttpServletResponse;
import org.pmiops.workbench.firecloud.api.ProfileApi;
import org.pmiops.workbench.firecloud.model.Me;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class FireCloudServiceImpl implements FireCloudService {

  @Override
  public boolean isRequesterEnabledInFirecloud() throws ApiException {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    ProfileApi profileApi = new ProfileApi();
    ApiClient apiClient = new ApiClient();
    apiClient.setAccessToken((String) authentication.getCredentials());
    profileApi.setApiClient(apiClient);

    Me me = profileApi.me();
    // Users can only use FireCloud if all three flags are enabled.
    return me.getEnabled() != null && isTrue(me.getEnabled().getAllUsersGroup())
        && isTrue(me.getEnabled().getGoogle()) && isTrue(me.getEnabled().getLdap());
  }

  private boolean isTrue(Boolean b) {
    return b != null && b == true;
  }
}
