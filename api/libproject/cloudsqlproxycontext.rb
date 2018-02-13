require_relative "../../aou-utils/utils/common"
require_relative "../../aou-utils/workbench"

class CloudSqlProxyContext

  def initialize(gcc)
    Workbench::assert_in_docker
    gcc.ensure_service_account
    @project = gcc.project
  end

  def run()
    # TODO(dmohs): An error here does not cause the main thread to die.
    @ps = fork do
      exec *%W{
        cloud_sql_proxy
          -instances #{@project}:us-central1:workbenchmaindb=tcp:0.0.0.0:3307
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
