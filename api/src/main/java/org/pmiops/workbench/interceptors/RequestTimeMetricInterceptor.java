package org.pmiops.workbench.interceptors;

import com.google.api.client.http.HttpMethods;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.pmiops.workbench.monitoring.LogsBasedMetricService;
import org.pmiops.workbench.monitoring.MeasurementBundle;
import org.pmiops.workbench.monitoring.labels.MetricLabel;
import org.pmiops.workbench.monitoring.views.DistributionMetric;
import org.springframework.stereotype.Service;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

@Service
public class RequestTimeMetricInterceptor extends HandlerInterceptorAdapter {

  private final LogsBasedMetricService logsBasedMetricService;
  private Clock clock;

  public RequestTimeMetricInterceptor(LogsBasedMetricService logsBasedMetricService, Clock clock) {
    this.logsBasedMetricService = logsBasedMetricService;
    this.clock = clock;
  }

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    if (shouldSkip(request, handler)) {
      return true;
    }
    request.setAttribute(RequestAttribute.START_INSTANT.toString(), clock.instant());
    return true;
  }

  private boolean shouldSkip(HttpServletRequest request, Object handler) {
    return (request.getMethod().equals(HttpMethods.OPTIONS))
        || (!(handler instanceof HandlerMethod));
  }

  @Override
  public void postHandle(
      HttpServletRequest request,
      HttpServletResponse response,
      Object handler,
      ModelAndView modelAndView) {
    if (shouldSkip(request, handler)) {
      return;
    }

    final String methodName = ((HandlerMethod) handler).getMethod().getName();

    Optional.ofNullable(request.getAttribute(RequestAttribute.START_INSTANT.getKeyName()))
        .map(obj -> (Instant) obj)
        .map(start -> Duration.between(start, clock.instant()))
        .map(Duration::toMillis)
        .ifPresent(
            elapsedMillis ->
                logsBasedMetricService.record(
                    MeasurementBundle.builder()
                        .addMeasurement(DistributionMetric.API_METHOD_TIME, elapsedMillis)
                        .addTag(MetricLabel.METHOD_NAME, methodName)
                        .build()));
  }
}
