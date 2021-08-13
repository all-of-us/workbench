require_relative "../../aou-utils/serviceaccounts"
require_relative "../../aou-utils/utils/common"
require_relative "../../aou-utils/workbench"

class CloudSqlProxyContext < ServiceAccountContext

  def run()
    # TODO(dmohs): An error here does not cause the main thread to die.
    super do
      @ps = fork do
        exec(*%W{
          cloud_sql_proxy
            -instances #{@project}:us-central1:workbenchmaindb=tcp:0.0.0.0:3307
            -credential_file=#{@path}
        })
      end
      begin
        Common.new.run_inline(%W{wait-for-it 0.0.0.0:3307 -t 120})
        yield
      ensure
        Process.kill "HUP", @ps
        Process.wait
      end
    end
  end
end
