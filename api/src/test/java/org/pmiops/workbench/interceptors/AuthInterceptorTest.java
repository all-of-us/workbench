package org.pmiops.workbench.interceptors;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.client.http.HttpMethods;
import com.google.api.client.http.HttpResponseException;
import com.google.api.services.oauth2.model.Userinfoplus;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.HttpHeaders;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.pmiops.workbench.annotations.AuthorityRequired;
import org.pmiops.workbench.api.ProfileApi;
import org.pmiops.workbench.auth.UserInfoService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.config.WorkbenchConfig.GoogleDirectoryServiceConfig;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.firecloud.ApiException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.Me;
import org.pmiops.workbench.firecloud.model.UserInfo;
import org.pmiops.workbench.model.Authority;
import org.pmiops.workbench.test.Providers;
import org.springframework.web.method.HandlerMethod;


/** mimicing a Swagger-generated wrapper */
class FakeApiController {
  public void handle() {}
}

/** mimicing our implementation, annotated */
class FakeController {
  @AuthorityRequired({Authority.REVIEW_RESEARCH_PURPOSE})
  public void handle() {}
}

public class AuthInterceptorTest {

  private static final long USER_ID = 123L;

  @Mock
  private UserInfoService userInfoService;
  @Mock
  private FireCloudService fireCloudService;
  @Mock
  private UserDao userDao;
  @Mock
  private HttpServletRequest request;
  @Mock
  private HttpServletResponse response;
  @Mock
  private HandlerMethod handler;
  @Mock
  private UserService userService;

  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule();

  private AuthInterceptor interceptor;
  private User user;
  private User userWithAuthorities;

  @Before
  public void setup() {
    WorkbenchConfig workbenchConfig = new WorkbenchConfig();
    workbenchConfig.googleDirectoryService = new GoogleDirectoryServiceConfig();
    workbenchConfig.googleDirectoryService.gSuiteDomain = "fake-domain.org";
    this.interceptor = new AuthInterceptor(userInfoService, fireCloudService,
        Providers.of(workbenchConfig), userDao, userService);
    this.user = new User();
    user.setUserId(USER_ID);
    user.setDisabled(false);
  }

  @Test
  public void preHandleOptions_OPTIONS() throws Exception {
    when(request.getMethod()).thenReturn(HttpMethods.OPTIONS);
    assertThat(interceptor.preHandle(request, response, handler)).isTrue();
  }

  @Test
  public void preHandleOptions_publicEndpoint() throws Exception {
    when(handler.getMethod()).thenReturn(getProfileApiMethod("isUsernameTaken"));
    when(request.getMethod()).thenReturn(HttpMethods.GET);
    assertThat(interceptor.preHandle(request, response, handler)).isTrue();
  }

