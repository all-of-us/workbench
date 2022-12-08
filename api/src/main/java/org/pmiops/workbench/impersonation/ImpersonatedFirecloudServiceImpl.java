package org.pmiops.workbench.impersonation;

import static org.pmiops.workbench.firecloud.FireCloudServiceImpl.TERMS_OF_SERVICE_BODY;

import java.io.IOException;
import javax.annotation.Nonnull;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.firecloud.FirecloudApiClientFactory;
import org.pmiops.workbench.firecloud.FirecloudRetryHandler;
import org.pmiops.workbench.firecloud.api.TermsOfServiceApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * An impersonation-enabled version of {@link org.pmiops.workbench.firecloud.FireCloudServiceImpl}
 *
 * <p>REMINDER: With great power comes great responsibility. Impersonation should not be used in
 * production, except where absolutely necessary.
 */
@Service
public class ImpersonatedFirecloudServiceImpl implements ImpersonatedFirecloudService {

  private final FirecloudApiClientFactory firecloudApiClientFactory;
  private final FirecloudRetryHandler retryHandler;

  @Autowired
  public ImpersonatedFirecloudServiceImpl(
      FirecloudApiClientFactory firecloudApiClientFactory, FirecloudRetryHandler retryHandler) {
    this.firecloudApiClientFactory = firecloudApiClientFactory;
    this.retryHandler = retryHandler;
  }

  @Override
  public void acceptTermsOfService(@Nonnull DbUser dbUser) throws IOException {
    TermsOfServiceApi termsOfServiceApi = getTosApi(dbUser);
    retryHandler.run((context) -> termsOfServiceApi.acceptTermsOfService(TERMS_OF_SERVICE_BODY));
  }

  @Override
  public boolean getUserTermsOfServiceStatus(@Nonnull DbUser dbUser) throws IOException {
    TermsOfServiceApi termsOfServiceApi = getTosApi(dbUser);
    return retryHandler.run((context) -> termsOfServiceApi.getTermsOfServiceStatus());
  }

  private TermsOfServiceApi getTosApi(@Nonnull DbUser dbUser) throws IOException {
    return new TermsOfServiceApi(
        firecloudApiClientFactory.newImpersonatedApiClient(dbUser.getUsername()));
  }
}
