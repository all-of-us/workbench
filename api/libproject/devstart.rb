# Calls to common.run_inline in this file may use a quoted string purposefully
# to cause system() or spawn() to run the command in a shell. Calls with arrays
# are not run in a shell, which can break usage of the CloudSQL proxy.

require_relative "../../aou-utils/serviceaccounts"
require_relative "../../aou-utils/utils/common"
require_relative "../../aou-utils/workbench"
require_relative "cloudsqlproxycontext"
require_relative "gcloudcontext"
require_relative "wboptionsparser"
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
              compute.googleapis.com admin.googleapis.com
              cloudbilling.googleapis.com sqladmin.googleapis.com sql-component.googleapis.com
              clouderrorreporting.googleapis.com bigquery-json.googleapis.com}

def ensure_docker(cmd_name, args)
  unless Workbench::in_docker?
    exec *(%W{docker-compose run --rm scripts ./project.rb #{cmd_name}} + args)
  end
end

class Options < OpenStruct
end

# Creates a default command-line argument parser.
# command_name: For help text.
def create_parser(command_name)
  OptionParser.new do |parser|
    parser.banner = "Usage: ./project.rb #{command_name} [options]"
    parser
  end
end

def read_db_vars_v2(gcc)
  Workbench::assert_in_docker
  vars = Common.new.capture_stdout(%W{
    gsutil cat gs://#{gcc.project}-credentials/vars.env
  })
  if vars.empty?
    Common.new.error "Failed to read gs://#{gcc.project}-credentials/vars.env"
    exit 1
  end
  Workbench::read_vars(Common.new.capture_stdout(%W{
    gsutil cat gs://#{gcc.project}-credentials/vars.env
  }))
end

def dev_up(*args)
  common = Common.new

  account = get_auth_login_account()
  if account == nil
    raise("Please run 'gcloud auth login' before starting the server.")
  end

  at_exit { common.run_inline %W{docker-compose down} }
  common.status "Starting database..."
  common.run_inline %W{docker-compose up -d db}
  common.status "Running database migrations..."
  common.run_inline %W{docker-compose run db-migration}
  common.run_inline %W{docker-compose run db-cdr-migration}
  common.run_inline %W{docker-compose run db-public-migration}
  common.run_inline %W{docker-compose run db-data-migration}

  common.status "Updating configuration..."
  common.run_inline %W{
    docker-compose run update-config
    -Pconfig_file=../config/config_local.json
  }
  run_api()
end

Common.register_command({
  :invocation => "dev-up",
  :description => "Brings up the development environment, including db migrations and config " \
     "update. (You can use run-api instead if database and config are up-to-date.)",
  :fn => lambda { |*args| dev_up(*args) }
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

def run_public_api_and_db()
  common = Common.new
  common.status "Starting database..."
  common.run_inline %W{docker-compose up -d db}
  common.status "Starting public API."
  common.run_inline_swallowing_interrupt %W{docker-compose up public-api}
end

Common.register_command({
  :invocation => "run-public-api",
  :description => "Runs the public api server (assumes database is up-to-date.)",
  :fn => lambda { |*args| run_public_api_and_db() }
})


def clean()
  common = Common.new
  common.run_inline %W{docker-compose run --rm api ./gradlew clean}
end

Common.register_command({
  :invocation => "clean",
  :description => "Runs gradle clean. Occasionally necessary before generating code from Swagger.",
  :fn => lambda { |*args| clean(*args) }
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
  :fn => lambda { |*args| run_api_and_db() }
})


def validate_swagger(cmd_name, args)
  ensure_docker cmd_name, args
  Common.new.run_inline %W{gradle validateSwagger} + args
end

Common.register_command({
  :invocation => "validate-swagger",
  :description => "Validate swagger definition files",
  :fn => lambda { |*args| validate_swagger("validate-swagger", args) }
})


def run_api_tests(cmd_name, args)
  ensure_docker cmd_name, args
  Common.new.run_inline %W{gradle test} + args
end

Common.register_command({
  :invocation => "test-api",
  :description => "Runs API tests. To run a single test, add (for example) " \
      "--tests org.pmiops.workbench.interceptors.AuthInterceptorTest",
  :fn => lambda { |*args| run_api_tests("test-api", args) }
})


def run_public_api_tests(cmd_name, args)
  ensure_docker cmd_name, args
  Dir.chdir('../public-api') do
    Common.new.run_inline %W{gradle test} + args
  end
end

Common.register_command({
  :invocation => "test-public-api",
  :description => "Runs public API tests. To run a single test, add (for example) " \
      "--tests org.pmiops.workbench.cdr.dao.AchillesAnalysisDaoTest",
  :fn => lambda { |*args| run_public_api_tests("test-public-api", args) }
})


def run_all_tests(cmd_name, args)
  run_api_tests(cmd_name, args)
  run_public_api_tests(cmd_name, args)
end

Common.register_command({
  :invocation => "test",
  :description => "Runs all tests (api and public-api). To run a single test, add (for example) " \
      "--tests org.pmiops.workbench.interceptors.AuthInterceptorTest",
  :fn => lambda { |*args| run_all_tests("test", args) }
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
  :fn => lambda { |*args| run_integration_tests("integration", *args) }
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
  :fn => lambda { |*args| run_bigquery_tests("bigquerytest", *args) }
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
  :fn => lambda { |*args| run_gradle("gradle", args) }
})


def connect_to_db(*args)
  common = Common.new

  cmd = "MYSQL_PWD=root-notasecret mysql --database=workbench"
  common.run_inline %W{docker-compose exec db sh -c #{cmd}}
end

Common.register_command({
  :invocation => "connect-to-db",
  :description => "Connect to the running database via mysql.",
  :fn => lambda { |*args| connect_to_db(*args) }
})


def docker_clean(*args)
  common = Common.new

  docker_images = `docker ps -aq`.gsub(/\s+/, " ")
  if !docker_images.empty?
    common.run_inline("docker rm -f #{docker_images}")
  end
  common.run_inline %W{docker-compose down --volumes}
end

Common.register_command({
  :invocation => "docker-clean",
  :description => \
    "Removes docker containers and volumes, allowing the next `dev-up` to" \
    " start from scratch (e.g., the database will be re-created). Includes ALL" \
    " docker images, not just for the API.",
  :fn => lambda { |*args| docker_clean(*args) }
})

def rebuild_image(*args)
  common = Common.new

  common.run_inline %W{docker-compose build}
end

Common.register_command({
  :invocation => "rebuild-image",
  :description => "Re-builds the dev docker image (necessary when Dockerfile is updated).",
  :fn => lambda { |*args| rebuild_image(*args) }
})


def get_service_account_creds_file(project, account, creds_file_path)
  common = Common.new
  service_account = "#{project}@appspot.gserviceaccount.com"
  common.run_inline %W{gcloud iam service-accounts keys create #{creds_file_path}
    --iam-account=#{service_account} --project=#{project} --account=#{account}}
end

def delete_service_accounts_creds(project, account, creds_file_path)
  tmp_private_key = `grep private_key_id #{creds_file_path} | cut -d\\\" -f4`.strip()
  service_account ="#{project}@appspot.gserviceaccount.com"
  common = Common.new
  common.run_inline %W{gcloud iam service-accounts keys delete #{tmp_private_key} -q
     --iam-account=#{service_account} --project=#{project} --account=#{account}}
  common.run_inline %W{rm #{creds_file_path}}
end

def activate_service_account(creds_file)
  common = Common.new
  common.run_inline %W{gcloud auth activate-service-account --key-file #{creds_file}}
end

def copy_file_to_gcs(source_path, bucket, filename)
  common = Common.new
  common.run_inline %W{gsutil cp #{source_path} gs://#{bucket}/#{filename}}
end

def get_file_from_gcs(bucket, filename, target_path)
  common = Common.new
  common.run_inline %W{gsutil cp gs://#{bucket}/#{filename} #{target_path}}
end

# Downloads database credentials from GCS and parses them into ENV.
def read_db_vars(creds_file, project)
  db_creds_file = Tempfile.new("#{project}-vars.env")
  begin
    activate_service_account(creds_file)
    get_file_from_gcs("#{project}-credentials", "vars.env", db_creds_file.path)
    ENV.update(Workbench::read_vars_file(db_creds_file.path))
  ensure
    db_creds_file.unlink
  end
  ENV["DB_PORT"] = "3307"
end

def run_cloud_sql_proxy(project, creds_file)
  common = Common.new
  if !File.file?("cloud_sql_proxy")
    op_sys = "linux"
    if RUBY_PLATFORM.downcase.include? "darwin"
      op_sys = "darwin"
    end
    common.run_inline %W{wget https://dl.google.com/cloudsql/cloud_sql_proxy.#{op_sys}.amd64 -O
      cloud_sql_proxy}
    common.run_inline %W{chmod +x cloud_sql_proxy}
  end
  cloud_sql_proxy_cmd = %W{./cloud_sql_proxy
      -instances #{project}:us-central1:workbenchmaindb=tcp:3307
      -credential_file=#{creds_file} &}
  puts "Running Cloud SQL Proxy with #{cloud_sql_proxy_cmd}. Note stdout/err not shown."
  pid = spawn(*cloud_sql_proxy_cmd)
  sleep 3.0  # Wait for the proxy to become active.
  puts "Cloud SQL Proxy running (PID #{pid})."
  return pid
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

def register_service_account(cmd_name, *args)
  op = WbOptionsParser.new(cmd_name, args)
  op.add_option(
        "--project [project]",
        lambda {|opts, v| opts.project = v},
        "Project to register the service account for"
  )
  op.parse.validate
  ServiceAccountContext.new(op.opts.project).run do
    Dir.chdir("../firecloud-tools") do
      common = Common.new
      common.run_inline %W{./run.sh scripts/register_service_account/register_service_account.py
           -j #{ENV["GOOGLE_APPLICATION_CREDENTIALS"]} -e all-of-us-research-tools@googlegroups.com}
    end
  end
end

Common.register_command({
  :invocation => "register-service-account",
  :description => "Registers a service account with Firecloud; do this once per account we use.",
  :fn => lambda { |*args| register_service_account("register-service-account", *args) }
})


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
        to_redact=pw)
  end
end

Common.register_command({
  :invocation => "drop-cloud-db",
  :description => "Drops the Cloud SQL database for the specified project",
  :fn => lambda { |*args| drop_cloud_db("drop-cloud-db", *args) }
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
        to_redact=pw)
  end
end

Common.register_command({
  :invocation => "drop-cloud-cdr",
  :description => "Drops the cdr schema of Cloud SQL database for the specified project",
  :fn => lambda { |*args| drop_cloud_cdr("drop-cloud-cdr", *args) }
})


def run_local_all_migrations(*args)
  common = Common.new

  common.run_inline %W{docker-compose run db-migration}
  common.run_inline %W{docker-compose run db-cdr-migration}
  common.run_inline %W{docker-compose run db-public-migration}
  common.run_inline %W{docker-compose run db-cdr-data-migration}
  common.run_inline %W{docker-compose run db-data-migration}
end

Common.register_command({
  :invocation => "run-local-all-migrations",
  :description => "Runs local data/schema migrations for cdr/workbench schemas.",
  :fn => lambda { |*args| run_local_all_migrations(*args) }
})


def run_local_data_migrations(*args)
  common = Common.new

  common.run_inline %W{docker-compose run db-cdr-data-migration}
  common.run_inline %W{docker-compose run db-data-migration}
end

Common.register_command({
  :invocation => "run-local-data-migrations",
  :description => "Runs local data migrations for cdr/workbench schemas.",
  :fn => lambda { |*args| run_local_data_migrations(*args) }
})


def run_local_bigdata_migrations(*args)
  common = Common.new
  common.run_inline %W{docker-compose run db-cdr-bigdata-migration}
end

Common.register_command({
  :invocation => "run-local-bigdata-migrations",
  :description => "Runs big data migrations for cdr schemas.",
  :fn => lambda { |*args| run_local_bigdata_migrations(*args) }
})


def generate_cdr_counts(*args)
  common = Common.new
  common.run_inline %W{docker-compose run db-generate-cdr-counts} + args
end

Common.register_command({
  :invocation => "generate-cdr-counts",
  :description => "generate-cdr-counts --bq-project <PROJECT> --bq-dataset <DATASET> --workbench-project <PROJECT> \
--public-project <PROJECT> --cdr-version=<''|YYYYMMDD> --bucket <BUCKET>
Generates databases in bigquery with data from a cdr that will be imported to mysql/cloudsql to be used by workbench and databrowser.",
  :fn => lambda { |*args| generate_cdr_counts(*args) }
})


def generate_local_cdr_db(*args)
  common = Common.new
  common.run_inline %W{docker-compose run db-generate-local-cdr-db} + args
end

Common.register_command({
  :invocation => "generate-local-cdr-db",
  :description => "generate-cloudsql-cdr --cdr-version <''|YYYYMMDD> --cdr-db-prefix <cdr|public> --bucket <BUCKET>
Creates and populates local mysql database from data in bucket made by generate-cdr-counts.",
  :fn => lambda { |*args| generate_local_cdr_db(*args) }
})


def generate_local_count_dbs(*args)
  common = Common.new
  common.run_inline %W{docker-compose run db-generate-local-count-dbs} + args
end

Common.register_command({
  :invocation => "generate-local-count-dbs",
  :description => "generate-local-count-dbs.sh --cdr-version <''|YYYYMMDD> --bucket <BUCKET>
Creates and populates local mysql databases cdr<VERSION> and public<VERSION> from data in bucket made by generate-cdr-counts.",
  :fn => lambda { |*args| generate_local_count_dbs(*args) }
})


def mysqldump_db(*args)
  common = Common.new
  common.run_inline %W{docker-compose run db-mysqldump-db} + args
end


Common.register_command({
  :invocation => "mysqldump-db",
  :description => "mysqldump-db db-name <LOCALDB> --bucket <BUCKET>
Dumps the local mysql db and uploads the .sql file to bucket",
  :fn => lambda { |*args| mysqldump_db(*args) }
})

def cloudsql_import(cmd_name, *args)
  op = WbOptionsParser.new(cmd_name, args)
  op.add_option(
      "--project [project]",
      lambda {|opts, v| opts.project = v},
      "Project to import the database into (e.g. all-of-us-rw-stable)"
  )
  op.add_option(
    "--instance [instance]",
    lambda {|opts, v| opts.instance = v},
    "Database instance to import into (e.g. workbenchmaindb)"
  )
  op.add_option(
    "--sql-dump-file [filename]",
    lambda {|opts, v| opts.file = v},
    "File name of the SQL dump to import"
  )
  op.add_option(
    "--bucket [bucket]",
    lambda {|opts, v| opts.bucket = v},
    "Name of the GCS bucket containing the SQL dump"
  )
  op.parse.validate
  ServiceAccountContext.new(op.opts.project).run do
    common = Common.new
    common.run_inline %W{docker-compose run db-cloudsql-import --instance #{op.opts.instance}
        --sql-dump-file #{op.opts.file} --bucket #{op.opts.bucket} --project #{op.opts.project}}
  end
end
Common.register_command({
                            :invocation => "cloudsql-import",
                            :description => "cloudsql-import --account <SERVICE_ACCOUNT> --project <PROJECT> --instance <CLOUDSQL_INSTANCE> --sql-dump-file <FILE.sql> --bucket <BUCKET>
Imports .sql file to cloudsql instance",
                            :fn => lambda { |*args| cloudsql_import("cloudsql-import", *args) }
                        })

def run_drop_cdr_db(*args)
  common = Common.new
  common.run_inline %W{docker-compose run drop-cdr-db}
end

Common.register_command({
  :invocation => "run-drop-cdr-db",
  :description => "Drops the cdr schema of SQL database for the specified project.",
  :fn => lambda { |*args| run_drop_cdr_db(*args) }
})


Common.register_command({
  :invocation => "run-cloud-data-migrations",
  :description => "Runs data migrations in the cdr and workbench schemas on the Cloud SQL database for the specified project.",
  :fn => lambda { |*args| run_cloud_data_migrations("run-cloud-data-migrations", args) }
})

def write_db_creds_file(project, cdr_db_name, public_db_name, root_password, workbench_password, public_password)
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
      db_creds_file.puts "PUBLIC_DB_NAME=#{public_db_name}"
      db_creds_file.puts "CLOUD_SQL_INSTANCE=#{instance_name}"
      db_creds_file.puts "LIQUIBASE_DB_USER=liquibase"
      db_creds_file.puts "LIQUIBASE_DB_PASSWORD=#{workbench_password}"
      db_creds_file.puts "MYSQL_ROOT_PASSWORD=#{root_password}"
      db_creds_file.puts "WORKBENCH_DB_USER=workbench"
      db_creds_file.puts "WORKBENCH_DB_PASSWORD=#{workbench_password}"
      db_creds_file.puts "PUBLIC_DB_CONNECTION_STRING=jdbc:google:mysql://#{instance_name}/#{public_db_name}?rewriteBatchedStatements=true"
      db_creds_file.puts "PUBLIC_DB_USER=public"
      db_creds_file.puts "PUBLIC_DB_PASSWORD=#{public_password}"
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
    lambda {|opts, v| opts.project = v},
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
    :fn => lambda { |*args| create_auth_domain("create-auth-domain", args) }
})

def update_user_registered_status(cmd_name, args)
  common = Common.new
  op = WbOptionsParser.new(cmd_name, args)
  op.add_option(
    "--project [project]",
    lambda {|opts, v| opts.project = v},
    "Project to update registered status for"
  )
  op.add_option(
    "--action [action]",
    lambda {|opts, v| opts.action = v},
    "Action to perform: add/remove."
  )
  op.add_option(
    "--account [account]",
    lambda {|opts, v| opts.account = v},
    "Account to perform update registered status as."
  )
  op.add_option(
    "--user [user]",
    lambda {|opts, v| opts.user = v},
    "User to grant or revoke registered access from."
  )
  action = op.opts.action
  account = op.opts.account
  user = op.opts.user
  op.parse.validate

  common.run_inline %W{gcloud auth login}
  token = common.capture_stdout %W{gcloud auth print-access-token}
  token = token.chomp
  common.run_inline %W{gcloud config set account #{op.opts.account}}
  header = "Authorization: Bearer #{token}"
  content_type = "Content-type: application/json"
  payload = "{\"email\": \"#{op.opts.user}\"}"
  domain_name = get_auth_domain(op.opts.project)
  if op.opts.action == "add"
    common.run_inline %W{curl -H #{header} -H #{content_type}
      -d #{payload} https://api-dot-#{op.opts.project}.appspot.com/v1/auth-domain/#{domain_name}/users}
  end

  if op.opts.action == "remove"
    common.run_inline %W{curl -X DELETE -H #{header} -H #{content_type}
      -d #{payload} https://api-dot-#{op.opts.project}.appspot.com/v1/auth-domain/#{domain_name}/users}
  end
end

Common.register_command({
  :invocation => "update-user-registered-status",
  :description => "Adds or removes a specified user from the registered access domain.\n" \
                  "Accepts three flags: --action [add/remove], --account [admin email], and --user [target user email]",
  :fn => lambda { |*args| update_user_registered_status("update_user_registered_status", args) }
})

def set_authority(cmd_name, *args)
  ensure_docker cmd_name, args
  op = WbOptionsParser.new(cmd_name, args)
  op.opts.remove = false
  op.opts.dry_run = false
  op.add_option(
       "--email [EMAIL,...]",
       lambda {|opts, v| opts.email = v},
       "Comma-separated list of user accounts to change. Required.")
   op.add_option(
       "--authority [AUTHORITY,...]",
       lambda {|opts, v| opts.authority = v},
       "Comma-separated list of user authorities to add or remove for the users. ")
   op.add_option(
       "--remove",
       lambda {|opts, v| opts.remove = "true"},
       "Remove authorities (rather than adding them.)")
   op.add_option(
       "--dry_run",
       lambda {|opts, v| opts.dry_run = "true"},
       "Make no changes.")
   op.add_validator lambda {|opts| raise ArgumentError unless opts.email and opts.authority}
   gcc = GcloudContextV2.new(op)
   op.parse.validate
   gcc.validate

   with_cloud_proxy_and_db(gcc) do |ctx|
     Dir.chdir("tools") do
       common = Common.new
       common.run_inline %W{
         gradle --info setAuthority
        -PappArgs=['#{op.opts.email}','#{op.opts.authority}',#{op.opts.remove},#{op.opts.dry_run}]}
     end
   end
end

Common.register_command({
  :invocation => "set-authority",
  :description => "Set user authorities (permissions). See set-authority --help.",
  :fn => lambda { |*args| set_authority("set-authority", *args) }
})

def get_test_service_account(cmd_name, *args)
  ensure_docker cmd_name, args
  op = WbOptionsParser.new(cmd_name, args)
  gcc = GcloudContextV2.new(op)
  op.parse.validate
  gcc.validate
  ServiceAccountContext.new(gcc.project).run do
    print "Service account key is now in sa-key.json"
  end
end

Common.register_command({
  :invocation => "get-service-creds",
  :description => "Copies sa-key.json locally (for use when running tests from an IDE, etc).",
  :fn => lambda { |*args| get_rest_service_account("get-service-creds", *args)}
})

def connect_to_cloud_db(cmd_name, *args)
  ensure_docker cmd_name, args
  common = Common.new
  op = WbOptionsParser.new(cmd_name, args)
  op.add_option(
    "--root",
    lambda {|opts, v| opts.root = true },
    "Connect as root")
  gcc = GcloudContextV2.new(op)
  op.parse.validate
  gcc.validate
  env = read_db_vars_v2(gcc)
  CloudSqlProxyContext.new(gcc.project).run do
    password = op.opts.root ? env["MYSQL_ROOT_PASSWORD"] : env["WORKBENCH_DB_PASSWORD"]
    user = op.opts.root ? "root" : env["WORKBENCH_DB_USER"]
    common.run_inline %W{
      mysql --host=127.0.0.1 --port=3307 --user=#{user}
      --database=#{env["DB_NAME"]} --password=#{password}},
      redact=password
  end
end

Common.register_command({
  :invocation => "connect-to-cloud-db",
  :description => "Connect to a Cloud SQL database via mysql.",
  :fn => lambda { |*args| connect_to_cloud_db("connect-to-cloud-db", *args) }
})


def deploy(cmd_name, args, with_cron, with_gsuite_admin)
  common = Common.new
  op = WbOptionsParser.new(cmd_name, args)
  op.add_option(
    "--version [version]",
    lambda {|opts, v| opts.version = v},
    "Version to deploy (e.g. your-username-test)"
  )
  op.add_option(
    "--promote",
    lambda {|opts, v| opts.promote = true},
    "Promote this deploy to make it available at the root URL"
  )
  op.add_option(
    "--no-promote",
    lambda {|opts, v| opts.promote = false},
    "Do not promote this deploy to make it available at the root URL"
  )
  op.add_option(
    "--quiet",
    lambda {|opts, v| opts.quiet = true},
    "Don't display a confirmation prompt when deploying"
  )
  gcc = GcloudContextV2.new(op)
  op.parse.validate
  gcc.validate
  env = read_db_vars_v2(gcc)
  ENV.update(env)

  # Clear out generated files, which may be out of date; they will be regenerated by appengineStage.
  common.run_inline %W{rm -f src/generated}
  if with_gsuite_admin
    common.run_inline %W{rm -f #{GSUITE_ADMIN_KEY_PATH}}
    # TODO: generate new key here
    get_gsuite_admin_key(gcc.project)
  end

  common.run_inline %W{gradle :appengineStage}
  promote = op.opts.promote.nil? ? (op.opts.version ? "--no-promote" : "--promote") \
    : (op.opts.promote ? "--promote" : "--no-promote")
  quiet = op.opts.quiet ? " --quiet" : ""

  common.run_inline %W{
    gcloud app deploy
      build/staged-app/app.yaml
  } + (with_cron ? %W{build/staged-app/WEB-INF/appengine-generated/cron.yaml} : []) +
    %W{--project #{gcc.project} #{promote}} +
    (op.opts.quiet ? %W{--quiet} : []) +
    (op.opts.version ? %W{--version #{op.opts.version}} : [])
end

def deploy_api(cmd_name, args)
  ensure_docker cmd_name, args
  common = Common.new
  common.status "Deploying api..."
  deploy(cmd_name, args, with_cron=true, with_gsuite_admin=true)
end

Common.register_command({
  :invocation => "deploy-api",
  :description => "Deploys the API server to the specified cloud project.",
  :fn => lambda { |*args| deploy_api("deploy-api", args) }
})


def deploy_public_api(cmd_name, args)
  ensure_docker cmd_name, args
  common = Common.new
  common.status "Deploying public-api..."
  Dir.chdir('../public-api') do
    deploy(cmd_name, args, with_cron=false, with_gsuite_admin=false)
  end
end

Common.register_command({
  :invocation => "deploy-public-api",
  :description => "Deploys the public API server to the specified cloud project.",
  :fn => lambda { |*args| deploy_public_api("deploy-public-api", args) }
})


def create_workbench_db()
  run_with_redirects(
    "cat db/create_db.sql | envsubst | " \
    "mysql -u \"root\" -p\"#{ENV["MYSQL_ROOT_PASSWORD"]}\" --host 127.0.0.1 --port 3307",
    to_redact=ENV["MYSQL_ROOT_PASSWORD"]
  )
end

def migrate_database()
  common = Common.new
  common.status "Migrating main database..."
  Dir.chdir("db") do
    common.run_inline(%W{gradle --info update -PrunList=main})
  end
end

def migrate_workbench_data()
  common = Common.new
  common.status "Migrating workbench data..."
  Dir.chdir("db") do
    common.run_inline(%W{gradle --info update -PrunList=data -Pcontexts=cloud})
  end
end

def get_config(project)
  configs = {
    TEST_PROJECT => "config_test.json",
    "all-of-us-rw-stable" => "config_stable.json",
  }
  return configs[project]
end

def get_auth_domain(project)
  config_json = get_config(project)
  return JSON.parse(File.read("config/#{config_json}"))["firecloud"]["registeredDomainName"]
end

def load_config(project)
  config_json = get_config(project)
  unless config_json
    raise("unknown project #{project}, expected one of #{configs.keys}")
  end

  common = Common.new
  common.status "Loading #{config_json} into database..."
  Dir.chdir("tools") do
    common.run_inline %W{
      gradle --info loadConfig
      -Pconfig_file=../config/#{config_json}
    }
  end
end

def with_cloud_proxy_and_db(gcc)
  ENV.update(read_db_vars_v2(gcc))
  ENV["DB_PORT"] = "3307" # TODO(dmohs): Use MYSQL_TCP_PORT to be consistent with mysql CLI.
  common = Common.new
  CloudSqlProxyContext.new(gcc.project).run do
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

def circle_deploy(cmd_name, args)
  # See https://circleci.com/docs/1.0/environment-variables/#build-details
  common = Common.new
  common.status "circle_deploy with branch='#{ENV.fetch("CIRCLE_BRANCH", "")}'" +
  " and tag='#{ENV.fetch("CIRCLE_TAG", "")}'"
  if ENV.has_key?("CIRCLE_BRANCH") and ENV.has_key?("CIRCLE_TAG")
    raise("expected exactly one of CIRCLE_BRANCH and CIRCLE_TAG env vars to be set")
  end
  is_master = ENV.fetch("CIRCLE_BRANCH", "") == "master"
  if !is_master and !ENV.has_key?("CIRCLE_TAG")
    common.status "not master or a git tag, nothing to deploy"
    return
  end

  unless Workbench::in_docker?
    exec *(%W{docker run --rm -v #{File.expand_path("..")}:/w -w /w/api
      allofustest/workbench:buildimage-0.0.9
      ./project.rb #{cmd_name}} + args)
  end

  common = Common.new
  unless File.exist? "circle-sa-key.json"
    common.error "Missing service account credentials file circle-sa-key.json."
    exit 1
  end

  if is_master
    common.status "Running database migrations..."
    with_cloud_proxy_and_db_env(cmd_name, args) do |ctx|
      migrate_database
      load_config(ctx.project)
    end
  end

  promote = ""
  version = ""
  if is_master
    # Note that --promote will generally be a no-op, as we expect
    # circle-ci-test to always be serving 100% traffic. Pushing to an existing
    # live version will immediately make those changes live. In the event that
    # someone mistakenly pushes a different version manually, this --promote
    # will restore us to the expected circle-ci-test version on the next commit.
    promote = "--promote"
    version = "circle-ci-test"
  elsif ENV.has_key?("CIRCLE_TAG")
    promote = "--no-promote"
    version = ENV["CIRCLE_TAG"]
  end

  deploy_api(cmd_name, args + %W{--quiet --version #{version} #{promote}})
  deploy_public_api(cmd_name, args + %W{--quiet --version #{version} #{promote}})
end

Common.register_command({
  :invocation => "circle-deploy",
  :description => "Deploys the API server from within the Circle CI envronment.",
  :fn => lambda { |*args| circle_deploy("circle-deploy", args) }
})


def run_cloud_migrations(cmd_name, args)
  ensure_docker cmd_name, args
  with_cloud_proxy_and_db_env(cmd_name, args) { migrate_database }
end

Common.register_command({
  :invocation => "run-cloud-migrations",
  :description => "Runs database migrations on the Cloud SQL database for the specified project.",
  :fn => lambda { |*args| run_cloud_migrations("run-cloud-migrations", args) }
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
  :fn => lambda { |*args| update_cloud_config("update-cloud-config", args) }
})

def docker_run(cmd_name, args)
  Common.new.run_inline %W{docker-compose run --rm scripts} + args
end

Common.register_command({
  :invocation => "docker-run",
  :description => "Runs the specified command in a docker container.",
  :fn => lambda { |*args| docker_run("docker-run", args) }
})

def print_scoped_access_token(cmd_name, args)
  ensure_docker cmd_name, args
  op = WbOptionsParser.new(cmd_name, args)
  op.add_typed_option(
    "--scopes s1,s2,s3",
    Array,
    lambda {|opts, v| opts.scopes = v},
    "Action to perform: add/remove."
  )
  gcc = GcloudContextV2.new(op)
  op.parse.validate
  gcc.validate
  ServiceAccountContext.new(gcc.project).run do
    scopes = %W{profile email} + op.opts.scopes

    require "googleauth"
    creds = Google::Auth::ServiceAccountCredentials.make_creds(
      json_key_io: File.open(GcloudContextV2::SA_KEY_PATH),
      scope: scopes
    )

    token_data = creds.fetch_access_token!
    puts "\n#{token_data["access_token"]}"
  end
end

Common.register_command({
  :invocation => "print-scoped-sa-access-token",
  :description => "Prints access token for the service account that has been scoped for API access.",
  :fn => lambda { |*args| print_scoped_access_token("print-scoped-sa-access-token", args) }
})

def create_project_resources(gcc)
  common = Common.new
  common.status "Enabling APIs..."
  for service in SERVICES
    common.run_inline("gcloud service-management enable #{service} --project #{gcc.project}")
  end
  common.status "Creating GCS bucket to store credentials..."
  common.run_inline %W{gsutil mb -p #{gcc.project} -c regional -l us-central1 gs://#{gcc.project}-credentials/}
  common.status "Creating Cloud SQL instances..."
  common.run_inline %W{gcloud sql instances create #{INSTANCE_NAME} --tier=db-n1-standard-2
                       --activation-policy=ALWAYS --backup-start-time 00:00
                       --failover-replica-name #{FAILOVER_INSTANCE_NAME} --enable-bin-log
                       --database-version MYSQL_5_7 --project #{gcc.project} --storage-auto-increase --async}
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

def setup_project_data(gcc, cdr_db_name, public_db_name,
                       root_password, workbench_password, public_password, args)
  common = Common.new
  # This changes database connection information; don't call this while the server is running!
  common.status "Writing DB credentials file..."
  write_db_creds_file(gcc.project, cdr_db_name, public_db_name, root_password, workbench_password,
                      public_password)
  common.status "Setting root password..."
  run_with_redirects("gcloud sql users set-password root % --project #{gcc.project} " +
                     "--instance #{INSTANCE_NAME} --password #{root_password}",
                     to_redact=root_password)
  # Don't delete the credentials created here; they will be stored in GCS and reused during
  # deployment, etc.
  CloudSqlProxyContext.new(gcc.project).run do
    with_cloud_proxy_and_db_env("setup-db-users", args) do |ctx|
      common.status "Copying service account key to GCS..."
      gsuite_admin_creds_file = Tempfile.new("gsuite-admin-sa.json")
      common.run_inline %W{gcloud iam service-accounts keys create #{gsuite_admin_creds_file}
          --iam-account=gsuite-admin@#{gcc.project}.iam.gserviceaccount.com --project=#{gcc.project}}
      common.run_inline %W{gsutil cp #{gsuite_admin_creds_file} gs://#{gcc.project}-credentials/gsuite-admin-sa.json}

      common.status "Setting up databases and users..."
      create_workbench_db

      common.status "Running schema migrations..."
      migrate_database
      # This will insert a CDR version row pointing at the CDR and public DB.
      migrate_workbench_data
    end
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
    lambda {|opts, v| opts.cdr_db_name = v},
    "Name of the default CDR db to use; required. (example: cdr20180206) This will subsequently " +
    "be created by cloudsql-import."
  )
  op.add_option(
    "--public-db-name [PUBLIC_DB]",
    lambda {|opts, v| opts.public_db_name = v},
    "Name of the public db to use for the data browser. (example: public20180206) This will " +
    "subsequently be created by cloudsql-import."
  )
  op.add_validator lambda {|opts| raise ArgumentError unless opts.cdr_db_name}
  op.add_validator lambda {|opts| raise ArgumentError unless opts.public_db_name}
  gcc = GcloudContextV2.new(op)

  op.parse.validate
  gcc.validate

  create_project_resources(gcc)
  setup_project_data(gcc, op.opts.cdr_db_name, op.opts.public_db_name,
                     random_password(), random_password(), random_password(), args)
end

Common.register_command({
  :invocation => "setup-cloud-project",
  :description => "Initializes resources within a cloud project that has already been created",
  :fn => lambda { |*args| setup_cloud_project("setup-cloud-project", *args) }
})
