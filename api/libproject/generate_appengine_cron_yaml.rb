# Generates the App Engine cron.yaml file for a given environment. This is meant
# to be invoked directly via Gradle before running the GAE application.
#
# - Extends cron_base.yaml with cron_{env}.yaml, if any
# - "url" property is treated as the primary key for the cron
# - Environment-specific yaml files may define the following special properties:
#   - aou_skip_reason: omit this job from the cron yaml, for the given reason
require 'fileutils'
require 'yaml'
require_relative "../../aou-utils/utils/common"

aou_env = ARGV[0]

CRON_DIR = "src/main/webapp/WEB-INF"

def to_cron_path(suffix)
  return "#{CRON_DIR}/cron_#{suffix}.yaml"
end

def merge_cron_yaml(base_yaml_file, env_yaml_file, output_yaml_file)
  common = Common.new

  base_yaml = YAML.load_file(base_yaml_file)

  env_cron_by_url = {}
  if File.file?(env_yaml_file)
    env_yaml = YAML.load_file(env_yaml_file)
    env_cron_by_url = env_yaml['cron'].map do |cron|
      url = cron['url']
      raise ArgumentError.new("missing required 'url' key for #{cron}") if url.nil?
      unless base_yaml['cron'].find { |base_cron| base_cron['url'] == url }
        raise ArgumentError.new("cron URL '#{url}' does not exist in base yaml")
      end
      [url, cron]
    end.to_h
  end

  out_yaml = {}
  out_yaml['cron'] = base_yaml['cron'].map do |base_cron|
    url = base_cron['url']
    env_cron = env_cron_by_url[url]
    if env_cron.nil?
      base_cron
    else
      if env_cron.key?('aou_skip_reason')
        common.status "Excluding cron '#{url}', reason: '#{env_cron['aou_skip_reason']}'"
        next
      end
      base_cron.merge(env_cron)
    end
  end.compact

  File.open(output_yaml_file, 'w') { |f| YAML.dump(out_yaml, f) }
end

merge_cron_yaml(to_cron_path("base"), to_cron_path(aou_env), "#{CRON_DIR}/cron.yaml")
