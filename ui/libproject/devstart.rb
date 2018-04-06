# UI project management commands and command-line flag definitions.

require "optparse"
require "set"
require_relative "../../aou-utils/serviceaccounts"
require_relative "../../aou-utils/swagger"
require_relative "../../aou-utils/utils/common"
require_relative "../../aou-utils/workbench"

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

class BuildOptions
  # Keep in sync with .angular-cli.json.
  ENV_CHOICES = %W{local-test local test staging stable}
  attr_accessor :env

  def initialize
    self.env = "dev"
  end

  def parse cmd_name, args
    parser = OptionParser.new do |parser|
      parser.banner = "Usage: ./project.rb #{cmd_name} [options]"
      parser.on(
        "--environment ENV", ENV_CHOICES,
        "Environment (default: local-test): [#{ENV_CHOICES.join(" ")}]") do |v|
        # The default environment file (called "dev" in Angular language)
        # compiles a local server to run against the deployed remote test API.
        self.env = v == "local-test" ? "dev" : v
      end
    end
    parser.parse args
    self
  end
end

# Command line parsing and run logic for deploy-ui command
class DeployUI
  attr_reader :common, :opts

  def initialize(cmd_name, args)
    @common = Common.new
    @cmd_name = cmd_name
    @args = args
    @parser = create_parser(cmd_name)
    @opts = Options.new
  end

  def add_options
    # TODO: Make flag handling more consistent with api/devstart.rb
    @parser.on("--project [PROJECT]",
        "Project to create credentials for (e.g. all-of-us-workbench-test). Required.") do |project|
      @opts.project = project
    end
    @parser.on("--account [ACCOUNT]",
      "Service account to act as for deployment, if any. Defaults to the GAE " +
      "default service account.") do |account|
      @opts.account = account
    end
    @parser.on("--version [VERSION]",
          "The name of the version to deploy. Required.") do |version|
       @opts.version = version
    end
    @parser.on("--promote",
               "Promote this version to immediately begin serving UI traffic. " +
               "Required: must set --promote or --no-promote") do
       @opts.promote = true
    end
    @parser.on("--no-promote",
               "Deploy, but do not yet serve traffic from this version. " +
               "Required: must set --promote or --no-promote") do
       @opts.promote = false
    end
  end

  def validate_options
    if @opts.project == nil || @opts.version == nil || @opts.promote == nil
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
      "all-of-us-rw-staging" => "staging",
      "all-of-us-rw-stable" => "stable",
    }
    environment_name = environment_names[@opts.project]

    build(@cmd_name, %W{--environment #{environment_name}})
    ServiceAccountContext.new(@opts.project, service_account=@opts.account).run do
      common.run_inline %W{gcloud app deploy
        --project #{@opts.project}
        --version #{@opts.version}
        #{opts.promote ? "--promote" : "--no-promote"}
      }
    end
  end
end

def build(cmd_name, args)
  ensure_docker cmd_name, args
  options = BuildOptions.new.parse(cmd_name, args)

  common = Common.new
  common.run_inline %W{yarn install}

  # Just use --aot for "test", which catches many compilation issues. Go full
  # --prod (includes --aot) for other environments. Don't use full --prod in the
  # test environment, as it takes twice as long to compile (1m vs 2m on 4/5/18)
  # and also uglifies the source.
  # See https://github.com/angular/angular-cli/wiki/build#--dev-vs---prod-builds.
  optimize = "--aot"
  if Set['staging', 'stable'].include?(options.env)
    optimize = "--prod"
  end
  common.run_inline %W{yarn run build
    #{optimize} --environment=#{options.env} --no-watch --no-progress}
end

Common.register_command({
  :invocation => "build",
  :description => "Builds the UI for the given environment.",
  :fn => lambda { |*args| build("build", args) }
})

def deploy_ui(cmd_name, args)
  ensure_docker cmd_name, args
  DeployUI.new(cmd_name, args).run
end

Common.register_command({
  :invocation => "deploy-ui",
  :description => "Deploys the UI to the specified cloud project.",
  :fn => lambda { |*args| deploy_ui("deploy-ui", args) }
})

def dev_up(*args)
  common = Common.new
  options = BuildOptions.new.parse("dev-up", args)

  install_dependencies

  ENV["ENV_FLAG"] = "--environment=#{options.env}"
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
