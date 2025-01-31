package org.pmiops.workbench.interceptors;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.client.http.HttpMethods;
import com.google.api.services.oauth2.model.Userinfo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import org.apache.http.HttpHeaders;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.annotations.AuthorityRequired;
import org.pmiops.workbench.api.CloudTaskRdrExportApi;
import org.pmiops.workbench.api.ProfileApi;
import org.pmiops.workbench.api.UserAdminApiController;
import org.pmiops.workbench.api.UserAdminController;
import org.pmiops.workbench.auth.UserInfoService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudMe;
import org.pmiops.workbench.firecloud.model.FirecloudUserInfo;
import org.pmiops.workbench.model.Authority;
import org.pmiops.workbench.user.DevUserRegistrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.method.HandlerMethod;

/** mimicking a Swagger-generated wrapper */
class FakeApiController {
  public void handle() {}
}

/** mimicking our implementation, annotated */
class FakeController {
  @AuthorityRequired({Authority.SECURITY_ADMIN})
  public void handle() {}

  // Needed for tests that look for this method.
  public void getMe() {}
}

@SpringJUnitConfig
public class AuthInterceptorTest {

  private static final long USER_ID = 123L;

  @MockBean private UserInfoService userInfoService;
  @MockBean private FireCloudService fireCloudService;
  @MockBean private UserDao userDao;
  @MockBean private DevUserRegistrationService devUserRegistrationService;

  private static WorkbenchConfig workbenchConfig;

  @Mock private HttpServletRequest mockRequest;
  @Mock private HttpServletResponse mockResponse;
  @Mock private HandlerMethod mockHandler;
  private DbUser user;

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Autowired private AuthInterceptor interceptor;

  @TestConfiguration
  @Import({FakeClockConfiguration.class, AuthInterceptor.class})
  @MockBean({UserService.class})
  static class Configuration {
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public WorkbenchConfig getWorkbenchConfig() {
      return workbenchConfig;
    }
  }

  @BeforeEach
  public void setUp() {
    workbenchConfig = WorkbenchConfig.createEmptyConfig();
    workbenchConfig.googleDirectoryService.gSuiteDomain = "fake-domain.org";
    workbenchConfig.auth.serviceAccountApiUsers.add("service-account@appspot.gserviceaccount.com");
    workbenchConfig.server.apiBaseUrl = "api.test.fake-research-aou.org";
    workbenchConfig.access.enableApiUrlCheck = true;
    when(mockRequest.getRequestURL())
        .thenReturn(new StringBuffer("api.test.fake-research-aou.org"));

    user = new DbUser();
    user.setUserId(USER_ID);
    user.setDisabled(false);
  }

  // the particular ProfileApiMethod used by most tests
  private Method getTestMethod() {
    return getProfileApiMethod("getMe");
  }

