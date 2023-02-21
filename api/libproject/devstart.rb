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
  return vars.merge({
    'CDR_DB_CONNECTION_STRING' => cdr_vars['DB_CONNECTION_STRING'],
    'CDR_DB_USER' => cdr_vars['WORKBENCH_DB_USER'],
    'CDR_DB_PASSWORD' => cdr_vars['WORKBENCH_DB_PASSWORD']
  })
end

def format_benchmark(bm)
  "%ds" % [bm.real]
end

def start_local_db_service()
  common = Common.new
  deadlineSec = 40

  bm = Benchmark.measure {
    common.run_inline %W{docker-compose up -d db}

    root_pass = Workbench.read_vars_file("db/local-vars.env")["MYSQL_ROOT_PASSWORD"]

    common.status "waiting up to #{deadlineSec}s for mysql service to start..."
    start = Time.now
    until (common.run "docker-compose exec -T db mysql -p#{root_pass} --silent -e 'SELECT 1;'").success?
      if Time.now - start >= deadlineSec
        raise("mysql docker service did not become available after #{deadlineSec}s")
      end
      sleep 1
    end
  }
  common.status "Database startup complete (#{format_benchmark(bm)})"
end

def dev_up(cmd_name, args)
  op = WbOptionsParser.new(cmd_name, args)
  op.opts.start_db = true
  op.add_option(
      "--nostart-db",
      ->(opts, _) { opts.start_db = false },
      "If specified, don't start the DB service. This is useful when running " +
      "within docker, i.e. on CircleCI, as the DB service runs via docker-compose")
  op.parse.validate

  common = Common.new

  account = get_auth_login_account()
  if account.nil?
    raise("Please run 'gcloud auth login' before starting the server.")
  end

  at_exit do
    common.run_inline %W{docker-compose down} if op.opts.start_db
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
  ENV["DB_HOST"] = "127.0.0.1"
  ENV["DB_CONNECTION_STRING"] = "jdbc:mysql://127.0.0.1/workbench?useSSL=false"
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
    common.run_inline "./gradlew --daemon appengineRun &"

    # incrementalHotSwap must be run without the Gradle daemon or stdout and stderr will not appear
    # in the output.
    common.run_inline %W{./gradlew --continuous incrementalHotSwap}
  rescue Interrupt
    # Do nothing
  ensure
    common.run_inline %W{./gradlew --stop}
  end
end

