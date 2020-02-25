package org.pmiops.workbench.google;

import com.google.cloud.monitoring.v3.MetricServiceClient;
import com.google.monitoring.v3.Aggregation;
import com.google.monitoring.v3.ListTimeSeriesRequest;
import com.google.monitoring.v3.ProjectName;
import com.google.monitoring.v3.TimeInterval;
import com.google.monitoring.v3.TimeSeries;
import com.google.protobuf.util.Durations;
import com.google.protobuf.util.Timestamps;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CloudMonitoringServiceImpl implements CloudMonitoringService {

  private static final Duration TRAILING_TIME_TO_QUERY = Duration.ofHours(6);

  private final MetricServiceClient metricServiceClient;

  @Autowired
  public CloudMonitoringServiceImpl(MetricServiceClient metricServiceClient) {
    this.metricServiceClient = metricServiceClient;
  }

  private static Aggregation getAggregation() {
    return Aggregation.newBuilder()
        .setPerSeriesAligner(Aggregation.Aligner.ALIGN_RATE)
        .setCrossSeriesReducer(Aggregation.Reducer.REDUCE_SUM)
        .setAlignmentPeriod(Durations.fromMinutes(1))
        .build();
  }

  private static TimeInterval getTimeInterval() {
    long windowStartMillis = Instant.now().minus(TRAILING_TIME_TO_QUERY).toEpochMilli();
    long windowEndMillis = Instant.now().toEpochMilli();

    return TimeInterval.newBuilder()
        .setStartTime(Timestamps.fromMillis(windowStartMillis))
        .setEndTime(Timestamps.fromMillis(windowEndMillis))
        .build();
  }

  @Override
  public Iterable<TimeSeries> getCloudStorageReceivedBytes(String workspaceNamespace) {
    ListTimeSeriesRequest request =
        ListTimeSeriesRequest.newBuilder()
            .setName(ProjectName.of(workspaceNamespace).toString())
            .setFilter("metric.type = \"storage.googleapis.com/network/received_bytes_count\"")
            .setAggregation(getAggregation())
            .setInterval(getTimeInterval())
            .setView(ListTimeSeriesRequest.TimeSeriesView.FULL)
            .build();

    return metricServiceClient.listTimeSeries(request).iterateAll();
  }
}
