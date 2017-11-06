require_relative "../../libproject/utils/common"
require_relative "credentials"
require_relative "gcloudcontext"

class CloudSqlProxyContext
  attr_reader :env_file_path

  def run(gcloud_context)
    common = Common.new
    bucket_file_name = Credentials::DB_VARS_FILE_NAMES.fetch(gcloud_context.env_key)
    @env_file_path = "db/vars.#{gcloud_context.env_key.to_s}.env"
    common.run_inline %W{
      docker-compose run --rm api
        gsutil cp gs://#{Credentials::CREDENTIALS_BUCKET_NAME}/#{bucket_file_name} #{@env_file_path}
    }
    begin
      common.run_inline %W{docker-compose up -d cloud-sql-proxy}
      begin
        yield(self)
      ensure
        common.run_inline %W{docker-compose down}
      end
    ensure
      Common.new.run_inline %W{rm -f #{@env_file_path}}
    end
  end
end
