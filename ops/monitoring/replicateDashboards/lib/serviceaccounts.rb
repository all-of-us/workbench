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

  SERVICE_ACCOUNT_KEY_PATH = "sa-key.json"

  def initialize(project, service_account = nil, path = nil)
    @project = project
    @service_account = service_account
    @credentials_path = path
    if not @credentials_path
      @credentials_path = File.expand_path(SERVICE_ACCOUNT_KEY_PATH)
    end
  end

  def existing_file_account(path)
    if File.exists?(path)
      begin
        return JSON.parse(File.read(path))["client_email"]
      rescue JSON::ParserError => e
        return nil
      end
    end
    return nil
  end

  def run()
    common = Common.new
    common.status("Setting environment variable GOOGLE_APPLICATION_CREDENTIALS to #{@credentials_path}")
    ENV["GOOGLE_APPLICATION_CREDENTIALS"] = @credentials_path
    service_account = @service_account
    common.status("service_account = #{@service_account}")

    if not service_account
      service_account = "#{@project}@appspot.gserviceaccount.com"
      common.status("service_account didn't exist and was reset to #{service_account}")
    end
    if service_account == existing_file_account(@credentials_path)
      # Don't generate another key if this account is already active. This can
      # happen for nested service account contexts, for example.
      common.status "Attaching to existing keyfile @ #{@credentials_path}"
      yield
      return
    end

    if service_account == "all-of-us-workbench-test@appspot.gserviceaccount.com"
      common.status("Using the tets env service account")
      unless File.exists?(@credentials_path)
        common.status("Fetching credentials from project bucket.")
        common.run_inline %W[gsutil cp gs://#{@project}-credentials/app-engine-default-sa.json
            #{@credentials_path}]
      end
      yield
    else
      common.status("Making a new service account from file at #{@credentials_path}")
      common.run_inline %W[gcloud iam service-accounts keys create #{@credentials_path}
          --iam-account=#{service_account} --project=#{@project}]
      begin
        yield
      ensure
        common.status("Deleting private key file")
        tmp_private_key = `grep private_key_id #{@credentials_path} | cut -d\\\" -f4`.strip()
        common.run_inline %W[gcloud iam service-accounts keys delete #{tmp_private_key} -q
           --iam-account=#{service_account} --project=#{@project}]
        common.run_inline %W[rm #{@credentials_path}]
      end
    end
  end
end
