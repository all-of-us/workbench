

require_relative "../../aou-utils/workbench"
require_relative "../../aou-utils/utils/common"



def setup_local_environment()
  root_password = ENV["MYSQL_ROOT_PASSWORD"]
  ENV.update(Workbench.read_vars_file("../api/db/vars.env"))
  ENV["DB_HOST"] = "127.0.0.1"
  ENV["MYSQL_ROOT_PASSWORD"] = root_password
  ENV["DB_CONNECTION_STRING"] = "jdbc:mysql://127.0.0.1/workbench?useSSL=false"
  ENV["PUBLIC_DB_CONNECTION_STRING"] = "jdbc:mysql://127.0.0.1/public?useSSL=false"
end

def ensure_docker(cmd_name, args)
  unless Workbench.in_docker?
    exec(*(%W{docker-compose run --rm scripts ./project.rb #{cmd_name}} + args))
  end
end


def start_local_api()
  setup_local_environment
  common = Common.new
  common.status "Starting API server..."
  common.run_inline %W{gradle appengineStart}
end

Common.register_command({
  :invocation => "start-local-api",
  :description => "Starts api using the local MySQL instance. You must set MYSQL_ROOT_PASSWORD before running this.",
  :fn => ->() { start_local_api() }
})

def run_public_api_tests(cmd_name, args)
  ensure_docker cmd_name, args
  Dir.chdir('../public-api') do
    Common.new.run_inline %W{gradle :test} + args
  end
end

Common.register_command({
  :invocation => "test-public-api",
  :description => "Runs public API tests. To run a single test, add (for example) " \
      "--tests org.pmiops.workbench.cdr.dao.AchillesAnalysisDaoTest",
  :fn => ->(*args) { run_public_api_tests("test-public-api", args) }
})