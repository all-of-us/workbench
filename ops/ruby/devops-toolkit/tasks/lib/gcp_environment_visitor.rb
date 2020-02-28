require_relative './service_account_manager'
require_relative './gcp_environment_info.rb'

class GcpEnvironmentVisitor

  # environments is an array of AouEnvironmentInfo objects
  def initialize(environments_path,
                 logger = Logger.new(STDOUT))
    @environments = load_environments_json(environments_path)
    @env_map = env_array_to_map(@environments)
    @logger = logger
  end

  attr_reader :environments
  attr_reader :env_map

  def visit(env_list = @environments)
    Array(env_list).each do |env|
      @logger.info(">>>>>>>>>>>>>>>> Entering #{env.short_name} >>>>>>>>>>>>>>>>")
      sa_mgr = ServiceAccountManager.new(env.project_id, env.service_account, @logger)
      sa_mgr.run do |svc_acct|
        yield env
      end
      @logger.info("<<<<<<<<<<<<<<<< Leaving #{env.short_name} <<<<<<<<<<<<<<<<\n")
    end
  end

  # Return the environment matching this short name.
  def env_by_short_name(short_name)
    @env_map[short_name]
  end

  def target_envs(source_env_short_name)
    @environments.select { |env| env.short_name != source_env_short_name }
  end

  private

  def load_environments_json(json_path)
    json = JSON.load(IO.read(json_path))
    json['environments'].map do |env|
      GcpEnvironmentInfo.new(env)
    end
  end

  def env_array_to_map(environments_array)
    result = {}
    environments_array.each do |env|
      result[env.short_name] = env
    end
    result
  end
end
