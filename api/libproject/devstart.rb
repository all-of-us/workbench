require_relative "utils/common"
require "io/console"
require "optparse"
require "tempfile"

def dev_up(args)
  common = Common.new
  common.docker.requires_docker

  common.run_inline %W{docker-compose up -d}
  at_exit { common.run_inline %W{docker-compose down} }

  common.run_inline_swallowing_interrupt %W{docker-compose logs -f api}
end

def connect_to_db(args)
  common = Common.new
  common.docker.requires_docker

  cmd = "MYSQL_PWD=root-notasecret mysql --database=workbench"
  common.run_inline %W{docker-compose exec db sh -c #{cmd}}
end

def rebuild_image(args)
  common = Common.new
  common.docker.requires_docker

  common.run_inline %W{docker-compose build}
end

def get_service_account_creds_file(project, account, creds_file)
  common = Common.new
  service_account ="#{project}@appspot.gserviceaccount.com"
  common.run_inline %W(gcloud iam service-accounts keys create #{creds_file.path}
    --iam-account=#{service_account} --project=#{project} --account=#{account})
end

def delete_service_accounts_creds(project, account, creds_file)
  tmp_private_key = `grep private_key_id #{creds_file.path} | cut -d\\\" -f4`.strip()
  service_account ="#{project}@appspot.gserviceaccount.com"
  common = Common.new
  common.run_inline %W(gcloud iam service-accounts keys delete #{tmp_private_key} -q
     --iam-account=#{service_account} --project=#{project} --account=#{account})
  creds_file.unlink
end

def activate_service_account(creds_file)
  common = Common.new
  common.run_inline %W(gcloud auth activate-service-account --key-file #{creds_file.path})
end

def copy_file_to_gcs(source_path, bucket, filename)
  common = Common.new
  common.run_inline %W{gsutil cp #{source_path} gs://#{bucket}/#{filename}}
end

def create_db_creds(args)
  options = {}
  OptionParser.new do |opts|
    opts.banner = "Usage: project.rb create-db-creds [options]"

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
        activate_service_account(service_account_creds_file)
        begin
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
  else
    raise(usage)
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
  :fn => Proc.new { |*args| connect_to_db(*args) }
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