  private void mockGetCallWithBearerToken() {
    Class mockClass = FakeController.class;
    when(mockHandler.getBeanType()).thenReturn(mockClass);
    when(mockHandler.getMethod()).thenReturn(getTestMethod());
    when(mockRequest.getMethod()).thenReturn(HttpMethods.GET);
    when(mockRequest.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer foo");
  }

  @Test
  public void preHandleOptions_OPTIONS() throws Exception {
    when(mockRequest.getMethod()).thenReturn(HttpMethods.OPTIONS);

    assertThat(interceptor.preHandle(mockRequest, mockResponse, mockHandler)).isTrue();
  }

  @Test
  public void preHandleOptions_publicEndpoint() throws Exception {
    when(mockHandler.getMethod()).thenReturn(getProfileApiMethod("isUsernameTaken"));
    when(mockRequest.getMethod()).thenReturn(HttpMethods.GET);

    assertThat(interceptor.preHandle(mockRequest, mockResponse, mockHandler)).isTrue();
  }

  @Test
  public void preHandleGet_noAuthorization() throws Exception {
    when(mockHandler.getMethod()).thenReturn(getTestMethod());
    when(mockRequest.getMethod()).thenReturn(HttpMethods.GET);

    assertThat(interceptor.preHandle(mockRequest, mockResponse, mockHandler)).isFalse();
    verify(mockResponse).sendError(HttpServletResponse.SC_UNAUTHORIZED);
  }

  @Test
  public void preHandleGet_nonBearerAuthorization() throws Exception {
    mockGetCallWithBearerToken();
    // Override the auth header to be an invalid bearer token.
    when(mockRequest.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("blah");

    assertThat(interceptor.preHandle(mockRequest, mockResponse, mockHandler)).isFalse();
    verify(mockResponse).sendError(HttpServletResponse.SC_UNAUTHORIZED);
  }

  @Test
  public void preHandleGet_usingServiceAccountNotInDb() throws Exception {
    mockGetCallWithBearerToken();
    Userinfo userInfo = new Userinfo();
    String serviceAccountEmail =
        workbenchConfig.auth.serviceAccountApiUsers.stream()
            .findFirst()
            .orElseThrow(
                () ->
                    new RuntimeException(
                        "WorkbenchConfig should contain a list of serviceAccountApiUsers"));
    userInfo.setEmail(serviceAccountEmail);
    when(userDao.findUserByUsername(serviceAccountEmail)).thenReturn(null);
    when(userInfoService.getUserInfo("foo")).thenReturn(userInfo);

    assertTrue(interceptor.preHandle(mockRequest, mockResponse, mockHandler));
  }

  @Test
  public void preHandleGet_usingServiceAccountInDb() throws Exception {
    mockGetCallWithBearerToken();
    Userinfo userInfo = new Userinfo();
    String serviceAccountEmail =
        workbenchConfig.auth.serviceAccountApiUsers.stream()
            .findFirst()
            .orElseThrow(
                () ->
                    new RuntimeException(
                        "WorkbenchConfig should contain a list of serviceAccountApiUsers"));
    userInfo.setEmail(serviceAccountEmail);
    when(userInfoService.getUserInfo("foo")).thenReturn(userInfo);
    when(userDao.findUserByUsername(serviceAccountEmail)).thenReturn(new DbUser());

    assertTrue(interceptor.preHandle(mockRequest, mockResponse, mockHandler));
  }

  @Test
  public void preHandleGet_userInfoError() {
    mockGetCallWithBearerToken();
    when(userInfoService.getUserInfo("foo")).thenThrow(new NotFoundException());

    assertThrows(
        NotFoundException.class,
        () -> interceptor.preHandle(mockRequest, mockResponse, mockHandler));
  }

  @Test
  public void preHandleGet_firecloudLookupFails() {
    mockGetCallWithBearerToken();

    Userinfo userInfo = new Userinfo();
    userInfo.setEmail("bob@bad-domain.org");
    when(userInfoService.getUserInfo("foo")).thenReturn(userInfo);
    when(fireCloudService.getMe()).thenThrow(new NotFoundException());

    assertThrows(
        NotFoundException.class,
        () -> interceptor.preHandle(mockRequest, mockResponse, mockHandler));
  }

  @Test
  public void preHandleGet_firecloudLookupSucceeds() throws Exception {
    mockGetCallWithBearerToken();
    Userinfo userInfo = new Userinfo();
    userInfo.setEmail("bob@bad-domain.org");
    when(userInfoService.getUserInfo("foo")).thenReturn(userInfo);
    FirecloudUserInfo fcUserInfo = new FirecloudUserInfo();
    fcUserInfo.setUserEmail("bob@fake-domain.org");
    FirecloudMe me = new FirecloudMe();
    me.setUserInfo(fcUserInfo);
    when(fireCloudService.getMe()).thenReturn(me);
    when(userDao.findUserByUsername("bob@fake-domain.org")).thenReturn(user);

    assertThat(interceptor.preHandle(mockRequest, mockResponse, mockHandler)).isTrue();
  }

  @Test
  public void preHandleGet_isVwbSA() throws Exception {
    mockGetCallWithBearerToken();
    workbenchConfig.vwb.exfilManagerServiceAccount = "exfil@vwb.org";
    Userinfo userInfo = new Userinfo();
    userInfo.setEmail("exfil@vwb.org");
    when(userInfoService.getUserInfo("foo")).thenReturn(userInfo);
    when(userDao.findUserByUsername("exfil@vwb.org")).thenReturn(new DbUser());
    when(mockRequest.getRequestURL())
        .thenReturn(new StringBuffer("api.test.fake-research-aou.org"));
    when(mockRequest.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer foo");

    assertThat(interceptor.preHandle(mockRequest, mockResponse, mockHandler)).isTrue();
  }

  @Test
  public void preHandleGet_firecloudLookupSucceedsNoUserRecordWrongDomain() throws Exception {
    mockGetCallWithBearerToken();
    Userinfo userInfo = new Userinfo();
    userInfo.setEmail("bob@bad-domain.org");
    when(userInfoService.getUserInfo("foo")).thenReturn(userInfo);
    FirecloudUserInfo fcUserInfo = new FirecloudUserInfo();
    fcUserInfo.setUserEmail("bob@also-bad-domain.org");
    FirecloudMe me = new FirecloudMe();
    me.setUserInfo(fcUserInfo);
    when(fireCloudService.getMe()).thenReturn(me);
    when(userDao.findUserByUsername("bob@also-bad-domain.org")).thenReturn(null);

    assertThat(interceptor.preHandle(mockRequest, mockResponse, mockHandler)).isFalse();
    verify(mockResponse).sendError(HttpServletResponse.SC_UNAUTHORIZED);
  }

  private void mockUserInfoSuccess() {
    Userinfo userInfo = new Userinfo();
    userInfo.setEmail("bob@fake-domain.org");
    when(userInfoService.getUserInfo("foo")).thenReturn(userInfo);
    when(userDao.findUserByUsername("bob@fake-domain.org")).thenReturn(user);
  }

  @Test
  public void preHandleGet_userInfoSuccess() throws Exception {
    mockGetCallWithBearerToken();
    mockUserInfoSuccess();

    assertThat(interceptor.preHandle(mockRequest, mockResponse, mockHandler)).isTrue();
  }

  @Test
  public void preHandleGet_disabledUser() {
    mockGetCallWithBearerToken();
    mockUserInfoSuccess();
    user.setUsername("expiredUser");
    user.setDisabled(true);

    Throwable exception =
        Assert.assertThrows(
            ForbiddenException.class,
            () -> interceptor.preHandle(mockRequest, mockResponse, mockHandler));
    assertEquals(
        "Rejecting request for disabled user account: expiredUser", exception.getMessage());
  }

  @Test
  public void preHandleGet_noUserRecord() throws Exception {
    workbenchConfig.access.unsafeAllowUserCreationFromGSuiteData = true;
    // Tests the flow where userDao doesn't contain a row for the authorized user.
    // When this functionality is enabled, the auth interceptor will lazily create a new
    // user record when none is found for the given G Suite user.
    mockGetCallWithBearerToken();
    mockUserInfoSuccess();
    // Override the userDao mock to return a null record.
    when(userDao.findUserByUsername(eq("bob@fake-domain.org"))).thenReturn(null);
    when(devUserRegistrationService.createUser(any())).thenReturn(user);

    assertThat(interceptor.preHandle(mockRequest, mockResponse, mockHandler)).isTrue();
  }

  @Test
  public void preHandleGet_noUserRecordAndNoDevRegistration() throws Exception {
    workbenchConfig.access.unsafeAllowUserCreationFromGSuiteData = false;
    mockGetCallWithBearerToken();
    mockUserInfoSuccess();
    when(userDao.findUserByUsername(eq("bob@fake-domain.org"))).thenReturn(null);

    assertThat(interceptor.preHandle(mockRequest, mockResponse, mockHandler)).isFalse();

    verify(devUserRegistrationService, never()).createUser(any());
  }

  @Test
  public void preHandleGet_missingAuthority() throws Exception {
    DbUser userWithWrongAuthorities =
        new DbUser().setAuthoritiesEnum(Collections.singleton(Authority.COMMUNICATIONS_ADMIN));
    when(userDao.findUserWithAuthorities(USER_ID)).thenReturn(userWithWrongAuthorities);

    mockGetCallWithBearerToken();
    mockUserInfoSuccess();
    Class<?> apiControllerClass = UserAdminApiController.class;
    Class<?>[] parameterTypes = {};
    String methodName = "getAllUsers";
    Method method = apiControllerClass.getMethod(methodName, parameterTypes);
    UserAdminApiController userAdminApiController =
        new UserAdminApiController(mock(UserAdminController.class));
    HandlerMethod handlerMethod = new HandlerMethod(userAdminApiController, method);

    assertThat(interceptor.preHandle(mockRequest, mockResponse, handlerMethod)).isFalse();
    verify(mockResponse).sendError(HttpServletResponse.SC_FORBIDDEN);
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
  public void authorityCheckPermitsWithNoAnnotation() {
    assertThat(interceptor.hasRequiredAuthority(getTestMethod(), new DbUser())).isTrue();
  }

  @Test
  public void authorityCheckPermitsWithMissingUser() throws Exception {
    Method apiControllerMethod = FakeController.class.getMethod("handle");
    Throwable exception =
        Assert.assertThrows(
            BadRequestException.class,
            () -> interceptor.hasRequiredAuthority(apiControllerMethod, null));
    assertEquals("User is not initialized; please register", exception.getMessage());
  }

  @Test
  public void authorityCheckDeniesWhenUserMissingAuthority() throws Exception {
    Method apiControllerMethod = FakeController.class.getMethod("handle");
    when(userDao.findUserWithAuthorities(USER_ID)).thenReturn(user);
    assertThat(interceptor.hasRequiredAuthority(apiControllerMethod, user)).isFalse();
  }

  @Test
  public void authorityCheckDeniesWhenUserHasWrongAuthority() throws Exception {
    DbUser userWithWrongAuthorities =
        new DbUser().setAuthoritiesEnum(Collections.singleton(Authority.COMMUNICATIONS_ADMIN));
    when(userDao.findUserWithAuthorities(USER_ID)).thenReturn(userWithWrongAuthorities);
    Method apiControllerMethod = FakeController.class.getMethod("handle");
    assertThat(interceptor.hasRequiredAuthority(apiControllerMethod, user)).isFalse();
  }

  @Test
  public void authorityCheckPermitsWhenUserHasAuthority() throws Exception {
    DbUser userWithAuthorities =
        new DbUser().setAuthoritiesEnum(Collections.singleton(Authority.SECURITY_ADMIN));
    when(userDao.findUserWithAuthorities(USER_ID)).thenReturn(userWithAuthorities);
    Method apiControllerMethod = FakeApiController.class.getMethod("handle");
    assertThat(interceptor.hasRequiredAuthority(apiControllerMethod, user)).isTrue();
  }

  @Test
  public void authorityCheckPermitsWhenUserHasDeveloperAuthority() throws Exception {
    DbUser userWithDeveloperAuthorities =
        new DbUser()
            .setUserId(USER_ID)
            .setAuthoritiesEnum(Collections.singleton(Authority.DEVELOPER));
    when(userDao.findUserWithAuthorities(USER_ID)).thenReturn(userWithDeveloperAuthorities);

    Method apiControllerMethod = FakeController.class.getMethod("handle");

    assertThat(interceptor.hasRequiredAuthority(apiControllerMethod, userWithDeveloperAuthorities))
        .isTrue();
  }

  @Test
  public void authorityCheckPermitsWhenUserHasAllAppropriateAuthority() throws Exception {
    DbUser userWithDeveloperAuthorities =
        new DbUser()
            .setUserId(USER_ID)
            .setAuthoritiesEnum(Collections.singleton(Authority.SECURITY_ADMIN));
    when(userDao.findUserWithAuthorities(USER_ID)).thenReturn(userWithDeveloperAuthorities);

    Method apiControllerMethod = FakeController.class.getMethod("handle");

    assertThat(interceptor.hasRequiredAuthority(apiControllerMethod, userWithDeveloperAuthorities))
        .isTrue();
  }

  @Test
  public void preHandle_apiBaseUrlNotMatch_forbidden() throws Exception {
    when(mockHandler.getMethod()).thenReturn(getTestMethod());
    when(mockRequest.getMethod()).thenReturn(HttpMethods.GET);

    when(mockRequest.getRequestURL()).thenReturn(new StringBuffer("domain"));

    assertThat(interceptor.preHandle(mockRequest, mockResponse, mockHandler)).isFalse();
    verify(mockResponse).sendError(HttpServletResponse.SC_FORBIDDEN);
  }

  @Test
  public void preHandle_apiBaseUrlNotMatch_cloudTask_allowed() throws Exception {
    when(mockRequest.getMethod()).thenReturn(HttpMethods.GET);
    when(mockHandler.getMethod())
        .thenReturn(
            CloudTaskRdrExportApi.class.getMethod(
                "exportResearcherDataBatch", List.class, Boolean.class));

    when(mockRequest.getRequestURL()).thenReturn(new StringBuffer("domain"));

    assertThat(interceptor.preHandle(mockRequest, mockResponse, mockHandler)).isTrue();
  }

  @Test
  public void preHandle_apiBaseUrlNotMatch_disableUrlCheck_allowed() throws Exception {
    workbenchConfig.access.enableApiUrlCheck = false;

    mockGetCallWithBearerToken();
    mockUserInfoSuccess();
    when(mockRequest.getRequestURL()).thenReturn(new StringBuffer("domain"));

    assertThat(interceptor.preHandle(mockRequest, mockResponse, mockHandler)).isTrue();
  }
}
