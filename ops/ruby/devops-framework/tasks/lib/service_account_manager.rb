require "json"
require 'tmpdir'
require 'fileutils'
require 'logger'
require_relative './process_runner.rb'

# Entering a service account context ensures that a keyfile exists at the given
# path for the given service account, and that GOOGLE_APPLICATION_CREDENTIALS is
# pointing to it (for application default credentials). Creates this SA key and
# file if necessary, and destroys it when leaving the context.
#
# Nested service account contexts are not supported.
class ServiceAccountManager

  SERVICE_ACCOUNT_KEY_FILE_NAME = "sa-key.json"

  def initialize(project, service_account, logger = Logger.new(STDOUT))
    @logger = logger
    @project = project
    @service_account = service_account
  end

  attr_reader :project
  attr_reader :service_account
  attr_reader :credentials_path

  def run()
    @logger.info("service_account = #{@service_account}")
    credentials_path = create_credentials_file

    @logger.info("Setting environment variable GOOGLE_APPLICATION_CREDENTIALS to #{credentials_path}")
    ENV["GOOGLE_APPLICATION_CREDENTIALS"] = credentials_path

    begin
      yield self
    ensure
      cleanup_key(credentials_path)
      ENV["GOOGLE_APPLICATION_CREDENTIALS"] = nil
    end
  end

  def make_credentials_path
    random_dir = SecureRandom.alphanumeric
    creds_dir_path = File.join(Dir.tmpdir, random_dir)
    Dir.mkdir(creds_dir_path)
    File.join(creds_dir_path, SERVICE_ACCOUNT_KEY_FILE_NAME)
  end

  def create_credentials_file
    credentials_path = make_credentials_path

    @logger.info("Making a new service account private key for #{@service_account} at #{credentials_path}")
    run_process %W[gcloud iam service-accounts keys create #{credentials_path}
        --iam-account=#{@service_account} --project=#{@project}]
    unless File.exists? credentials_path
      @logger.warning("Failed to create the key file at #{credentials_path}")
    end
    credentials_path
  end

  def cleanup_key(credentials_path)
    @logger.info("Deleting private key file")
    tmp_private_key = `grep private_key_id #{credentials_path} | cut -d\\\" -f4`.strip
    @logger.info("Deleting SA key for #{tmp_private_key}")
    run_process %W[gcloud iam service-accounts keys delete #{tmp_private_key} -q
         --iam-account=#{service_account} --project=#{@project}]
    FileUtils.remove_dir(File.dirname(credentials_path))
  end

  def run_process(cmd)
    ProcessRunner.run(cmd)
  end
end
