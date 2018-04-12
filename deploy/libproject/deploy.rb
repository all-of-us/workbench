require "open3"
require "set"
require_relative "../../aou-utils/serviceaccounts"
require_relative "../../aou-utils/utils/common"
require_relative "../../aou-utils/workbench"
require_relative "../../api/libproject/devstart"
require_relative "../../api/libproject/gcloudcontext"
require_relative "../../api/libproject/wboptionsparser"


DOCKER_KEY_FILE_PATH = "/creds/sa-key.json"

def get_source_project(p)
  # Returns the project preceding "p" in the release promotion process.
  return {
    "all-of-us-rw-stable" => "all-of-us-rw-staging"
  }[p]
end

def capture_stdout2(cmd)
  out, _ = Open3.capture2(*cmd)
  return out
end

def get_live_gae_version(project)
  versions = capture_stdout2 %W{
    gcloud app
    --format json(id,service,traffic_split)
    --project #{project}
    versions list
  }
  if versions.empty?
    Common.new.error "Failed to get live version from gcloud"
    exit 1
  end
  services = Set["api", "default", "public-api"]
  actives = JSON.parse(versions).select{|v| v["traffic_split"] == 1.0}
  if actives.empty?
    return nil
  elsif actives.length > services.length
    # Found some extra services. Log
    return nil
  elsif services != actives.map{|v| v["service"]}.to_set
    # Found the wrong services
    return nil
  end

  versions = actives.map{|v| v["id"]}.to_set
  if versions.length != 1
    return nil
  end
  return versions.to_a.first
end

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
    "--git_version [git version]",
    lambda {|opts, v| opts.git_version = v},
    "GitHub tag or branch, e.g. 'v1-0-rc1', 'master'"
  )
  op.add_option(
    "--app_version [app version]",
    lambda {|opts, v| opts.app_version = v},
    "App Engine version to deploy as"
  )
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

  common = Common.new
  unless Workbench::in_docker?
    source_project = get_source_project(op.opts.project)
    live_version = get_live_gae_version(op.opts.project)
    live_source_version = source_project and get_live_gae_version(source_project)
    if live_source_version and not op.opts.app_version
      common.status "--app_version defaulting to #{live_source_version}; " +
                    "found on project #{source_project}"
      op.opts.app_version = live_source_version
    end
    if live_source_version and not op.opts.git_version
      common.status "--git_version defaulting to #{live_source_version}; " +
                    "found on project #{source_project}"
      op.opts.git_version = live_source_version
    end
    if not op.opts.git_version or not op.opts.app_version
      if source_project
        common.error "Failed to find a promotion candidate version in project "+
                     source_project
      else
        common.error "Project #{op.opts.project} has no environment from which " +
                     "to promote a release; please verify that you intended to " +
                     "deploy to this environment using this method (uncommon)"
      end
      common.error "No default version found, both --git_version and " +
                   "--app_version must be specified"
      exit 1
    end

    # TODO: Might be nice to emit the last version creation time here as a
    # sanity check (need to pick which service to do that for...).
    common.status "Current live version is '#{live_version}' (project " +
                  "#{op.opts.project}); will deploy git version " +
                  "'#{op.opts.git_version}' as app engine version " +
                  "'#{op.opts.app_version}'"

    key_file = Tempfile.new(["#{op.opts.account}-key", ".json"])
    ServiceAccountContext.new(
      op.opts.project, account=op.opts.account, path=key_file.path).run do
      common.run_inline %W{docker-compose build deploy}
      common.run_inline %W{
        docker-compose run --rm
        -e WORKBENCH_VERSION=#{op.opts.git_version}
        -v #{key_file.path}:#{DOCKER_KEY_FILE_PATH}
        deploy deploy/project.rb #{cmd_name}
        --account #{op.opts.account}
        --project #{op.opts.project}
        #{op.opts.promote ? "--promote" : "--no-promote"}
        --app_version #{op.opts.app_version}
        --git_version #{op.opts.git_version}
        --key_file #{DOCKER_KEY_FILE_PATH}
      }
      return
    end
  end

  # Everything following runs only within Docker.
  if op.opts.key_file.nil?
    raise ArgumentError.new("--key_file is required when running within docker")
  end
  if op.opts.app_version.nil?
    raise ArgumentError.new("--app_version is required when running within docker")
  end
  if op.opts.git_version.nil?
    raise ArgumentError.new("--git_version is required when running within docker")
  end
  # TODO: This helper should throw - not exit, else we don't hit the finally
  # and clean up the creds file.
  common.run_inline %W{gcloud auth activate-service-account -q --key-file #{op.opts.key_file}}

  # TODO: Create/update the Jira ticket.
  # TODO: Don't grab another key inside of here.
  common.run_inline %W{
    ../api/project.rb deploy
      --project all-of-us-workbench-test
      --account #{op.opts.account}
      --version #{op.opts.app_version}
      #{op.opts.promote ? "--promote" : "--no-promote"}
  }

  common.run_inline %W{
    ../ui/project.rb deploy-ui
      --project all-of-us-workbench-test
      --account #{op.opts.account}
      --version #{op.opts.app_version}
      #{op.opts.promote ? "--promote" : "--no-promote"}
  }
end

Common.register_command({
  :invocation => "deploy",
  :description => "",
  :fn => lambda { |*args| deploy("deploy", args) }
})

def docker_clean(*args)
  common = Common.new
  common.run_inline %W{docker-compose down --volumes}
end

Common.register_command({
  :invocation => "docker-clean",
  :description => \
    "Removes docker containers and volumes, allowing the next `deploy` to " \
    "start from scratch (e.g., no git repo cache). This should not normally " \
    "be necessary outside of deploy script development",
  :fn => lambda { |*args| docker_clean(*args) }
})
