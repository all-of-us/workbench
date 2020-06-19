package org.pmiops.workbench.interceptors;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.client.http.HttpMethods;
import com.google.api.services.oauth2.model.Userinfoplus;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.HttpHeaders;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.pmiops.workbench.annotations.AuthorityRequired;
import org.pmiops.workbench.api.ProfileApi;
import org.pmiops.workbench.auth.UserInfoService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.config.WorkbenchConfig.AuthConfig;
import org.pmiops.workbench.config.WorkbenchConfig.GoogleDirectoryServiceConfig;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudMe;
import org.pmiops.workbench.firecloud.model.FirecloudUserInfo;
import org.pmiops.workbench.google.DirectoryService;
import org.pmiops.workbench.model.Authority;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.test.context.junit4.SpringRunner;
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

@RunWith(SpringRunner.class)
public class AuthInterceptorTest {

  private static final long USER_ID = 123L;

  @MockBean private UserInfoService userInfoService;
  @MockBean private FireCloudService fireCloudService;
  @MockBean private UserDao userDao;
  @MockBean private UserService userService;
  @MockBean private DirectoryService directoryService;

  private static WorkbenchConfig workbenchConfig;

  @Mock private HttpServletRequest request;
  @Mock private HttpServletResponse response;
  @Mock private HandlerMethod handler;
  private DbUser user;

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Autowired private AuthInterceptor interceptor;

  @TestConfiguration
  @Import({AuthInterceptor.class})
  static class Configuration {
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public WorkbenchConfig getWorkbenchConfig() {
      return workbenchConfig;
    }
  }

  @Before
  public void setUp() {
    workbenchConfig = WorkbenchConfig.createEmptyConfig();
    workbenchConfig.googleDirectoryService = new GoogleDirectoryServiceConfig();
    workbenchConfig.googleDirectoryService.gSuiteDomain = "fake-domain.org";
    workbenchConfig.auth = new AuthConfig();
    workbenchConfig.auth.serviceAccountApiUsers = new ArrayList<>();
    workbenchConfig.auth.serviceAccountApiUsers.add("service-account@appspot.gserviceaccount.com");
    user = new DbUser();
    user.setUserId(USER_ID);
    user.setDisabled(false);
  }

