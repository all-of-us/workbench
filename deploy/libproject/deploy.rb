require_relative "../../aou-utils/serviceaccounts"
require_relative "../../aou-utils/utils/common"
require_relative "../../aou-utils/workbench"
require_relative "../../api/libproject/devstart"
require_relative "../../api/libproject/gcloudcontext"
require_relative "../../api/libproject/wboptionsparser"


DOCKER_KEY_FILE_PATH = "/creds/sa-key.json"

def deploy(cmd_name, args)
  op = WbOptionsParser.new(cmd_name, args)
  op.add_option(
    "--project [project]",
    lambda {|opts, v| opts.project = v},
    "The Google Cloud project to deploy to."
  )
  op.add_option(
    "--account [account]",
    lambda {|opts, v| opts.account = v},
    "Service account to act as for deployment, if any. Defaults to the GAE " +
    "default service account."
  )
  op.add_option(
    "--key_file [key file]",
    lambda {|opts, v| opts.key_file = v},
    "Path to a service account key file to be used for deployment"
  )
  op.add_option(
    "--version [version]",
    lambda {|opts, v| opts.version = v},
    "GitHub tag version, e.g. 'v1-0-rc1'"
  )
  op.add_validator lambda {|opts| raise ArgumentError unless opts.version}
  op.add_option(
    "--promote",
    lambda {|opts, v| opts.promote = true},
    "Promote this version to immediately begin serving traffic"
  )
  op.add_option(
    "--no-promote",
    lambda {|opts, v| opts.promote = false},
    "Deploy, but do not yet serve traffic from this version - DB migrations are still applied"
  )
  op.add_validator lambda {|opts| raise ArgumentError if opts.project.nil?}
  op.add_validator lambda {|opts| raise ArgumentError if opts.account.nil?}
  op.add_validator lambda {|opts| raise ArgumentError if opts.promote.nil?}

  op.parse.validate

  # TODO: precondition check against current deployed GAE version?
  common = Common.new
  unless Workbench::in_docker?
    key_file = Tempfile.new(["#{op.opts.account}-key", ".json"])
    ServiceAccountContext.new(
      op.opts.project, account=op.opts.account, path=key_file.path).run do
      common.run_inline %W{
        docker-compose run --rm
        -e WORKBENCH_VERSION=#{op.opts.version}
        -v #{key_file.path}:#{DOCKER_KEY_FILE_PATH}
        deploy deploy/project.rb #{cmd_name}
        --key_file #{DOCKER_KEY_FILE_PATH}
      } + args
      return
    end
  else
    if op.opts.key_file.nil?
      raise ArgumentError.new("--key_file is required when running within docker")
    end
    # TODO: This helper should throw - not exit, else we don't hit the finally
    # and clean up the creds file.
    common.run_inline %W{gcloud auth activate-service-account -q --key-file #{op.opts.key_file}}
  end

  # TODO: Create/update the Jira ticket.
  # TODO: Don't grab another key inside of here.
  common.run_inline %W{
    ../api/project.rb deploy
      --project all-of-us-workbench-test
      --account #{op.opts.account}
      --version #{op.opts.version}
      #{op.opts.promote ? "--promote" : "--no-promote"}
  }

  common.run_inline %W{
    ../ui/project.rb deploy-ui
      --project all-of-us-workbench-test
      --account #{op.opts.account}
      --version #{op.opts.version}
      #{op.opts.promote ? "--promote" : "--no-promote"}
  }
end

Common.register_command({
  :invocation => "deploy",
  :description => "",
  :fn => lambda { |*args| deploy("deploy", args) }
})
