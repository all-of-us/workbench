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
      sa_mgr = ServiceAccountManager.new(env.project_id, env.service_account, @logger)
      sa_mgr.run do |svc_acct|
        @logger.info(">>>>>>>>>>>>>>>> Entering #{env.short_name} >>>>>>>>>>>>>>>>")
        yield env
        @logger.info("<<<<<<<<<<<<<<<< Leaving #{env.short_name} <<<<<<<<<<<<<<<<")
      end
    end
  end

  # Return the environment matching this short name.
  def env_by_short_name(short_name)
    @env_map[short_name]
    # @environments.select { |env| env.short_name == short_name }.first
  end

  def target_envs(source_env_short_name)
    @environments.select { |env| env.short_name != source_env_short_name }
  end

  private

  def load_environments_json(json_path)
    json = JSON.load(IO.read(json_path))

<<<<<<< gcp_environment_visitor.rb
    json['environments'].each.map do |env|
=======
    result = json['environments'].each.map do |env|
>>>>>>> gcp_environment_visitor.rb
      GcpEnvironmentInfo.new(env)
    end
    # if result.map { |e| e.short_name }.uniq.count < result.count
    #   @logger.error("Each environment must have a unique short name.")
    # end
    result
  end

  def env_array_to_map(environments_array)
    result = {}
    environments_array.each do |env|
      result[env.short_name] = env
    end
    result
  end
end
