require_relative "utils/common"

ENV["UID"] = "#{Process.euid}"

module Workbench
  WORKBENCH_ROOT = File.expand_path(File.join(File.dirname(__FILE__), '..'))

  def check_submodules()
    # `git clone` includes submodule folders but nothing else.
    unless File.exists? File.join(WORKBENCH_ROOT, "libproject", "utils", "README.md")
      unless system(*%W{git submodule update --init})
        common.error "`git submodule update` failed."
        exit 1
      end
    end
  end
  module_function :check_submodules

  def ensure_git_hooks()
    common = Common.new
    unless common.capture_stdout(%W{git config --get core.hooksPath}).chomp == "hooks"
      common.run_inline %W{git config core.hooksPath hooks}
    end
  end
  module_function :ensure_git_hooks

  def handle_argv_or_die(main_filename)
    common = Common.new
    Dir.chdir(File.dirname(main_filename))

    check_submodules
    ensure_git_hooks
    unless ENV["CIRCLECI"] == "true"
      common.docker.requires_docker
    end

    if File.exist?("db")
      # docker-compose will not run any container if api/db/vars.dev.env doesn't exist (including
      # the container that generates this file), so create an empty one if necessary.
      # TODO(dmohs): This needs to be run before any docker-compose command, but it doesn't belong
      # here.
      unless File.exist?("db/vars.dev.env")
        common.run_inline %W{touch db/vars.dev.env}
      end
    end

    if ARGV.length == 0 or ARGV[0] == "--help"
      common.print_usage
      exit 0
    end

    command = ARGV.first
    args = ARGV.drop(1)

    common.handle_or_die(command, *args)
  end
  module_function :handle_argv_or_die
end
