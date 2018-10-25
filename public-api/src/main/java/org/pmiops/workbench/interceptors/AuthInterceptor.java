package org.pmiops.workbench.interceptors;

import com.google.api.client.http.HttpMethods;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.pmiops.workbench.privateWorkbench.PrivateWorkbenchService;
import org.pmiops.workbench.privateWorkbench.model.Profile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;


/**
 * Intercepts all non-OPTIONS API requests to ensure they have an appropriate auth token.
 *
 * Checks handler methods for annotations like
 *     @AuthorityRequired({Authority.REVIEW_RESEARCH_PURPOSE})
 * to enforce granular permissions.
 */
@Service
public class AuthInterceptor extends HandlerInterceptorAdapter {
  private static final Logger log = Logger.getLogger(AuthInterceptor.class.getName());
  private static final String authName = "aou_oauth";

  private final PrivateWorkbenchService privateWorkbenchService;

  @Autowired
  public AuthInterceptor(PrivateWorkbenchService privateWorkbenchService) {
    this.privateWorkbenchService = privateWorkbenchService;
  }

  /**
   * Returns true iff the request is auth'd and should proceed. Publishes authenticated user info
   * using Spring's SecurityContext.
   * @param handler The Swagger-generated ApiController. It contains our handler as a private
   *     delegate.
   */
  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    // Clear the security context before we start, to make sure we're not using authentication
    // from a previous request.
    SecurityContextHolder.clearContext();

    // OPTIONS methods requests don't need authorization.
    if (request.getMethod().equals(HttpMethods.OPTIONS)) {
      return true;
    }

    HandlerMethod method = (HandlerMethod) handler;

    boolean isAuthRequired = false;
    ApiOperation apiOp = AnnotationUtils.findAnnotation(method.getMethod(), ApiOperation.class);
    if (apiOp != null) {
      for (Authorization auth : apiOp.authorizations()) {
        if (auth.value().equals(authName)) {
          isAuthRequired = true;
          break;
        }
      }
    }
    if (!isAuthRequired) {
      return true;
    }

    String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

    if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
      log.warning("No bearer token found in request");
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
      return false;
    }

    Profile profile = privateWorkbenchService.getMe();
    return true;
  }

  @Override
  public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                         ModelAndView modelAndView) throws Exception {
    // Clear the security context, just to make sure nothing subsequently uses the credentials
    // set up in here.
    SecurityContextHolder.clearContext();
  }
}
