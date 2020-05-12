require "google/cloud/monitoring"
require 'logger'
require 'json'

require_relative "./lib/service_account_manager"
require_relative './lib/gcp_environment_visitor'

CUSTOM_METRIC_FILTER = "metric.type = starts_with(\"custom.googleapis.com/\")"
LOGS_BASED_METRIC_FILTER = "metric.type = starts_with(\"logging.googleapis.com/\")"
USER_LOGS_BASED_METRIC_FILTER = "metric.type = starts_with(\"logging.googleapis.com/user/\")"
FILE_SUFFIXES = {:yaml => '.yaml', :json => '.json'}

class MonitoringAssets
  def initialize(envs_path, output_dir='.', logger = Logger.new(STDOUT), output_format = :yaml)
    @visitor = GcpEnvironmentVisitor.new(envs_path, logger)
    @output_dir = output_dir
    @logger = logger
    @output_format = output_format
  end

  attr_accessor(:current_env, :metric_client, :project_path, :alert_client)
  attr_reader(:visitor, :logger, :output_dir)

  # Demonstrate a simple usage of the AouEnvironmentVisitor with a few read-only operations
  # on monitoring things in all the environments.
  def inventory
    visit_envs do |env_bundle|
      counts  = {}
      # pp env_bundle
      # exit
      metric_client = @metric_client

      resources = metric_client.list_monitored_resource_descriptors(@project_path)
      logger.info("found #{resources.count} monitored resources")
      counts['monitored_resources'] = resources.count

      all_metrics = metric_client.list_metric_descriptors(@project_path)
      counts['metric_descriptors'] =  all_metrics.count
      logger.info("found #{all_metrics.count} metric descriptors")

      custom_metrics = metric_client.list_metric_descriptors(@project_path, {filter: CUSTOM_METRIC_FILTER})
      logger.info("found  #{custom_metrics.count} custom metrics")
      counts['custom_metrics'] = custom_metrics.count
      custom_metrics.each do |metric|
        logger.info("\t#{metric.name}: #{metric.description}")
      end

      user_logs_based_metrics = @metric_client.list_metric_descriptors(@project_path, {filter: USER_LOGS_BASED_METRIC_FILTER})
      counts['user_logs_based_metrics'] = user_logs_based_metrics.count
      logger.info("found  #{user_logs_based_metrics.count} user-defined  logs-based metrics")

      user_logs_based_metrics.each do |metric|
        logger.info("\t#{metric.name}: #{metric.description}")
      end

      total_logs_based_metrics = metric_client.list_metric_descriptors(@project_path, {filter: LOGS_BASED_METRIC_FILTER})
      logger.info("found  #{total_logs_based_metrics.count} total logs-based metrics")

      policies = @alert_client.list_alert_policies(@project_path)
      logger.info("found #{policies.count} alerting policies")
      counts['policies'] = policies.count

      policies.each do |policy|
        logger.info("\t#{policy.display_name}, #{policy.name}, #{policy.conditions.count} conditions")
      end

      logger.info("Total counts for #{@current_env.short_name}: #{counts}")
    end
  end

  DO_BACKUP_ALL = false # TODO(jaycarlton): make this an option

  # For every environment in the envs_file, fetch all items of interest from the appropriate
  # Stackdriver APIs. (Note that in future we may wish to broaden this to other )
  def backup_config
    visit_envs do |env_bundle|
      backup_alert_policies
      if DO_BACKUP_ALL
        backup_all_metrics
      else
        backup_custom_metrics
        backup_user_logs_based_metrics
      end
      backup_group_members
      backup_monitored_resources
      backup_notification_channels
      backup_notification_channels
    end
  end

  private

  def backup_alert_policies
    policies = @alert_client.list_alert_policies(@project_path)
    backup_assets(policies)
  end

  def backup_custom_metrics
    custom_metrics = @metric_client.list_metric_descriptors(@project_path, {filter: CUSTOM_METRIC_FILTER})
    backup_assets(custom_metrics)
  end

  def backup_user_logs_based_metrics
    user_logs_based_metrics = @metric_client.list_metric_descriptors(@project_path, {filter: USER_LOGS_BASED_METRIC_FILTER})
    backup_assets(user_logs_based_metrics)
  end

  def backup_monitored_resources
    resources = @metric_client.list_monitored_resource_descriptors(@project_path)
    backup_assets(resources)
  end

  # Grab all the metrics, including very many defined by Google and partners. This method is here
  # more for completeness and to allow introspection/navigation of them than anything else, as we
  # can't write to these to restore them.
  def backup_all_metrics
    metrics = @metric_client.list_metric_descriptors(@project_path)
    backup_assets(metrics)
  end

  def backup_group_members
    members = @group_client.list_groups(@project_path)
    backup_assets(members)
  end

  def backup_notification_channels
    channels = @notification_channel_client.list_notification_channels(@project_path)
    backup_assets(channels)
  end

  # Backup a collection of Stackdriver assets
  def backup_assets(assets)
    assets.each(&method(:backup_asset))
  end

  # Backup the cloud asset to a file in a path based on its fully
  # qualified name and a filename from the based on the current time.
  def backup_asset(asset)
    full_path = File.join(make_output_path(asset), make_file_name)
    logger.info("Writing #{asset.display_name} to #{full_path}")
    text = serialize(asset)
    IO.write(full_path, text)
  end

  # Serialize the Stackdriver object instance to a persistent format such as YAML or JSON
  def serialize(asset)
    hash = asset.to_h
    case @output_format
    when :yaml
      return hash.to_yaml
    when :json
      return JSON.pretty_generate(hash)
    else
      @logger.WARN("Unrecognized output format '#{@output_format}'")
      return asset.to_s
    end
  end

  # Generate a file name based on the current 24-hour time. This will
  # sort such that later times are later alphabetically.
  def make_file_name
    DateTime.now.strftime("%FT%T").gsub(':', '-') + FILE_SUFFIXES[@output_format]
  end

  # Private visitor that makes the distracting client initializations for each environment. The
  # client constructors take project details from the global scope.
  # TODO(jaycarlton): explicitly provide constructor parameters for the clients so refactoring
  # becomes easier
  def visit_envs
    visitor.visit do |env|
      @current_env = env
      @metric_client = Google::Cloud::Monitoring::Metric.new
      @project_path = Google::Cloud::Monitoring::V3::MetricServiceClient.project_path(@current_env.project_id)
      @alert_client = Google::Cloud::Monitoring::V3::AlertPolicyServiceClient.new
      @group_client = Google::Cloud::Monitoring::V3::GroupServiceClient.new
      @notification_channel_client = Google::Cloud::Monitoring::V3::NotificationChannelServiceClient.new
      yield env
    end

    # These shouldn't work anymore anyway, but tidy them up just in case somebody has code after a
    # visit_envs block
    @current_env = nil
    @metric_client = nil
    @project_path = nil
    @alert_client = nil
    @group_client = nil
    @notification_channel_client = nil
  end

  # Build an otput directory hierarchy from the asset's name. This generally looks like
  # "projects/$project_name/$asset_type/$asset_descriptor". In the case of metric descriptors
  # there's also an intermediate level for the metric prefix, such as custom.googleapis.com
  def make_output_path(asset)
    FileUtils.mkdir_p(File.join(output_dir, asset.name))
  end
end

