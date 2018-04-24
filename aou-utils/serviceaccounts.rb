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
    @path = path
    if not @path
      @path = File.expand_path(SERVICE_ACCOUNT_KEY_PATH)
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
    ENV["GOOGLE_APPLICATION_CREDENTIALS"] = @path
    service_account = @service_account
    if not service_account
      service_account = "#{@project}@appspot.gserviceaccount.com"
    end
    if service_account == existing_file_account(@path)
      # Don't generate another key if this account is already active. This can
      # happen for nested service account contexts, for example.
      common.status "Attaching to existing keyfile @ #{@path}"
      yield
      return
    end

    if service_account == "all-of-us-workbench-test@appspot.gserviceaccount.com"
      unless File.exists?(@path)
        common.run_inline %W{gsutil cp gs://#{@project}-credentials/app-engine-default-sa.json
            #{@path}}
      end
      yield
    else
      common.run_inline %W{gcloud iam service-accounts keys create #{@path}
          --iam-account=#{service_account} --project=#{@project}}
      begin
        yield
      ensure
        tmp_private_key = `grep private_key_id #{@path} | cut -d\\\" -f4`.strip()
        common.run_inline %W{gcloud iam service-accounts keys delete #{tmp_private_key} -q
           --iam-account=#{service_account} --project=#{@project}}
        common.run_inline %W{rm #{@path}}
      end
    end
  end
end
