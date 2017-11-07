require_relative "../../libproject/utils/common"
require_relative "environments"
require "json"

class GcloudContextV2
  attr_reader :account, :project

  def initialize(env_key)
    # We use both gcloud and gsutil commands for various tasks. While gcloud can take arguments,
    # gsutil uses the current gcloud config, so we just ensure a reasonable config is present here
    # rather than allowing arguments.
    common = Common.new
    common.status "Reading glcoud configuration..."
    configs = common.capture_stdout \
        %W{docker-compose run --rm api gcloud --format=json config configurations list}
    active_config = JSON.parse(configs).select{|x| x["is_active"]}.first
    common.status "Using '#{active_config["name"]}' gcloud configuration"
    @account = active_config["properties"]["core"]["account"]
    @project = active_config["properties"]["core"]["project"]
    common.status "  account: #{@account}"
    common.status "  project: #{@project}"
    unless @account && @project
      common.error "Account and project required for gcloud config." \
          " See gcloud config configurations --help."
      exit 1
    end
    unless @account.end_with?("@pmi-ops.org")
      common.error "Account is not a pmi-ops.org account: #{@account}"
      exit 1
    end
    unless @project == Environments::PROJECT_NAMES[env_key]
      common.error "Project '#{@project}' does not match current environment key #{env_key}"
      exit 1
    end
  end
end
