package org.pmiops.workbench.interceptors;

import com.google.api.client.http.HttpMethods;
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
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.Authority;
import org.pmiops.workbench.model.ErrorCode;
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
 * <p>Checks handler methods for annotations
 * like @AuthorityRequired({Authority.REVIEW_RESEARCH_PURPOSE}) to enforce granular permissions.
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
  public AuthInterceptor(
      UserInfoService userInfoService,
      FireCloudService fireCloudService,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      UserDao userDao,
      UserService userService) {
    this.userInfoService = userInfoService;
    this.fireCloudService = fireCloudService;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.userDao = userDao;
    this.userService = userService;
  }

  /**
   * Returns true iff the request is auth'd and should proceed. Publishes authenticated user info
   * using Spring's SecurityContext.
   *
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

    final String token = authorizationHeader.substring("Bearer".length()).trim();
    final Userinfoplus OAuth2Userinfo = userInfoService.getUserInfo(token);

    // The Workbench considers the user's generated GSuite email to be their userName
    // Don't confuse this with the user's Contact Email, which is unrelated
    String userName = OAuth2Userinfo.getEmail();

    // TODO: check Google group membership to ensure user is in registered user group

    WorkbenchConfig workbenchConfig = workbenchConfigProvider.get();
    if (workbenchConfig.auth.serviceAccountApiUsers.contains(userName)) {
      // Whitelisted service accounts are able to make API calls, too.
      // TODO: stop treating service accounts as normal users, have a separate table for them,
      // administrators.
      DbUser user = userDao.findUserByUsername(userName);
      if (user == null) {
        user = userService.createServiceAccountUser(userName);
      }
      SecurityContextHolder.getContext()
          .setAuthentication(
              new UserAuthentication(user, OAuth2Userinfo, token, UserType.SERVICE_ACCOUNT));
      log.log(Level.INFO, "{0} service account in use", userName);
      return true;
    }
    String gsuiteDomainSuffix = "@" + workbenchConfig.googleDirectoryService.gSuiteDomain;
    if (!userName.endsWith(gsuiteDomainSuffix)) {
      // Temporarily set the authentication with no user, so we can look up what user this
      // corresponds to in FireCloud.
      SecurityContextHolder.getContext()
          .setAuthentication(
              new UserAuthentication(null, OAuth2Userinfo, token, UserType.SERVICE_ACCOUNT));
      // If the email isn't in our GSuite domain, try FireCloud; we could be dealing with a
      // pet service account. In both AofU and FireCloud, the pet SA is treated as if it were
      // the user it was created for.
      userName = fireCloudService.getMe().getUserInfo().getUserEmail();
      if (!userName.endsWith(gsuiteDomainSuffix)) {
        log.log(
            Level.INFO,
            "User {0} isn't in domain {1}, can't access the workbench",
            new Object[] {userName, gsuiteDomainSuffix});
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        return false;
      }
    }
    DbUser user = userDao.findUserByUsername(userName);
    if (user == null) {
      // TODO(danrodney): start populating contact email in Google account, use it here.
      user = userService.createUser(OAuth2Userinfo);
    } else {
      if (user.getDisabled()) {
        throw new ForbiddenException(
            WorkbenchException.errorResponse(
                "Rejecting request for disabled user account: " + user.getUsername(),
                ErrorCode.USER_DISABLED));
      }
    }

    SecurityContextHolder.getContext()
        .setAuthentication(
            new UserAuthentication(user, OAuth2Userinfo, token, UserType.RESEARCHER));

    // TODO: setup this in the context, get rid of log statement
    log.log(Level.FINE, "{0} logged in", OAuth2Userinfo.getEmail());

    if (!hasRequiredAuthority(method, user)) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN);
      return false;
    }

    return true;
  }

  @Override
  public void postHandle(
      HttpServletRequest request,
      HttpServletResponse response,
      Object handler,
      ModelAndView modelAndView)
      throws Exception {
    // Clear the security context, just to make sure nothing subsequently uses the credentials
    // set up in here.
    SecurityContextHolder.clearContext();
  }

  /**
   * Checks any @AuthorityRequired annotation on the controller's handler method.
   *
   * <p>There is a hierarchy of Swagger-generated interfaces/wrappers around our controllers: FooApi
   * (interface generated by Swagger) FooApiController (generated by Swagger, handed to
   * AuthInterceptor) private FooApiDelegate delegate; FooApiDelegate (interface generated by
   * Swagger) FooController implements FooApiDelegate (we implement this) We can only annotate
   * FooController methods, but are given a FooApiController, so we use reflection to hack our way
   * to FooController's method.
   *
   * @param handlerMethod The HandlerMethod for this request.
   * @param user Database details of the authenticated user.
   */
  boolean hasRequiredAuthority(HandlerMethod handlerMethod, DbUser user) {
    return hasRequiredAuthority(InterceptorUtils.getControllerMethod(handlerMethod), user);
  }

  boolean hasRequiredAuthority(Method controllerMethod, DbUser user) {
    String controllerMethodName =
        controllerMethod.getDeclaringClass().getName() + "." + controllerMethod.getName();
    AuthorityRequired req = controllerMethod.getAnnotation(AuthorityRequired.class);
    if (req != null) {
      if (user == null) {
        throw new BadRequestException("User is not initialized; please register");
      }
      // Fetch the user with authorities, since they aren't loaded during normal
      user = userDao.findUserWithAuthorities(user.getUserId());
      Collection<Authority> granted = user.getAuthoritiesEnum();

      // DEVELOPER subsumes all other authorities.
      if (granted.contains(Authority.DEVELOPER)
          || granted.containsAll(Arrays.asList(req.value()))) {
        return true;
      } else {
        log.log(
            Level.INFO,
            "{0} required authorities {1} but user had only {2}.",
            new Object[] {
              controllerMethodName, Arrays.toString(req.value()), Arrays.toString(granted.toArray())
            });
        return false;
      }
    }
    return true; // No @AuthorityRequired annotation found at runtime, default to allowed.
  }
}
