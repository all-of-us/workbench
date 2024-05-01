package org.pmiops.workbench.exfiltration;

import com.google.cloud.bigquery.BigQuery.QueryResultsOption;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.Job;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.TableResult;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import javax.inject.Provider;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.model.DbEgressEvent;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.model.AuditEgressRuntimeLogEntry;
import org.pmiops.workbench.model.AuditEgressRuntimeLogGroup;
import org.pmiops.workbench.model.SumologicEgressEvent;
import org.pmiops.workbench.utils.FieldValues;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EgressLogService {
  /**
   * Scan logs ahead of the egress window by this amount, in the event that the egress activity
   * started before or near the beginning. This is not guaranteed to catch all relevant logs.
   */
  public static final Duration LOG_START_OFFSET = Duration.ofMinutes(5L);

  public static final int RUNTIME_LOG_LIMIT = 1000;

  public static final List<EgressTerraRuntimeLogPattern> terraRuntimeLogPatterns =
      ImmutableList.of(
          new EgressTerraRuntimeLogPattern("Notebook Interactions", "%.ipynb%"),
          new EgressTerraRuntimeLogPattern("File downloads", "%download%"),
          new EgressTerraRuntimeLogPattern("Errors", "%error%"));

  private static final String GCE_LOG_LOCATION = "WorkspaceRuntimeLogs.cos_containers";
  private static final String DATAPROC_LOG_LOCATION = "WorkspaceRuntimeLogs.jupyter";

  // The text column which contains full log message.
  private static final String GCE_TEXT_COLUMN_NAME = "jsonPayload.message";
  private static final String DATAPROC_TEXT_COLUMN_NAME = "textPayload";

  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final BigQueryService bigQueryService;

  @Autowired
  public EgressLogService(
      Provider<WorkbenchConfig> workbenchConfigProvider, BigQueryService bigQueryService) {
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.bigQueryService = bigQueryService;
  }

  /**
   * Fetches groups of runtime logs for the given egress events which may be relevant to
   * investigation of said event. Row count is restricted for performance reason (see totalEntries).
   * Within each group, logs are sorted by descending timestamp order. All logs returned are
   * restricted to the approximate time frame of the event.
   */
  public List<AuditEgressRuntimeLogGroup> getRuntimeLogGroups(DbEgressEvent event) {
    Duration windowSize =
        Duration.ofSeconds(Optional.ofNullable(event.getEgressWindowSeconds()).orElse(0l));
    Instant endTime = event.getCreationTime().toInstant();
    Optional<SumologicEgressEvent> maybeSumologicEvent =
        maybeParseSumologicEvent(event.getSumologicEvent());
    if (maybeSumologicEvent.isEmpty()) {
      return new ArrayList<>();
    }
    Instant startTime =
        maybeSumologicEvent
            .map(SumologicEgressEvent::getTimeWindowStart)
            .filter(Objects::nonNull)
            .map(Instant::ofEpochMilli)
            .orElse(endTime.minus(windowSize))
            .minus(LOG_START_OFFSET);

    Map<String, QueryParameterValue> baseParams =
        ImmutableMap.<String, QueryParameterValue>builder()
            .put("project_id", QueryParameterValue.string(event.getWorkspace().getGoogleProject()))
            .put(
                "start_time",
                QueryParameterValue.timestamp(FieldValues.toTimestampMicroseconds(startTime)))
            .put(
                "end_time",
                QueryParameterValue.timestamp(FieldValues.toTimestampMicroseconds(endTime)))
            .build();

    Map<EgressTerraRuntimeLogPattern, Job> bigQueryLogJobs =
        terraRuntimeLogPatterns.stream()
            .collect(
                Collectors.toMap(
                    Function.identity(),
                    (runtimeLogPattern) ->
                        startBigQueryJob(
                            runtimeLogPattern,
                            baseParams,
                            getDatasetId(maybeSumologicEvent),
                            getTextColumnName(maybeSumologicEvent))));

    return terraRuntimeLogPatterns.stream()
        .map(
            (pattern) -> {
              TableResult result;
              try {
                result =
                    bigQueryLogJobs
                        .get(pattern)
                        .getQueryResults(QueryResultsOption.pageSize(RUNTIME_LOG_LIMIT));
              } catch (InterruptedException e) {
                throw new ServerErrorException("failed while waiting for BigQuery job", e);
              }
              return toAuditEgressRuntimeLogGroup(pattern, result);
            })
        .collect(Collectors.toList());
  }

  private Optional<SumologicEgressEvent> maybeParseSumologicEvent(@Nullable String sumologicEvent) {
    if (sumologicEvent == null) {
      return Optional.empty();
    }
    try {
      return Optional.of(new Gson().fromJson(sumologicEvent, SumologicEgressEvent.class));
    } catch (JsonSyntaxException e) {
      return Optional.empty();
    }
  }

  private AuditEgressRuntimeLogGroup toAuditEgressRuntimeLogGroup(
      EgressTerraRuntimeLogPattern logPattern, TableResult result) {
    return new AuditEgressRuntimeLogGroup()
        .name(logPattern.getName())
        .pattern(logPattern.getLogMessagePattern())
        .entries(
            StreamSupport.stream(result.getValues().spliterator(), false)
                .map(this::toRuntimeLogEntry)
                .collect(Collectors.toList()))
        .totalEntries((int) result.getTotalRows());
  }

  private AuditEgressRuntimeLogEntry toRuntimeLogEntry(FieldValueList fvl) {
    return new AuditEgressRuntimeLogEntry()
        .timestamp(FieldValues.getInstant(fvl.get("timestamp")).toString())
        .message(fvl.get("message").getStringValue());
  }

  private String getDatasetId(Optional<SumologicEgressEvent> sumologicEgressEvent) {
    return isGce(sumologicEgressEvent)
        ? workbenchConfigProvider.get().firecloud.workspaceLogsProject + "." + GCE_LOG_LOCATION
        : workbenchConfigProvider.get().firecloud.workspaceLogsProject
            + "."
            + DATAPROC_LOG_LOCATION;
  }

  private String getTextColumnName(Optional<SumologicEgressEvent> sumologicEgressEvent) {
    return isGce(sumologicEgressEvent) ? GCE_TEXT_COLUMN_NAME : DATAPROC_TEXT_COLUMN_NAME;
  }

  private boolean isGce(Optional<SumologicEgressEvent> sumologicEgressEvent) {
    // If getGceEgressMib is more than zero, we see this egress triggered by GCE, otherwise, it's
    // Dataproc.
    // TODO(yonghao): Revisit this logic and probably make an enum once we understand what does GKE
    // sumologic event looks like.
    return sumologicEgressEvent
        .map(SumologicEgressEvent::getGceEgressMib)
        .map(d -> d > 0.0)
        .orElse(false);
  }

  private Job startBigQueryJob(
      EgressTerraRuntimeLogPattern runtimeLogPattern,
      Map<String, QueryParameterValue> baseParams,
      String datasetId,
      String textColumn) {
    return bigQueryService.startQuery(
        QueryJobConfiguration.newBuilder(
                String.format(
                    "SELECT timestamp, %s AS message"
                        + " FROM %s"
                        + " WHERE resource.labels.project_id = @project_id"
                        + "  AND timestamp BETWEEN @start_time AND @end_time"
                        + "  AND %s"
                        + " LIKE @log_pattern"
                        + " ORDER BY timestamp DESC",
                    textColumn, datasetId, textColumn))
            .setNamedParameters(
                ImmutableMap.<String, QueryParameterValue>builder()
                    .putAll(baseParams)
                    .put(
                        "log_pattern",
                        QueryParameterValue.string(runtimeLogPattern.getLogMessagePattern()))
                    .build())
            .build());
  }
}
