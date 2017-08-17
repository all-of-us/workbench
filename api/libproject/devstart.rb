require_relative "utils/common"
require "io/console"

def dev_up(args)
  common = Common.new
  common.docker.requires_docker

  common.run_inline %W{docker-compose up -d}
  at_exit { common.run_inline %W{docker-compose down} }

  common.run_inline_swallowing_interrupt %W{docker-compose logs -f api}
end

def rebuild_image(args)
  common = Common.new
  common.docker.requires_docker

  common.run_inline %W{docker-compose build}
end

def auth_login(account)
  common = Common.new
  common.run_inline %W{gcloud auth login #{account}}
end

def set_project(project)
  common = Common.new
  common.run_inline %W{gcloud config set project #{project}}
end

def copy_file_to_gcs(source_path, bucket, filename)
  common = Common.new
  common.run_inline %W{gsutil cp #{source_path} gs://#{bucket}/#{filename}}
end

def create_db_creds(args)
  usage = "Usage: --project <project> --account <account>"
  common = Common.new()
  if args.has_key?("project") && args.has_key?("account")
    project = args["project"]
    account = args["account"]
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

    auth_login(account)
    set_project(project)
    instance_name = "#{project}:us-central1:workbenchmaindb"
    creds_filename = "/tmp/vars.env"
    creds_file = File.new(creds_filename, "w")
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
        copy_file_to_gcs(creds_filename, "#{project}-credentials", "vars.env")
      ensure
        File.delete(creds_file)
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
  :invocation => "rebuild-image",
  :description => "Re-builds the dev docker image (necessary when Dockerfile is updated).",
  :fn => Proc.new { |args| rebuild_image(args) }
})

Common.register_command({
  :invocation => "create-db-creds",
  :description => "Creates database credentials in a file in GCS; accepts project and account args",
  :fn => Proc.new { |args| create_db_creds(args) }
})
