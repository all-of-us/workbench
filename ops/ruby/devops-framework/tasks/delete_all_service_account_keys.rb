# require 'logger'
require 'open3'
require_relative 'lib/process_runner'

class DeleteAllServiceAccountKeys
  def initialize(options, logger = Logger.new(STDOUT), is_dry_run = true)
    envs_json = JSON.load(IO.read(options[:'envs-file']))
    @environments = envs_json['environments'].map { |env| GcpEnvironmentInfo.new(env) }
    @logger = options[:logger]
    @is_dry_run = options[:'dry-run']
  end

  # This tool does not use the GcpEnvironmentVisitor, as it's specifically cleaning up after it.
  def run
    @environments.each do |env|
      keys = get_keys(env)
      keys.each do |key|
        if @is_dry_run == true
          @logger.info("would delete key #{key} for SA #{env.service_account} in project")
        else
          delete_key(key, env)
        end
      end
    end
  end


  # Get key JSON objects
  def get_keys(env)
    list_cmd = %W[gcloud iam service-accounts keys list
      --iam-account=#{env.service_account}
      --project=#{env.project_id}
      --format=json].join(' ')
    stdout, stderr = Open3.capture2(list_cmd)
    @logger.error(stderr) if stderr
    json_array = JSON.load(stdout)
    json_array
  end

  def delete_key(key, env)
    if key['keyType'] == 'SYSTEM_MANAGED'
      @logger.info("Skipping system-managed key #{key['name']}")
      return
    end

    delete_cmd = %W[yes | gcloud iam service-accounts keys delete
      #{key['name']}
      --iam-account=#{env.service_account}
      --project=#{env.project_id}
    ].join(' ')

    stdout, stderr = Open3.capture2(delete_cmd)
    @logger.error(stderr) if stderr
    stdout
  end
end
