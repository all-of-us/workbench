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

def create_parser(command)
  OptionParser.new do |parser|
    parser.banner = "Usage: ./project.rb #{command} [options]"
    parser
  end
end

def add_default_options(parser, options)
  parser.on("--project [PROJECT]",
      "Project to create credentials for (e.g. all-of-us-workbench-test)") do |project|
    options.project = project
  end
  parser.on("--account [ACCOUNT]",
       "Account to use when creating credentials (your.name@pmi-ops.org); "\
       "use this or --creds-file") do |account|
    options.account = account
  end
  parser.on("--creds-file [CREDS-FILE]",
       "Path to a file containing credentials; use this or --account.") do |creds_file|
    options.creds_file = creds_file
  end
  parser
end

def validate_default_options(parser, options)
  if options.project == nil || !((options.account == nil) ^ (options.creds_file == nil))
    puts parser.help
    exit 1
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

  common.status "Updating configuration..."
  common.run_inline %W{docker-compose run update-config}

  run_api(account)
end

def run_api(account)
  common = Common.new
  # TODO(dmohs): This can be simplified now that we are using a shared service account.
  do_run_with_creds("all-of-us-workbench-test", account, nil, lambda { |project, account, creds_file|
    common.status "Starting API. This can take a while. Thoughts on reducing development cycle time"
    common.status "are here:"
    common.status "  https://github.com/all-of-us/workbench/blob/master/api/doc/2017/dev-cycle.md"
    at_exit { common.run_inline %W{docker-compose down} }
    common.run_inline_swallowing_interrupt %W{docker-compose up api}
  })
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
  do_run_with_creds("all-of-us-workbench-test", account, nil, lambda { |project, account, creds_file|
    common.run_inline %W{docker-compose run --rm api ./gradlew integration} + args
  })
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
    system("docker rm -f #{docker_images}")
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
  puts "Running Cloud SQL Proxy..."
  pid = spawn(*%W{./cloud_sql_proxy
      -instances #{project}:us-central1:workbenchmaindb=tcp:3307
      -credential_file=#{creds_file} &})
  common.run_inline %W{sleep 3}
  return pid
end

def do_run_migrations(creds_file, project)
  read_db_vars(creds_file, project)
  common = Common.new
  create_db_file = Tempfile.new("#{project}-create-db.sql")
  begin
    unless system("cat db/create_db.sql | envsubst > #{create_db_file.path}")
      raise("Error generating create_db file; exiting.")
    end
    puts "Creating database if it does not exist..."
    unless system("mysql -u \"root\" -p\"#{ENV["MYSQL_ROOT_PASSWORD"]}\" --host 127.0.0.1 "\
              "--port 3307 < #{create_db_file.path}")
      raise("Error creating database; exiting.")
    end
  ensure
    create_db_file.unlink
  end
  ENV["DB_PORT"] = "3307"
  puts "Upgrading database..."
  unless system("cd db && ../gradlew --info update && cd ..")
    raise("Error upgrading database. Exiting.")
  end
end

def do_drop_db(creds_file, project)
  read_db_vars(creds_file, project)
  drop_db_file = Tempfile.new("#{project}-drop-db.sql")
  begin
    unless system("cat db/drop_db.sql | envsubst > #{drop_db_file.path}")
      raise("Error generating drop_db file; exiting.")
    end
    puts "Dropping database..."
    unless system("mysql -u \"root\" -p\"#{ENV["MYSQL_ROOT_PASSWORD"]}\" --host 127.0.0.1 "\
              "--port 3307 < #{drop_db_file.path}")
      raise("Error dropping database; exiting.")
    end
  ensure
    drop_db_file.unlink
  end
end

def get_auth_login_account()
  return `gcloud config get-value account`.strip()
end

def run_with_creds(args, command, proc)
  parser = create_parser(command)
  options = Options.new
  add_default_options parser, options
  parser.parse ARGV
  validate_default_options parser, options
  do_run_with_creds(options.project, options.account, options.creds_file, proc)
end

def do_run_with_creds(project, account, creds_file, proc)
  if creds_file == nil
    service_account_creds_file = Tempfile.new("#{project}-creds.json")
    if project == "all-of-us-workbench-test"
      creds_filename = "sa-key.json"
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
      proc.call(project, account, creds_filename)
    else
      # Create a creds file and use it; clean up when done.
      get_service_account_creds_file(project, account, service_account_creds_file)
      begin
        proc.call(project, account, service_account_creds_file.path)
      ensure
        delete_service_accounts_creds(project, account, service_account_creds_file)
      end
    end
  else
    proc.call(project, account, creds_file)
  end
end

def run_with_cloud_sql_proxy(args, command, proc)
  run_with_creds(args, command, lambda { |project, account, creds_file|
    pid = run_cloud_sql_proxy(project, creds_file)
    begin
      proc.call(project, account, creds_file)
    ensure
      Process.kill("HUP", pid)
    end
  })
end

def register_service_account(*args)
  run_with_creds(args, "register-service-account", lambda { |project, account, creds_file|
    common = Common.new
    ENV["GOOGLE_APPLICATION_CREDENTIALS"] = creds_file
    system("cd ../firecloud-tools &&  ./run.sh " \
      "register_service_account/register_service_account.py -j #{creds_file} -o #{account}")
  })
end

def drop_cloud_db(*args)
  run_with_cloud_sql_proxy(args, "drop-cloud-db", lambda { |project, account, creds_file|
    do_drop_db(creds_file, project)
  })
end

def connect_to_cloud_db(*args)
  run_with_cloud_sql_proxy(args, "connect-to-cloud-db", lambda { |project, account, creds_file|
    read_db_vars(creds_file, project)
    system("mysql -u \"workbench\" -p\"#{ENV["WORKBENCH_DB_PASSWORD"]}\" --host 127.0.0.1 "\
           "--port 3307 --database #{ENV["DB_NAME"]}")
  })
end

def update_cloud_config(*args)
  run_with_cloud_sql_proxy(args, "connect-to-cloud-db", lambda { |project, account, creds_file|
    read_db_vars(creds_file, project)
    ENV["DB_PORT"] = "3307"
    unless system("cd tools && ../gradlew --info loadConfig && cd ..")
        raise("Error updating configuration. Exiting.")
    end
  })
end

def run_cloud_migrations(*args)
  run_with_cloud_sql_proxy(args, "run-cloud-migrations", lambda { |project, account, creds_file|
    puts "Running migrations..."
    do_run_migrations(creds_file, project)
  })
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
  run_with_creds(args, "create-db-creds", lambda { |project, account, creds_file|
    do_create_db_creds(project, account, creds_file)
  })
end

def get_test_service_account_creds(*args)
  run_with_creds(args, "get-service-creds", lambda { |project, account, creds_file|
    if project != "all-of-us-workbench-test"
      raise("Only call this with all-of-us-workbench-test")
    end
    puts "Creds file is now at: #{creds_file}"
  })
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
  :description => "Creates sa-key.json locally (for use when running tests, etc.)",
  :fn => lambda { |*args| get_test_service_account_creds(*args) }
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
  :invocation => "run-cloud-migrations",
  :description => "Runs database migrations on the Cloud SQL database for the specified project.",
  :fn => lambda { |*args| run_cloud_migrations(*args) }
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
