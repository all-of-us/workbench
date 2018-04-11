require_relative "workbench"
require_relative "utils/common"

# Puts the AppEngine service account credentials in "sa-key.json" for use in command line
# utilities; sets GOOGLE_APPLICATION_CREDENTIALS to point at it, so application default
# credentials will use it.
# For environments other than test, deletes the credentials after use.
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

  def run()
    ENV["GOOGLE_APPLICATION_CREDENTIALS"] = @path
    common = Common.new
    if @project == "all-of-us-workbench-test" and not @service_account
      unless File.exists?(@path)
        common.run_inline %W{gsutil cp gs://#{@project}-credentials/app-engine-default-sa.json
            #{@path}}
      end
      yield
    else
      service_account = @service_account
      if not service_account
        service_account = "#{@project}@appspot.gserviceaccount.com"
      end
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
