package org.pmiops.workbench.auth;

import com.google.api.services.oauth2.model.Userinfoplus;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import org.pmiops.workbench.db.model.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

public class UserAuthentication implements Authentication {

  private final User user;
  private final Userinfoplus userInfo;
  private final String bearerToken;

  public UserAuthentication(User user, Userinfoplus userInfo, String bearerToken) {
    this.user = user;
    this.userInfo = userInfo;
    this.bearerToken = bearerToken;
  }

  @Override
  public String getName() {
    return userInfo.getEmail();
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return ImmutableList.of();
  }

  @Override
  public String getCredentials() {
    return bearerToken;
  }

  @Override
  public Object getDetails() {
    return null;
  }

  @Override
  public Userinfoplus getPrincipal() {
    return userInfo;
  }

  @Override
  public boolean isAuthenticated() {
    return true;
  }

  @Override
  public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
  }

  public User getUser() {
    return user;
  }
}
