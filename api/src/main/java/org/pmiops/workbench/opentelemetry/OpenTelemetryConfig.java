package org.pmiops.workbench.opentelemetry;

import static org.pmiops.workbench.utils.AppEngineUtils.IS_GAE;

import com.google.cloud.opentelemetry.metric.GoogleCloudMetricExporter;
import com.google.cloud.opentelemetry.trace.TraceExporter;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import java.util.logging.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class OpenTelemetryConfig {
  // The rate at which traces are sampled when running on App engine. This should be a much smaller
  // value than the running locally.
  private static final Double GAE_SAMPLER_RATE = 0.1;
  private static final Logger LOGGER = Logger.getLogger(OpenTelemetryConfig.class.getName());

  /** Creates an OpenTelemetry {@link SdkTracerProvider} */
  @Bean
  @Primary
  public SdkTracerProvider traceProvider() {
    if (IS_GAE) {
      LOGGER.info("Running on GAE, enable GCP TraceExporter");
      return SdkTracerProvider.builder()
          .addSpanProcessor(
              BatchSpanProcessor.builder(TraceExporter.createWithDefaultConfiguration()).build())
          .setSampler(Sampler.traceIdRatioBased(GAE_SAMPLER_RATE))
          .build();
    } else {
      LOGGER.warning("Disable Trace exporter, this should only happen in local dev environment.");
      return SdkTracerProvider.builder()
          .addSpanProcessor(SimpleSpanProcessor.create(new NoopSpanExporter()))
          .setSampler(Sampler.traceIdRatioBased(1.0))
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
