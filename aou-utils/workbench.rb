require_relative "utils/common"

Common.unregister_upgrade_self_command

ENV["UID"] = "#{Process.euid}"

module Workbench
  LIBPROJECT_DIR = File.dirname(__FILE__)
  WORKBENCH_ROOT = File.expand_path(File.join(LIBPROJECT_DIR, '..'))

  def check_submodules()
    # `git clone` includes submodule folders but nothing else.
    unless File.exists? File.join(LIBPROJECT_DIR, "utils", "README.md")
      unless system(*%W{git submodule update --init --recursive})
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

  def in_docker?()
    File.exist?("/.dockerenv")
  end
  module_function :in_docker?

  def assert_in_docker()
    raise StandardError.new("Not within a docker container") unless Workbench::in_docker?
  end
  module_function :assert_in_docker

  def setup_workspace()
    check_submodules
    ensure_git_hooks
  end
  module_function :setup_workspace

  # Runs a command (typically project.rb) from the main file's directory.
  def handle_argv_or_die(main_filename)
    common = Common.new
    Dir.chdir(File.dirname(main_filename))

    setup_workspace
    unless in_docker?
      common.docker.requires_docker
    end

    if ARGV.length == 0 or ARGV[0] == "--help"
      common.print_usage
      exit 0
    end

    common.handle_or_die(ARGV)
  end
  module_function :handle_argv_or_die

  def read_vars(source)
    hash = {}
    source.each_line do |line|
      line = line.strip()
      if !line.empty?
        parts = line.split("=")
        if parts.length > 1
          hash[parts[0].strip()] = parts[1].strip()
        end
      end
    end
    hash
  end
  module_function :read_vars

  def read_vars_file(path)
    File.open(path, "r") do |f|
      read_vars f
    end
  end
  module_function :read_vars_file

end
