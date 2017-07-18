package org.pmiops.workbench.auth;

import com.google.api.services.oauth2.model.Userinfoplus;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

public class UserAuthentication implements Authentication {

  private final Userinfoplus userInfo;
  private final String bearerToken;

  public UserAuthentication(Userinfoplus userInfo, String bearerToken) {
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
}
