require 'google/cloud/monitoring/dashboard/v1'
require_relative 'lib/gcp_environment_visitor.rb'

class Dashboards
  RESOURCE_NAMESPACE_PATTERN = /resource.label."namespace"="\w+"/

  def initialize(options)
    @envs_file = options[:'envs-file']
    @source_metric_name = options[:'source-uri']
    @source_env_short_name = options[:'source-env']
    @logger = options[:logger] || Logger.new(STDOUT)
    @is_dry_run = options[:'dry-run']
    @visitor = GcpEnvironmentVisitor.new(@envs_file, @logger)
  end

  # List all the custom dashboards in each environment
  def list
    @visitor.visit do |env|
      dashboard_client = Google::Cloud::Monitoring::Dashboard::V1::DashboardsServiceClient.new
      dashboard_client.list_dashboards(env.formatted_project_number).each do |dash|
        @logger.info("#{dash.display_name}\t#{dashboard_console_link(dash, env.project_id)}")
        @logger.info(dash.to_json)
      end
    end
  end

  private

  # Construct a link to the monitoring console. Note that in the IntelliJ debugger console,
  # these are clickable links. I don't see this out of the box in my OS's terminal though.
  def dashboard_console_link(dashboard, project_id)
    "https://console.cloud.google.com/monitoring/dashboards/custom/#{dashboard_number(dashboard)}" +
        "?project=#{project_id}"
  end

  # The dashboard link requires the number to the far right in the name,
  # which  is formated like projects/<project_number>/dashboards/<dashboard_number>
  def dashboard_number(dashboard)
    dashboard.name.split('/')[-1]
  end
end

# # Retrieve the template dashboard from the test environment.
# visitor.visit_single('test') do |env|
#   dashboard_client = Google::Cloud::Monitoring::Dashboard::V1::DashboardsServiceClient.new
#   dashboard_template = dashboard_client.get_dashboard(ARGV[0])
#   puts "Template dashboard: display_name: #{dashboard_template.display_name}, name: #{dashboard_template.name}"
#   puts dashboard_template
# end
#
# # Dashboard create requests can't have ETAG specified
# dashboard_template.etag = ''
#
# # use the same number for dashboards across projects
# dashboard_number = ARGV[1]
#
# visitor.visit do |env|
#   ENV['GOOGLE_CLOUD_PROJECT'] = env.project_id
#   namespace = env.short_name
#   dashboard_client = Google::Cloud::Monitoring::Dashboard::V1::DashboardsServiceClient.new
#
#   # Build a new client in the context of the current project.
#   replacement_dashboard = dashboard_template.dup
#   replacement_dashboard.name = "projects/#{env.project_number}/dashboards/#{dashboard_number}"
#   environment_title = "[#{namespace.capitalize}]"
#   replacement_dashboard.display_name =
#       dashboard_template.display_name.gsub(/\[(.*)\]/, environment_title)
#   puts "new display_name: #{replacement_dashboard.display_name}, name: #{replacement_dashboard.name}"
#
#   replacement_dashboard.grid_layout.widgets.each do |widget|
#     puts "Updating widget #{widget.title}"
#     widget.xy_chart.data_sets.map! do |data_set|
#       old_filter = data_set.time_series_query.time_series_filter.filter
#       data_set.time_series_query.time_series_filter.filter =
#           old_filter.gsub(RESOURCE_NAMESPACE_PATTERN,"resource.label.\"namespace\"=\"#{namespace}\"")
#       data_set
#     end
#   end
#
#   puts(replacement_dashboard.to_json)
#
#   # add the dashboard to the project
#   puts "creating with parent: #{env.formatted_project_number}, dashboard name: #{replacement_dashboard.name}"
#   dashboard_client.create_dashboard(env.formatted_project_number, replacement_dashboard) do |stuff|
#     puts "block received: #{stuff.to_s}"
#   end
# end
