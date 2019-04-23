package org.pmiops.workbench.auth;

import javax.inject.Provider;
import org.pmiops.workbench.db.model.User;
import org.springframework.stereotype.Service;

@Service
public class UserProvider {
  private final Provider<User> userProvider;

  public UserProvider(Provider<User> userProvider) {
    this.userProvider = userProvider;
  }

  public User get() {
    return userProvider.get();
  }
}
