require 'google/cloud/logging'
require_relative 'lib/gcp_environment_visitor.rb'

# Take a source environment and logs-based metric URI and replicate it across all target environments
# Note well: if two environments share a GCP project, this means they share a metrics namespace.
# If those projects share a log file (distinguished by MonitoredResource), then we dont' want to duplicate
# a metric into that shared environment.
class LogsBasedMetrics
  def initialize(options)
    @envs_file = options[:'envs-file']
    @source_dashboard_number = options[:'source-uri']
    @source_env_short_name = options[:'source-env']
    @logger = options[:logger] || Logger.new(STDOUT)
    @is_dry_run = options[:'replicate']
    @visitor = GcpEnvironmentVisitor.new(@envs_file, @logger)
  end

  def replicate
    # Visit the source environment and smuggle out a metric definition
    source_metric = get_source_metric

    # Replicate to the other environments (all but source env)
    copy_to_target_envs(source_metric)
  end

  private

  # Fetch the source metric by name from the source environment
  def get_source_metric
    source_env = @visitor.env_by_short_name(@source_env_short_name)
    @logger.info("Source environment is #{source_env.to_s}")
    source_metric = nil # extract from its environment
    @visitor.visit(source_env) do |env|
      logging_client = Google::Cloud::Logging.new({project: env.project_id})
      metrics = logging_client.metrics
      source_metric = metrics.select { |m| m.name == @source_dashboard_number }.first
      @logger.info("located source metric #{source_metric.name}")
    end
    source_metric
  end

  # Copy the source metric into a new metric on each target enviornment.
  def copy_to_target_envs(source_metric)
    target_envs = visitor.environments.select { |env| env.short_name != @source_env_short_name }

    visitor.visit(target_envs) do |tgt_env|
      logging_client = Google::Cloud::Logging.new({project: tgt_env.project_id})
      if exists?(source_metric.name, logging_client)
        @logger.warning("Skipping target env #{tgt_env.shrot_name} as #{source_metric.name} is already there.")
        next
      else
        # TODO(jaycarlton): Set namespace equal to project short name in the filter
        @logger.info("Making copy of #{source_metric.name} in #{tgt_env.short_name} env")
        copied_metric = logging_client.create_metric(source_metric.name, source_metric.filter, {description: source_metric.description})
        @logger.info("Created new metric in #{tgt_env.short_name}:\n#{metric_to_str(copied_metric)}")
      end
    end
  end

  def metric_to_str(metric)
    "name=#{metric.name}\nfilter=\"#{metric.filter}\"\ndescription=\"#{metric.description}\""
  end

  def exists?(metric_name, logging_client)
    metrics = logging_client.metrics
    metrics.any? { |m| m.name == metric_name}
  end
end
