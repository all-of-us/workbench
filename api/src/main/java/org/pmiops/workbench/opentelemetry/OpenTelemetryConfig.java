package org.pmiops.workbench.opentelemetry;

import com.google.cloud.opentelemetry.trace.TraceExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import java.util.logging.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class OpenTelemetryConfig {
  private static final Logger LOGGER = Logger.getLogger(OpenTelemetryConfig.class.getName());

  public static boolean IS_GAE = System.getProperty("com.google.appengine.runtime.version") != null;

  /** Creates an OpenTelemetry {@link SdkTracerProvider} */
  @Bean
  @Primary
  public SdkTracerProvider traceProvider() {
    if (IS_GAE) {
      LOGGER.info("Running on GAE, enable GCP TraceExporter");
      return SdkTracerProvider.builder()
          .addSpanProcessor(BatchSpanProcessor.builder(TraceExporter.createWithDefaultConfiguration()).build())
          .build();
    } else {
      return SdkTracerProvider.builder()
          .build();
    }
  }

}
