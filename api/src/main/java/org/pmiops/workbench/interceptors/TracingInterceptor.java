package org.pmiops.workbench.interceptors;

import com.google.api.client.http.HttpMethods;
import com.google.cloud.opentelemetry.trace.TraceExporter;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.swagger.annotations.ApiOperation;
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
import org.springframework.web.servlet.AsyncHandlerInterceptor;

@Service
public class TracingInterceptor implements AsyncHandlerInterceptor {
  private static final Tracer tracer = GlobalOpenTelemetry.getTracer("org.pmiops.workbench");
  private static final Logger log = Logger.getLogger(TracingInterceptor.class.getName());

  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  @Autowired
  public TracingInterceptor(Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.workbenchConfigProvider = workbenchConfigProvider;
    try {
      var traceExporter = TraceExporter.createWithDefaultConfiguration();
      OpenTelemetrySdk.builder()
          .setTracerProvider(
              SdkTracerProvider.builder()
                  .addSpanProcessor(BatchSpanProcessor.builder(traceExporter).build())
                  .build())
          .buildAndRegisterGlobal();
    } catch (Exception e) {
      log.log(Level.WARNING, "Failed to setup tracing", e);
    }
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    if (request.getMethod().equals(HttpMethods.OPTIONS)) {
      return true;
    }
    if (!(handler instanceof HandlerMethod)) {
      return true;
    }

    HandlerMethod handlerMethod = (HandlerMethod) handler;
    SpanBuilder requestSpanBuilder =
        tracer.spanBuilder(handlerMethod.getMethod().getName()).setSpanKind(SpanKind.SERVER);
    if (workbenchConfigProvider.get().server.traceAllRequests) {
      requestSpanBuilder.setSpanKind(SpanKind.SERVER);
    }
    Context requestScopedSpan = requestSpanBuilder.startSpan().storeInContext(Context.current());

    final Span currentSpan = Span.fromContext(requestScopedSpan);
    currentSpan.setAttribute("aou-env", workbenchConfigProvider.get().server.shortName);
    currentSpan.setAttribute("method", request.getMethod());
    currentSpan.setAttribute("path", request.getRequestURI());
    ApiOperation apiOp =
        AnnotationUtils.findAnnotation(handlerMethod.getMethod(), ApiOperation.class);
    if (apiOp != null) {
      currentSpan.setAttribute("description", apiOp.notes());
      currentSpan.setAttribute("responseType", apiOp.response().toString());
    }

    request.setAttribute(RequestAttribute.TRACE.toString(), requestScopedSpan);
    return true;
  }

  @Override
  public void afterCompletion(
      HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
    if (request.getAttribute(RequestAttribute.TRACE.toString()) != null) {
      ((Scope) request.getAttribute(RequestAttribute.TRACE.toString())).close();
    }
  }
}
