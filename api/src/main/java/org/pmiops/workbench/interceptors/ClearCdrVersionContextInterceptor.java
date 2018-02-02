package org.pmiops.workbench.interceptors;

import com.google.api.client.http.HttpMethods;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.pmiops.workbench.cdr.CdrVersionContext;
import org.pmiops.workbench.db.model.CdrVersion;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

/**
 * Clears the CDR version (the controller is expected to
 * then specify it based on the request using {@link CdrVersionContext#setCdrVersion(CdrVersion)
 * if CDR metadata is accessed.)
 */
@Service
public class ClearCdrVersionContextInterceptor extends HandlerInterceptorAdapter {

  private static final Logger logger =
      Logger.getLogger(ClearCdrVersionContextInterceptor.class.getName());

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    // OPTIONS methods requests don't need CDR version setup.
    if (request.getMethod().equals(HttpMethods.OPTIONS)) {
      return true;
    }
    CdrVersionContext.clearCdrVersion();
    return true;
  }

}
