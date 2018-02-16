require_relative "../../aou-utils/serviceaccounts"
require_relative "../../aou-utils/utils/common"
require_relative "../../aou-utils/workbench"

class CloudSqlProxyContext < ServiceAccountContext

  def run()
    @ps = fork do
      exec *%W{
        cloud_sql_proxy
          -instances #{@project}:us-central1:workbenchmaindb=tcp:0.0.0.0:3307
          -credential_file=#{ServiceAccountContext::SERVICE_ACCOUNT_KEY_PATH}
      }
    end
    sleep 0.2 # Allow cloud_sql_proxy to start and fail early if that is going to happen.
    exit_code = Process.wait(@ps, Process::WNOHANG)
    if exit_code
      Common.new.error "cloud_sql_proxy failed to start"
      exit exit_code
    end
    begin
      Common.new.run_inline %W{wait-for localhost:3307 --timeout=5}
      yield
    ensure
      Process.kill "HUP", @ps
      Process.wait
    end
  end
end
