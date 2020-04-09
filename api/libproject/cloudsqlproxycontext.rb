require_relative "../../aou-utils/serviceaccounts"
require_relative "../../aou-utils/workbench"

class CloudSqlProxyContext < ServiceAccountContext

  def run()
    # TODO(dmohs): An error here does not cause the main thread to die.
    super do
      @ps = fork do
        command = %W{
          cloud_sql_proxy
            -instances #{@project}:us-central1:workbenchmaindb=tcp:0.0.0.0:3307
            -credential_file=#{@path}
        }
        Common.new.status("Forked with command #{command}")
        exec(*command)
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
end
