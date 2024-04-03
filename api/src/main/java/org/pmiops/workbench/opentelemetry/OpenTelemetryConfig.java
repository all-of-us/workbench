package org.pmiops.workbench.opentelemetry;

import com.google.cloud.opentelemetry.trace.TraceExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class OpenTelemetryConfig {
  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  @Autowired
  public OpenTelemetryConfig(Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.workbenchConfigProvider = workbenchConfigProvider;
  }

  /** Creates an OpenTelemetry {@link SdkTracerProvider} */
  @Bean
  @Primary
  public SdkTracerProvider traceProvider() {
    SpanExporter traceExporter;
    if (workbenchConfigProvider.get().server.openTelemetryGcpExporterEnabled) {
      traceExporter = TraceExporter.createWithDefaultConfiguration();
    } else {
      traceExporter = TraceExporter.createWithDefaultConfiguration();
    }
    return SdkTracerProvider.builder()
        .addSpanProcessor(BatchSpanProcessor.builder(traceExporter).build())
        .build();
  }
}
