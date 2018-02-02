package org.pmiops.workbench.interceptors;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

import com.google.api.client.http.HttpMethods;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.pmiops.workbench.api.AuditApi;
import org.pmiops.workbench.api.WorkspacesApi;
import org.springframework.web.method.HandlerMethod;


public class CronInterceptorTest {
  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock
  private HandlerMethod handler;
  @Mock
  private HttpServletRequest request;
  @Mock
  private HttpServletResponse response;

  private CronInterceptor interceptor;

  @Before
  public void setup() {
    interceptor = new CronInterceptor();
  }

  @Test
  public void preHandleOptions_OPTIONS() throws Exception {
    when(request.getMethod()).thenReturn(HttpMethods.OPTIONS);
    assertThat(interceptor.preHandle(request, response, handler)).isTrue();
  }

  @Test
  public void prehandleForCronNoHeader() throws Exception {
    when(request.getMethod()).thenReturn(HttpMethods.GET);
    when(handler.getMethod()).thenReturn(AuditApi.class.getMethod("auditBigQuery"));
    assertThat(interceptor.preHandle(request, response, handler)).isFalse();
  }

  @Test
  public void prehandleForCronWithBadHeader() throws Exception {
    when(request.getMethod()).thenReturn(HttpMethods.GET);
    when(handler.getMethod()).thenReturn(AuditApi.class.getMethod("auditBigQuery"));
    when(request.getHeader(CronInterceptor.GAE_CRON_HEADER)).thenReturn("asdf");
    assertThat(interceptor.preHandle(request, response, handler)).isFalse();
  }

  @Test
  public void prehandleForCronWithHeader() throws Exception {
    when(request.getMethod()).thenReturn(HttpMethods.GET);
    when(handler.getMethod()).thenReturn(AuditApi.class.getMethod("auditBigQuery"));
    when(request.getHeader(CronInterceptor.GAE_CRON_HEADER)).thenReturn("true");
    assertThat(interceptor.preHandle(request, response, handler)).isTrue();
  }

  @Test
  public void prehandleForNonCron() throws Exception {
    when(request.getMethod()).thenReturn(HttpMethods.GET);
    when(handler.getMethod()).thenReturn(WorkspacesApi.class.getMethod("getWorkspaces"));
    assertThat(interceptor.preHandle(request, response, handler)).isTrue();
  }
}
