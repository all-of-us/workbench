# Calls to common.run_inline in this file may use a quoted string purposefully
# to cause system() or spawn() to run the command in a shell. Calls with arrays
# are not run in a shell, which can break usage of the CloudSQL proxy.

require_relative "../../aou-utils/serviceaccounts"
require_relative "../../aou-utils/utils/common"
require_relative "../../aou-utils/workbench"
require_relative "affirm"
require_relative "cloudsqlproxycontext"
require_relative "environments"
require_relative "gcloudcontext"
require_relative "wboptionsparser"
require "benchmark"
require "fileutils"
require "optparse"
require "ostruct"
require "tempfile"
require "net/http"
require "json"

INSTANCE_NAME = "workbenchmaindb"
FAILOVER_INSTANCE_NAME = "workbenchbackupdb"
SERVICES = %W{servicemanagement.googleapis.com storage-component.googleapis.com iam.googleapis.com
              compute.googleapis.com admin.googleapis.com appengine.googleapis.com
              cloudbilling.googleapis.com sqladmin.googleapis.com sql-component.googleapis.com
              clouderrorreporting.googleapis.com bigquery-json.googleapis.com}
DRY_RUN_CMD = %W{echo [DRY_RUN]}

def run_inline_or_log(dry_run, args)
  cmd_prefix = dry_run ? DRY_RUN_CMD : []
  Common.new.run_inline(cmd_prefix + args)
end

def get_cdr_sql_project(project)
  return must_get_env_value(project, :cdr_sql_instance).split(":")[0]
end

def init_new_cdr_db(args)
  Dir.chdir('db-cdr') do
    Common.new.run_inline %W{./generate-cdr/init-new-cdr-db.sh} + args
  end
end

def gcs_vars_path(project)
  return "gs://#{project}-credentials/vars.env"
end

def read_db_vars(gcc)
  vars = Workbench.read_vars(Common.new.capture_stdout(%W{
    gsutil cat #{gcs_vars_path(gcc.project)}
  }))
  if vars.empty?
    Common.new.error "Failed to read #{gcs_vars_path(gcc.project)}"
    exit 1
  end
  # Note: CDR project and target project may be the same.
  cdr_project = get_cdr_sql_project(gcc.project)
  cdr_vars_path = gcs_vars_path(cdr_project)
  cdr_vars = Workbench.read_vars(Common.new.capture_stdout(%W{
    gsutil cat #{cdr_vars_path}
  }))
  if cdr_vars.empty?
    Common.new.error "Failed to read #{cdr_vars_path}"
    exit 1
  end
  vars["GOOGLE_APPLICATION_CREDENTIALS"] = "sa-key.json"
  return vars.merge({
    'CDR_DB_CONNECTION_STRING' => cdr_vars['DB_CONNECTION_STRING'],
    'CDR_DB_USER' => cdr_vars['WORKBENCH_DB_USER'],
    'CDR_DB_PASSWORD' => cdr_vars['WORKBENCH_DB_PASSWORD']
  })
end

def format_benchmark(bm)
  "%ds" % [bm.real]
end

DEADLINE_SEC = 120
def start_local_db_service()
  common = Common.new

  bm = Benchmark.measure {
    common.run_inline %W{docker compose up -d db}

    root_pass = "root-notasecret"

    common.status "waiting up to #{DEADLINE_SEC}s for mysql service to start..."
    start = Time.now
    until (common.run "docker compose exec -T db mysql -p#{root_pass} --silent -e 'SELECT 1;'").success?
      if Time.now - start >= DEADLINE_SEC
        raise("mysql docker service did not become available after #{DEADLINE_SEC}s")
      end
      sleep 1
    end
  }
  common.status "Database startup complete (#{format_benchmark(bm)})"
end

def dev_up_tanagra(cmd_name, args)
  op = WbOptionsParser.new(cmd_name, args)
  op.opts.disable_auth = false
  op.opts.drop_db = false
  op.add_option(
    "--disable-auth",
    ->(opts, _) { opts.disable_auth = true },
    "Disable Tanagra AuthN/AuthZ")
  op.add_option(
    "--debug",
    ->(opts, _) { opts.debug = true },
    "Start tanagra in debug mode")
  op.add_option(
    "--drop-db",
    ->(opts, _) { opts.drop_db = true },
    "Drop Tanagra db")
  op.add_option(
    "--version [version]",
    ->(opts, v) { opts.version = v},
    "Tanagra version"
  )
  op.add_option(
    "--branch [branch]",
    ->(opts, v) { opts.branch = v},
    "Tanagra branch"
  )
  op.parse.validate

  if (op.opts.version && op.opts.branch)
    puts "Please only provide version or branch as an arg"
    exit 1
  end

  ENV["GOOGLE_APPLICATION_CREDENTIALS"] = File.expand_path("sa-key.json")
  env_project = ENVIRONMENTS["local"]
  underlays = env_project.fetch(:tanagra_underlay_files)

  common = Common.new
  common.status "Setting up local environment for tanagra API"
  setup_local_environment()

  common.status "starting local database - mariadb"
  start_local_db_service()

  Dir.chdir('../tanagra-aou-utils') do
    if op.opts.version
      common.run_inline("../ui/project.rb tanagra-dep --env local --version #{op.opts.version}")
    elsif op.opts.branch
      common.run_inline("../ui/project.rb tanagra-dep --env local --branch #{op.opts.branch}")
    else
      common.run_inline("../ui/project.rb tanagra-dep --env local")
    end
    dis_auth = op.opts.disable_auth ? '-a ' : ''
    d_db = op.opts.drop_db ? '-d ' : ''
    debug = op.opts.debug ? '-b ' : ''
    args = dis_auth + d_db + debug + "-u #{underlays}"
    common.status "Starting Tanagra API server"
    common.run_inline("./run_tanagra_server.sh #{args}")
  end
end

Common.register_command({
  :invocation => "dev-up-tanagra",
  :description => "Brings up tanagra service environment and connects to db.",
  :fn => ->(*args) { dev_up_tanagra("dev-up-tanagra",args) }
})

def dev_up(cmd_name, args)
  op = WbOptionsParser.new(cmd_name, args)
  op.opts.start_db = true
  op.add_option(
      "--nostart-db",
      ->(opts, _) { opts.start_db = false },
      "If specified, don't start the DB service. This is useful when running " +
      "within docker, i.e. on CircleCI, as the DB service runs via docker compose")
  op.parse.validate

  common = Common.new

  account = get_auth_login_account()
  if account.nil?
    raise("Please run 'gcloud auth login' before starting the server.")
  end

  at_exit do
    common.run_inline %W{docker compose down} if op.opts.start_db
  end

  setup_local_environment()

  overall_bm = Benchmark.measure {
    start_local_db_service() if op.opts.start_db

    common.status "Database init & migrations..."
    bm = Benchmark.measure {
      Dir.chdir('db') do
        common.run_inline %W{./run-migrations.sh main}
      end
      init_new_cdr_db %W{--cdr-db-name cdr}
    }
    common.status "Database init & migrations complete (#{format_benchmark(bm)})"

    common.status "Loading configs & data..."
    bm = Benchmark.measure {
      common.run_inline %W{./libproject/load_local_data_and_configs.sh}
    }
    common.status "Loading configs complete (#{format_benchmark(bm)})"
  }
  common.status "Total dev-env setup time: #{format_benchmark(overall_bm)}"

  run_api_incremental()
end

Common.register_command({
  :invocation => "dev-up",
  :description => "Brings up the development environment, including db migrations and config " \
     "update. (You can use run-api instead if database and config are up-to-date.)",
  :fn => ->(*args) { dev_up("dev-up", args) }
})

def setup_local_environment()
  ENV.update(Workbench.read_vars_file("db/local-vars.env"))
  ENV.update(must_get_env_value("local", :gae_vars))
  ENV.update({"WORKBENCH_ENV" => "local"})
end

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

def run_api_incremental()
  common = Common.new

  # The GAE gradle configuration depends on the existence of an sa-key.json file for auth.
  get_test_service_account()

  begin
    common.status "Starting API server..."
    # appengineStart must be run with the Gradle daemon or it will stop outputting logs as soon as
    # the application has finished starting.
    common.run_inline "./gradlew --daemon appengineRun"

  rescue Interrupt
    # Do nothing
  ensure
    common.run_inline %W{./gradlew --stop}
  end
end

def run_api_and_db()
  setup_local_environment

  common = Common.new
  at_exit { common.run_inline %W{docker compose down} }
  start_local_db_service()

  run_api_incremental()
end

Common.register_command({
  :invocation => "run-api",
  :description => "Runs the api server (assumes database and config are already up-to-date.)",
  :fn => ->() { run_api_and_db() }
})


def validate_swagger(cmd_name, args)
  Common.new.run_inline %W{./gradlew validateSwagger} + args
end

Common.register_command({
  :invocation => "validate-swagger",
  :description => "Validate swagger definition files",
  :fn => ->(*args) { validate_swagger("validate-swagger", args) }
})


def run_tests(cmd_name, args)
  Common.new.run_inline %W{./gradlew :test} + args
end

Common.register_command({
  :invocation => "test",
  :description => "Runs all unit tests. To run a single test, add (for example) " \
      "--tests org.pmiops.workbench.interceptors.AuthInterceptorTest",
  :fn => ->(*args) { run_tests("test", args) }
})

def run_integration_tests(cmd_name, *args)
  common = Common.new
  ServiceAccountContext.new(TEST_PROJECT).run do
    common.run_inline %W{./gradlew integrationTest} + args
  end
end

Common.register_command({
  :invocation => "integration",
  :description => "Runs integration tests. Excludes nightly-only tests.",
  :fn => ->(*args) { run_integration_tests("integration", *args) }
})

def run_bigquery_tests(cmd_name, *args)
  common = Common.new
  ServiceAccountContext.new(TEST_PROJECT).run do
    common.run_inline %W{./gradlew bigQueryTest} + args
  end
end

Common.register_command({
  :invocation => "bigquerytest",
  :description => "Runs bigquerytest tests.",
  :fn => ->(*args) { run_bigquery_tests("bigquerytest", *args) }
})

