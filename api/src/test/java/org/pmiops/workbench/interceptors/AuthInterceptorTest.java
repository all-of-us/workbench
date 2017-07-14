package org.pmiops.workbench.interceptors;


import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static com.google.common.truth.Truth.assertThat;

import com.google.api.client.http.HttpMethods;
import com.google.api.client.http.HttpResponseException;
import com.google.api.services.oauth2.model.Userinfoplus;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.HttpHeaders;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.pmiops.workbench.auth.UserInfoService;

public class AuthInterceptorTest {

  @Mock
  private UserInfoService userInfoService;
  @Mock
  private HttpServletRequest request;
  @Mock
  private HttpServletResponse response;
  @Mock
  private Object handler;

  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule();

  private AuthInterceptor interceptor;

  @Before
  public void setup() {
    interceptor = new AuthInterceptor(userInfoService);
  }

  @Test
  public void preHandleOptions_noAuthRequired() throws Exception {
    when(request.getMethod()).thenReturn(HttpMethods.OPTIONS);
    assertThat(interceptor.preHandle(request, response, handler)).isTrue();

  }

  @Test
  public void preHandleGet_noAuthorization() throws Exception {
    when(request.getMethod()).thenReturn(HttpMethods.GET);
    assertThat(interceptor.preHandle(request, response, handler)).isFalse();
    verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED);
  }

  @Test
  public void preHandleGet_nonBearerAuthorization() throws Exception {
    when(request.getMethod()).thenReturn(HttpMethods.GET);
    when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("blah");
    assertThat(interceptor.preHandle(request, response, handler)).isFalse();
    verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED);
  }

  @Test
  public void preHandleGet_userInfoError() throws Exception {
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
  public void preHandleGet_userInfoSuccess() throws Exception {
    when(request.getMethod()).thenReturn(HttpMethods.GET);
    when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer foo");
    when(userInfoService.getUserInfo("foo")).thenReturn(new Userinfoplus());
    assertThat(interceptor.preHandle(request, response, handler)).isTrue();
  }
}
