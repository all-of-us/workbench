# Calls to common.run_inline in this file may use a quoted string purposefully
# to cause system() or spawn() to run the command in a shell. Calls with arrays
# are not run in a shell, which can break usage of the CloudSQL proxy.

require_relative "../../aou-utils/serviceaccounts"
require_relative "../../aou-utils/utils/common"
require_relative "../../aou-utils/workbench"
require_relative "cloudsqlproxycontext"
require_relative "gcloudcontext"
require_relative "wboptionsparser"
require "benchmark"
require "fileutils"
require "io/console"
require "json"
require "optparse"
require "ostruct"
require "tempfile"

TEST_PROJECT = "all-of-us-workbench-test"
GSUITE_ADMIN_KEY_PATH = "src/main/webapp/WEB-INF/gsuite-admin-sa.json"
INSTANCE_NAME = "workbenchmaindb"
FAILOVER_INSTANCE_NAME = "workbenchbackupdb"
SERVICES = %W{servicemanagement.googleapis.com storage-component.googleapis.com iam.googleapis.com
              compute.googleapis.com admin.googleapis.com appengine.googleapis.com
              cloudbilling.googleapis.com sqladmin.googleapis.com sql-component.googleapis.com
              clouderrorreporting.googleapis.com bigquery-json.googleapis.com}
DRY_RUN_CMD = %W{echo [DRY_RUN]}

def make_gae_vars(min, max)
  {
    "GAE_MIN_IDLE_INSTANCES" => min.to_s,
    "GAE_MAX_INSTANCES" => max.to_s
  }
end
TEST_GAE_VARS = make_gae_vars(0, 10)

# TODO: Make environment/project flags consistent across commands, consider
# using a environment keywords as dict keys here, e.g. :test, :staging, etc.
ENVIRONMENTS = {
  "local" => {
    :env_name => "local",
    :api_endpoint_host => "localhost:8081",
    :cdr_sql_instance => "workbench",
    :config_json => "config_local.json",
    :cdr_versions_json => "cdr_versions_local.json",
    :featured_workspaces_json => "featured_workspaces_local.json",
    :gae_vars => TEST_GAE_VARS
  },
  "all-of-us-workbench-test" => {
    :env_name => "test",
    :api_endpoint_host => "api-dot-#{TEST_PROJECT}.appspot.com",
    :cdr_sql_instance => "#{TEST_PROJECT}:us-central1:workbenchmaindb",
    :config_json => "config_test.json",
    :cdr_versions_json => "cdr_versions_test.json",
    :featured_workspaces_json => "featured_workspaces_test.json",
    :gae_vars => TEST_GAE_VARS
  },
  "all-of-us-rw-staging" => {
    :env_name => "staging",
    :api_endpoint_host => "api-dot-all-of-us-rw-staging.appspot.com",
    :cdr_sql_instance => "#{TEST_PROJECT}:us-central1:workbenchmaindb",
    :config_json => "config_staging.json",
    :cdr_versions_json => "cdr_versions_staging.json",
    :featured_workspaces_json => "featured_workspaces_staging.json",
    :gae_vars => TEST_GAE_VARS
  },
  "all-of-us-rw-perf" => {
    :env_name => "perf",
    :api_endpoint_host => "api-dot-all-of-us-rw-perf.appspot.com",
    :cdr_sql_instance => "#{TEST_PROJECT}:us-central1:workbenchmaindb",
    :config_json => "config_perf.json",
    :cdr_versions_json => "cdr_versions_perf.json",
    :featured_workspaces_json => "featured_workspaces_perf.json",
    :gae_vars => make_gae_vars(20, 20)
  },
  "all-of-us-rw-stable" => {
    :env_name => "stable",
    :api_endpoint_host => "api-dot-all-of-us-rw-stable.appspot.com",
    :cdr_sql_instance => "#{TEST_PROJECT}:us-central1:workbenchmaindb",
    :config_json => "config_stable.json",
    :cdr_versions_json => "cdr_versions_stable.json",
    :featured_workspaces_json => "featured_workspaces_stable.json",
    :gae_vars => TEST_GAE_VARS
  },
  "all-of-us-rw-preprod" => {
    :env_name => "preprod",
    :api_endpoint_host => "api-dot-all-of-us-rw-preprod.appspot.com",
    :cdr_sql_instance => "all-of-us-rw-preprod:us-central1:workbenchmaindb",
    :config_json => "config_preprod.json",
    :cdr_versions_json => "cdr_versions_preprod.json",
    :featured_workspaces_json => "featured_workspaces_preprod.json",
    :gae_vars => TEST_GAE_VARS
  },
  "all-of-us-rw-prod" => {
    :env_name => "prod",
    :api_endpoint_host => "api.workbench.researchallofus.org",
    :cdr_sql_instance => "all-of-us-rw-prod:us-central1:workbenchmaindb",
    :config_json => "config_prod.json",
    :cdr_versions_json => "cdr_versions_prod.json",
    :featured_workspaces_json => "featured_workspaces_prod.json",
    :gae_vars => make_gae_vars(8, 64)
  }
}

def run_inline_or_log(dry_run, args)
  cmd_prefix = dry_run ? DRY_RUN_CMD : []
  Common.new.run_inline(cmd_prefix + args)
end

def must_get_env_value(env, key)
  unless ENVIRONMENTS.fetch(env, {}).has_key?(key)
    raise ArgumentError.new("env '#{env}' lacks key #{key}")
  end
  return ENVIRONMENTS[env][key]
end

def get_cdr_sql_project(project)
  return must_get_env_value(project, :cdr_sql_instance).split(":")[0]
end

def ensure_docker_sync()
  common = Common.new
  at_exit do
    common.run_inline %W{docker-sync stop}
  end
  common.run_inline %W{docker-sync start}
end

