package org.pmiops.workbench.monitoring;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.baggage.BaggageBuilder;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.metrics.DoubleCounter;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.MetricProducer;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.pmiops.workbench.monitoring.views.Metric;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MonitoringServiceImpl implements MonitoringService {
  private static final Logger logger = Logger.getLogger(MonitoringServiceImpl.class.getName());
  private boolean viewsAreRegistered = false;
  private SdkMeterProvider sdkMeterProvider;
  private Meter meter;
  private OtlpGrpcMetricExporter otlpGrpcMetricExporter;
  private Baggage baggage;

  @Autowired
  MonitoringServiceImpl(
      SdkMeterProvider sdkMeterProvider,
      Meter meter,
      OtlpGrpcMetricExporter otlpGrpcMetricExporter,
      Baggage baggage) {
    this.sdkMeterProvider = sdkMeterProvider;
    this.meter = meter;
    this.otlpGrpcMetricExporter = otlpGrpcMetricExporter;
    this.baggage = baggage;
  }

//  private void initStatsConfigurationIdempotent() {
//    if (!viewsAreRegistered) {
//      registerMetricViews();
//      viewsAreRegistered = true;
//    }
//    MetricProducer metricProducer = sdkMeterProvider.getMetricProducer();
//    otlpGrpcMetricExporter.export(metricProducer.collectAllMetrics());  }

  private void registerMetricViews() {
    // TODO: Implement the equivalent of registering views in OpenTelemetry
  }

  @Override
  public void recordValues(Map<Metric, Number> metricToValue, Map<AttributeKey<?>, Object> tags) {
//    initStatsConfigurationIdempotent();
    if (metricToValue.isEmpty()) {
      logger.warning("recordValue() called with empty map.");
      return;
    }

    final BaggageBuilder baggageBuilder = baggage.toBuilder();
    tags.forEach((tagKey, tagValue) -> baggageBuilder.put(tagKey.toString(), tagValue.toString()));

    metricToValue.forEach((metric, value) -> {
      if (metric.getMeasureClass().equals(Long.class)) {
        LongCounter longCounter = meter.counterBuilder(metric.getName()).build();
        longCounter.add(value.longValue(), baggageBuilder.build());
      } else if (metric.getMeasureClass().equals(Double.class)) {
        DoubleCounter doubleCounter = meter.histogramBuilder(metric.getName()).build();
        doubleCounter.add(value.doubleValue(), baggageBuilder.build());
      } else {
        logger.log(
            Level.WARNING,
            String.format("Unrecognized measure class %s", metric.getMeasureClass().getName()));
      }
    });

    logger.fine(String.format("Record measurements: %s, tags: %s", metricToValue, tags));
  }
}