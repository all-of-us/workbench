package org.pmiops.workbench.ras;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.model.Profile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RasLinkService {
  //The RAS url suffix for exchanging token using auth code.
  private static final String tokenUrlSuffix = "/auth/oauth/v2/token";
  // The RAS url suffix for initializing authorize request.
  private static final String authorizeUrlSuffix = "/auth/oauth/v2/authorize";
  // The RAS url suffix for fetching user info, i.e. GA4GH Passport.
  private static final String userInfoUrlSuffix = "/openid/connect/v1.1/userinfo";

  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  @Autowired
  public RasLinkService(
      Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.workbenchConfigProvider = workbenchConfigProvider;
  }

  public Profile linkRasLoginGovAccount(String authCode) {
    String rasHost = workbenchConfigProvider.get().ras.host;
    AuthorizationCodeFlow flow = OAuthHelper.newAuthCodeFlow(workbenchConfigProvider.get().ras.clientId);
    String accessToken = OAuthHelper.codeExchange(authCode);

  }
}
