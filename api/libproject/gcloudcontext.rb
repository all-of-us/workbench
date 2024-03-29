require_relative "../../aou-utils/utils/common"
require_relative "../../aou-utils/workbench"
require "json"

def get_active_gcloud_account()
  common = Common.new
  configs = common.capture_stdout %W{gcloud --format=json config configurations list}
  active_config = JSON.parse(configs).select{|x| x["is_active"]}.first
  return active_config["properties"]["core"]["account"]
end

class GcloudContextV2
  attr_reader :account, :creds_file, :project

  def initialize(options_parser)
    @options_parser = options_parser
    # We use both gcloud and gsutil commands for various tasks. While gcloud can take arguments,
    # gsutil uses the current gcloud config, so we want to grab and verify the account from there.
    # We do NOT grab the project from gcloud config since that can easily be set to something
    # dangerous, like a production project. Instead, we always require that parameter explicitly.
    @options_parser.add_option(
      "--project [GOOGLE_PROJECT]",
      ->(opts, v) { opts.project = v},
      "Google project to act on (e.g. all-of-us-workbench-test)"
    )
    @options_parser.add_option(
      "--creds-file [PATH]",
      ->(opts, v) { opts.creds_file = v},
      "Path to JSON-encoded Google service account file."
    )
    @options_parser.add_validator ->(opts) { raise ArgumentError unless opts.project}
  end

  def validate()
    common = Common.new
    @project = @options_parser.opts.project
    @creds_file = @options_parser.opts.creds_file
    if @creds_file
      common.run_inline %W{gcloud auth activate-service-account --key-file #{@creds_file}}
    else
      @account = GcloudContextV2.validate_gcloud_auth
    end
  end

  def self.validate_gcloud_auth()
    common = Common.new
    common.status "Reading gcloud configuration..."
    account = get_active_gcloud_account()
    common.status "  account: #{account}"
    unless account
      common.error "Account must be set in gcloud config. Try:\n" \
            "  gcloud auth login your.name@pmi-ops.org"
      exit 1
    end
    unless account.end_with?("@pmi-ops.org") \
          || account.end_with?("iam.gserviceaccount.com")
      common.error "Account is not a pmi-ops.org or service account:" \
            " #{account}. Try:\n gcloud auth login your.name@pmi-ops.org"
      exit 1
    end

    return account
  end
end
