package org.pmiops.workbench.interceptors;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.api.client.http.HttpMethods;
import com.google.api.client.http.HttpResponseException;
import com.google.api.services.oauth2.model.Userinfoplus;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.firecloud.ApiException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.DataAccessLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import org.pmiops.workbench.annotations.AuthorityRequired;
import org.pmiops.workbench.auth.ProfileService;
import org.pmiops.workbench.auth.UserAuthentication;
import org.pmiops.workbench.auth.UserInfoService;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.model.Authority;


/**
 * Intercepts all non-OPTIONS API requests to ensure they have an appropriate auth token.
 *
 * Checks handler methods for annotations like
 *     @AuthorityRequired({Authority.REVIEW_RESEARCH_PURPOSE})
 * to enforce granular permissions.
 */
@Service
public class CorsInterceptor extends HandlerInterceptorAdapter {
  private static final Logger log = Logger.getLogger(CorsInterceptor.class.getName());


  public static final String CREDENTIALS_NAME = "Access-Control-Allow-Credentials";
  public static final String ORIGIN_NAME = "Access-Control-Allow-Origin";
  public static final String METHODS_NAME = "Access-Control-Allow-Methods";
  public static final String HEADERS_NAME = "Access-Control-Allow-Headers";
  public static final String MAX_AGE_NAME = "Access-Control-Max-Age";

  /**
   * Returns true iff the request is auth'd and should proceed. Publishes authenticated user info
   * using Spring's SecurityContext.
   * @param handler The Swagger-generated ApiController. It contains our handler as a private
   *     delegate.
   */
  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    response.setHeader(CREDENTIALS_NAME, "true");
    response.setHeader(ORIGIN_NAME, "*");
    response.setHeader(METHODS_NAME, "GET, HEAD, POST, PUT, DELETE, PATCH, TRACE, OPTIONS");
    response.setHeader(HEADERS_NAME, "Origin, X-Requested-With, Content-Type, Accept, Authorization");

    return true;
  }


}
