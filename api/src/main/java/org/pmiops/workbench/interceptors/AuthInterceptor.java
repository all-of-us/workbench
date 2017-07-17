package org.pmiops.workbench.interceptors;

import com.google.api.client.http.HttpMethods;
import com.google.api.client.http.HttpResponseException;
import com.google.api.services.oauth2.model.Userinfoplus;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.pmiops.workbench.auth.UserAuthentication;
import org.pmiops.workbench.auth.UserInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

/**
 * Intercepts all non-OPTIONS API requests to ensure they have an appropriate auth token.
 */
@Service
public class AuthInterceptor extends HandlerInterceptorAdapter {
  private static final Logger log = Logger.getLogger(AuthInterceptor.class.getName());

  private final UserInfoService userInfoService;

  @Autowired
  public AuthInterceptor(UserInfoService userInfoService) {
    this.userInfoService = userInfoService;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    // OPTIONS methods requests don't need authorization.
    if (request.getMethod().equals(HttpMethods.OPTIONS)) {
      return true;
    }

    String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

    if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
      log.warning("No bearer token found in request");
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
      return false;
    }

    String token = authorizationHeader.substring("Bearer".length()).trim();
    Userinfoplus userInfo;
    try {
      userInfo = userInfoService.getUserInfo(token);
    } catch (HttpResponseException e) {
      log.log(Level.WARNING,
          "{0} response getting user info for bearer token {1}: {2}",
          new Object[] { e.getStatusCode(), token, e.getStatusMessage() });
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
      return false;
    }
    // TODO: get token info and check that as well
    
    // TODO: check Google group membership to ensure user is in registered user group

    SecurityContextHolder.getContext().setAuthentication(new UserAuthentication(userInfo, token));
    
    // TODO: setup this in the context, get rid of log statement
    log.log(Level.INFO, "{0} logged in", userInfo.getEmail());

    return true;
  }
}