  private void mockGetCallWithBearerToken() {
    when(handler.getMethod()).thenReturn(getProfileApiMethod("getBillingProjects"));
    when(request.getMethod()).thenReturn(HttpMethods.GET);
    when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer foo");
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
    mockGetCallWithBearerToken();
    // Override the auth header to be an invalid bearer token.
    when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("blah");

    assertThat(interceptor.preHandle(request, response, handler)).isFalse();
    verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED);
  }

  @Test(expected = NotFoundException.class)
  public void preHandleGet_userInfoError() throws Exception {
    mockGetCallWithBearerToken();
    when(userInfoService.getUserInfo("foo")).thenThrow(new NotFoundException());

    interceptor.preHandle(request, response, handler);
  }

  @Test(expected = NotFoundException.class)
  public void preHandleGet_firecloudLookupFails() throws Exception {
    mockGetCallWithBearerToken();

    Userinfoplus userInfo = new Userinfoplus();
    userInfo.setEmail("bob@bad-domain.org");
    when(userInfoService.getUserInfo("foo")).thenReturn(userInfo);
    when(fireCloudService.getMe()).thenThrow(new NotFoundException());

    interceptor.preHandle(request, response, handler);
  }

  @Test
  public void preHandleGet_firecloudLookupSucceeds() throws Exception {
    mockGetCallWithBearerToken();
    Userinfoplus userInfo = new Userinfoplus();
    userInfo.setEmail("bob@bad-domain.org");
    when(userInfoService.getUserInfo("foo")).thenReturn(userInfo);
    FirecloudUserInfo fcUserInfo = new FirecloudUserInfo();
    fcUserInfo.setUserEmail("bob@fake-domain.org");
    FirecloudMe me = new FirecloudMe();
    me.setUserInfo(fcUserInfo);
    when(fireCloudService.getMe()).thenReturn(me);
    when(userDao.findUserByUsername("bob@fake-domain.org")).thenReturn(user);

    assertThat(interceptor.preHandle(request, response, handler)).isTrue();
  }

  @Test
  public void preHandleGet_firecloudLookupSucceedsNoUserRecordWrongDomain() throws Exception {
    mockGetCallWithBearerToken();
    Userinfoplus userInfo = new Userinfoplus();
    userInfo.setEmail("bob@bad-domain.org");
    when(userInfoService.getUserInfo("foo")).thenReturn(userInfo);
    FirecloudUserInfo fcUserInfo = new FirecloudUserInfo();
    fcUserInfo.setUserEmail("bob@also-bad-domain.org");
    FirecloudMe me = new FirecloudMe();
    me.setUserInfo(fcUserInfo);
    when(fireCloudService.getMe()).thenReturn(me);
    when(userDao.findUserByUsername("bob@also-bad-domain.org")).thenReturn(null);

    assertThat(interceptor.preHandle(request, response, handler)).isFalse();
    verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED);
  }

  private void mockUserInfoSuccess() {
    Userinfoplus userInfo = new Userinfoplus();
    userInfo.setEmail("bob@fake-domain.org");
    when(userInfoService.getUserInfo("foo")).thenReturn(userInfo);
    when(userDao.findUserByUsername("bob@fake-domain.org")).thenReturn(user);
  }

  @Test
  public void preHandleGet_userInfoSuccess() throws Exception {
    mockGetCallWithBearerToken();
    mockUserInfoSuccess();

    assertThat(interceptor.preHandle(request, response, handler)).isTrue();
  }

  @Test
  public void preHandleGet_noUserRecord() throws Exception {
    // Tests the flow where userDao doesn't contain a row for the authorized user.
    // When this functionality is enabled, the auth interceptor will lazily create a new
    // user record when none is found for the given G Suite user.
    mockGetCallWithBearerToken();
    mockUserInfoSuccess();
    // Override the userDao mock to return a null record.
    when(userDao.findUserByUsername(eq("bob@fake-domain.org"))).thenReturn(null);
    when(directoryService.getContactEmailAddress(eq("bob@fake-domain.org")))
        .thenReturn("bob@gmail.com");
    when(userService.createUser(any(), eq("bob@gmail.com"))).thenReturn(user);

    assertThat(interceptor.preHandle(request, response, handler)).isTrue();
  }

  @Test
  public void preHandleGet_noUserRecordAndNoContactEmail() throws Exception {
    mockGetCallWithBearerToken();
    mockUserInfoSuccess();
    when(userDao.findUserByUsername(eq("bob@fake-domain.org"))).thenReturn(null);
    when(directoryService.getContactEmailAddress(eq("bob@fake-domain.org")))
        .thenThrow(new NullPointerException());
    // When GSuite doesn't have the contact email stored, we should fall back to the RW username
    // as contact email address.s
    when(userService.createUser(any(), eq("bob@fake-domain.org"))).thenReturn(user);

    assertThat(interceptor.preHandle(request, response, handler)).isTrue();
  }

  private Method getProfileApiMethod(String methodName) {
    for (Method method : ProfileApi.class.getDeclaredMethods()) {
      if (method.getName().equals(methodName)) {
        return method;
      }
    }
    throw new RuntimeException("Method \"" + methodName + "\" not found");
  }

  @Test
  public void authorityCheckPermitsWithNoAnnotation() throws Exception {
    Method method = getProfileApiMethod("getBillingProjects");
    assertThat(interceptor.hasRequiredAuthority(method, new DbUser())).isTrue();
  }

  @Test
  public void authorityCheckDeniesWhenUserMissingAuthority() throws Exception {
    Method apiControllerMethod = FakeController.class.getMethod("handle");
    when(userDao.findUserWithAuthorities(USER_ID)).thenReturn(user);
    assertThat(interceptor.hasRequiredAuthority(apiControllerMethod, user)).isFalse();
  }

  @Test
  public void authorityCheckPermitsWhenUserHasAuthority() throws Exception {
    DbUser userWithAuthorities = new DbUser();
    Set<Authority> required = new HashSet<>();
    required.add(Authority.REVIEW_RESEARCH_PURPOSE);
    userWithAuthorities.setAuthoritiesEnum(required);
    when(userDao.findUserWithAuthorities(USER_ID)).thenReturn(userWithAuthorities);
    Method apiControllerMethod = FakeApiController.class.getMethod("handle");
    assertThat(interceptor.hasRequiredAuthority(apiControllerMethod, user)).isTrue();
  }
}
