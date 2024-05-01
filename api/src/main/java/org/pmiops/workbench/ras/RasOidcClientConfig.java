package org.pmiops.workbench.ras;

import static org.pmiops.workbench.ras.RasLinkConstants.AUTHORIZE_URL_SUFFIX;
import static org.pmiops.workbench.ras.RasLinkConstants.RAS_SECRET_BUCKET_NAME;
import static org.pmiops.workbench.ras.RasLinkConstants.TOKEN_URL_SUFFIX;
import static org.pmiops.workbench.ras.RasLinkConstants.USER_INFO_URL_SUFFIX;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import java.io.IOException;
import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.google.CloudStorageClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.annotation.RequestScope;

@Configuration
public class RasOidcClientConfig {
  static final String RAS_OIDC_CLIENT = "RAS_OIDC_CLIENT";
  static final String RAS_HTTP_TRANSPORT = "RAS_HTTP_TRANSPORT";

  @Bean(RAS_OIDC_CLIENT)
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  @Lazy
  public OpenIdConnectClient rasOidcClient(
      CloudStorageClient cloudStorageClient,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      @Qualifier(RAS_HTTP_TRANSPORT) HttpTransport httpTransport)
      throws IOException {
    String rasHost = workbenchConfigProvider.get().ras.host;
    String rasClientSecret = cloudStorageClient.getCredentialsBucketString(RAS_SECRET_BUCKET_NAME);
    String rasClientId = workbenchConfigProvider.get().ras.clientId;

    return OpenIdConnectClient.Builder.newBuilder()
        .setClientId(rasClientId)
        .setClientSecret(rasClientSecret)
        .setAuthorizeUrl(rasHost + AUTHORIZE_URL_SUFFIX)
        .setTokenUrl(rasHost + TOKEN_URL_SUFFIX)
        .setUserInfoUrl(rasHost + USER_INFO_URL_SUFFIX)
        .setHttpTransport(httpTransport)
        .build();
  }

  @Bean(RAS_HTTP_TRANSPORT)
  HttpTransport httpTransport() {
    return new NetHttpTransport();
  }
}
