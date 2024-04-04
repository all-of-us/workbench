package org.pmiops.workbench.opentelemetry;

import com.google.cloud.opentelemetry.metric.GoogleCloudMetricExporter;
import com.google.cloud.opentelemetry.trace.TraceExporter;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import java.util.logging.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class OpenTelemetryConfig {
  private static final Logger LOGGER = Logger.getLogger(OpenTelemetryConfig.class.getName());

  public static boolean IS_GAE =
      System.getProperty("com.google.appengine.runtime.version") != null
          && !System.getProperty("com.google.appengine.runtime.version").startsWith("dev");

  /** Creates an OpenTelemetry {@link SdkTracerProvider} */
  @Bean
  @Primary
  public SdkTracerProvider traceProvider() {
    if (IS_GAE) {
      LOGGER.info("Running on GAE, enable GCP TraceExporter");
      return SdkTracerProvider.builder()
          .addSpanProcessor(
              BatchSpanProcessor.builder(TraceExporter.createWithDefaultConfiguration()).build())
          .build();
    } else {
      LOGGER.warning("Disable Trace exporter, this should only happen in local dev environment.");
      return SdkTracerProvider.builder()
          .addSpanProcessor(SimpleSpanProcessor.create(new NoopSpanExporter()))
          .build();
    }
  }

  @Bean
  @Primary
  public SdkMeterProvider metricReader() {
    if (IS_GAE) {
      LOGGER.info("Running on GAE, enable GCP MeterReader");
      return SdkMeterProvider.builder()
          .registerMetricReader(
              PeriodicMetricReader.create(
                  GoogleCloudMetricExporter.createWithDefaultConfiguration()))
          .build();
    } else {
      LOGGER.warning("Disable MetricReader, this should only happen in local dev environment.");
      return SdkMeterProvider.builder()
          .registerMetricReader(PeriodicMetricReader.create(new NoopMetricExporter()))
          .build();
    }
  }
}
