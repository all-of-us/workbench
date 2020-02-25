require 'logger'
require 'optparse'
require_relative 'tasks/count_monitoring_assets'
require_relative 'tasks/replicate_logs_based_metric'
require_relative 'tasks/delete_all_service_account_keys'

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
    parser.on('-s', '--source-env [SOURCE-ENV]', String, 'Short name for source URI (lowercase)')
    parser.on('-d', '--dry-run TRUE', 'Dry run if true')
  end.parse!({into: options})

  #Now raise an exception if we have not found a required arg
  raise OptionParser::MissingArgument.new('task') if options[:task].nil?
  raise OptionParser::MissingArgument.new('envs-file') if options[:'envs-file'].nil?

  options
end

def run_task(options)
  options[:logger] = Logger.new(STDOUT)

  # New tasks must be included here.
  case options[:task]
  when 'inventory'
    MonitoringAssets.new(options[:'envs-file'], options[:logger]).inventory
  when 'replicate-logs-based-metric'
    ReplicateLogsBasedMetric.new(options, options[:logger]).run
  when 'delete-all-service-account-keys'
    DeleteAllServiceAccountKeys.new(options).run
  else
    logger.error("Unrecognized task #{options[:task]}")
  end
end

options = parse_options

# Begin execution
run_task(options)
