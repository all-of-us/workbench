package org.pmiops.workbench.interceptors;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.pmiops.workbench.auth.TokenVerifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

/**
 * Intercepts all non-OPTIONS API requests to ensure they have an appropriate auth token.
 */
@Service
public class AuthInterceptor extends HandlerInterceptorAdapter {

  private static final Logger log = Logger.getLogger(AuthInterceptor.class.getName());

  private final TokenVerifier tokenVerifier;

  @Autowired
  public AuthInterceptor(TokenVerifier tokenVerifier) {
    this.tokenVerifier = tokenVerifier;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    if (request.getMethod() == HttpMethod.OPTIONS.name()) {
      // OPTIONS methods don't have to be authenticated.
      return true;
    }
    String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

    if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
      return false;
    }

    String token = authorizationHeader.substring("Bearer".length()).trim();
    log.log(Level.INFO, "token: {0}", tokenVerifier.verifyBearerToken(token));
    return true;
  }

  @Override
  public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
      ModelAndView modelAndView) throws Exception {

  }

  @Override
  public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
      Object handler, Exception ex) throws Exception {

  }
}
