package org.pmiops.workbench.google;

import static com.google.api.client.googleapis.util.Utils.getDefaultJsonFactory;
import static com.google.api.client.googleapis.util.Utils.getDefaultTransport;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.admin.directory.Directory;
import com.google.api.services.admin.directory.DirectoryScopes;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.pmiops.workbench.google.Utils;
import org.springframework.stereotype.Service;

@Service
public class DirectoryServiceImpl implements DirectoryService {

  private static final String APPLICATION_NAME = "All of Us Researcher Workbench";
  private static final List<String> SCOPES = Arrays.asList(
      DirectoryScopes.ADMIN_DIRECTORY_USER_READONLY
  );

  private GoogleCredential createCredentialWithImpersonation() {
    GoogleCredential credential = Utils.getDefaultGoogleCredential();
    return new GoogleCredential.Builder()
        .setTransport(getDefaultTransport())
        .setJsonFactory(getDefaultJsonFactory())
        // Must be an admin user in the GSuite domain.
        // TODO(dmohs): Domain should come from config.
        .setServiceAccountUser("directory-service@fake-research-aou.org")
        .setServiceAccountId(credential.getServiceAccountId())
        .setServiceAccountScopes(SCOPES)
        .setServiceAccountPrivateKey(credential.getServiceAccountPrivateKey())
        .setServiceAccountPrivateKeyId(credential.getServiceAccountPrivateKeyId())
        .setTokenServerEncodedUrl(credential.getTokenServerEncodedUrl()).build();
  }

  private Directory getGoogleDirectoryService() {
    return new Directory.Builder(getDefaultTransport(), getDefaultJsonFactory(),
          createCredentialWithImpersonation())
        .setApplicationName(APPLICATION_NAME)
        .build();
  }

  public boolean isUsernameTaken(String username) {
    try {
      getGoogleDirectoryService().users().get(username + "@fake-research-aou.org").execute();
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
}
