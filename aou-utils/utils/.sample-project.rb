#!/usr/bin/env ruby

PROJECT_SCRIPTS_DIR = "libproject"

unless Dir.exists? "#{PROJECT_SCRIPTS_DIR}/common"
  unless system(*%W{mkdir -p #{PROJECT_SCRIPTS_DIR}})
    STDERR.puts "mkdir failed."
    exit 1
  end
  unless system(*%W{
      git clone https://github.com/dmohs/project-management.git
      #{PROJECT_SCRIPTS_DIR}/common
    })
    STDERR.puts "git clone failed."
    exit 1
  end
end

require_relative "#{PROJECT_SCRIPTS_DIR}/common/common"
Dir.foreach(PROJECT_SCRIPTS_DIR) do |item|
  unless item =~ /^\.\.?$/ || item == "common"
    require_relative "#{PROJECT_SCRIPTS_DIR}/#{item}"
  end
end

# Uncomment this line if using these tools as a submodule instead of a clone.
# Common.unregister_upgrade_self_command

c = Common.new

if ARGV.length == 0 or ARGV[0] == "--help"
  c.print_usage
  exit 0
end

command = ARGV.first
args = ARGV.drop(1)

c.handle_or_die(command, *args)
