require_relative "../../aou-utils/utils/common"
require_relative "../../aou-utils/workbench"
require "json"

class GcloudContextV2
  attr_reader :account, :creds_file, :project
  SA_KEY_PATH = "src/main/webapp/WEB-INF/sa-key.json"

  def initialize(options_parser)
    Workbench::assert_in_docker
    @options_parser = options_parser
    # We use both gcloud and gsutil commands for various tasks. While gcloud can take arguments,
    # gsutil uses the current gcloud config, so we want to grab and verify the account from there.
    # We do NOT grab the project from gcloud config since that can easily be set to something
    # dangerous, like a production project. Instead, we always require that parameter explicitly.
    @options_parser.add_option(
      "--project [GOOGLE_PROJECT]",
      lambda {|opts, v| opts.project = v},
      "Google project to act on (e.g. all-of-us-workbench-test)"
    )
    @options_parser.add_option(
      "--creds-file [PATH]",
      lambda {|opts, v| opts.creds_file = v},
      "Path to JSON-encoded Google service account file."
    )
    @options_parser.add_validator lambda {|opts| raise ArgumentError unless opts.project}
  end

  def validate()
    common = Common.new
    @project = @options_parser.opts.project
    @creds_file = @options_parser.opts.creds_file
    if @creds_file
      common.run_inline %W{gcloud auth activate-service-account --key-file #{@creds_file}}
    else
      common.status "Reading glcoud configuration..."
      configs = common.capture_stdout %W{gcloud --format=json config configurations list}
      active_config = JSON.parse(configs).select{|x| x["is_active"]}.first
      common.status "Using '#{active_config["name"]}' gcloud configuration"
      @account = active_config["properties"]["core"]["account"]
      common.status "  account: #{@account}"
      unless @account
        common.error "Account must be set in gcloud config. Try:\n" \
            "  gcloud auth login your.name@pmi-ops.org"
        exit 1
      end
      unless @account.end_with?("@pmi-ops.org") || @creds_file
        common.error "Account is not a pmi-ops.org account: #{@account}. Try:\n" \
            "  gcloud auth login your.name@pmi-ops.org"
        exit 1
      end
    end
  end

  def ensure_service_account()
    # TODO(dmohs): Also ensure project_id in this file matches @project.
    unless File.exist? SA_KEY_PATH
      Common.new.run_inline %W{
        gsutil cp
          gs://#{@project}-credentials/app-engine-default-sa.json
          #{SA_KEY_PATH}
      }
    end
  end
end
