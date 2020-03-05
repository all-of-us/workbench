package org.pmiops.workbench.interceptors;

import java.time.Duration;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

/**
 * Interceptor for handling request or response headers relating to security.
 *
 * <p>Note: there is a Spring module for handling some of this functionality; for now we've decided
 * not to install this as it brings along with it a lot of baked in functionality we don't want,
 * like basic auth.
 */
@Service
public class SecurityHeadersInterceptor extends HandlerInterceptorAdapter {
  private static final long HSTS_MAX_AGE = Duration.ofDays(365).getSeconds();

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    response.setHeader(
        "Strict-Transport-Security",
        String.format("max-age=%d; includeSubDomains; preload", HSTS_MAX_AGE));
    return true;
  }
}
