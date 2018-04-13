package org.pmiops.workbench.interceptors;

import com.google.api.client.http.HttpMethods;
import com.google.api.client.http.HttpResponseException;
import com.google.api.services.oauth2.model.Userinfoplus;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.pmiops.workbench.annotations.AuthorityRequired;
import org.pmiops.workbench.auth.UserAuthentication;
import org.pmiops.workbench.auth.UserAuthentication.UserType;
import org.pmiops.workbench.auth.UserInfoService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ExceptionUtils;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.firecloud.ApiException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.Authority;
import org.pmiops.workbench.model.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.method.HandlerMethod;
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

  private final UserInfoService userInfoService;
  private final FireCloudService fireCloudService;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final UserDao userDao;
  private final UserService userService;

  @Autowired
  public AuthInterceptor(UserInfoService userInfoService, FireCloudService fireCloudService,
      Provider<WorkbenchConfig> workbenchConfigProvider, UserDao userDao, UserService userService) {
    this.userInfoService = userInfoService;
    this.fireCloudService = fireCloudService;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.userDao = userDao;
    this.userService = userService;
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
    // We suspect that security context, not having been cleared before, is still set here sometimes.
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null) {
      log.info("No authentication already set");
    } else {
      log.info("Authentication already set!");
      if (authentication instanceof UserAuthentication) {
        log.info("User authentication = " + ((UserAuthentication) authentication).getPrincipal().getEmail());
      }
    }

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

    // TODO: check Google group membership to ensure user is in registered user group

    String userEmail = userInfo.getEmail();
    WorkbenchConfig workbenchConfig = workbenchConfigProvider.get();
    if (workbenchConfig.auth.serviceAccountApiUsers.contains(userEmail)) {
      // Whitelisted service accounts are able to make API calls, too.
      // TODO: stop treating service accounts as normal users, have a separate table for them,
      // administrators.
      User user = userDao.findUserByEmail(userEmail);
      if (user == null) {
        user = userService.createServiceAccountUser(userEmail);
      }
      SecurityContextHolder.getContext().setAuthentication(new UserAuthentication(user, userInfo,
          token, UserType.SERVICE_ACCOUNT));
      log.log(Level.INFO, "{0} service account in use", userInfo.getEmail());
      return true;
    }
    String gsuiteDomainSuffix =
        "@" + workbenchConfig.googleDirectoryService.gSuiteDomain;
    if (!userEmail.endsWith(gsuiteDomainSuffix)) {
      try {
        // If the email isn't in our GSuite domain, try FireCloud; we could be dealing with a
        // pet service account. In both AofU and FireCloud, the pet SA is treated as if it were
        // the user it was created for.
        userEmail = fireCloudService.getMe().getUserInfo().getUserEmail();
      } catch (ApiException e) {
        log.log(Level.INFO, "FireCloud lookup for {0} failed, can't access the workbench: {1}",
            new Object[]{userInfo.getEmail(), e.getMessage()});
        response.sendError(e.getCode());
        return false;
      }
      if (!userEmail.endsWith(gsuiteDomainSuffix)) {
        log.log(Level.INFO, "User {0} isn't in domain {1}, can't access the workbench",
            new Object[] { userEmail, gsuiteDomainSuffix });
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
        return false;
      }
    }
    User user = userDao.findUserByEmail(userEmail);
    if (user == null) {
      // TODO(danrodney): start populating contact email in Google account, use it here.
      user = userService.createUser(userInfo.getGivenName(), userInfo.getFamilyName(),
            userInfo.getEmail(), null);
    } else {
      if (user.getDisabled()) {
        throw new ForbiddenException(ExceptionUtils.errorResponse(ErrorCode.USER_DISABLED, "This user account has been disabled."));
      }
    }

    SecurityContextHolder.getContext().setAuthentication(new UserAuthentication(user, userInfo,
        token, UserType.RESEARCHER));

    // TODO: setup this in the context, get rid of log statement
    log.log(Level.INFO, "{0} logged in", userInfo.getEmail());

    if (!hasRequiredAuthority(method, user)) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN);
      return false;
    }

    return true;
  }

  /**
   * Checks any @AuthorityRequired annotation on the controller's handler method.
   *
   * There is a hierarchy of Swagger-generated interfaces/wrappers around our controllers:
   *     FooApi (interface generated by Swagger)
   *     FooApiController (generated by Swagger, handed to AuthInterceptor)
   *       private FooApiDelegate delegate;
   *     FooApiDelegate (interface generated by Swagger)
   *     FooController implements FooApiDelegate (we implement this)
   * We can only annotate FooController methods, but are given a FooApiController, so we use
   * reflection to hack our way to FooController's method.
   *
   * @param handlerMethod The HandlerMethod for this request.
   * @param user Database details of the authenticated user.
   */
  boolean hasRequiredAuthority(HandlerMethod handlerMethod, User user) {
    return hasRequiredAuthority(InterceptorUtils.getControllerMethod(handlerMethod), user);
  }

  boolean hasRequiredAuthority(Method controllerMethod, User user) {
    String controllerMethodName =
        controllerMethod.getDeclaringClass().getName() + "." + controllerMethod.getName();
    AuthorityRequired req = controllerMethod.getAnnotation(AuthorityRequired.class);
    if (req != null) {
      if (user == null) {
        throw new BadRequestException("User is not initialized; please register");
      }
      // Fetch the user with authorities, since they aren't loaded during normal
      user = userDao.findUserWithAuthorities(user.getUserId());
      Collection<Authority> granted = user.getAuthorities();
      if (granted.containsAll(Arrays.asList(req.value()))) {
        return true;
      } else {
        log.log(
            Level.INFO,
            "{0} required authorities {1} but user had only {2}.",
            new Object[] {
                controllerMethodName,
                Arrays.toString(req.value()),
                Arrays.toString(granted.toArray())});
        return false;
      }
    }
    return true;  // No @AuthorityRequired annotation found at runtime, default to allowed.
  }
}
