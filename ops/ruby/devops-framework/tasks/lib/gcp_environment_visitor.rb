require_relative './service_account_manager'
require_relative './gcp_environment_info.rb'

class GcpEnvironmentVisitor

  # environments is an array of AouEnvironmentInfo objects
  def initialize(environments_path,
                 logger = Logger.new(STDOUT))
    @environments = load_environments_json(environments_path)
    @logger = logger
  end

  attr_reader :environments

  def load_environments_json(json_path)
    json = JSON.load(IO.read(json_path))

    json['environments'].each.map do |env|
      GcpEnvironmentInfo.new(env)
    end
  end

  def visit
    @environments.each do |environment|
      sa_mgr = ServiceAccountManager.new(environment.project_id, environment.service_account)
      sa_mgr.run do
        @logger.info(">>>>>>>>>>>>>>>> Entering #{environment.short_name} >>>>>>>>>>>>>>>>")
        yield environment
        @logger.info("<<<<<<<<<<<<<<<< Leaving #{environment.short_name} <<<<<<<<<<<<<<<<")
      end
    end
  end
end
