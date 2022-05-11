package org.pmiops.workbench.auth;

import com.google.api.services.oauth2.model.Userinfo;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import org.pmiops.workbench.db.model.DbUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

public class UserAuthentication implements Authentication {

  public enum UserType {
    // A researcher or their pet service account
    RESEARCHER,
    // A GCP service account (not affiliated with a researcher)
    SERVICE_ACCOUNT
  }

  private final DbUser user;
  private final Userinfo userInfo;
  private final String bearerToken;
  private final UserType userType;

  public UserAuthentication(DbUser user, Userinfo userInfo, String bearerToken, UserType userType) {
    this.user = user;
    this.userInfo = userInfo;
    this.bearerToken = bearerToken;
    this.userType = userType;
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
  public Userinfo getPrincipal() {
    return userInfo;
  }

  @Override
  public boolean isAuthenticated() {
    return true;
  }

  @Override
  public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {}

  public DbUser getUser() {
    return user;
  }

  public UserType getUserType() {
    return userType;
  }
}
