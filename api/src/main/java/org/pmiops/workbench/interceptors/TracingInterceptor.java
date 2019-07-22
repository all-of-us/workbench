package org.pmiops.workbench.interceptors;

import io.opencensus.common.Scope;
import io.opencensus.exporter.trace.stackdriver.StackdriverTraceConfiguration;
import io.opencensus.exporter.trace.stackdriver.StackdriverTraceExporter;
import io.opencensus.trace.SpanBuilder;
import io.opencensus.trace.Tracer;
import io.opencensus.trace.Tracing;
import io.opencensus.trace.samplers.Samplers;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.firecloud.FirecloudApiClientTracer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

// Interceptor to create a trace of the lifecycle of api calls.
@Service
public class TracingInterceptor extends HandlerInterceptorAdapter {
  private static final Tracer tracer = Tracing.getTracer();
  private static final Logger log = Logger.getLogger(FirecloudApiClientTracer.class.getName());

  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  private Map<Integer, Scope> spanMap;

  @Autowired
  public TracingInterceptor(Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.spanMap = new HashMap<>();
    try {
      StackdriverTraceExporter.createAndRegister(StackdriverTraceConfiguration.builder().build());
    } catch (IOException e) {
      log.log(Level.WARNING, "Failed to setup tracing");
    }
  }

  /**
   * @param handler The Swagger-generated ApiController. It contains our handler as a private
   *     delegate.
   */
  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {

    SpanBuilder requestSpanBuilder =
        tracer.spanBuilder(
            workbenchConfigProvider.get().server.shortName + request.getRequestURI());

    if (workbenchConfigProvider.get().server.alwaysTrace) {
      requestSpanBuilder.setSampler(Samplers.alwaysSample());
    }

    Scope requestSpan = requestSpanBuilder.startScopedSpan();
    this.spanMap.put(request.hashCode(), requestSpan);
    return true;
  }

  @Override
  public void postHandle(
      HttpServletRequest request,
      HttpServletResponse response,
      Object handler,
      ModelAndView modelAndView)
      throws Exception {
    if (spanMap.get(request.hashCode()) != null) {
      spanMap.get(request.hashCode()).close();
    }
  }
}
