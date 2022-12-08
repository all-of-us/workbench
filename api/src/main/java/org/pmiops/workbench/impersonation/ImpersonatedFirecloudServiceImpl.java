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
    TermsOfServiceApi termsOfServiceApi =
        new TermsOfServiceApi(
            firecloudApiClientFactory.newImpersonatedApiClient(dbUser.getUsername()));
    retryHandler.run((context) -> termsOfServiceApi.acceptTermsOfService(TERMS_OF_SERVICE_BODY));
  }

  @Override
  public boolean getUserTermsOfServiceStatus(@Nonnull DbUser dbUser) throws IOException {
    TermsOfServiceApi termsOfServiceApi =
        new TermsOfServiceApi(
            firecloudApiClientFactory.newImpersonatedApiClient(dbUser.getUsername()));
    return retryHandler.run((context) -> termsOfServiceApi.getTermsOfServiceStatus());
  }
}
