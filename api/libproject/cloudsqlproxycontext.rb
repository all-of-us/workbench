require_relative "../../aou-utils/serviceaccounts"
require_relative "../../aou-utils/utils/common"
require_relative "../../aou-utils/workbench"
require_relative "./mysql_docker"

class CloudSqlProxyContext < ServiceAccountContext

  DOCKER_PROXY_NAME = 'rw_cloud_sql_proxy'
  DEADLINE_SEC = 120

  def run()
    common = Common.new
    # TODO(dmohs): An error here does not cause the main thread to die.
    super do
      ps = nil
      docker_container_id = nil
      instance = "#{@project}:us-central1:workbenchmaindb"
      if Workbench.in_docker?
        ps = fork do
          exec(*%W{
          cloud_sql_proxy
            --port 3307
            #{instance}
            --credentials-file=#{@path}
          })
        end
      else
        if common.run(%W{docker kill #{DOCKER_PROXY_NAME}}).success?
          common.warning "found and killed existing cloud sql proxy docker service"
        end
 
        docker_container_id = common.capture_stdout(%W{docker run -d
             -u #{ENV["UID"]}
             -v #{@keyfile_path}:/config
             --publish 3307:3307 
             --rm
             --name #{DOCKER_PROXY_NAME}
             gcr.io/cloud-sql-connectors/cloud-sql-proxy:2.14.2
             --address 0.0.0.0
             --port 3307
             --credentials-file=/config
             #{instance}
          }).chomp
      end
      begin
        common.status "waiting up to #{DEADLINE_SEC}s for cloudsql proxy to start..."
        start = Time.now
        until common.run(maybe_dockerize_mysql_cmd("mysqladmin ping --host 0.0.0.0 --port 3307")).success?
          if Time.now - start >= DEADLINE_SEC
            raise("mysql docker service did not become available after #{DEADLINE_SEC}s")
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
