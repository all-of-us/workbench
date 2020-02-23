require "google/cloud/monitoring"
require "./lib/service_account_manager"
require './aou_environment_visitor'
require 'logger'

logger = Logger.new(STDOUT)
visitor = AouEnvironmentVisitor.new('monitoring-alerts-admin')

puts "visitor: #{pp(visitor.to_json)}"

# Demonstrate a simple usage of the AouEnvironmentVisitor
visitor.visit do |environment|
  metric_client = Google::Cloud::Monitoring::Metric.new
  formatted_name = Google::Cloud::Monitoring::V3::MetricServiceClient.project_path(environment.project_id)

  logger.info('in environment ' + pp(environment))

  resources = metric_client.list_monitored_resource_descriptors(formatted_name)
  logger.info("found #{resources.count} monitored resources")

  # metric_client.list_metric_descriptors(formatted_name).each do |metric|
  #   puts(metric.to_json)
  # end
  metrics = metric_client.list_metric_descriptors(formatted_name)
  logger.info("found #{metrics.count} metric descriptors")

  # alert_policy_client = Google::Cloud::Monitoring::V3::AlertPolicy.new
  #
  # alerting_policies = metric_client.list_alerting_policies()

end
