package org.pmiops.workbench.interceptors;

import com.google.common.collect.Lists;
import io.opencensus.common.Scope;
import io.opencensus.exporter.stats.stackdriver.StackdriverStatsExporter;
import io.opencensus.exporter.trace.stackdriver.StackdriverTraceConfiguration;
import io.opencensus.exporter.trace.stackdriver.StackdriverTraceExporter;
import io.opencensus.stats.Aggregation;
import io.opencensus.stats.BucketBoundaries;
import io.opencensus.stats.Measure;
import io.opencensus.stats.Stats;
import io.opencensus.stats.StatsRecorder;
import io.opencensus.stats.View;
import io.opencensus.stats.ViewManager;
import io.opencensus.trace.SpanBuilder;
import io.opencensus.trace.Tracer;
import io.opencensus.trace.Tracing;
import io.opencensus.trace.samplers.Samplers;
import java.io.IOException;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.firecloud.FirecloudApiClientTracer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

// Interceptor to create a trace of the lifecycle of api calls.
@Service
public class TracingInterceptor extends HandlerInterceptorAdapter {
  private static final Tracer tracer = Tracing.getTracer();
  private static final Logger log = Logger.getLogger(FirecloudApiClientTracer.class.getName());
  private static final String TRACE_ATTRIBUTE_KEY = "Tracing Span";
  private static final StatsRecorder STATS_RECORDER = Stats.getStatsRecorder();
  private static final Measure.MeasureLong LATENCY_MS = Measure.MeasureLong.create(
      "task_latency",
      "The task latency in milliseconds",
      "ms");
  private static final BucketBoundaries LATENCY_BOUNDARIES = BucketBoundaries.create(
      Lists.newArrayList(0d, 100d, 200d, 400d, 1000d, 2000d, 4000d));

  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  @Autowired
  public TracingInterceptor(Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.workbenchConfigProvider = workbenchConfigProvider;

    View view = View.create(
        View.Name.create("api_call_latency_distribution"),
        "The distribution of api call latencies.",
        LATENCY_MS,
        Aggregation.Distribution.create(LATENCY_BOUNDARIES),
        Collections.emptyList());
    ViewManager viewManager = Stats.getViewManager();
    viewManager.registerView(view);

    try {
      StackdriverTraceExporter.createAndRegister(StackdriverTraceConfiguration.builder().build());
      StackdriverStatsExporter.createAndRegister();
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
    Random rand = new Random();
    for (int i = 0; i < 100; i++) {
      long ms = (long) (TimeUnit.MILLISECONDS.convert(5, TimeUnit.SECONDS) * rand.nextDouble());
      System.out.println(String.format("Latency %d: %d", i, ms));
      STATS_RECORDER.newMeasureMap().put(LATENCY_MS, ms).record();
    }
  }
}
