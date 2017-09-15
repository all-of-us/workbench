package org.pmiops.workbench.firecloud;

import static org.springframework.http.HttpStatus.NOT_FOUND;

import javax.inject.Provider;
import org.pmiops.workbench.firecloud.api.BillingApi;
import org.pmiops.workbench.firecloud.api.ProfileApi;
import org.pmiops.workbench.firecloud.model.CreateRawlsBillingProjectFullRequest;
import org.pmiops.workbench.firecloud.model.Me;
import org.pmiops.workbench.firecloud.model.Profile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
// TODO: consider retrying internally when FireCloud returns a 503
public class FireCloudServiceImpl implements FireCloudService {

  // TODO: put this in a config object in the database
  private static final String ALL_OF_US_BILLING_ACCOUNT = "billingAccounts/001A68-D1B344-975E93";

  private final Provider<ProfileApi> profileApiProvider;
  private final Provider<BillingApi> billingApiProvider;

  @Autowired
  public FireCloudServiceImpl(Provider<ProfileApi> profileApiProvider,
      Provider<BillingApi> billingApiProvider) {
    this.profileApiProvider = profileApiProvider;
    this.billingApiProvider = billingApiProvider;
  }

  @Override
  public boolean isRequesterEnabledInFirecloud() throws ApiException {
    ProfileApi profileApi = profileApiProvider.get();
    try {
      Me me = profileApi.me();
      // Users can only use FireCloud if the Google and LDAP flags are enabled.
      return me.getEnabled() != null
          && isTrue(me.getEnabled().getGoogle()) && isTrue(me.getEnabled().getLdap());
    } catch (ApiException e) {
      if (e.getCode() == NOT_FOUND.value()) {
        return false;
      }
      throw e;
    }
  }

  @Override
  public void registerUser(String contactEmail, String firstName, String lastName)
      throws ApiException {
    ProfileApi profileApi = profileApiProvider.get();
    Profile profile = new Profile();
    profile.setContactEmail(contactEmail);
    profile.setFirstName(firstName);
    profile.setLastName(lastName);
    // TODO: make these fields not required in Firecloud and stop passing them in, or prompt for
    // them (RW-29)
    profile.setTitle("None");
    profile.setInstitute("None");
    profile.setInstitutionalProgram("None");
    profile.setProgramLocationCity("None");
    profile.setProgramLocationState("None");
    profile.setProgramLocationCountry("None");
    profile.setPi("None");
    profile.setNonProfitStatus("None");

    profileApi.setProfile(profile);
  }

  @Override
  public void createAllOfUsBillingProject(String projectName) throws ApiException {
    BillingApi billingApi = billingApiProvider.get();
    CreateRawlsBillingProjectFullRequest request = new CreateRawlsBillingProjectFullRequest();
    request.setBillingAccount(ALL_OF_US_BILLING_ACCOUNT);
    request.setProjectName(projectName);
    billingApi.createBillingProjectFull(request);
  }

  @Override
  public void addUserToBillingProject(String email, String projectName) throws ApiException {
    BillingApi billingApi = billingApiProvider.get();
    billingApi.addUserToBillingProject(projectName, "user", email);
  }

  private boolean isTrue(Boolean b) {
    return b != null && b == true;
  }
}
