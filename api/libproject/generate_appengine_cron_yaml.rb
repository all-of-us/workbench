# Generates the App Engine cron.yaml file for a given environment. This is meant
# to be invoked directly via Gradle before running the GAE application.
# For now, this picks cron_{env}.yaml if it exists, cron_default.yaml otherwise.
#
# Future work: it will likely be desirable to support extension rather than
# wholesale file replacement, e.g. to alter just the schedule or exclude a
# particular cron for a given environment.
require 'fileutils'

aou_env = ARGV[0]

CRON_DIR = "src/main/webapp/WEB-INF"

def to_cron_path(suffix)
  return "#{CRON_DIR}/cron_#{suffix}.yaml"
end

from_path = to_cron_path("default")
if File.file?(to_cron_path(aou_env))
  from_path = to_cron_path(aou_env)
end

FileUtils.cp(from_path, "#{CRON_DIR}/cron.yaml")
