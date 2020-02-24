require './lib/service_account_manager'
require './gcp_environment_info.rb'

# Visitor
class AouEnvironmentVisitor

  attr_reader :environments
  # environments is an array of AouEnvironmentInfo objects
  def initialize(environments_path,
                 logger = Logger.new(STDOUT))
    load_environments_json(environments_path)
    @logger = logger
  end

  attr_reader :environments

  def load_environments_json(json_path)
    json = JSON.load(IO.read(json_path))

    @environments = json.environments.each.map |env|
      GcpEnvironmentInfo.new(env)
    end
    self
  end

  def visit
    @envyeironments.each do |environment|
      ServiceAccountManager.new(environment.project_id, environment.service_account).run do
        # @logger.info(">>>>>>>>>>>>>>>> Entering #{environment.short_name} >>>>>>>>>>>>>>>>")
        yield environment
        # @logger.info("<<<<<<<<<<<<<<<< Leaving #{environment.short_name} <<<<<<<<<<<<<<<<")
      end
    end
  end

end
