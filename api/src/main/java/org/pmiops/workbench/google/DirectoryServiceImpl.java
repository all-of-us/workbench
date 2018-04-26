package org.pmiops.workbench.google;

import static com.google.api.client.googleapis.util.Utils.getDefaultJsonFactory;

import com.google.api.client.extensions.appengine.http.UrlFetchTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.admin.directory.Directory;
import com.google.api.services.admin.directory.DirectoryScopes;
import com.google.api.services.admin.directory.model.User;
import com.google.api.services.admin.directory.model.UserName;
import java.io.InputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import javax.inject.Provider;
import javax.servlet.ServletContext;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.exceptions.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class DirectoryServiceImpl implements DirectoryService {

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

  final Provider<GoogleCredential> googleCredentialProvider;
  final Provider<WorkbenchConfig> configProvider;
  final HttpTransport httpTransport;

  @Autowired
  public DirectoryServiceImpl(Provider<GoogleCredential> googleCredentialProvider,
      Provider<WorkbenchConfig> configProvider,
      HttpTransport httpTransport) {
    this.googleCredentialProvider = googleCredentialProvider;
    this.configProvider = configProvider;
    this.httpTransport = httpTransport;
  }

  private GoogleCredential createCredentialWithImpersonation() {
    GoogleCredential googleCredential = googleCredentialProvider.get();
    String gSuiteDomain = configProvider.get().googleDirectoryService.gSuiteDomain;
    return new GoogleCredential.Builder()
        .setTransport(httpTransport)
        .setJsonFactory(getDefaultJsonFactory())
        // Must be an admin user in the GSuite domain.
        .setServiceAccountUser("directory-service@"+gSuiteDomain)
        .setServiceAccountId(googleCredential.getServiceAccountId())
        .setServiceAccountScopes(SCOPES)
        .setServiceAccountPrivateKey(googleCredential.getServiceAccountPrivateKey())
        .setServiceAccountPrivateKeyId(googleCredential.getServiceAccountPrivateKeyId())
        .setTokenServerEncodedUrl(googleCredential.getTokenServerEncodedUrl())
        .build();
  }

  private Directory getGoogleDirectoryService() {
    return new Directory.Builder(httpTransport, getDefaultJsonFactory(),
          createCredentialWithImpersonation())
        .setApplicationName(APPLICATION_NAME)
        .build();
  }

  @Override
  public User getUser(String email) throws IOException {
    try {
      return ExceptionUtils.executeWithRetries(getGoogleDirectoryService().users().get(email));
    } catch (GoogleJsonResponseException e) {
      if (e.getDetails().getCode() == HttpStatus.NOT_FOUND.value()) {
        return null;
      }
      throw e;
    }
  }

  @Override
  public boolean isUsernameTaken(String username) throws IOException {
    String gSuiteDomain = configProvider.get().googleDirectoryService.gSuiteDomain;
    return getUser(username + "@" + gSuiteDomain) != null;
  }

  @Override
  public User createUser(String givenName, String familyName, String username, String password)
      throws IOException {
    String gSuiteDomain = configProvider.get().googleDirectoryService.gSuiteDomain;
    User user = new User()
      .setPrimaryEmail(username+"@"+gSuiteDomain)
      .setPassword(password)
      .setName(new UserName().setGivenName(givenName).setFamilyName(familyName));
    try {
      ExceptionUtils.executeWithRetries(getGoogleDirectoryService().users().insert(user));
    } catch (GoogleJsonResponseException e) {
      throw ExceptionUtils.convertGoogleIOException(e);
    }
    return user;
  }

  @Override
  public void deleteUser(String username) throws IOException {
    String gSuiteDomain = configProvider.get().googleDirectoryService.gSuiteDomain;
    try {
      ExceptionUtils.executeWithRetries(getGoogleDirectoryService().users()
          .delete(username + "@" + gSuiteDomain));
    } catch (GoogleJsonResponseException e) {
      if (e.getDetails().getCode() == HttpStatus.NOT_FOUND.value()) {
        // Deleting a user that doesn't exist will have no effect.
        return;
      }
      throw e;
    }
  }
}
