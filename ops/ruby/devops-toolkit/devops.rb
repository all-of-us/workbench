#!/usr/bin/env ruby

require 'logger'
require 'optparse'
require 'ostruct'

require_relative 'tasks/monitoring_assets'
require_relative 'tasks/dashboards'
require_relative 'tasks/developer_environment'
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
    parser.on('-t', '--task [TASK]', String, 'Task to be run in each environment')
    parser.on('-e', '--envs-file [ENVS]', String, 'Path to environments JSON file. See examples directory.')
    parser.on('-s', '--source-uri [SOURCE-URI]', String, 'URI or fully qualified name for source asset')
    parser.on('-u', '--source-env [SOURCE-ENV]', String, 'Short name for source Environment (lowercase), for example "staging".')
    parser.on('-d', '--dry-run', 'Execute a dry run of the task')
    parser.on('-p', '--output-dir [OUTPUT-DIR]', 'Output directory')
    parser.on('-i', '--input-tools-file [INPUT-TOOLS-FILE]', String, 'Input YAML file for tools')
    parser.on('-o', '--output-file [OUTPUT-FILE]', String, 'Output file for task.')
    parser.on('-f', '--output-format [OUTPUT-FORMAT]', String, 'Output format for task. Currently YAML and JSON are supported.')

  end.parse!({into: options})

  # Now raise an exception if we have not found an argument required by all taks
  raise OptionParser::MissingArgument.new('task') if options[:task].nil?

  options
end

# TODO(jaycarlton) take in log level as an argument
def setup_logger
  logger = Logger.new(STDOUT)
  logger.level = Logger::INFO
  logger.datetime_format = '%Y-%m-%d %H:%M:%S'
  logger.formatter = proc do |severity, datetime, _progname, msg|
    "#{datetime} #{severity}: #{msg}\n"
  end

  logger
end

# TODO(jaycarlton): Tasks should handle parsing their own arguments, and work
# more like sub-commands. Which means we'll need a new notion of sub-command
# followed by task, e.g. ./devops.rb dashboards list --envs-file...
def run_task(options)
  options[:logger] = setup_logger
  # New tasks must be included here.
  case options[:task]
  when 'backup-config'
    raise OptionParser::MissingArgument.new('envs-file') if options[:'envs-file'].to_s.empty?
    raise OptionParser::MissingArgument.new('output-dir') if options[:'output-dir'].to_s.empty?
    MonitoringAssets.new(File.expand_path(options[:'envs-file']), File.expand_path(options[:'output-dir']),
                         options[:logger], options[:'output-format'].to_s.downcase.to_sym).backup_config
  when 'delete-all-service-account-keys'
    # Delete all user-generated SA keys for given service account. Should only be necessary
    # to clean up after debug sessions that killed the process before it had time to delete the
    # key associated with the current environment. Note that this may cause other users' jobs to fail
    # if they are also using the same service account with temporary file-based keys.
    ServiceAccounts.new(options).delete_all_keys
  when 'list-all-service-account-keys'
    ServiceAccounts.new(options).list_keys
  when 'list-dashboards'
    Dashboards.new(options).list
  when 'list-dev-tools'
    DeveloperEnvironment.new(options).list
  when 'inventory'
    raise OptionParser::MissingArgument.new('envs-file') if options[:'envs-file'].to_s.empty?
    MonitoringAssets.new(options[:'envs-file'], options[:logger]).inventory
  when 'replicate-dashboard'
    Dashboards.new(options).replicate
  when 'replicate-logs-based-metric'
    LogsBasedMetrics.new(options).replicate
  else
    logger.error("Unrecognized task #{options[:task]}")
  end
end

# Begin execution
run_task(parse_options)
