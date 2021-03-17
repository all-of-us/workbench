package org.pmiops.workbench.ras;

import static org.pmiops.workbench.ras.RasLinkConstants.AUTHORIZE_URL_SUFFIX;
import static org.pmiops.workbench.ras.RasLinkConstants.RAS_SECRET_BUCKET_NAME;
import static org.pmiops.workbench.ras.RasLinkConstants.TOKEN_URL_SUFFIX;
import static org.pmiops.workbench.ras.RasLinkConstants.USER_INFO_URL_SUFFIX;

import java.io.IOException;
import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.google.CloudStorageClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
public class RasOidcClientConfig {
  static final String RAS_OIDC_CLIENT = "RAS_OIDC_CLIENT";

  @Bean(RAS_OIDC_CLIENT)
  @Lazy
  public OpenIdConnectClient rasOidcClient(
      CloudStorageClient cloudStorageClient,
      Provider<WorkbenchConfig> workbenchConfigProvider) throws IOException {
    String rasClientSecret = cloudStorageClient.getCredentialsBucketString(RAS_SECRET_BUCKET_NAME);
    String rasClientId = workbenchConfigProvider.get().ras.clientId;
    String rasTokenUrl = workbenchConfigProvider.get().ras.host + TOKEN_URL_SUFFIX;
    String rasAuthorizeUrl = workbenchConfigProvider.get().ras.host + AUTHORIZE_URL_SUFFIX;
    String rasUserInfoUrl = workbenchConfigProvider.get().ras.host + USER_INFO_URL_SUFFIX;

    return new OpenIdConnectClient(rasClientId, rasClientSecret, rasTokenUrl, rasAuthorizeUrl, rasUserInfoUrl);
  }
}
