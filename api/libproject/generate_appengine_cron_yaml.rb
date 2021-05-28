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

  out_yaml = YAML.load_file(base_yaml_file)
  base_crons = out_yaml['cron'].map { |c| [c['url'], c] }.to_h

  if File.file?(env_yaml_file)
    env_yaml = YAML.load_file(env_yaml_file)
    env_yaml['cron'].each do |env_cron|

      raise ArgumentError.new("missing required 'url' key for #{env_cron}") unless env_cron.key?('url')
      url = env_cron['url']
      raise ArgumentError.new("cron URL '#{url}' does not exist in base yaml") unless base_crons.key?(url)

      if env_cron.key?('aou_skip_reason')
        common.status "Excluding cron '#{url}', reason: '#{env_cron['aou_skip_reason']}'"
        out_yaml['cron'].reject! {|o| o['url'] == url}
        next
      end

      # This indirectly mutates out_yaml
      base_crons[url].merge!(env_cron)
    end
  end

  File.open(output_yaml_file, 'w') { |f| YAML.dump(out_yaml, f) }
end

merge_cron_yaml(to_cron_path("base"), to_cron_path(aou_env), "#{CRON_DIR}/cron.yaml")
