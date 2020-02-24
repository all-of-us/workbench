require 'logger'
require_relative 'tasks/count_monitoring_assets.rb'

# Single entry point for the devops framework. This is the only true Ruby Script file. The
# rest are classes.
#
# Based on input commands, this script delegates the work to  a task class, which should not need to
# know anything about environment variables such as ARGV, the run directory,  etc. Tasks should also avoid
# global variables like `logger`, so that we can control logging preferences from the top level.

logger = Logger.new(STDOUT)
monitoring_assets = MonitoringAssets.new(ARGV[0], logger)
monitoring_assets.inventory
