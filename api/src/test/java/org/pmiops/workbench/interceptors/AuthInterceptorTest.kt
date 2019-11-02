package org.pmiops.workbench.interceptors

import com.google.common.truth.Truth.assertThat
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

import com.google.api.client.http.HttpMethods
import com.google.api.services.oauth2.model.Userinfoplus
import java.lang.reflect.Method
import java.util.ArrayList
import java.util.HashSet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import org.apache.http.HttpHeaders
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.pmiops.workbench.annotations.AuthorityRequired
import org.pmiops.workbench.api.ProfileApi
import org.pmiops.workbench.auth.UserInfoService
import org.pmiops.workbench.config.WorkbenchConfig
import org.pmiops.workbench.config.WorkbenchConfig.AuthConfig
import org.pmiops.workbench.config.WorkbenchConfig.GoogleDirectoryServiceConfig
import org.pmiops.workbench.db.dao.UserDao
import org.pmiops.workbench.db.dao.UserService
import org.pmiops.workbench.db.model.User
import org.pmiops.workbench.exceptions.NotFoundException
import org.pmiops.workbench.firecloud.FireCloudService
import org.pmiops.workbench.firecloud.model.Me
import org.pmiops.workbench.firecloud.model.UserInfo
import org.pmiops.workbench.model.Authority
import org.pmiops.workbench.test.Providers
import org.springframework.web.method.HandlerMethod

/** mimicing a Swagger-generated wrapper  */
internal class FakeApiController {
    fun handle() {}
}

/** mimicing our implementation, annotated  */
internal class FakeController {
    @AuthorityRequired(Authority.REVIEW_RESEARCH_PURPOSE)
    fun handle() {
    }
}

class AuthInterceptorTest {

    @Mock
    private val userInfoService: UserInfoService? = null
    @Mock
    private val fireCloudService: FireCloudService? = null
    @Mock
    private val userDao: UserDao? = null
    @Mock
    private val request: HttpServletRequest? = null
    @Mock
    private val response: HttpServletResponse? = null
    @Mock
    private val handler: HandlerMethod? = null
    @Mock
    private val userService: UserService? = null

    @Rule
    var mockitoRule = MockitoJUnit.rule()

    private var interceptor: AuthInterceptor? = null
    private var user: User? = null

    @Before
    fun setUp() {
        val workbenchConfig = WorkbenchConfig()
        workbenchConfig.googleDirectoryService = GoogleDirectoryServiceConfig()
        workbenchConfig.googleDirectoryService.gSuiteDomain = "fake-domain.org"
        workbenchConfig.auth = AuthConfig()
        workbenchConfig.auth.serviceAccountApiUsers = ArrayList()
        workbenchConfig.auth.serviceAccountApiUsers.add("service-account@appspot.gserviceaccount.com")
        this.interceptor = AuthInterceptor(
                userInfoService, fireCloudService, Providers.of(workbenchConfig), userDao, userService)
        this.user = User()
        user!!.userId = USER_ID
        user!!.disabled = false
    }

    @Test
    @Throws(Exception::class)
    fun preHandleOptions_OPTIONS() {
        `when`(request!!.method).thenReturn(HttpMethods.OPTIONS)
        assertThat(interceptor!!.preHandle(request, response, handler)).isTrue()
    }

    @Test
    @Throws(Exception::class)
    fun preHandleOptions_publicEndpoint() {
        `when`(handler!!.method).thenReturn(getProfileApiMethod("isUsernameTaken"))
        `when`(request!!.method).thenReturn(HttpMethods.GET)
        assertThat(interceptor!!.preHandle(request, response, handler)).isTrue()
    }

    @Test
    @Throws(Exception::class)
    fun preHandleGet_noAuthorization() {
        `when`(handler!!.method).thenReturn(getProfileApiMethod("getBillingProjects"))
        `when`(request!!.method).thenReturn(HttpMethods.GET)
        assertThat(interceptor!!.preHandle(request, response, handler)).isFalse()
        verify<HttpServletResponse>(response).sendError(HttpServletResponse.SC_UNAUTHORIZED)
    }