def run_api_and_db()
  setup_local_environment

  common = Common.new
  at_exit { common.run_inline %W{docker-compose down} }
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
  common.run_inline %W{docker-compose exec db sh -c #{cmd}}
end

Common.register_command({
  :invocation => "connect-to-db",
  :description => "Connect to the running database via mysql.",
  :fn => ->() { connect_to_db() }
})


def docker_clean()
  common = Common.new

  # --volumes clears out any cached data between runs, e.g. the MySQL database
  common.run_inline %W{docker-compose down --volumes}

  # This keyfile gets created and cached locally on dev-up. Though it's not
  # specific to Docker, it is mounted locally for docker runs. For lack of a
  # better "dev teardown" hook, purge that file here; e.g. in case we decide to
  # invalidate a dev key or change the service account.
  common.run_inline %W{rm -f #{ServiceAccountContext::SERVICE_ACCOUNT_KEY_PATH}}

  # See https://github.com/docker/compose/issues/3447
  common.status "Cleaning complete. docker-compose 'not found' errors can be safely ignored"
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

def create_tables(cmd_name, *args)
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
    common.run_inline %W{./generate-cdr/create-tables.sh #{op.opts.bq_project} #{op.opts.bq_dataset} #{op.opts.create_prep_tables}}
  end
end

Common.register_command({
  :invocation => "create-tables",
  :description => "Create the CDR indices tables.",
  :fn => ->(*args) { create_tables("create-tables", *args) }
})

def build_static_prep_tables(cmd_name, *args)
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

  op.add_validator ->(opts) { raise ArgumentError unless opts.bq_project and opts.bq_dataset}
  op.parse.validate

  common = Common.new
  Dir.chdir('db-cdr') do
    common.run_inline %W{./generate-cdr/build-static-prep-tables.sh #{op.opts.bq_project} #{op.opts.bq_dataset}}
  end
end

Common.register_command({
  :invocation => "build-static-prep-tables",
  :description => "Create prep tables from csv files in Google bucket",
  :fn => ->(*args) { build_static_prep_tables("build-static-prep-tables", *args) }
})

def build_prep_concept_merged(cmd_name, *args)
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

  op.add_validator ->(opts) { raise ArgumentError unless opts.bq_project and opts.bq_dataset}
  op.parse.validate

  common = Common.new
  Dir.chdir('db-cdr') do
    common.run_inline %W{./generate-cdr/build-prep-concept-merged.sh #{op.opts.bq_project} #{op.opts.bq_dataset}}
  end
end

Common.register_command({
  :invocation => "build-prep-concept-merged",
  :description => "Create prep concept tables",
  :fn => ->(*args) { build_prep_concept_merged("build-prep-concept-merged", *args) }
})

def build_cb_survey_version(cmd_name, *args)
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

  op.add_validator ->(opts) { raise ArgumentError unless opts.bq_project and opts.bq_dataset}
  op.parse.validate

  common = Common.new
  Dir.chdir('db-cdr') do
    common.run_inline %W{./generate-cdr/build-cb-survey-version.sh #{op.opts.bq_project} #{op.opts.bq_dataset}}
  end
end

Common.register_command({
  :invocation => "build-cb-survey-version",
  :description => "Generates the cb_survey_version table",
  :fn => ->(*args) { build_cb_survey_version("build-cb-survey-version", *args) }
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

def build_ds_linking(cmd_name, *args)
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

  op.add_validator ->(opts) { raise ArgumentError unless opts.bq_project and opts.bq_dataset}
  op.parse.validate

  common = Common.new
  Dir.chdir('db-cdr') do
    common.run_inline %W{./generate-cdr/build-ds-linking.sh #{op.opts.bq_project} #{op.opts.bq_dataset}}
  end
end

Common.register_command({
  :invocation => "build-ds-linking",
  :description => "Generates the big query denormalized tables for dataset builder",
  :fn => ->(*args) { build_ds_linking("build-ds-linking", *args) }
})

def build_ds_tables(cmd_name, *args)
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
      "--table-token [table-token]",
      ->(opts, v) { opts.table_token = v},
      "Generate specified table - Required"
  )

  op.add_validator ->(opts) { raise ArgumentError unless opts.bq_project and opts.bq_dataset and opts.table_token}
  op.parse.validate

  common = Common.new
  Dir.chdir('db-cdr') do
    common.run_inline %W{./generate-cdr/build-ds-tables.sh #{op.opts.bq_project} #{op.opts.bq_dataset} #{op.opts.table_token}}
  end
end

Common.register_command({
  :invocation => "build-ds-tables",
  :description => "Generates the big query denormalized tables for dataset builder",
  :fn => ->(*args) { build_ds_tables("build-ds-tables", *args) }
})

def build_review_all_events(cmd_name, *args)
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
    "--domain-token [domain-token]",
    ->(opts, v) { opts.domain_token = v},
    "Generate for domain-token - Required."
  )

  op.add_validator ->(opts) { raise ArgumentError unless opts.bq_project and opts.bq_dataset and opts.domain_token}
  op.parse.validate

  common = Common.new
  Dir.chdir('db-cdr') do
    common.run_inline %W{./generate-cdr/build-review-all-events.sh #{op.opts.bq_project} #{op.opts.bq_dataset} #{op.opts.domain_token}}
  end
end

Common.register_command({
  :invocation => "build-review-all-events",
  :description => "Generates the big query denormalized tables for review",
  :fn => ->(*args) { build_review_all_events("build-review-all-events", *args) }
})

def build_cb_search_person(cmd_name, *args)
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

  op.add_validator ->(opts) { raise ArgumentError unless opts.bq_project and opts.bq_dataset}
  op.parse.validate

  common = Common.new
  Dir.chdir('db-cdr') do
    common.run_inline %W{./generate-cdr/build-cb-search-person.sh #{op.opts.bq_project} #{op.opts.bq_dataset}}
  end
end

Common.register_command({
  :invocation => "build-cb-search-person",
  :description => "Generates the big query denormalized tables for search",
  :fn => ->(*args) { build_cb_search_person("build-cb-search-person", *args) }
})

def build_cb_criteria_missing_codes(cmd_name, *args)
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

  op.add_validator ->(opts) { raise ArgumentError unless opts.bq_project and opts.bq_dataset}
  op.parse.validate

  common = Common.new
  Dir.chdir('db-cdr') do
    common.run_inline %W{./generate-cdr/build-cb-criteria-missing-codes.sh #{op.opts.bq_project} #{op.opts.bq_dataset}}
  end
end

Common.register_command({
  :invocation => "build-cb-criteria-missing-codes",
  :description => "Adds other codes not already captured",
  :fn => ->(*args) { build_cb_criteria_missing_codes("build-cb-criteria-missing-codes", *args) }
})

def build_cb_criteria_menu(cmd_name, *args)
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

  op.add_validator ->(opts) { raise ArgumentError unless opts.bq_project and opts.bq_dataset}
  op.parse.validate

  common = Common.new
  Dir.chdir('db-cdr') do
    common.run_inline %W{./generate-cdr/build-cb-criteria-menu.sh #{op.opts.bq_project} #{op.opts.bq_dataset}}
  end
end

Common.register_command({
  :invocation => "build-cb-criteria-menu",
  :description => "Generates the criteria menu for cohort builder",
  :fn => ->(*args) { build_cb_criteria_menu("build-cb-criteria-menu", *args) }
})

def build_output_tables(cmd_name, *args)
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
  :invocation => "build-output-tables",
  :description => "Build tables for output dataset",
  :fn => ->(*args) { build_output_tables("build-output-tables", *args) }
})

def build_cb_criteria_attribute_tables_and_cleanup(cmd_name, *args)
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

  op.add_validator ->(opts) { raise ArgumentError unless opts.bq_project and opts.bq_dataset}
  op.parse.validate

  common = Common.new
  Dir.chdir('db-cdr') do
    common.run_inline %W{./generate-cdr/build-cb-criteria-attribute-tables-and-cleanup.sh #{op.opts.bq_project} #{op.opts.bq_dataset}}
  end
end

Common.register_command({
  :invocation => "build-cb-criteria-attribute-tables-and-cleanup",
  :description => "Populate other cb_* tables",
  :fn => ->(*args) { build_cb_criteria_attribute_tables_and_cleanup("build-cb-criteria-attribute-tables-and-cleanup", *args) }
})

def build_cb_criteria_full_text_synonym(cmd_name, *args)
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

  op.add_validator ->(opts) { raise ArgumentError unless opts.bq_project and opts.bq_dataset}
  op.parse.validate

  common = Common.new
  Dir.chdir('db-cdr') do
    common.run_inline %W{./generate-cdr/build-cb-criteria-full-text-synonym.sh #{op.opts.bq_project} #{op.opts.bq_dataset}}
  end
end

Common.register_command({
  :invocation => "build-cb-criteria-full-text-synonym",
  :description => "Populate other cb_* tables",
  :fn => ->(*args) { build_cb_criteria_full_text_synonym("build-cb-criteria-full-text-synonym", *args) }
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

def build_prep_table(cmd_name, *args)
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
    common.run_inline %W{./generate-cdr/build-prep-#{op.opts.script}.sh #{op.opts.bq_project} #{op.opts.bq_dataset}}
  end
end

Common.register_command({
  :invocation => "build-prep-table",
  :description => "Build a prep table",
  :fn => ->(*args) { build_prep_table("build-prep-table", *args) }
})

def build_cb_criteria(cmd_name, *args)
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
    common.run_inline %W{./generate-cdr/build-cb-criteria-#{op.opts.script}.sh #{op.opts.bq_project} #{op.opts.bq_dataset}}
  end
end

Common.register_command({
  :invocation => "build-cb-criteria",
  :description => "Builds cb_criteria",
  :fn => ->(*args) { build_cb_criteria("build-cb-criteria", *args) }
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

  with_cloud_proxy_and_db(gcc) do
    common = Common.new
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

def write_db_creds_file(project, cdr_db_name, root_password, workbench_password, readonly_password)
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
      db_creds_file.puts "DEV_READONLY_DB_USER=dev-readonly"
      db_creds_file.puts "DEV_READONLY_DB_PASSWORD=#{readonly_password}"
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
      ["--fc-base-url", fc_config["baseUrl"]],
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

  with_cloud_proxy_and_db(gcc) do
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

  with_cloud_proxy_and_db(gcc) do
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

def delete_workspaces(cmd_name, *args)
  common = Common.new

  op = WbOptionsParser.new(cmd_name, args)
  op.opts.dry_run = true
  if op.opts.project.to_s.empty?
    op.opts.project = TEST_PROJECT
  end

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
        ./gradlew deleteWorkspaces
       -PappArgs=[#{flags.join(',')}]}
  end
end

DELETE_WORKSPACES_CMD = "delete-workspaces"

Common.register_command({
    :invocation => DELETE_WORKSPACES_CMD,
    :description => "Delete workspaces listed in given file.\n",
    :fn => ->(*args) {delete_workspaces(DELETE_WORKSPACES_CMD, *args)}
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

  with_cloud_proxy_and_db(gcc) do
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
  op.opts.remove = false
  op.opts.dry_run = false
  op.add_option(
       "--email [EMAIL,...]",
       ->(opts, v) { opts.email = v},
       "Comma-separated list of user accounts to change. Required.")
  op.add_option(
      "--authority [AUTHORITY,...]",
      ->(opts, v) { opts.authority = v},
      "Comma-separated list of user authorities to add or remove for the users. " +
      "Include keyword ALL to include all authorities; typically that should only " +
      "be used with removals. When granting authorities, use DEVELOPER to gain full access")
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
  op = authority_options(cmd_name, args)
  gcc = GcloudContextV2.new(op)
  op.parse.validate
  gcc.validate

  if not op.opts.remove and op.opts.authority.upcase.include? "ALL"
    get_user_confirmation(
      "Adding ALL authorities is redundant and rarely useful; to transitively " +
      "grant all authorities, simply add the all-encompassing DEVELOPER authority.\n" +
      "Do you want to add ALL authorities anyways?"
    )
  end

  with_cloud_proxy_and_db(gcc) do
    common = Common.new
    common.run_inline %W{
      ./gradlew setAuthority
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

  with_cloud_proxy_and_db(gcc) do
    ServiceAccountContext.new(gcc.project).run do
      common.run_inline %W{
         ./gradlew createWgsCohortExtractionBillingProjectWorkspace
         -PappArgs=[#{flags.join(',')}]}
    end
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
    "Google project to act on (e.g. all-of-us-workbench-test). Cannot be used with --all-projects"
  )
  op.opts.all_projects = false
  op.add_option(
    "--all-projects [all-projects]",
    ->(opts, _) { opts.all_projects = true},
    "Create snapshot in every AoU environment. Cannot be used with --project.")

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
    "--method-name [method-name]",
    ->(opts, v) { opts.method_name = v},
    "Agora method name to create snapshot in. default: WorkbenchConfig.wgsCohortExtraction.extractionMethodConfigurationName
          Method Namespace will be pulled from WorkbenchConfig.wgsCohortExtraction.extractionMethodConfigurationNamespace")
  op.add_validator ->(opts) {
    if (!opts.project and !opts.all_projects)
      common.error "A project must be set or --all-projects must be true"
      raise ArgumentError
    end
  }

  # Use GcloudContextV2 to validate gcloud auth but we need to drop the
  # --project argument validation that's built into the constructor
  GcloudContextV2.validate_gcloud_auth()
  op.parse.validate

  source_file_commit_hash = get_github_commit_hash(op.opts.source_git_repo, op.opts.source_git_ref)

  projects = op.opts.all_projects ? ENVIRONMENTS.keys - ["local"] : [op.opts.project]
  projects.each { |project|
    extractionConfig = get_config(project)['wgsCohortExtraction']
    flags = ([
      ["--config-json", get_config_file(project)],
      ["--source-git-repo", op.opts.source_git_repo],
      ["--source-git-path", op.opts.source_git_path],
      ["--source-git-ref", source_file_commit_hash],
      ["--method-namespace", extractionConfig['extractionMethodConfigurationNamespace']],
      ["--method-name", op.opts.method_name || extractionConfig['extractionMethodConfigurationName']],
    ]).map { |kv| "#{kv[0]}=#{kv[1]}" }
    flags.map! { |f| "'#{f}'" }

    ServiceAccountContext.new(project).run do
      common.run_inline %W{
       ./gradlew createTerraMethodSnapshot
       -PappArgs=[#{flags.join(',')}]}
    end
  }
end

Common.register_command({
  :invocation => "create-terra-method-snapshot",
  :description => "Create Terra Method snapshot in single or all environments.
    Method Namespace will be pulled from WorkbenchConfig.wgsCohortExtraction.extractionMethodConfigurationNamespace",
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
  op.add_option(
      "--project [project]",
      ->(opts, v) { opts.project = v},
      "Required project ID")
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
      -PappArgs=['describe','#{api_url}','#{gcc.project}','#{ctx.service_account}','#{op.opts.runtime_id}']}
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
      "--runtime-project [project]",
      ->(opts, v) { opts.runtime_project = v},
      "Optionally filter by runtime project")
  op.add_option(
      "--include-deleted",
      ->(opts, _) { opts.include_deleted = true },
      "Whether to include deleted runtimes in the results; typically should only be used in " +
      "combination with --runtime-project, otherwise this could be very slow")
  op.add_option(
      "--format [format]",
      ->(opts, v) { opts.format = v },
      "JSON or TABULAR, defaults to TABULAR (summary)")
  op.opts.runtime_project = ""
  op.opts.include_deleted = false
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
      ./gradlew manageLeonardoRuntimes -PappArgs=['list','#{api_url}','#{op.opts.include_deleted}','#{op.opts.runtime_project}','#{op.opts.format}']
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

  with_cloud_proxy_and_db(gcc) do
    cdr_config_file = must_get_env_value(gcc.project, :cdr_config_json)
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
      "mariadb:10.2 /bin/bash -c " +
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
  with_cloud_proxy_and_db_env(cmd_name, args) do |ctx|
    # with_cloud_proxy_and_db_env loads env vars into scope which parameterize this call
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

def migrate_database(dry_run = false)
  common = Common.new
  common.status "Migrating main database..."
  Dir.chdir("db") do
    run_inline_or_log(dry_run, %W{../gradlew update -PrunList=main})
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
  featured_workspaces_json = must_get_env_value(project, :featured_workspaces_json)
  unless config_json and featured_workspaces_json
    raise("unknown project #{project}, expected one of #{configs.keys}")
  end

  common = Common.new
  common.status "Loading #{config_json} into database..."
  run_inline_or_log(dry_run, %W{./gradlew loadConfig -Pconfig_key=main -Pconfig_file=config/#{config_json}})
  run_inline_or_log(dry_run, %W{./gradlew loadConfig -Pconfig_key=cdrBigQuerySchema -Pconfig_file=config/cdm/cdm_5_2.json})
  run_inline_or_log(dry_run, %W{./gradlew loadConfig -Pconfig_key=featuredWorkspaces -Pconfig_file=config/#{featured_workspaces_json}})
end

def with_cloud_proxy_and_db(gcc, service_account = nil, key_file = nil)
  ENV.update(read_db_vars(gcc))
  ENV.update(must_get_env_value(gcc.project, :gae_vars))
  ENV["DB_PORT"] = "3307" # TODO(dmohs): Use MYSQL_TCP_PORT to be consistent with mysql CLI.
  CloudSqlProxyContext.new(gcc.project, service_account, key_file).run do
    yield(gcc)
  end
end

def with_optional_cloud_proxy_and_db(gcc, service_account = nil, key_file = nil)
  common = Common.new
  if gcc.project == 'local'
    start_local_db_service()
    yield gcc
  else
    common.status("Creating cloud proxy for environment #{gcc.project}")
    with_cloud_proxy_and_db(gcc, service_account, key_file) do |gcc|
      yield gcc
    end
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
end

Common.register_command({
  :invocation => "deploy",
  :description => "Run DB migrations and deploy the API server",
  :fn => ->(*args) { deploy("deploy", args) }
})


def run_cloud_migrations(cmd_name, args)
  with_cloud_proxy_and_db_env(cmd_name, args) { migrate_database }
end

Common.register_command({
  :invocation => "run-cloud-migrations",
  :description => "Runs database migrations on the Cloud SQL database for the specified project.",
  :fn => ->(*args) { run_cloud_migrations("run-cloud-migrations", args) }
})

def update_cloud_config(cmd_name, args)
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

def setup_project_data(gcc, cdr_db_name)
  root_password = random_password()
  workbench_password = random_password()
  readonly_password = random_password()

  common = Common.new
  # This changes database connection information; don't call this while the server is running!
  common.status "Writing DB credentials file..."
  write_db_creds_file(gcc.project, cdr_db_name, root_password, workbench_password, readonly_password)
  common.status "Setting root password..."
  run_with_redirects("gcloud sql users set-password root --host % --project #{gcc.project} " +
                     "--instance #{INSTANCE_NAME} --password #{root_password}",
                     root_password)
  # Don't delete the credentials created here; they will be stored in GCS and reused during
  # deployment, etc.
  with_cloud_proxy_and_db(gcc) do
    common.status "Setting up databases and users..."
    create_or_update_workbench_db

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

  create_project_resources(gcc)
  setup_project_data(gcc, op.opts.cdr_db_name)
end

Common.register_command({
  :invocation => "setup-cloud-project",
  :description => "Initializes resources within a cloud project that has already been created",
  :fn => ->(*args) { setup_cloud_project("setup-cloud-project", *args) }
})

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

  with_cloud_proxy_and_db(gcc) do
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
  with_cloud_proxy_and_db(gcc) do
    common.run_inline %W{./gradlew exportWorkspaceOperations}
  end
end

EXPORT_WORKSPACE_OPERATIONS_CMD = "export-workspace-operations"

Common.register_command({
    :invocation => EXPORT_WORKSPACE_OPERATIONS_CMD,
    :description => "Export the workspace_operations table.",
    :fn => ->(*args) {export_workspace_operations(EXPORT_WORKSPACE_OPERATIONS_CMD, *args)}
})
