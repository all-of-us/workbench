package org.pmiops.workbench.privateWorkbench;

import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.privateWorkbench.api.ProfileApi;
import org.pmiops.workbench.privateWorkbench.model.Profile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.inject.Provider;
import java.util.logging.Logger;

@Service
// TODO: consider retrying internally when FireCloud returns a 503
public class PrivateWorkbenchServiceImpl implements PrivateWorkbenchService {
  private static final Logger log = Logger.getLogger(PrivateWorkbenchServiceImpl.class.getName());

  private final Provider<ProfileApi> profileApiProvider;

  @Autowired
  public PrivateWorkbenchServiceImpl(Provider<ProfileApi> profileApiProvider) {
    this.profileApiProvider = profileApiProvider;
  }

  @Override
  public Profile getMe() throws ApiException {
    ProfileApi profileApi = profileApiProvider.get();
    try {
      return profileApi.getMe();
    } catch (ApiException exception) {
      throw exception;
    }
  }
}
