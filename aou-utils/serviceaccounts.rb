require "json"
require "set"
require_relative "workbench"
require_relative "utils/common"
require "tmpdir"

# Entering a service account context ensures that a keyfile exists at the given
# path for the given service account, and that GOOGLE_APPLICATION_CREDENTIALS is
# pointing to it (for application default credentials). Creates this SA key and
# file if necessary, and destroys it when leaving the context.
# The test SA key is a special case for local development, and is initialized
# but not deleted.
class ServiceAccountContext
  attr_reader :project, :service_account, :keyfile_path

  TEST_SERVICE_ACCOUNTS = Set[
    "all-of-us-workbench-test@appspot.gserviceaccount.com",
    "aou-db-test@appspot.gserviceaccount.com"
  ]
  SERVICE_ACCOUNT_KEY_PATH = "sa-key.json"

  def initialize(project, service_account = nil, keyfile_path = nil)
    @project = project
    @service_account = service_account
    if not service_account
      @service_account = "#{@project}@appspot.gserviceaccount.com"
    end
    @keyfile_path = keyfile_path
    if not @keyfile_path
      if TEST_SERVICE_ACCOUNTS.include? @service_account
        @keyfile_path = File.expand_path(SERVICE_ACCOUNT_KEY_PATH)
      else
        # By default Tempfile on OS X does not use a docker-friendly location/
        # use /tmp/colima to make it work with colima, see https://github.com/abiosoft/colima/issues/844 for more details
        colima_path = "/tmp/colima"
        Dir.mkdir(colima_path) unless File.exist?(colima_path)
        @keyfile_path = Tempfile.new(["#{@service_account}-key", ".json"], colima_path).path
      end
    end
  end

  def existing_file_account(keyfile_path)
    if File.exist?(keyfile_path)
      begin
        return JSON.parse(File.read(keyfile_path))["client_email"]
      rescue JSON::ParserError => e
        return nil
      end
    end
    return nil
  end

  def run()
    common = Common.new
    ENV["GOOGLE_APPLICATION_CREDENTIALS"] = @keyfile_path
    if @service_account == existing_file_account(@keyfile_path)
      # Don't generate another key if this account is already active. This can
      # happen for nested service account contexts, for example.
      common.status "Attaching to existing keyfile @ #{@keyfile_path}"
      yield(self)
      return
    end
    if TEST_SERVICE_ACCOUNTS.include? @service_account
      common.status "Copying key from GCS for #{@service_account} @ #{@keyfile_path}"
      common.run_inline %W{gsutil cp gs://#{@project}-credentials/app-engine-default-sa.json
            #{@keyfile_path}}
      yield(self)
    else
      common.status "Creating new key for #{@service_account} @ #{@keyfile_path}"
      common.run_inline %W{gcloud iam service-accounts keys create #{@keyfile_path}
          --iam-account=#{@service_account} --project=#{@project}}
      begin
        yield(self)
      ensure
        tmp_private_key = `grep private_key_id #{@keyfile_path} | cut -d\\\" -f4`.strip()
        common.run_inline %W{gcloud iam service-accounts keys delete #{tmp_private_key} -q
           --iam-account=#{@service_account} --project=#{@project}}
        common.run_inline %W{rm #{@keyfile_path}}
      end
    end
  end
end
