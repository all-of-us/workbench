# UI project management commands and command-line flag definitions.

require "optparse"
require "set"
require_relative "../../aou-utils/serviceaccounts"
require_relative "../../aou-utils/utils/common"
require_relative "../../aou-utils/workbench"
require_relative "../../api/libproject/environments"
require_relative "../../api/libproject/gcloudcontext"
require_relative "../../api/libproject/wboptionsparser"

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

def run_inline_or_log(dry_run, args)
  cmd_prefix = dry_run ? DRY_RUN_CMD : []
  Common.new.run_inline(cmd_prefix + args)
end

def build(cmd_name, args)
  options = BuildOptions.new.parse(cmd_name, args)

  common = Common.new

  common.run_inline %W{yarn install --frozen-lockfile}
  common.run_inline %W{yarn run deps #{options.env}}

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

def deploy_tanagra_ui(cmd_name, args)
  op = WbOptionsParser.new(cmd_name, args)
  op.opts.dry_run = false
  op.add_option(
    "--project [project]",
    ->(opts, v) { opts.project = v},
    "The Google Cloud project to deploy to."
  )
  op.add_option(
    "--account [account]",
    ->(opts, v) { opts.account = v},
    "Service account to act as for deployment, if any. Defaults to the GAE " +
    "default service account."
  )
  op.add_option(
    "--dry-run",
    ->(opts, _) { opts.dry_run = true},
    "Don't actually deploy, just log the command lines which would be " +
    "executed on a real invocation."
  )
  op.add_option(
    "--promote",
    ->(opts, _) { opts.promote = true},
    "Promote this version to immediately begin serving API traffic"
  )
  op.add_option(
    "--no-promote",
    ->(opts, _) { opts.promote = false},
    "Deploy, but do not yet serve traffic from this version - DB migrations are still applied"
  )
  op.add_option(
    "--quiet",
    ->(opts, _) { opts.quiet = true},
    "Don't display a confirmation prompt when deploying"
  )
  op.add_validator ->(opts) { raise ArgumentError.new("promote option required") if opts.promote.nil?}

  gcc = GcloudContextV2.new(op)
  op.parse.validate
  gcc.validate

  promote = "--no-promote"
  unless op.opts.promote.nil?
    promote = op.opts.promote ? "--promote" : "--no-promote"
  else
    promote = op.opts.version ? "--no-promote" : "--promote"
  end

  common = Common.new
  env_project = ENVIRONMENTS[op.opts.project]
  env = env_project.fetch(:env_name)
  tanagra_dep("tanagra-dep", ["--env", "#{env}"])

  Dir.chdir('../tanagra-aou-utils/tanagra/ui') do
    common.status "Building Tanagra UI..."
    common.run_inline("npm ci")
    common.status "npm run codegen"
    common.run_inline("npm run codegen")
    ui_base_url = get_config(op.opts.project)["tanagra"]["workbenchBaseUrl"]
    common.status "PUBLIC_URL=/tanagra REACT_APP_POST_MESSAGE_ORIGIN=#{ui_base_url} REACT_APP_GET_LOCAL_AUTH_TOKEN=true npm run build --if-present"
    common.run_inline("PUBLIC_URL=/tanagra REACT_APP_POST_MESSAGE_ORIGIN=#{ui_base_url} REACT_APP_GET_LOCAL_AUTH_TOKEN=true npm run build --if-present")

    common.status "Copying build into appengine folder..."
    common.run_inline("mkdir -p ../../appengine && cp -av ./build ../../appengine/")
  end

  Dir.chdir('../tanagra-aou-utils') do
    common.status "Building appengine config file..."
    common.run_inline("sed 's/${SERVICE_ACCOUNT}/#{op.opts.project}@appspot.gserviceaccount.com/g' tanagra-ui.yaml > ./appengine/tanagra-ui.yaml")
  end

  deploy_version = "tanagra-" + env_project.fetch(:tanagra_tag)
  deploy_version.gsub!(".", "-")

  Dir.chdir('../tanagra-aou-utils/appengine') do
    common.status "Deploying Tanagra UI to appengine..."
    run_inline_or_log(op.opts.dry_run, %W{
      gcloud app deploy tanagra-ui.yaml
      } + %W{--project #{gcc.project} #{promote}} +
      (op.opts.quiet ? %W{--quiet} : []) +
      (deploy_version ? %W{--version #{deploy_version}} : []))
  end
  common.status "Deployment of Tanagra UI complete!"
end

def tanagra_dep(cmd_name, args)
  op = WbOptionsParser.new(cmd_name, args)
  op.add_option(
    "--env [env]",
    ->(opts, v) { opts.env = v},
    "The environment we are building deps for"
  )
  op.add_option(
    "--version [version]",
    ->(opts, v) { opts.version = v},
    "Version to deploy (e.g. your-username-test)"
  )
  op.add_option(
    "--branch [branch]",
    ->(opts, v) { opts.branch = v},
    "Branch to deploy (e.g. freemabd/DT-549)"
  )
  op.add_validator ->(opts) { raise ArgumentError.new("env required") unless opts.env }
  op.parse.validate

  validate_proper_env_args(op)

  common = Common.new
  Dir.chdir('../tanagra-aou-utils') do
    unless File.directory?('tanagra')
      common.status "Need to clone repo"
      common.run_inline %W{git clone https://github.com/DataBiosphere/tanagra.git}
    end
    Dir.chdir('tanagra') do
      checkout_branch_or_tag(op, common)
    end
  end

  common.status "Pulling Tanagra deps complete!"
end

def checkout_branch_or_tag(op, common)
  common.run_inline %W{git fetch}
  if (op.opts.branch)
    common.status "Checkout specified Tanagra branch"
    common.run_inline %W{git checkout #{op.opts.branch}}
    common.run_inline %W{git pull origin #{op.opts.branch}}
  else
    env_project = ENVIRONMENTS[environment_name_to_project_name(op.opts.env)]
    common.status op.opts.version ? "Checkout specified Tanagra tag" : "Using project specified tag from environment variables."
    deploy_version = op.opts.version ? op.opts.version : env_project.fetch(:tanagra_tag)
    common.run_inline %W{git checkout tags/#{deploy_version}}
  end
end

def environment_name_to_project_name(env)
  environment_names_to_project_names = {
      "dev" => "local",
      "local" => "local",
      "test" => "all-of-us-workbench-test",
      "staging" => "all-of-us-rw-staging",
      "stable" => "all-of-us-rw-stable",
      "preprod" => "all-of-us-rw-preprod",
      "prod" => "all-of-us-rw-prod",
  }
  environment_names_to_project_names[env]
end

def validate_proper_env_args(op)
  if (op.opts.version && op.opts.branch)
    puts "Please only provide version or branch as an arg"
    exit 1
  elsif (environment_name_to_project_name(op.opts.env) != "local" && (op.opts.branch || op.opts.version))
    puts "Branch or version args are only allowed with local deployments"
    exit 1
  end
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
    Common.register_command({
                                :invocation => "deploy-tanagra-ui",
                                :description => "Deploys the Tanagra UI.",
                                :fn => ->(*args) { deploy_tanagra_ui("deploy-tanagra-ui", args) }
                            })
    Common.register_command({
                                :invocation => "tanagra-dep",
                                :description => "Build Tanagra deps",
                                :fn => ->(*args) { tanagra_dep("tanagra-dep", args) }
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

    common.run_inline(%W{yarn deps #{environment_name}})
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