def connect_to_db()
  common = Common.new
  common.status "Starting database if necessary..."
  start_local_db_service()
  cmd = "MYSQL_PWD=root-notasecret mysql --database=workbench"
  common.run_inline %W{docker compose exec db sh -c #{cmd}}
end

Common.register_command({
  :invocation => "connect-to-db",
  :description => "Connect to the running database via mysql.",
  :fn => ->() { connect_to_db() }
})


def docker_clean()
  common = Common.new

  # --volumes clears out any cached data between runs, e.g. the MySQL database
  common.run_inline %W{docker compose down --volumes}

  # This keyfile gets created and cached locally on dev-up. Though it's not
  # specific to Docker, it is mounted locally for docker runs. For lack of a
  # better "dev teardown" hook, purge that file here; e.g. in case we decide to
  # invalidate a dev key or change the service account.
  common.run_inline %W{rm -f #{ServiceAccountContext::SERVICE_ACCOUNT_KEY_PATH}}

  # See https://github.com/docker/compose/issues/3447
  common.status "Cleaning complete. docker compose 'not found' errors can be safely ignored"
end

Common.register_command({
  :invocation => "docker-clean",
  :description => \
    "Removes docker containers and volumes, allowing the next `dev-up` to" \
    " start from scratch (e.g., the database will be re-created).",
  :fn => ->() { docker_clean() }
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
  op = WbOptionsParser.new(cmd_name, args)
  gcc = GcloudContextV2.new(op)
  op.parse.validate
  gcc.validate
  CloudSqlProxyContext.new(gcc.project).run do
    puts "Dropping database..."
    pw = ENV["MYSQL_ROOT_PASSWORD"]
    run_with_redirects(
        "cat db/drop_db.sql | envsubst | " +
        maybe_dockerize_mysql_cmd(
          "mysql -u \"root\" -p\"#{pw}\" --host 127.0.0.1 --port 3307",
          true # interactive
        ),
        pw)
  end
end

Common.register_command({
  :invocation => "drop-cloud-db",
  :description => "Drops the Cloud SQL database for the specified project",
  :fn => ->(*args) { drop_cloud_db("drop-cloud-db", *args) }
})

def drop_cloud_cdr(cmd_name, *args)
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
  setup_local_environment()
  start_local_db_service()

  common = Common.new
  Dir.chdir('db') do
    common.run_inline %W{./run-migrations.sh main}
  end

  init_new_cdr_db %W{--cdr-db-name cdr}
  init_new_cdr_db %W{--cdr-db-name cdr --run-list data --context local}
end

Common.register_command({
  :invocation => "run-local-all-migrations",
  :description => "Runs local data/schema migrations for the cdr and workbench schemas.",
  :fn => ->() { run_local_all_migrations() }
})

def liquibase_gradlew_command(command, argument = '', run_list = '')
  full_cmd_array = %W{../gradlew #{command}}

  # Currently there's only one activity (main), and leaving out the runList argument causes
  # it to run that activity. Leaving out the runList is equivalent to specifying all of the
  # activities to run in unspecified order, but we set it in gradle.properties and pull it in as
  # an external property anyway.

  unless run_list.to_s.empty?
    full_cmd_array << "-PrunList=#{run_list}"
  end

  unless argument.to_s.empty?
    full_cmd_array << "-PliquibaseCommandValue=#{argument}"
  end

  full_cmd_array
end

# Run a Liquibase command against the specified project. Where possible, show SQL
# statements and ask user for verification.
def run_liquibase(cmd_name, *args)
  command_to_sql = {
      'changelogSync' => 'changelogSyncSQL',
      'markNextChangesetRan' => 'markNextChangesetRanSQL',
      'rollback' => 'rollbackSQL',
      'rollbackCount' => 'rollbackCountSQL',
      'rollbackToDate' => 'rollbackToDateSQL',
      'update' => 'updateSQL',
      'updateCount' => 'updateCountSql', # Stet. Official command name doesn't match Liquibase plugin task
      'updateToTag' => 'updateToTagSQL'
  }

  common = Common.new

  op = WbOptionsParser.new(cmd_name, args)
  op.add_typed_option(
      "--command [command]",
      String,
      ->(opts, c) { opts.command = c},
      "Liquibase command, e.g. update, rollback, tag, validate. See "+
          "https://www.liquibase.org/documentation/command_line.html")
  op.add_typed_option(
      "--argument [argument]",
      String,
      ->(opts, a) { opts.argument = a},
      "Liquibase command argument, e.g. count or tag value")
  op.add_typed_option(
        "--run-list [run_list]",
        String,
        ->(opts, rl) { opts.run_list = rl },
        "Liquibase runList, a comma-separated list of activities in the liquibase task")
  op.add_typed_option(
        '--project [project]',
        String,
        ->(opts, p) { opts.project = p },
        'AoU environment GCP project full name. Used to pick MySQL instance & credentials.'
  )
  op.add_validator ->(opts) {
    if opts.command.to_s.empty?
      raise ArgumentError.new("command is required")
    end
  }
  op.parse.validate

  if op.opts.project.to_s.empty?
    op.opts.project = 'local'
  end

  context = GcloudContextV2.new(op)
  context.validate

  with_optional_cloud_proxy_and_db(context) do |gcc|
    common.status('inside with_optional_cloud_proxy_and_db')
    common.status("project: #{gcc.project}, account: #{gcc.account}, creds_file: #{gcc.creds_file}, dir: #{Dir.pwd}")
    command = op.opts.command

    Dir.chdir('db') # 'cd' can't be run inline in this context

    verification_command = command_to_sql[command]

    unless verification_command.to_s.empty?
      verification_full_cmd = liquibase_gradlew_command(verification_command, op.opts.argument, op.opts.run_list)
      common.run_inline(verification_full_cmd)
      get_user_confirmation("Execute SQL commands above?")
    end

    full_cmd = liquibase_gradlew_command(command, op.opts.argument, op.opts.run_list)
    common.run_inline(full_cmd)
  end
end

Common.register_command({
    :invocation => "run-liquibase",
    :description => "Run liquibase command with optional argument",
    :fn => ->(*args) { run_liquibase('run-liquibase', *args) }
})

def run_local_data_migrations()
  setup_local_environment()
  start_local_db_service()

  init_new_cdr_db %W{--cdr-db-name cdr --run-list data --context local}
end

Common.register_command({
  :invocation => "run-local-data-migrations",
  :description => "Runs local data migrations for the cdr schema.",
  :fn => ->() { run_local_data_migrations() }
})

def run_local_rw_migrations()
  setup_local_environment()
  start_local_db_service()

  common = Common.new
  Dir.chdir('db') do
    common.run_inline %W{./run-migrations.sh main}
  end
end

Common.register_command({
  :invocation => "run-local-rw-migrations",
  :description => "Runs local migrations for the workbench schema.",
  :fn => ->() { run_local_rw_migrations() }
})

def create_cdr_indices(cmd_name, *args)
  op = WbOptionsParser.new(cmd_name, args)
  op.opts.data_browser = false
  op.opts.create_prep_tables = true
  op.opts.branch = "main"
  op.add_option(
    "--branch [branch]",
    ->(opts, v) { opts.branch = v},
    "Branch - Optional - Default is main."
  )
  op.add_option(
    "--project [project]",
    ->(opts, v) { opts.project = v},
    "Project name - Required"
  )
  op.add_option(
    "--bq-dataset [bq-dataset]",
    ->(opts, v) { opts.bq_dataset = v},
    "BQ dataset - Required."
  )
  op.add_option(
    "--cdr-version [cdr-version]",
    ->(opts, v) { opts.cdr_version = v},
    "CDR version - Required."
  )
  op.add_option(
    "--data-browser [data-browser]",
    ->(opts, v) { opts.data_browser = v},
    "Generate for data browser - Optional - Default is false"
  )
  op.add_option(
    "--create-prep-tables [create-prep-tables]",
    ->(opts, v) { opts.create_prep_tables = v},
    "Create all prep tables - Optional - Default is true"
  )

  op.add_validator ->(opts) { raise ArgumentError unless opts.project and opts.bq_dataset and opts.cdr_version}
  op.parse.validate

  env = ENVIRONMENTS[op.opts.project]
  cdr_source = env.fetch(:source_cdr_project)
  if op.opts.data_browser
    cdr_source = "aou-res-curation-prod"
  end
  common = Common.new
  content_type = "Content-Type: application/json"
  accept = "Accept: application/json"
  circle_token = "Circle-Token: "
  payload = "{ \"branch\": \"#{op.opts.branch}\", \"parameters\": { \"wb_create_cdr_indices\": true, \"cdr_source_project\": \"#{cdr_source}\", \"cdr_source_dataset\": \"#{op.opts.bq_dataset}\", \"project\": \"#{op.opts.project}\", \"cdr_version_db_name\": \"#{op.opts.cdr_version}\", \"data_browser\": #{op.opts.data_browser}, \"create_prep_tables\": #{op.opts.create_prep_tables} }}"
  common.run_inline "curl -X POST https://circleci.com/api/v2/project/github/all-of-us/cdr-indices/pipeline -H '#{content_type}' -H '#{accept}' -H \"#{circle_token}\ $(cat ~/.circle-creds/key.txt)\" -d '#{payload}'"
end

Common.register_command({
  :invocation => "create-cdr-indices",
  :description => "Create the CDR indices in circle.",
  :fn => ->(*args) { create_cdr_indices("create-cdr-indices", *args) }
})

def create_tanagra_prep_tables(cmd_name, *args)
  op = WbOptionsParser.new(cmd_name, args)
  op.opts.branch = "main"
  op.add_option(
    "--branch [branch]",
    ->(opts, v) { opts.branch = v},
    "Branch - Optional - Default is main."
  )
  op.add_option(
    "--project [project]",
    ->(opts, v) { opts.project = v},
    "Project name - Required"
  )
  op.add_option(
    "--bq-dataset [bq-dataset]",
    ->(opts, v) { opts.bq_dataset = v},
    "BQ dataset - Required."
  )

  op.add_validator ->(opts) { raise ArgumentError unless opts.project and opts.bq_dataset}
  op.parse.validate

  env = ENVIRONMENTS[op.opts.project]
  cdr_source = env.fetch(:source_cdr_project)
  common = Common.new
  content_type = "Content-Type: application/json"
  accept = "Accept: application/json"
  circle_token = "Circle-Token: "
  payload = "{ \"branch\": \"#{op.opts.branch}\", \"parameters\": { \"wb_create_tanagra_prep_tables\": true, \"cdr_source_project\": \"#{cdr_source}\", \"cdr_source_dataset\": \"#{op.opts.bq_dataset}\", \"project\": \"#{op.opts.project}\" }}"
  common.run_inline "curl -X POST https://circleci.com/api/v2/project/github/all-of-us/cdr-indices/pipeline -H '#{content_type}' -H '#{accept}' -H \"#{circle_token}\ $(cat ~/.circle-creds/key.txt)\" -d '#{payload}'"
end

Common.register_command({
  :invocation => "create-tanagra-prep-tables",
  :description => "Create prep tables for tanagra.",
  :fn => ->(*args) { create_tanagra_prep_tables("create-tanagra-prep-tables", *args) }
})

def build_prep_survey(cmd_name, *args)
  op = WbOptionsParser.new(cmd_name, args)
  op.add_option(
      "--bq-project [bq-project]",
      ->(opts, v) { opts.bq_project = v},
      "BQ Project - Required."
  )
  op.add_option(
      "--bq-dataset [bq-dataset]",
      ->(opts, v) { opts.bq_dataset = v},
      "BQ dataset - Required."
  )
  op.add_option(
      "--filename [filename]",
      ->(opts, v) { opts.filename = v},
      "Filename - Required."
  )

  op.add_validator ->(opts) { raise ArgumentError unless opts.bq_project and opts.bq_dataset and opts.filename}
  op.parse.validate

  common = Common.new
  Dir.chdir('db-cdr') do
    common.run_inline %W{./generate-cdr/build-prep-survey.sh #{op.opts.bq_project} #{op.opts.bq_dataset} #{op.opts.filename}}
  end
end

Common.register_command({
  :invocation => "build-prep-survey",
  :description => "Build the prep_survey table.",
  :fn => ->(*args) { build_prep_survey("build-prep-survey", *args) }
})

def create_cdr_indices_tables(cmd_name, *args)
  op = WbOptionsParser.new(cmd_name, args)
  op.opts.create_prep_tables = true
  op.add_option(
      "--bq-project [bq-project]",
      ->(opts, v) { opts.bq_project = v},
      "BQ Project - Required."
  )
  op.add_option(
      "--bq-dataset [bq-dataset]",
      ->(opts, v) { opts.bq_dataset = v},
      "BQ Dataset - Required."
  )
  op.add_option(
    "--create-prep-tables [create-prep-tables]",
    ->(opts, v) { opts.create_prep_tables = v},
    "Create all prep tables - Optional - Default is true"
  )

  op.add_validator ->(opts) { raise ArgumentError unless opts.bq_project and opts.bq_dataset}
  op.parse.validate

  common = Common.new
  Dir.chdir('db-cdr') do
    common.run_inline %W{./generate-cdr/create-cdr-indices-tables.sh #{op.opts.bq_project} #{op.opts.bq_dataset} #{op.opts.create_prep_tables}}
  end
end

Common.register_command({
  :invocation => "create-cdr-indices-tables",
  :description => "Create the CDR indices tables.",
  :fn => ->(*args) { create_cdr_indices_tables("create-cdr-indices-tables", *args) }
})

def create_tanagra_tables(cmd_name, *args)
  op = WbOptionsParser.new(cmd_name, args)
  op.add_option(
      "--bq-project [bq-project]",
      ->(opts, v) { opts.bq_project = v},
      "BQ Project - Required."
  )
  op.add_option(
      "--bq-dataset [bq-dataset]",
      ->(opts, v) { opts.bq_dataset = v},
      "BQ Dataset - Required."
  )

  op.add_validator ->(opts) { raise ArgumentError unless opts.bq_project and opts.bq_dataset}
  op.parse.validate

  common = Common.new
  Dir.chdir('db-cdr') do
    common.run_inline %W{./generate-cdr/create-tanagra-tables.sh #{op.opts.bq_project} #{op.opts.bq_dataset}}
  end
end

Common.register_command({
  :invocation => "create-tanagra-tables",
  :description => "Create the CDR indices tables.",
  :fn => ->(*args) { create_tanagra_tables("create-tanagra-tables", *args) }
})

def create_fitbit_tables_with_id_column(cmd_name, *args)
  op = WbOptionsParser.new(cmd_name, args)
  op.add_option(
      "--bq-project [bq-project]",
      ->(opts, v) { opts.bq_project = v},
      "BQ Project - Required."
  )
  op.add_option(
      "--bq-dataset [bq-dataset]",
      ->(opts, v) { opts.bq_dataset = v},
      "BQ Dataset - Required."
  )

  op.add_validator ->(opts) { raise ArgumentError unless opts.bq_project and opts.bq_dataset}
  op.parse.validate

  common = Common.new
  Dir.chdir('db-cdr') do
    common.run_inline %W{./generate-cdr/create-fitbit-tables-with-id-column.sh #{op.opts.bq_project} #{op.opts.bq_dataset}}
  end
end

Common.register_command({
  :invocation => "create-fitbit-tables-with-id-column",
  :description => "Create all fitbit tables with id column.",
  :fn => ->(*args) { create_fitbit_tables_with_id_column("create-fitbit-tables-with-id-column", *args) }
})

def build_tanagra_pfhh_table(cmd_name, *args)
  op = WbOptionsParser.new(cmd_name, args)
  op.add_option(
      "--bq-project [bq-project]",
      ->(opts, v) { opts.bq_project = v},
      "BQ Project - Required."
  )
  op.add_option(
      "--bq-dataset [bq-dataset]",
      ->(opts, v) { opts.bq_dataset = v},
      "BQ Dataset - Required."
  )

  op.add_validator ->(opts) { raise ArgumentError unless opts.bq_project and opts.bq_dataset}
  op.parse.validate

  common = Common.new
  Dir.chdir('db-cdr') do
    common.run_inline %W{./generate-cdr/build-tanagra-pfhh-table.sh #{op.opts.bq_project} #{op.opts.bq_dataset}}
  end
end

Common.register_command({
  :invocation => "build-tanagra-pfhh-table",
  :description => "Build the Tanagra PFHH table.",
  :fn => ->(*args) { build_tanagra_pfhh_table("build-tanagra-pfhh-table", *args) }
})

def build_cdr_indices_tables(cmd_name, *args)
  op = WbOptionsParser.new(cmd_name, args)
  op.add_option(
    "--bq-project [bq-project]",
    ->(opts, v) { opts.bq_project = v},
    "BQ Project - Required."
  )
  op.add_option(
    "--bq-dataset [bq-dataset]",
    ->(opts, v) { opts.bq_dataset = v},
    "BQ dataset - Required."
  )
  op.add_option(
    "--script [script]",
    ->(opts, v) { opts.script = v},
    "Script - Required."
  )

  op.add_validator ->(opts) { raise ArgumentError unless opts.bq_project and opts.bq_dataset and opts.script }
  op.parse.validate

  common = Common.new
  Dir.chdir('db-cdr') do
    common.run_inline %W{./generate-cdr/build-#{op.opts.script}.sh #{op.opts.bq_project} #{op.opts.bq_dataset}}
  end
end

Common.register_command({
  :invocation => "build-cdr-indices-tables",
  :description => "Build CDR indices tables for specified script",
  :fn => ->(*args) { build_cdr_indices_tables("build-cdr-indices-tables", *args) }
})

def build_search_all_events(cmd_name, *args)
  op = WbOptionsParser.new(cmd_name, args)
  op.opts.data_browser = false
  op.add_option(
    "--bq-project [bq-project]",
    ->(opts, v) { opts.bq_project = v},
    "BQ Project - Required."
  )
  op.add_option(
    "--bq-dataset [bq-dataset]",
    ->(opts, v) { opts.bq_dataset = v},
    "BQ dataset - Required."
  )
  op.add_option(
    "--data-browser [data-browser]",
    ->(opts, v) { opts.data_browser = v},
    "Generate for data browser - Optional - Default is false"
  )
  op.add_option(
    "--domain-token [domain-token]",
    ->(opts, v) { opts.domain_token = v},
    "Generate for domain-token - Required."
  )
  op.add_validator ->(opts) { raise ArgumentError unless opts.bq_project and opts.bq_dataset and opts.domain_token }
  op.parse.validate

  common = Common.new
  Dir.chdir('db-cdr') do
    common.run_inline %W{./generate-cdr/build-search-all-events.sh #{op.opts.bq_project} #{op.opts.bq_dataset} #{op.opts.data_browser} #{op.opts.domain_token}}
  end
end

Common.register_command({
  :invocation => "build-search-all-events",
  :description => "Generates big query denormalized search. Used by cohort builder. Must be run once when a new cdr is released",
  :fn => ->(*args) { build_search_all_events("build-search-all-events", *args) }
})

def build_cdr_indices_tables_by_domain(cmd_name, *args)
  op = WbOptionsParser.new(cmd_name, args)
  op.add_option(
    "--bq-project [bq-project]",
    ->(opts, v) { opts.bq_project = v},
    "BQ Project - Required."
  )
  op.add_option(
    "--bq-dataset [bq-dataset]",
    ->(opts, v) { opts.bq_dataset = v},
    "BQ dataset - Required."
  )
  op.add_option(
    "--script [script]",
    ->(opts, v) { opts.script = v},
    "Script - Required."
  )
  op.add_option(
    "--domain [domain]",
    ->(opts, v) { opts.domain = v},
    "Generate specified table by domain - Required"
  )

  op.add_validator ->(opts) { raise ArgumentError unless opts.bq_project and opts.bq_dataset and opts.script and opts.domain }
  op.parse.validate

  common = Common.new
  Dir.chdir('db-cdr') do
    common.run_inline %W{./generate-cdr/#{op.opts.script}.sh #{op.opts.bq_project} #{op.opts.bq_dataset} #{op.opts.domain}}
  end
end

Common.register_command({
  :invocation => "build-cdr-indices-tables-by-domain",
  :description => "Builds tables for review and dataset builder by domain",
  :fn => ->(*args) { build_cdr_indices_tables_by_domain("build-cdr-indices-tables-by-domain", *args) }
})

def build_cdr_indices_output_tables(cmd_name, *args)
  op = WbOptionsParser.new(cmd_name, args)
  op.add_option(
      "--bq-project [bq-project]",
      ->(opts, v) { opts.bq_project = v},
      "BQ Project - Required."
  )
  op.add_option(
      "--bq-dataset [bq-dataset]",
      ->(opts, v) { opts.bq_dataset = v},
      "BQ dataset - Required."
  )
  op.add_option(
      "--output-project [output-project]",
      ->(opts, v) { opts.output_project = v},
      "Output Project - Required."
  )
  op.add_option(
      "--output-dataset [output-dataset]",
      ->(opts, v) { opts.output_dataset = v},
      "Output dataset - Required."
  )
  op.add_option(
    "--script [script]",
    ->(opts, v) { opts.script = v},
    "Script - Required."
  )

  op.add_validator ->(opts) { raise ArgumentError unless opts.bq_project and opts.bq_dataset and opts.output_project and opts.output_dataset and opts.script }
  op.parse.validate

  common = Common.new
  Dir.chdir('db-cdr') do
    common.run_inline %W{./generate-cdr/#{op.opts.script}.sh #{op.opts.bq_project} #{op.opts.bq_dataset} #{op.opts.output_project} #{op.opts.output_dataset}}
  end
end

Common.register_command({
  :invocation => "build-cdr-indices-output-tables",
  :description => "Build CDR indices tables for output dataset",
  :fn => ->(*args) { build_cdr_indices_output_tables("build-cdr-indices-output-tables", *args) }
})

def stage_redcap_files(cmd_name, *args)
  op = WbOptionsParser.new(cmd_name, args)
  op.add_option(
      "--date [date]",
      ->(opts, v) { opts.date = v},
      "Redcap file date - Required."
  )
  op.add_option(
      "--dataset [dataset]",
      ->(opts, v) { opts.dataset = v},
      "Dataset name - Required."
  )
  op.add_validator ->(opts) { raise ArgumentError unless opts.date and opts.dataset }
  op.parse.validate

  common = Common.new
  Dir.chdir('db-cdr/generate-cdr') do
    common.run_inline %W{python stage-redcap-files.py --date #{op.opts.date} --dataset #{op.opts.dataset}}
  end
end

Common.register_command({
  :invocation => "stage-redcap-files",
  :description => "Cleanup redcap files for CDR indices ingestion.",
  :fn => ->(*args) { stage_redcap_files("stage-redcap-files", *args) }
})

def build_cb_criteria_demographics(cmd_name, *args)
  op = WbOptionsParser.new(cmd_name, args)
  op.opts.data_browser = false
  op.add_option(
    "--bq-project [bq-project]",
    ->(opts, v) { opts.bq_project = v},
    "BQ Project - Required."
  )
  op.add_option(
    "--bq-dataset [bq-dataset]",
    ->(opts, v) { opts.bq_dataset = v},
    "BQ dataset - Required."
  )
  op.add_option(
    "--data-browser [data-browser]",
    ->(opts, v) { opts.data_browser = v},
    "Generate for data browser - Optional - Default is false"
  )
  op.add_validator ->(opts) { raise ArgumentError unless opts.bq_project and opts.bq_dataset }
  op.parse.validate

  common = Common.new
  Dir.chdir('db-cdr') do
    common.run_inline %W{./generate-cdr/build-cb-criteria-demographics.sh #{op.opts.bq_project} #{op.opts.bq_dataset} #{op.opts.data_browser}}
  end
end

Common.register_command({
  :invocation => "build-cb-criteria-demographics",
  :description => "Builds cb_criteria",
  :fn => ->(*args) { build_cb_criteria_demographics("build-cb-criteria-demographics", *args) }
})

def create_local_csv_files(cmd_name, *args)
  op = WbOptionsParser.new(cmd_name, args)
  op.add_option(
    "--project [project]",
    ->(opts, v) { opts.project = v},
    "Project name"
  )
 op.add_option(
    "--dataset [dataset]",
    ->(opts, v) { opts.dataset = v},
    "Dataset name"
   )
  op.add_validator ->(opts) { raise ArgumentError unless opts.project and opts.dataset }
  op.parse.validate

  common = Common.new
  Dir.chdir('db-cdr') do
    common.run_inline %W{./generate-cdr/create-local-csv-files.sh #{ENVIRONMENTS[op.opts.project][:source_cdr_project]} #{op.opts.dataset}}
  end
end

Common.register_command({
  :invocation => "create-local-csv-files",
  :description => "Loads local data from dataset and project",
  :fn => ->(*args) { create_local_csv_files("create-local-csv-files", *args) }
})

def validate_cdr(cmd_name, *args)
  op = WbOptionsParser.new(cmd_name, args)
  op.add_option(
    "--project [project]",
    ->(opts, v) { opts.project = v},
    "Project name"
  )
 op.add_option(
    "--dataset [dataset]",
    ->(opts, v) { opts.dataset = v},
    "Dataset name"
   )
  op.add_validator ->(opts) { raise ArgumentError unless opts.project and opts.dataset }
  op.parse.validate

  common = Common.new
  Dir.chdir('db-cdr') do
    common.run_inline %W{./generate-cdr/validate-cdr.sh #{ENVIRONMENTS[op.opts.project][:source_cdr_project]} #{op.opts.dataset}}
  end
end

Common.register_command({
  :invocation => "validate-cdr",
  :description => "Validates synthetic data and CDR",
  :fn => ->(*args) { validate_cdr("validate-cdr", *args) }
})

def import_cdr_indices_build_to_cloudsql(cmd_name, *args)
  op = WbOptionsParser.new(cmd_name, args)
  op.add_option(
    "--bq-project [bq-project]",
    ->(opts, v) { opts.bq_project = v},
    "BQ Project - Required."
  )
  op.add_option(
    "--bq-dataset [bq-dataset]",
    ->(opts, v) { opts.bq_dataset = v},
    "BQ dataset - Required."
  )
  op.add_option(
    "--project [project]",
    ->(opts, v) { opts.project = v},
    "Project - Required."
  )
  op.add_option(
    "--cdr-version [cdr-version]",
    ->(opts, v) { opts.cdr_version = v},
    "CDR version - Required."
  )
  op.add_validator ->(opts) { raise ArgumentError unless opts.bq_project and opts.bq_dataset and opts.project and opts.cdr_version }
  op.parse.validate
  gcc = GcloudContextV2.new(op)
  op.parse.validate
  gcc.validate()

  ENV.update(read_db_vars(gcc))
  ENV.update(must_get_env_value(gcc.project, :gae_vars))
  ENV["DB_HOST"] = "127.0.0.1" # Temporary fix until we decide on how to handle this correctly.
  ENV["DB_PORT"] = "3307" # Temporary fix until we decide on how to handle this correctly.

  common = Common.new
  CloudSqlProxyContext.new(gcc.project).run do
    Dir.chdir('db-cdr') do
      common.run_inline %W{./generate-cdr/import-cdr-indices-build-to-cloudsql.sh #{op.opts.bq_project} #{op.opts.bq_dataset} #{op.opts.project} #{op.opts.cdr_version}}
    end
  end
end

Common.register_command({
  :invocation => "import-cdr-indices-build-to-cloudsql",
  :description => "Imports CB related tables to mysql/cloudsql to be used by workbench.",
  :fn => ->(*args) { import_cdr_indices_build_to_cloudsql("import-cdr-indices-build-to-cloudsql", *args) }
})

def set_grants(cmd_name, *args)
  op = WbOptionsParser.new(cmd_name, args)
  op.add_option(
    "--project [project]",
    ->(opts, v) { opts.project = v},
    "Project - Required."
  )
  op.add_option(
    "--cdr-version [cdr-version]",
    ->(opts, v) { opts.cdr_version = v},
    "CDR version - Required."
  )
  op.add_validator ->(opts) { raise ArgumentError unless opts.project and opts.cdr_version }
  op.parse.validate
  gcc = GcloudContextV2.new(op)
  op.parse.validate
  gcc.validate()

  ENV.update(read_db_vars(gcc))
  ENV.update(must_get_env_value(gcc.project, :gae_vars))
  ENV["DB_HOST"] = "127.0.0.1" # Temporary fix until we decide on how to handle this correctly.
  ENV["DB_PORT"] = "3307" # Temporary fix until we decide on how to handle this correctly.

  common = Common.new
  CloudSqlProxyContext.new(gcc.project).run do
    Dir.chdir('db-cdr') do
      common.run_inline %W{./generate-cdr/set-grants.sh #{op.opts.cdr_version}}
    end
  end
end

Common.register_command({
  :invocation => "set-grants",
  :description => "Set grants on new cdr database.",
  :fn => ->(*args) { set_grants("set-grants", *args) }
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
  op.add_validator ->(opts) { raise ArgumentError unless opts.sa_project and opts.source_dataset and opts.destination_dataset }
  op.parse.validate

  source_project = "#{op.opts.source_dataset}".split(':').first
  ServiceAccountContext.new(op.opts.sa_project).run do
    common = Common.new
    common.status "Copying from '#{op.opts.source_dataset}' -> '#{op.opts.dest_dataset}'"
    Dir.chdir('db-cdr') do
      common.run_inline %W{./generate-cdr/copy-bq-dataset.sh #{op.opts.source_dataset} #{op.opts.destination_dataset} #{source_project}}
    end
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
    Dir.chdir('db-cdr') do
      common = Common.new
      common.run_inline %W{./generate-cdr/cloudsql-import.sh
        --project #{op.opts.project}
        --instance #{op.opts.instance}
        --database #{op.opts.database}
        --bucket #{op.opts.bucket}
        --file #{op.opts.file}}
    end
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
  Dir.chdir('db-cdr') do
    common.run_inline %W{./generate-cdr/generate-local-cdr-db.sh} + args
  end
end

Common.register_command({
  :invocation => "generate-local-cdr-db",
  :description => "generate-cloudsql-cdr --cdr-version <synth_r_20XXqX_X> --cdr-db-prefix <cdr> --bucket <BUCKET>
Creates and populates local mysql database from data in bucket made by import-cdr-indices-to-cloudsql.",
  :fn => ->(*args) { generate_local_cdr_db(*args) }
})


def generate_local_count_dbs(*args)
  common = Common.new
  Dir.chdir('db-cdr') do
    common.run_inline %W{./generate-cdr/generate-local-count-dbs.sh} + args
  end
end

Common.register_command({
  :invocation => "generate-local-count-dbs",
  :description => "generate-local-count-dbs --cdr-version <synth_r_20XXqX_X> --bucket <BUCKET>
Creates and populates local mysql databases cdr<VERSION> from data in bucket made by import-cdr-indices-to-cloudsql.",
  :fn => ->(*args) { generate_local_count_dbs(*args) }
})


def mysqldump_db(*args)
  common = Common.new
  Dir.chdir('db-cdr') do
    common.run_inline %W{./generate-cdr/make-mysqldump.sh} + args
  end
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
  Dir.chdir('db-cdr') do
    common.run_inline %W{./generate-cdr/local-mysql-import.sh
        --sql-dump-file #{op.opts.file} --bucket #{op.opts.bucket}}
  end
end
Common.register_command({
                            :invocation => "local-mysql-import",
                            :description => "local-mysql-import --sql-dump-file <FILE.sql> --bucket <BUCKET>
Imports .sql file to local mysql instance",
                            :fn => ->(*args) { local_mysql_import("local-mysql-import", *args) }
                        })


def run_drop_cdr_db()
  common = Common.new
  Dir.chdir('db-cdr') do
    common.run_inline %W{./run-drop-db.sh}
  end
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

def create_auth_domain(cmd_name, args)
  op = WbOptionsParser.new(cmd_name, args)
  op.add_option(
    "--project [project]",
    ->(opts, v) { opts.project = v},
    "Workbench Project (environment) for creating the authorization domain"
  )
  op.add_option(
    "--tier [tier]",
    ->(opts, v) { opts.tier = v},
    "Access tier for creating the authorization domain"
  )
  op.add_option(
    "--user [user]",
    ->(opts, v) { opts.user = v},
    "A Workbench user you control with DEVELOPER Authority in the environment"
  )
  op.add_validator ->(opts) { raise ArgumentError unless opts.project and opts.user and opts.tier }
  op.parse.validate

  common = Common.new
  common.run_inline %W{gcloud auth login #{op.opts.user}}
  token = common.capture_stdout %W{gcloud auth print-access-token}
  token = token.chomp
  header = "Authorization: Bearer #{token}"
  content_type = "Content-type: application/json"
  api_base_url = get_server_config(op.opts.project)["apiBaseUrl"]

  domain_name = get_auth_domain_name(op.opts.project, op.opts.tier)
  common.run_inline %W{curl -X POST -H #{header} -H #{content_type} -d {}
     #{api_base_url}/v1/auth-domain/#{domain_name}}
end

Common.register_command({
  :invocation => "create-auth-domain",
  :description => "Creates an authorization domain in Terra for users of the supplied tier",
    :fn => ->(*args) { create_auth_domain("create-auth-domain", args) }
})

def fix_desynchronized_billing_project_owners(cmd_name, *args)
  common = Common.new

  op = WbOptionsParser.new(cmd_name, args)
  op.opts.dry_run = true
  op.opts.project = TEST_PROJECT

  op.add_typed_option(
      "--dry_run=[dry_run]",
      TrueClass,
      ->(opts, v) { opts.dry_run = v},
      "When true, print debug lines instead of performing writes. Defaults to true.")
  op.add_option(
      "--billing-project-ids [project_id1,...]",
      ->(opts, v) { opts.billing_project_ids = v},
      "Optional billing projects IDs to update. By default all projects are considered")

  op.opts.billing_project_ids = ''
  gcc = GcloudContextV2.new(op)
  op.parse.validate
  gcc.validate()

  if op.opts.dry_run
    common.status "DRY RUN -- CHANGES WILL NOT BE PERSISTED"
  end

  fc_config = get_fc_config(op.opts.project)
  domain = get_config(op.opts.project)["googleDirectoryService"]["gSuiteDomain"]
  flags = ([
      ["--rawls-base-url", fc_config["rawlsBaseUrl"]],
      ["--researcher-domain", domain]
    ] + op.opts.billing_project_ids.split(',').map{ |bp| ["--billing-project-ids", bp] }
    ).map { |kv| "#{kv[0]}=#{kv[1]}" }
  if op.opts.dry_run
    flags += ["--dry-run"]
  end
  # Gradle args need to be single-quote wrapped.
  flags.map! { |f| "'#{f}'" }
  ServiceAccountContext.new(gcc.project).run do
    common.run_inline %W{
        ./gradlew fixDesynchronizedBillingProjectOwners
       -PappArgs=[#{flags.join(',')}]}
  end
end

Common.register_command({
    :invocation => "fix-desynchronized-billing-project-owners",
    :description => "Fixes desynchronized billing project owners",
    :fn => ->(*args) {fix_desynchronized_billing_project_owners("fix-desynchronized-billing-project-owners", *args)}
})

def update_user_disabled_status(cmd_name, args)
  common = Common.new
  op = WbOptionsParser.new(cmd_name, args)
  op.opts.project = TEST_PROJECT
  op.add_option(
    "--project [project]",
    ->(opts, v) { opts.project = v},
    "Project to update disabled status for"
  )
  op.add_option(
    "--disabled [disabled]",
    ->(opts, v) { opts.disabled = v},
    "Disabled state to set: true/false"
  )
  op.add_option(
    "--admin_account [admin_account_email]",
    ->(opts, v) { opts.account = v},
    "Workbench account with ACCESS_CONTROL_ADMIN Authority to perform the update disabled status action"
  )
  op.add_option(
    "--user [target_email]",
    ->(opts, v) { opts.user = v},
    "User to update the disabled status for"
  )
  op.add_validator ->(opts) {
    raise ArgumentError unless (opts.disabled != nil and opts.account and opts.user)
  }
  op.parse.validate

  common.run_inline %W{gcloud auth login #{op.opts.account}}
  token = common.capture_stdout %W{gcloud auth print-access-token}
  token = token.chomp
  header = "Authorization: Bearer #{token}"
  content_type = "Content-type: application/json"
  payload = "{\"username\": \"#{op.opts.user}\", \"accountDisabledStatus\": {\"disabled\": \"#{op.opts.disabled}\"}}"
  endpoint = "/v1/admin/users/updateAccount"
  common.run_inline %W{curl -X POST -H #{header} -H #{content_type}
      -d #{payload} https://#{ENVIRONMENTS[op.opts.project][:api_endpoint_host]}#{endpoint}}
end

UPDATE_USER_DISABLED_CMD = "update-user-disabled-status"

Common.register_command({
  :invocation => UPDATE_USER_DISABLED_CMD,
  :description => "Set a Workbench user's disabled status by email, using another Workbench admin account.\n" \
                  "Disabling a user immediately revokes CDR access and restricted API access in the \n" \
                  "Workbench, if they had access to begin with. When a disabled user loads the Workbench UI, \n" \
                  "they are redirected to a page which explains that they are disabled. This is currently the \n" \
                  "only automated means by which the user is notified of their disabled status.\n" \
                  "This tool can be used as a manual backup to the Workbench user admin UI, which supports the same disable function.\n" \
                  "Requires four flags: --project [env project] --disabled [true/false], --admin_account [admin email], and --user [target user email]",
  :fn => ->(*args) { update_user_disabled_status(UPDATE_USER_DISABLED_CMD, args) }
})

def fetch_firecloud_user_profile(cmd_name, *args)
  common = Common.new

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

  ENV.update(read_db_vars(gcc))
  ServiceAccountContext.new(gcc.project).run do
    common.run_inline %W{
        ./gradlew fetchFireCloudUserProfile
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

  op = WbOptionsParser.new(cmd_name, args)
  op.opts.project = TEST_PROJECT

  op.add_typed_option(
      "--workspace-namespace [workspace-namespace]",
      String,
      ->(opts, v) { opts.workspace_namespace = v},
      "Fetches details for workspace(s) that match the given namespace (e.g. 'aou-rw-231823128'")

  # Create a cloud context and apply the DB connection variables to the environment.
  # These will be read by Gradle and passed as Spring Boot properties to the command-line.
  gcc = GcloudContextV2.new(op)
  op.parse.validate
  gcc.validate()

  fc_config = get_fc_config(op.opts.project)

  flags = ([
      ["--rawls-base-url", fc_config["rawlsBaseUrl"]],
      ["--workspace-namespace", op.opts.workspace_namespace]
  ]).map { |kv| "#{kv[0]}=#{kv[1]}" }
  flags.map! { |f| "'#{f}'" }

  ENV.update(read_db_vars(gcc))
  ServiceAccountContext.new(gcc.project).run do
    common.run_inline %W{
        ./gradlew fetchWorkspaceDetails
       -PappArgs=[#{flags.join(',')}]}
  end
end

Common.register_command({
    :invocation => "fetch-workspace-details",
    :description => "Fetch workspace details.\n",
    :fn => ->(*args) {fetch_workspace_details("fetch-workspace-details", *args)}
})

def can_skip_token_generation(token_filenames)
  staleness_limit_minutes = 15

  for f in token_filenames do
    return false unless File.file?(f)

    contents = File.read(f)
    return false if contents.nil? || contents.empty?

    parsed = JSON.parse(contents)
    return false unless parsed.is_a?(Hash) and parsed.has_key?('token')

    created_at = Time.at(parsed.fetch('created_at_epoch_seconds', 0))
    age_minutes = (Time.now - created_at) / 60
    return false unless age_minutes < staleness_limit_minutes
  end

  return true
end

def generate_impersonated_user_tokens(cmd_name, *args)
  common = Common.new

  op = WbOptionsParser.new(cmd_name, args)
  op.add_typed_option(
      "--output-token-dir [output-token-dir]",
      String,
      ->(opts, v) { opts.output_token_dir = v},
      "Directory within which to generate the impersonated token(s).")
  op.add_typed_option(
      "--impersonated-usernames [impersonated-username1, ...]",
      String,
      ->(opts, v) { opts.impersonated_usernames = v},
      "Comma-separated AoU researcher username(s) to impersonate, e.g. calbach@fake-research-aou.org\n")
  op.add_validator ->(opts) { raise ArgumentError unless (opts.output_token_dir and opts.impersonated_usernames)}
  op.parse.validate

  unless File.exist?(op.opts.output_token_dir)
    raise ArgumentError.new("The token output directory #{op.opts.output_token_dir} does not exist")
  end

  usernames = op.opts.impersonated_usernames.split(',').uniq
  token_filenames = usernames.map{ |u| "#{op.opts.output_token_dir}/#{u}.json" }
  if can_skip_token_generation(token_filenames)
    common.status("Recent access tokens already exist, skipping generation")
    return
  end

  user_email_domain = nil
  usernames.each do |username|
   split_email = username.split('@')
   if split_email.nil? || split_email[1].nil?
     raise ArgumentError.new("Username is not a valid email address: " + username)
   end

   if user_email_domain.nil?
     user_email_domain = split_email[1]
   else
     if user_email_domain != split_email[1]
       raise ArgumentError.new("All usernames must have the same email domain. #{split_email[1]} does not match #{user_email_domain}")
     end
   end
  end

  # derive the project_id from the usernames, failing if this is not possible
  project_id = nil
  ENVIRONMENTS.each_key do |project|
    if project == "local"
      next
    end

    config = get_config(project)
    if user_email_domain == config["googleDirectoryService"]["gSuiteDomain"]
      project_id = project
      break
    end
  end

  if project_id.nil?
    raise ArgumentError.new("Invalid domain #{user_email_domain} for given usernames - target must be an AoU research domain email")
  end

  if ["all-of-us-rw-prod", "all-of-us-rw-preprod"].include? project_id
    get_user_confirmation(
      "Using impersonation in a production environment is highly discouraged, " +
      "and should only be considered in a break-glass scenario. Check with the " +
      "Workbench team that all other options have been exhausted before " +
      "continuing. Continue?")
  end

  flags = ([
      ["--project-id", project_id]
    ] +
    token_filenames.map{ |filename| ["--output-token-filename", filename] } +
    usernames.map{ |username| ["--impersonated-username", username] }
  ).map { |kv| "#{kv[0]}=#{kv[1]}" }
  flags.map! { |f| "'#{f}'" }

  ServiceAccountContext.new(project_id).run do
    common.run_inline %W{
        ./gradlew generateImpersonatedUserTokens
       -PappArgs=[#{flags.join(',')}]}
  end
end

Common.register_command({
    :invocation => "generate-impersonated-user-tokens",
    :description => "Generate impersonated oauth token(s) for target researcher(s)",
    :fn => ->(*args) {generate_impersonated_user_tokens("generate-impersonated-user-tokens", *args)}
})

def load_institutions(cmd_name, *args)
  common = Common.new

  op = WbOptionsParser.new(cmd_name, args)
  op.opts.dry_run = true
  op.opts.project = TEST_PROJECT

  op.add_typed_option(
      "--dry_run=[dry_run]",
      TrueClass,
      ->(opts, v) { opts.dry_run = v},
      "When true, print debug lines instead of performing writes. Defaults to true.")

  op.add_typed_option(
      "--import-filename [import-filename]",
      String,
      ->(opts, v) { opts.importFilename = v},
      "File (JSON) containing list of institutions to save")

  # Create a cloud context and apply the DB connection variables to the environment.
  # These will be read by Gradle and passed as Spring Boot properties to the command-line.
  gcc = GcloudContextV2.new(op)
  op.parse.validate
  gcc.validate()

  if op.opts.dry_run
    common.status "DRY RUN -- CHANGES WILL NOT BE PERSISTED"
  end

  gradle_args = ([
      ["--import-filename", op.opts.importFilename]
  ]).map { |kv| "#{kv[0]}=#{kv[1]}" }
  if op.opts.dry_run
    gradle_args += ["--dry-run"]
  end
  # Gradle args need to be single-quote wrapped.
  gradle_args.map! { |f| "'#{f}'" }

  ENV.update(read_db_vars(gcc))
  ServiceAccountContext.new(gcc.project).run do
    common.run_inline %W{
        ./gradlew loadInstitutions
       -PappArgs=[#{gradle_args.join(',')}]}
  end
end

LOAD_INSTITUTIONS_CMD = "load-institutions"

Common.register_command({
    :invocation => LOAD_INSTITUTIONS_CMD,
    :description => "Load institutions specified in given file.",
    :fn => ->(*args) {load_institutions(LOAD_INSTITUTIONS_CMD, *args)}
})

def invalidate_rdr_export(cmd_name, *args)
  common = Common.new

  op = WbOptionsParser.new(cmd_name, args)
  op.opts.dry_run = true
  op.opts.project = TEST_PROJECT

  op.add_typed_option(
      "--dry_run=[dry_run]",
      TrueClass,
      ->(opts, v) { opts.dry_run = v},
      "When true, print debug lines instead of performing writes. Defaults to true.")
  op.add_typed_option(
      "--entity-type [USER|WORKSPACE]",
      String,
      ->(opts, v) { opts.entity_type = v},
      "The RDR entity type to export. USER or WORKSPACE.")
  op.add_typed_option(
      "--id-list-filename [id-list-filename]",
      String,
      ->(opts, v) { opts.id_list_filename = v},
      "File containing list of entities by ID to export to RDR. " +
      "Each line should contain a single entity id (workspace or user database ID). If unspecified, " +
      "ALL entities of this type will be invalidated")

  op.add_validator ->(opts) { raise ArgumentError unless opts.entity_type}

  # Create a cloud context and apply the DB connection variables to the environment.
  # These will be read by Gradle and passed as Spring Boot properties to the command-line.
  gcc = GcloudContextV2.new(op)
  op.parse.validate
  gcc.validate()

  if op.opts.dry_run
    common.status "DRY RUN -- CHANGES WILL NOT BE PERSISTED"
  end

  get_user_confirmation(
    "RDR export invalidation should rarely be used; for backfilling data you " +
    "should use backfill-entities-to-rdr instead. If you still think you need to run this, " +
    "please consult with the team before continuing. Continue anyways?")

  flags = ([
    ['--entity-type', op.opts.entity_type]
  ]).map { |kv| "#{kv[0]}=#{kv[1]}" }
  if op.opts.id_list_filename
    flags += ["--id-list-filename", op.opts.id_list_filename]
  end
  if op.opts.dry_run
    flags += ["--dry-run"]
  end
  # Gradle args need to be single-quote wrapped.
  flags.map! { |f| "'#{f}'" }

  ENV.update(read_db_vars(gcc))
  ServiceAccountContext.new(gcc.project).run do
    common.run_inline %W{
        ./gradlew invalidateRdrExport
       -PappArgs=[#{flags.join(',')}]}
  end
end

INVALIDATE_RDR_EXPORT = "invalidate-rdr-export";

Common.register_command({
    :invocation => INVALIDATE_RDR_EXPORT,
    :description => "Invalidate exported RDR entities, causing them to be resent on the next RDR export.\n",
    :fn => ->(*args) {invalidate_rdr_export(INVALIDATE_RDR_EXPORT, *args)}
})

def backfill_entities_to_rdr(cmd_name, *args)
  common = Common.new

  op = WbOptionsParser.new(cmd_name, args)
  op.opts.dry_run = true

  op.add_typed_option(
    "--dry_run=[dry_run]",
    TrueClass,
    ->(opts, v) { opts.dry_run = v},
    "When true, print the number of workspaces that will be exported, will not export")
  op.add_typed_option(
    "--entity-type [USER|WORKSPACE]",
    String,
    ->(opts, v) { opts.entity_type = v},
    "The RDR entity type to export. USER or WORKSPACE.")
  op.add_typed_option(
    "--limit [LIMIT]",
    String,
    ->(opts, v) { opts.limit = v},
    "The number of workspaces exported will not to exceed this limit.")

  # Create a cloud context and apply the DB connection variables to the environment.
  # These will be read by Gradle and passed as Spring Boot properties to the command-line.

  context = GcloudContextV2.new(op)
  op.parse.validate
  context.validate()

  flags = ([
    ['--entity-type', op.opts.entity_type]
  ]).map { |kv| "#{kv[0]}=#{kv[1]}" }
  if op.opts.dry_run
    flags += ["--dry-run"]
  end
  if op.opts.limit
    flags += ["--limit", op.opts.limit]
  end
  # Gradle args need to be single-quote wrapped.
  flags.map! { |f| "'#{f}'" }

  gradleCommand = %W{
    ./gradlew backfillEntitiesToRdr
   -PappArgs=[#{flags.join(',')}]}

  with_optional_cloud_proxy_and_db(context) do
    common.run_inline gradleCommand
  end
end

BACKFILL_ENTITIES_TO_RDR = "backfill-entities-to-rdr";

Common.register_command({
    :invocation => BACKFILL_ENTITIES_TO_RDR,
    :description => "Backfill workspaces from workspace table, exporting them to the rdr.\n",
    :fn => ->(*args) {backfill_entities_to_rdr(BACKFILL_ENTITIES_TO_RDR, *args)}
})

def authority_options(cmd_name, args)
  op = WbOptionsParser.new(cmd_name, args)
  op.opts.authority = ""
  op.opts.remove = false
  op.opts.remove_all = false
  op.opts.dry_run = false
  op.add_option(
       "--email [EMAIL,...]",
       ->(opts, v) { opts.email = v},
       "Comma-separated list of user accounts to change. Required.")
  op.add_option(
      "--authority [AUTHORITY,...]",
      ->(opts, v) { opts.authority = v},
      "Comma-separated list of user authorities to add or remove for the users. " +
      "When granting authorities, use DEVELOPER to gain full access. " +
      "Exactly one of --authority or --remove-all must be passed.")
  op.add_option(
      "--remove",
      ->(opts, _) { opts.remove = "true"},
      "Remove authorities (rather than adding them) when using the --authority argument.")
  op.add_option(
    "--remove-all",
    ->(opts, _) { opts.remove_all = true},
    "Removes all authorities from user(s). Exactly one of --authority or --remove-all must be passed."
  )
  op.add_option(
      "--dry_run",
      ->(opts, _) { opts.dry_run = "true"},
      "Make no changes.")
  op.add_validator ->(opts) { raise ArgumentError unless opts.email and ((opts.authority != "") ^ opts.remove_all)}
  return op
end

def set_authority(cmd_name, *args)
  op = authority_options(cmd_name, args)
  gcc = GcloudContextV2.new(op)
  op.parse.validate
  gcc.validate

  ENV.update(read_db_vars(gcc))
  ServiceAccountContext.new(gcc.project).run do
    Common.new.run_inline %W{
      ./gradlew setAuthority
     -PappArgs=['#{op.opts.email}','#{op.opts.authority}',#{op.opts.remove},#{op.opts.dry_run},#{op.opts.remove_all}]}
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

  app_args = ["-PappArgs=['#{op.opts.email}','#{op.opts.authority}',#{op.opts.remove},#{op.opts.dry_run},#{op.opts.remove_all}]"]
  common = Common.new
  common.run_inline %W{./gradlew setAuthority} + app_args
end

Common.register_command({
  :invocation => "set-authority-local",
  :description => "Set user authorities on a local server (permissions); "\
                  "requires a local server is running (dev-up or run-api). "\
                  "See set-authority-local --help.",
  :fn => ->(*args) { set_authority_local("set-authority-local", *args) }
})

def create_wgs_cohort_extraction_bp_workspace(cmd_name, *args)
  common = Common.new
  op = WbOptionsParser.new(cmd_name, args)
  op.add_option(
      "--billing-project-name [billing-project-name]",
      ->(opts, v) { opts.billing_project_name = v},
      "Name of Billing project to create")
  op.add_option(
      "--workspace-name [workspace-name]",
      ->(opts, v) { opts.workspace_name = v},
      "Name of Terra workspace name to create")
  op.add_option(
    "--owners [EMAIL,...]",
    ->(opts, v) { opts.owners = v},
    "Comma-separated list of Terra user accounts to add to workspace ACL.")
  op.add_validator ->(opts) {
    unless (opts.billing_account or opts.billing_project_name or opts.workspace_name)
      common.error "all arguments must be provided"
      raise ArgumentError
    end
  }

  gcc = GcloudContextV2.new(op)
  op.parse.validate
  gcc.validate

  flags = ([
      ["--config-json", get_config_file(op.opts.project)],
      ["--billing-project-name", op.opts.billing_project_name],
      ["--workspace-name", op.opts.workspace_name],
      ["--owners", op.opts.owners],
  ]).map { |kv| "#{kv[0]}=#{kv[1]}" }
  flags.map! { |f| "'#{f}'" }

  ENV.update(read_db_vars(gcc))
  ServiceAccountContext.new(gcc.project).run do
    common.run_inline %W{
       ./gradlew createWgsCohortExtractionBillingProjectWorkspace
       -PappArgs=[#{flags.join(',')}]}
  end
end

Common.register_command({
  :invocation => "create-wgs-cohort-extraction-bp-workspace",
  :description => "Create Terra billing project and workspace impersonating the Genomics Cohort Extraction SA. This will NOT show up as an AoU workspace.",
  :fn => ->(*args) { create_wgs_cohort_extraction_bp_workspace("create-wgs-cohort-extraction-bp-workspace", *args) }
})

def get_github_commit_hash(repo, ref)
  # Check if we got a commit and return it if so
  response = Net::HTTP.get(URI("https://api.github.com/repos/#{repo}/commits/#{ref}"))
  begin
    return JSON.parse(response)['sha']
  rescue NoMethodError
  end

  # Otherwise, try to resolve it as a branch name
  response = Net::HTTP.get(URI("https://api.github.com/repos/#{repo}/branches/#{ref}"))
  begin
    return JSON.parse(response)['commit']['sha']
  rescue NoMethodError
    raise ArgumentError.new("Branch #{ref} not found in Github repository #{repo}")
  end
end

def create_terra_method_snapshot(cmd_name, *args)
  common = Common.new
  op = WbOptionsParser.new(cmd_name, args)
  op.add_option(
    "--project [GOOGLE_PROJECT]",
    ->(opts, v) { opts.project = v},
    "Google project to act on (e.g. all-of-us-workbench-test)."
# if we re-enable all-projects, update to:
#    "Google project to act on (e.g. all-of-us-workbench-test). Cannot be used with --all-projects"
  )
  op.opts.all_projects = false

# disabled until we figure out how to make the GcloudContextV2 constructor
# work with it (may not be worth it)
#
# this was a workaround which apparently doesn't work anymore
  # Use GcloudContextV2 to validate gcloud auth but we need to drop the
  # --project argument validation that's built into the constructor
  #   GcloudContextV2.validate_gcloud_auth()
  #   op.parse.validate
#
#   op.add_option(
#     "--all-projects [all-projects]",
#     ->(opts, _) { opts.all_projects = true},
#     "Create snapshot in every AoU environment. Cannot be used with --project.")

  op.add_option(
    "--source-git-repo [source-git-repo]",
    ->(opts, v) { opts.source_git_repo = v},
    "git owner/repo where the source file is located. ex. broadinstitute/gatk")

  op.add_option(
    "--source-git-path [source-git-path]",
    ->(opts, v) { opts.source_git_path = v},
    "git path where the source file is located, relative to the repo's root directory.
          ex. scripts/variantstore/wdl/GvsExtractCohortFromSampleNames.wdl")

  op.add_option(
    "--source-git-ref [source-git-ref]",
    ->(opts, v) { opts.source_git_ref = v},
    "git commit or branch where the source file is located. ex. ah_var_store")

  op.add_option(
    "--method-namespace [method-namespace]",
    ->(opts, v) { opts.method_namespace = v},
    "Agora method namespace to create snapshot in.")

  op.add_option(
    "--method-name [method-name]",
    ->(opts, v) { opts.method_name = v},
    "Agora method name to create snapshot in.")

  op.add_validator ->(opts) {
    unless (opts.project and opts.source_git_repo and opts.source_git_path and opts.source_git_ref and opts.method_namespace and opts.method_name)
      common.error "All arguments are required"
      raise ArgumentError
    end
  }

  gcc = GcloudContextV2.new(op)
  op.parse.validate
  gcc.validate()

  source_file_commit_hash = get_github_commit_hash(op.opts.source_git_repo, op.opts.source_git_ref)

  projects = op.opts.all_projects ? ENVIRONMENTS.keys - ["local"] : [op.opts.project]
  projects.each { |project|
    flags = ([
      ["--config-json", get_config_file(project)],
      ["--source-git-repo", op.opts.source_git_repo],
      ["--source-git-path", op.opts.source_git_path],
      ["--source-git-ref", source_file_commit_hash],
      ["--method-namespace", op.opts.method_namespace],
      ["--method-name", op.opts.method_name],
    ]).map { |kv| "#{kv[0]}=#{kv[1]}" }
    flags.map! { |f| "'#{f}'" }

    ENV.update(read_db_vars(gcc))
    ServiceAccountContext.new(gcc.project).run do
      common.run_inline %W{
       ./gradlew createTerraMethodSnapshot
       -PappArgs=[#{flags.join(',')}]}
    end
  }
end

Common.register_command({
  :invocation => "create-terra-method-snapshot",
  :description => "Create Terra Method snapshot in a single environment.",
  :fn => ->(*args) { create_terra_method_snapshot("create-terra-method-snapshot", *args) }
})

def delete_runtimes(cmd_name, *args)
  common = Common.new
  op = WbOptionsParser.new(cmd_name, args)
  op.opts.dry_run = true
  op.add_option(
      "--min-age-days [DAYS]",
      ->(opts, v) { opts.min_age_days = v},
      "Minimum age filter in days for runtimes to delete, e.g. 21")
  op.add_option(
      "--ids [RUNTIME_ID1,...]",
      ->(opts, v) { opts.runtime_ids = v},
      "Runtime IDs to delete, e.g. 'aou-test-f1-1/all-of-us'")
  op.add_option(
      "--nodry-run",
      ->(opts, _) { opts.dry_run = false},
      "Actually delete runtimes, defaults to dry run")
  op.add_validator ->(opts) {
    unless (opts.min_age_days or opts.runtime_ids)
      common.error "--ids or --min-age-days must be provided"
      raise ArgumentError
    end
  }

  gcc = GcloudContextV2.new(op)
  op.parse.validate
  gcc.validate

  if op.opts.dry_run
    common.status "DRY RUN -- CHANGES WILL NOT BE PERSISTED"
  end

  api_url = get_leo_api_url(gcc.project)
  ServiceAccountContext.new(gcc.project).run do
    common.run_inline %W{
       ./gradlew manageLeonardoRuntimes
      -PappArgs=['delete','#{api_url}','#{op.opts.min_age_days}','#{op.opts.runtime_ids}',#{op.opts.dry_run}]}
  end
end

Common.register_command({
  :invocation => "delete-runtimes",
  :description => "Delete runtimes matching the provided criteria within an environment",
  :fn => ->(*args) { delete_runtimes("delete-runtimes", *args) }
})

def describe_runtime(cmd_name, *args)
  op = WbOptionsParser.new(cmd_name, args)
  op.add_option(
      "--id [RUNTIME_ID]",
      ->(opts, v) { opts.runtime_id = v},
      "Required runtime ID to describe, e.g. 'aou-test-f1-1/all-of-us'")
  op.add_validator ->(opts) { raise ArgumentError unless opts.runtime_id }

  # Add the GcloudContext after setting up the project parameter to avoid
  # earlier validation failures.
  gcc = GcloudContextV2.new(op)
  op.parse.validate
  gcc.validate

  api_url = get_leo_api_url(gcc.project)
  ServiceAccountContext.new(gcc.project).run do |ctx|
    common = Common.new
    common.run_inline %W{
       ./gradlew manageLeonardoRuntimes
      -PappArgs=['describe','#{api_url}','#{ctx.service_account}','#{op.opts.runtime_id}']}
  end
end

Common.register_command({
  :invocation => "describe-runtime",
  :description => "Describe a given leonardo runtime",
  :fn => ->(*args) { describe_runtime("describe-runtime", *args) }
})


def list_runtimes(cmd_name, *args)
  op = WbOptionsParser.new(cmd_name, args)
  gcc = GcloudContextV2.new(op)
  op.add_option(
      "--google-project [project]",
      ->(opts, v) { opts.google_project = v},
      "Optionally filter by google project")
  op.add_option(
      "--format [format]",
      ->(opts, v) { opts.format = v },
      "JSON or TABULAR, defaults to TABULAR (summary)")
  op.opts.google_project = ""
  op.opts.format = "TABULAR"

  op.add_validator ->(opts) {
    unless ["JSON", "TABULAR"].include? opts.format
      raise ArgumentError.new("invalid format specified: #{opts.format}")
    end
  }

  op.parse.validate
  gcc.validate

  api_url = get_leo_api_url(gcc.project)
  ServiceAccountContext.new(gcc.project).run do
    common = Common.new
    common.run_inline %W{
      ./gradlew manageLeonardoRuntimes -PappArgs=['list','#{api_url}','#{op.opts.google_project}','#{op.opts.format}']
    }
  end
end

Common.register_command({
  :invocation => "list-runtimes",
  :description => "List all runtimes in this environment",
  :fn => ->(*args) { list_runtimes("list-runtimes", *args) }
})

def update_cdr_config_options(cmd_name, args)
  op = WbOptionsParser.new(cmd_name, args)
  op.opts.dry_run = false
  op.add_option(
      "--dry_run",
      ->(opts, _) { opts.dry_run = "true"},
      "Make no changes.")
  return op
end

def update_cdr_config_for_project(cdr_config_file, dry_run)
  common = Common.new
  common.run_inline %W{
    ./gradlew updateCdrConfig
   -PappArgs=['#{cdr_config_file}',#{dry_run}]}
end

def update_cdr_config(cmd_name, *args)
  op = update_cdr_config_options(cmd_name, args)
  gcc = GcloudContextV2.new(op)
  op.parse.validate
  gcc.validate

  ENV.update(read_db_vars(gcc))
  cdr_config_file = must_get_env_value(gcc.project, :cdr_config_json)
  ServiceAccountContext.new(gcc.project).run do
    update_cdr_config_for_project("config/#{cdr_config_file}", op.opts.dry_run)
  end
end

Common.register_command({
  :invocation => "update-cdr-config",
  :description => "Update CDR config (tiers and versions) in a cloud environment",
  :fn => ->(*args) { update_cdr_config("update-cdr-config", *args)}
})

def update_cdr_config_local(cmd_name, *args)
  setup_local_environment
  op = update_cdr_config_options(cmd_name, args)
  op.parse.validate
  cdr_config_file = 'config/cdr_config_local.json'
  dry_run = false
  app_args = ["-PappArgs=['#{cdr_config_file}',#{dry_run}]"]
  common = Common.new
  common.run_inline %W{./gradlew updateCdrConfig} + app_args
end

Common.register_command({
  :invocation => "update-cdr-config-local",
  :description => "Update CDR config (tiers and versions) in the local environment",
  :fn => ->(*args) { update_cdr_config_local("update-cdr-config-local", *args)}
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
  common = Common.new
  op = WbOptionsParser.new(cmd_name, args)
  op.add_option(
    "--db-user [user]",
    ->(opts, v) { opts.db_user = v },
    "Optional database user to connect as, defaults to 'dev-readonly'. " +
    "To perform mutations use 'workbench'. Avoid using 'root' unless " +
    "absolutely necessary.")

  gcc = GcloudContextV2.new(op)
  op.parse.validate
  gcc.validate

  if op.opts.db_user.nil? || op.opts.db_user.empty?
    op.opts.db_user = "dev-readonly"
  end

  env = read_db_vars(gcc)
  user_to_password = {
    "dev-readonly" => env["DEV_READONLY_DB_PASSWORD"],
    "workbench" => env["WORKBENCH_DB_PASSWORD"],
    "root" => env["MYSQL_ROOT_PASSWORD"]
  }
  unless user_to_password.has_key?(op.opts.db_user)
    Common.new.error(
      "invalid --db-user provided, wanted one of #{user_to_password.keys}, got '#{op.opts.db_user}'")
    exit 1
  end
  db_password = user_to_password[op.opts.db_user]

  CloudSqlProxyContext.new(gcc.project).run do
    if op.opts.db_user == "dev-readonly"
      common.status ""
      common.status "Database session will be read-only; use --db-user to change this"
      common.status ""
    end
    common.status "Fetch credentials from #{gcs_vars_path(gcc.project)} to connect through a different SQL tool"
    common.run_inline(
      maybe_dockerize_mysql_cmd(
        "mysql --host=127.0.0.1 --port=3307 --user=#{op.opts.db_user} " +
        "--database=#{env["DB_NAME"]} --password=#{db_password}",
        true, true # interactive, tty
      ),
      db_password)
  end

end

Common.register_command({
  :invocation => "connect-to-cloud-db",
  :description => "Connect to a Cloud SQL database via mysql.",
  :fn => ->(*args) { connect_to_cloud_db("connect-to-cloud-db", *args) }
})

def connect_to_cloud_db_binlog(cmd_name, *args)
  common = Common.new
  op = WbOptionsParser.new(cmd_name, args)
  gcc = GcloudContextV2.new(op)
  op.parse.validate
  gcc.validate
  env = read_db_vars(gcc)
  CloudSqlProxyContext.new(gcc.project).run do
    common.status "\n" + "*" * 80
    common.status "Listing available journal files: "

    password = env["MYSQL_ROOT_PASSWORD"]
    run_with_redirects(
      "echo 'SHOW BINARY LOGS;' | " +
      maybe_dockerize_mysql_cmd(
        "mysql --host=127.0.0.1 --port=3307 --user=root " +
        "--database=#{env['DB_NAME']} --password=#{password}",
        true # interactive
      ),
      password)
    common.status "*" * 80

    common.status "\n" + "*" * 80
    common.status "mysql login has been configured. Pick a journal file from " +
                  "above and run commands like: "
    common.status "  mysqlbinlog -R mysql-bin.xxxxxx\n"
    common.status "See the Workbench playbook for more details."
    common.status "*" * 80

    run_with_redirects(
      "docker run -i -t --rm --network host --entrypoint '' " +
      "-v $(pwd)/libproject/with-mysql-login.sh:/with-mysql-login.sh " +
      "mariadb:10.11.8 /bin/bash -c " +
      "'export MYSQL_HOME=$(./with-mysql-login.sh root #{password}); /bin/bash'", password)
  end
end

Common.register_command({
  :invocation => "connect-to-cloud-db-binlog",
  :description => "Connect to a Cloud SQL database for mysqlbinlog access",
  :fn => ->(*args) { connect_to_cloud_db_binlog("connect-to-cloud-db-binlog", *args) }
})

def deploy_app(cmd_name, args, with_cron, with_queue)
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
  Dir.chdir("snippets-menu") do
    common.run_inline(%W{./build.rb build-snippets-menu})
  end
  common.run_inline %W{./gradlew :appengineStage}
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
    (with_queue ? %W{build/staged-app/WEB-INF/appengine-generated/queue.yaml} : []) +
    %W{--project #{gcc.project} #{promote}} +
    (op.opts.quiet ? %W{--quiet} : []) +
    (op.opts.version ? %W{--version #{op.opts.version}} : []))
end

def deploy_api(cmd_name, args)
  common = Common.new
  common.status "Deploying api..."
  deploy_app(cmd_name, args, true, true)
end

Common.register_command({
  :invocation => "deploy-api",
  :description => "Deploys the API server to the specified cloud project.",
  :fn => ->(*args) { deploy_api("deploy-api", args) }
})

def create_or_update_workbench_db()
  # This method assumes that a cloud SQL proxy is active, and that appropriate
  # env variables are exported to correspond to the target environment.
  run_with_redirects(
    "cat db/create_db.sql | envsubst | " +
    maybe_dockerize_mysql_cmd(
      "mysql -u \"root\" -p\"#{ENV["MYSQL_ROOT_PASSWORD"]}\" --host 127.0.0.1 --port 3307",
      true # interactive
    ),
    ENV["MYSQL_ROOT_PASSWORD"]
  )
end

def create_or_update_workbench_db_cmd(cmd_name, args)
  op = WbOptionsParser.new(cmd_name, args)
  gcc = GcloudContextV2.new(op)
  op.parse.validate
  gcc.validate()
  ENV.update(read_db_vars(gcc))
  ServiceAccountContext.new(gcc.project).run do
    create_or_update_workbench_db
  end
end

Common.register_command({
  :invocation => "create-or-update-workbench-db",
  :description => "Creates or updates the Workbench database and users.\n" \
                  "This can be used in the event that new users or permissions " \
                  "are added to create_db.sql, as this is not currently rerun " \
                  "during deployment. This process is separate from Liquibase " \
                  "migrations as these changes may require root, be necessary " \
                  "to bootstrap Liquibase, or require sensitive variables such " \
                  "as passwords which are unavailable to Liquibase.",
  :fn => ->(*args) { create_or_update_workbench_db_cmd("create-or-update-workbench-db", args) }
})

def migrate_database(gcc, serviceAccount = nil, dry_run = false)
  common = Common.new
  common.status "Migrating main database..."
  CloudSqlProxyContext.new(gcc.project, serviceAccount, gcc.creds_file).run do
    Dir.chdir("db") do
      run_inline_or_log(dry_run, %W{../gradlew update -PrunList=main})
    end
  end
end

def get_config_file(project)
  config_json = must_get_env_value(project, :config_json)
  return "config/#{config_json}"
end

def get_fc_config(project)
  return get_config(project)["firecloud"]
end

def get_billing_config(project)
  return get_config(project)["billing"]
end

def get_server_config(project)
  return get_config(project)["server"]
end

def get_billing_project_prefix(project)
  return get_billing_config(project)["projectNamePrefix"]
end

def get_leo_api_url(project)
  return get_fc_config(project)["leoBaseUrl"]
end

def get_access_tier_config(project, tier_short_name)
  tiers = get_cdr_config(project)["accessTiers"]
  tier = tiers.find { |tier| tier['shortName'] == tier_short_name }
  unless tier
    raise("Could not find access tier '#{tier_short_name}' in cdr_config for project #{project}")
  end
  return tier
end

def get_auth_domain_name(project, tier)
  return get_access_tier_config(project, tier)["authDomainName"]
end

def get_firecloud_base_url(project)
  return get_fc_config(project)["baseUrl"]
end

def load_config(project, dry_run = false)
  config_json = must_get_env_value(project, :config_json)
  unless config_json
    raise("unknown project #{project}, expected one of #{configs.keys}")
  end

  common = Common.new
  common.status "Loading #{config_json} into database..."
  run_inline_or_log(dry_run, %W{./gradlew loadConfig -Pconfig_key=main -Pconfig_file=config/#{config_json}})
  run_inline_or_log(dry_run, %W{./gradlew loadConfig -Pconfig_key=cdrBigQuerySchema -Pconfig_file=config/cdm/cdm_5_2.json})
end

def with_optional_cloud_proxy_and_db(gcc, service_account = nil, key_file = nil)
  common = Common.new
  if gcc.project == 'local'
    start_local_db_service()
    yield gcc
  else
    ENV.update(read_db_vars(gcc))
    ServiceAccountContext.new(gcc.project, service_account, key_file).run do
      yield gcc
    end
  end
end

def deploy(cmd_name, args)
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
  op.add_validator ->(opts) { raise ArgumentError.new("version required") unless opts.version }
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
  op.add_validator ->(opts) { raise ArgumentError.new("promote option required") if opts.promote.nil?}

  gcc = GcloudContextV2.new(op)
  op.parse.validate
  gcc.validate

  common = Common.new
  common.status "Running database migrations..."
  ENV.update(read_db_vars(gcc))
  # Note: `gcc` does not get correctly initialized with 'op.opts.account' so we need to be explicit
  migrate_database(gcc, op.opts.account, op.opts.dry_run)
  if (op.opts.key_file)
    ENV["GOOGLE_APPLICATION_CREDENTIALS"] = op.opts.key_file
  end
  load_config(gcc.project, op.opts.dry_run)
  cdr_config_file = must_get_env_value(gcc.project, :cdr_config_json)
  update_cdr_config_for_project("config/#{cdr_config_file}", op.opts.dry_run)

  # Keep the cloud proxy context open for the service account credentials.
  dry_flag = op.opts.dry_run ? %W{--dry-run} : []
  deploy_args = %W{
    --project #{gcc.project}
    --version #{op.opts.version}
    #{op.opts.promote ? "--promote" : "--no-promote"}
    --quiet
  } + dry_flag
  deploy_api(cmd_name, deploy_args)
end

Common.register_command({
  :invocation => "deploy",
  :description => "Run DB migrations and deploy the API server",
  :fn => ->(*args) { deploy("deploy", args) }
})


def deploy_tanagra(cmd_name, args)
  op = WbOptionsParser.new(cmd_name, args)
  op.opts.dry_run = false
  op.add_option(
    "--project [project]",
    ->(opts, v) { opts.project = v},
    "The Google Cloud project to deploy to."
  )
  op.add_option(
    "--account [account]",
    ->(opts, v) { opts.account = v},
    "Service account to act as for deployment, if any. Defaults to the GAE " +
    "default service account."
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
  op.add_option(
    "--quiet",
    ->(opts, _) { opts.quiet = true},
    "Don't display a confirmation prompt when deploying"
  )
  op.add_option(
    "--auth-token [auth-token]",
    ->(opts, v) { opts.auth_token = v},
    "Github token"
  )
  op.add_validator ->(opts) { raise ArgumentError.new("promote option required") if opts.promote.nil?}

  gcc = GcloudContextV2.new(op)
  op.parse.validate
  gcc.validate

  env_project = ENVIRONMENTS[op.opts.project]
  ENV.update(read_db_vars(gcc))
  ENV.update({"TANAGRA_SERVICE_ACCOUNT" => op.opts.project})
  ENV.update({"TANAGRA_ACCESS_CONTROL_BASE_PATH" => env_project.fetch(:api_endpoint_host)})
  ENV.update({"TANAGRA_ACCESS_CONTROL_MODEL" => env_project.fetch(:tanagra_access_control_model)})
  ENV.update({"TANAGRA_AUTH_DISABLE_CHECKS" => env_project.fetch(:tanagra_auth_disable_checks)})
  ENV.update({"TANAGRA_AUTH_GCP_ACCESS_TOKEN" => env_project.fetch(:tanagra_auth_gcp_access_token)})
  ENV.update({"TANAGRA_UNDERLAY_FILES" => env_project.fetch(:tanagra_underlay_files)})

  promote = "--no-promote"
  unless op.opts.promote.nil?
    promote = op.opts.promote ? "--promote" : "--no-promote"
  else
    promote = op.opts.version ? "--no-promote" : "--promote"
  end

  common = Common.new

  env = env_project.fetch(:env_name)
  common.run_inline("../ui/project.rb tanagra-dep --env #{env}")

  Dir.chdir('../tanagra-aou-utils/tanagra') do
    common.status "Building Tanagra API..."
    common.run_inline("GITHUB_ACTOR='vda-cicd' GITHUB_TOKEN='#{op.opts.auth_token}' ./gradlew -x test -PisMySQL clean service:build")

    common.status "Copying jar into appengine folder..."
    common.run_inline("mkdir -p ../appengine && cp ./service/build/libs/*SNAPSHOT.jar ../appengine/tanagraapi.jar")
  end

  Dir.chdir('../tanagra-aou-utils') do
    common.status "Building appengine config file..."
    common.run_inline("envsubst < tanagra-api.yaml > appengine/tanagra-api.yaml")

    common.status "Building env variables file..."
    common.run_inline("envsubst < tanagra_env_variables_template.yaml > appengine/tanagra_env_variables.yaml")
  end

  deploy_version = "tanagra-" + env_project.fetch(:tanagra_tag)
  deploy_version.gsub!(".", "-")

  Dir.chdir('../tanagra-aou-utils/appengine') do
    common.status "Deploying Tanagra API to appengine..."
    run_inline_or_log(op.opts.dry_run, %W{
      gcloud app deploy tanagra-api.yaml
      } + %W{--project #{gcc.project} #{promote}} +
      (op.opts.quiet ? %W{--quiet} : []) +
      (deploy_version ? %W{--version #{deploy_version}} : []))
  end
  common.status "Deployment of Tanagra API complete!"

end

Common.register_command({
  :invocation => "deploy-tanagra",
  :description => "Deploy the Tanagra API",
  :fn => ->(*args) { deploy_tanagra("deploy-tanagra", args) }
})


def run_cloud_migrations(cmd_name, args)
  op = WbOptionsParser.new(cmd_name, args)
  gcc = GcloudContextV2.new(op)
  op.parse.validate
  gcc.validate()
  ENV.update(read_db_vars(gcc))
  ServiceAccountContext.new(gcc.project).run do
    migrate_database(gcc)
  end
end

Common.register_command({
  :invocation => "run-cloud-migrations",
  :description => "Runs database migrations on the Cloud SQL database for the specified project.",
  :fn => ->(*args) { run_cloud_migrations("run-cloud-migrations", args) }
})

def update_cloud_config(cmd_name, args)
  op = WbOptionsParser.new(cmd_name, args)
  gcc = GcloudContextV2.new(op)
  op.parse.validate
  gcc.validate()
  ENV.update(read_db_vars(gcc))
  ServiceAccountContext.new(gcc.project).run do
    load_config(gcc.project)
  end
end

Common.register_command({
  :invocation => "update-cloud-config",
  :description => "Updates configuration in Cloud SQL database for the specified project.",
  :fn => ->(*args) { update_cloud_config("update-cloud-config", args) }
})

def docker_run(args)
  Common.new.run_inline %W{docker compose run --rm scripts} + args
end

Common.register_command({
  :invocation => "docker-run",
  :description => "Runs the specified command in a docker container.",
  :fn => ->(*args) { docker_run(args) }
})

def create_project_resources(gcc)
  common = Common.new
  common.status "Enabling Cloud Service APIs..."
  for service in SERVICES
    common.run_inline("gcloud services enable #{service} --project #{gcc.project}")
  end
  common.status "Creating GCS bucket to store credentials..."
  common.run_inline %W{gsutil mb -p #{gcc.project} -c regional -l us-central1 gs://#{gcc.project}-credentials/}
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

def random_password()
  return rand(36**20).to_s(36)
end

def set_access_module_timestamps(cmd_name, *args)
  common = Common.new

  op = WbOptionsParser.new(cmd_name, args)
  op.opts.project = TEST_PROJECT
  op.add_option(
    "--profile-user [profile-user]",
    ->(opts, v) { opts.profile_user = v },
    "User whose timestamps should be updated for the Profile module.  Use full email address.")
  op.add_option(
      "--ras-user [ras-user]",
      ->(opts, v) { opts.ras_user = v },
      "User whose timestamps should be updated for the RAS module.  Use full email address.")
  op.add_validator ->(opts) { raise ArgumentError.new('--profile-user is required') if opts.profile_user.nil?}
  op.add_validator ->(opts) { raise ArgumentError.new('--ras-user is required') if opts.ras_user.nil?}
  op.parse.validate

  # Create a cloud context and apply the DB connection variables to the environment.
  # These will be read by Gradle and passed as Spring Boot properties to the command-line.
  gcc = GcloudContextV2.new(op)
  gcc.validate()

  gradle_args = ([
      ["--profile-user", op.opts.profile_user],
      ["--ras-user", op.opts.ras_user]
  ]).map { |kv| "#{kv[0]}=#{kv[1]}" }
  # Gradle args need to be single-quote wrapped.
  gradle_args.map! { |f| "'#{f}'" }

  ENV.update(read_db_vars(gcc))
  ServiceAccountContext.new(gcc.project).run do
    common.run_inline %W{./gradlew setAccessModuleTimestamps -PappArgs=[#{gradle_args.join(',')}]}
  end
end

SET_ACCESS_MODULE_TIMESTAMPS_CMD = "set-access-module-timestamps"

Common.register_command({
    :invocation => SET_ACCESS_MODULE_TIMESTAMPS_CMD,
    :description => "Set access module timestamps for e2e test users.",
    :fn => ->(*args) {set_access_module_timestamps(SET_ACCESS_MODULE_TIMESTAMPS_CMD, *args)}
})

def export_workspace_operations(cmd_name, *args)
  op = WbOptionsParser.new(cmd_name, args)
  op.add_typed_option(
        '--project [project]',
        String,
        ->(opts, p) { opts.project = p },
        'AoU environment GCP project full name. Used to pick MySQL instance & credentials.')
   op.opts.project = TEST_PROJECT
   op.parse.validate

  # Create a cloud context and apply the DB connection variables to the environment.
  # These will be read by Gradle and passed as Spring Boot properties to the command-line.
  gcc = GcloudContextV2.new(op)
  gcc.validate()

  common = Common.new
  ENV.update(read_db_vars(gcc))
  ServiceAccountContext.new(gcc.project).run do
    common.run_inline %W{./gradlew exportWorkspaceOperations}
  end
end

EXPORT_WORKSPACE_OPERATIONS_CMD = "export-workspace-operations"

Common.register_command({
    :invocation => EXPORT_WORKSPACE_OPERATIONS_CMD,
    :description => "Export the workspace_operations table.",
    :fn => ->(*args) {export_workspace_operations(EXPORT_WORKSPACE_OPERATIONS_CMD, *args)}
})

def export_import_cloudsql_cdr(cmd_name, *args)
  op = WbOptionsParser.new(cmd_name, args)
  op.add_option(
    "--cdr-db-name [cdr-db-name]",
    ->(opts, v) { opts.cdr_db_name = v},
    "cdrDbName - Name used cdr_config. Required."
  )
  op.add_validator ->(opts) { raise ArgumentError unless opts.cdr_db_name }
  op.parse.validate

  common = Common.new
  common.run_inline %W{./db-cdr/generate-cdr/export-import-cloudsql-cdr.sh #{op.opts.cdr_db_name}}

end

Common.register_command({
  :invocation => "export-import-cloudsql-cdr",
  :description => "Export cdr database from preprod and import to prod.",
  :fn => ->(*args) { export_import_cloudsql_cdr("export-import-cloudsql-cdr", *args) }
})

def verify_cloud_cdr_counts(cmd_name, *args)
  op = WbOptionsParser.new(cmd_name, args)
  op.add_option(
    "--cdr-db-name [cdr-db-name]",
    ->(opts, v) { opts.cdr_db_name = v},
    "cdrDbName - Name used cdr_config. Required."
  )
  op.add_validator ->(opts) { raise ArgumentError unless opts.cdr_db_name }
  op.opts.db_user = "dev-readonly" # do not override always read-only user
  op.opts.script= "db-cdr/generate-cdr/verify-counts.sql" # script cannot do any modifications

  # for preprod
  op.opts.project = "all-of-us-rw-preprod"
  verify_preprod_prod_counts(op)

  # for prod
  op.opts.project = "all-of-us-rw-prod"
  verify_preprod_prod_counts(op)

end

Common.register_command({
  :invocation => "verify-cloud-cdr-counts",
  :description => "Connect to a Cloud SQL database via mysql and run the provided sql file.",
  :fn => ->(*args) { verify_cloud_cdr_counts("verify-cloud-cdr-counts", *args) }
})

def verify_preprod_prod_counts(op)
  common = Common.new
  gcc = GcloudContextV2.new(op)
  op.parse.validate
  gcc.validate
  sql = File.read(op.opts.script)
  sql = sql.gsub(/@SCHEMA_NAME/, op.opts.cdr_db_name)
  sql = sql.gsub(/@PROJECT/, op.opts.project)
  env = read_db_vars(gcc)
  user_to_password = {
    "dev-readonly" => env["DEV_READONLY_DB_PASSWORD"],
  }
  db_password = user_to_password[op.opts.db_user]

  CloudSqlProxyContext.new(gcc.project).run do
    common.status "Fetch credentials from #{gcs_vars_path(gcc.project)} to connect through a different SQL tool"
    common.status common.bold_term_text(common.red_term_text("======="+op.opts.project+"======="))
    common.run_inline(
      run_mysql_cmd(" mysql --table --host=127.0.0.1 --port=3307 --user=#{op.opts.db_user} " +
          " --database=#{env["DB_NAME"]} --password=#{db_password} -e \"#{sql}\""),
      db_password)
    common.status " "
  end
end

def run_mysql_cmd(cmd)
  if Workbench.in_docker?
    return cmd
  end
  return "docker run " +
    "--rm " +
    "--network host " +
    "--entrypoint '' " +
    "-it " +
    "mariadb:10.11.8 " +
    cmd
end

def delete_workspaces(cmd_name, *args)
  common = Common.new

  op = WbOptionsParser.new(cmd_name, args)

  op.add_typed_option(
        '--project [project]',
        String,
        ->(opts, v) { opts.project = v },
        'AoU environment GCP project full name. Used to pick MySQL instance & credentials.')
  op.opts.project = TEST_PROJECT

  op.add_typed_option(
        '--username [email]',
        String,
        ->(opts, v) { opts.username = v },
        'The user whose workspaces we want to delete.')
  op.add_validator ->(opts) { raise ArgumentError.new("Username required") unless opts.username }

  op.add_typed_option(
      '--limit [limit]',
      String,
      ->(opts, v) { opts.limit = v },
      'The maximum number of workspaces to delete per step.')

  op.add_typed_option(
      '--delete',
      TrueClass,
      ->(opts, v) { opts.delete = v },
      'Set to delete. Defaults to count only.')
  op.opts.delete = false

  op.parse.validate

  # Create a cloud context and apply the DB connection variables to the environment.
  # These will be read by Gradle and passed as Spring Boot properties to the command-line.
  gcc = GcloudContextV2.new(op)
  gcc.validate()

  gradle_args = ([
    ["--project", op.opts.project],
    ["--username", op.opts.username],
    ["--limit", op.opts.limit],
    ["--delete", op.opts.delete],
  ]).map { |kv| "#{kv[0]}=#{kv[1]}" }
  # Gradle args need to be single-quote wrapped.
  gradle_args.map! { |f| "'#{f}'" }

  ENV.update(read_db_vars(gcc))
  CloudSqlProxyContext.new(gcc.project).run do
    common.run_inline %W{./gradlew deleteWorkspaces -PappArgs=[#{gradle_args.join(',')}]}
  end
end

DELETE_WORKSPACES_CMD = "delete-workspaces"

Common.register_command({
    :invocation => DELETE_WORKSPACES_CMD,
    :description => "Delete a user's workspaces in AoU as well as any in Terra-Rawls which are no longer in AoU",
    :fn => ->(*args) {delete_workspaces(DELETE_WORKSPACES_CMD, *args)}
})

def backfill_gsuite_user_data(cmd_name, *args)
  common = Common.new

  op = WbOptionsParser.new(cmd_name, args)
  op.opts.dry_run = false
  op.opts.project = TEST_PROJECT

  op.add_typed_option(
    '--project [project]',
    String,
    ->(opts, v) { opts.project = v },
    'AoU environment GCP project full name. Used to pick MySQL instance & credentials.')
  op.opts.project = TEST_PROJECT

  op.add_typed_option(
    '--dry-run [dryrun]',
    TrueClass,
    ->(opts, v) { opts.dry_run = v },
    'If true, no modifications will be made')


  op.parse.validate

  # Create a cloud context and apply the DB connection variables to the environment.
  # These will be read by Gradle and passed as Spring Boot properties to the command-line.
  gcc = GcloudContextV2.new(op)
  gcc.validate()

  if op.opts.dry_run
    common.status "DRY RUN -- CHANGES WILL NOT BE PERSISTED"
  end

  gradle_args = ([
    ["--project", op.opts.project],
    ["--dry-run", op.opts.dry_run],
  ]).map { |kv| "#{kv[0]}=#{kv[1]}" }
  # Gradle args need to be single-quote wrapped.
  gradle_args.map! { |f| "'#{f}'" }

  ENV.update(read_db_vars(gcc))
  CloudSqlProxyContext.new(gcc.project).run do
    common.run_inline %W{./gradlew backfillGSuiteUserData -PappArgs=[#{gradle_args.join(',')}]}
  end
end

Common.register_command({
    :invocation => "backfill-absorb",
    :description => "Backfills the Absorb External Department ID field in GSuite.\n",
    :fn => ->(*args) {backfill_gsuite_user_data("backfill_gsuite_user_data", *args)}
})

def list_disks(cmd_name, *args)
  common = Common.new

  op = WbOptionsParser.new(cmd_name, args)

  op.add_typed_option(
    '--project [project]',
    String,
    ->(opts, v) { opts.project = v },
    'AoU environment GCP project full name. Used to pick MySQL instance & credentials.')
  op.opts.project = TEST_PROJECT

  op.add_typed_option(
    '--output [output file name]',
    String,
    ->(opts, v) { opts.output = v },
    'Output file name.')

  op.add_validator ->(opts) { raise ArgumentError unless opts.output}

  op.parse.validate


  # Create a cloud context and apply the DB connection variables to the environment.
  # These will be read by Gradle and passed as Spring Boot properties to the command-line.
  gcc = GcloudContextV2.new(op)
  gcc.validate()

  gradle_args = ([
    ["--output", op.opts.output],
  ]).map { |kv| "#{kv[0]}=#{kv[1]}" }
  # Gradle args need to be single-quote wrapped.
  gradle_args.map! { |f| "'#{f}'" }

  ENV.update(read_db_vars(gcc))
  CloudSqlProxyContext.new(gcc.project).run do
    common.run_inline %W{./gradlew listDisks -PappArgs=[#{gradle_args.join(',')}]}
  end
end

LIST_DISKS_CMD = "list-disks"

Common.register_command({
    :invocation => LIST_DISKS_CMD,
    :description => "Creates a CSV report of all persistent disks in the environment.",
    :fn => ->(*args) {list_disks(LIST_DISKS_CMD, *args)}
})

VALID_EMAIL_OPTION = 'egress'
def send_email(cmd_name, *args)
  common = Common.new

  op = WbOptionsParser.new(cmd_name, args)

  op.add_typed_option(
    '--project [project]',
    String,
    ->(opts, v) { opts.project = v },
    'AoU environment GCP project full name. Used to pick MySQL instance & credentials.')
  op.opts.project = TEST_PROJECT

  op.add_typed_option(
    '--email [which email to send]',
    String,
    ->(opts, v) { opts.email = v },
    "Currently, 'egress' is the only valid option.")
  op.opts.email = VALID_EMAIL_OPTION

  op.add_typed_option(
    '--username [user name]',
    String,
    ->(opts, v) { opts.username = v },
    'User name.')

  op.add_typed_option(
    '--given_name [given name]',
    String,
    ->(opts, v) { opts.given_name = v },
    'User given (first) name.')

  op.add_typed_option(
    '--contact [contact email]',
    String,
    ->(opts, v) { opts.contact = v },
    'User contact email.')

  op.add_typed_option(
    '--disable',
    String,
    ->(opts, _) { opts.disable = true },
    'If specified, sends the DISABLE_USER egress email.  Defaults to the SUSPEND_COMPUTE egress email.')
  op.opts.disable = false

  op.add_validator ->(opts) { raise ArgumentError unless opts.username and opts.given_name and opts.contact and opts.email == VALID_EMAIL_OPTION }

  op.parse.validate

  gradle_args = ([
    ["--username", op.opts.username],
    ["--given_name", op.opts.given_name],
    ["--contact", op.opts.contact],
    ["--disable", op.opts.disable],
    ["--email", op.opts.email],
 ]).map { |kv| "#{kv[0]}=#{kv[1]}" }
  # Gradle args need to be single-quote wrapped.
  gradle_args.map! { |f| "'#{f}'" }

  # Create a cloud context and apply the DB connection variables to the environment.
  # These will be read by Gradle and passed as Spring Boot properties to the command-line.
  gcc = GcloudContextV2.new(op)
  gcc.validate()
  ENV.update(read_db_vars(gcc))
  CloudSqlProxyContext.new(gcc.project).run do
    common.run_inline %W{./gradlew sendEmail -PappArgs=[#{gradle_args.join(',')}]}
  end
end

SEND_EMAIL_CMD = "send-email"

# example usage:
#   ./project.rb send-email \
#   --username joel@fake-research-aou.org \
#   --contact thibault@broadinstitute.org \
#   --disable
Common.register_command({
    :invocation => SEND_EMAIL_CMD,
    :description => "Sends a system email.  Currently limited to egress emails.",
    :fn => ->(*args) {send_email(SEND_EMAIL_CMD, *args)}
})

def run_genomic_extraction(cmd_name, *args)
  common = Common.new

  op = WbOptionsParser.new(cmd_name, args)

  op.add_typed_option(
    '--project [project]',
    String,
    ->(opts, v) { opts.project = v },
    'AoU environment GCP project full name. Used to pick MySQL instance & credentials.')
  op.opts.project = TEST_PROJECT

  op.add_typed_option(
    '--namespace [workspace namespace]',
    String,
    ->(opts, v) { opts.namespace = v },
    'The workspace namespace to run the extraction from.')

  op.add_typed_option(
    '--dataset_id [dataset id]',
    String,
    ->(opts, v) { opts.dataset_id = v },
    'The dataset to record in the DB as associated with this extraction (arbitrary but must exist).')

  op.add_typed_option(
    '--person_id_file [person_ids.txt]',
    String,
    ->(opts, v) { opts.person_id_file = v },
    'The file of person IDs to use in the extraction.  Note: skips the first row, assumed to be a \
    header. Person ID lines are plain unquoted text.')

  op.add_typed_option(
    '--legacy [true/false]',
    String,
    ->(opts, v) { opts.legacy = v },
    'Use legacy (v7 and earlier) workflow (true) or v8+ workflow (false).')

  op.add_typed_option(
    '--filter_set [filter set]',
    String,
    ->(opts, v) { opts.filter_set = v },
    'Filter set name.')

  op.add_typed_option(
    '--cdr_bq_project [project]',
    String,
    ->(opts, v) { opts.cdr_bq_project = v },
    "The CDR's BigQuery project.")

  op.add_typed_option(
    '--wgs_bq_dataset [dataset]',
    String,
    ->(opts, v) { opts.wgs_bq_dataset = v },
    "The CDR's WGS BigQuery dataset")

  op.add_validator ->(opts) {
    raise ArgumentError unless opts.namespace and opts.dataset_id and opts.person_id_file and opts.legacy and opts.filter_set and opts.cdr_bq_project and opts.wgs_bq_dataset
  }

  op.parse.validate

  gradle_args = ([
    ["--namespace", op.opts.namespace],
    ["--dataset_id", op.opts.dataset_id],
    ["--person_id_file", op.opts.person_id_file],
    ["--legacy", op.opts.legacy],
    ["--filter_set", op.opts.filter_set],
    ["--cdr_bq_project", op.opts.cdr_bq_project],
    ["--wgs_bq_dataset", op.opts.wgs_bq_dataset],
 ]).map { |kv| "#{kv[0]}=#{kv[1]}" }
  # Gradle args need to be single-quote wrapped.
  gradle_args.map! { |f| "'#{f}'" }

  # Create a cloud context and apply the DB connection variables to the environment.
  # These will be read by Gradle and passed as Spring Boot properties to the command-line.
  gcc = GcloudContextV2.new(op)
  gcc.validate()
  ENV.update(read_db_vars(gcc))
  CloudSqlProxyContext.new(gcc.project).run do
    common.run_inline %W{./gradlew runGenomicExtraction -PappArgs=[#{gradle_args.join(',')}]}
  end

end

GENOMIC_EXTRACTION_CMD = "run-extraction"

# example usage:
# ./project.rb run-extraction \
# --namespace aou-rw-test-0bead07c \
# --dataset_id 65204 \
# --person_id_file dataset_ids.txt \
# --legacy false \
# --filter_set echo-controls \
# --cdr_bq_project fc-aou-cdr-synth-test-2 \
# --wgs_bq_dataset echo_controls
Common.register_command({
    :invocation => GENOMIC_EXTRACTION_CMD,
    :description => "Runs a genomic extraction workflow.  Requires a workspace in the Controlled Tier but can vary from what it specifies in its CDR Configuration.",
    :fn => ->(*args) {run_genomic_extraction(GENOMIC_EXTRACTION_CMD, *args)}
})
