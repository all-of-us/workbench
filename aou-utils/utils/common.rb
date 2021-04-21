require "open3"
require "ostruct"
require "yaml"

require_relative "dockerhelper"
require_relative "syncfiles"

class Common
  @@commands = []

  def self.register_command(command)
    invocation = command[:invocation]
    fn = command[:fn]
    if fn.nil?
      error "No :fn key defined for command #{invocation}"
      exit 1
    end
    if fn.is_a?(Symbol)
      unless Object.private_method_defined?(fn)
        self.new.error "Function #{fn.to_s} is not defined for #{invocation}."
        exit 1
      end
    else
      # TODO(dmohs): Deprecation warning.
      unless fn.is_a?(Proc)
        self.new.error ":fn key for #{invocation} does not define a Proc or Symbol"
        exit 1
      end
    end

    @@commands.push(command)
  end

  def self.unregister_upgrade_self_command()
    @@commands.reject! { |x| x[:upgrade_self_command] }
  end

  def self.commands()
    @@commands
  end

  attr :docker
  attr :sf

  def initialize()
    @docker = DockerHelper.new(self)
    @sf = SyncFiles.new(self)
  end

  def print_usage()
    STDERR.puts "\nUsage: ./project.rb <command> <options>\n\n"
    STDERR.puts "COMMANDS\n\n"
    @@commands.each do |command|
      STDERR.puts bold_term_text(command[:invocation])
      STDERR.puts command[:description] || "(no description specified)"
      STDERR.puts
    end
  end

  def handle_or_die(args)
    if args.length == 0 or args[0] == "--help"
      print_usage
      exit 0
    end

    if args[0] == "--cmplt" # Shell completion argument name inspired by vault
      # Form of args: --cmplt <index-of-current-argument> ./project.rb arg arg arg
      index = args[1].to_i
      word = args[2 + index]
      puts @@commands.select{ |x| x[:invocation].start_with?(word) }
          .map{ |x| x[:invocation]}.join("\n")
      exit 0
    end

    command = args.first
    handler = @@commands.select{ |x| x[:invocation] == command }.first
    if handler.nil?
      error "#{command} command not found."
      exit 1
    end

    fn = handler[:fn]
    if fn.is_a?(Symbol)
      method(fn).call(*args)
    else
      handler[:fn].call(*args.drop(1))
    end
  end

  def load_env()
    if not File.exists?("project.yaml")
      error "Missing project.yaml"
      exit 1
    end
    OpenStruct.new YAML.load(File.read("project.yaml"))
  end

  def red_term_text(text)
    "\033[0;31m#{text}\033[0m"
  end

  def blue_term_text(text)
    "\033[0;36m#{text}\033[0m"
  end

  def yellow_term_text(text)
    "\033[0;33m#{text}\033[0m"
  end

  def bold_term_text(text)
    "\033[1m#{text}\033[0m"
  end

  def status(text)
    STDERR.puts blue_term_text(text)
  end

  def warning(text)
    STDERR.puts yellow_term_text(text)
  end

  def error(text)
    STDERR.puts red_term_text(text)
  end

  def put_command(cmd, redact=nil)
    if cmd.is_a?(String)
      command_string = "+ #{cmd}"
    else
      command_string = "+ #{cmd.join(" ")}"
    end
    command_to_echo = command_string.clone
    if redact
      command_to_echo.sub! redact, "*" * redact.length
    end
    STDERR.puts command_to_echo
  end

  # Pass err=nil to suppress stderr.
  def capture_stdout(cmd, err = STDERR)
    if err.nil?
      err = "/dev/null"
    end
    output, _ = Open3.capture2(*cmd, :err => err)
    output
  end

  def run_inline(cmd, redact=nil)
    put_command(cmd, redact)

    # `system`, by design (?!), hides stderr when the command fails.
    if ENV["PROJECTRB_USE_SYSTEM"] == "true"
      if not system(*cmd)
        exit $?.exitstatus
      end
    else
      pid = spawn(*cmd)
      Process.wait pid
      if $?.exited?
        if !$?.success?
          exit $?.exitstatus
        end
      else
        error "Command exited abnormally."
        exit 1
      end
    end
  end

  def run_inline_swallowing_interrupt(cmd)
    begin
      run_inline cmd
    rescue Interrupt
    end
  end

  def run_or_fail(cmd, redact=nil)
    put_command(cmd, redact)
    Open3.popen3(*cmd) do |i, o, e, t|
      i.close
      if not t.value.success?
        STDERR.write red_term_text(e.read)
        exit t.value.exitstatus
      end
    end
  end

  def run(cmd)
    Open3.popen3(*cmd) do |i, o, e, t|
      i.close
      t.value
    end
  end

  def pipe(*cmds)
    s = cmds.map { |x| x.join(" ") }
    s = s.join(" | ")
    STDERR.puts "+ #{s}"
    Open3.pipeline(*cmds).each do |status|
      unless status.success?
        error "Piped command failed"
        exit 1
      end
    end
  end
end

def upgrade_self(_)
  c = Common.new
  Dir.chdir(File.dirname(__FILE__)) do
    c.run_inline %W{git pull}
  end
  c.status "Tools upgraded to latest version."
end

Common.register_command({
  :invocation => "upgrade-self",
  :description => "Upgrades this project tool to the latest version.",
  :fn => :upgrade_self,
  :upgrade_self_command => true,
})
