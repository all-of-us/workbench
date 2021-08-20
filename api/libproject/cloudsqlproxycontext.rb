require_relative "../../aou-utils/serviceaccounts"
require_relative "../../aou-utils/utils/common"
require_relative "../../aou-utils/workbench"

class CloudSqlProxyContext < ServiceAccountContext

  def run()
    common = Common.new
    # TODO(dmohs): An error here does not cause the main thread to die.
    super do
      ps = nil
      docker_container_id = nil
      instance = "#{@project}:us-central1:workbenchmaindb=tcp:0.0.0.0:3307"
      mysqladmin_cmd = "mysqladmin "
      if Workbench.in_docker?
        ps = fork do
          exec(*%W{
          cloud_sql_proxy
            -instances #{instance}
            -credential_file=#{@path}
          })
        end
      else
        # Containerize mysql usage if we're not inside docker already. This
        # avoids the requirement for devs to have mysql installed on their workstations.
        mysqladmin_cmd = "docker run --rm --network host --entrypoint '' mysql:5.7.27 mysqladmin "
        print("docker run -d -u #{ENV["UID"]} -v #{@keyfile_path}:/config " +
              "-p 0.0.0.0:3307:3307 gcr.io/cloudsql-docker/gce-proxy:1.19.1 /cloud_sql_proxy "+
              "-instances=#{instance} -credential_file=/config\n\n")
        docker_container_id = common.capture_stdout(%W{docker run -d
             -u #{ENV["UID"]}
             -v #{@keyfile_path}:/config
             -p 0.0.0.0:3307:3307
             gcr.io/cloudsql-docker/gce-proxy:1.19.1 /cloud_sql_proxy
             -instances=#{instance}
             -credential_file=/config
          }).chomp
      end
      begin
        deadlineSec = 40

        common.status "waiting up to #{deadlineSec}s for cloudsql proxy to start..."
        start = Time.now
        until (common.run mysqladmin_cmd + "ping --host 0.0.0.0 --port 3307 --silent").success?
          if Time.now - start >= deadlineSec
            raise("mysql docker service did not become available after #{deadlineSec}s")
          end
          sleep 1
        end
        yield
      ensure
        if ps
          Process.kill "HUP", ps
          Process.wait
        else if docker_container_id
          common.run_inline(%W{docker kill #{docker_container_id}})
        end
      end
    end
  end
end
end
