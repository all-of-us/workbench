require "optparse"
require_relative "download-swagger-codegen-cli"
require_relative "utils/common"

def ensure_git_hooks()
  common = Common.new
  unless common.capture_stdout(%W{git config --get core.hooksPath}).chomp == "hooks"
    common.run_inline %W{git config core.hooksPath hooks}
  end
end

def install_dependencies()
  common = Common.new
  common.docker.requires_docker

  common.run_inline %W{docker-compose run --rm ui npm install}
end

def swagger_regen()
  common = Common.new
  common.docker.requires_docker

  unless File.exist?(SWAGGER_CODEGEN_CLI_JAR)
    download_swagger_codegen_cli
  end

  common.run_inline %W{docker-compose run --rm ui npm run codegen}
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

def dev_up(*args)
  common = Common.new
  common.docker.requires_docker

  options = DevUpOptions.new.parse(args)

  ensure_git_hooks

  install_dependencies

  unless File.exist?(SWAGGER_CODEGEN_CLI_JAR)
    download_swagger_codegen_cli
  end

  ENV["ENV_FLAG"] = options.env == "local" ? "" : "--environment=#{options.env}"
  at_exit { common.run_inline %W{docker-compose down} }
  common.run_inline %W{docker-compose run -d tests}

  common.status "Tests started. Open\n"
  common.status "    http://localhost:9876/debug.html\n"
  common.status "in a browser to view/run tests."

  common.run_inline %W{docker-compose run --rm --service-ports ui}
end

def clean_git_hooks()
  common.run_inline %W{find ../.git/hooks -type l -delete}
end

def rebuild_image()
  common = Common.new
  common.docker.requires_docker

  common.run_inline %W{docker-compose build}
end

Common.register_command({
  :invocation => "dev-up",
  :description => "Brings up the development environment.",
  :fn => Proc.new { |*args| dev_up(*args) }
})

Common.register_command({
  :invocation => "install-dependencies",
  :description => "Installs dependencies via npm.",
  :fn => Proc.new { |*args| install_dependencies(*args) }
})

Common.register_command({
  :invocation => "swagger-regen",
  :description => "Regenerates API client libraries from Swagger definitions.",
  :fn => Proc.new { |*args| swagger_regen(*args) }
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