  @Test
  public void preHandleGet_noAuthorization() throws Exception {
    when(handler.getMethod()).thenReturn(getProfileApiMethod("getBillingProjects"));
    when(request.getMethod()).thenReturn(HttpMethods.GET);
    assertThat(interceptor.preHandle(request, response, handler)).isFalse();
    verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED);
  }

  @Test
  public void preHandleGet_nonBearerAuthorization() throws Exception {
    when(handler.getMethod()).thenReturn(getProfileApiMethod("getBillingProjects"));
    when(request.getMethod()).thenReturn(HttpMethods.GET);
    when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("blah");
    assertThat(interceptor.preHandle(request, response, handler)).isFalse();
    verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED);
  }

  @Test
  public void preHandleGet_userInfoError() throws Exception {
    when(handler.getMethod()).thenReturn(getProfileApiMethod("getBillingProjects"));
    when(request.getMethod()).thenReturn(HttpMethods.GET);
    when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer foo");
    when(userInfoService.getUserInfo("foo")).thenThrow(
        new HttpResponseException.Builder(404, null,
            new com.google.api.client.http.HttpHeaders())
        .build());
    assertThat(interceptor.preHandle(request, response, handler)).isFalse();
    verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED);
  }

  @Test
  public void preHandleGet_firecloudLookupFails() throws Exception {
    when(handler.getMethod()).thenReturn(getProfileApiMethod("getBillingProjects"));
    when(request.getMethod()).thenReturn(HttpMethods.GET);
    when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer foo");
    Userinfoplus userInfo = new Userinfoplus();
    userInfo.setEmail("bob@bad-domain.org");
    when(userInfoService.getUserInfo("foo")).thenReturn(userInfo);
    when(fireCloudService.getMe()).thenThrow(
        new ApiException(HttpServletResponse.SC_NOT_FOUND, "blah"));
    assertThat(interceptor.preHandle(request, response, handler)).isFalse();
    verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
  }

  @Test
  public void preHandleGet_firecloudLookupSucceeds() throws Exception {
    when(handler.getMethod()).thenReturn(getProfileApiMethod("getBillingProjects"));
    when(request.getMethod()).thenReturn(HttpMethods.GET);
    when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer foo");
    Userinfoplus userInfo = new Userinfoplus();
    userInfo.setEmail("bob@bad-domain.org");
    when(userInfoService.getUserInfo("foo")).thenReturn(userInfo);
    UserInfo fcUserInfo = new UserInfo();
    fcUserInfo.setUserEmail("bob@fake-domain.org");
    Me me = new Me();
    me.setUserInfo(fcUserInfo);
    when(fireCloudService.getMe()).thenReturn(me);
    when(userDao.findUserByEmail("bob@fake-domain.org")).thenReturn(user);
    assertThat(interceptor.preHandle(request, response, handler)).isTrue();
  }

  @Test
  public void preHandleGet_firecloudLookupSucceedsNoUserRecordWrongDomain() throws Exception {
    when(handler.getMethod()).thenReturn(getProfileApiMethod("getBillingProjects"));
    when(request.getMethod()).thenReturn(HttpMethods.GET);
    when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer foo");
    Userinfoplus userInfo = new Userinfoplus();
    userInfo.setEmail("bob@bad-domain.org");
    when(userInfoService.getUserInfo("foo")).thenReturn(userInfo);
    UserInfo fcUserInfo = new UserInfo();
    fcUserInfo.setUserEmail("bob@also-bad-domain.org");
    Me me = new Me();
    me.setUserInfo(fcUserInfo);
    when(fireCloudService.getMe()).thenReturn(me);
    when(userDao.findUserByEmail("bob@also-bad-domain.org")).thenReturn(null);
    assertThat(interceptor.preHandle(request, response, handler)).isFalse();
    verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
  }

  @Test
  public void preHandleGet_userInfoSuccess() throws Exception {
    when(handler.getMethod()).thenReturn(getProfileApiMethod("getBillingProjects"));
    when(request.getMethod()).thenReturn(HttpMethods.GET);
    when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer foo");
    Userinfoplus userInfo = new Userinfoplus();
    userInfo.setEmail("bob@fake-domain.org");
    when(userInfoService.getUserInfo("foo")).thenReturn(userInfo);
    when(userDao.findUserByEmail("bob@fake-domain.org")).thenReturn(user);
    assertThat(interceptor.preHandle(request, response, handler)).isTrue();
  }

  @Test
  public void preHandleGet_noUserRecord() throws Exception {
    when(handler.getMethod()).thenReturn(getProfileApiMethod("getBillingProjects"));
    when(request.getMethod()).thenReturn(HttpMethods.GET);
    when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer foo");
    Userinfoplus userInfo = new Userinfoplus();
    userInfo.setGivenName("Bob");
    userInfo.setFamilyName("Jones");
    userInfo.setEmail("bob@fake-domain.org");
    when(userInfoService.getUserInfo("foo")).thenReturn(userInfo);
    when(userDao.findUserByEmail("bob@fake-domain.org")).thenReturn(null);
    when(userService.createUser("Bob", "Jones", "bob@fake-domain.org", null))
        .thenReturn(user);
    assertThat(interceptor.preHandle(request, response, handler)).isTrue();
  }

  private Method getProfileApiMethod(String methodName) {
    for (Method method : ProfileApi.class.getDeclaredMethods()) {
      if (method.getName().equals(methodName)) {
        return method;
      }
    }
    throw new RuntimeException("Method \""+methodName+"\" not found");
  }

  @Test
  public void authorityCheckPermitsWithNoAnnotation() throws Exception {
    Method method = getProfileApiMethod("getBillingProjects");
    assertThat(interceptor.hasRequiredAuthority(method, new User())).isTrue();
  }

  @Test
  public void authorityCheckDeniesWhenUserMissingAuthority() throws Exception {
    Method apiControllerMethod = FakeController.class.getMethod("handle");
    when(userDao.findUserWithAuthorities(USER_ID)).thenReturn(user);
    assertThat(interceptor.hasRequiredAuthority(apiControllerMethod, user)).isFalse();
  }

  @Test
  public void authorityCheckPermitsWhenUserHasAuthority() throws Exception {
    User userWithAuthorities = new User();
    Set<Authority> required = new HashSet<Authority>();
    required.add(Authority.REVIEW_RESEARCH_PURPOSE);
    userWithAuthorities.setAuthorities(required);
    when(userDao.findUserWithAuthorities(USER_ID)).thenReturn(userWithAuthorities);
    Method apiControllerMethod = FakeApiController.class.getMethod("handle");
    assertThat(interceptor.hasRequiredAuthority(apiControllerMethod, user)).isTrue();
  }
}
