# Generates the App Engine cron.yaml file for a given environment. This is meant
# to be invoked directly via Gradle before running the GAE application.
# For now, this picks cron_{env}.yaml if it exists, cron_default.yaml otherwise.
#
# Future work: it will likely be desirable to support extension rather than
# wholesale file replacement, e.g. to alter just the schedule or exclude a
# particular cron for a given environment.
require 'fileutils'

env = ARGV[0]

cron_dir = "src/main/webapp/WEB-INF"
to_cron_path = ->(suffix) { "#{cron_dir}/cron_#{suffix}.yaml" }

path = to_cron_path.call("default")
if File.file?(to_cron_path.call(env))
  path = to_cron_path.call(env)
end

FileUtils.cp(path, "#{cron_dir}/cron.yaml")
