require 'logger'
require 'optparse'
require_relative 'tasks/count_monitoring_assets'
require_relative 'tasks/dashboards'
require_relative 'tasks/logs_based_metrics'
require_relative 'tasks/service_accounts'

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
    parser.on('-s', '--source-uri [SOURCE-URI]', String, 'URI or FQ name for source asset')
    parser.on('-u', '--source-env [SOURCE-ENV]', String, 'Short name for source Environment (lowercase)')
    parser.on('-d', '--dry-run', 'Execute a dry run of the task')
  end.parse!({into: options})

  #Now raise an exception if we have not found a required arg
  raise OptionParser::MissingArgument.new('task') if options[:task].nil?
  raise OptionParser::MissingArgument.new('envs-file') if options[:'envs-file'].nil?

  options
end

def build_logger
  logger = Logger.new(STDOUT)
  logger.formatter = proc do |severity, datetime, progname, msg|
    "#{datetime} #{severity}: #{msg}\n"
  end
  logger.datetime_format = '%Y-%m-%d %H:%M:%S'
  logger
end

def run_task(options)
  options[:logger] = build_logger

  # New tasks must be included here.
  case options[:task]
  when 'list-dashboards'
    Dashboards.new(options).list
  when 'inventory'
    MonitoringAssets.new(options[:'envs-file'], options[:logger]).inventory
  when 'replicate-logs-based-metric'
    LogsBasedMetrics.new(options).replicate
  when 'delete-all-service-account-keys'
    ServiceAccounts.new(options).delete_all_keys
  when 'list-dashboards'
    Dashboards.new(options).list
  else
    build_logger.error("Unrecognized task #{options[:task]}")
  end
end

options = parse_options

# Begin execution
run_task(options)
