require_relative "../../aou-utils/workbench"
require_relative "../../aou-utils/utils/common"

# Puts the AppEngine service account credentials in "sa-key.json" for use in command line
# utilities; sets GOOGLE_APPLICATION_CREDENTIALS to point at it, so application default
# credentials will use it.
# For environments other than test, deletes the credentials after use.
class ServiceAccountContext

  SERVICE_ACCOUNT_KEY_PATH = "sa-key.json"

  def initialize(project)
    @project = project
  end

  def run()
    ENV["GOOGLE_APPLICATION_CREDENTIALS"] = File.expand_path(SERVICE_ACCOUNT_KEY_PATH)
    common = Common.new
    if @project == "all-of-us-workbench-test"
      common.run_inline %W{gsutil cp gs://#{@project}-credentials/app-engine-default-sa.json
            #{SERVICE_ACCOUNT_KEY_PATH}}
      yield
    else
      service_account = "#{@project}@appspot.gserviceaccount.com"
      common.run_inline %W{gcloud iam service-accounts keys create #{SERVICE_ACCOUNT_KEY_PATH}
          --iam-account=#{service_account} --project=#{@project}}
      begin
        yield
      ensure
        tmp_private_key = `grep private_key_id #{SERVICE_ACCOUNT_KEY_PATH} | cut -d\\\" -f4`.strip()
        service_account ="#{@project}@appspot.gserviceaccount.com"
        common.run_inline %W{gcloud iam service-accounts keys delete #{tmp_private_key} -q
           --iam-account=#{service_account} --project=#{@project}}
        common.run_inline %W{rm #{SERVICE_ACCOUNT_KEY_PATH}}
      end
    end
  end
end
