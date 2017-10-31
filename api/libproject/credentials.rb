require_relative "../../libproject/utils/common"

class Credentials

  CREDENTIALS_BUCKET_NAME = "all-of-us-workbench-test-credentials"

  SA_FILE_NAMES = {
    :dev=>"all-of-us-workbench-test-9b5c623a838e.json",
    # TODO(dmohs): production
    :prod=>nil,
  }

  def maybe_download_sa(env_key)
    maybe_download SA_FILE_NAMES.fetch(env_key), "sa-key.json"
  end

  DB_VARS_FILE_NAMES = {
    :dev=>"vars.env",
    # TODO(dmohs): production
    :prod=>nil,
  }

  def maybe_download_db_vars(env_key)
    maybe_download DB_VARS_FILE_NAMES.fetch(env_key), "db/vars.#{env_key.to_s}.env"
  end

  def maybe_download(bucket_file_name, destination_path)
    common = Common.new
    awk_command = "{if($2 == \"True\") print}"
    common.status "Active gcloud account:"
    common.pipe(
      %W{docker-compose run --rm api gcloud config configurations list},
      %W{awk #{awk_command}}
    )
    if File.empty?(destination_path)
      Common.new.run_inline %W{
        docker-compose run --rm api
          gsutil cp gs://#{CREDENTIALS_BUCKET_NAME}/#{bucket_file_name} #{destination_path}
      }
    end
  end
end
