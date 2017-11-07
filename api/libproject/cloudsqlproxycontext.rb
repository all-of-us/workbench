require_relative "../../libproject/utils/common"
require_relative "environments"

class CloudSqlProxyContext
  attr_reader :env_file_path

  def initialize(env_key, gcloud_context)
    raise ArgumentError("Invalid GcloudContext") unless gcloud_context.account
    @bucket_name = Environments::CREDENTIALS_BUCKET_NAMES.fetch(env_key)
    @bucket_file_name = Environments::DB_VARS_FILE_NAMES.fetch(env_key)
    @env_file_path = "db/vars.#{env_key}.env"
  end

  def run()
    common = Common.new
    common.run_inline %W{
      docker-compose run --rm api
        gsutil cp gs://#{@bucket_name}/#{@bucket_file_name} #{@env_file_path}
    }
    begin
      common.run_inline %W{docker-compose up -d cloud-sql-proxy}
      begin
        yield(@env_file_path)
      ensure
        common.run_inline %W{docker-compose down}
      end
    ensure
      Common.new.run_inline %W{rm -f #{@env_file_path}}
    end
  end
end
