require 'google/cloud/monitoring/dashboard/v1'
require_relative 'lib/gcp_environment_visitor.rb'

class Dashboards
  RESOURCE_NAMESPACE_PATTERN = /resource.label."namespace"="\w+"/
  CUSTOM_DASHBOARD_FILTER = "dashboard.name = starts_with(\"custom.googleapis.com/\")"

  def initialize(options)
    @envs_file = options[:'envs-file']
    @source_dashboard_number = options[:'source-uri']
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
        @logger.info("#{dash.display_name}\t#{self.class.dashboard_console_link(dash, env.project_id)}")
        @logger.info(dash.to_json)
        dash.grid_layout.widgets.each do |widget|
          @logger.info("\tChart \"#{widget.title}\"")
        end
      end
    end
  end

  # Take a dashboard from the source environment, tweak it for each target environment, and
  # create a copy.
  # TODO(jaycarlton) We have two replicate tasks already, and the pattern vis-a-vis the visitor
  # is the same. So we can think about making this replication visiting pattern part of the visitor
  # itself, or otherwise genericker than it is currently.
  def replicate
    source_dashboard = get_source_dashboard
    @logger.info("Retrieved source dashboard #{source_dashboard.to_json}")
    copy_to_target_envs(source_dashboard)
  end

  # Construct a link to the monitoring console. Note that in the IntelliJ debugger console,
  # these are clickable links. I don't see this out of the box in my OS's terminal though.
  def self.dashboard_console_link(dashboard, project_id)
    "https://console.cloud.google.com/monitoring/dashboards/custom/#{dashboard_number(dashboard)}" +
        "?project=#{project_id}"
  end

  # The dashboard link requires the number to the far right in the name,
  # which  is formated like projects/<project_number>/dashboards/<dashboard_number>
  def self.dashboard_number(dashboard)
    dashboard.name.split('/')[-1]
  end

  # Create the resource string/URI for a dashboard. The API expects the format
  # projects/{project_id_or_number}/dashboards/{dashboard_id}
  def self.dashboard_resource_path(env, dashboard_number)
    "projects/#{env.project_number}/dashboards/#{dashboard_number}"
  end

  # Return true if this dashbaord is already present in the current environment.
  # Irritatingly, we can't just just get_dashboard for this, as it throws an exception if
  # it's not found.
  def self.dashboard_exists?(dashboard_client, env, dashboard_name)
    # all = dashboard_client.list_dashboards(env.formatted_project_number, filter: CUSTOM_DASHBOARD_FILTER)
    all = dashboard_client.list_dashboards(env.formatted_project_number)
    all.any? { |dash| dash.name == dashboard_name }
  end

  private

  def get_source_dashboard
    source_env = @visitor.env_map[@source_env_short_name]
    # source_env = @visitor.env_by_short_name(@source_env_short_name)
    result = nil
    @visitor.visit(source_env) do |env|
      dashboard_client = Google::Cloud::Monitoring::Dashboard::V1::DashboardsServiceClient.new
      source_dashboard_path = self.class.dashboard_resource_path(env, @source_dashboard_number)
      result = dashboard_client.get_dashboard(source_dashboard_path)
      @logger.info("Template dashboard: display_name: #{result.display_name}, name: #{result.name}")
      @logger.info(result)
    end
    result
  end

  def copy_to_target_envs(source_dashboard)
    target_envs = @visitor.target_envs(@source_env_short_name) # environments.select { |env| env.short_name != @source_env_short_name }
    @visitor.visit(target_envs) do |env|
      dashboard_client = Google::Cloud::Monitoring::Dashboard::V1::DashboardsServiceClient.new

      # Need to perform existence check with new environment's project number in the resource path.
      target_resource_path = self.class.dashboard_resource_path(env, self.class.dashboard_number(source_dashboard))
      if self.class.dashboard_exists?(dashboard_client, env, target_resource_path)
        @logger.warn("Skipping target env #{env.short_name} as #{source_dashboard.name} is already there.")
        next
      end

      # Customize this dashboard for the current environment. Typically this means setting the
      # namespace properly in all the charts and/or adjusting the title.
      replacement_dashboard = build_replacement_dashboard(env, source_dashboard, target_resource_path)

      # add the dashboard to the project
      @logger.info("creating with parent: #{env.formatted_project_number}, dashboard name: #{replacement_dashboard.name}")
      dashboard_client.create_dashboard(env.formatted_project_number, replacement_dashboard) do |created_dash|
        @logger.info("created dashboard: #{created_dash.to_json}")
      end
    end
  end

  def build_replacement_dashboard(env, source_dashboard, target_resource_path)
    # result = Google::Monitoring::Dashboard::V1::Dashboard.new
    source_dashboard.freeze # avoid contaminating our source dashboard
    result = source_dashboard.dup
    result.etag = '' # populated by the create API
    result.name = target_resource_path
    # result.grid_layout = Google::Monitoring::Dashboard::V1::GridLayout.new
    # Monitored Resource namespace is set up in StackDriver to match our environment short name (lowercased)
    namespace = env.short_name
    environment_title = "[#{namespace.capitalize}]"

    # Keep the environment title in the dashboard title for human-readability, and to separate
    # any environments that share a GCP project in the Dashboards view.
    result.display_name =
        source_dashboard.display_name.gsub(/\[(.*)\]/, environment_title)
    @logger.info("new display_name: #{result.display_name}, name: #{result.name}")

    # Fixup filters on all the metrics in result
    result.grid_layout.widgets.map! do |widget|
        new_widget = widget.dup
        widget.xy_chart.data_sets.map! do |data_set|
          # @logger.info("\tUpdating metric #{data_set.name}")
          update_data_set(data_set, namespace)
        end
        new_widget
    end

    @logger.info("Replacement dashboard: #{result.to_json}")
    result
  end

  def update_data_set(data_set, namespace)
    new_data_set = data_set.dup
    old_filter = data_set.time_series_query.time_series_filter.filter
    new_data_set.time_series_query.time_series_filter.filter =
        replace_filter_namespace(namespace, old_filter)
    @logger.info("Metric filter is now #{new_data_set.time_series_query.time_series_filter.filter}")
    new_data_set
  end

  def replace_filter_namespace(namespace, old_filter)
    old_filter.gsub(RESOURCE_NAMESPACE_PATTERN, "resource.label.\"namespace\"=\"#{namespace}\"")
  end
end
