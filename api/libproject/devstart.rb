# Calls to common.run_inline in this file may use a quoted string purposefully
# to cause system() or spawn() to run the command in a shell. Calls with arrays
# are not run in a shell, which can break usage of the CloudSQL proxy.

require_relative "../../libproject/utils/common"
require_relative "../../libproject/workbench"
require "io/console"
require "json"
require "optparse"
require "ostruct"
require "tempfile"
require "fileutils"

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

  common.status "Updating configuration..."
  common.run_inline %W{docker-compose run update-config}

  run_api(account)
end

def run_api(account)
  common = Common.new
  # TODO(dmohs): This can be simplified now that we are using a shared service account.
  do_run_with_creds("all-of-us-workbench-test", account, nil) do |creds_file|
    common.status "Starting API. This can take a while. Thoughts on reducing development cycle time"
    common.status "are here:"
    common.status "  https://github.com/all-of-us/workbench/blob/master/api/doc/2017/dev-cycle.md"
    at_exit { common.run_inline %W{docker-compose down} }
    common.run_inline_swallowing_interrupt %W{docker-compose up api}
  end
end

def clean()
  common = Common.new
  common.run_inline %W{docker-compose run --rm api ./gradlew clean}
end

def run_api_and_db(*args)
  common = Common.new
  account = get_auth_login_account()
  if account == nil
    raise("Please run 'gcloud auth login' before starting the server.")
  end
  common.status "Starting database..."
  common.run_inline %W{docker-compose up -d db}
  run_api(account)
end

def run_tests(*args)
  common = Common.new

  common.run_inline %W{docker-compose run --rm api ./gradlew test} + args
end

def run_integration_tests(*args)
  common = Common.new

  account = get_auth_login_account()
  do_run_with_creds("all-of-us-workbench-test", account, nil) do |creds_file|
    common.run_inline %W{docker-compose run --rm api ./gradlew integration} + args
  end
end

def run_bigquery_tests(*args)
  common = Common.new

  account = get_auth_login_account()
  do_run_with_creds("all-of-us-workbench-test", account, nil) do |creds_file|
    common.run_inline %W{docker-compose run --rm api ./gradlew bigquerytest} + args
  end
end

def run_gradle(*args)
  common = Common.new
  common.run_inline %W{docker-compose run --rm api ./gradlew} + args
end

