require_relative "../../aou-utils/workbench"
require_relative "../../aou-utils/utils/common"

# Puts the AppEngine service account credentials in "sa-key.json" for use in command line
# utilities; sets GOOGLE_APPLICATION_CREDENTIALS to point at it, so application default
# credentials will use it.
# For environments other than test, deletes the credentials after use.
class ServiceAccountContext

  def initialize(project)
    @project = project
    @sa_key_path = "sa-key.json"
  end

  def run()
    ENV["GOOGLE_APPLICATION_CREDENTIALS"] = @sa_key_path
    common = Common.new
    if @project == "all-of-us-workbench-test"
      common.run_inline %W{gsutil cp gs://#{@project}-credentials/app-engine-default-sa.json
            #{@sa_key_path}}
      yield
    else
      service_account = "#{@project}@appspot.gserviceaccount.com"
      common.run_inline %W{gcloud iam service-accounts keys create #{@sa_key_path}
          --iam-account=#{service_account} --project=#{@project}}
      begin
        yield
      ensure
        tmp_private_key = `grep private_key_id #{@sa_key_path} | cut -d\\\" -f4`.strip()
        service_account ="#{@project}@appspot.gserviceaccount.com"
        common.run_inline %W{gcloud iam service-accounts keys delete #{tmp_private_key} -q
           --iam-account=#{service_account} --project=#{@project}}
        common.run_inline %W{rm #{@sa_key_path}}
      end
    end
  end
end
