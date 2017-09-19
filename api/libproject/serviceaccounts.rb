require_relative "../../libproject/utils/common"

class ServiceAccounts

  CREDENTIALS_BUCKET_NAME = "all-of-us-workbench-test-credentials"

  SA_FILE_NAMES = {
    :directory_service => {
      :dev => "fake-research-aou-5db00a85f01b.json",
      # TODO(dmohs): production
      :prod => nil,
    }
  }

  def maybe_download(env_key, service_account_key)
    common = Common.new

    saved_sa_file_name = "#{service_account_key.to_s.gsub("_", "-")}-sa.json"
    unless File.exist?(saved_sa_file_name)
      environments = SA_FILE_NAMES.fetch(service_account_key)
      file_name = environments.fetch(env_key)
      common.run_inline %W{
        gsutil cp gs://#{CREDENTIALS_BUCKET_NAME}/#{file_name} #{saved_sa_file_name}
      }
    end
  end
end
