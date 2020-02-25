require 'google/cloud/logging'
require_relative 'lib/gcp_environment_visitor.rb'

# Take a source environment and logs-based metric URI and replicate it across all target environments
class ReplicateLogsBasedMetric
  def initialize(options, logger)
    @envs_file = options[:'envs-file']
    @source_metric_name = options[:'source-uri']
    @source_env_short_name = options[:'environments']
    @logger = logger
  end

  def run
    # retrieve the source metric from its project
    #
    visitor = GcpEnvironmentVisitor.new(@envs_file, @logger)

    # need to visit a single environment
    #
    source_env = visitor.environments.select { |env| env.short_name == @source_env_short_name }.first
    @logger.info("Source environment is #{source_env.to_s}")

    source_metric = nil # extract from its environment
    visitor.visit(source_env) do |env|
      logging_client = Google::Cloud::Logging.new({project: env.project_id})
      metrics = logging_client.metrics
      source_metric = metrics.select { |m| m.name == @source_metric_name}.first
    end

    @logger.info("located source metric #{source_metric}")
  end
end
