require "google/cloud/monitoring"
require "./lib/serviceaccounts"
require './aou_environment_visitor'

common = Common.new
# service_account_context = ServiceAccountContext.new(project_id)
visitor = AouEnvironmentVisitor.new('monitoring-alerts-admin')

puts "visitor: #{pp(visitor.to_json)}"

# service_account_context.run do
visitor.visit do |environment|
  metric_client = Google::Cloud::Monitoring::Metric.new
  formatted_name = Google::Cloud::Monitoring::V3::MetricServiceClient.project_path(environment.project_id)

  # ENV['MONITORING_CREDENTIALS'] = ENV['GOOGLE_APPLICATION_CREDENTIALS']
  #  puts JSON.load(IO.read('./sa-key.json'))
  common.status(pp(environment))

  # metric_client.list_monitored_resource_descriptors(formatted_name).each do |element|
  #   puts(element.to_json)
  # end
  resources = metric_client.list_monitored_resource_descriptors(formatted_name)
  common.status("found #{resources.count} monitored resources")

  # metric_client.list_metric_descriptors(formatted_name).each do |metric|
  #   puts(metric.to_json)
  # end

end
