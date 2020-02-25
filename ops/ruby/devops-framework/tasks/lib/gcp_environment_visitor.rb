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

  def visit(env_list = @environments)
    Array(env_list).each do |env|
      sa_mgr = ServiceAccountManager.new(env.project_id, env.service_account, @logger)
      sa_mgr.run do |svc_acct|
        @logger.info(">>>>>>>>>>>>>>>> Entering #{env.short_name} >>>>>>>>>>>>>>>>")
        yield env
        @logger.info("<<<<<<<<<<<<<<<< Leaving #{env.short_name} <<<<<<<<<<<<<<<<")
      end
    end
  end
end
