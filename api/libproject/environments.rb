require_relative "../../libproject/utils/common"

class Environments
  # TODO(dmohs): production

  PROJECT_NAMES = {
    :dev=>"all-of-us-workbench-test",
  }

  CREDENTIALS_BUCKET_NAMES = {
    :dev=>"all-of-us-workbench-test-credentials",
  }

  SA_FILE_NAMES = {
    :dev=>"all-of-us-workbench-test-9b5c623a838e.json",
  }

  DB_VARS_FILE_NAMES = {
    :dev=>"vars.env",
  }
end
