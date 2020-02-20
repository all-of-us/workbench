require '../replicateDashboards/aou_environment_visitor'

require './aou_environment_visitor.rb'
require 'google/cloud/monitoring/dashboard/v1'

RESOURCE_NAMESPACE_PATTERN = /resource.label."namespace"="\w+"/

key_path = './monitoring-key.json'
ENV["GOOGLE_APPLICATION_CREDENTIALS"] = key_path
#ENV['MONITORING_CREDENTIALS'] = key_path

visitor = AouEnvironmentVisitor.new(
    [],
    'monitoring-alerts-admin@all-of-us-workbench-test.iam.gserviceaccount.com',
    './monitoring-key.json')

visitor.load_json_map

dashboard_template = nil

# Retrieve the template dashboard from the test environment.
visitor.visit_single('test') do |env|
  dashboard_client = Google::Cloud::Monitoring::Dashboard::V1::DashboardsServiceClient.new
  dashboard_template = dashboard_client.get_dashboard(ARGV[0])
  puts "Template dashboard: display_name: #{dashboard_template.display_name}, name: #{dashboard_template.name}"
  puts dashboard_template
end

# Dashboard create requests can't have ETAG specified
dashboard_template.etag = ''

# use the same number for dashboards across projects
dashboard_number = ARGV[1]

visitor.visit do |env|
  ENV['GOOGLE_CLOUD_PROJECT'] = env.project_id
  namespace = env.short_name
  dashboard_client = Google::Cloud::Monitoring::Dashboard::V1::DashboardsServiceClient.new

  # Build a new client in the context of the current project.
  replacement_dashboard = dashboard_template.dup
  replacement_dashboard.name = "projects/#{env.project_number}/dashboards/#{dashboard_number}"
  environment_title = "[#{namespace.capitalize}]"
  replacement_dashboard.display_name =
      dashboard_template.display_name.gsub(/\[(.*)\]/, environment_title)
  puts "new display_name: #{replacement_dashboard.display_name}, name: #{replacement_dashboard.name}"

  replacement_dashboard.grid_layout.widgets.each do |widget|
    puts "Updating widget #{widget.title}"
    widget.xy_chart.data_sets.map! do |data_set|
      old_filter = data_set.time_series_query.time_series_filter.filter
      data_set.time_series_query.time_series_filter.filter =
          old_filter.gsub(RESOURCE_NAMESPACE_PATTERN,"resource.label.\"namespace\"=\"#{namespace}\"")
      data_set
    end
  end

  puts(replacement_dashboard.to_json)

    # add the dashboard to the project
  puts "creating with parent: #{env.formatted_project_number}, dashboard name: #{replacement_dashboard.name}"
  dashboard_client.create_dashboard(env.formatted_project_number, replacement_dashboard) do |stuff|
    puts "block received: #{stuff.to_s}"
  end
end

ENV['GOOGLE_APPLICATION_CREDENTIALS'] = nil
