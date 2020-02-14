package org.pmiops.workbench.interceptors;

import com.google.common.base.Stopwatch;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.inject.Provider;
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
public class ElapsedTimeDistributionInterceptor extends HandlerInterceptorAdapter {

  private final LogsBasedMetricService logsBasedMetricService;
  private Clock clock;
  private final Map<HttpServletRequest, Instant> requestToStartTime = new HashMap<>();
  public ElapsedTimeDistributionInterceptor(LogsBasedMetricService logsBasedMetricService,
      Clock clock) {
    this.logsBasedMetricService = logsBasedMetricService;
    this.clock = clock;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    if (!(handler instanceof HandlerMethod)) {
      return true;
    }
    requestToStartTime.put(request, clock.instant());
    return true;
  }

  @Override
  public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
      ModelAndView modelAndView) {
    if (!(handler instanceof HandlerMethod)) {
      return;
    }

    final String methodName = ((HandlerMethod) handler).getMethod().getName();

    final Optional<Instant> startTime = Optional.ofNullable(requestToStartTime.get(request));
    if (startTime.isPresent()) {
      final Duration elapsedTime = Duration.between(startTime.get(), clock.instant());
      requestToStartTime.remove(request);
      logsBasedMetricService.record(MeasurementBundle.builder()
          .addMeasurement(DistributionMetric.API_METHOD_TIME, elapsedTime.toMillis())
          .addTag(MetricLabel.METHOD_NAME, methodName)
      .build());
    }
  }
}
