require_relative "../../libproject/utils/common"

class CloudSqlProxyContext
  attr_reader :env_file_path

  def initialize(gcc)
    @bucket_name = "#{gcc.project}-credentials"
    @env_file_path = "db/vars.#{gcc.project}.env"
  end

  def run()
    common = Common.new
    common.run_inline %W{
      docker-compose run --rm api
        gsutil cp gs://#{@bucket_name}/vars.env #{@env_file_path}
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
