package org.pmiops.workbench.shibboleth;

import javax.inject.Provider;
import org.pmiops.workbench.shibboleth.api.ShibbolethApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ShibbolethServiceImpl implements ShibbolethService {

  private final Provider<ShibbolethApi> shibbolethApiProvider;
  private final ShibbolethRetryHandler shibbolethRetryHandler;

  @Autowired
  public ShibbolethServiceImpl(
      Provider<ShibbolethApi> shibbolethApiProvider,
      ShibbolethRetryHandler shibbolethRetryHandler) {
    this.shibbolethApiProvider = shibbolethApiProvider;
    this.shibbolethRetryHandler = shibbolethRetryHandler;
  }

  @Override
  public void updateShibbolethToken(String jwt) {
    ShibbolethApi shibbolethApi = shibbolethApiProvider.get();
    shibbolethRetryHandler.run(
        (context) -> {
          shibbolethApi.postShibbolethToken(jwt);
          return null;
        });
  }
}
