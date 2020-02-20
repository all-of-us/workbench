require('json')
require('./dashboard_template.rb')
require 'logger'
require "google/cloud/monitoring"

logger = Logger.new(STDOUT)
logger.level = Logger::INFO

template_json = JSON.load(IO.read(ARGV[0]))

target_projects = JSON.load(IO.read(ARGV[1]))

dashboard_template = DashboardTemplate.new(template_json, target_projects, logger)

environment = 'perf'
new_dashboard = dashboard_template.populate(environment)

logger.info('Output of template against:')
logger.info(pp(new_dashboard))
output_json_name = "output-#{environment}.json"

IO.write(ARGV[2], JSON.pretty_generate(new_dashboard))