package org.pmiops.workbench.monitoring;

import com.google.api.MonitoredResource;
import com.google.appengine.api.modules.ModulesException;
import com.google.appengine.api.modules.ModulesService;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import io.opencensus.exporter.stats.stackdriver.StackdriverStatsConfiguration;
import io.opencensus.exporter.stats.stackdriver.StackdriverStatsExporter;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Provider;
import org.jetbrains.annotations.NotNull;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.springframework.stereotype.Service;

/**
 * Simple wrapper service so we can mock out the initialization step (which depends directly on a
 * public static method)
 */
@Service
public class StackdriverStatsExporterService {

  private static final Logger logger =
      Logger.getLogger(StackdriverStatsExporterService.class.getName());
  private static final String STACKDRIVER_CUSTOM_METRICS_PREFIX = "custom.googleapis.com/";
  private static final String MONITORED_RESOURCE_TYPE = "generic_node";
  public static final String PROJECT_ID_LABEL = "project_id";
  public static final String LOCATION_LABEL = "location";
  public static final String NAMESPACE_LABEL = "namespace";
  public static final String NODE_ID_LABEL = "node_id";
  public static final String UNKNOWN_INSTANCE_PREFIX = "unknown-";
  public static final Set<String> MONITORED_RESOURCE_LABELS =
      ImmutableSet.of(PROJECT_ID_LABEL, LOCATION_LABEL, NAMESPACE_LABEL, NODE_ID_LABEL);
  private boolean initialized;
  private Provider<WorkbenchConfig> workbenchConfigProvider;
  private ModulesService modulesService;
  private Optional<String> spoofedNodeId;

  public StackdriverStatsExporterService(
      Provider<WorkbenchConfig> workbenchConfigProvider, ModulesService modulesService) {
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.modulesService = modulesService;
    this.initialized = false;
    this.spoofedNodeId = Optional.empty();
  }

  /**
   * Create and register the stats configuration on the stats exporter. This operation should only
   * happen once, so we have an isInitialized guard for that.
   */
  public void createAndRegister() {
    if (!initialized) {
      try {
        final StackdriverStatsConfiguration configuration = makeStackdriverStatsConfiguration();
        StackdriverStatsExporter.createAndRegister(configuration);
        logger.info(
            String.format(
                "Configured StackDriver exports with configuration:\n%s",
                configuration.toString()));
        initialized = true;
      } catch (IOException e) {
        logger.log(Level.WARNING, "Failed to initialize global StackdriverStatsExporter.", e);
      }
    }
  }

  @VisibleForTesting
  public StackdriverStatsConfiguration makeStackdriverStatsConfiguration() {
    return StackdriverStatsConfiguration.builder()
        .setMetricNamePrefix(STACKDRIVER_CUSTOM_METRICS_PREFIX)
        .setProjectId(getProjectId())
        .setMonitoredResource(getMonitoringMonitoredResource())
        .build();
  }

  private MonitoredResource getMonitoringMonitoredResource() {
    return MonitoredResource.newBuilder()
        .setType(MONITORED_RESOURCE_TYPE)
        .putLabels(PROJECT_ID_LABEL, getProjectId())
        .putLabels(LOCATION_LABEL, getLocation())
        .putLabels(NAMESPACE_LABEL, getEnvironmentShortName())
        .putLabels(NODE_ID_LABEL, getNodeId())
        .build();
  }

  /**
   * Make a MonitoredResource to idenfity the log. Note that this is nearly identical, but a
   * different, unrelated class, from com.google.api.MonitoredResource used in Stackdriver
   * Monitoring. They both have the same type and label inputs though, which is helpful.
   *
   * <p>The MonitoredResource should be constant for all execution time for the application.
   */
  public com.google.cloud.MonitoredResource getLoggingMonitoredResource() {
    final MonitoredResource monitoringMonitoredResource = getMonitoringMonitoredResource();
    com.google.cloud.MonitoredResource.Builder builder =
        com.google.cloud.MonitoredResource.newBuilder(monitoringMonitoredResource.getType());

    MONITORED_RESOURCE_LABELS.forEach(
        label -> addLabelAndValue(builder, monitoringMonitoredResource, label));
    return builder.build();
  }

  private com.google.cloud.MonitoredResource.Builder addLabelAndValue(
      com.google.cloud.MonitoredResource.Builder builder,
      com.google.api.MonitoredResource otherResource,
      String label) {
    return builder.addLabel(label, otherResource.getLabelsOrThrow(label));
  }

  private String getProjectId() {
    return workbenchConfigProvider.get().server.projectId;
  }

  private String getLocation() {
    return workbenchConfigProvider.get().server.appEngineLocationId;
  }

  private String getNodeId() {
    try {
      return modulesService.getCurrentInstanceId();
    } catch (ModulesException e) {
      final String newNodeId = getSpoofedNodeId();
      if (workbenchConfigProvider.get().server.shortName.equals("Local")) {
        logger.log(Level.INFO, String.format("Spoofed nodeID for local process is %s.", newNodeId));
      } else {
        logger.warning(
            String.format(
                "Failed to retrieve instance ID from ModulesService. Using %s instead.",
                newNodeId));
      }
      return getSpoofedNodeId();
    }
  }

  /**
   * Stackdriver instances have very long, random ID strings. When running locally, however, the
   * ModulesService throws an exception, so we need to assign non-conflicting and non-repeating ID
   * strings.
   *
   * @return
   */
  private String getSpoofedNodeId() {
    if (!spoofedNodeId.isPresent()) {
      spoofedNodeId = Optional.of(UNKNOWN_INSTANCE_PREFIX + UUID.randomUUID().toString());
    }
    return spoofedNodeId.get();
  }

  @NotNull
  private String getEnvironmentShortName() {
    return workbenchConfigProvider.get().server.shortName.toLowerCase();
  }
}