    @Test
    @Throws(Exception::class)
    fun preHandleGet_nonBearerAuthorization() {
        `when`(handler!!.method).thenReturn(getProfileApiMethod("getBillingProjects"))
        `when`(request!!.method).thenReturn(HttpMethods.GET)
        `when`(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("blah")
        assertThat(interceptor!!.preHandle(request, response, handler)).isFalse()
        verify<HttpServletResponse>(response).sendError(HttpServletResponse.SC_UNAUTHORIZED)
    }

    @Test(expected = NotFoundException::class)
    @Throws(Exception::class)
    fun preHandleGet_userInfoError() {
        `when`(handler!!.method).thenReturn(getProfileApiMethod("getBillingProjects"))
        `when`(request!!.method).thenReturn(HttpMethods.GET)
        `when`(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer foo")
        `when`(userInfoService!!.getUserInfo("foo")).thenThrow(NotFoundException())
        interceptor!!.preHandle(request, response, handler)
    }

    @Test(expected = NotFoundException::class)
    @Throws(Exception::class)
    fun preHandleGet_firecloudLookupFails() {
        `when`(handler!!.method).thenReturn(getProfileApiMethod("getBillingProjects"))
        `when`(request!!.method).thenReturn(HttpMethods.GET)
        `when`(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer foo")
        val userInfo = Userinfoplus()
        userInfo.email = "bob@bad-domain.org"
        `when`(userInfoService!!.getUserInfo("foo")).thenReturn(userInfo)
        `when`<Any>(fireCloudService!!.me).thenThrow(NotFoundException())
        interceptor!!.preHandle(request, response, handler)
    }

    @Test
    @Throws(Exception::class)
    fun preHandleGet_firecloudLookupSucceeds() {
        `when`(handler!!.method).thenReturn(getProfileApiMethod("getBillingProjects"))
        `when`(request!!.method).thenReturn(HttpMethods.GET)
        `when`(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer foo")
        val userInfo = Userinfoplus()
        userInfo.email = "bob@bad-domain.org"
        `when`(userInfoService!!.getUserInfo("foo")).thenReturn(userInfo)
        val fcUserInfo = UserInfo()
        fcUserInfo.setUserEmail("bob@fake-domain.org")
        val me = Me()
        me.setUserInfo(fcUserInfo)
        `when`<Any>(fireCloudService!!.me).thenReturn(me)
        `when`(userDao!!.findUserByEmail("bob@fake-domain.org")).thenReturn(user)
        assertThat(interceptor!!.preHandle(request, response, handler)).isTrue()
    }

    @Test
    @Throws(Exception::class)
    fun preHandleGet_firecloudLookupSucceedsNoUserRecordWrongDomain() {
        `when`(handler!!.method).thenReturn(getProfileApiMethod("getBillingProjects"))
        `when`(request!!.method).thenReturn(HttpMethods.GET)
        `when`(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer foo")
        val userInfo = Userinfoplus()
        userInfo.email = "bob@bad-domain.org"
        `when`(userInfoService!!.getUserInfo("foo")).thenReturn(userInfo)
        val fcUserInfo = UserInfo()
        fcUserInfo.setUserEmail("bob@also-bad-domain.org")
        val me = Me()
        me.setUserInfo(fcUserInfo)
        `when`<Any>(fireCloudService!!.me).thenReturn(me)
        `when`(userDao!!.findUserByEmail("bob@also-bad-domain.org")).thenReturn(null)
        assertThat(interceptor!!.preHandle(request, response, handler)).isFalse()
        verify<HttpServletResponse>(response).sendError(HttpServletResponse.SC_UNAUTHORIZED)
    }

    @Test
    @Throws(Exception::class)
    fun preHandleGet_userInfoSuccess() {
        `when`(handler!!.method).thenReturn(getProfileApiMethod("getBillingProjects"))
        `when`(request!!.method).thenReturn(HttpMethods.GET)
        `when`(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer foo")
        val userInfo = Userinfoplus()
        userInfo.email = "bob@fake-domain.org"
        `when`(userInfoService!!.getUserInfo("foo")).thenReturn(userInfo)
        `when`(userDao!!.findUserByEmail("bob@fake-domain.org")).thenReturn(user)
        assertThat(interceptor!!.preHandle(request, response, handler)).isTrue()
    }

    @Test
    @Throws(Exception::class)
    fun preHandleGet_noUserRecord() {
        `when`(handler!!.method).thenReturn(getProfileApiMethod("getBillingProjects"))
        `when`(request!!.method).thenReturn(HttpMethods.GET)
        `when`(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer foo")
        val userInfo = Userinfoplus()
        userInfo.givenName = "Bob"
        userInfo.familyName = "Jones"
        userInfo.email = "bob@fake-domain.org"
        `when`(userInfoService!!.getUserInfo("foo")).thenReturn(userInfo)
        `when`(userDao!!.findUserByEmail("bob@fake-domain.org")).thenReturn(null)
        `when`(userService!!.createUser(
                "Bob", "Jones", "bob@fake-domain.org", null, null, null, null, null, null, null))
                .thenReturn(user)
        assertThat(interceptor!!.preHandle(request, response, handler)).isTrue()
    }

    private fun getProfileApiMethod(methodName: String): Method {
        for (method in ProfileApi::class.java!!.getDeclaredMethods()) {
            if (method.getName() == methodName) {
                return method
            }
        }
        throw RuntimeException("Method \"$methodName\" not found")
    }

    @Test
    @Throws(Exception::class)
    fun authorityCheckPermitsWithNoAnnotation() {
        val method = getProfileApiMethod("getBillingProjects")
        assertThat(interceptor!!.hasRequiredAuthority(method, User())).isTrue()
    }

    @Test
    @Throws(Exception::class)
    fun authorityCheckDeniesWhenUserMissingAuthority() {
        val apiControllerMethod = FakeController::class.java.getMethod("handle")
        `when`(userDao!!.findUserWithAuthorities(USER_ID)).thenReturn(user)
        assertThat(interceptor!!.hasRequiredAuthority(apiControllerMethod, user)).isFalse()
    }

    @Test
    @Throws(Exception::class)
    fun authorityCheckPermitsWhenUserHasAuthority() {
        val userWithAuthorities = User()
        val required = HashSet<Authority>()
        required.add(Authority.REVIEW_RESEARCH_PURPOSE)
        userWithAuthorities.authoritiesEnum = required
        `when`(userDao!!.findUserWithAuthorities(USER_ID)).thenReturn(userWithAuthorities)
        val apiControllerMethod = FakeApiController::class.java.getMethod("handle")
        assertThat(interceptor!!.hasRequiredAuthority(apiControllerMethod, user)).isTrue()
    }

    companion object {

        private val USER_ID = 123L
    }
}
