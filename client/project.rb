#!/usr/bin/env ruby
require_relative "libproject/devstart.rb"

common = Common.new
if ARGV.length == 0 or ARGV[0] == "--help"
  common.print_usage
  exit 0
end

command = ARGV.first
args = ARGV.drop(1)

common.handle_or_die(command, *args)
