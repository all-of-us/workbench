package org.pmiops.workbench.interceptors;

import com.google.api.client.http.HttpMethods;
import java.lang.reflect.Method;
import java.util.logging.Logger;
import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.pmiops.workbench.annotations.UsesDefaultCdr;
import org.pmiops.workbench.cdr.CdrVersionContext;
import org.pmiops.workbench.db.model.CdrVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

/**
 * Sets CdrVersionContext with the default CDR version on controller methods or classes annotated
 * with {@link UsesDefaultCdr}; otherwise clears the CDR version (the controller is expected to
 * then specify it based on the request using {@link CdrVersionContext#setCdrVersion(CdrVersion)
 * if CDR metadata is accessed.)
 */
@Service
public class DefaultCdrVersionInterceptor extends HandlerInterceptorAdapter {

  private static final Logger logger = Logger.getLogger(DefaultCdrVersionInterceptor.class.getName());
  private final Provider<CdrVersion> defaultCdrVersionProvider;

  @Autowired
  public DefaultCdrVersionInterceptor(@Qualifier("defaultCdr") Provider<CdrVersion>
      cdrVersionProvider) {
    this.defaultCdrVersionProvider = cdrVersionProvider;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    // OPTIONS methods requests don't need CDR version setup.
    if (request.getMethod().equals(HttpMethods.OPTIONS)) {
      return true;
    }
    Method method = InterceptorUtils.getControllerMethod((HandlerMethod) handler);
    CdrVersionContext.clearCdrVersion();
    if (method.getAnnotation(UsesDefaultCdr.class) != null ||
        method.getDeclaringClass().getAnnotation(UsesDefaultCdr.class) != null) {
      CdrVersionContext.setCdrVersion(defaultCdrVersionProvider.get());
    }
    return true;
  }

}