def connect_to_db(*args)
  common = Common.new

  cmd = "MYSQL_PWD=root-notasecret mysql --database=workbench"
  common.run_inline %W{docker-compose exec db sh -c #{cmd}}
end

def docker_clean(*args)
  common = Common.new

  docker_images = `docker ps -aq`.gsub(/\s+/, " ")
  if !docker_images.empty?
    common.run_inline("docker rm -f #{docker_images}")
  end
  common.run_inline %W{docker-compose down --volumes}
end

def rebuild_image(*args)
  common = Common.new

  common.run_inline %W{docker-compose build}
end

def get_service_account_creds_file(project, account, creds_file)
  common = Common.new
  service_account = "#{project}@appspot.gserviceaccount.com"
  common.run_inline %W{gcloud iam service-accounts keys create #{creds_file.path}
    --iam-account=#{service_account} --project=#{project} --account=#{account}}
end

def delete_service_accounts_creds(project, account, creds_file)
  tmp_private_key = `grep private_key_id #{creds_file.path} | cut -d\\\" -f4`.strip()
  service_account ="#{project}@appspot.gserviceaccount.com"
  common = Common.new
  common.run_inline %W{gcloud iam service-accounts keys delete #{tmp_private_key} -q
     --iam-account=#{service_account} --project=#{project} --account=#{account}}
  creds_file.unlink
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
    db_creds_file.open
    db_creds_file.each_line do |line|
      line = line.strip()
      if !line.empty?
        parts = line.split("=")
        ENV[parts[0]] = parts[1]
      end
    end
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

# Fetches a credentials file. Passes the path of the credentials to a block.
# For all-of-us-workbench-test only, it leaves the (lazy-fetched) creds on disk;
# for any other project, it cleans them up after the block is run.
def do_run_with_creds(project, account, creds_file)
  if creds_file == nil
    service_account_creds_file = Tempfile.new("#{project}-creds.json")
    if project == "all-of-us-workbench-test"
      creds_filename = "src/main/webapp/WEB-INF/sa-key.json"
      # For test, use a locally stored key file copied from GCS (which we leave hanging
      # around.)
      if !File.file?(creds_filename)
        # Create a temporary creds file for accessing GCS.
        get_service_account_creds_file(project, account, service_account_creds_file)
        begin
          activate_service_account(service_account_creds_file.path)
          # Copy the stable creds file from its path in GCS to sa-key.json.
          # Important: we must leave this key file in GCS, and not delete it in Cloud Console,
          # or local development will stop working.
          get_file_from_gcs("all-of-us-workbench-test-credentials",
              "all-of-us-workbench-test-9b5c623a838e.json", creds_filename)
        ensure
          # Delete the temporary creds we created.
          delete_service_accounts_creds(project, account, service_account_creds_file)
        end
      end
      yield(creds_filename)
    else
      # Create a creds file and use it; clean up when done.
      get_service_account_creds_file(project, account, service_account_creds_file)
      begin
        yield(service_account_creds_file.path)
      ensure
        delete_service_accounts_creds(project, account, service_account_creds_file)
      end
    end
  else
    yield(creds_file)
  end
end

def register_service_account(*args)
  GcloudContext.new("register-service-account", args).run do |ctx|
    Dir.chdir("../firecloud-tools") do
      ctx.common.run_inline(
          "./run.sh register_service_account/register_service_account.py" \
          " -j #{cts.opts.creds_file} -o #{ctx.opts.account}")
    end
  end
end

def drop_cloud_db(*args)
  GcloudContext.new("drop-cloud-db", args, true).run do |ctx|
    puts "Dropping database..."
    pw = ENV["MYSQL_ROOT_PASSWORD"]
    run_with_redirects(
        "cat db/drop_db.sql | envsubst | " \
        "mysql -u \"root\" -p\"#{pw}\" --host 127.0.0.1 --port 3307",
        to_redact=pw)
  end
end

def drop_cloud_cdr(*args)
  GcloudContext.new("drop-cloud-cdr", args, true).run do |ctx|
    puts "Dropping cdr database..."
    pw = ENV["MYSQL_ROOT_PASSWORD"]
    run_with_redirects(
        "cat db-cdr/drop_db.sql | envsubst | " \
        "mysql -u \"root\" -p\"#{pw}\" --host 127.0.0.1 --port 3307",
        to_redact=pw)
  end
end

def connect_to_cloud_db(*args)
  GcloudContext.new("connect-to-cloud-db", args, true).run do |ctx|
    pw = ENV["WORKBENCH_DB_PASSWORD"]
    # TODO Switch this to run_inline once Common supports redaction.
    run_with_redirects(
        "mysql -u \"workbench\" -p\"#{pw}\" --host 127.0.0.1 "\
        "--port 3307 --database #{ENV["DB_NAME"]}",
        to_redact=pw)
  end
end

def update_cloud_config(*args)
  GcloudContext.new("update-cloud-config", args, true).run do |ctx|
    Dir.chdir("tools") do
      ctx.common.run_inline %W{#{ctx.gradlew_path} --info loadConfig}
    end
  end
end

def run_cloud_migrations(*args)
  GcloudContext.new("run-cloud-migrations", args, true).run do |ctx|
    puts "Running migrations..."
    puts "Creating database if it does not exist..."
    pw = ENV["MYSQL_ROOT_PASSWORD"]
    run_with_redirects(
        "cat db/create_db.sql | envsubst | " \
        "mysql -u \"root\" -p\"#{pw}\" --host 127.0.0.1 --port 3307",
        to_redact=pw)
    puts "Upgrading database..."
    Dir.chdir("db") do
      ctx.common.run_inline("#{ctx.gradlew_path} --info update")
    end
  end
end

def run_cloud_cdr_migrations(*args)
  GcloudContext.new("run-cloud-cdr-migrations", args, true).run do |ctx|
    puts "Running cdr migrations..."
    puts "Creating cdr database if it does not exist..."
    pw = ENV["MYSQL_ROOT_PASSWORD"]
    run_with_redirects(
        "cat db-cdr/create_db.sql | envsubst | " \
        "mysql -u \"root\" -p\"#{pw}\" --host 127.0.0.1 --port 3307",
        to_redact=pw)
    puts "Upgrading cdr database..."
    Dir.chdir("db-cdr") do
      ctx.common.run_inline("#{ctx.gradlew_path} --info update -PrunList=main")
    end
  end
end

def do_create_db_creds(project, account, creds_file)
  puts "Enter the root DB user password:"
  root_password = STDIN.noecho(&:gets)
  puts "Enter the root DB user password again:"
  root_password_2 = STDIN.noecho(&:gets)
  if root_password != root_password_2
    raise("Root password entries didn't match.")
  end
  puts "Enter the workbench DB user password:"
  workbench_password = STDIN.noecho(&:gets)
  puts "Enter the workbench DB user password again:"
  workbench_password_2 = STDIN.noecho(&:gets)
  if workbench_password != workbench_password_2
    raise("Workbench password entries didn't match.")
  end

  instance_name = "#{project}:us-central1:workbenchmaindb"
  db_creds_file = Tempfile.new("#{project}-vars.env")
  if db_creds_file
    begin
      db_creds_file.puts "DB_CONNECTION_STRING=jdbc:google:mysql://#{instance_name}/workbench"
      db_creds_file.puts "DB_DRIVER=com.mysql.jdbc.GoogleDriver"
      db_creds_file.puts "DB_HOST=127.0.0.1"
      db_creds_file.puts "DB_NAME=workbench"
      db_creds_file.puts "CLOUD_SQL_INSTANCE=#{instance_name}"
      db_creds_file.puts "LIQUIBASE_DB_USER=liquibase"
      db_creds_file.puts "LIQUIBASE_DB_PASSWORD=#{workbench_password}"
      db_creds_file.puts "MYSQL_ROOT_PASSWORD=#{root_password}"
      db_creds_file.puts "WORKBENCH_DB_USER=workbench"
      db_creds_file.puts "WORKBENCH_DB_PASSWORD=#{workbench_password}"
      db_creds_file.close

      activate_service_account(creds_file)
      copy_file_to_gcs(db_creds_file.path, "#{project}-credentials", "vars.env")
    ensure
      db_creds_file.unlink
    end
  else
    raise("Error creating file.")
  end
end

def create_db_creds(*args)
  GcloudContext.new("create-db-creds", args, true).run do |ctx|
    do_create_db_creds(ctx.opts.project, ctx.opts.account, ctx.opts.creds_file)
  end
end

# Run commands with various gcloud setup/teardown: authorization and,
# optionally, a CloudSQL proxy.
class GcloudContext
  attr_reader :common, :gradlew_path, :opts

  def initialize(command_name, args, use_cloudsql_proxy = false)
    @common = Common.new
    @args = args
    @parser = create_parser(command_name)
    # Clients may access options to get default options (project etc)
    # as well as their own custom options.
    @opts = Options.new
    @use_cloudsql_proxy = use_cloudsql_proxy

    @gradlew_path = File.join(Workbench::WORKBENCH_ROOT, "api", "gradlew")
  end

  # Clients may override add_options and validate_options to add flags.
  def add_options
    @parser.on("--project [PROJECT]",
        "Project to create credentials for (e.g. all-of-us-workbench-test)") do |project|
      @opts.project = project
    end
    @parser.on("--account [ACCOUNT]",
         "Account to use when creating credentials (your.name@pmi-ops.org); "\
         "use this or --creds_file") do |account|
      @opts.account = account
    end
    @parser.on("--creds_file [CREDS_FILE]",
         "Path to a file containing credentials; use this or --account.") do |creds_file|
      @opts.creds_file = creds_file
    end
  end

  def validate_options
    if @opts.project == nil || !((@opts.account == nil) ^ (@opts.creds_file == nil))
      puts @parser.help
      exit 1
    end
  end

  # Sets up credentials (and optionally CloudSQL proxy), yields to a provided
  # block (passing the block itself / the GcloudContext), and then tears down.
  def run
    add_options
    @parser.parse @args
    validate_options
    ENV["GOOGLE_APPLICATION_CREDENTIALS"] = @opts.creds_file
    do_run_with_creds(@opts.project, @opts.account, @opts.creds_file) do |creds_file|
      @opts.creds_file = creds_file
      begin
        if @use_cloudsql_proxy
          cloudsql_proxy_pid = run_cloud_sql_proxy(@opts.project, @opts.creds_file)
          read_db_vars(@opts.creds_file, @opts.project)
        end

        yield(self)
      ensure
        if @use_cloudsql_proxy
          puts "Cleaning up CloudSQL proxy (PID #{cloudsql_proxy_pid})."
          Process.kill("HUP", cloudsql_proxy_pid)
        end
      end
    end
  end
end


# Command-line parsing and main "run" implementation for deploy-api.
class DeployApi < GcloudContext
  def add_options
    super
    @parser.on(
        "--version [VERSION]",
        "The name of the version to deploy. Required."
        ) do |version|
      @opts.version = version
    end
    @parser.on(
        "--promote",
        "Use this if you want to promote this version so it receives traffic. By default, it won't."
        ) do |promote|
      @opts.promote = "promote"
    end
    @opts.promote = "no-promote" # default
  end

  def validate_options
    if @opts.project == nil || @opts.account == nil ||@opts.version == nil
      puts @parser.help
      exit 1
    end
  end

  def run
    super do
      # Populate environment variables based on DB credentials
      read_db_vars(@opts.creds_file, @opts.project)
      # This triggers logic in generate_appengine_web_xml.sh to use environment variables set above,
      # rather than reading from vars.env.
      ENV["CIRCLECI"] = "true"
      @common.run_inline %W{#{@gradlew_path} :appengineStage}
      @common.run_inline %W{gcloud app deploy build/staged-app/app.yaml --project #{@opts.project} --account #{@opts.account}
          --version #{@opts.version} --#{@opts.promote}}
    end
  end
end


# Command-line parsing and main "run" implementation for set-authority.
class SetAuthority < GcloudContext
  # Adds command-line flags specific to set-authority.
  def add_options
    super
    @parser.on(
        "--email [EMAIL,...]",
        "Comma-separated list of user accounts to change. Required."
        ) do |email|
      @opts.email = email
    end
    @parser.on(
        "--add_authority [AUTHORITY,...]",
        "Comma-separated list of user authorities to add for the users. " \
        "One of added or removed authorities is required.") do |authority|
      @opts.add_authority = authority
    end
    @parser.on(
        "--rm_authority [AUTHORITY,...]",
        "Comma-separated list of user authorities to remove from the users."
        ) do |authority|
      @opts.rm_authority = authority
    end
    @parser.on("--dry_run", "Make no changes.") do |dry_run|
      @opts.dry_run = "true"
    end
    @opts.dry_run = "false"  # default
  end

  def validate_options
    super
    if @opts.email == nil || (@opts.add_authority == nil && @opts.rm_authority == nil)
      puts @parser.help
      exit 1
    end
  end

  def run
    super do
      Dir.chdir("tools") do
        @common.run_inline %W{
            #{@gradlew_path} --info setAuthority
            -PappArgs=['#{@opts.email}','#{@opts.add_authority}','#{@opts.rm_authority}',#{@opts.dry_run}]}
      end
    end
  end
end

# The test creds are always left in api/sa-key.json. This simply adds validation
# that the command is only run for the test project, and logs the path of
# the file written.
class GetTestServiceAccountCreds < GcloudContext
  def validate_options
    super
    if @opts.project != "all-of-us-workbench-test"
      raise("Only call this with all-of-us-workbench-test")
    end
  end

  def run
    super do
      puts "Creds file is now at: #{File.absolute_path(@opts.creds_file)}"
    end
  end
end

Common.register_command({
  :invocation => "dev-up",
  :description => "Brings up the development environment, including db migrations and config " \
     "update. (You can use run-api instead if database and config are up-to-date.)",
  :fn => lambda { |*args| dev_up(*args) }
})

Common.register_command({
  :invocation => "run-api",
  :description => "Runs the api server (assumes database and config are already up-to-date.)",
  :fn => lambda { |*args| run_api_and_db(*args) }
})

Common.register_command({
  :invocation => "clean",
  :description => "Runs gradle clean. Occasionally necessary before generating code from Swagger.",
  :fn => lambda { |*args| clean(*args) }
})

Common.register_command({
  :invocation => "get-service-creds",
  :description => "Copies sa-key.json locally (for use when running tests from an IDE, etc).",
  :fn => lambda { |*args| GetTestServiceAccountCreds.new("get-service-creds", args).run }
})

Common.register_command({
  :invocation => "test",
  :description => "Runs tests. To run a single test, add (for example) " \
      "--tests org.pmiops.workbench.interceptors.AuthInterceptorTest",
  :fn => lambda { |*args| run_tests(*args) }
})

Common.register_command({
  :invocation => "integration",
  :description => "Runs integration tests.",
  :fn => lambda { |*args| run_integration_tests(*args) }
})

Common.register_command({
  :invocation => "bigquerytest",
  :description => "Runs bigquerytest tests.",
  :fn => lambda { |*args| run_bigquery_tests(*args) }
})

Common.register_command({
  :invocation => "gradle",
  :description => "Runs gradle inside the API docker container with the given arguments.",
  :fn => lambda { |*args| run_gradle(*args) }
})

Common.register_command({
  :invocation => "connect-to-db",
  :description => "Connect to the running database via mysql.",
  :fn => lambda { |*args| connect_to_db(*args) }
})

Common.register_command({
  :invocation => "docker-clean",
  :description => \
    "Removes docker containers and volumes, allowing the next `dev-up` to" \
    " start from scratch (e.g., the database will be re-created). Includes ALL" \
    " docker images, not just for the API.",
  :fn => lambda { |*args| docker_clean(*args) }
})

Common.register_command({
  :invocation => "rebuild-image",
  :description => "Re-builds the dev docker image (necessary when Dockerfile is updated).",
  :fn => lambda { |*args| rebuild_image(*args) }
})

Common.register_command({
  :invocation => "create-db-creds",
  :description => "Creates database credentials in a file in GCS; accepts project and account args",
  :fn => lambda { |*args| create_db_creds(*args) }
})

Common.register_command({
  :invocation => "drop-cloud-db",
  :description => "Drops the Cloud SQL database for the specified project",
  :fn => lambda { |*args| drop_cloud_db(*args) }
})

Common.register_command({
  :invocation => "drop-cloud-cdr",
  :description => "Drops the cdr schema of Cloud SQL database for the specified project",
  :fn => lambda { |*args| drop_cloud_cdr(*args) }
})

Common.register_command({
  :invocation => "run-cloud-migrations",
  :description => "Runs database migrations on the Cloud SQL database for the specified project.",
  :fn => lambda { |*args| run_cloud_migrations(*args) }
})

Common.register_command({
  :invocation => "run-cloud-cdr-migrations",
  :description => "Runs database migrations for cdr schema on the Cloud SQL database for the specified project.",
  :fn => lambda { |*args| run_cloud_cdr_migrations(*args) }
})

Common.register_command({
  :invocation => "connect-to-cloud-db",
  :description => "Connect to a Cloud SQL database via mysql.",
  :fn => lambda { |*args| connect_to_cloud_db(*args) }
})

Common.register_command({
  :invocation => "register-service-account",
  :description => "Registers a service account with Firecloud; do this once per account we use.",
  :fn => lambda { |*args| register_service_account(*args) }
})

Common.register_command({
  :invocation => "update-cloud-config",
  :description => "Updates configuration in Cloud SQL database for the specified project.",
  :fn => lambda { |*args| update_cloud_config(*args) }
})

Common.register_command({
  :invocation => "set-authority",
  :description => "Set user authorities (permissions). See set-authority --help.",
  :fn => lambda { |*args| SetAuthority.new("set-authority", args, true).run }
})

Common.register_command({
  :invocation => "deploy-api",
  :description => "Deploys the API server to the specified cloud project.",
  :fn => lambda { |*args| DeployApi.new("deploy-api", args).run }
})


