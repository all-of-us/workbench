#!/usr/bin/env ruby

# `git clone` includes submodule folders but nothing else.
unless File.exists? "libproject/utils/README.md"
  unless system(*%W{git submodule update libproject/utils})
    STDERR.puts "`git submodule update` failed."
    exit 1
  end
end

require_relative "libproject/utils/common"

#
# Custom script files
#

require_relative "libproject/devstart.rb"

#
# End custom script files
#

common = Common.new

if ARGV.length == 0 or ARGV[0] == "--help"
  common.print_usage
  exit 0
end

command = ARGV.first
args = ARGV.drop(1)

common.handle_or_die(command, *args)
