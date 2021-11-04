package org.pmiops.workbench.interceptors;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

import com.google.api.client.http.HttpMethods;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.api.CloudTaskRdrExportApi;
import org.pmiops.workbench.api.WorkspacesApi;
import org.pmiops.workbench.model.ArrayOfLong;
import org.springframework.context.annotation.Import;
import org.springframework.web.method.HandlerMethod;

@Import(FakeClockConfiguration.class)
public class CloudTaskInterceptorTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock private HandlerMethod handler;
  @Mock private HttpServletRequest request;
  @Mock private HttpServletResponse response;

  private CloudTaskInterceptor interceptor;

  private final String CLOUD_TASK_METHOD_NAME = "exportResearcherData";

  @BeforeEach
  public void setUp() {
    interceptor = new CloudTaskInterceptor();
  }

  @Test
  public void preHandleOptions_OPTIONS() throws Exception {
    when(request.getMethod()).thenReturn(HttpMethods.OPTIONS);
    assertThat(interceptor.preHandle(request, response, handler)).isTrue();
  }

  @Test
  public void prehandleForCloudTaskNoHeader() throws Exception {
    when(request.getMethod()).thenReturn(HttpMethods.POST);
    when(handler.getMethod())
        .thenReturn(
            CloudTaskRdrExportApi.class.getMethod(CLOUD_TASK_METHOD_NAME, ArrayOfLong.class));
    assertThat(interceptor.preHandle(request, response, handler)).isFalse();
  }

  @Test
  public void prehandleForCloudTaskWithHeader() throws Exception {
    when(request.getMethod()).thenReturn(HttpMethods.POST);
    when(handler.getMethod())
        .thenReturn(
            CloudTaskRdrExportApi.class.getMethod(CLOUD_TASK_METHOD_NAME, ArrayOfLong.class));
    when(request.getHeader(CloudTaskInterceptor.QUEUE_NAME_REQUEST_HEADER))
        .thenReturn("rdrExportQueue");
    assertThat(interceptor.preHandle(request, response, handler)).isTrue();
  }

  @Test
  public void prehandleForNonCloudTask() throws Exception {
    when(request.getMethod()).thenReturn(HttpMethods.GET);
    when(handler.getMethod()).thenReturn(WorkspacesApi.class.getMethod("getWorkspaces"));
    assertThat(interceptor.preHandle(request, response, handler)).isTrue();
  }
}
