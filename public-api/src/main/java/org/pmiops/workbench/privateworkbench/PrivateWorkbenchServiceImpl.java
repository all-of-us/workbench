package org.pmiops.workbench.privateworkbench;

import org.pmiops.workbench.privateworkbench.api.ProfileApi;
import org.pmiops.workbench.privateworkbench.model.Profile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.inject.Provider;

@Service
// TODO: consider retrying internally when FireCloud returns a 503
public class PrivateWorkbenchServiceImpl implements PrivateWorkbenchService {
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
