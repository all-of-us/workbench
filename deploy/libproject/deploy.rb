require "open3"
require "set"
require_relative "../../aou-utils/serviceaccounts"
require_relative "../../aou-utils/utils/common"
require_relative "../../aou-utils/workbench"
require_relative "../../api/libproject/devstart"
require_relative "../../api/libproject/gcloudcontext"
require_relative "../../api/libproject/wboptionsparser"


DOCKER_KEY_FILE_PATH = "/creds/sa-key.json"
STAGING_PROJECT = "all-of-us-rw-staging"
VERSION_RE = /^v[[:digit:]]+-[[:digit:]]+-rc[[:digit:]]+$/

# TODO(calbach): Factor these utils down into common.rb
def capture_stdout(cmd)
  # common.capture_stdout suppresses stderr, which is not desired.
  out, _ = Open3.capture2(*cmd)
  return out
end

def yellow_term_text(text)
  "\033[0;33m#{text}\033[0m"
end

def warning(text)
  STDERR.puts yellow_term_text(text)
end

def get_live_gae_version(project, validate_version=true)
  common = Common.new
  versions = capture_stdout %W{
    gcloud app
    --format json(id,service,traffic_split)
    --project #{project}
    versions list
  }
  if versions.empty?
    common.error "Failed to get live GAE version for project '#{project}'"
    exit 1
  end
  services = Set["api", "default", "public-api"]
  actives = JSON.parse(versions).select{|v| v["traffic_split"] == 1.0}
  if actives.empty?
    warning "Found 0 active GAE services in project '#{project}'"
    return nil
  elsif services != actives.map{|v| v["service"]}.to_set
    warning "Found active services #{v}, expected #{services} for project " +
            "'#{project}'"
    return nil
  end

  versions = actives.map{|v| v["id"]}.to_set
  if versions.length != 1
    warning "Found varying IDs across GAE services in project '#{project}': " +
            "#{versions}"
    return nil
  end
  v = versions.to_a.first
  if validate_version and not VERSION_RE.match(v)
    warning "Found a live version '#{v}' in project '#{project}', but it " +
            "doesn't match the expected release version format"
    return nil
  end
  return v
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
    "GitHub tag or branch, e.g. 'v1-0-rc1', 'origin/master'. Branch names " +
    "must be prefixed with 'origin/'. By default, uses the current live " +
    "staging release tag (if staging is in a good state)"
  )
  op.add_option(
    "--app_version [app version]",
    lambda {|opts, v| opts.app_version = v},
    "App Engine version to deploy as. By default, uses the current live " +
    "staging release version (if staging is in a good state)"
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
    if not op.opts.git_version or not op.opts.app_version
      if op.opts.project == STAGING_PROJECT
        common.error "--git_version and --app_version are required when " +
                     "using this script to deploy to staging. Note: this " +
                     "should be an uncommon use case, please see the release " +
                     "documentation for details"
        exit 1
      end
      live_staging_version = get_live_gae_version(STAGING_PROJECT)
      if not live_staging_version
        common.error "No default staging version could be determined for " +
                     "promotion; please investigate or else be explicit in " +
                     "the version to promote by specifying both " +
                     "--git_version and --app_version"
        exit 1
      end
      if live_staging_version and not op.opts.app_version
        common.status "--app_version defaulting to '#{live_staging_version}'; " +
                      "found on project '#{STAGING_PROJECT}'"
        op.opts.app_version = live_staging_version
      end
      if live_staging_version and not op.opts.git_version
        common.status "--git_version defaulting to '#{live_staging_version}'; " +
                      "found on project '#{STAGING_PROJECT}'"
        op.opts.git_version = live_staging_version
      end
    end

    # TODO: Might be nice to emit the last version creation time here as a
    # sanity check (need to pick which service to do that for...).
    live_version = get_live_gae_version(op.opts.project, validate_version=false)
    common.status "Current live version is '#{live_version}' (project " +
                  "#{op.opts.project})"
    puts "Will deploy git version '#{op.opts.git_version}' as App Engine " +
         "version '#{op.opts.app_version}' in project '#{op.opts.project}'"
    printf "Continue? (Y/n): "
    got = STDIN.gets.chomp.strip.upcase
    unless got == '' or got == 'Y'
      exit 1
    end

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
      --project #{op.opts.project}
      --account #{op.opts.account}
      --version #{op.opts.app_version}
      #{op.opts.promote ? "--promote" : "--no-promote"}
  }

  common.run_inline %W{
    ../ui/project.rb deploy-ui
      --project #{op.opts.project}
      --account #{op.opts.account}
      --version #{op.opts.app_version}
      #{op.opts.promote ? "--promote" : "--no-promote"}
      --quiet
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
