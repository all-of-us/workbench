require_relative "../../libproject/utils/common"

class CloudSqlProxyContext

  def initialize(gcc)
    gcc.ensure_service_account
  end

  def run()
    common = Common.new
    common.run_inline %W{docker-compose up -d cloud-sql-proxy}
    begin
      sleep 1 # TODO(dmohs): Detect running better.
      yield
    ensure
      common.run_inline %W{docker-compose stop cloud-sql-proxy}
      common.run_inline %W{docker-compose rm --force cloud-sql-proxy}
    end
  end
end
