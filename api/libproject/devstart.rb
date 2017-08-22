require_relative "utils/common"
require "io/console"
require "optparse"
require "tempfile"

def dev_up(args)
  common = Common.new
  common.docker.requires_docker

  at_exit { common.run_inline %W{docker-compose down} }
  common.run_inline %W{docker-compose up -d db}
  common.run_inline %W{docker-compose run db-migration}
  common.run_inline_swallowing_interrupt %W{docker-compose up api}
end

def connect_to_db(args)
  common = Common.new
  common.docker.requires_docker

  cmd = "MYSQL_PWD=root-notasecret mysql --database=workbench"
  common.run_inline %W{docker-compose exec db sh -c #{cmd}}
end

def docker_clean(args)
  common = Common.new
  common.docker.requires_docker

  common.run_inline %W{docker-compose down --volumes}
end

def rebuild_image(args)
  common = Common.new
  common.docker.requires_docker

  common.run_inline %W{docker-compose build}
end

def get_service_account_creds_file(project, account, creds_file)
  common = Common.new
  service_account ="#{project}@appspot.gserviceaccount.com"
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
  common.run_inline %W{gcloud auth activate-service-account --key-file #{creds_file.path}}
end

def copy_file_to_gcs(source_path, bucket, filename)
  common = Common.new
  common.run_inline %W{gsutil cp #{source_path} gs://#{bucket}/#{filename}}
end

def get_file_from_gcs(bucket, filename, target_path)
  common = Common.new
  common.run_inline %W{gsutil cp gs://#{bucket}/#{filename} #{target_path}}
end

def read_db_vars(project)
  creds_file = Tempfile.new("#{project}-vars.env")
  begin
    get_file_from_gcs("#{project}-credentials", "vars.env", creds_file.path)
    creds_file.open
    creds_file.each_line do |line|
      line = line.strip()
      if !line.empty?
        parts = line.split("=")
        ENV[parts[0]] = parts[1]
      end
    end
  ensure
    creds_file.unlink
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
      -credential_file=#{creds_file.path} &})
  common.run_inline %W{sleep 3}
  return pid
end

def do_run_migrations(project)
  read_db_vars(project)
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
  unless system("cd db && ../gradlew --no-daemon --info update && cd ..")
    raise("Error upgrading database. Exiting.")
  end
end

def do_drop_db(project)
  read_db_vars(project)
  common = Common.new
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

def get_project_and_account(args, command)
  options = {}
  OptionParser.new do |opts|
    opts.banner = "Usage: project.rb #{command} [options]"

    opts.on("--project [PROJECT]", "Project to create credentials for") do |project|
      options["project"] = project
    end
    opts.on("--account [ACCOUNT]",
      "Account to use when creating credentials (your.name@pmi-ops.org)") do |account|
          options["account"] = account
    end
  end.parse!(args)

  usage = "Usage: --project <project> --account <account>"
  common = Common.new()
  if options.has_key?("project") && options.has_key?("account")
    project = options["project"]
    account = options["account"]
    if project == nil || account == nil
      raise(usage)
    end
  else
    raise(usage)
  end
  return options
end

def drop_cloud_db(args)
  options = get_project_and_account(args, "drop-cloud-db")
  project = options["project"]
  account = options["account"]
  service_account_creds_file = Tempfile.new("#{project}-creds.json")
  get_service_account_creds_file(project, account, service_account_creds_file)
  begin
    pid = run_cloud_sql_proxy(project, service_account_creds_file)
    begin
      do_drop_db(project)
    ensure
      Process.kill("HUP", pid)
    end
  ensure
    delete_service_accounts_creds(project, account, service_account_creds_file)
  end
end

def connect_to_cloud_db(args)
  options = get_project_and_account(args, "connect-to-cloud-db")
  project = options["project"]
  account = options["account"]
  service_account_creds_file = Tempfile.new("#{project}-creds.json")
  get_service_account_creds_file(project, account, service_account_creds_file)
  begin
    read_db_vars(project)
    pid = run_cloud_sql_proxy(project, service_account_creds_file)
    begin
      system("mysql -u \"workbench\" -p\"#{ENV["WORKBENCH_DB_PASSWORD"]}\" --host 127.0.0.1 "\
                    "--port 3307")
    ensure
      Process.kill("HUP", pid)
    end
  ensure
    delete_service_accounts_creds(project, account, service_account_creds_file)
  end
end

def run_cloud_migrations(args)
  options = get_project_and_account(args, "run-cloud-migrations")
  project = options["project"]
  account = options["account"]
  service_account_creds_file = Tempfile.new("#{project}-creds.json")
  get_service_account_creds_file(project, account, service_account_creds_file)
  begin
    pid = run_cloud_sql_proxy(project, service_account_creds_file)
    begin
      puts "Running migrations..."
      do_run_migrations(project)
    ensure
      Process.kill("HUP", pid)
    end
  ensure
    delete_service_accounts_creds(project, account, service_account_creds_file)
  end
end

def create_db_creds(args)
  options = get_project_and_account(args, "create-db-creds")
  project = options["project"]
  account = options["account"]
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
  creds_file = Tempfile.new("#{project}-vars.env")
  if creds_file
    begin
      creds_file.puts "DB_CONNECTION_STRING=jdbc:google:mysql://#{instance_name}/workbench"
      creds_file.puts "DB_DRIVER=com.mysql.jdbc.GoogleDriver"
      creds_file.puts "DB_HOST=127.0.0.1"
      creds_file.puts "DB_NAME=workbench"
      creds_file.puts "CLOUD_SQL_INSTANCE=#{instance_name}"
      creds_file.puts "LIQUIBASE_DB_USER=liquibase"
      creds_file.puts "LIQUIBASE_DB_PASSWORD=#{workbench_password}"
      creds_file.puts "MYSQL_ROOT_PASSWORD=#{root_password}"
      creds_file.puts "WORKBENCH_DB_USER=workbench"
      creds_file.puts "WORKBENCH_DB_PASSWORD=#{workbench_password}"
      creds_file.close

      # Get service account credentials
      service_account_creds_file = Tempfile.new("#{project}-creds.json")
      get_service_account_creds_file(project, account, service_account_creds_file)
      begin
        activate_service_account(service_account_creds_file)
        copy_file_to_gcs(creds_file.path, "#{project}-credentials", "vars.env")
      ensure
        delete_service_accounts_creds(project, account, service_account_creds_file)
      end
    ensure
      creds_file.unlink
    end
  else
    raise("Error creating file.")
  end
end

Common.register_command({
  :invocation => "dev-up",
  :description => "Brings up the development environment.",
  :fn => Proc.new { |args| dev_up(args) }
})

Common.register_command({
  :invocation => "connect-to-db",
  :description => "Connect to the running database via mysql.",
  :fn => Proc.new { |args| connect_to_db(args) }
})

Common.register_command({
  :invocation => "docker-clean",
  :description => \
    "Removes docker containers and volumes, allowing the next `dev-up` to\n" \
    "start from scratch (e.g., the database will be re-created).",
  :fn => Proc.new { |args| docker_clean(args) }
})

Common.register_command({
  :invocation => "rebuild-image",
  :description => "Re-builds the dev docker image (necessary when Dockerfile is updated).",
  :fn => Proc.new { |args| rebuild_image(args) }
})

Common.register_command({
  :invocation => "create-db-creds",
  :description => "Creates database credentials in a file in GCS; accepts project and account args",
  :fn => Proc.new { |args| create_db_creds(args) }
})

Common.register_command({
  :invocation => "drop-cloud-db",
  :description => "Drops the Cloud SQL database for the specified project",
  :fn => Proc.new { |args| drop_cloud_db(args) }
})

Common.register_command({
  :invocation => "run-cloud-migrations",
  :description => "Runs database migrations on the Cloud SQL database for the specified project.",
  :fn => Proc.new { |args| run_cloud_migrations(args) }
})

Common.register_command({
  :invocation => "connect-to-cloud-db",
  :description => "Connect to a Cloud SQL database via mysql.",
  :fn => Proc.new { |args| connect_to_cloud_db(args) }
})
