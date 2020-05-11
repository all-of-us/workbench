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
      resources = env_bundle[:metric_client].list_monitored_resource_descriptors(env_bundle[:metric_project_path])
      logger.info("found #{resources.count} monitored resources")
      counts['monitored_resources'] = resources.count

      all_metrics = env_bundle[:metric_client].list_metric_descriptors(env_bundle[:metric_project_path])
      counts['metric_descriptors'] =  all_metrics.count
      logger.info("found #{all_metrics.count} metric descriptors")

      custom_metrics = env_bundle[:metric_client].list_metric_descriptors(env_bundle[:metric_project_path], {filter: CUSTOM_METRIC_FILTER})
      logger.info("found  #{custom_metrics.count} custom metrics")
      counts['custom_metrics'] = custom_metrics.count
      custom_metrics.each do |metric|
        logger.info("\t#{metric.name}: #{metric.description}")
      end

      user_logs_based_metrics = metric_client.list_metric_descriptors(env_bundle[:metric_project_path], {filter: USER_LOGS_BASED_METRIC_FILTER})
      counts['user_logs_based_metrics'] = user_logs_based_metrics.count
      logger.info("found  #{user_logs_based_metrics.count} user-defined  logs-based metrics")

      user_logs_based_metrics.each do |metric|
        logger.info("\t#{metric.name}: #{metric.description}")
      end

      total_logs_based_metrics = metric_client.list_metric_descriptors(env_bundle[:metric_project_path], {filter: LOGS_BASED_METRIC_FILTER})
      logger.info("found  #{total_logs_based_metrics.count} total logs-based metrics")

      # alerts_client = Google::Cloud::Monitoring::V3::AlertPolicyServiceClient.new
      policies = alerts_client.list_alert_policies(current_env.formatted_project_name)
      logger.info("found #{policies.count} alerting policies")
      counts['policies'] = policies.count

      policies.each do |policy|
        logger.info("\t#{policy.display_name}, #{policy.name}, #{policy.conditions.count} conditions")
      end

      logger.info("Total counts for #{current_env.short_name}: #{counts}")
    end
  end

  def backup_config
    visit_envs do |env_bundle|
      backup_alert_policies(env_bundle)
    end
  end

  private

  def backup_alert_policies(env_bundle)
    policies = env_bundle[:alerts_client].list_alert_policies(env_bundle[:path])
    logger.info("found #{policies.count} alerting policies")

    policies.each do |policy|
      path = make_output_path('policies', env_bundle[:current_env].short_name)
      logger.info("Writing #{policy.display_name} to #{path}")
      File.write(path, JSON.pretty_generate(policy.to_json))
    end
  end

  # Thin wrapper around the visitor so we don't have to pass the env around
  def visit_envs
    visitor.visit do |env|
      env_bundle = {}
      env_bundle[:current_env] = env
      env_bundle[:metric_client] = Google::Cloud::Monitoring::Metric.new
      env_bundle[:path] = Google::Cloud::Monitoring::V3::MetricServiceClient.project_path(env.project_id)
      env_bundle[:alerts_client] = Google::Cloud::Monitoring::V3::AlertPolicyServiceClient.new
      yield env_bundle
    end
  end


  def make_output_path(env_short_name, category)
    env_output_dir = File.join(output_dir, env_short_name)
    Dir.mkdir(env_output_dir) unless Dir.exist?(env_output_dir)
    File.join(env_output_dir, "#{category}.json")
  end
end

