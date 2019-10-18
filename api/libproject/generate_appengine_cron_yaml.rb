require 'fileutils'

env = ARGV[0]

cron_dir = "src/main/webapp/WEB-INF"
to_cron_path = ->(env) { "#{cron_dir}/cron_#{env}.yaml" }

path = to_cron_path.call("default")
if File.file?(to_cron_path.call(env))
  path = to_cron_path.call(env)
end

FileUtils.cp(path, "#{cron_dir}/cron.yaml")
