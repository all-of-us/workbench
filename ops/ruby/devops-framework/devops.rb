#!/usr/bin/env ruby

require 'logger'
require 'optparse'
require_relative 'tasks/count_monitoring_assets'

# Single entry point for the devops framework. This is the only true Ruby Script file. The
# rest are classes.
#
# Based on input commands, this script delegates the work to  a task class, which should not need to
# know anything about environment variables such as ARGV, the run directory,  etc. Tasks should also avoid
# global variables like `logger`, so that we can control logging preferences from the top level.

# TODO(jaycarlton): work out a scheme for task-specific options (i.e. subcommands)
def parse_options
  options = {}
  OptionParser.new do |parser|
    parser.on('-t', '--task [TASK]', String, 'Task to be in in each environment')
    parser.on('-e', '--envs-file [ENVS]', String, 'Path to environments JSON file.')
  end.parse!({into: options})

  #Now raise an exception if we have not found a required arg
  raise OptionParser::MissingArgument.new('task') if options[:task].nil?
  raise OptionParser::MissingArgument.new('envs-file') if options[:'envs-file'].nil?

  options
end

def run_task(options)
  logger = Logger.new(STDOUT)

  # New tasks must be included here.
  case options[:task]
  when 'inventory'
    monitoring_assets = MonitoringAssets.new(options[:'envs-file'], logger)
    monitoring_assets.inventory
  else
    logger.error("Unrecognized task #{options[:task]}")
  end
end

options = parse_options

# Begin execution
run_task(options)
