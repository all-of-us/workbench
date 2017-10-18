package org.pmiops.workbench.google;

import com.google.api.Http;
import com.google.api.client.http.HttpResponseException;
import com.google.api.services.admin.directory.model.User;
import java.io.IOException;

/**
 * Encapsulate Googe APIs for handling GSuite user accounts.
 */
public interface DirectoryService {
  public boolean isUsernameTaken(String username) throws IOException;
  public User createUser(String givenName, String familyName, String username, String password)
      throws IOException;
  public void deleteUser(String username) throws IOException;
}
