package org.pmiops.workbench.interceptors

import com.google.api.client.http.HttpMethods
import com.google.api.services.oauth2.model.Userinfoplus
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.Authorization
import java.lang.reflect.Method
import java.util.Arrays
import java.util.logging.Level
import java.util.logging.Logger
import javax.inject.Provider
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import org.pmiops.workbench.annotations.AuthorityRequired
import org.pmiops.workbench.auth.UserAuthentication
import org.pmiops.workbench.auth.UserAuthentication.UserType
import org.pmiops.workbench.auth.UserInfoService
import org.pmiops.workbench.config.WorkbenchConfig
import org.pmiops.workbench.db.dao.UserDao
import org.pmiops.workbench.db.dao.UserService
import org.pmiops.workbench.db.model.User
import org.pmiops.workbench.exceptions.BadRequestException
import org.pmiops.workbench.exceptions.ForbiddenException
import org.pmiops.workbench.exceptions.WorkbenchException
import org.pmiops.workbench.firecloud.FireCloudService
import org.pmiops.workbench.model.Authority
import org.pmiops.workbench.model.ErrorCode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.http.HttpHeaders
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter

/**
 * Intercepts all non-OPTIONS API requests to ensure they have an appropriate auth token.
 *
 *
 * Checks handler methods for annotations
 * like @AuthorityRequired({Authority.REVIEW_RESEARCH_PURPOSE}) to enforce granular permissions.
 */
