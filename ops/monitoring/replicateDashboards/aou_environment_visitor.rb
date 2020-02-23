require './lib/service_account_manager'
require './aou_environment_info.rb'
require_relative './lib/utils/common'

# Visitor
class AouEnvironmentVisitor

  attr_reader :environments
  # environments is an array of AouEnvironmentInfo objects
  def initialize(service_account_prefix, environments_json_path = './environments.json')
    @service_account_prefix = service_account_prefix
    @service_account = nil
    load_json_map(environments_json_path)
    # @service_account = service_account_name(service_account_prefix)
    # @credentials_path = credentials_path
    @common = Common.new
  end

  attr_reader :environments
  attr_reader :service_account
  attr_reader :service_account_prefix
  # attr_reader :credentials_path

  def load_json_map(json_path = './environments.json')
    environment_to_project = JSON.load(IO.read(json_path))

    @environments = environment_to_project.map do |name, project_info|
      AouEnvironmentInfo.new(name, project_info['project_id'], project_info['project_number'])
    end
    self
  end

  def visit
    @environments.each do |environment|
      @service_account = service_account_name(@service_account_prefix, environment.project_id)

      @common.status("Visiting environment #{environment.short_name} in project #{environment.project_id} with SA #{@service_account}.")
      ServiceAccountManager.new(environment.project_id, @service_account).run do
        yield environment
      end
      @common.status("Leaving environment #{environment.short_name} in project #{environment.project_id}.\n")
    end
  end

  def visit_single(short_name)
    environment = @environments.find { |env| env.short_name == short_name }
    ServiceAccountManager.new(environment.project_id, @service_account, @credentials_path).run do
      yield environment
    end
  end

  def service_account_name(prefix, project)
    "#{prefix}@#{project}.iam.gserviceaccount.com"
  end

  def to_s
    "#{}"
  end
end
