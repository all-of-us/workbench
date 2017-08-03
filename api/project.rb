#!/usr/bin/env ruby

unless Dir.exists? ".project/common"
  unless system(*%W{mkdir -p .project})
    STDERR.puts "mkdir failed."
    exit 1
  end
  unless system(*%W{git clone https://github.com/dmohs/project-management.git .project/common})
    STDERR.puts "git clone failed."
    exit 1
  end
end

require_relative ".project/common/common"
Dir.foreach(".project") do |item|
  unless item =~ /^\.\.?$/ || item == "common"
    require_relative ".project/#{item}"
  end
end

c = Common.new

if ARGV.length == 0 or ARGV[0] == "--help"
  c.print_usage
  exit 0
end

command = ARGV.first
args = ARGV.drop(1)

c.handle_or_die(command, *args)
