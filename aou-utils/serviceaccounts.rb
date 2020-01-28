require "json"
require_relative "workbench"
require_relative "utils/common"

# Entering a service account context ensures that a keyfile exists at the given
# path for the given service account, and that GOOGLE_APPLICATION_CREDENTIALS is
# pointing to it (for application default credentials). Creates this SA key and
# file if necessary, and destroys it when leaving the context.
# The test SA key is a special case for local development, and is initialized
# but not deleted.
class ServiceAccountContext
  attr_reader :project, :service_account, :keyfile_path

  SERVICE_ACCOUNT_KEY_PATH = "sa-key.json"

  def initialize(project, service_account = nil, keyfile_path = nil)
    @project = project
    @service_account = service_account
    if not service_account
      @service_account = "#{@project}@appspot.gserviceaccount.com"
    end
    @keyfile_path = keyfile_path
    if not @keyfile_path
      @keyfile_path = File.expand_path(SERVICE_ACCOUNT_KEY_PATH)
    end
  end

  def existing_file_account(keyfile_path)
    if File.exists?(keyfile_path)
      begin
        return JSON.parse(File.read(keyfile_path))["client_email"]
      rescue JSON::ParserError => e
        return nil
      end
    end
    return nil
  end

  def run()
    common = Common.new
    ENV["GOOGLE_APPLICATION_CREDENTIALS"] = @keyfile_path
    if @service_account == existing_file_account(@keyfile_path)
      # Don't generate another key if this account is already active. This can
      # happen for nested service account contexts, for example.
      common.status "Attaching to existing keyfile @ #{@keyfile_path}"
      yield(self)
      return
    end

    if @service_account == "all-of-us-workbench-test@appspot.gserviceaccount.com"
      common.status "Copying key from GCS for #{@service_account} @ #{@keyfile_path}"
      common.run_inline %W{gsutil cp gs://#{@project}-credentials/app-engine-default-sa.json
            #{@keyfile_path}}
      yield(self)
    else
      common.status "Creating new key for #{@service_account} @ #{@keyfile_path}"
      common.run_inline %W{gcloud iam service-accounts keys create #{@keyfile_path}
          --iam-account=#{@service_account} --project=#{@project}}
      begin
        yield(self)
      ensure
        tmp_private_key = `grep private_key_id #{@keyfile_path} | cut -d\\\" -f4`.strip()
        common.run_inline %W{gcloud iam service-accounts keys delete #{tmp_private_key} -q
           --iam-account=#{@service_account} --project=#{@project}}
        common.run_inline %W{rm #{@keyfile_path}}
      end
    end
  end
end
