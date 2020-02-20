require "google/cloud/monitoring/v3"
require 'Time'

metric_client = Google::Cloud::Monitoring::V3::Metric.new
puts metric_client

project_id = `gcloud config get-value project`.strip!
puts "project_id = " + project_id

project_qualified_name = Google::Cloud::Monitoring::V3::MetricServiceClient.project_path(project_id)
puts project_qualified_name

Aggregation = Google::Monitoring::V3::Aggregation.new(
    alignment_period:   { seconds: 1200 },
    per_series_aligner: Google::Monitoring::V3::Aggregation::Aligner::ALIGN_MEAN
)

Interval = Google::Monitoring::V3::TimeInterval.new
now = Time.now
Interval.end_time = Google::Protobuf::Timestamp.new seconds: now.to_i, nanos: now.usec
Interval.start_time = Google::Protobuf::Timestamp.new seconds: now.to_i - 1200, nanos: now.usec

# Iterate over all results.
metric_client.list_time_series(
    project_qualified_name,
    'metric.type = starts_with("custom.googleapis.com/")',
    Interval,
    Google::Monitoring::V3::ListTimeSeriesRequest::TimeSeriesView::FULL).each do |element|
  # Process element.
  puts element
end

# # Or iterate over results one page at a time.
# metric_client.list_monitored_resource_descriptors(formatted_name).each_page do |page|
#   # Process each page at a time.
#   page.each do |element|
#     # Process element.
#   end
# end
