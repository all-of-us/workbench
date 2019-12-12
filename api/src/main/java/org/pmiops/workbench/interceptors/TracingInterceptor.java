package org.pmiops.workbench.interceptors;

import com.google.api.client.http.HttpMethods;
import io.opencensus.common.Scope;
import io.opencensus.exporter.trace.stackdriver.StackdriverTraceConfiguration;
import io.opencensus.exporter.trace.stackdriver.StackdriverTraceExporter;
import io.opencensus.trace.AttributeValue;
import io.opencensus.trace.Span;
import io.opencensus.trace.SpanBuilder;
import io.opencensus.trace.Tracer;
import io.opencensus.trace.Tracing;
import io.opencensus.trace.samplers.Samplers;
import io.swagger.annotations.ApiOperation;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.method.HandlerMethod;
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
    // OPTIONS methods requests don't need traces.
    if (request.getMethod().equals(HttpMethods.OPTIONS)) {
      return true;
    }
    // All of our Swagger-generated handlers extend the HandlerMethod class, but we'll check this
    // before casting to avoid runtime exceptions.
    if (!(handler instanceof HandlerMethod)) {
      return true;
    }

    // Create a new scoped span, which will set the span context for any other traces created
    // from within this request-handling thread.
    //
    // This span will end up in Stackdriver with the name format like "Recv.[methodName]" where
    // methodName is the method name defined in our Swagger YAML.
    // Example: Recv.syncBillingProjectStatus
    HandlerMethod handlerMethod = (HandlerMethod) handler;
    SpanBuilder requestSpanBuilder =
        tracer.spanBuilder(handlerMethod.getMethod().getName()).setSpanKind(Span.Kind.SERVER);
    if (workbenchConfigProvider.get().server.traceAllRequests) {
      requestSpanBuilder.setSampler(Samplers.alwaysSample());
    }
    Scope requestScopedSpan = requestSpanBuilder.startScopedSpan();

    // Log some additional key-value attributes, which will be visible in the details pane on
    // Stackdriver.
    final Span currentSpan = tracer.getCurrentSpan();
    currentSpan.putAttribute(
        "aou-env",
        AttributeValue.stringAttributeValue(workbenchConfigProvider.get().server.shortName));
    currentSpan.putAttribute("method", AttributeValue.stringAttributeValue(request.getMethod()));
    currentSpan.putAttribute("path", AttributeValue.stringAttributeValue(request.getRequestURI()));
    ApiOperation apiOp =
        AnnotationUtils.findAnnotation(handlerMethod.getMethod(), ApiOperation.class);
    if (apiOp != null) {
      currentSpan.putAttribute("description", AttributeValue.stringAttributeValue(apiOp.notes()));
      currentSpan.putAttribute(
          "responseType", AttributeValue.stringAttributeValue(apiOp.response().toString()));
    }

    // Store the span as a payload within our request so we can close the span on completion.
    request.setAttribute(TRACE_ATTRIBUTE_KEY, requestScopedSpan);
    return true;
  }

  @Override
  public void afterCompletion(
      HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
      throws Exception {
    if (request.getAttribute(TRACE_ATTRIBUTE_KEY) != null) {
      ((Scope) request.getAttribute(TRACE_ATTRIBUTE_KEY)).close();
    }
  }
}
