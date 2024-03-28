package org.pmiops.workbench.monitoring;

import com.google.common.base.Stopwatch;
import io.opencensus.stats.Stats;
import io.opencensus.stats.StatsRecorder;
import io.opencensus.stats.ViewManager;
import io.opencensus.tags.Tagger;
import io.opencensus.tags.Tags;
import io.opentelemetry.sdk.metrics.InstrumentSelector;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.View;
import io.opentelemetry.sdk.metrics.export.MetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import io.opentelemetry.instrumentation.spring.autoconfigure.EnableOpenTelemetry;
@Configuration
@EnableOpenTelemetry
public class MonitoringSpringConfiguration {

  @Bean
  public ViewManager getViewManager() {
    return Stats.getViewManager();
  }

  @Bean
  public StatsRecorder getStatsRecorder() {
    return Stats.getStatsRecorder();
  }

  @Bean
  public Tagger getTagger() {
    return Tags.getTagger();
  }

  /**
   * Creates an OpenTelemetry {@link SdkMeterProvider} with all metrics readers and views in the
   * spring context
   */
  @Bean
  public SdkMeterProvider getMeterProvider(
      Resource otelResource,
      ObjectProvider<MetricReader> metricReaders,
      ObjectProvider<Pair<InstrumentSelector, View>> views) {
    var sdkMeterProviderBuilder = SdkMeterProvider.builder().addResource(otelResource);
    metricReaders.stream().forEach(sdkMeterProviderBuilder::registerMetricReader);
    views.stream()
        .forEach(pair -> sdkMeterProviderBuilder.registerView(pair.getFirst(), pair.getSecond()));
    return sdkMeterProviderBuilder.build();
  }

  /**
   * Creates an OpenTelemetry {@link SdkTracerProvider} with all span processors in the spring
   * context
   */
  @Bean
  public SdkTracerProvider terraTraceProvider(
      Resource resource,
      ObjectProvider<SpanProcessor> spanProcessors,
      TracingProperties tracingProperties) {
    var tracerProviderBuilder = SdkTracerProvider.builder().setResource(resource);
    spanProcessors.stream().forEach(tracerProviderBuilder::addSpanProcessor);
    tracerProviderBuilder.setSampler(
        new ExcludingUrlSampler(
            Optional.ofNullable(tracingProperties.excludedUrls()).orElse(DEFAULT_EXCLUDED_URLS),
            Sampler.parentBased(Sampler.traceIdRatioBased(tracingProperties.samplingRatio()))));
    return tracerProviderBuilder.build();
  }


  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  public Stopwatch getStopwatch() {
    return Stopwatch.createUnstarted();
  }
}
