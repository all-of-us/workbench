package org.pmiops.workbench.google;

import static com.google.api.client.googleapis.util.Utils.getDefaultJsonFactory;
import static com.google.api.client.googleapis.util.Utils.getDefaultTransport;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.extensions.appengine.auth.oauth2.AppIdentityCredential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.admin.directory.Directory;
import com.google.api.services.admin.directory.DirectoryScopes;
import com.google.api.services.admin.directory.model.User;
import com.google.api.services.admin.directory.model.UserName;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import javax.inject.Provider;

import org.pmiops.workbench.api.WorkspacesController;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.google.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DirectoryServiceImpl implements DirectoryService {

  private static final Logger log = Logger.getLogger(DirectoryServiceImpl.class.getName());

  static final String APPLICATION_NAME = "All of Us Researcher Workbench";

  // This list must exactly match the scopes allowed via the GSuite Domain Admin page here:
  // https://admin.google.com/AdminHome?chromeless=1#OGX:ManageOauthClients
  // For example, ADMIN_DIRECTORY_USER does not encapsulate ADMIN_DIRECTORY_USER_READONLY — it must
  // be explicit.
  // The "Client Name" field in that form must be the cient ID of the service account. The field
  // will accept the email address of the service account and lookup the correct client ID giving
  // the impression that the email address is an acceptable substitute, but testing shows that this
  // doesn't actually work.
  static final List<String> SCOPES = Arrays.asList(
      DirectoryScopes.ADMIN_DIRECTORY_USER, DirectoryScopes.ADMIN_DIRECTORY_USER_READONLY
  );

  final Provider<WorkbenchConfig> configProvider;

  @Autowired
  public DirectoryServiceImpl(Provider<WorkbenchConfig> configProvider) {
    this.configProvider = configProvider;
  }


  private GoogleCredential createCredentialWithImpersonation() {
    String gSuiteDomain = configProvider.get().googleDirectoryService.gSuiteDomain;
    GoogleCredential credential = Utils.getDefaultGoogleCredential();
    return credential.createScoped(SCOPES);

    /*log.info("B2 = " + credential.getServiceAccountId());
    try {
      boolean refreshed = credential.refreshToken();
      log.info("C = " + credential.getServiceAccountId());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    log.info("D = " + credential.getServiceAccountId());
    return new GoogleCredential.Builder()
        .setTransport(getDefaultTransport())
        .setJsonFactory(getDefaultJsonFactory())
        // Must be an admin user in the GSuite domain.
        .setServiceAccountUser("directory-service@"+gSuiteDomain)
        .setServiceAccountId(credential.getServiceAccountId())
        .setServiceAccountScopes(SCOPES)
        .setServiceAccountPrivateKey(credential.getServiceAccountPrivateKey())
        .setServiceAccountPrivateKeyId(credential.getServiceAccountPrivateKeyId())
        .setTokenServerEncodedUrl(credential.getTokenServerEncodedUrl()).build();
    */
  }

  private Directory getGoogleDirectoryService() {
    return new Directory.Builder(getDefaultTransport(), getDefaultJsonFactory(),
          createCredentialWithImpersonation())
        .setApplicationName(APPLICATION_NAME)
        .build();
  }

  public boolean isUsernameTaken(String username) {
    String gSuiteDomain = configProvider.get().googleDirectoryService.gSuiteDomain;
    try {
      getGoogleDirectoryService().users().get(username+"@"+gSuiteDomain).execute();
      return true; // successful call means user exists
    } catch (GoogleJsonResponseException e) {
      if (e.getDetails().getCode() == 404) {
        return false;
      } else {
        throw new RuntimeException(e);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void createUser(String givenName, String familyName, String username, String password) {
    String gSuiteDomain = configProvider.get().googleDirectoryService.gSuiteDomain;
    User user = new User()
      .setPrimaryEmail(username+"@"+gSuiteDomain)
      .setPassword(password)
      .setName(new UserName().setGivenName(givenName).setFamilyName(familyName));
    try {
      getGoogleDirectoryService().users().insert(user).execute();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void deleteUser(String username) {
    String gSuiteDomain = configProvider.get().googleDirectoryService.gSuiteDomain;
    try {
      getGoogleDirectoryService().users().delete(username+"@"+gSuiteDomain).execute();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
