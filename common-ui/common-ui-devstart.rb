# Common UI project management commands and command-line flag definitions.

require "optparse"
require "set"
require_relative "../aou-utils/serviceaccounts"
require_relative "../aou-utils/swagger"
require_relative "../aou-utils/utils/common"
require_relative "../aou-utils/workbench"

DRY_RUN_CMD = %W{echo [DRY_RUN]}

class Options < OpenStruct
end

# Ensure the docker is running what you want by calling the command to run it
def ensure_docker(cmd_name, args)
  unless Workbench.in_docker?
    exec(*(%W{docker-compose run --rm #{@ui_name} ./project.rb #{cmd_name}} + args))
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

def swagger_regen(cmd_name)
  ensure_docker cmd_name, []

  common = Common.new
  Workbench::Swagger.download_swagger_codegen_cli
  common.run_inline %W{yarn run codegen}
end

def build(cmd_name, ui_name, args)
  ensure_docker cmd_name, args
  options = BuildOptions.new.parse(cmd_name, args)

  common = Common.new
  common.run_inline %W{yarn install --frozen-lockfile}

  # Just use --aot for "test", which catches many compilation issues. Go full
  # --prod (includes --aot) for other environments. Don't use full --prod in the
  # test environment, as it takes twice as long to compile (1m vs 2m on 4/5/18)
  # and also uglifies the source.
  # See https://github.com/angular/angular-cli/wiki/build#--dev-vs---prod-builds.
  optimize = "--aot"
  if Set['perf', 'staging', 'stable', 'prod'].include?(options.env)
    optimize = "--prod"
  end

  # Angular version 5 requires --environment instead of --configuration as an option
  angular_opts = "--configuration=#{options.env}"
  common.run_inline %W{yarn run build
      #{optimize} #{angular_opts} --no-watch --no-progress}
end

class CommonUiDevStart
  def initialize(ui_name)
    @ui_name = ui_name;
  end

  # Install ui dependencies with yarn
  def install_dependencies()
    common = Common.new
    common.run_inline %W{docker-compose run --rm #{@ui_name} yarn install}
  end

  def deploy_ui(cmd_name, args)
    ensure_docker cmd_name, args
    DeployUI.new(cmd_name, @ui_name, args).run
  end

  def dev_up(*args)
    common = Common.new
    options = BuildOptions.new.parse("dev-up", args)

    install_dependencies

    ENV["ENV_FLAG"] = "--configuration=#{options.env}"
    at_exit { common.run_inline %W{docker-compose down} }

    # Can't use swagger_regen here as it enters docker.
    common.run_inline %W{docker-compose run --rm #{@ui_name} yarn run codegen}
    common.run_inline %W{docker-compose run -d --service-ports tests}

    common.status "Tests started. Open\n"
    common.status "    http://localhost:9876/debug.html for ui \n"
    common.status "in a browser to view/run tests."

    common.run_inline %W{docker-compose run --rm --service-ports #{@ui_name}}
  end

  def test()
    common = Common.new

    install_dependencies

    at_exit { common.run_inline %W{docker-compose down} }

    # Can't use swagger_regen here as it enters docker.
    common.run_inline %W{docker-compose run --rm #{@ui_name} yarn run codegen}
    common.run_inline %W{docker-compose run --rm --service-ports tests}
  end

  def run_linter()
    Common.new.run_inline %W{docker-compose run --rm #{@ui_name} yarn run lint}
  end

  def rebuild_image()
    common = Common.new
    common.run_inline %W{docker-compose build}
  end

  def docker_run(_cmd_name, args)
    Common.new.run_inline %W{docker-compose run --rm #{@ui_name}} + args
  end

  def clean_environment()
    common = Common.new
    common.run_inline %W{rm -rf node_modules}
    Common.new.run_inline %W{docker-compose down --volumes --rmi=local}
  end

  def test_angular()
    Common.new.run_inline %W{docker-compose run --rm #{@ui_name} yarn test --no-watch --no-progress --browsers=ChromeHeadless}
  end

  def test_react()
    Common.new.run_inline %W{docker-compose run --rm #{@ui_name} yarn test-react --detectOpenHandles --forceExit --runInBand}
  end

  def register_commands
    Common.register_command({
                            :invocation => "install-dependencies",
                            :description => "Installs dependencies via yarn.",
                            :fn => Proc.new { |*args| install_dependencies(*args) }
                        })
    Common.register_command({
                                :invocation => "deploy-ui",
                                :description => "Deploys the UI to the specified cloud project.",
                                :fn => ->(*args) { deploy_ui("deploy-ui", args) }
                            })
    Common.register_command({
                                :invocation => "build",
                                :description => "Builds the UI for the given environment.",
                                :fn => ->(*args) { build("build", @ui_name, args) }
                            })
    Common.register_command({
                                :invocation => "dev-up",
                                :description => "Brings up the development environment.",
                                :fn => Proc.new { |*args| dev_up(*args) }
                            })
    Common.register_command({
                                :invocation => "swagger-regen",
                                :description => "Regenerates API client libraries from Swagger definitions.",
                                :fn => Proc.new { |_| swagger_regen("swagger-regen") }
                            })
    Common.register_command({
                                :invocation => "test",
                                :description => "Brings up the testing environment.",
                                :fn => Proc.new { |_| test() }
                            })
    Common.register_command({
                                :invocation => "lint",
                                :description => "Runs linter.",
                                :fn => Proc.new { |_| run_linter() }
                            })

    Common.register_command({
                                :invocation => "rebuild-image",
                                :description => "Re-builds the dev docker image (necessary when Dockerfile is updated).",
                                :fn => Proc.new { |_| rebuild_image() }
                            })
    Common.register_command({
                                :invocation => "clean-environment",
                                :description => "Removes node modules, docker volumes and images.",
                                :fn => ->() { clean_environment() }
                            })
    Common.register_command({
                                :invocation => "docker-run",
                                :description => "Runs the specified command in a docker container.",
                                :fn => ->(*args) { docker_run("docker-run", args) }
                            })
    Common.register_command({
        :invocation => "test-angular",
        :description => "Runs the Angular test suite.",
        :fn => Proc.new { |_| test_angular() }
    })
    Common.register_command({
        :invocation => "test-react",
        :description => "Runs the React test suite.",
        :fn => Proc.new { |_| test_react() }
    })
end

end

class BuildOptions
  # Keep in sync with .angular-cli.json.
  ENV_CHOICES = %W{local-test local test perf staging stable prod}
  attr_accessor :env

  def initialize
    self.env = "dev"
  end

  def parse cmd_name, args
    parser = OptionParser.new do |p|
      p.banner = "Usage: ./project.rb #{cmd_name} [options]"
      p.on(
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

  def initialize(cmd_name, ui_name, args)
    @common = Common.new
    @cmd_name = cmd_name
    @ui_name = ui_name
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
    @parser.on("--dry-run",
               "Don't actually deploy, just log the command lines which would be " +
               "executed on a real invocation.") do
      @opts.dry_run = true
    end
    @parser.on("--version [VERSION]",
               "The name of the version to deploy. Required.") do |version|
      @opts.version = version
    end
    @parser.on("--key-file [keyfile]",
               "Service account key file to use for deployment authorization") do |key_file|
      @opts.key_file = key_file
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
    @parser.on("--quiet",
               "Whether to suppress user prompts; shown by default") do
      @opts.quiet = true
    end
  end

  def validate_options
    if @opts.project.nil? || @opts.version.nil? || @opts.promote.nil?
      puts @parser.help
      exit 1
    end
  end

  def run
    add_options
    @parser.parse @args
    validate_options
    project_names_to_environment_names = {
        "all-of-us-workbench-test" => "test",
        "all-of-us-rw-perf" => "perf",
        "all-of-us-rw-staging" => "staging",
        "all-of-us-rw-stable" => "stable",
        "all-of-us-rw-prod" => "prod",
    }
    environment_name = project_names_to_environment_names[@opts.project]

    swagger_regen(@cmd_name)
    build(@cmd_name, @ui_name, %W{--environment #{environment_name}})
    ServiceAccountContext.new(@opts.project, @opts.account, @opts.key_file).run do
      cmd_prefix = @opts.dry_run ? DRY_RUN_CMD : []
      common.run_inline(cmd_prefix + %W{gcloud app deploy
       --project #{@opts.project}
       --version #{@opts.version}
       #{opts.promote ? "--promote" : "--no-promote"}} + (@opts.quiet ? %W{--quiet} : []))
    end
  end
end
