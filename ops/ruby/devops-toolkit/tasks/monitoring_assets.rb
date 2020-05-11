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
      # make_env_output_dir(env_bundle[:current_env].short_name)

      backup_alert_policies(env_bundle)

      # backup_monitored_resources(env_bundle)
    end
  end

  private

  def backup_alert_policies(env_bundle)
    policies = env_bundle[:alerts_client].list_alert_policies(env_bundle[:path])
    logger.info("found #{policies.count} alerting policies in #{env_bundle[:current_env].short_name}")

    policies.each do |policy|
      # path = make_output_path(env_bundle[:current_env].short_name, 'policies')
      path = make_output_path(policy)
      file_name = DateTime.now.to_s + '.json'
      full_path = File.join(path, file_name)
      logger.info("Writing #{policy.display_name} to #{full_path}")
      IO.write(full_path, policy.to_json)
    end
  end

  def backup_monitored_resources(env_bundle)
    resources = env_bundle[:metric_client].list_monitored_resource_descriptors(env_bundle[:path])
    logger.info("found #{resources.count} monitored resources policies in #{env_bundle[:current_env].short_name}")

    resources.each do |resource|
      path = make_output_path(env_bundle[:current_env].short_name, 'monitored_resources')
      logger.info("Writing #{resource.display_name} to #{path}")
      IO.write(path, resource.to_json)
    end
  end

  # TODO(jaycarlton): make this reusable outside this class
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

  # def make_output_path(env_short_name, category)
  #   env_output_dir = File.join(output_dir, env_short_name)
  #   File.join(env_output_dir, "#{category}.json")
  # end

  # take a name like projects/all-of-us-rw-prod/alertPolicies/6212216586218347852 and make a dir out of it
  #
  def make_output_path(asset)
    FileUtils.mkdir_p(File.join(output_dir, asset.name))
  end
  #
  # def make_env_output_dir(env_short_name)
  #   env_output_dir = File.join(output_dir, env_short_name)
  #   Dir.mkdir(env_output_dir) unless Dir.exist?(env_output_dir)
  #   env_output_dir
  # end

end

