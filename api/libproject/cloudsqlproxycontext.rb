require_relative "../../aou-utils/utils/common"
require_relative "../../aou-utils/workbench"

class CloudSqlProxyContext

  def initialize(gcc)
    Workbench::assert_in_docker
    gcc.ensure_service_account
  end

  def run()
    @ps = fork do
      exec *%W{
        cloud_sql_proxy
          -instances all-of-us-workbench-test:us-central1:workbenchmaindb=tcp:0.0.0.0:3307
          -credential_file=src/main/webapp/WEB-INF/sa-key.json
      }
    end
    begin
      sleep 1 # TODO(dmohs): Detect running better.
      yield
    ensure
      Process.kill "HUP", @ps
      Process.wait
    end
  end
end
