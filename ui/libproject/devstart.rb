# UI project management commands and command-line flag definitions.

require "optparse"
require_relative "../../libproject/utils/common"
require_relative "../../libproject/workbench"
require_relative "../../libproject/swagger"

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

def install_dependencies()
  common = Common.new

  common.run_inline %W{docker-compose run --rm ui yarn install}
end

def swagger_regen()
  common = Common.new

  Workbench::Swagger.download_swagger_codegen_cli

  common.run_inline %W{docker-compose run --rm ui yarn run codegen}
end

class DevUpOptions
  ENV_CHOICES = %W{local test prod}
  attr_accessor :env

  def initialize
    self.env = "test"
  end

  def parse args
    parser = OptionParser.new do |parser|
      parser.banner = "Usage: ./project.rb dev-up [options]"
      parser.on(
          "--environment ENV", ENV_CHOICES, "Environment [local (default), test, prod]") do |v|
        self.env = v
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
    common.run_inline %W{node_modules/@angular/cli/bin/ng build --environment=test}
    common.run_inline %W{gcloud app deploy --project #{@opts.project} --account #{@opts.account}
                         --version #{@opts.version} --#{@opts.promote}}
  end
end


def dev_up(*args)
  common = Common.new

  options = DevUpOptions.new.parse(args)

  install_dependencies

  ENV["ENV_FLAG"] = options.env == "local" ? "" : "--environment=#{options.env}"
  at_exit { common.run_inline %W{docker-compose down} }
  swagger_regen()
  common.run_inline %W{docker-compose run -d --service-ports tests}

  common.status "Tests started. Open\n"
  common.status "    http://localhost:9876/debug.html\n"
  common.status "in a browser to view/run tests."

  common.run_inline %W{docker-compose run --rm --service-ports ui}
end

def run_linter()
  Common.new.run_inline %W{docker-compose run --rm ui yarn run lint}
end

def rebuild_image()
  common = Common.new

  common.run_inline %W{docker-compose build}
end

Common.register_command({
  :invocation => "dev-up",
  :description => "Brings up the development environment.",
  :fn => Proc.new { |*args| dev_up(*args) }
})

Common.register_command({
  :invocation => "install-dependencies",
  :description => "Installs dependencies via yarn.",
  :fn => Proc.new { |*args| install_dependencies(*args) }
})

Common.register_command({
  :invocation => "swagger-regen",
  :description => "Regenerates API client libraries from Swagger definitions.",
  :fn => Proc.new { |*args| swagger_regen(*args) }
})

Common.register_command({
  :invocation => "lint",
  :description => "Runs linter.",
  :fn => Proc.new { |*args| run_linter(*args) }
})

Common.register_command({
  :invocation => "rebuild-image",
  :description => "Re-builds the dev docker image (necessary when Dockerfile is updated).",
  :fn => Proc.new { |*args| rebuild_image(*args) }
})

Common.register_command({
  :invocation => "clean-git-hooks",
  :description => "Removes symlinks created by shared-git-hooks. Necessary before re-installing.",
  :fn => Proc.new { |*args| clean-git-hooks(*args) }
})

Common.register_command({
  :invocation => "deploy-ui",
  :description => "Deploys the UI to the specified cloud project.",
  :fn => lambda { |*args| DeployUI.new("deploy-ui", args).run }
})
