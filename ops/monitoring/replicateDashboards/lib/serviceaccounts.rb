require "json"
require_relative "utils/common"
require 'tmpdir'
require 'fileutils'

# Entering a service account context ensures that a keyfile exists at the given
# path for the given service account, and that GOOGLE_APPLICATION_CREDENTIALS is
# pointing to it (for application default credentials). Creates this SA key and
# file if necessary, and destroys it when leaving the context.
# The test SA key is a special case for local development, and is initialized
# but not deleted.
class ServiceAccountContext

  SERVICE_ACCOUNT_KEY_PATH = "sa-key.json"

  def initialize(project, service_account, credentials_path = nil)
    @common = Common.new
    @project = project
    @service_account = service_account
    @credentials_path = credentials_path
    unless @credentials_path
      make_credentials_path
    end
  end

  attr_reader :project
  attr_reader :service_account
  attr_reader :credentials_path

  def existing_file_account(path)
    if File.exists?(path)
      begin
        return JSON.parse(File.read(path))["client_email"]
      rescue JSON::ParserError => e
        return nil
      end
    end
    nil
  end

  def run()
    @common.status("Setting environment variable GOOGLE_APPLICATION_CREDENTIALS to #{@credentials_path}")
    ENV["GOOGLE_APPLICATION_CREDENTIALS"] = @credentials_path
    @common.status("service_account = #{@service_account}")

    if @service_account == existing_file_account(@credentials_path)
      # Don't generate another key if this account is already active. This can
      # happen for nested service account contexts, for example.
      @common.status "Attaching to existing keyfile @ #{@credentials_path}"
      yield self
      return
    end

    create_credentials_file

    begin
      yield self
    ensure
      cleanup_key
    end
  end

  private

  def make_credentials_path
    creds_dir_path = File.join(Dir.tmpdir, 'service_account_keys')
    Dir.mkdir(creds_dir_path) unless File.exists?(creds_dir_path)
    @credentials_path = File.join(creds_dir_path, SERVICE_ACCOUNT_KEY_PATH)
  end

  def create_credentials_file
    @common.status("Making a new service account private key for #{@service_account} at #{@credentials_path}")
    @common.run_inline %W[gcloud iam service-accounts keys create #{@credentials_path}
        --iam-account=#{@service_account} --project=#{@project}]
  end

  def cleanup_key
    @common.status("Deleting private key file")
    tmp_private_key = `grep private_key_id #{@credentials_path} | cut -d\\\" -f4`.strip()
    @common.status("Deleting SA key for #{tmp_private_key}")
    @common.run_inline %W[gcloud iam service-accounts keys delete #{tmp_private_key} -q
         --iam-account=#{service_account} --project=#{@project}]
    # File.delete(@credentials_path)
    FileUtils.remove_dir(File.dirname(@credentials_path))
  end
end
