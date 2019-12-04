package org.pmiops.workbench.interceptors;

import io.opencensus.common.Scope;
import io.opencensus.exporter.trace.stackdriver.StackdriverTraceConfiguration;
import io.opencensus.exporter.trace.stackdriver.StackdriverTraceExporter;
import io.opencensus.trace.SpanBuilder;
import io.opencensus.trace.Tracer;
import io.opencensus.trace.Tracing;
import io.opencensus.trace.samplers.Samplers;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

// Interceptor to create a trace of the lifecycle of api calls.
@Service
public class TracingInterceptor extends HandlerInterceptorAdapter {
  private static final Tracer tracer = Tracing.getTracer();
  private static final Logger log = Logger.getLogger(TracingInterceptor.class.getName());
  private static final String TRACE_ATTRIBUTE_KEY = "Tracing Span";

  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  @Autowired
  public TracingInterceptor(Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.workbenchConfigProvider = workbenchConfigProvider;
    try {
      StackdriverTraceExporter.createAndRegister(StackdriverTraceConfiguration.builder().build());
    } catch (IOException e) {
      log.log(Level.WARNING, "Failed to setup tracing", e);
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
            String.format(
                "%s/%s%s",
                workbenchConfigProvider.get().server.shortName,
                request.getMethod(),
                request.getRequestURI()));

    if (workbenchConfigProvider.get().server.traceAllRequests) {
      requestSpanBuilder.setSampler(Samplers.alwaysSample());
    }

    Scope requestSpan = requestSpanBuilder.startScopedSpan();
    request.setAttribute(TRACE_ATTRIBUTE_KEY, requestSpan);
    return true;
  }

  @Override
  public void afterCompletion(
      HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
      throws Exception {
    ((Scope) request.getAttribute(TRACE_ATTRIBUTE_KEY)).close();
  }
}
