require_relative "../../libproject/utils/common"

class Credentials

  CREDENTIALS_BUCKET_NAME = "all-of-us-workbench-test-credentials"

  SA_FILE_NAMES = {
    :dev=>"all-of-us-workbench-test-9b5c623a838e.json",
    # TODO(dmohs): production
    :prod=>nil,
  }

  DB_VARS_FILE_NAMES = {
    :dev=>"vars.env",
    # TODO(dmohs): production
    :prod=>nil,
  }
end
