package org.pmiops.workbench.interceptors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.pmiops.workbench.cdr.CdrVersionContext;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.AsyncHandlerInterceptor;

/**
 * Clears the CDR version (the controller is expected to then specify it based on the request if CDR
 * metadata is accessed.)
 */
@Service
public class ClearCdrVersionContextInterceptor implements AsyncHandlerInterceptor {

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    // OPTIONS methods requests don't need CDR version setup.
    if (request.getMethod().equals(HttpMethod.OPTIONS.name())) {
      return true;
    }
    CdrVersionContext.clearCdrVersion();
    return true;
  }
}
