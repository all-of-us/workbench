require_relative "../../aou-utils/serviceaccounts"
require_relative "../../aou-utils/utils/common"
require_relative "../../aou-utils/workbench"

class CloudSqlProxyContext < ServiceAccountContext

  def run()
    common = Common.new
    # TODO(dmohs): An error here does not cause the main thread to die.
    super do
      ps = nil
      cid = nil
      instance = "#{@project}:us-central1:workbenchmaindb=tcp:0.0.0.0:3307"
      if Workbench.in_docker?
        ps = fork do
          exec(*%W{
          cloud_sql_proxy
            -instances #{instance}
            -credential_file=#{@path}
          })
        end
      else
        cid = common.capture_stdout(%W{docker run -d
             -v #{@keyfile_path}:/config
             -p 0.0.0.0:3307:3307
             gcr.io/cloudsql-docker/gce-proxy:1.19.1 /cloud_sql_proxy
             -instances=#{instance}
             -credential_file=/config
          }).chomp
        end
      begin
        # XXX: health cmd for first
        common.run_inline(%W{sleep 5})
        yield
      ensure
        Process.kill "INT", ps if ps
        common.run_inline(%W{docker kill #{cid}}) if cid
        Process.wait
      end
    end
  end
end