@Service
class AuthInterceptor @Autowired
constructor(
        private val userInfoService: UserInfoService,
        private val fireCloudService: FireCloudService,
        private val workbenchConfigProvider: Provider<WorkbenchConfig>,
        private val userDao: UserDao,
        private val userService: UserService) : HandlerInterceptorAdapter() {

    /**
     * Returns true iff the request is auth'd and should proceed. Publishes authenticated user info
     * using Spring's SecurityContext.
     *
     * @param handler The Swagger-generated ApiController. It contains our handler as a private
     * delegate.
     */
    @Throws(Exception::class)
    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse?, handler: Any?): Boolean {
        // Clear the security context before we start, to make sure we're not using authentication
        // from a previous request.
        SecurityContextHolder.clearContext()

        // OPTIONS methods requests don't need authorization.
        if (request.method == HttpMethods.OPTIONS) {
            return true
        }

        val method = handler as HandlerMethod?

        var isAuthRequired = false
        val apiOp = AnnotationUtils.findAnnotation(method!!.method, ApiOperation::class.java)
        if (apiOp != null) {
            for (auth in apiOp.authorizations()) {
                if (auth.value() == authName) {
                    isAuthRequired = true
                    break
                }
            }
        }
        if (!isAuthRequired) {
            return true
        }

        val authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION)

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            log.warning("No bearer token found in request")
            response!!.sendError(HttpServletResponse.SC_UNAUTHORIZED)
            return false
        }

        val token = authorizationHeader.substring("Bearer".length).trim { it <= ' ' }
        val userInfo = userInfoService.getUserInfo(token)

        // TODO: check Google group membership to ensure user is in registered user group

        var userEmail = userInfo.email
        val workbenchConfig = workbenchConfigProvider.get()
        if (workbenchConfig.auth.serviceAccountApiUsers.contains(userEmail)) {
            // Whitelisted service accounts are able to make API calls, too.
            // TODO: stop treating service accounts as normal users, have a separate table for them,
            // administrators.
            var user: User? = userDao.findUserByEmail(userEmail)
            if (user == null) {
                user = userService.createServiceAccountUser(userEmail)
            }
            SecurityContextHolder.getContext().authentication = UserAuthentication(user, userInfo, token, UserType.SERVICE_ACCOUNT)
            log.log(Level.INFO, "{0} service account in use", userInfo.email)
            return true
        }
        val gsuiteDomainSuffix = "@" + workbenchConfig.googleDirectoryService.gSuiteDomain
        if (!userEmail.endsWith(gsuiteDomainSuffix)) {
            // Temporarily set the authentication with no user, so we can look up what user this
            // corresponds to in FireCloud.
            SecurityContextHolder.getContext().authentication = UserAuthentication(null, userInfo, token, UserType.SERVICE_ACCOUNT)
            // If the email isn't in our GSuite domain, try FireCloud; we could be dealing with a
            // pet service account. In both AofU and FireCloud, the pet SA is treated as if it were
            // the user it was created for.
            userEmail = fireCloudService.me.getUserInfo().getUserEmail()
            if (!userEmail.endsWith(gsuiteDomainSuffix)) {
                log.log(
                        Level.INFO,
                        "User {0} isn't in domain {1}, can't access the workbench",
                        arrayOf<Any>(userEmail, gsuiteDomainSuffix))
                response!!.sendError(HttpServletResponse.SC_UNAUTHORIZED)
                return false
            }
        }
        var user: User? = userDao.findUserByEmail(userEmail)
        if (user == null) {
            // TODO(danrodney): start populating contact email in Google account, use it here.
            user = userService.createUser(
                    userInfo.givenName,
                    userInfo.familyName,
                    userInfo.email, null, null, null, null, null, null, null)
        } else {
            if (user.disabled) {
                throw ForbiddenException(
                        WorkbenchException.errorResponse(
                                ErrorCode.USER_DISABLED, "This user account has been disabled."))
            }
        }

        SecurityContextHolder.getContext().authentication = UserAuthentication(user, userInfo, token, UserType.RESEARCHER)

        // TODO: setup this in the context, get rid of log statement
        log.log(Level.INFO, "{0} logged in", userInfo.email)

        if (!hasRequiredAuthority(method, user)) {
            response!!.sendError(HttpServletResponse.SC_FORBIDDEN)
            return false
        }

        return true
    }

    @Throws(Exception::class)
    override fun postHandle(
            request: HttpServletRequest?,
            response: HttpServletResponse?,
            handler: Any?,
            modelAndView: ModelAndView?) {
        // Clear the security context, just to make sure nothing subsequently uses the credentials
        // set up in here.
        SecurityContextHolder.clearContext()
    }

    /**
     * Checks any @AuthorityRequired annotation on the controller's handler method.
     *
     *
     * There is a hierarchy of Swagger-generated interfaces/wrappers around our controllers: FooApi
     * (interface generated by Swagger) FooApiController (generated by Swagger, handed to
     * AuthInterceptor) private FooApiDelegate delegate; FooApiDelegate (interface generated by
     * Swagger) FooController implements FooApiDelegate (we implement this) We can only annotate
     * FooController methods, but are given a FooApiController, so we use reflection to hack our way
     * to FooController's method.
     *
     * @param handlerMethod The HandlerMethod for this request.
     * @param user Database details of the authenticated user.
     */
    internal fun hasRequiredAuthority(handlerMethod: HandlerMethod, user: User?): Boolean {
        return hasRequiredAuthority(InterceptorUtils.getControllerMethod(handlerMethod), user)
    }

    internal fun hasRequiredAuthority(controllerMethod: Method, user: User?): Boolean {
        var user = user
        val controllerMethodName = controllerMethod.declaringClass.name + "." + controllerMethod.name
        val req = controllerMethod.getAnnotation(AuthorityRequired::class.java)
        if (req != null) {
            if (user == null) {
                throw BadRequestException("User is not initialized; please register")
            }
            // Fetch the user with authorities, since they aren't loaded during normal
            user = userDao.findUserWithAuthorities(user.userId)
            val granted = user!!.authoritiesEnum

            // DEVELOPER subsumes all other authorities.
            if (granted!!.contains(Authority.DEVELOPER) || granted.containsAll(Arrays.asList<Array<Authority>>(*req.value()))) {
                return true
            } else {
                log.log(
                        Level.INFO,
                        "{0} required authorities {1} but user had only {2}.",
                        arrayOf<Any>(controllerMethodName, Arrays.toString(req.value()), Arrays.toString(granted.toTypedArray())))
                return false
            }
        }
        return true // No @AuthorityRequired annotation found at runtime, default to allowed.
    }

    companion object {
        private val log = Logger.getLogger(AuthInterceptor::class.java.name)
        private val authName = "aou_oauth"
    }
}
