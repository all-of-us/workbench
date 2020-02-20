require "google/cloud/monitoring"
require "./lib/serviceaccounts"

project_id = 'all-of-us-workbench-test'
service_account_context = ServiceAccountContext.new(project_id)
puts "service_account_context: #{pp(service_account_context)}"

service_account_context.run do

  metric_client = Google::Cloud::Monitoring::Metric.new
  formatted_name = Google::Cloud::Monitoring::V3::MetricServiceClient.project_path(project_id)

  ENV['MONITORING_CREDENTIALS'] = ENV['GOOGLE_APPLICATION_CREDENTIALS']
  puts JSON.load(IO.read('./sa-key.json'))

  metric_client.list_metric_descriptors(formatted_name).each do |metric|
    puts(metric.to_json)
  end

  metric_client.list_monitored_resource_descriptors(formatted_name).each do |element|
    puts(element.to_json)
  end
end
