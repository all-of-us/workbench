require "google/cloud/monitoring"
require 'logger'

require_relative "./lib/service_account_manager"
require_relative './lib/gcp_environment_visitor'

CUSTOM_METRIC_FILTER = "metric.type = starts_with(\"custom.googleapis.com/\")"
LOGS_BASED_METRIC_FILTER = "metric.type = starts_with(\"logging.googleapis.com/\")"
USER_LOGS_BASED_METRIC_FILTER = "metric.type = starts_with(\"logging.googleapis.com/user/\")"

logger = Logger.new(STDOUT)
class MonitoringAssets
  def initialize(envrionments_path_json, logger = Logger.new(STDOUT))
    @visitor = GcpEnvironmentVisitor.new(envrionments_path_json, logger)
    @logger = logger
  end

  # Demonstrate a simple usage of the AouEnvironmentVisitor with a few read-only operations
  # on monitoring things in all the environments.
  def count
    @visitor.visit do |env|
      counts  = {}
      metric_client = Google::Cloud::Monitoring::Metric.new
      metric_project_path = Google::Cloud::Monitoring::V3::MetricServiceClient.project_path(env.project_id)

      resources = metric_client.list_monitored_resource_descriptors(metric_project_path)
      @logger.info("found #{resources.count} monitored resources")
      counts['monitored_resources'] = resources.count

      all_metrics = metric_client.list_metric_descriptors(metric_project_path)
      counts['metric_descriptors'] =  all_metrics.count
      @logger.info("found #{all_metrics.count} metric descriptors")

      custom_metrics = metric_client.list_metric_descriptors(metric_project_path, {filter: CUSTOM_METRIC_FILTER})
      @logger.info("found  #{custom_metrics.count} custom metrics")
      counts['custom_metrics'] = custom_metrics.count
      custom_metrics.each do |metric|
        @logger.info("\t#{metric.name}: #{metric.description}")
      end

      user_logs_based_metrics = metric_client.list_metric_descriptors(metric_project_path, {filter: USER_LOGS_BASED_METRIC_FILTER})
      counts['user_logs_based_metrics'] = user_logs_based_metrics.count
      @logger.info("found  #{user_logs_based_metrics.count} user-defined  logs-baed metrics")

      user_logs_based_metrics.each do |metric|
        @logger.info("\t#{metric.name}: #{metric.description}")
      end

      total_logs_based_metrics = metric_client.list_metric_descriptors(metric_project_path, {filter: LOGS_BASED_METRIC_FILTER})
      @logger.info("found  #{total_logs_based_metrics.count} total logs-baed metrics")

      alerts_client = Google::Cloud::Monitoring::V3::AlertPolicyServiceClient.new
      policies = alerts_client.list_alert_policies(env.formatted_project_name)
      @logger.info("found #{policies.count} alerting policies")
      counts['policies'] = policies.count

      policies.each do |policy|
        @logger.info("\t#{policy.name}")
      end
      # dashboard_client = Google::Cloud::Monitoring::Dashboard::V1::DashboardsServiceClient.new
      # dashboard_client.list_dashboards
      @logger.info("Total counts for #{env.short_name}: #{pp(counts)}")
    end

  end
end

