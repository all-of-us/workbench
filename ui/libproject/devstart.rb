# UI project management commands and command-line flag definitions.

require "optparse"
require "set"
require_relative "../../aou-utils/serviceaccounts"
require_relative "../../aou-utils/utils/common"
require_relative "../../aou-utils/workbench"

DRY_RUN_CMD = %W{echo [DRY_RUN]}

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

def build(cmd_name, args)
  options = BuildOptions.new.parse(cmd_name, args)

  common = Common.new

  common.run_inline %W{gsutil cp gs://all-of-us-workbench-test-credentials/.npmrc ..}
  common.run_inline %W{yarn install --frozen-lockfile}
  common.run_inline %W{yarn run deps}

  # Just use --aot for "test", which catches many compilation issues. Go full
  # --prod (includes --aot) for other environments. Don't use full --prod in the
  # test environment, as it takes twice as long to compile (1m vs 2m on 4/5/18)
  # and also uglifies the source.
  # See https://github.com/angular/angular-cli/wiki/build#--dev-vs---prod-builds.
  optimize = "--aot"
  if Set['staging', 'stable', 'preprod', 'prod'].include?(options.env)
    optimize = "--prod"
  end

  react_opts = "REACT_APP_ENVIRONMENT=#{options.env}"
  common.run_inline "#{react_opts} yarn run build #{optimize} --no-watch --no-progress"
end

class DevStart
  def deploy_ui(cmd_name, args)
    DeployUI.new(cmd_name, args).run
  end

  def register_commands
    Common.register_command({
                                :invocation => "deploy-ui",
                                :description => "Deploys the UI to the specified cloud project.",
                                :fn => ->(*args) { deploy_ui("deploy-ui", args) }
                            })
    Common.register_command({
                                :invocation => "build",
                                :description => "Builds the UI for the given environment.",
                                :fn => ->(*args) { build("build", args) }
                            })
  end
end

class BuildOptions
  # Keep in sync with .angular-cli.json.
  ENV_CHOICES = %W{local-test local test staging stable preprod prod}
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
        "all-of-us-rw-staging" => "staging",
        "all-of-us-rw-stable" => "stable",
        "all-of-us-rw-preprod" => "preprod",
        "all-of-us-rw-prod" => "prod",
    }
    environment_name = project_names_to_environment_names[@opts.project]

    common.run_inline(%W{yarn deps})
    build(@cmd_name, %W{--environment #{environment_name}})
    ServiceAccountContext.new(@opts.project, @opts.account, @opts.key_file).run do
      cmd_prefix = @opts.dry_run ? DRY_RUN_CMD : []
      common.run_inline(cmd_prefix + %W{gcloud app deploy
       --project #{@opts.project}
       --version #{@opts.version}
       #{opts.promote ? "--promote" : "--no-promote"}} + (@opts.quiet ? %W{--quiet} : []))
    end
  end
end

DevStart.new.register_commands