def ensure_docker(cmd_name, args=nil)
  args = (args or [])
  unless Workbench.in_docker?
    ensure_docker_sync()
    exec(*(%W{docker-compose run --rm scripts ./project.rb #{cmd_name}} + args))
  end
end

def init_new_cdr_db(args)
  Common.new.run_inline %W{docker-compose run cdr-scripts generate-cdr/init-new-cdr-db.sh} + args
end

# exec against a live local API server - used for script access to a local API
# server or database.
def ensure_docker_api(cmd_name, args)
  if Workbench.in_docker?
    return
  end
  Process.wait spawn(*(%W{docker-compose exec api ./project.rb #{cmd_name}} + args))
  unless $?.exited? and $?.success?
    Common.new.error "command against docker-compose service 'api' failed, " +
                     "please verify your local API server is running (dev-up " +
                     "or run-api)"
  end
  if $?.exited?
    exit $?.exitstatus
  end
  exit 1
end

def read_db_vars(gcc)
  Workbench.assert_in_docker
  vars_path = "gs://#{gcc.project}-credentials/vars.env"
  vars = Workbench.read_vars(Common.new.capture_stdout(%W{
    gsutil cat #{vars_path}
  }))
  if vars.empty?
    Common.new.error "Failed to read #{vars_path}"
    exit 1
  end
  # Note: CDR project and target project may be the same.
  cdr_project = get_cdr_sql_project(gcc.project)
  cdr_vars_path = "gs://#{cdr_project}-credentials/vars.env"
  cdr_vars = Workbench.read_vars(Common.new.capture_stdout(%W{
    gsutil cat #{cdr_vars_path}
  }))
  if cdr_vars.empty?
    Common.new.error "Failed to read #{cdr_vars_path}"
    exit 1
  end
  return vars.merge({
    'CDR_DB_CONNECTION_STRING' => cdr_vars['DB_CONNECTION_STRING'],
    'CDR_DB_USER' => cdr_vars['WORKBENCH_DB_USER'],
    'CDR_DB_PASSWORD' => cdr_vars['WORKBENCH_DB_PASSWORD']
  })
end

def format_benchmark(bm)
  "%ds" % [bm.real]
end

def dev_up()
  common = Common.new

  account = get_auth_login_account()
  if account.nil?
    raise("Please run 'gcloud auth login' before starting the server.")
  end

  at_exit do
    common.run_inline %W{docker-compose down}
  end
  ensure_docker_sync()

  overall_bm = Benchmark.measure {
    common.status "Database startup..."
    bm = Benchmark.measure {
      common.run_inline %W{docker-compose up -d db}
    }
    common.status "Database startup complete (#{format_benchmark(bm)})"

    common.status "Database init & migrations..."
    bm = Benchmark.measure {
      common.run_inline %W{
        docker-compose run db-scripts ./run-migrations.sh main
      }
      init_new_cdr_db %W{--cdr-db-name cdr}
    }
    common.status "Database init & migrations complete (#{format_benchmark(bm)})"

    common.status "Loading configs & data..."
    bm = Benchmark.measure {
      common.run_inline %W{
        docker-compose run api-scripts ./libproject/load_local_data_and_configs.sh
      }
    }
    common.status "Loading configs complete (#{format_benchmark(bm)})"

  }
  common.status "Total dev-env setup time: #{format_benchmark(overall_bm)}"

  common.status "Starting API server..."
  run_api()
end

Common.register_command({
  :invocation => "dev-up",
  :description => "Brings up the development environment, including db migrations and config " \
     "update. (You can use run-api instead if database and config are up-to-date.)",
  :fn => ->() { dev_up() }
})

def start_api_reqs()
  common = Common.new
  common.status "Starting database..."
  common.run_inline %W{docker-compose up -d db}
  common.status "Starting elastic..."
  common.run_inline %W{docker-compose up -d elastic}
end

Common.register_command({
  :invocation => "start-api-reqs",
  :description => "Starts up the services required for the API server" \
     "(assumes database and config are already up-to-date.)",
  :fn => Proc.new { start_api_reqs() }
})

def stop_api_reqs()
  common = Common.new
  common.status "Stopping database..."
  common.run_inline %W{docker-compose stop db}
  common.status "Stopping elastic..."
  common.run_inline %W{docker-compose stop elastic}
end

Common.register_command({
  :invocation => "stop-api-reqs",
  :description => "Stops the services required for the API server",
  :fn => Proc.new { stop_api_reqs() }
})

def setup_local_environment()
  root_password = ENV["MYSQL_ROOT_PASSWORD"]
  ENV.update(Workbench.read_vars_file("db/vars.env"))
  ENV.update(must_get_env_value("local", :gae_vars))
  ENV.update({"WORKBENCH_ENV" => "local"})
  ENV["DB_HOST"] = "127.0.0.1"
  ENV["MYSQL_ROOT_PASSWORD"] = root_password
  ENV["DB_CONNECTION_STRING"] = "jdbc:mysql://127.0.0.1/workbench?useSSL=false"
end

# TODO(RW-605): This command doesn't actually execute locally as it assumes a docker context.
#
# This command is only ever meant to be run via CircleCI; see .circleci/config.yml
def run_local_migrations()
  setup_local_environment
  # Runs migrations against the local database.
  common = Common.new
  Dir.chdir('db') do
    common.run_inline %W{./run-migrations.sh main}
  end
  Dir.chdir('db-cdr/generate-cdr') do
    common.run_inline %W{./init-new-cdr-db.sh --cdr-db-name cdr}
  end
  common.run_inline %W{gradle :loadConfig -Pconfig_key=main -Pconfig_file=config/config_local.json}
  common.run_inline %W{gradle :loadConfig -Pconfig_key=cdrBigQuerySchema -Pconfig_file=config/cdm/cdm_5_2.json}
  common.run_inline %W{gradle :loadConfig -Pconfig_key=featuredWorkspaces -Pconfig_file=config/featured_workspaces_local.json}
  common.run_inline %W{gradle :updateCdrVersions -PappArgs=['config/cdr_versions_local.json',false]}
end

Common.register_command({
  :invocation => "run-local-migrations",
  :description => "Runs DB migrations with the local MySQL instance. You must set MYSQL_ROOT_PASSWORD before running this.",
  :fn => ->() { run_local_migrations() }
})

def start_local_api()
  setup_local_environment
  common = Common.new
  ServiceAccountContext.new(TEST_PROJECT).run do
    common.status "Starting API server..."
    common.run_inline %W{gradle appengineStart}
  end
end

Common.register_command({
  :invocation => "start-local-api",
  :description => "Starts api using the local MySQL instance. You must set MYSQL_ROOT_PASSWORD before running this.",
  :fn => ->() { start_local_api() }
})

def stop_local_api()
  setup_local_environment
  common = Common.new
  common.status "Stopping API server..."
  common.run_inline %W{gradle appengineStop}
end

Common.register_command({
  :invocation => "stop-local-api",
  :description => "Stops locally running api.",
  :fn => ->() { stop_local_api() }
})

def run_local_api_tests()
  common = Common.new
  status = common.capture_stdout %W{curl --silent --fail http://localhost:8081/}
  if status != 'AllOfUs Workbench API'
    common.error "Error probing api; received: #{status}"
    common.error "Server logs:"
    common.run_inline %W{cat build/dev-appserver-out/dev_appserver.out}
    exit 1
  end
  common.status "api started up."
end

Common.register_command({
  :invocation => "run-local-api-tests",
  :description => "Runs smoke tests against local api server",
  :fn => ->() { run_local_api_tests() }
})

def get_gsuite_admin_key(project)
  unless File.exist? GSUITE_ADMIN_KEY_PATH
    common = Common.new
    common.run_inline("gsutil cp gs://#{project}-credentials/gsuite-admin-sa.json #{GSUITE_ADMIN_KEY_PATH}")
  end
end

def run_api()
  common = Common.new
  ServiceAccountContext.new(TEST_PROJECT).run do
    get_gsuite_admin_key(TEST_PROJECT)
    common.status "Starting API. This can take a while. Thoughts on reducing development cycle time"
    common.status "are here:"
    common.status "  https://github.com/all-of-us/workbench/blob/master/api/doc/2017/dev-cycle.md"
    at_exit { common.run_inline %W{docker-compose down} }
    common.run_inline_swallowing_interrupt %W{docker-compose up api}
  end
end

def clean()
  common = Common.new
  common.run_inline %W{docker-compose run --rm api gradle clean}
end

Common.register_command({
  :invocation => "clean",
  :description => "Runs gradle clean. Occasionally necessary before generating code from Swagger.",
  :fn => ->(*args) { clean(*args) }
})


def run_api_and_db()
  common = Common.new
  common.status "Starting database..."
  common.run_inline %W{docker-compose up -d db}
  run_api()
end

Common.register_command({
  :invocation => "run-api",
  :description => "Runs the api server (assumes database and config are already up-to-date.)",
  :fn => ->() { run_api_and_db() }
})


def validate_swagger(cmd_name, args)
  ensure_docker cmd_name, args
  Common.new.run_inline %W{gradle validateSwagger} + args
end

Common.register_command({
  :invocation => "validate-swagger",
  :description => "Validate swagger definition files",
  :fn => ->(*args) { validate_swagger("validate-swagger", args) }
})


def run_api_tests(cmd_name, args)
  ensure_docker cmd_name, args
  Common.new.run_inline %W{gradle :test} + args
end

Common.register_command({
  :invocation => "test-api",
  :description => "Runs API tests. To run a single test, add (for example) " \
      "--tests org.pmiops.workbench.interceptors.AuthInterceptorTest",
  :fn => ->(*args) { run_api_tests("test-api", args) }
})

def run_all_tests(cmd_name, args)
  run_api_tests(cmd_name, args)
end

Common.register_command({
  :invocation => "test",
  :description => "Runs all tests (api). To run a single test, add (for example) " \
      "--tests org.pmiops.workbench.interceptors.AuthInterceptorTest",
  :fn => ->(*args) { run_all_tests("test", args) }
})


def run_integration_tests(cmd_name, *args)
  ensure_docker cmd_name, args
  common = Common.new
  ServiceAccountContext.new(TEST_PROJECT).run do
    get_gsuite_admin_key(TEST_PROJECT)
    common.run_inline %W{gradle integration} + args
  end
end

Common.register_command({
  :invocation => "integration",
  :description => "Runs integration tests.",
  :fn => ->(*args) { run_integration_tests("integration", *args) }
})

def run_bigquery_tests(cmd_name, *args)
  ensure_docker cmd_name, args
  common = Common.new
  ServiceAccountContext.new(TEST_PROJECT).run do
    common.run_inline %W{gradle bigquerytest} + args
  end
end

Common.register_command({
  :invocation => "bigquerytest",
  :description => "Runs bigquerytest tests.",
  :fn => ->(*args) { run_bigquery_tests("bigquerytest", *args) }
})

def run_rainforest_tests(cmd_name, *args)
  ensure_docker cmd_name, args
  common = Common.new
  # The bucket is hardcoded to staging, because that is currently the only
  # environment we can run tests in. There is, however, an identical key in
  # each of the other environments.
  token = `gsutil cat gs://all-of-us-rw-staging-credentials/rainforest-key.txt`
  common.run_inline %W{rainforest run --run-group 4450 --token #{token}}
end

Common.register_command({
  :invocation => "rainforesttest",
  :description => "Runs rainforest tests.",
  :fn => ->(*args) { run_rainforest_tests("rainforesttest", *args) }
})

def run_gradle(cmd_name, args)
  ensure_docker cmd_name, args
  begin
    Common.new.run_inline %W{gradle} + args
  ensure
    if $! && $!.status != 0
      Common.new.error "Command exited with non-zero status"
      exit 1
    end
  end
end

Common.register_command({
  :invocation => "gradle",
  :description => "Runs gradle inside the API docker container with the given arguments.",
  :fn => ->(*args) { run_gradle("gradle", args) }
})


def connect_to_db()
  common = Common.new
  common.status "Starting database if necessary..."
  common.run_inline %W{docker-compose up -d db}
  cmd = "MYSQL_PWD=root-notasecret mysql --database=workbench"
  common.run_inline %W{docker-compose exec db sh -c #{cmd}}
end

Common.register_command({
  :invocation => "connect-to-db",
  :description => "Connect to the running database via mysql.",
  :fn => ->() { connect_to_db() }
})


def docker_clean()
  common = Common.new

  # --volumes clears out any cached data between runs, e.g. MySQL database or Elasticsearch.
  # --rmi local forces a rebuild of any local dev images on the next run - usually the pieces will
  #   still be cached and this is fast.
  common.run_inline %W{docker-compose down --volumes --rmi local}

  # This keyfile gets created and cached locally on dev-up. Though it's not
  # specific to Docker, it is mounted locally for docker runs. For lack of a
  # better "dev teardown" hook, purge that file here; e.g. in case we decide to
  # invalidate a dev key or change the service account.
  common.run_inline %W{rm -f #{ServiceAccountContext::SERVICE_ACCOUNT_KEY_PATH} #{GSUITE_ADMIN_KEY_PATH}}

  # See https://github.com/docker/compose/issues/3447
  common.status "Cleaning complete. docker-compose 'not found' errors can be safely ignored"
end

Common.register_command({
  :invocation => "docker-clean",
  :description => \
    "Removes docker containers and volumes, allowing the next `dev-up` to" \
    " start from scratch (e.g., the database will be re-created). Includes ALL" \
    " docker images, not just for the API.",
  :fn => ->() { docker_clean() }
})

def rebuild_image()
  common = Common.new

  common.run_inline %W{docker-compose build}
end

Common.register_command({
  :invocation => "rebuild-image",
  :description => "Re-builds the dev docker image (necessary when Dockerfile is updated).",
  :fn => ->() { rebuild_image() }
})

def copy_file_to_gcs(source_path, bucket, filename)
  common = Common.new
  common.run_inline %W{gsutil cp #{source_path} gs://#{bucket}/#{filename}}
end

# Common.run_inline uses spawn() which doesn't handle pipes/redirects.
def run_with_redirects(command_string, to_redact = "")
  common = Common.new
  command_to_echo = command_string.clone
  if to_redact
    command_to_echo.sub! to_redact, "*" * to_redact.length
  end
  common.put_command(command_to_echo)
  unless system(command_string)
    raise("Error running: " + command_to_echo)
  end
end

def get_auth_login_account()
  return `gcloud config get-value account`.strip()
end

def drop_cloud_db(cmd_name, *args)
  ensure_docker cmd_name, args
  op = WbOptionsParser.new(cmd_name, args)
  gcc = GcloudContextV2.new(op)
  op.parse.validate
  gcc.validate
  CloudSqlProxyContext.new(gcc.project).run do
    puts "Dropping database..."
    pw = ENV["MYSQL_ROOT_PASSWORD"]
    run_with_redirects(
        "cat db/drop_db.sql | envsubst | " \
        "mysql -u \"root\" -p\"#{pw}\" --host 127.0.0.1 --port 3307",
        pw)
  end
end

Common.register_command({
  :invocation => "drop-cloud-db",
  :description => "Drops the Cloud SQL database for the specified project",
  :fn => ->(*args) { drop_cloud_db("drop-cloud-db", *args) }
})

def drop_cloud_cdr(cmd_name, *args)
  ensure_docker cmd_name, args
  op = WbOptionsParser.new(cmd_name, args)
  gcc = GcloudContextV2.new(op)
  op.parse.validate
  gcc.validate
  CloudSqlProxyContext.new(gcc.project).run do
    puts "Dropping cdr database..."
    pw = ENV["MYSQL_ROOT_PASSWORD"]
    run_with_redirects(
        "cat db-cdr/drop_db.sql | envsubst | " \
        "mysql -u \"root\" -p\"#{pw}\" --host 127.0.0.1 --port 3307",
        pw)
  end
end

Common.register_command({
  :invocation => "drop-cloud-cdr",
  :description => "Drops the cdr schema of Cloud SQL database for the specified project",
  :fn => ->(*args) { drop_cloud_cdr("drop-cloud-cdr", *args) }
})

def run_local_all_migrations()
  ensure_docker_sync()
  common = Common.new
  common.run_inline %W{docker-compose run db-scripts ./run-migrations.sh main}

  init_new_cdr_db %W{--cdr-db-name cdr}
  init_new_cdr_db %W{--cdr-db-name cdr --run-list data --context local}
end

Common.register_command({
  :invocation => "run-local-all-migrations",
  :description => "Runs local data/schema migrations for the cdr and workbench schemas.",
  :fn => ->() { run_local_all_migrations() }
})

def run_local_data_migrations()
  ensure_docker_sync()
  init_new_cdr_db %W{--cdr-db-name cdr --run-list data --context local}
end

Common.register_command({
  :invocation => "run-local-data-migrations",
  :description => "Runs local data migrations for the cdr schema.",
  :fn => ->() { run_local_data_migrations() }
})

def run_local_rw_migrations()
  ensure_docker_sync()
  common = Common.new
  common.run_inline %W{docker-compose run db-scripts ./run-migrations.sh main}
end

Common.register_command({
  :invocation => "run-local-rw-migrations",
  :description => "Runs local migrations for the workbench schema.",
  :fn => ->() { run_local_rw_migrations() }
})

def make_bq_denormalized_tables(cmd_name, *args)
  op = WbOptionsParser.new(cmd_name, args)
  op.add_option(
    "--bq-project [bq-project]",
    ->(opts, v) { opts.bq_project = v},
    "BQ Project. Required."
  )
  op.add_option(
    "--bq-dataset [bq-dataset]",
    ->(opts, v) { opts.bq_dataset = v},
    "BQ dataset. Required."
  )
  op.add_option(
    "--cdr-date [cdr-date]",
    ->(opts, v) { opts.cdr_date = v},
    "CDR date is Required. Please use the date from the source CDR. <YYYY-mm-dd>"
  )
  op.add_validator ->(opts) { raise ArgumentError unless opts.bq_project and opts.bq_dataset and opts.cdr_date }
  op.parse.validate

  common = Common.new
  common.run_inline %W{docker-compose run db-make-bq-tables ./generate-cdr/make-bq-denormalized-tables.sh #{op.opts.bq_project} #{op.opts.bq_dataset} #{op.opts.cdr_date}}
end

Common.register_command({
  :invocation => "make-bq-denormalized-tables",
  :description => "make-bq-denormalized-tables --bq-project <PROJECT> --bq-dataset <DATASET> --cdr-date <YYYY-mm-dd>
Generates big query denormalized tables for search and review. Used by cohort builder. Must be run once when a new cdr is released",
  :fn => ->(*args) { make_bq_denormalized_tables("make-bq-denormalized-tables", *args) }
})

def make_bq_denormalized_review(cmd_name, *args)
  op = WbOptionsParser.new(cmd_name, args)
  op.add_option(
    "--bq-project [bq-project]",
    ->(opts, v) { opts.bq_project = v},
    "BQ Project. Required."
  )
  op.add_option(
    "--bq-dataset [bq-dataset]",
    ->(opts, v) { opts.bq_dataset = v},
    "BQ dataset. Required."
  )
  op.add_validator ->(opts) { raise ArgumentError unless opts.bq_project and opts.bq_dataset }
  op.parse.validate

  common = Common.new
  common.run_inline %W{docker-compose run db-make-bq-tables ./generate-cdr/make-bq-denormalized-review.sh #{op.opts.bq_project} #{op.opts.bq_dataset}}
end

Common.register_command({
  :invocation => "make-bq-denormalized-review",
  :description => "make-bq-denormalized-review --bq-project <PROJECT> --bq-dataset <DATASET>
Generates big query denormalized tables for review. Used by cohort builder. Must be run once when a new cdr is released",
  :fn => ->(*args) { make_bq_denormalized_review("make-bq-denormalized-review", *args) }
})

def make_bq_denormalized_search(cmd_name, *args)
  op = WbOptionsParser.new(cmd_name, args)
  op.add_option(
    "--bq-project [bq-project]",
    ->(opts, v) { opts.bq_project = v},
    "BQ Project. Required."
  )
  op.add_option(
    "--bq-dataset [bq-dataset]",
    ->(opts, v) { opts.bq_dataset = v},
    "BQ dataset. Required."
  )
  op.add_option(
    "--cdr-date [cdr-date]",
    ->(opts, v) { opts.cdr_date = v},
    "CDR date is Required. Please use the date from the source CDR. <YYYY-mm-dd>"
  )
  op.add_validator ->(opts) { raise ArgumentError unless opts.bq_project and opts.bq_dataset and opts.cdr_date }
  op.parse.validate

  common = Common.new
  common.run_inline %W{docker-compose run db-make-bq-tables ./generate-cdr/make-bq-denormalized-search.sh #{op.opts.bq_project} #{op.opts.bq_dataset} #{op.opts.cdr_date}}
end

Common.register_command({
  :invocation => "make-bq-denormalized-search",
  :description => "make-bq-denormalized-search --bq-project <PROJECT> --bq-dataset <DATASET> --cdr-date <YYYY-mm-dd>
Generates big query denormalized search. Used by cohort builder. Must be run once when a new cdr is released",
  :fn => ->(*args) { make_bq_denormalized_search("make-bq-denormalized-search", *args) }
})

def make_bq_denormalized_dataset(cmd_name, *args)
  op = WbOptionsParser.new(cmd_name, args)
  op.add_option(
    "--bq-project [bq-project]",
    ->(opts, v) { opts.bq_project = v},
    "BQ Project. Required."
  )
  op.add_option(
    "--bq-dataset [bq-dataset]",
    ->(opts, v) { opts.bq_dataset = v},
    "BQ dataset. Required."
  )
  op.add_validator ->(opts) { raise ArgumentError unless opts.bq_project and opts.bq_dataset }
  op.parse.validate

  common = Common.new
  common.run_inline %W{docker-compose run db-make-bq-tables ./generate-cdr/make-bq-denormalized-dataset.sh #{op.opts.bq_project} #{op.opts.bq_dataset}}
end

Common.register_command({
                            :invocation => "make-bq-denormalized-dataset",
                            :description => "make-bq-denormalized-dataset --bq-project <PROJECT> --bq-dataset <DATASET>
Generates big query denormalized dataset tables. Used by Data Set Builder. Must be run once when a new cdr is released",
                            :fn => ->(*args) { make_bq_denormalized_dataset("make-bq-denormalized-dataset", *args) }
                        })

def make_bq_dataset_linking(cmd_name, *args)
  op = WbOptionsParser.new(cmd_name, args)
  op.add_option(
    "--bq-project [bq-project]",
    ->(opts, v) { opts.bq_project = v},
    "BQ Project. Required."
  )
  op.add_option(
    "--bq-dataset [bq-dataset]",
    ->(opts, v) { opts.bq_dataset = v},
    "BQ dataset. Required."
  )
  op.add_validator ->(opts) { raise ArgumentError unless opts.bq_project and opts.bq_dataset }
  op.parse.validate

  common = Common.new
  common.run_inline %W{docker-compose run db-make-bq-tables ./generate-cdr/make-bq-dataset-linking.sh #{op.opts.bq_project} #{op.opts.bq_dataset}}
end

Common.register_command({
                            :invocation => "make-bq-dataset-linking",
                            :description => "make-bq-dataset-linking --bq-project <PROJECT> --bq-dataset <DATASET>
Generates big query dataset linking tables. Used by Data Set Builder to show users values information.
Must be run once when a new cdr is released",
                            :fn => ->(*args) { make_bq_dataset_linking("make-bq-dataset-linking", *args) }
                        })

def generate_criteria_table(*args)
  common = Common.new
  common.run_inline %W{docker-compose run db-make-bq-tables ./generate-cdr/generate-criteria-table.sh} + args
end

Common.register_command({
  :invocation => "generate-criteria-table",
  :description => "generate-criteria-table --bq-project <PROJECT> --bq-dataset <DATASET>
Generates the criteria table in big query. Used by cohort builder. Must be run once when a new cdr is released",
  :fn => ->(*args) { generate_criteria_table(*args) }
})

def generate_cb_criteria_tables(*args)
  common = Common.new
  common.run_inline %W{docker-compose run db-make-bq-tables ./generate-cdr/generate-cb-criteria-tables.sh} + args
end

Common.register_command({
  :invocation => "generate-cb-criteria-tables",
  :description => "generate-cb-criteria-tables --bq-project <PROJECT> --bq-dataset <DATASET>
Generates the criteria table in big query. Used by cohort builder. Must be run once when a new cdr is released",
  :fn => ->(*args) { generate_cb_criteria_tables(*args) }
})

def generate_private_cdr_counts(cmd_name, *args)
  op = WbOptionsParser.new(cmd_name, args)
  op.add_option(
    "--bq-project [bq-project]",
    ->(opts, v) { opts.bq_project = v},
    "BQ Project. Required."
  )
  op.add_option(
    "--bq-dataset [bq-dataset]",
    ->(opts, v) { opts.bq_dataset = v},
    "BQ dataset. Required."
  )
  op.add_option(
    "--workbench-project [workbench-project]",
    ->(opts, v) { opts.workbench_project = v},
    "Workbench Project. Required."
  )
  op.add_option(
    "--cdr-version [cdr-version]",
    ->(opts, v) { opts.cdr_version = v},
    "CDR version. Required."
  )
  op.add_option(
    "--bucket [bucket]",
    ->(opts, v) { opts.bucket = v},
    "GCS bucket. Required."
  )
  op.add_validator ->(opts) { raise ArgumentError unless opts.bq_project and opts.bq_dataset and opts.workbench_project and opts.cdr_version and opts.bucket }
  op.parse.validate

  ServiceAccountContext.new(op.opts.workbench_project).run do
    common = Common.new
    common.run_inline %W{docker-compose run db-make-bq-tables ./generate-cdr/generate-private-cdr-counts.sh #{op.opts.bq_project} #{op.opts.bq_dataset} #{op.opts.workbench_project} #{op.opts.cdr_version} #{op.opts.bucket}}
  end
end

Common.register_command({
  :invocation => "generate-private-cdr-counts",
  :description => "generate-private-cdr-counts --bq-project <PROJECT> --bq-dataset <DATASET> --workbench-project <PROJECT> \
 --cdr-version=<''|YYYYMMDD> --bucket <BUCKET>
Generates databases in bigquery with data from a de-identified cdr that will be imported to mysql/cloudsql to be used by workbench.",
  :fn => ->(*args) { generate_private_cdr_counts("generate-private-cdr-counts", *args) }
})

def copy_bq_tables(cmd_name, *args)
  op = WbOptionsParser.new(cmd_name, args)
  op.add_option(
    "--sa-project [sa-project]",
    ->(opts, v) { opts.sa_project = v},
    "Service Account Project. Required."
  )
  op.add_option(
    "--source-dataset [source-dataset]",
    ->(opts, v) { opts.source_dataset = v},
    "Source dataset. Required."
  )
  op.add_option(
    "--destination-dataset [destination-dataset]",
    ->(opts, v) { opts.destination_dataset = v},
    "Destination Dataset. Required."
  )
  op.add_option(
    "--table-prefixes [prefix1,prefix2,...]",
    ->(opts, v) { opts.table_prefixes = v},
    "Comma-delimited table prefixes to filter the publish by, e.g. cb_,ds_. " +
    "This should only be used in special situations e.g. when the auxilliary " +
    "cb_ or ds_ tables need to be updated, or if there was an issue with the " +
    "publish. In general, CDRs should be treated as immutable after the " +
    "initial publish."
  )
  op.add_validator ->(opts) { raise ArgumentError unless opts.sa_project and opts.source_dataset and opts.destination_dataset }
  op.parse.validate

  # This is a grep filter. It matches all tables, by default.
  table_filter = ""
  if op.opts.table_prefixes
    prefixes = op.opts.table_prefixes.split(",")
    table_filter = "^\\(#{prefixes.join("\\|")}\\)"
  end

  source_project = "#{op.opts.source_dataset}".split(':').first
  ServiceAccountContext.new(op.opts.sa_project).run do
    common = Common.new
    common.status "Copying from '#{op.opts.source_dataset}' -> '#{op.opts.dest_dataset}'"
    common.run_inline %W{docker-compose run db-make-bq-tables ./generate-cdr/copy-bq-dataset.sh #{op.opts.source_dataset} #{op.opts.destination_dataset} #{source_project} #{table_filter}}
  end
end

Common.register_command({
  :invocation => "copy-bq-tables",
  :description => "Copies tables or filters from source to destination",
  :fn => ->(*args) { copy_bq_tables("copy-bq-tables", *args) }
})

def cloudsql_import(cmd_name, *args)
  op = WbOptionsParser.new(cmd_name, args)
  op.add_option(
      "--project [project]",
      ->(opts, v) { opts.project = v},
      "Project the Cloud Sql instance is in"
  )
  op.add_option(
      "--instance [instance]",
      ->(opts, v) { opts.instance = v},
      "Cloud SQL instance"
  )
  op.add_option(
      "--database [database]",
      ->(opts, v) { opts.database = v},
      "Database name"
  )
  op.add_option(
      "--bucket [bucket]",
      ->(opts, v) { opts.bucket = v},
      "Name of the GCS bucket containing the SQL dump"
  )
  op.add_option(
      "--file [file]",
      ->(opts, v) { opts.file = v},
      "File name to import"
    )
  op.parse.validate

  ServiceAccountContext.new(op.opts.project).run do
    common = Common.new
    common.run_inline %W{docker-compose run db-cloudsql-import
          --project #{op.opts.project} --instance #{op.opts.instance} --database #{op.opts.database}
          --bucket #{op.opts.bucket} --file #{op.opts.file}}
  end
end


Common.register_command({
  :invocation => "cloudsql-import",
  :description => "cloudsql-import --project <PROJECT> --instance <CLOUDSQL_INSTANCE>
   --database <DATABASE> --bucket <BUCKET> [--create-db-sql-file <SQL.sql>] [--file <ONLY_IMPORT_ME>]
Import bucket of files or a single file in a bucket to a cloudsql database",
                            :fn => ->(*args) { cloudsql_import("cloud-sql-import", *args) }
                        })

def generate_local_cdr_db(*args)
  common = Common.new
  common.run_inline %W{docker-compose run db-make-bq-tables ./generate-cdr/generate-local-cdr-db.sh} + args
end

Common.register_command({
  :invocation => "generate-local-cdr-db",
  :description => "generate-cloudsql-cdr --cdr-version <synth_r_20XXqX_X> --cdr-db-prefix <cdr> --bucket <BUCKET>
Creates and populates local mysql database from data in bucket made by generate-private-cdr-counts.",
  :fn => ->(*args) { generate_local_cdr_db(*args) }
})


def generate_local_count_dbs(*args)
  common = Common.new
  common.run_inline %W{docker-compose run db-make-bq-tables ./generate-cdr/generate-local-count-dbs.sh} + args
end

Common.register_command({
  :invocation => "generate-local-count-dbs",
  :description => "generate-local-count-dbs --cdr-version <synth_r_20XXqX_X> --bucket <BUCKET>
Creates and populates local mysql databases cdr<VERSION> from data in bucket made by generate-private-cdr-counts.",
  :fn => ->(*args) { generate_local_count_dbs(*args) }
})


def mysqldump_db(*args)
  common = Common.new
  common.run_inline %W{docker-compose run db-make-bq-tables ./generate-cdr/make-mysqldump.sh} + args
end


Common.register_command({
  :invocation => "mysqldump-local-db",
  :description => "mysqldump-local-db --db-name <LOCALDB> --bucket <BUCKET>
Dumps the local mysql db and uploads the .sql file to bucket",
  :fn => ->(*args) { mysqldump_db(*args) }
})

def local_mysql_import(cmd_name, *args)
  op = WbOptionsParser.new(cmd_name, args)
  op.add_option(
    "--sql-dump-file [filename]",
    ->(opts, v) { opts.file = v},
    "File name of the SQL dump to import"
  )
  op.add_option(
    "--bucket [bucket]",
    ->(opts, v) { opts.bucket = v},
    "Name of the GCS bucket containing the SQL dump"
  )
  op.parse.validate

  common = Common.new
  common.run_inline %W{docker-compose run db-local-mysql-import
        --sql-dump-file #{op.opts.file} --bucket #{op.opts.bucket}}
end
Common.register_command({
                            :invocation => "local-mysql-import",
                            :description => "local-mysql-import --sql-dump-file <FILE.sql> --bucket <BUCKET>
Imports .sql file to local mysql instance",
                            :fn => ->(*args) { local_mysql_import("local-mysql-import", *args) }
                        })


def run_drop_cdr_db()
  ensure_docker_sync()
  common = Common.new
  common.run_inline %W{docker-compose run cdr-scripts ./run-drop-db.sh}
end

Common.register_command({
  :invocation => "run-drop-cdr-db",
  :description => "Drops the cdr schema of SQL database for the specified project.",
  :fn => ->() { run_drop_cdr_db() }
})


Common.register_command({
  :invocation => "run-cloud-data-migrations",
  :description => "Runs data migrations in the cdr and workbench schemas on the Cloud SQL database for the specified project.",
  :fn => ->(*args) { run_cloud_data_migrations("run-cloud-data-migrations", args) }
})

def write_db_creds_file(project, cdr_db_name, root_password, workbench_password)
  instance_name = "#{project}:us-central1:workbenchmaindb"
  db_creds_file = Tempfile.new("#{project}-vars.env")
  if db_creds_file
    begin
      db_creds_file.puts "DB_CONNECTION_STRING=jdbc:google:mysql://#{instance_name}/workbench?rewriteBatchedStatements=true"
      db_creds_file.puts "DB_DRIVER=com.mysql.jdbc.GoogleDriver"
      db_creds_file.puts "DB_HOST=127.0.0.1"
      db_creds_file.puts "DB_NAME=workbench"
      # TODO: make our CDR migration scripts update *all* CDR versions listed in the cdr_version
      # table of the workbench DB; then this shouldn't be needed anymore.
      db_creds_file.puts "CDR_DB_NAME=#{cdr_db_name}"
      db_creds_file.puts "CLOUD_SQL_INSTANCE=#{instance_name}"
      db_creds_file.puts "LIQUIBASE_DB_USER=liquibase"
      db_creds_file.puts "LIQUIBASE_DB_PASSWORD=#{workbench_password}"
      db_creds_file.puts "MYSQL_ROOT_PASSWORD=#{root_password}"
      db_creds_file.puts "WORKBENCH_DB_USER=workbench"
      db_creds_file.puts "WORKBENCH_DB_PASSWORD=#{workbench_password}"
      db_creds_file.close

      copy_file_to_gcs(db_creds_file.path, "#{project}-credentials", "vars.env")
    ensure
      db_creds_file.unlink
    end
  else
    raise("Error creating file.")
  end
end

def create_auth_domain(cmd_name, args)
  op = WbOptionsParser.new(cmd_name, args)
  op.add_option(
    "--project [project]",
    ->(opts, v) { opts.project = v},
    "Project to create the authorization domain"
  )
  op.parse.validate

  common = Common.new
  common.run_inline %W{gcloud auth login}
  token = common.capture_stdout %W{gcloud auth print-access-token}
  token = token.chomp
  header = "Authorization: Bearer #{token}"
  content_type = "Content-type: application/json"

  domain_name = get_auth_domain(op.opts.project)
  common.run_inline %W{curl -X POST -H #{header} -H #{content_type} -d {}
     https://api-dot-#{op.opts.project}.appspot.com/v1/auth-domain/#{domain_name}}
end

Common.register_command({
  :invocation => "create-auth-domain",
  :description => "Creates an authorization domain in Firecloud for registered users",
    :fn => ->(*args) { create_auth_domain("create-auth-domain", args) }
})

def backfill_billing_project_owners(cmd_name, *args)
  common = Common.new
  ensure_docker cmd_name, args

  op = WbOptionsParser.new(cmd_name, args)
  op.opts.dry_run = true
  op.opts.project = TEST_PROJECT

  op.add_typed_option(
      "--dry_run=[dry_run]",
      TrueClass,
      ->(opts, v) { opts.dry_run = v},
      "When true, print debug lines instead of performing writes. Defaults to true.")

  gcc = GcloudContextV2.new(op)
  op.parse.validate
  gcc.validate()

  if op.opts.dry_run
    common.status "DRY RUN -- CHANGES WILL NOT BE PERSISTED"
  end

  fc_config = get_fc_config(op.opts.project)
  flags = ([
      ["--fc-base-url", fc_config["baseUrl"]],
      ["--billing-project-prefix", get_billing_project_prefix(op.opts.project)]
  ]).map { |kv| "#{kv[0]}=#{kv[1]}" }
  if op.opts.dry_run
    flags += ["--dry-run"]
  end
  # Gradle args need to be single-quote wrapped.
  flags.map! { |f| "'#{f}'" }
  ServiceAccountContext.new(gcc.project).run do
    common.run_inline %W{
        gradle backfillBillingProjectOwners
       -PappArgs=[#{flags.join(',')}]}
  end
end

Common.register_command({
    :invocation => "backfill-billing-project-owners",
    :description => "Backfills billing project owner role for owners",
    :fn => ->(*args) {backfill_billing_project_owners("backfill-billing-project-owners", *args)}
})

def update_user_registered_status(cmd_name, args)
  common = Common.new
  op = WbOptionsParser.new(cmd_name, args)
  op.add_option(
    "--project [project]",
    ->(opts, v) { opts.project = v},
    "Project to update registered status for"
  )
  op.add_option(
    "--disabled [disabled]",
    ->(opts, v) { opts.disabled = v},
    "Disabled state to set: true/false."
  )
  op.add_option(
    "--account [account]",
    ->(opts, v) { opts.account = v},
    "Account to perform update registered status as."
  )
  op.add_option(
    "--user [user]",
    ->(opts, v) { opts.user = v},
    "User to grant or revoke registered access from."
  )
  op.parse.validate

  common.run_inline %W{gcloud auth login}
  token = common.capture_stdout %W{gcloud auth print-access-token}
  token = token.chomp
  common.run_inline %W{gcloud config set account #{op.opts.account}}
  header = "Authorization: Bearer #{token}"
  content_type = "Content-type: application/json"
  payload = "{\"email\": \"#{op.opts.user}\", \"disabled\": \"#{op.opts.disabled}\"}"
  domain_name = get_auth_domain(op.opts.project)
  common.run_inline %W{curl -X POST -H #{header} -H #{content_type}
      -d #{payload} https://#{ENVIRONMENTS[op.opts.project][:api_endpoint_host]}/v1/auth-domain/#{domain_name}/users}
end

Common.register_command({
  :invocation => "update-user-registered-status",
  :description => "Adds or removes a specified user from the registered access domain.\n" \
                  "Accepts three flags: --disabled [true/false], --account [admin email], and --user [target user email]",
  :fn => ->(*args) { update_user_registered_status("update_user_registered_status", args) }
})

def fetch_firecloud_user_profile(cmd_name, *args)
  common = Common.new
  ensure_docker cmd_name, args

  op = WbOptionsParser.new(cmd_name, args)
  op.opts.project = TEST_PROJECT

  op.add_typed_option(
      "--user [user]",
      String,
      ->(opts, v) { opts.user = v},
      "The AoU user to fetch FireCloud data for (e.g. 'gjordan@fake-research-aou.org'")

  # Create a cloud context and apply the DB connection variables to the environment.
  # These will be read by Gradle and passed as Spring Boot properties to the command-line.
  gcc = GcloudContextV2.new(op)
  op.parse.validate
  gcc.validate()

  with_cloud_proxy_and_db(gcc) do
    common.run_inline %W{
        gradle fetchFireCloudUserProfile
       -PappArgs=["#{op.opts.user}"]}
  end
end

Common.register_command({
  :invocation => "fetch-firecloud-user-profile",
  :description => "Fetches and logs FireCloud profile data for an AoU user.\n",
  :fn => ->(*args) {fetch_firecloud_user_profile("fetch-firecloud-user-profile", *args)}
})

def fetch_workspace_details(cmd_name, *args)
  common = Common.new
  ensure_docker cmd_name, args

  op = WbOptionsParser.new(cmd_name, args)
  op.opts.project = TEST_PROJECT

  op.add_typed_option(
      "--workspace-project-id [workspace-project-id]",
      String,
      ->(opts, v) { opts.workspace_project_id = v},
      "Fetches details for workspace(s) that match the given project ID / namespace (e.g. 'aou-rw-231823128'")

  # Create a cloud context and apply the DB connection variables to the environment.
  # These will be read by Gradle and passed as Spring Boot properties to the command-line.
  gcc = GcloudContextV2.new(op)
  op.parse.validate
  gcc.validate()

  fc_config = get_fc_config(op.opts.project)

  flags = ([
      ["--fc-base-url", fc_config["baseUrl"]],
      ["--workspace-project-id", op.opts.workspace_project_id]
  ]).map { |kv| "#{kv[0]}=#{kv[1]}" }
  flags.map! { |f| "'#{f}'" }

  with_cloud_proxy_and_db(gcc) do
    common.run_inline %W{
        gradle fetchWorkspaceDetails
       -PappArgs=[#{flags.join(',')}]}
  end
end

Common.register_command({
    :invocation => "fetch-workspace-details",
    :description => "Fetch workspace details.\n",
    :fn => ->(*args) {fetch_workspace_details("fetch-workspace-details", *args)}
})

def export_workspace_data(cmd_name, *args)
  common = Common.new
  ensure_docker cmd_name, args

  op = WbOptionsParser.new(cmd_name, args)
  op.opts.project = TEST_PROJECT

  op.add_typed_option(
      "--export-filename [export-filename]",
      String,
      ->(opts, v) { opts.export_filename = v},
      "Filename of export file to write to")

  # Create a cloud context and apply the DB connection variables to the environment.
  # These will be read by Gradle and passed as Spring Boot properties to the command-line.
  gcc = GcloudContextV2.new(op)
  op.parse.validate
  gcc.validate()

  flags = ([
      ["--export-filename", op.opts.export_filename]
  ]).map { |kv| "#{kv[0]}=#{kv[1]}" }
  flags.map! { |f| "'#{f}'" }

  with_cloud_proxy_and_db(gcc) do
    common.run_inline %W{
        gradle exportWorkspaceData
       -PappArgs=[#{flags.join(',')}]}
  end
end

Common.register_command({
    :invocation => "export-workspace-data",
    :description => "Export workspace data to CSV.\n",
    :fn => ->(*args) {export_workspace_data("export-workspace-data", *args)}
})

def delete_workspaces(cmd_name, *args)
  common = Common.new
  ensure_docker cmd_name, args

  op = WbOptionsParser.new(cmd_name, args)
  op.opts.dry_run = true
  op.opts.project = TEST_PROJECT

  op.add_typed_option(
      "--dry_run=[dry_run]",
      TrueClass,
      ->(opts, v) { opts.dry_run = v},
      "When true, print debug lines instead of performing writes. Defaults to true.")

  op.add_typed_option(
      "--delete-list-filename [delete-list-filename]",
      String,
      ->(opts, v) { opts.deleteListFilename = v},
      "File containing list of workspaces to delete.
      Each line should contain a single workspace's namespace and firecloud name, separated by a comma
      Example: ws-namespace-1,fc-id-1 \n ws-namespace-2,fc-id-2 \n ws-namespace-3, fc-id-3")

  # Create a cloud context and apply the DB connection variables to the environment.
  # These will be read by Gradle and passed as Spring Boot properties to the command-line.
  gcc = GcloudContextV2.new(op)
  op.parse.validate
  gcc.validate()

  if op.opts.dry_run
    common.status "DRY RUN -- CHANGES WILL NOT BE PERSISTED"
  end

  flags = ([
      ["--delete-list-filename", op.opts.deleteListFilename]
  ]).map { |kv| "#{kv[0]}=#{kv[1]}" }
  if op.opts.dry_run
    flags += ["--dry-run"]
  end
  # Gradle args need to be single-quote wrapped.
  flags.map! { |f| "'#{f}'" }

  with_cloud_proxy_and_db(gcc) do
    common.run_inline %W{
        gradle deleteWorkspaces
       -PappArgs=[#{flags.join(',')}]}
  end
end

DELETE_WORKSPACES_CMD = "delete-workspaces"

Common.register_command({
    :invocation => DELETE_WORKSPACES_CMD,
    :description => "Delete workspaces listed in given file.\n",
    :fn => ->(*args) {delete_workspaces(DELETE_WORKSPACES_CMD, *args)}
})

def authority_options(cmd_name, args)
  op = WbOptionsParser.new(cmd_name, args)
  op.opts.remove = false
  op.opts.dry_run = false
  op.add_option(
       "--email [EMAIL,...]",
       ->(opts, v) { opts.email = v},
       "Comma-separated list of user accounts to change. Required.")
  op.add_option(
      "--authority [AUTHORITY,...]",
      ->(opts, v) { opts.authority = v},
      "Comma-separated list of user authorities to add or remove for the users. ")
  op.add_option(
      "--remove",
      ->(opts, _) { opts.remove = "true"},
      "Remove authorities (rather than adding them.)")
  op.add_option(
      "--dry_run",
      ->(opts, _) { opts.dry_run = "true"},
      "Make no changes.")
  op.add_validator ->(opts) { raise ArgumentError unless opts.email and opts.authority}
  return op
end

def set_authority(cmd_name, *args)
  ensure_docker cmd_name, args
  op = authority_options(cmd_name, args)
  gcc = GcloudContextV2.new(op)
  op.parse.validate
  gcc.validate

  with_cloud_proxy_and_db(gcc) do
    common = Common.new
    common.run_inline %W{
      gradle setAuthority
     -PappArgs=['#{op.opts.email}','#{op.opts.authority}',#{op.opts.remove},#{op.opts.dry_run}]}
  end
end

Common.register_command({
  :invocation => "set-authority",
  :description => "Set user authorities (permissions). See set-authority --help.",
  :fn => ->(*args) { set_authority("set-authority", *args) }
})

def set_authority_local(cmd_name, *args)
  setup_local_environment

  op = authority_options(cmd_name, args)
  op.parse.validate

  app_args = ["-PappArgs=['#{op.opts.email}','#{op.opts.authority}',#{op.opts.remove},#{op.opts.dry_run}]"]
  common = Common.new
  common.run_inline %W{docker-compose run api-scripts ./gradlew setAuthority} + app_args
end

Common.register_command({
  :invocation => "set-authority-local",
  :description => "Set user authorities on a local server (permissions); "\
                  "requires a local server is running (dev-up or run-api). "\
                  "See set-authority-local --help.",
  :fn => ->(*args) { set_authority_local("set-authority-local", *args) }
})

def delete_clusters(cmd_name, *args)
  ensure_docker cmd_name, args
  op = WbOptionsParser.new(cmd_name, args)
  op.opts.dry_run = true
  op.add_option(
      "--min-age-days [DAYS]",
      ->(opts, v) { opts.min_age_days = v},
      "Optional minimum age filter in days for clusters to delete, e.g. 21")
  op.add_option(
      "--ids [CLUSTER_ID1,...]",
      ->(opts, v) { opts.cluster_ids = v},
      "Optional cluster IDs to delete, e.g. 'aou-test-f1-1/all-of-us'")
  op.add_option(
      "--nodry-run",
      ->(opts, _) { opts.dry_run = false},
      "Actually delete clusters, defaults to dry run")
  gcc = GcloudContextV2.new(op)
  op.parse.validate
  gcc.validate

  api_url = get_leo_api_url(gcc.project)
  ServiceAccountContext.new(gcc.project).run do
    common = Common.new
    common.run_inline %W{
       gradle manageClusters
      -PappArgs=['delete','#{api_url}','#{op.opts.min_age_days}','#{op.opts.cluster_ids}',#{op.opts.dry_run}]}
  end
end

Common.register_command({
  :invocation => "delete-clusters",
  :description => "Delete all clusters in this environment",
  :fn => ->(*args) { delete_clusters("delete-clusters", *args) }
})

def describe_cluster(cmd_name, *args)
  ensure_docker cmd_name, args
  op = WbOptionsParser.new(cmd_name, args)
  op.add_option(
      "--id [CLUSTER_ID]",
      ->(opts, v) { opts.cluster_id = v},
      "Required cluster ID to describe, e.g. 'aou-test-f1-1/all-of-us'")
  op.add_option(
      "--project [project]",
      ->(opts, v) { opts.project = v},
      "Optional project ID; by default will infer the project form the cluster ID")
  op.add_validator ->(opts) { raise ArgumentError unless opts.cluster_id }
  op.parse.validate

  # Infer the project from the cluster ID project ID. If for some reason, the
  # target cluster ID does not conform to the current billing prefix (e.g. if we
  # changed the prefix), --project can be used to override this.
  common = Common.new
  matching_prefix = ""
  project_from_cluster = nil
  ENVIRONMENTS.each_key do |env|
    env_prefix = get_billing_project_prefix(env)
    if op.opts.cluster_id.start_with?(env_prefix)
      # Take the most specific prefix match, since prod is a substring of the others.
      if matching_prefix.length < env_prefix.length
        project_from_cluster = env
        matching_prefix = env_prefix
      end
    end
  end
  if project_from_cluster == "local"
    project_from_cluster = TEST_PROJECT
  end
  common.warning "unable to determine project by cluster ID" unless project_from_cluster
  unless op.opts.project
    op.opts.project = project_from_cluster
  end

  # Add the GcloudContext after setting up the project parameter to avoid
  # earlier validation failures.
  gcc = GcloudContextV2.new(op)
  op.parse.validate
  gcc.validate

  api_url = get_leo_api_url(gcc.project)
  ServiceAccountContext.new(gcc.project).run do |ctx|
    common = Common.new
    common.run_inline %W{
       gradle manageClusters
      -PappArgs=['describe','#{api_url}','#{gcc.project}','#{ctx.service_account}','#{op.opts.cluster_id}']}
  end
end

Common.register_command({
  :invocation => "describe-cluster",
  :description => "Describe all cluster in this environment",
  :fn => ->(*args) { describe_cluster("describe-cluster", *args) }
})


def list_clusters(cmd_name, *args)
  ensure_docker cmd_name, args
  op = WbOptionsParser.new(cmd_name, args)
  gcc = GcloudContextV2.new(op)
  op.parse.validate
  gcc.validate

  api_url = get_leo_api_url(gcc.project)
  ServiceAccountContext.new(gcc.project).run do
    common = Common.new
    common.run_inline %W{
      gradle manageClusters -PappArgs=['list','#{api_url}']
    }
  end
end

Common.register_command({
  :invocation => "list-clusters",
  :description => "List all clusters in this environment",
  :fn => ->(*args) { list_clusters("list-clusters", *args) }
})

def load_es_index(cmd_name, *args)
  op = WbOptionsParser.new(cmd_name, args)

  op.opts.env = "local"
  op.add_option(
    "--environment [ENV]",
    ->(opts, v) { opts.env = v},
    "Environment to load into; 'local' or a GCP project name, e.g. " +
    "'all-of-us-workbench-test'")
  op.add_validator ->(opts) { raise ArgumentError unless ENVIRONMENTS.has_key? opts.env }

  op.add_option(
    "--cdr-version [VERSION]",
    ->(opts, v) { opts.cdr_version = v},
    "CDR version, e.g. 'synth_r_2019q1_2', used to name the index. Value " +
    "should eventually match elasticIndexBaseName in the cdr_versions_*.json " +
    "configurations. Defaults to 'cdr' for local runs")

  # TODO(RW-2213): Generalize this subsampling approach for all local development work.
  op.add_option(
      "--participant-inclusion-inverse-prob [DENOMINATOR]",
      ->(opts, v) { opts.inverse_prob = v},
      "The inverse probabilty to index a participant, used to index a " +
      "sample of participants. For example, 1000 would index ~1/1000 of participants in the " +
      "target dataset. Defaults to 1K for local loads (~1K participants on the " +
      "1M participant synthetic CDR), defaults to 1 for any other GCP project.")
  op.parse.validate

  if op.opts.inverse_prob.nil?
    op.opts.inverse_prob = op.opts.env == "local" ? 1000 : 1
  end
  if op.opts.cdr_version.nil?
    raise ArgumentError unless op.opts.env == "local"
    op.opts.cdr_version = 'cdr'
  end

  unless Workbench.in_docker?
    ensure_docker_sync()
    exec(*(%W{docker-compose run --rm es-scripts ./project.rb #{cmd_name}} + args))
  end

  base_url = get_es_base_url(op.opts.env)
  auth_project = op.opts.env == "local" ? nil : op.opts.env

  common = Common.new
  # TODO(calbach): Parameterize most of these flags. For now this is hardcoded
  # to work against the synthetic CDR into a local ES (using test Workbench).
  create_flags = (([
    ['--query-project-id', 'all-of-us-ehr-dev'],
    ['--es-base-url', base_url],
    # Matches cdr_versions_local.json
    ['--cdr-version', op.opts.cdr_version],
    ['--cdr-big-query-dataset', 'all-of-us-ehr-dev.synthetic_cdr20180606'],
    ['--scratch-big-query-dataset', 'all-of-us-ehr-dev.workbench_elastic'],
    ['--scratch-gcs-bucket', 'all-of-us-workbench-test-elastic-exports'],
    ['--participant-inclusion-inverse-prob', op.opts.inverse_prob]
  ] + (auth_project.nil? ? [] : [
    ['--es-auth-project', auth_project]
  ])).map { |kv| "#{kv[0]}=#{kv[1]}" } + [
    '--delete-indices'
    # Gradle args need to be single-quote wrapped.
  ]).map { |f| "'#{f}'" }
  ServiceAccountContext.new((auth_project or TEST_PROJECT)).run do
    common.run_inline %W{gradle elasticSearchIndexer -PappArgs=['create',#{create_flags.join(',')}]}
  end
end

Common.register_command({
  :invocation => "load-es-index",
  :description => "Create Elasticsearch index",
  :fn => ->(*args) { load_es_index("load-es-index", *args) }
})

def update_cdr_version_options(cmd_name, args)
  op = WbOptionsParser.new(cmd_name, args)
  op.opts.dry_run = false
  op.add_option(
      "--dry_run",
      ->(opts, _) { opts.dry_run = "true"},
      "Make no changes.")
  return op
end

def update_cdr_versions_for_project(versions_file, dry_run)
  common = Common.new
  common.run_inline %W{
    gradle updateCdrVersions
   -PappArgs=['#{versions_file}',#{dry_run}]}
end

def update_cdr_versions(cmd_name, *args)
  ensure_docker cmd_name, args
  op = update_cdr_version_options(cmd_name, args)
  gcc = GcloudContextV2.new(op)
  op.parse.validate
  gcc.validate

  with_cloud_proxy_and_db(gcc) do
    versions_file = must_get_env_value(gcc.project, :cdr_versions_json)
    update_cdr_versions_for_project("/w/api/config/#{versions_file}", op.opts.dry_run)
  end
end

Common.register_command({
  :invocation => "update-cdr-versions",
  :description => "Update CDR versions in a cloud environment",
  :fn => ->(*args) { update_cdr_versions("update-cdr-versions", *args)}
})

def update_cdr_versions_local(cmd_name, *args)
  ensure_docker_sync()
  setup_local_environment
  op = update_cdr_version_options(cmd_name, args)
  op.parse.validate
  versions_file = 'config/cdr_versions_local.json'
  app_args = ["-PappArgs=['/w/api/" + versions_file + "',false]"]
  common = Common.new
  common.run_inline %W{docker-compose run api-scripts ./gradlew updateCdrVersions} + app_args
end

Common.register_command({
  :invocation => "update-cdr-versions-local",
  :description => "Update CDR versions in the local environment",
  :fn => ->(*args) { update_cdr_versions_local("update-cdr-versions-local", *args)}
})

def get_test_service_account()
  ServiceAccountContext.new(TEST_PROJECT).run do
    print "Service account key is now in sa-key.json"
  end
end

Common.register_command({
  :invocation => "get-test-service-creds",
  :description => "Copies sa-key.json locally (for use when running tests from an IDE, etc).",
  :fn => ->() { get_test_service_account()}
})

def connect_to_cloud_db(cmd_name, *args)
  ensure_docker cmd_name, args
  common = Common.new
  op = WbOptionsParser.new(cmd_name, args)
  op.add_option(
    "--root",
    ->(opts, _) { opts.root = true },
    "Connect as root")
  gcc = GcloudContextV2.new(op)
  op.parse.validate
  gcc.validate
  env = read_db_vars(gcc)
  CloudSqlProxyContext.new(gcc.project).run do
    password = op.opts.root ? env["MYSQL_ROOT_PASSWORD"] : env["WORKBENCH_DB_PASSWORD"]
    user = op.opts.root ? "root" : env["WORKBENCH_DB_USER"]
    common.run_inline %W{
      mysql --host=127.0.0.1 --port=3307 --user=#{user}
      --database=#{env["DB_NAME"]} --password=#{password}},
      password
  end
end

Common.register_command({
  :invocation => "connect-to-cloud-db",
  :description => "Connect to a Cloud SQL database via mysql.",
  :fn => ->(*args) { connect_to_cloud_db("connect-to-cloud-db", *args) }
})

def connect_to_cloud_db_binlog(cmd_name, *args)
  ensure_docker cmd_name, args
  common = Common.new
  op = WbOptionsParser.new(cmd_name, args)
  gcc = GcloudContextV2.new(op)
  op.parse.validate
  gcc.validate
  env = read_db_vars(gcc)
  CloudSqlProxyContext.new(gcc.project).run do
    common.status "\n" + "*" * 80
    common.status "Listing available journal files: "

    # "root" is required for binlog access.
    password = env["MYSQL_ROOT_PASSWORD"]
    run_with_redirects(
      "echo 'SHOW BINARY LOGS;' | " +
      "mysql --host=127.0.0.1 --port=3307 --user=root " +
      "--database=#{env['DB_NAME']} --password=#{password}", password)
    common.status "*" * 80

    common.status "\n" + "*" * 80
    common.status "mysql login has been configured. Pick a journal file from " +
                  "above and run commands like: "
    common.status "  mysqlbinlog -R mysql-bin.xxxxxx\n"
    common.status "See the Workbench playbook for more details."
    common.status "*" * 80

    # Work out of /tmp for easy local file redirection. We don't want binlogs
    # winding up back in Workbench source control accidentally.
    run_with_redirects(
      "export MYSQL_HOME=$(with-mysql-login.sh root #{password}); " +
      "cd /tmp; /bin/bash", password)
  end
end

Common.register_command({
  :invocation => "connect-to-cloud-db-binlog",
  :description => "Connect to a Cloud SQL database for mysqlbinlog access",
  :fn => ->(*args) { connect_to_cloud_db_binlog("connect-to-cloud-db-binlog", *args) }
})


def deploy_gcs_artifacts(cmd_name, args)
  ensure_docker cmd_name, args

  common = Common.new
  op = WbOptionsParser.new(cmd_name, args)
  op.opts.dry_run = false
  op.add_option(
    "--dry-run",
    ->(opts, _) { opts.dry_run = true},
    "Don't actually push, just log the command lines which would be " +
    "executed on a real invocation."
  )
  gcc = GcloudContextV2.new(op)
  op.parse.validate
  gcc.validate
  auth_domain_group = get_auth_domain_group(gcc.project)

  Dir.chdir("cluster-resources") do
    common.run_inline(%W{./build.rb build-snippets-menu})
    run_inline_or_log(op.opts.dry_run, %W{
      gsutil cp
      initialize_notebook_cluster.sh
      start_notebook_cluster.sh
      activity-checker-extension.js
      aou-download-policy-extension.js
      aou-upload-policy-extension.js
      generated/aou-snippets-menu.js
      gs://#{gcc.project}-cluster-resources/
    })
    # This bucket must be readable by all AoU researchers and their pet service accounts
    # account (https://github.com/DataBiosphere/leonardo/issues/220). Sharing with all
    # registered users. The firecloud.org check is to avoid circular requirements in
    # environment setup
    if !auth_domain_group.nil? and !auth_domain_group.empty?
      run_inline_or_log(op.opts.dry_run, %W{
        gsutil iam ch group:#{auth_domain_group}:objectViewer gs://#{gcc.project}-cluster-resources
      })
    end
  end
end

Common.register_command({
  :invocation => "deploy-gcs-artifacts",
  :description => "Deploys any GCS artifacts associated with this environment.",
  :fn => ->(*args) { deploy_gcs_artifacts("deploy-gcs-artifacts", args) }
})

def deploy_app(cmd_name, args, with_cron, with_gsuite_admin)
  common = Common.new
  op = WbOptionsParser.new(cmd_name, args)
  op.opts.dry_run = false
  op.add_option(
    "--version [version]",
    ->(opts, v) { opts.version = v},
    "Version to deploy (e.g. your-username-test)"
  )
  op.add_option(
    "--promote",
    ->(opts, _) { opts.promote = true},
    "Promote this deploy to make it available at the root URL"
  )
  op.add_option(
    "--no-promote",
    ->(opts, _) { opts.promote = false},
    "Do not promote this deploy to make it available at the root URL"
  )
  op.add_option(
    "--dry-run",
    ->(opts, _) { opts.dry_run = true},
    "Don't actually deploy, just log the command lines which would be " +
    "executed on a real invocation."
  )
  op.add_option(
    "--quiet",
    ->(opts, _) { opts.quiet = true},
    "Don't display a confirmation prompt when deploying"
  )
  gcc = GcloudContextV2.new(op)
  op.parse.validate
  gcc.validate

  ENV.update(read_db_vars(gcc))
  ENV.update(must_get_env_value(gcc.project, :gae_vars))
  ENV.update({"WORKBENCH_ENV" => must_get_env_value(gcc.project, :env_name)})

  # Clear out generated files, which may be out of date; they will be regenerated by appengineStage.
  common.run_inline %W{rm -rf src/generated}
  if with_gsuite_admin
    common.run_inline %W{rm -f #{GSUITE_ADMIN_KEY_PATH}}
    # TODO: generate new key here
    get_gsuite_admin_key(gcc.project)
  end
  Dir.chdir("cluster-resources") do
    common.run_inline(%W{./build.rb generate-static-files})
  end
  common.run_inline %W{gradle :appengineStage}
  promote = "--no-promote"
  unless op.opts.promote.nil?
    promote = op.opts.promote ? "--promote" : "--no-promote"
  else
    promote = op.opts.version ? "--no-promote" : "--promote"
  end

  run_inline_or_log(op.opts.dry_run, %W{
    gcloud app deploy
      build/staged-app/app.yaml
  } + (with_cron ? %W{build/staged-app/WEB-INF/appengine-generated/cron.yaml} : []) +
    %W{--project #{gcc.project} #{promote}} +
    (op.opts.quiet ? %W{--quiet} : []) +
    (op.opts.version ? %W{--version #{op.opts.version}} : []))
end

def deploy_api(cmd_name, args)
  ensure_docker cmd_name, args
  common = Common.new
  common.status "Deploying api..."
  deploy_app(cmd_name, args, true, true)
end

Common.register_command({
  :invocation => "deploy-api",
  :description => "Deploys the API server to the specified cloud project.",
  :fn => ->(*args) { deploy_api("deploy-api", args) }
})

def create_workbench_db()
  run_with_redirects(
    "cat db/create_db.sql | envsubst | " \
    "mysql -u \"root\" -p\"#{ENV["MYSQL_ROOT_PASSWORD"]}\" --host 127.0.0.1 --port 3307",
    ENV["MYSQL_ROOT_PASSWORD"]
  )
end

def migrate_database(dry_run = false)
  common = Common.new
  common.status "Migrating main database..."
  Dir.chdir("db") do
    run_inline_or_log(dry_run, %W{gradle update -PrunList=main})
  end
end

def get_fc_config(project)
  config_json = must_get_env_value(project, :config_json)
  return JSON.parse(File.read("config/#{config_json}"))["firecloud"]
end

def get_billing_config(project)
  config_json = must_get_env_value(project, :config_json)
  return JSON.parse(File.read("config/#{config_json}"))["billing"]
end

def get_billing_project_prefix(project)
  return get_billing_config(project)["projectNamePrefix"]
end

def get_leo_api_url(project)
  return get_fc_config(project)["leoBaseUrl"]
end

def get_auth_domain(project)
  return get_fc_config(project)["registeredDomainName"]
end

def get_auth_domain_group(project)
  return get_fc_config(project)["registeredDomainGroup"]
end

def get_firecloud_base_url(project)
  return get_fc_config(project)["baseUrl"]
end

def get_es_base_url(env)
  config_json = must_get_env_value(env, :config_json)
  return JSON.parse(File.read("config/#{config_json}"))["elasticsearch"]["baseUrl"]
end

def load_config(project, dry_run = false)
  config_json = must_get_env_value(project, :config_json)
  featured_workspaces_json = must_get_env_value(project, :featured_workspaces_json)
  unless config_json and featured_workspaces_json
    raise("unknown project #{project}, expected one of #{configs.keys}")
  end

  common = Common.new
  common.status "Loading #{config_json} into database..."
  run_inline_or_log(dry_run, %W{gradle loadConfig -Pconfig_key=main -Pconfig_file=config/#{config_json}})
  run_inline_or_log(dry_run, %W{gradle loadConfig -Pconfig_key=cdrBigQuerySchema -Pconfig_file=config/cdm/cdm_5_2.json})
  run_inline_or_log(dry_run, %W{gradle loadConfig -Pconfig_key=featuredWorkspaces -Pconfig_file=config/#{featured_workspaces_json}})
end

def with_cloud_proxy_and_db(gcc, service_account = nil, key_file = nil)
  ENV.update(read_db_vars(gcc))
  ENV.update(must_get_env_value(gcc.project, :gae_vars))
  ENV["DB_PORT"] = "3307" # TODO(dmohs): Use MYSQL_TCP_PORT to be consistent with mysql CLI.
  CloudSqlProxyContext.new(gcc.project, service_account, key_file).run do
    yield(gcc)
  end
end

def with_cloud_proxy_and_db_env(cmd_name, args)
  op = WbOptionsParser.new(cmd_name, args)
  gcc = GcloudContextV2.new(op)
  op.parse.validate
  gcc.validate
  with_cloud_proxy_and_db(gcc) do |ctx|
    yield ctx
  end
end

def deploy(cmd_name, args)
  ensure_docker cmd_name, args

  op = WbOptionsParser.new(cmd_name, args)
  op.opts.dry_run = false
  op.add_option(
    "--account [account]",
    ->(opts, v) { opts.account = v},
    "Service account to act as for deployment, if any. Defaults to the GAE " +
    "default service account."
  )
  op.add_option(
    "--version [version]",
    ->(opts, v) { opts.version = v},
    "Version to deploy (e.g. your-username-test)"
  )
  op.add_validator ->(opts) { raise ArgumentError unless opts.version}
  op.add_option(
    "--key-file [keyfile]",
    ->(opts, v) { opts.key_file = v},
    "Service account key file to use for deployment authorization"
  )
  op.add_option(
    "--dry-run",
    ->(opts, _) { opts.dry_run = true},
    "Don't actually deploy, just log the command lines which would be " +
    "executed on a real invocation."
  )
  op.add_option(
    "--promote",
    ->(opts, _) { opts.promote = true},
    "Promote this version to immediately begin serving API traffic"
  )
  op.add_option(
    "--no-promote",
    ->(opts, _) { opts.promote = false},
    "Deploy, but do not yet serve traffic from this version - DB migrations are still applied"
  )
  op.add_validator ->(opts) { raise ArgumentError if opts.promote.nil?}

  gcc = GcloudContextV2.new(op)
  op.parse.validate
  gcc.validate

  common = Common.new
  common.status "Running database migrations..."
  with_cloud_proxy_and_db(gcc, op.opts.account, op.opts.key_file) do |ctx|
    migrate_database(op.opts.dry_run)
    load_config(ctx.project, op.opts.dry_run)
    versions_file = must_get_env_value(gcc.project, :cdr_versions_json)
    update_cdr_versions_for_project("config/#{versions_file}", op.opts.dry_run)

    common.run_inline %W{gradle loadDataDictionary -PappArgs=#{op.opts.dry_run ? true : false}}

    common.status "Pushing GCS artifacts..."
    dry_flag = op.opts.dry_run ? %W{--dry-run} : []
    deploy_gcs_artifacts(cmd_name, %W{--project #{ctx.project}} + dry_flag)

    # Keep the cloud proxy context open for the service account credentials.
    deploy_args = %W{
      --project #{gcc.project}
      --version #{op.opts.version}
      #{op.opts.promote ? "--promote" : "--no-promote"}
      --quiet
    } + dry_flag
    deploy_api(cmd_name, deploy_args)
  end
end

Common.register_command({
  :invocation => "deploy",
  :description => "Run DB migrations and deploy the API server",
  :fn => ->(*args) { deploy("deploy", args) }
})


def run_cloud_migrations(cmd_name, args)
  ensure_docker cmd_name, args
  with_cloud_proxy_and_db_env(cmd_name, args) { migrate_database }
end

Common.register_command({
  :invocation => "run-cloud-migrations",
  :description => "Runs database migrations on the Cloud SQL database for the specified project.",
  :fn => ->(*args) { run_cloud_migrations("run-cloud-migrations", args) }
})

def update_cloud_config(cmd_name, args)
  ensure_docker cmd_name, args
  with_cloud_proxy_and_db_env(cmd_name, args) do |ctx|
    load_config(ctx.project)
  end
end

Common.register_command({
  :invocation => "update-cloud-config",
  :description => "Updates configuration in Cloud SQL database for the specified project.",
  :fn => ->(*args) { update_cloud_config("update-cloud-config", args) }
})

def docker_run(args)
  Common.new.run_inline %W{docker-compose run --rm scripts} + args
end

Common.register_command({
  :invocation => "docker-run",
  :description => "Runs the specified command in a docker container.",
  :fn => ->(*args) { docker_run(args) }
})

def print_scoped_access_token(cmd_name, args)
  ensure_docker cmd_name, args
  op = WbOptionsParser.new(cmd_name, args)
  op.add_typed_option(
    "--scopes s1,s2,s3",
    Array,
    ->(opts, v) { opts.scopes = v},
    "Action to perform: add/remove."
  )
  gcc = GcloudContextV2.new(op)
  op.parse.validate
  gcc.validate
  ServiceAccountContext.new(gcc.project).run do
    if op.opts.scopes.nil?
      op.opts.scopes = []
    end

    scopes = %W{profile email} + op.opts.scopes

    require "googleauth"

    File.open(ServiceAccountContext::SERVICE_ACCOUNT_KEY_PATH) do |f|
      creds = Google::Auth::ServiceAccountCredentials.make_creds(
          json_key_io: f,
          scope: scopes
      )

      token_data = creds.fetch_access_token!
      puts "\n#{token_data["access_token"]}"
    end

  end
end

Common.register_command({
  :invocation => "print-scoped-sa-access-token",
  :description => "Prints access token for the service account that has been scoped for API access.",
  :fn => ->(*args) { print_scoped_access_token("print-scoped-sa-access-token", args) }
})

def create_project_resources(gcc)
  common = Common.new
  common.status "Enabling Cloud Service APIs..."
  for service in SERVICES
    common.run_inline("gcloud services enable #{service} --project #{gcc.project}")
  end
  common.status "Creating GCS bucket to store credentials..."
  common.run_inline %W{gsutil mb -p #{gcc.project} -c regional -l us-central1 gs://#{gcc.project}-credentials/}
  common.status "Creating GCS bucket to store scripts..."
  common.run_inline %W{gsutil mb -p #{gcc.project} -c regional -l us-central1 gs://#{gcc.project}-cluster-resources/}
  common.status "Creating Cloud SQL instances..."
  common.run_inline %W{gcloud sql instances create #{INSTANCE_NAME} --tier=db-n1-standard-2
                       --activation-policy=ALWAYS --backup-start-time 00:00 --require-ssl
                       --failover-replica-name #{FAILOVER_INSTANCE_NAME} --enable-bin-log
                       --database-version MYSQL_5_7 --project #{gcc.project} --storage-auto-increase --async --maintenance-release-channel preview --maintenance-window-day SAT --maintenance-window-hour 5}
  common.status "Waiting for database instance to become ready..."
  loop do
    sleep 3.0
    db_status = `gcloud sql instances describe workbenchmaindb --project #{gcc.project} | grep state`
    common.status "DB status: #{db_status}"
    break if db_status.include? "RUNNABLE"
  end
  common.status "Creating AppEngine app..."
  common.run_inline %W{gcloud app create --region us-central --project #{gcc.project}}
end

def setup_project_data(gcc, cdr_db_name)
  root_password, workbench_password = random_password(), random_password()

  common = Common.new
  # This changes database connection information; don't call this while the server is running!
  common.status "Writing DB credentials file..."
  write_db_creds_file(gcc.project, cdr_db_name, root_password, workbench_password)
  common.status "Setting root password..."
  run_with_redirects("gcloud sql users set-password root --host % --project #{gcc.project} " +
                     "--instance #{INSTANCE_NAME} --password #{root_password}",
                     root_password)
  # Don't delete the credentials created here; they will be stored in GCS and reused during
  # deployment, etc.
  with_cloud_proxy_and_db(gcc) do
    common.status "Copying GSuite service account key to GCS..."
    gsuite_admin_creds_file = Tempfile.new("gsuite-admin-sa.json").path
    common.run_inline %W{gcloud iam service-accounts keys create #{gsuite_admin_creds_file}
        --iam-account=gsuite-admin@#{gcc.project}.iam.gserviceaccount.com --project=#{gcc.project}}
    common.run_inline %W{gsutil cp #{gsuite_admin_creds_file} gs://#{gcc.project}-credentials/gsuite-admin-sa.json}

    common.status "Copying FireCloud service account key to GCS..."
    firecloud_admin_creds_file = Tempfile.new("firecloud-admin-sa.json").path
    common.run_inline %W{gcloud iam service-accounts keys create #{firecloud_admin_creds_file}
        --iam-account=firecloud-admin@#{gcc.project}.iam.gserviceaccount.com --project=#{gcc.project}}
    common.run_inline %W{gsutil cp #{firecloud_admin_creds_file} gs://#{gcc.project}-credentials/firecloud-admin-sa.json}


    common.status "Setting up databases and users..."
    create_workbench_db

    common.status "Running schema migrations..."
    migrate_database

  end
end

def random_password()
  return rand(36**20).to_s(36)
end

# TODO: add a goal which updates passwords but nothing else
# TODO: add a goal which updates CDR DBs but nothing else

def setup_cloud_project(cmd_name, *args)
  ensure_docker cmd_name, args
  op = WbOptionsParser.new(cmd_name, args)
  op.add_option(
    "--cdr-db-name [CDR_DB]",
    ->(opts, v) { opts.cdr_db_name = v},
    "Name of the default CDR db to use; required. (example: cdr20180206) This will subsequently " +
    "be created by cloudsql-import."
  )
  op.add_validator ->(opts) { raise ArgumentError unless opts.cdr_db_name}
  gcc = GcloudContextV2.new(op)

  op.parse.validate
  gcc.validate

  # create_project_resources(gcc)
  # setup_project_data(gcc, op.opts.cdr_db_name)
  deploy_gcs_artifacts(cmd_name, %W{--project #{gcc.project}})
end

Common.register_command({
  :invocation => "setup-cloud-project",
  :description => "Initializes resources within a cloud project that has already been created",
  :fn => ->(*args) { setup_cloud_project("setup-cloud-project", *args) }
})

def start_api_and_incremental_build(cmd_name, args)
  ensure_docker cmd_name, args
  common = Common.new
  begin
    common.status "API server startup..."
    bm = Benchmark.measure {
      # appengineStart must be run with the Gradle daemon or it will stop outputting logs as soon as
      # the application has finished starting.
      common.run_inline %W{gradle --daemon appengineStart}
      common.run_inline "tail -f -n 0 /w/api/build/dev-appserver-out/dev_appserver.out &"
      # incrementalHotSwap must be run without the Gradle daemon or stdout and stderr will not appear
      # in the output.
    }
    common.status "API server startup complete (#{format_benchmark(bm)})"

    common.run_inline %W{gradle --no-daemon --continuous incrementalHotSwap}
  ensure
    common.run_inline %W{gradle --stop}
  end
end

# TODO(dmohs): This is really isn't meant to be run directly, so it'd be better to hide it from the
# menu of options.
Common.register_command({
  :invocation => "start-api-and-incremental-build",
  :description => "Used internally by other commands.",
  :fn => ->(*args) { start_api_and_incremental_build("start-api-and-incremental-build", args) }
})
