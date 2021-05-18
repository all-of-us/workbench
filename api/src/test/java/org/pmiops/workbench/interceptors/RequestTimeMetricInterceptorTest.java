package org.pmiops.workbench.interceptors;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import com.google.api.client.http.HttpMethods;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.pmiops.workbench.monitoring.LogsBasedMetricService;
import org.pmiops.workbench.monitoring.MeasurementBundle;
import org.pmiops.workbench.monitoring.labels.MetricLabel;
import org.pmiops.workbench.monitoring.views.DistributionMetric;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;

@RunWith(SpringRunner.class)
public class RequestTimeMetricInterceptorTest {

  private static final Instant START_INSTANT = Instant.parse("2007-01-03T00:00:00.00Z");
  private static final long DURATION_MILLIS = 1500L;
  private static final Instant END_INSTANT = START_INSTANT.plusMillis(DURATION_MILLIS);
  private static final String METHOD_NAME = "frobnicate";

  @Mock private HttpServletRequest mockHttpServletRequest;
  @Mock private HttpServletResponse mockHttpServletResponse;
  @Mock private HandlerMethod mockHandlerMethod;
  @Mock private Method mockMethod;
  @Mock private ModelAndView mockModelAndView;

  @MockBean private Clock mockClock;
  @MockBean private LogsBasedMetricService mockLogsBasedMetricService;

  @Captor private ArgumentCaptor<String> attributeKeyCaptor;
  @Captor private ArgumentCaptor<Object> attributeValueCaptor;
  @Captor private ArgumentCaptor<MeasurementBundle> measurementBundleCaptor;

  @Autowired private RequestTimeMetricInterceptor requestTimeMetricInterceptor;

  @TestConfiguration
  @Import({RequestTimeMetricInterceptor.class})
  public static class Config {}

  @Before
  public void setup() {
    doReturn(START_INSTANT).when(mockClock).instant();
    doReturn(HttpMethods.GET).when(mockHttpServletRequest).getMethod();
    doReturn(METHOD_NAME).when(mockMethod).getName();
    doReturn(mockMethod).when(mockHandlerMethod).getMethod();
  }

  @Test
  public void testPreHandle_getRequest() {
    boolean result =
        requestTimeMetricInterceptor.preHandle(
            mockHttpServletRequest, mockHttpServletResponse, mockHandlerMethod);
    assertThat(result).isTrue();
    verify(mockHttpServletRequest)
        .setAttribute(attributeKeyCaptor.capture(), attributeValueCaptor.capture());
    assertThat(attributeKeyCaptor.getValue())
        .isEqualTo(RequestAttribute.START_INSTANT.getKeyName());
    Object attrValueObj = attributeValueCaptor.getValue();
    assertThat(attrValueObj instanceof Instant).isTrue();
    //noinspection ConstantConditions
    assertThat((Instant) attrValueObj).isEqualTo(START_INSTANT);
  }

  @Test
  public void testPreHandle_skipsOptionsRequest() {
    doReturn(HttpMethods.OPTIONS).when(mockHttpServletRequest).getMethod();
    boolean result =
        requestTimeMetricInterceptor.preHandle(
            mockHttpServletRequest, mockHttpServletResponse, mockHandlerMethod);
    assertThat(result).isTrue();
    verify(mockHttpServletRequest, never()).setAttribute(anyString(), any());
  }

  @Test
  public void testPreHandle_skipsUnsupportedHandler() {
    boolean result =
        requestTimeMetricInterceptor.preHandle(
            mockHttpServletRequest, mockHttpServletResponse, new Object());
    assertThat(result).isTrue();
    verify(mockHttpServletRequest, never()).setAttribute(anyString(), any());
  }

  @Test
  public void testPostHandle() {
    doReturn(END_INSTANT).when(mockClock).instant();
    doReturn(START_INSTANT)
        .when(mockHttpServletRequest)
        .getAttribute(RequestAttribute.START_INSTANT.getKeyName());
    requestTimeMetricInterceptor.postHandle(
        mockHttpServletRequest, mockHttpServletResponse, mockHandlerMethod, mockModelAndView);
    verify(mockHttpServletRequest).getAttribute(RequestAttribute.START_INSTANT.getKeyName());

    verify(mockLogsBasedMetricService).record(measurementBundleCaptor.capture());
    assertThat(
            measurementBundleCaptor
                .getValue()
                .getMeasurements()
                .get(DistributionMetric.API_METHOD_TIME))
        .isEqualTo(DURATION_MILLIS);

    assertThat(measurementBundleCaptor.getValue().getTags()).hasSize(1);
    assertThat(measurementBundleCaptor.getValue().getTagValue(MetricLabel.METHOD_NAME).get())
        .isEqualTo(METHOD_NAME);
  }

  @Test
  public void testPostHandle_skipsOptionsRequest() {
    doReturn(HttpMethods.OPTIONS).when(mockHttpServletRequest).getMethod();
    requestTimeMetricInterceptor.postHandle(
        mockHttpServletRequest, mockHttpServletResponse, mockHandlerMethod, mockModelAndView);
    verify(mockHttpServletRequest, never()).setAttribute(anyString(), any());
    verifyZeroInteractions(mockLogsBasedMetricService);
  }

  @Test
  public void testPostHandle_skipsUnsupportedHandler() {
    requestTimeMetricInterceptor.postHandle(
        mockHttpServletRequest, mockHttpServletResponse, new Object(), mockModelAndView);
    verify(mockHttpServletRequest, never()).setAttribute(anyString(), any());
    verifyZeroInteractions(mockLogsBasedMetricService);
  }
}
