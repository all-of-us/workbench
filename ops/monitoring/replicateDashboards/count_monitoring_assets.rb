require "google/cloud/monitoring"
require "./lib/service_account_manager"
require './gcp_environment_visitor'
require 'logger'

logger = Logger.new(STDOUT)
envrionments_path_json = ARGV[0]
envirnoment_visitor = GcpEnvironmentVisitor.new(envrionments_path_json, logger)

puts "visitor: #{pp(envirnoment_visitor.to_json)}"

# Demonstrate a simple usage of the AouEnvironmentVisitor
envirnoment_visitor.visit do |environment|
  metric_client = Google::Cloud::Monitoring::Metric.new
  metric_project_path = Google::Cloud::Monitoring::V3::MetricServiceClient.project_path(environment.project_id)

  resources = metric_client.list_monitored_resource_descriptors(metric_project_path)
  logger.info("found #{resources.count} monitored resources")

  metrics = metric_client.list_metric_descriptors(metric_project_path)
  logger.info("found #{metrics.count} metric descriptors")
end
