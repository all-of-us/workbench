require "google/cloud/monitoring"
require 'logger'

require_relative "./lib/service_account_manager"
require_relative './lib/gcp_environment_visitor'

CUSTOM_METRIC_FILTER = "metric.type = starts_with(\"custom.googleapis.com/\")"
LOGS_BASED_METRIC_FILTER = "metric.type = starts_with(\"logging.googleapis.com/\")"
USER_LOGS_BASED_METRIC_FILTER = "metric.type = starts_with(\"logging.googleapis.com/user/\")"

class MonitoringAssets
  def initialize(envs_path, output_dir='.', logger = Logger.new(STDOUT))
    @visitor = GcpEnvironmentVisitor.new(envs_path, logger)
    @output_dir = output_dir
    @logger = logger
  end

  attr_accessor(:current_env, :metric_client, :metric_project_path, :alerts_client)
  attr_reader(:visitor, :logger, :output_dir)

  # Demonstrate a simple usage of the AouEnvironmentVisitor with a few read-only operations
  # on monitoring things in all the environments.
  def inventory
    visit_envs do |env_bundle|
      counts  = {}
      # pp env_bundle
      # exit
      metric_client = env_bundle[:metric_client]

      resources = metric_client.list_monitored_resource_descriptors(env_bundle[:path])
      logger.info("found #{resources.count} monitored resources")
      counts['monitored_resources'] = resources.count

      all_metrics = metric_client.list_metric_descriptors(env_bundle[:path])
      counts['metric_descriptors'] =  all_metrics.count
      logger.info("found #{all_metrics.count} metric descriptors")

      custom_metrics = metric_client.list_metric_descriptors(env_bundle[:path], {filter: CUSTOM_METRIC_FILTER})
      logger.info("found  #{custom_metrics.count} custom metrics")
      counts['custom_metrics'] = custom_metrics.count
      custom_metrics.each do |metric|
        logger.info("\t#{metric.name}: #{metric.description}")
      end

      user_logs_based_metrics = env_bundle[:metric_client].list_metric_descriptors(env_bundle[:path], {filter: USER_LOGS_BASED_METRIC_FILTER})
      counts['user_logs_based_metrics'] = user_logs_based_metrics.count
      logger.info("found  #{user_logs_based_metrics.count} user-defined  logs-based metrics")

      user_logs_based_metrics.each do |metric|
        logger.info("\t#{metric.name}: #{metric.description}")
      end

      total_logs_based_metrics = metric_client.list_metric_descriptors(env_bundle[:path], {filter: LOGS_BASED_METRIC_FILTER})
      logger.info("found  #{total_logs_based_metrics.count} total logs-based metrics")

      policies = env_bundle[:alerts_client].list_alert_policies(env_bundle[:current_env].formatted_project_name)
      logger.info("found #{policies.count} alerting policies")
      counts['policies'] = policies.count

      policies.each do |policy|
        logger.info("\t#{policy.display_name}, #{policy.name}, #{policy.conditions.count} conditions")
      end

      logger.info("Total counts for #{env_bundle[:current_env].short_name}: #{counts}")
    end
  end

  def backup_config
    visit_envs do |env_bundle|
      backup_notification_channels(env_bundle)

      # backup_group_members(env_bundle)
      # backup_alert_policies(env_bundle)
      # backup_monitored_resources(env_bundle)
      # backup_all_metrics(env_bundle)
      # backup_notification_channels(env_bundle)
    end
  end

  private

  def backup_alert_policies(env_bundle)
    policies = env_bundle[:alerts_client].list_alert_policies(env_bundle[:path])
    backup_assets(policies)
  end

  def backup_monitored_resources(env_bundle)
    resources = env_bundle[:metric_client].list_monitored_resource_descriptors(env_bundle[:path])
    backup_assets(resources)
  end

  def backup_all_metrics(env_bundle)
    metrics = env_bundle[:metric_client].list_metric_descriptors(env_bundle[:path])
    backup_assets(metrics)
  end

  def backup_group_members(env_bundle)
    members = env_bundle[:group_client].list_groups(env_bundle[:path])
    backup_assets(members)
  end

  def backup_notification_channels(env_bundle)
    channels = env_bundle[:notification_channel_client].list_notification_channels(env_bundle[:path])
    backup_assets(channels)
  end

  def backup_assets(assets)
    assets.each(&method(:backup_asset))
  end

  # Backup the cloud asset to a file in a path based on its fully
  # qualified name and a filename from the based on the current time.
  def backup_asset(asset)
    full_path = File.join(make_output_path(asset), make_file_name)
    logger.info("Writing #{asset.display_name} to #{full_path}")
    IO.write(full_path, asset.to_json)
  end

  def make_file_name
    DateTime.now.strftime("%FT%T").gsub(':', '-') + '.json'
  end

  # Private visitor that makes the distracting client initializations for each environment.
  def visit_envs
    visitor.visit do |env|
      env_bundle = {}
      env_bundle[:current_env] = env
      env_bundle[:metric_client] = Google::Cloud::Monitoring::Metric.new
      env_bundle[:path] = Google::Cloud::Monitoring::V3::MetricServiceClient.project_path(env.project_id)
      env_bundle[:alerts_client] = Google::Cloud::Monitoring::V3::AlertPolicyServiceClient.new
      env_bundle[:group_client] = Google::Cloud::Monitoring::V3::GroupServiceClient.new
      env_bundle[:notification_channel_client] = Google::Cloud::Monitoring::V3::NotificationChannelServiceClient.new
      yield env_bundle
    end
  end

  def make_output_path(asset)
    FileUtils.mkdir_p(File.join(output_dir, asset.name))
  end
end

