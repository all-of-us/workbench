require_relative "../../aou-utils/utils/common"
require_relative "../../aou-utils/workbench"
require_relative "serviceaccounts"

class CloudSqlProxyContext < ServiceAccountContext

  def run()
    # TODO(dmohs): An error here does not cause the main thread to die.
    super
    @ps = fork do
      exec *%W{
        cloud_sql_proxy
          -instances #{@project}:us-central1:workbenchmaindb=tcp:0.0.0.0:3307
          -credential_file={@sa_key_path}
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
