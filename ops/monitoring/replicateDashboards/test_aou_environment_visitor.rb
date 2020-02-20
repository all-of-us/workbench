require './aou_environment_visitor.rb'
require "google/cloud/monitoring"

visitor = AouEnvironmentVisitor.new
visitor.load_json_map

visitor.visit do |env|
  # Build a new client in the context of the current project.
  metric_client = Google::Cloud::Monitoring::Metric.new
  formatted_project_name = Google::Cloud::Monitoring::V3::MetricServiceClient.project_path(env.project_id)

  puts "formatted name for metrics client: #{formatted_project_name}"
  num_metrics = metric_client.list_metric_descriptors(formatted_project_name).count
  puts "#{env.short_name} has #{num_metrics} metrics descriptors.\n"
end