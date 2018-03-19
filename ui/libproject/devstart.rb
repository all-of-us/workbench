# UI project management commands and command-line flag definitions.

require "optparse"
require_relative "../../aou-utils/serviceaccounts"
require_relative "../../aou-utils/utils/common"
require_relative "../../aou-utils/workbench"
require_relative "../../aou-utils/swagger"

class Options < OpenStruct
end

def ensure_docker(cmd_name, args)
  unless Workbench::in_docker?
    exec *(%W{docker-compose run --rm ui ./project.rb #{cmd_name}} + args)
  end
end

# Creates a default command-line argument parser.
# command_name: For help text.
def create_parser(command_name)
  OptionParser.new do |parser|
    parser.banner = "Usage: ./project.rb #{command_name} [options]"
    parser
  end
end

def install_dependencies()
  common = Common.new
  common.run_inline %W{docker-compose run --rm ui yarn install}
end

Common.register_command({
  :invocation => "install-dependencies",
  :description => "Installs dependencies via yarn.",
  :fn => Proc.new { |*args| install_dependencies(*args) }
})

def swagger_regen()
  common = Common.new
  Workbench::Swagger.download_swagger_codegen_cli
  common.run_inline %W{docker-compose run --rm ui yarn run codegen}
end

Common.register_command({
  :invocation => "swagger-regen",
  :description => "Regenerates API client libraries from Swagger definitions.",
  :fn => Proc.new { |*args| swagger_regen(*args) }
})

class DevUpOptions
  ENV_CHOICES = %W{local-test local test prod}
  attr_accessor :env

  def initialize
    self.env = nil
  end

  def parse args
    parser = OptionParser.new do |parser|
      parser.banner = "Usage: ./project.rb dev-up [options]"
      parser.on(
          "--environment ENV", ENV_CHOICES, "Environment [local-test (default), local, test, prod]") do |v|
        # The default environment file (called "dev" in Angular language)
        # compiles a local server to run against the deployed remote test API.
        # Leave this unset to get that default.
        self.env = v == "local-test" ? nil : v
      end
    end
    parser.parse args
    self
  end
end

# Command line parsing and run logic for deploy-ui command
class DeployUI
  attr_reader :common, :opts

  def initialize(command_name, args)
    @common = Common.new
    @args = args
    @parser = create_parser(command_name)
    @opts = Options.new
  end

  def add_options
    @parser.on("--project [PROJECT]",
        "Project to create credentials for (e.g. all-of-us-workbench-test). Required.") do |project|
      @opts.project = project
    end
    @parser.on("--account [ACCOUNT]",
         "Account to use when creating credentials (your.name@pmi-ops.org). Required.") do |account|
      @opts.account = account
    end
    @parser.on("--version [VERSION]",
          "The name of the version to deploy. Required.") do |version|
       @opts.version = version
    end
    @parser.on("--promote",
          "Use this if you want to promote this version so it receives traffic. By default, it won't."
          ) do |promote|
       @opts.promote = "promote"
    end
    @opts.promote = "no-promote" # default
  end

  def validate_options
    if @opts.project == nil || @opts.account == nil || @opts.version == nil
      puts @parser.help
      exit 1
    end
  end

  def run
    add_options
    @parser.parse @args
    validate_options
    environment_names = {
      "all-of-us-workbench-test" => "test",
      "all-of-us-rw-stable" => "stable",
    }
    environment_name = environment_names[@opts.project]
    common.run_inline %W{yarn run build --environment=#{environment_name} --no-watch --no-progress}
    ServiceAccountContext.new(@opts.project).run do
      common.run_inline %W{gcloud app deploy --project #{@opts.project} --version #{@opts.version} --#{@opts.promote}}
    end
  end
end

def deploy_ui(cmd_name, args)
  ensure_docker cmd_name, args
  DeployUI.new("deploy-ui", args).run
end

Common.register_command({
  :invocation => "deploy-ui",
  :description => "Deploys the UI to the specified cloud project.",
  :fn => lambda { |*args| deploy_ui("deploy-ui", args) }
})

def dev_up(*args)
  common = Common.new

  options = DevUpOptions.new.parse(args)

  install_dependencies

  ENV["ENV_FLAG"] = options.env ? "--environment=#{options.env}" : ""
  at_exit { common.run_inline %W{docker-compose down} }
  swagger_regen()
  common.run_inline %W{docker-compose run -d --service-ports tests}

  common.status "Tests started. Open\n"
  common.status "    http://localhost:9876/debug.html\n"
  common.status "in a browser to view/run tests."

  common.run_inline %W{docker-compose run --rm --service-ports ui}
end

Common.register_command({
  :invocation => "dev-up",
  :description => "Brings up the development environment.",
  :fn => Proc.new { |*args| dev_up(*args) }
})

def run_linter()
  Common.new.run_inline %W{docker-compose run --rm ui yarn run lint}
end

Common.register_command({
  :invocation => "lint",
  :description => "Runs linter.",
  :fn => Proc.new { |*args| run_linter(*args) }
})

def rebuild_image()
  common = Common.new

  common.run_inline %W{docker-compose build}
end

Common.register_command({
  :invocation => "rebuild-image",
  :description => "Re-builds the dev docker image (necessary when Dockerfile is updated).",
  :fn => Proc.new { |*args| rebuild_image(*args) }
})

def docker_run(cmd_name, args)
  Common.new.run_inline %W{docker-compose run --rm ui} + args
end

Common.register_command({
  :invocation => "docker-run",
  :description => "Runs the specified command in a docker container.",
  :fn => lambda { |*args| docker_run("docker-run", args) }
})

def clean_environment(cmd_name, args)
  common = Common.new
  common.run_inline %W{rm -rf node_modules}
  Common.new.run_inline %W{docker-compose down --volumes --rmi=local}
end

Common.register_command({
  :invocation => "clean-environment",
  :description => "Removes node modules, docker volumes and images.",
  :fn => lambda { |*args| clean_environment("clean-environment", args) }
})
