require 'open3'

# This class avoids the GcpEnvironmentVisitor because we don't want to create or delete
# any new SA keys, and we're doing everything with the gcloud command line anyway.
class ServiceAccounts
  def initialize(options)
    envs_json = JSON.load(IO.read(options[:'envs-file']))
    @environments = envs_json['environments'].map { |env| GcpEnvironmentInfo.new(env) }
    @logger = options[:logger]
    @is_dry_run = !!options[:'dry-run']
  end

  def list_keys
    @environments.each do |env|
      get_keys(env).each do |key|
        @logger.info(key)
      end
    end
  end

  # This tool does not use the GcpEnvironmentVisitor, as it's specifically cleaning up after it.
  def delete_all_keys
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
    stdout, stderr = Open3.capture2(list_cmd) # stderr is spammed by this command, but here for debugging
    JSON.load(stdout)
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
